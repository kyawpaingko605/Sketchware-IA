package pro.sketchware.activities.chat;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AiChatSettingsHelper {

    public static final String PREFS_NAME = "ia_settings";
    public static final String PREF_CURRENT_PROVIDER = "current_ai_provider";
    public static final String PREF_CURRENT_MODEL = "current_ai_model";
    public static final String PREF_CUSTOM_MODELS = "custom_models_json";
    public static final String PREF_CHAT_MODE = "chat_mode";

    private AiChatSettingsHelper() {
    }

    public static final class ModelOption {
        public final String providerId;
        public final String providerLabel;
        public final String model;

        public ModelOption(String providerId, String providerLabel, String model) {
            this.providerId = providerId;
            this.providerLabel = providerLabel;
            this.model = model;
        }

        public String getDisplayLabel() {
            return providerLabel + " • " + model;
        }
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getChatMode(SharedPreferences prefs) {
        String mode = prefs.getString(PREF_CHAT_MODE, "agent");
        if ("normal".equals(mode) || "gather".equals(mode) || "agent".equals(mode)) {
            return mode;
        }
        return "agent";
    }

    public static void setChatMode(SharedPreferences prefs, String mode) {
        prefs.edit().putString(PREF_CHAT_MODE, mode).apply();
    }

    public static List<ModelOption> getVisibleModelOptions(SharedPreferences prefs) {
        List<ModelOption> options = new ArrayList<>();
        for (ProviderGroup group : getAllProviderGroups(prefs)) {
            if (!isProviderSupportedInChat(group.providerId) || !isProviderConfigured(prefs, group.providerId)) {
                continue;
            }
            for (String model : group.models) {
                if (!isModelHidden(prefs, group.providerId, model)) {
                    options.add(new ModelOption(group.providerId, group.label, model));
                }
            }
        }
        return options;
    }

    public static void ensureValidCurrentSelection(SharedPreferences prefs) {
        String provider = prefs.getString(PREF_CURRENT_PROVIDER, "");
        String model = prefs.getString(PREF_CURRENT_MODEL, "");
        if (isCurrentSelectionValid(prefs, provider, model)) {
            return;
        }

        List<ModelOption> options = getVisibleModelOptions(prefs);
        if (options.isEmpty()) {
            return;
        }

        ModelOption first = options.get(0);
        prefs.edit()
                .putString(PREF_CURRENT_PROVIDER, first.providerId)
                .putString(PREF_CURRENT_MODEL, first.model)
                .apply();
    }

    public static boolean isCurrentSelectionValid(SharedPreferences prefs, String providerId, String model) {
        if (providerId == null || model == null || providerId.trim().isEmpty() || model.trim().isEmpty()) {
            return false;
        }
        for (ModelOption option : getVisibleModelOptions(prefs)) {
            if (providerId.equals(option.providerId) && model.equals(option.model)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isProviderSupportedInChat(String providerId) {
        return "anthropic".equals(providerId)
                || "openai".equals(providerId)
                || "gemini".equals(providerId)
                || "groq".equals(providerId)
                || "deepseek".equals(providerId)
                || "openrouter".equals(providerId)
                || "grok_xai".equals(providerId)
                || "mistral".equals(providerId)
                || "openai_compatible".equals(providerId)
                || "litellm".equals(providerId)
                || "ollama".equals(providerId)
                || "vllm".equals(providerId)
                || "lm_studio".equals(providerId);
    }

    public static boolean isProviderConfigured(SharedPreferences prefs, String providerId) {
        return switch (providerId) {
            case "ollama" -> !getPreferenceValue(prefs, "local_provider_ollama_url", "http://127.0.0.1:11434").isEmpty();
            case "vllm" -> !getPreferenceValue(prefs, "local_provider_vllm_url", "http://localhost:8000").isEmpty();
            case "lm_studio" -> !getPreferenceValue(prefs, "local_provider_lm_studio_url", "http://localhost:1234").isEmpty();
            case "anthropic" -> !getPreferenceValue(prefs, "anthropic_api_key", "").isEmpty();
            case "openai" -> !getPreferenceValue(prefs, "openai_api_key", "").isEmpty();
            case "deepseek" -> !getPreferenceValue(prefs, "deepseek_api_key", "").isEmpty();
            case "openrouter" -> !getPreferenceValue(prefs, "openrouter_api_key", "").isEmpty();
            case "openai_compatible" -> !getPreferenceValue(prefs, "openai_compatible_base_url", "").isEmpty();
            case "gemini" -> !getPreferenceValue(prefs, "gemini_api_key", "").isEmpty();
            case "groq" -> !getPreferenceValue(prefs, "groq_api_key", "").isEmpty();
            case "grok_xai" -> !getPreferenceValue(prefs, "grok_xai_api_key", "").isEmpty();
            case "mistral" -> !getPreferenceValue(prefs, "mistral_api_key", "").isEmpty();
            case "litellm" -> !getPreferenceValue(prefs, "litellm_base_url", "").isEmpty();
            default -> true;
        };
    }

    private static String getPreferenceValue(SharedPreferences prefs, String key, String defaultValue) {
        return prefs.getString(key, defaultValue).trim();
    }

    private static boolean isModelHidden(SharedPreferences prefs, String providerId, String model) {
        return prefs.getBoolean(modelHiddenKey(providerId, model), false);
    }

    private static String modelHiddenKey(String providerId, String model) {
        return "model_hidden_" + slugify(providerId) + "_" + slugify(model);
    }

    private static String slugify(String value) {
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_");
    }

    private static JSONArray readJsonArrayPreference(SharedPreferences prefs, String key) {
        String raw = prefs.getString(key, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private static List<ProviderGroup> getAllProviderGroups(SharedPreferences prefs) {
        List<ProviderGroup> groups = getCatalogProviderGroups();
        JSONArray array = readJsonArrayPreference(prefs, PREF_CUSTOM_MODELS);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String providerId = item.optString("providerId", "custom");
            String providerLabel = item.optString("providerLabel", "Custom");
            String model = item.optString("model", "");
            if (model.isEmpty()) {
                continue;
            }
            ProviderGroup target = null;
            for (ProviderGroup group : groups) {
                if (group.providerId.equals(providerId)) {
                    target = group;
                    break;
                }
            }
            if (target == null) {
                target = new ProviderGroup(providerId, providerLabel, new ArrayList<>());
                groups.add(target);
            }
            if (!target.models.contains(model)) {
                target.models.add(model);
            }
        }
        return groups;
    }

    @NonNull
    private static List<ProviderGroup> getCatalogProviderGroups() {
        List<ProviderGroup> groups = new ArrayList<>();
        groups.add(new ProviderGroup("ollama", "Ollama", new ArrayList<>(List.of("qwen3.5:397b-cloud"))));
        groups.add(new ProviderGroup("vllm", "vLLM", new ArrayList<>()));
        groups.add(new ProviderGroup("lm_studio", "LM Studio", new ArrayList<>()));
        groups.add(new ProviderGroup("anthropic", "Anthropic", new ArrayList<>(List.of(
                "claude-opus-4-0",
                "claude-sonnet-4-0",
                "claude-3-7-sonnet-latest",
                "claude-3-5-sonnet-latest",
                "claude-3-5-haiku-latest",
                "claude-3-opus-latest"
        ))));
        groups.add(new ProviderGroup("openai", "OpenAI", new ArrayList<>(List.of(
                "gpt-4.1",
                "gpt-4.1-mini",
                "gpt-4.1-nano",
                "o3",
                "o4-mini"
        ))));
        groups.add(new ProviderGroup("deepseek", "DeepSeek", new ArrayList<>(List.of(
                "deepseek-chat",
                "deepseek-reasoner"
        ))));
        groups.add(new ProviderGroup("openrouter", "OpenRouter", new ArrayList<>(List.of(
                "anthropic/claude-opus-4",
                "anthropic/claude-sonnet-4",
                "qwen/qwen3-235b-a22b",
                "anthropic/claude-3.7-sonnet",
                "anthropic/claude-3.5-sonnet",
                "deepseek/deepseek-r1",
                "deepseek/deepseek-r1-zero:free",
                "mistralai/devstral-small:free"
        ))));
        groups.add(new ProviderGroup("openai_compatible", "OpenAI-Compatible", new ArrayList<>()));
        groups.add(new ProviderGroup("gemini", "Gemini", new ArrayList<>(List.of(
                "gemini-2.5-pro-exp-03-25",
                "gemini-2.5-flash-preview-04-17",
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite",
                "gemini-2.5-pro-preview-05-06"
        ))));
        groups.add(new ProviderGroup("groq", "Groq", new ArrayList<>(List.of(
                "qwen-qwq-32b",
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant"
        ))));
        groups.add(new ProviderGroup("grok_xai", "Grok (xAI)", new ArrayList<>(List.of(
                "grok-2",
                "grok-3",
                "grok-3-mini",
                "grok-3-fast",
                "grok-3-mini-fast"
        ))));
        groups.add(new ProviderGroup("mistral", "Mistral", new ArrayList<>(List.of(
                "codestral-latest",
                "devstral-small-latest",
                "mistral-large-latest",
                "mistral-medium-latest",
                "ministral-3b-latest",
                "ministral-8b-latest"
        ))));
        groups.add(new ProviderGroup("litellm", "LiteLLM", new ArrayList<>()));
        return groups;
    }

    private static final class ProviderGroup {
        final String providerId;
        final String label;
        final List<String> models;

        ProviderGroup(String providerId, String label, List<String> models) {
            this.providerId = providerId;
            this.label = label;
            this.models = models;
        }
    }
}
