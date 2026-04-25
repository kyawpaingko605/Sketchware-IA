package pro.sketchware.network;

import android.content.Context;
import android.content.SharedPreferences;
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
import pro.sketchware.R;

/**
 * AiProviderService - Robust service for AI communication with Streaming support.
 * Inspired by Void Editor's streaming architecture.
 */
public class AiProviderService {
    private static final String TAG = "AiProviderService";
    private static AiProviderService instance;

    private final Context context;
    private final OkHttpClient client;

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

    public void sendStreamingMessage(String message, JSONArray tools, JSONArray chatHistory, String systemContext, StreamListener listener) {
        SharedPreferences prefs = context.getSharedPreferences("ia_settings", Context.MODE_PRIVATE);
        String currentProvider = prefs.getString("current_ai_provider", "groq");
        String currentModel = prefs.getString("current_ai_model", "llama-3.1-8b-instant");

        String baseUrl;
        String providerApiKey;
        boolean isEnabled;

        switch (currentProvider) {
            case "openai":
                baseUrl = "https://api.openai.com/v1/chat/completions";
                providerApiKey = prefs.getString("openai_api_key", "");
                isEnabled = prefs.getBoolean("openai_enabled", false);
                break;
            case "gemini":
                baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
                providerApiKey = prefs.getString("gemini_api_key", "");
                isEnabled = prefs.getBoolean("gemini_enabled", false);
                break;
            case "groq":
            default:
                baseUrl = "https://api.groq.com/openai/v1/chat/completions";
                providerApiKey = prefs.getString("groq_api_key", "");
                isEnabled = prefs.getBoolean("groq_enabled", false);
                break;
        }

        if (!isEnabled || providerApiKey.isEmpty()) {
            listener.onError("Provider not enabled or API key missing", null);
            return;
        }

        JSONArray messages = new JSONArray();
        try {
            // Build system prompt (Personality)
            messages.put(new JSONObject().put("role", "system").put("content", "You are May, a premium AI coding assistant for Sketchware. Be professional and useful."));
            
            // Add dynamic context
            if (systemContext != null && !systemContext.isEmpty()) {
                messages.put(new JSONObject().put("role", "system").put("content", "CONTEXT:\n" + systemContext));
            }

            // Add history
            if (chatHistory != null) {
                for (int i = 0; i < chatHistory.length(); i++) {
                    messages.put(chatHistory.get(i));
                }
            }

            // Only append a fresh user turn when the caller actually provided one.
            if (message != null && !message.trim().isEmpty()) {
                messages.put(new JSONObject().put("role", "user").put("content", message));
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", currentModel);
            jsonBody.put("messages", messages);
            jsonBody.put("stream", true); // Enable streaming!
            if (tools != null && tools.length() > 0) {
                jsonBody.put("tools", tools);
            }

            Request request = new Request.Builder()
                    .url(baseUrl)
                    .addHeader("Authorization", "Bearer " + providerApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    listener.onError(e.getMessage(), e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
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
                    }
                }
            });

        } catch (Exception e) {
            listener.onError("Request preparation error", e);
        }
    }
}
