package pro.sketchware.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pro.sketchware.R;

public class MorphClient {
    private static final String TAG = "MorphClient";
    private static final String BASE_URL = "https://api.morphllm.com/v1/chat/completions";
    private static final String MODEL = "morph-v3-fast";

    private static MorphClient instance;

    private final Context context;
    private final OkHttpClient client;
    private String apiKey;

    private MorphClient() {
        this.context = pro.sketchware.SketchApplication.getContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static synchronized MorphClient getInstance() {
        if (instance == null) {
            instance = new MorphClient();
        }
        return instance;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private String getDeviceLanguage() {
        try {
            Resources resources = context.getResources();
            Configuration configuration = resources.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return configuration.getLocales().get(0).getLanguage();
            } else {
                //noinspection deprecation
                return configuration.locale.getLanguage();
            }
        } catch (Throwable t) {
            return Locale.getDefault().getLanguage();
        }
    }

    private void navigateToSettings() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            try {
                Toast.makeText(context, context.getString(R.string.morph_api_key_not_configured_message), Toast.LENGTH_SHORT).show();
                android.content.Intent intent = new android.content.Intent(context, pro.sketchware.activities.settings.IaSettingsActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open AI settings", e);
            }
        });
    }

    public String sendMessage(String message) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences("ia_settings", Context.MODE_PRIVATE);
        boolean morphEnabled = prefs.getBoolean("morph_enabled", false);
        if (!morphEnabled) {
            navigateToSettings();
            throw new IOException(context.getString(R.string.morph_api_key_not_configured_title));
        }

        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = prefs.getString("morph_api_key", "");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            navigateToSettings();
            throw new IOException(context.getString(R.string.morph_api_key_not_configured_title));
        }

        String deviceLanguage = getDeviceLanguage();
        String systemPrompt = String.format("You are an expert Android developer assistant specialized in analyzing code errors and providing fixes. Always respond in language: %s. When asked to provide fixes in JSON format, return ONLY valid JSON without markdown code blocks.", deviceLanguage);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", MODEL);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.put(new JSONObject().put("role", "user").put("content", message));
            jsonBody.put("messages", messages);
            jsonBody.put("temperature", 0.7);
            jsonBody.put("max_tokens", 4000);
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON for request", e);
            throw new IOException("Error preparing request", e);
        }

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .build();

        int maxRetries = 3;
        long backoffMs = 1000L;

        IOException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if (attempt < maxRetries && (code == 429 || (code >= 500 && code < 600))) {
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ignored) { }
                        backoffMs *= 2;
                        continue;
                    }
                    throw new IOException("Request error: " + code + " - " + (response.body() != null ? response.body().string() : ""));
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray choices = jsonResponse.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    Log.e(TAG, "Response missing choices: " + responseBody);
                    throw new IOException("Invalid response from Morph API");
                }
                JSONObject firstChoice = choices.getJSONObject(0);
                String content = null;
                if (firstChoice.has("message")) {
                    content = firstChoice.getJSONObject("message").optString("content", null);
                }
                if (content == null) {
                    content = firstChoice.optString("text", null);
                }
                if (content == null) {
                    Log.e(TAG, "Could not extract content: " + responseBody);
                    throw new IOException("AI response content is empty");
                }
                return content;
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ignored) { }
                    backoffMs *= 2;
                    continue;
                }
                Log.e(TAG, "Error processing response", e);
                throw e;
            } catch (Exception e) {
                Log.e(TAG, "Error processing response", e);
                throw new IOException("Error processing API response", e);
            }
        }

        if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException("Unknown error contacting Morph API");
        }
    }

    /**
     * Aplica edição de código usando o formato específico da Morph API
     * @param initialCode Código inicial
     * @param codeEdit Edição proposta (pode conter // ...existing code...)
     * @param instructions Instruções sobre o que fazer
     * @return Código editado e mesclado
     * @throws IOException Se houver erro na requisição
     */
    public String applyCodeEdit(String initialCode, String codeEdit, String instructions) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences("ia_settings", Context.MODE_PRIVATE);
        boolean morphEnabled = prefs.getBoolean("morph_enabled", false);
        if (!morphEnabled) {
            navigateToSettings();
            throw new IOException(context.getString(R.string.morph_api_key_not_configured_title));
        }

        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = prefs.getString("morph_api_key", "");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            navigateToSettings();
            throw new IOException(context.getString(R.string.morph_api_key_not_configured_title));
        }

        // Formatar mensagem no formato específico da Morph
        String content = String.format("<instruction>%s</instruction>\n<code>%s</code>\n<update>%s</update>",
                instructions, initialCode, codeEdit);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", MODEL);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "user").put("content", content));
            jsonBody.put("messages", messages);
            jsonBody.put("temperature", 0.7);
            jsonBody.put("max_tokens", 4000);
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON for code edit request", e);
            throw new IOException("Error preparing code edit request", e);
        }

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .build();

        int maxRetries = 3;
        long backoffMs = 1000L;

        IOException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    int code = response.code();
                    if (attempt < maxRetries && (code == 429 || (code >= 500 && code < 600))) {
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ignored) { }
                        backoffMs *= 2;
                        continue;
                    }
                    throw new IOException("Request error: " + code + " - " + (response.body() != null ? response.body().string() : ""));
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray choices = jsonResponse.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    Log.e(TAG, "Response missing choices: " + responseBody);
                    throw new IOException("Invalid response from Morph API");
                }
                JSONObject firstChoice = choices.getJSONObject(0);
                String mergedCode = null;
                if (firstChoice.has("message")) {
                    mergedCode = firstChoice.getJSONObject("message").optString("content", null);
                }
                if (mergedCode == null) {
                    mergedCode = firstChoice.optString("text", null);
                }
                if (mergedCode == null) {
                    Log.e(TAG, "Could not extract merged code: " + responseBody);
                    throw new IOException("AI response content is empty");
                }
                return mergedCode;
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ignored) { }
                    backoffMs *= 2;
                    continue;
                }
                Log.e(TAG, "Error processing code edit response", e);
                throw e;
            } catch (Exception e) {
                Log.e(TAG, "Error processing code edit response", e);
                throw new IOException("Error processing API response", e);
            }
        }

        if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException("Unknown error contacting Morph API");
        }
    }
}

