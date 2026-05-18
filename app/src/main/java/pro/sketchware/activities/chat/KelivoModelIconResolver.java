package pro.sketchware.activities.chat;

import java.util.Locale;

import pro.sketchware.R;

public final class KelivoModelIconResolver {

    private KelivoModelIconResolver() {
    }

    public static int resolve(String providerId, String modelId) {
        String key = ((providerId == null ? "" : providerId)
                + " "
                + (modelId == null ? "" : modelId)).toLowerCase(Locale.US);
        if (containsAny(key, "openai", "gpt") || key.matches(".*\\bo[0-9].*")) {
            return R.drawable.ic_model_openai;
        }
        if (key.contains("gemini")) {
            return R.drawable.ic_model_gemini;
        }
        if (key.contains("google")) {
            return R.drawable.ic_model_google;
        }
        if (key.contains("claude")) {
            return R.drawable.ic_model_claude;
        }
        if (key.contains("anthropic")) {
            return R.drawable.ic_model_anthropic;
        }
        if (key.contains("deepseek")) {
            return R.drawable.ic_model_deepseek;
        }
        if (containsAny(key, "qwen", "qwq", "qvq")) {
            return R.drawable.ic_model_qwen;
        }
        if (containsAny(key, "grok", "xai")) {
            return R.drawable.ic_model_xai;
        }
        if (key.contains("openrouter")) {
            return R.drawable.ic_model_openrouter;
        }
        if (key.contains("mistral")) {
            return R.drawable.ic_model_mistral;
        }
        if (containsAny(key, "llama", "meta")) {
            return R.drawable.ic_model_meta;
        }
        if (key.contains("ollama")) {
            return R.drawable.ic_model_ollama;
        }
        if (key.contains("gemma")) {
            return R.drawable.ic_model_gemma;
        }
        if (key.contains("silicon")) {
            return R.drawable.ic_model_siliconflow;
        }
        if (containsAny(key, "kimi", "moonshot")) {
            return R.drawable.ic_model_kimi;
        }
        return 0;
    }

    public static String initial(String providerId, String modelId) {
        String source = ChatMessage.hasVisibleText(modelId) ? modelId : providerId;
        if (!ChatMessage.hasVisibleText(source)) {
            return "?";
        }
        return source.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
