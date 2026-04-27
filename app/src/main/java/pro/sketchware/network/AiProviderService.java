package pro.sketchware.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import pro.sketchware.SketchApplication;
import pro.sketchware.activities.chat.AiChatSettingsHelper;
import pro.sketchware.activities.chat.ContextBuilder;

/**
 * Provider-aware AI service with OpenAI-compatible and Anthropic-specific
 * streaming paths plus retry and XML fallback support.
 */
public class AiProviderService {
    private static final String TAG = "AiProviderService";
    private static final int MAX_PROVIDER_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 1200L;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private static AiProviderService instance;

    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;
    private volatile Call currentStreamingCall;

    public interface StreamListener {
        void onContent(String delta);
        void onReasoning(String delta);
        void onToolCall(String name, String arguments, String id);
        void onFinalMessage(String fullContent, String fullReasoning);
        void onError(String message, Throwable t);
    }

    private enum ProviderFamily {
        OPENAI_COMPATIBLE,
        ANTHROPIC
    }

    private static final class ProviderConfig {
        final ProviderFamily family;
        final String baseUrl;
        final String apiKey;
        final JSONObject extraHeaders;
        final boolean supportsNativeTools;

        ProviderConfig(ProviderFamily family, String baseUrl, String apiKey, JSONObject extraHeaders, boolean supportsNativeTools) {
            this.family = family;
            this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
            this.apiKey = apiKey == null ? "" : apiKey.trim();
            this.extraHeaders = extraHeaders == null ? new JSONObject() : extraHeaders;
            this.supportsNativeTools = supportsNativeTools;
        }
    }

    private static final class ToolCallAccumulator {
        private final int index;
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();
        private final StringBuilder id = new StringBuilder();

        ToolCallAccumulator(int index) {
            this.index = index;
        }

        void appendId(String value) {
            appendIfPresent(id, value);
        }

        void appendName(String value) {
            appendIfPresent(name, value);
        }

        void appendArguments(String value) {
            appendIfPresent(arguments, value);
        }

        String getId() {
            String current = id.toString().trim();
            return current.isEmpty() ? "tool_" + index + "_" + UUID.randomUUID() : current;
        }

        String getName() {
            return name.toString().trim();
        }

        String getArguments() {
            String raw = arguments.toString().trim();
            if (raw.isEmpty()) {
                return "{}";
            }
            try {
                return new JSONObject(raw).toString();
            } catch (Exception ignored) {
                return raw;
            }
        }

        boolean hasAnyPayload() {
            return name.length() > 0 || arguments.length() > 0 || id.length() > 0;
        }

        boolean isReady() {
            return !getName().isEmpty();
        }

        private void appendIfPresent(StringBuilder builder, String value) {
            if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
                return;
            }
            builder.append(value);
        }
    }

    private static final class XmlToolCallExtraction {
        final String cleanedContent;
        final String toolName;
        final String toolArguments;
        final String toolId;

        XmlToolCallExtraction(String cleanedContent, String toolName, String toolArguments, String toolId) {
            this.cleanedContent = cleanedContent == null ? "" : cleanedContent;
            this.toolName = toolName == null ? "" : toolName;
            this.toolArguments = toolArguments == null ? "{}" : toolArguments;
            this.toolId = toolId == null ? "" : toolId;
        }
    }

    private static final class AnthropicStreamState {
        final StringBuilder fullContent = new StringBuilder();
        final StringBuilder fullReasoning = new StringBuilder();
        final ToolCallAccumulator firstTool = new ToolCallAccumulator(0);
        String lastEmittedToolName = "";
        String lastEmittedToolArgs = "";
        String lastEmittedToolId = "";
    }

    private static final class OpenAiStreamState {
        final StringBuilder fullContent = new StringBuilder();
        final StringBuilder fullReasoning = new StringBuilder();
        final Map<Integer, ToolCallAccumulator> toolCalls = new LinkedHashMap<>();
        String lastEmittedToolName = "";
        String lastEmittedToolArgs = "";
        String lastEmittedToolId = "";
    }

    private AiProviderService() {
        this.context = SketchApplication.getContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized AiProviderService getInstance() {
        if (instance == null) {
            instance = new AiProviderService();
        }
        return instance;
    }

    public void sendStreamingMessage(ContextBuilder.Result requestContext, JSONArray tools, String chatMode, StreamListener listener) {
        SharedPreferences prefs = context.getSharedPreferences(AiChatSettingsHelper.PREFS_NAME, Context.MODE_PRIVATE);
        String currentProvider = prefs.getString(AiChatSettingsHelper.PREF_CURRENT_PROVIDER, "groq");
        String currentModel = prefs.getString(AiChatSettingsHelper.PREF_CURRENT_MODEL, "llama-3.1-8b-instant");

        ProviderConfig providerConfig = resolveProviderConfig(prefs, currentProvider);
        if (providerConfig == null) {
            listener.onError("Unsupported provider: " + currentProvider, null);
            return;
        }

        if (!AiChatSettingsHelper.isProviderConfigured(prefs, currentProvider)) {
            listener.onError("Provider not enabled or API key missing", null);
            return;
        }
        if (providerConfig.baseUrl.isEmpty()) {
            listener.onError("Provider endpoint is missing", null);
            return;
        }

        dispatchRequest(providerConfig, currentProvider, currentModel, requestContext, tools, chatMode, listener, 0);
    }

    public void cancelCurrentStream() {
        Call call = currentStreamingCall;
        if (call != null) {
            call.cancel();
        }
        currentStreamingCall = null;
    }

    private void dispatchRequest(ProviderConfig providerConfig, String providerId, String modelName,
                                 ContextBuilder.Result requestContext, JSONArray tools, String chatMode,
                                 StreamListener listener, int retryCount) {
        if (providerConfig.family == ProviderFamily.ANTHROPIC) {
            sendAnthropicStreamingRequest(providerConfig, modelName, requestContext, tools, chatMode, listener, providerId, retryCount);
        } else {
            sendOpenAiCompatibleStreamingRequest(providerConfig, modelName, requestContext, tools, chatMode, listener, providerId, retryCount);
        }
    }

    private void sendOpenAiCompatibleStreamingRequest(ProviderConfig providerConfig, String modelName,
                                                      ContextBuilder.Result requestContext, JSONArray tools, String chatMode,
                                                      StreamListener listener, String providerId, int retryCount) {
        try {
            JSONArray messages = new JSONArray();
            if (!TextUtils.isEmpty(requestContext.getSystemContext())) {
                messages.put(new JSONObject()
                        .put("role", "system")
                        .put("content", requestContext.getSystemContext()));
            }
            JSONArray history = requestContext.getMessages();
            for (int i = 0; i < history.length(); i++) {
                messages.put(history.get(i));
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", modelName);
            jsonBody.put("messages", messages);
            jsonBody.put("stream", true);

            boolean useNativeTools = requestContext.getProviderFormat() == ContextBuilder.ProviderFormat.OPENAI
                    && providerConfig.supportsNativeTools
                    && tools != null
                    && tools.length() > 0
                    && !"normal".equals(chatMode);
            if (useNativeTools) {
                jsonBody.put("tools", tools);
                jsonBody.put("tool_choice", "auto");
            }

            Request request = new Request.Builder()
                    .url(providerConfig.baseUrl)
                    .headers(buildOpenAiHeaders(providerConfig))
                    .post(RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE))
                    .build();

            executeWithRetry(request, retryCount, providerId, listener, (call, response) -> {
                String contentType = response.header("Content-Type", "");
                if (contentType.contains("application/json")) {
                    String body = response.body() != null ? response.body().string() : "";
                    handleOpenAiJsonResponse(body, requestContext, tools, listener);
                    return;
                }
                try (BufferedSource source = response.body().source()) {
                    readOpenAiEventStream(source, requestContext, tools, listener);
                }
            });
        } catch (Exception e) {
            listener.onError("Request preparation error", e);
        }
    }

    private void sendAnthropicStreamingRequest(ProviderConfig providerConfig, String modelName,
                                               ContextBuilder.Result requestContext, JSONArray tools, String chatMode,
                                               StreamListener listener, String providerId, int retryCount) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", modelName);
            jsonBody.put("messages", requestContext.getMessages());
            jsonBody.put("stream", true);
            jsonBody.put("max_tokens", 4096);
            if (!TextUtils.isEmpty(requestContext.getSystemContext())) {
                jsonBody.put("system", requestContext.getSystemContext());
            }

            boolean useNativeTools = requestContext.getProviderFormat() == ContextBuilder.ProviderFormat.ANTHROPIC
                    && tools != null
                    && tools.length() > 0
                    && !"normal".equals(chatMode);
            if (useNativeTools) {
                jsonBody.put("tools", convertToolsToAnthropic(tools));
                jsonBody.put("tool_choice", new JSONObject().put("type", "auto"));
            }

            Request request = new Request.Builder()
                    .url(providerConfig.baseUrl)
                    .headers(buildAnthropicHeaders(providerConfig))
                    .post(RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE))
                    .build();

            executeWithRetry(request, retryCount, providerId, listener, (call, response) -> {
                try (BufferedSource source = response.body().source()) {
                    readAnthropicEventStream(source, listener);
                }
            });
        } catch (Exception e) {
            listener.onError("Request preparation error", e);
        }
    }

    private void executeWithRetry(Request request, int retryCount, String providerId, StreamListener listener,
                                  ResponseHandler responseHandler) {
        Call call = client.newCall(request);
        currentStreamingCall = call;
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call failedCall, IOException e) {
                if (currentStreamingCall == failedCall) {
                    currentStreamingCall = null;
                }
                if (failedCall.isCanceled()) {
                    listener.onError("cancelled", e);
                    return;
                }
                if (shouldRetryForFailure(e, retryCount)) {
                    scheduleRetry(request, retryCount, providerId, listener, responseHandler);
                    return;
                }
                listener.onError(e.getMessage(), e);
            }

            @Override
            public void onResponse(Call respondedCall, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    if (currentStreamingCall == respondedCall) {
                        currentStreamingCall = null;
                    }
                    if (shouldRetryForStatus(response.code(), retryCount)) {
                        scheduleRetry(request, retryCount, providerId, listener, responseHandler);
                        return;
                    }
                    listener.onError(buildHttpErrorMessage(providerId, response.code(), errorBody), null);
                    return;
                }

                try (Response safeResponse = response) {
                    responseHandler.handle(respondedCall, safeResponse);
                } catch (Exception e) {
                    if (shouldRetryForFailure(e, retryCount)) {
                        scheduleRetry(request, retryCount, providerId, listener, responseHandler);
                        return;
                    }
                    listener.onError("Stream reading error", e);
                    return;
                } finally {
                    if (currentStreamingCall == respondedCall) {
                        currentStreamingCall = null;
                    }
                }
            }
        });
    }

    private void scheduleRetry(Request request, int retryCount, String providerId, StreamListener listener,
                               ResponseHandler responseHandler) {
        if (retryCount >= MAX_PROVIDER_RETRIES) {
            listener.onError("Request failed after retries for provider: " + providerId, null);
            return;
        }
        mainHandler.postDelayed(() -> executeWithRetry(request, retryCount + 1, providerId, listener, responseHandler),
                RETRY_DELAY_MS * (retryCount + 1L));
    }

    private void readOpenAiEventStream(BufferedSource source, ContextBuilder.Result requestContext,
                                       JSONArray tools, StreamListener listener) throws IOException {
        OpenAiStreamState state = new OpenAiStreamState();
        String line;
        while ((line = source.readUtf8Line()) != null) {
            if (!line.startsWith("data:")) {
                continue;
            }

            String data = line.substring(5).trim();
            if (data.isEmpty()) {
                continue;
            }
            if ("[DONE]".equals(data)) {
                break;
            }

            try {
                handleOpenAiChunk(new JSONObject(data), state, listener);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing stream chunk: " + data, e);
            }
        }

        completeOpenAiRequest(state, requestContext, tools, listener);
    }

    private void handleOpenAiJsonResponse(String body, ContextBuilder.Result requestContext,
                                          JSONArray tools, StreamListener listener) {
        OpenAiStreamState state = new OpenAiStreamState();
        try {
            JSONObject json = new JSONObject(body);
            JSONArray choices = json.optJSONArray("choices");
            JSONObject firstChoice = choices != null && choices.length() > 0 ? choices.optJSONObject(0) : null;
            JSONObject message = firstChoice == null ? null : firstChoice.optJSONObject("message");
            if (message != null) {
                String content = sanitizeStreamValue(message.opt("content"));
                if (!content.isEmpty()) {
                    state.fullContent.append(content);
                    listener.onContent(content);
                }

                String reasoning = readReasoningText(message);
                if (!reasoning.isEmpty()) {
                    state.fullReasoning.append(reasoning);
                    listener.onReasoning(reasoning);
                }

                JSONArray toolCalls = message.optJSONArray("tool_calls");
                appendOpenAiToolCalls(toolCalls, state, listener);
            }
        } catch (Exception e) {
            listener.onError("Failed to parse JSON response", e);
            return;
        }

        completeOpenAiRequest(state, requestContext, tools, listener);
    }

    private void handleOpenAiChunk(JSONObject json, OpenAiStreamState state, StreamListener listener) {
        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return;
        }

        JSONObject delta = choices.optJSONObject(0) == null ? null : choices.optJSONObject(0).optJSONObject("delta");
        if (delta == null) {
            return;
        }

        String content = readStreamText(delta, "content");
        if (!content.isEmpty()) {
            state.fullContent.append(content);
            listener.onContent(content);
        }

        String reasoning = readReasoningText(delta);
        if (!reasoning.isEmpty()) {
            state.fullReasoning.append(reasoning);
            listener.onReasoning(reasoning);
        }

        appendOpenAiToolCalls(delta.optJSONArray("tool_calls"), state, listener);
    }

    private void appendOpenAiToolCalls(JSONArray toolCalls, OpenAiStreamState state, StreamListener listener) {
        if (toolCalls == null || toolCalls.length() == 0) {
            return;
        }

        for (int i = 0; i < toolCalls.length(); i++) {
            JSONObject toolCall = toolCalls.optJSONObject(i);
            if (toolCall == null) {
                continue;
            }

            int index = toolCall.optInt("index", i);
            ToolCallAccumulator accumulator = state.toolCalls.get(index);
            if (accumulator == null) {
                accumulator = new ToolCallAccumulator(index);
                state.toolCalls.put(index, accumulator);
            }

            accumulator.appendId(sanitizeStreamValue(toolCall.opt("id")));
            JSONObject function = toolCall.optJSONObject("function");
            if (function != null) {
                accumulator.appendName(sanitizeStreamValue(function.opt("name")));
                accumulator.appendArguments(sanitizeStreamValue(function.opt("arguments")));
            }
        }

        ToolCallAccumulator first = firstReadyOpenAiTool(state);
        if (first != null) {
            maybeEmitToolCall(first.getName(), first.getArguments(), first.getId(), state, listener);
        }
    }

    private ToolCallAccumulator firstReadyOpenAiTool(OpenAiStreamState state) {
        for (ToolCallAccumulator accumulator : state.toolCalls.values()) {
            if (accumulator.isReady() || accumulator.hasAnyPayload()) {
                return accumulator;
            }
        }
        return null;
    }

    private void completeOpenAiRequest(OpenAiStreamState state, ContextBuilder.Result requestContext,
                                       JSONArray tools, StreamListener listener) {
        String finalContent = state.fullContent.toString();
        ToolCallAccumulator firstTool = firstReadyOpenAiTool(state);
        if (firstTool != null && firstTool.isReady()) {
            maybeEmitToolCall(firstTool.getName(), firstTool.getArguments(), firstTool.getId(), state, listener);
        } else if (requestContext.getProviderFormat() == ContextBuilder.ProviderFormat.XML_FALLBACK) {
            XmlToolCallExtraction extraction = extractXmlToolCall(finalContent, tools);
            if (extraction != null) {
                finalContent = extraction.cleanedContent;
                maybeEmitToolCall(extraction.toolName, extraction.toolArguments, extraction.toolId, state, listener);
            }
        }

        if (finalContent.trim().isEmpty() && state.fullReasoning.toString().trim().isEmpty()
                && (firstTool == null || !firstTool.isReady())) {
            listener.onError("Void-style provider response was empty.", null);
            return;
        }
        listener.onFinalMessage(finalContent, state.fullReasoning.toString());
    }

    private void readAnthropicEventStream(BufferedSource source, StreamListener listener) throws IOException {
        AnthropicStreamState state = new AnthropicStreamState();
        String currentEvent = "";
        StringBuilder dataBuffer = new StringBuilder();
        String line;

        while ((line = source.readUtf8Line()) != null) {
            if (line.startsWith("event:")) {
                currentEvent = line.substring(6).trim();
                continue;
            }
            if (line.startsWith("data:")) {
                if (dataBuffer.length() > 0) {
                    dataBuffer.append('\n');
                }
                dataBuffer.append(line.substring(5).trim());
                continue;
            }
            if (line.isEmpty()) {
                dispatchAnthropicEvent(currentEvent, dataBuffer.toString(), state, listener);
                currentEvent = "";
                dataBuffer.setLength(0);
            }
        }

        if (dataBuffer.length() > 0) {
            dispatchAnthropicEvent(currentEvent, dataBuffer.toString(), state, listener);
        }

        if (!state.firstTool.getName().isEmpty()) {
            maybeEmitAnthropicToolCall(state.firstTool.getName(), state.firstTool.getArguments(), state.firstTool.getId(), state, listener);
        }

        if (state.fullContent.toString().trim().isEmpty()
                && state.fullReasoning.toString().trim().isEmpty()
                && state.firstTool.getName().isEmpty()) {
            listener.onError("Anthropic response was empty.", null);
            return;
        }
        listener.onFinalMessage(state.fullContent.toString(), state.fullReasoning.toString());
    }

    private void dispatchAnthropicEvent(String eventName, String data,
                                        AnthropicStreamState state, StreamListener listener) {
        if (data == null || data.trim().isEmpty() || "[DONE]".equals(data.trim())) {
            return;
        }
        try {
            JSONObject json = new JSONObject(data);
            String type = json.optString("type", eventName == null ? "" : eventName);

            if ("error".equals(type)) {
                JSONObject error = json.optJSONObject("error");
                throw new IOException(error == null ? "Anthropic stream error" : error.toString());
            }

            if ("content_block_start".equals(type)) {
                JSONObject block = json.optJSONObject("content_block");
                if (block == null) {
                    return;
                }
                String blockType = block.optString("type", "");
                if ("text".equals(blockType)) {
                    String text = block.optString("text", "");
                    if (!text.isEmpty()) {
                        state.fullContent.append(text);
                        listener.onContent(text);
                    }
                } else if ("thinking".equals(blockType)) {
                    String text = block.optString("thinking", "");
                    if (!text.isEmpty()) {
                        state.fullReasoning.append(text);
                        listener.onReasoning(text);
                    }
                } else if ("redacted_thinking".equals(blockType)) {
                    String text = "[redacted_thinking]";
                    state.fullReasoning.append(text);
                    listener.onReasoning(text);
                } else if ("tool_use".equals(blockType)) {
                    state.firstTool.appendId(block.optString("id", ""));
                    state.firstTool.appendName(block.optString("name", ""));
                    maybeEmitAnthropicToolCall(state.firstTool.getName(), state.firstTool.getArguments(), state.firstTool.getId(), state, listener);
                }
                return;
            }

            if ("content_block_delta".equals(type)) {
                JSONObject delta = json.optJSONObject("delta");
                if (delta == null) {
                    return;
                }
                String deltaType = delta.optString("type", "");
                if ("text_delta".equals(deltaType)) {
                    String text = delta.optString("text", "");
                    if (!text.isEmpty()) {
                        state.fullContent.append(text);
                        listener.onContent(text);
                    }
                } else if ("thinking_delta".equals(deltaType)) {
                    String text = delta.optString("thinking", "");
                    if (!text.isEmpty()) {
                        state.fullReasoning.append(text);
                        listener.onReasoning(text);
                    }
                } else if ("input_json_delta".equals(deltaType)) {
                    state.firstTool.appendArguments(delta.optString("partial_json", ""));
                    maybeEmitAnthropicToolCall(state.firstTool.getName(), state.firstTool.getArguments(), state.firstTool.getId(), state, listener);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Headers buildOpenAiHeaders(ProviderConfig providerConfig) {
        Headers.Builder headers = new Headers.Builder();
        headers.add("Content-Type", "application/json");
        if (!providerConfig.apiKey.isEmpty()) {
            headers.add("Authorization", "Bearer " + providerConfig.apiKey);
        }
        addExtraHeaders(headers, providerConfig.extraHeaders);
        return headers.build();
    }

    private Headers buildAnthropicHeaders(ProviderConfig providerConfig) {
        Headers.Builder headers = new Headers.Builder();
        headers.add("Content-Type", "application/json");
        headers.add("x-api-key", providerConfig.apiKey);
        headers.add("anthropic-version", "2023-06-01");
        addExtraHeaders(headers, providerConfig.extraHeaders);
        return headers.build();
    }

    private void addExtraHeaders(Headers.Builder headers, JSONObject extraHeaders) {
        JSONArray headerNames = extraHeaders == null ? null : extraHeaders.names();
        for (int i = 0; headerNames != null && i < headerNames.length(); i++) {
            String name = headerNames.optString(i, "");
            if (name.isEmpty()) {
                continue;
            }
            headers.add(name, extraHeaders.optString(name, ""));
        }
    }

    private JSONArray convertToolsToAnthropic(JSONArray openAiTools) {
        JSONArray anthropicTools = new JSONArray();
        for (int i = 0; i < openAiTools.length(); i++) {
            JSONObject openAiTool = openAiTools.optJSONObject(i);
            JSONObject function = openAiTool == null ? null : openAiTool.optJSONObject("function");
            if (function == null) {
                continue;
            }

            try {
                JSONObject anthropicTool = new JSONObject();
                anthropicTool.put("name", function.optString("name", ""));
                anthropicTool.put("description", function.optString("description", ""));
                anthropicTool.put("input_schema", function.optJSONObject("parameters") == null
                        ? new JSONObject().put("type", "object").put("properties", new JSONObject())
                        : function.optJSONObject("parameters"));
                anthropicTools.put(anthropicTool);
            } catch (Exception ignored) {
            }
        }
        return anthropicTools;
    }

    private ProviderConfig resolveProviderConfig(SharedPreferences prefs, String providerId) {
        switch (providerId) {
            case "anthropic":
                return new ProviderConfig(
                        ProviderFamily.ANTHROPIC,
                        "https://api.anthropic.com/v1/messages",
                        prefs.getString("anthropic_api_key", ""),
                        readHeadersJson(null),
                        true
                );
            case "openai":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        "https://api.openai.com/v1/chat/completions",
                        prefs.getString("openai_api_key", ""),
                        readHeadersJson(null),
                        true
                );
            case "gemini":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                        prefs.getString("gemini_api_key", ""),
                        readHeadersJson(null),
                        true
                );
            case "groq":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        "https://api.groq.com/openai/v1/chat/completions",
                        prefs.getString("groq_api_key", ""),
                        readHeadersJson(null),
                        true
                );
            case "deepseek":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        "https://api.deepseek.com/chat/completions",
                        prefs.getString("deepseek_api_key", ""),
                        readHeadersJson(null),
                        true
                );
            case "openrouter":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        "https://openrouter.ai/api/v1/chat/completions",
                        prefs.getString("openrouter_api_key", ""),
                        readHeadersJson("{\"HTTP-Referer\":\"https://github.com/FabioSilva11/Sketchware-IA\",\"X-Title\":\"Sketchware IA\"}"),
                        true
                );
            case "openai_compatible":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        normalizeChatCompletionsUrl(prefs.getString("openai_compatible_base_url", "")),
                        prefs.getString("openai_compatible_api_key", ""),
                        readHeadersJson(prefs.getString("openai_compatible_headers", "{}")),
                        false
                );
            case "grok_xai":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        "https://api.x.ai/v1/chat/completions",
                        prefs.getString("grok_xai_api_key", ""),
                        readHeadersJson(null),
                        true
                );
            case "mistral":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        "https://api.mistral.ai/v1/chat/completions",
                        prefs.getString("mistral_api_key", ""),
                        readHeadersJson(null),
                        true
                );
            case "litellm":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        normalizeChatCompletionsUrl(prefs.getString("litellm_base_url", "")),
                        "",
                        readHeadersJson(null),
                        false
                );
            case "ollama":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        normalizeOpenAiLocalUrl(prefs.getString("local_provider_ollama_url", "http://127.0.0.1:11434")),
                        "",
                        readHeadersJson(null),
                        false
                );
            case "vllm":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        normalizeOpenAiLocalUrl(prefs.getString("local_provider_vllm_url", "http://localhost:8000")),
                        "",
                        readHeadersJson(null),
                        false
                );
            case "lm_studio":
                return new ProviderConfig(
                        ProviderFamily.OPENAI_COMPATIBLE,
                        normalizeOpenAiLocalUrl(prefs.getString("local_provider_lm_studio_url", "http://localhost:1234")),
                        "",
                        readHeadersJson(null),
                        false
                );
            default:
                return null;
        }
    }

    private JSONObject readHeadersJson(String raw) {
        try {
            return raw == null || raw.trim().isEmpty() ? new JSONObject() : new JSONObject(raw);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private String readStreamText(JSONObject jsonObject, String key) {
        if (jsonObject == null || key == null || !jsonObject.has(key) || jsonObject.isNull(key)) {
            return "";
        }
        return sanitizeStreamValue(jsonObject.opt(key));
    }

    private String readReasoningText(JSONObject jsonObject) {
        String[] keys = new String[] {"reasoning_content", "reasoning", "thinking"};
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            String value = readStreamText(jsonObject, key);
            if (!value.isEmpty()) {
                builder.append(value);
            }
        }
        return builder.toString();
    }

    private String sanitizeStreamValue(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return "";
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text.trim()) ? "" : text;
    }

    private String normalizeOpenAiLocalUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.endsWith("/api/chat")) {
            return trimmed;
        }
        if (trimmed.contains("/v1/chat/completions")) {
            return trimmed;
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/chat/completions";
        }
        return trimmed + "/v1/chat/completions";
    }

    private String normalizeChatCompletionsUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/chat/completions";
        }
        if (trimmed.endsWith("/")) {
            return trimmed + "chat/completions";
        }
        return trimmed + "/chat/completions";
    }

    private boolean shouldRetryForStatus(int statusCode, int retryCount) {
        return retryCount < MAX_PROVIDER_RETRIES
                && (statusCode == 408 || statusCode == 409 || statusCode == 429 || statusCode >= 500);
    }

    private boolean shouldRetryForFailure(Throwable throwable, int retryCount) {
        return retryCount < MAX_PROVIDER_RETRIES && throwable instanceof IOException;
    }

    private String buildHttpErrorMessage(String providerId, int statusCode, String errorBody) {
        String compactBody = errorBody == null ? "" : errorBody.trim();
        if (compactBody.length() > 400) {
            compactBody = compactBody.substring(0, 400).trim() + "...";
        }
        if (compactBody.isEmpty()) {
            return "API Error from " + providerId + ": HTTP " + statusCode;
        }
        return "API Error from " + providerId + ": HTTP " + statusCode + " - " + compactBody;
    }

    private XmlToolCallExtraction extractXmlToolCall(String fullContent, JSONArray tools) {
        if (fullContent == null || fullContent.trim().isEmpty() || tools == null || tools.length() == 0) {
            return null;
        }

        try {
            for (int i = 0; i < tools.length(); i++) {
                JSONObject tool = tools.optJSONObject(i);
                JSONObject function = tool == null ? null : tool.optJSONObject("function");
                String toolName = function == null ? "" : function.optString("name", "").trim();
                if (toolName.isEmpty()) {
                    continue;
                }

                String openTag = "<" + toolName + ">";
                String closeTag = "</" + toolName + ">";
                int start = fullContent.indexOf(openTag);
                int end = fullContent.indexOf(closeTag);
                if (start < 0 || end < 0 || end < start) {
                    continue;
                }

                String inner = fullContent.substring(start + openTag.length(), end);
                JSONObject params = new JSONObject();
                JSONObject schema = function.optJSONObject("parameters");
                JSONObject properties = schema == null ? null : schema.optJSONObject("properties");
                JSONArray names = properties == null ? null : properties.names();
                for (int j = 0; names != null && j < names.length(); j++) {
                    String paramName = names.optString(j, "").trim();
                    if (paramName.isEmpty()) {
                        continue;
                    }
                    String paramValue = readXmlTag(inner, paramName);
                    if (!paramValue.isEmpty()) {
                        params.put(paramName, paramValue);
                    }
                }

                String cleaned = (fullContent.substring(0, start) + fullContent.substring(end + closeTag.length())).trim();
                return new XmlToolCallExtraction(
                        cleaned,
                        toolName,
                        params.toString(),
                        "xml_call_" + UUID.randomUUID()
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse XML tool call fallback", e);
        }
        return null;
    }

    private String readXmlTag(String xml, String tagName) {
        if (xml == null || tagName == null || tagName.trim().isEmpty()) {
            return "";
        }
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = xml.indexOf(openTag);
        int end = xml.indexOf(closeTag);
        if (start < 0 || end < 0 || end < start) {
            return "";
        }
        return xml.substring(start + openTag.length(), end).trim();
    }

    private void maybeEmitToolCall(String name, String arguments, String id, OpenAiStreamState state, StreamListener listener) {
        String safeName = name == null ? "" : name.trim();
        String safeArguments = arguments == null || arguments.trim().isEmpty() ? "{}" : arguments.trim();
        String safeId = id == null || id.trim().isEmpty() ? "tool_" + UUID.randomUUID() : id.trim();
        if (safeName.isEmpty()) {
            return;
        }
        if (safeName.equals(state.lastEmittedToolName)
                && safeArguments.equals(state.lastEmittedToolArgs)
                && safeId.equals(state.lastEmittedToolId)) {
            return;
        }
        state.lastEmittedToolName = safeName;
        state.lastEmittedToolArgs = safeArguments;
        state.lastEmittedToolId = safeId;
        listener.onToolCall(safeName, safeArguments, safeId);
    }

    private void maybeEmitAnthropicToolCall(String name, String arguments, String id,
                                            AnthropicStreamState state, StreamListener listener) {
        String safeName = name == null ? "" : name.trim();
        String safeArguments = arguments == null || arguments.trim().isEmpty() ? "{}" : arguments.trim();
        String safeId = id == null || id.trim().isEmpty() ? "tool_" + UUID.randomUUID() : id.trim();
        if (safeName.isEmpty()) {
            return;
        }
        if (safeName.equals(state.lastEmittedToolName)
                && safeArguments.equals(state.lastEmittedToolArgs)
                && safeId.equals(state.lastEmittedToolId)) {
            return;
        }
        state.lastEmittedToolName = safeName;
        state.lastEmittedToolArgs = safeArguments;
        state.lastEmittedToolId = safeId;
        listener.onToolCall(safeName, safeArguments, safeId);
    }

    private interface ResponseHandler {
        void handle(Call call, Response response) throws Exception;
    }
}
