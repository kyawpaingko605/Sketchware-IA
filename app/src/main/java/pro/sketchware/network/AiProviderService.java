package pro.sketchware.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;
import pro.sketchware.activities.chat.AiChatSettingsHelper;

/**
 * AiProviderService - Robust service for AI communication with Streaming support.
 * Inspired by Void Editor's streaming architecture.
 */
public class AiProviderService {
    private static final String TAG = "AiProviderService";
    private static AiProviderService instance;

    private final Context context;
    private final OkHttpClient client;
    private volatile Call currentStreamingCall;

    public interface StreamListener {
        void onContent(String delta);
        void onReasoning(String delta);
        void onToolCall(String name, String arguments, String id);
        void onFinalMessage(String fullContent, String fullReasoning);
        void onError(String message, Throwable t);
    }

    private AiProviderService() {
        this.context = pro.sketchware.SketchApplication.getContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // No timeout for streaming
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized AiProviderService getInstance() {
        if (instance == null) {
            instance = new AiProviderService();
        }
        return instance;
    }

    public void sendStreamingMessage(String message, JSONArray tools, JSONArray chatHistory, String systemPrompt, String chatMode, StreamListener listener) {
        SharedPreferences prefs = context.getSharedPreferences("ia_settings", Context.MODE_PRIVATE);
        String currentProvider = prefs.getString("current_ai_provider", "groq");
        String currentModel = prefs.getString("current_ai_model", "llama-3.1-8b-instant");

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

        JSONArray messages = new JSONArray();
        try {
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            }

            if (chatHistory != null) {
                for (int i = 0; i < chatHistory.length(); i++) {
                    messages.put(chatHistory.get(i));
                }
            }

            if (message != null && !message.trim().isEmpty()) {
                messages.put(new JSONObject().put("role", "user").put("content", message));
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", currentModel);
            jsonBody.put("messages", messages);
            jsonBody.put("stream", true);
            if (tools != null && tools.length() > 0) {
                jsonBody.put("tools", tools);
            }
            if (!TextUtils.isEmpty(chatMode) && !"normal".equals(chatMode)) {
                jsonBody.put("tool_choice", "auto");
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .url(providerConfig.baseUrl)
                    .addHeader("Content-Type", "application/json");
            if (!providerConfig.apiKey.isEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer " + providerConfig.apiKey);
            }
            if (providerConfig.extraHeaders != null) {
                JSONArray headerNames = providerConfig.extraHeaders.names();
                for (int i = 0; headerNames != null && i < headerNames.length(); i++) {
                    String name = headerNames.optString(i, "");
                    if (name.isEmpty()) {
                        continue;
                    }
                    requestBuilder.addHeader(name, providerConfig.extraHeaders.optString(name, ""));
                }
            }
            Request request = requestBuilder
                    .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                    .build();

            Call call = client.newCall(request);
            currentStreamingCall = call;
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (currentStreamingCall == call) {
                        currentStreamingCall = null;
                    }
                    if (call.isCanceled()) {
                        listener.onError("cancelled", e);
                        return;
                    }
                    listener.onError(e.getMessage(), e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        if (currentStreamingCall == call) {
                            currentStreamingCall = null;
                        }
                        listener.onError("API Error: " + response.code(), null);
                        return;
                    }

                    try (BufferedSource source = response.body().source()) {
                        StringBuilder fullContent = new StringBuilder();
                        StringBuilder fullReasoning = new StringBuilder();
                        
                        String line;
                        while ((line = source.readUtf8Line()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if (data.equals("[DONE]")) break;

                                try {
                                    JSONObject json = new JSONObject(data);
                                    JSONArray choices = json.optJSONArray("choices");
                                    if (choices != null && choices.length() > 0) {
                                        JSONObject delta = choices.getJSONObject(0).optJSONObject("delta");
                                        if (delta != null) {
                                            // Content
                                            String content = delta.optString("content", "");
                                            if (!content.isEmpty()) {
                                                fullContent.append(content);
                                                listener.onContent(content);
                                            }

                                            // Reasoning (for models like DeepSeek or O1)
                                            String reasoning = delta.optString("reasoning_content", "");
                                            if (reasoning.isEmpty()) {
                                                reasoning = delta.optString("reasoning", "");
                                            }
                                            if (!reasoning.isEmpty()) {
                                                fullReasoning.append(reasoning);
                                                listener.onReasoning(reasoning);
                                            }

                                            // Tool Calls
                                            JSONArray toolCalls = delta.optJSONArray("tool_calls");
                                            if (toolCalls != null && toolCalls.length() > 0) {
                                                JSONObject toolCall = toolCalls.getJSONObject(0);
                                                String id = toolCall.optString("id", "");
                                                JSONObject function = toolCall.optJSONObject("function");
                                                if (function != null) {
                                                    String name = function.optString("name", "");
                                                    String args = function.optString("arguments", "");
                                                    listener.onToolCall(name, args, id);
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing stream chunk: " + data, e);
                                }
                            }
                        }
                        listener.onFinalMessage(fullContent.toString(), fullReasoning.toString());
                    } catch (Exception e) {
                        listener.onError("Stream reading error", e);
                    } finally {
                        if (currentStreamingCall == call) {
                            currentStreamingCall = null;
                        }
                    }
                }
            });

        } catch (Exception e) {
            listener.onError("Request preparation error", e);
        }
    }

    public void cancelCurrentStream() {
        Call call = currentStreamingCall;
        if (call != null) {
            call.cancel();
        }
        currentStreamingCall = null;
    }

    private ProviderConfig resolveProviderConfig(SharedPreferences prefs, String providerId) {
        switch (providerId) {
            case "openai":
                return new ProviderConfig(
                        "https://api.openai.com/v1/chat/completions",
                        prefs.getString("openai_api_key", ""),
                        readHeadersJson(null)
                );
            case "gemini":
                return new ProviderConfig(
                        "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                        prefs.getString("gemini_api_key", ""),
                        readHeadersJson(null)
                );
            case "groq":
                return new ProviderConfig(
                        "https://api.groq.com/openai/v1/chat/completions",
                        prefs.getString("groq_api_key", ""),
                        readHeadersJson(null)
                );
            case "deepseek":
                return new ProviderConfig(
                        "https://api.deepseek.com/chat/completions",
                        prefs.getString("deepseek_api_key", ""),
                        readHeadersJson(null)
                );
            case "openrouter":
                return new ProviderConfig(
                        "https://openrouter.ai/api/v1/chat/completions",
                        prefs.getString("openrouter_api_key", ""),
                        readHeadersJson("{\"HTTP-Referer\":\"https://github.com/FabioSilva11/Sketchware-IA\",\"X-Title\":\"Sketchware IA\"}")
                );
            case "openai_compatible":
                return new ProviderConfig(
                        normalizeChatCompletionsUrl(prefs.getString("openai_compatible_base_url", "")),
                        prefs.getString("openai_compatible_api_key", ""),
                        readHeadersJson(prefs.getString("openai_compatible_headers", "{}"))
                );
            case "grok_xai":
                return new ProviderConfig(
                        "https://api.x.ai/v1/chat/completions",
                        prefs.getString("grok_xai_api_key", ""),
                        readHeadersJson(null)
                );
            case "mistral":
                return new ProviderConfig(
                        "https://api.mistral.ai/v1/chat/completions",
                        prefs.getString("mistral_api_key", ""),
                        readHeadersJson(null)
                );
            case "litellm":
                return new ProviderConfig(
                        normalizeChatCompletionsUrl(prefs.getString("litellm_base_url", "")),
                        "",
                        readHeadersJson(null)
                );
            case "ollama":
                return new ProviderConfig(
                        normalizeOpenAiLocalUrl(prefs.getString("local_provider_ollama_url", "http://127.0.0.1:11434")),
                        "",
                        readHeadersJson(null)
                );
            case "vllm":
                return new ProviderConfig(
                        normalizeOpenAiLocalUrl(prefs.getString("local_provider_vllm_url", "http://localhost:8000")),
                        "",
                        readHeadersJson(null)
                );
            case "lm_studio":
                return new ProviderConfig(
                        normalizeOpenAiLocalUrl(prefs.getString("local_provider_lm_studio_url", "http://localhost:1234")),
                        "",
                        readHeadersJson(null)
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

    private static final class ProviderConfig {
        final String baseUrl;
        final String apiKey;
        final JSONObject extraHeaders;

        ProviderConfig(String baseUrl, String apiKey, JSONObject extraHeaders) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey == null ? "" : apiKey.trim();
            this.extraHeaders = extraHeaders == null ? new JSONObject() : extraHeaders;
        }
    }
}
