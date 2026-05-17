package pro.sketchware.network;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pro.sketchware.SketchApplication;
import pro.sketchware.activities.chat.port.VoidPortSettings;

/**
 * Morph Fast Apply API — merges {@code code_edit} snippets into file content.
 *
 * @see <a href="https://docs.morphllm.com/api-reference/endpoint/apply">Morph Apply API</a>
 */
public final class MorphApplyService {
    private static final String MORPH_API_URL = "https://api.morphllm.com/v1/chat/completions";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private MorphApplyService() {
    }

    public static boolean isConfigured(SharedPreferences prefs) {
        if (prefs == null) {
            return false;
        }
        if (!prefs.getBoolean("morph_enabled", true)) {
            return false;
        }
        return !prefs.getString("morph_api_key", "").trim().isEmpty();
    }

    public static boolean isConfigured() {
        Context context = SketchApplication.getContext();
        if (context == null) {
            return false;
        }
        return isConfigured(VoidPortSettings.prefs(context));
    }

    public static String apply(String originalCode, String instructions, String codeEdit) throws IOException {
        Context context = SketchApplication.getContext();
        if (context == null) {
            throw new IOException("Application context unavailable.");
        }
        return apply(VoidPortSettings.prefs(context), originalCode, instructions, codeEdit);
    }

    public static String apply(SharedPreferences prefs, String originalCode, String instructions, String codeEdit)
            throws IOException {
        if (!isConfigured(prefs)) {
            throw new IOException("Morph API key not configured. Add your key in IA settings.");
        }

        String apiKey = prefs.getString("morph_api_key", "").trim();
        String model = resolveModel(prefs);
        String safeInstructions = instructions == null || instructions.trim().isEmpty()
                ? "Apply the requested code edit to the file."
                : instructions.trim();
        String safeOriginal = originalCode == null ? "" : originalCode;
        String safeEdit = codeEdit == null ? "" : codeEdit;

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", buildMorphPayload(safeInstructions, safeOriginal, safeEdit));

        JSONArray messages = new JSONArray();
        messages.put(userMessage);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0);

        Request request = new Request.Builder()
                .url(MORPH_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Morph API HTTP " + response.code() + ": " + compactError(responseBody));
            }
            String merged = parseOpenAiContent(responseBody);
            if (merged == null || merged.trim().isEmpty()) {
                throw new IOException("Morph API returned empty merged content.");
            }
            return merged;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Morph apply failed: " + e.getMessage(), e);
        }
    }

    static String buildMorphPayload(String instructions, String originalCode, String codeEdit) {
        return "<instruction>" + instructions + "</instruction>\n"
                + "<code>" + originalCode + "</code>\n"
                + "<update>" + codeEdit + "</update>";
    }

    private static String resolveModel(SharedPreferences prefs) {
        String applyMode = prefs.getString(VoidPortSettings.PREF_APPLY_MODE, VoidPortSettings.APPLY_MODE_FAST);
        if (VoidPortSettings.APPLY_MODE_CAREFUL.equals(applyMode)) {
            return "morph-v3-large";
        }
        if (VoidPortSettings.APPLY_MODE_BALANCED.equals(applyMode)) {
            return "auto";
        }
        return "morph-v3-fast";
    }

    private static String parseOpenAiContent(String responseBody) throws Exception {
        JSONObject json = new JSONObject(responseBody);
        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return "";
        }
        JSONObject message = choices.optJSONObject(0).optJSONObject("message");
        if (message == null) {
            return "";
        }
        return message.optString("content", "");
    }

    private static String compactError(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "unknown error";
        }
        String trimmed = body.trim();
        if (trimmed.length() > 240) {
            return trimmed.substring(0, 240) + "...";
        }
        return trimmed;
    }
}
