package pro.sketchware.activities.chat.port;

import android.content.SharedPreferences;

import org.json.JSONObject;

public final class VoidPortLlmMessage {
    public enum ProviderFamily {
        OPENAI_COMPATIBLE,
        ANTHROPIC
    }

    public static final class ProviderConfig {
        public final ProviderFamily family;
        public final String baseUrl;
        public final String apiKey;
        public final JSONObject extraHeaders;
        public final boolean supportsNativeTools;

        public ProviderConfig(ProviderFamily family, String baseUrl, String apiKey, JSONObject extraHeaders, boolean supportsNativeTools) {
            this.family = family;
            this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
            this.apiKey = apiKey == null ? "" : apiKey.trim();
            this.extraHeaders = extraHeaders == null ? new JSONObject() : extraHeaders;
            this.supportsNativeTools = supportsNativeTools;
        }
    }

    private VoidPortLlmMessage() {
    }

    public static ProviderConfig resolveProviderConfig(SharedPreferences prefs, String providerId) {
        if (providerId == null) {
            return null;
        }
        return switch (providerId) {
            case "anthropic" -> new ProviderConfig(
                    ProviderFamily.ANTHROPIC,
                    "https://api.anthropic.com/v1/messages",
                    prefs.getString("anthropic_api_key", ""),
                    readHeadersJson(null),
                    true
            );
            case "openai" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    "https://api.openai.com/v1/chat/completions",
                    prefs.getString("openai_api_key", ""),
                    readHeadersJson(null),
                    true
            );
            case "gemini" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
                    prefs.getString("gemini_api_key", ""),
                    readHeadersJson(null),
                    true
            );
            case "groq" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    "https://api.groq.com/openai/v1/chat/completions",
                    prefs.getString("groq_api_key", ""),
                    readHeadersJson(null),
                    true
            );
            case "deepseek" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    "https://api.deepseek.com/chat/completions",
                    prefs.getString("deepseek_api_key", ""),
                    readHeadersJson(null),
                    true
            );
            case "openrouter" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    "https://openrouter.ai/api/v1/chat/completions",
                    prefs.getString("openrouter_api_key", ""),
                    readHeadersJson("{\"HTTP-Referer\":\"https://github.com/FabioSilva11/Sketchware-IA\",\"X-Title\":\"Sketchware IA\"}"),
                    true
            );
            case "openai_compatible" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    normalizeChatCompletionsUrl(prefs.getString("openai_compatible_base_url", "")),
                    prefs.getString("openai_compatible_api_key", ""),
                    readHeadersJson(prefs.getString("openai_compatible_headers", "{}")),
                    false
            );
            case "grok_xai" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    "https://api.x.ai/v1/chat/completions",
                    prefs.getString("grok_xai_api_key", ""),
                    readHeadersJson(null),
                    true
            );
            case "mistral" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    "https://api.mistral.ai/v1/chat/completions",
                    prefs.getString("mistral_api_key", ""),
                    readHeadersJson(null),
                    true
            );
            case "litellm" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    normalizeChatCompletionsUrl(prefs.getString("litellm_base_url", "")),
                    "",
                    readHeadersJson(null),
                    false
            );
            case "ollama" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    normalizeOllamaUrl(prefs.getString("ollama_base_url", "https://ollama.com/api")),
                    prefs.getString("ollama_api_key", ""),
                    readHeadersJson(null),
                    true
            );
            case "vllm" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    normalizeOpenAiLocalUrl(prefs.getString("local_provider_vllm_url", "http://localhost:8000")),
                    "",
                    readHeadersJson(null),
                    false
            );
            case "lm_studio" -> new ProviderConfig(
                    ProviderFamily.OPENAI_COMPATIBLE,
                    normalizeOpenAiLocalUrl(prefs.getString("local_provider_lm_studio_url", "http://localhost:1234")),
                    "",
                    readHeadersJson(null),
                    false
            );
            default -> null;
        };
    }

    public static int maxOutputTokens(String providerId, String modelName) {
        VoidPortModelCapabilities.Capabilities capabilities =
                VoidPortModelCapabilities.getModelCapabilities(providerId, modelName);
        boolean reasoningEnabled = capabilities.reasoningCapabilities.supportsReasoning
                && !capabilities.reasoningCapabilities.canTurnOffReasoning;
        return capabilities.effectiveReservedOutputTokenSpace(reasoningEnabled);
    }

    public static boolean shouldUseNativeTools(String providerId, String modelName, ProviderConfig providerConfig) {
        if (providerConfig == null || !providerConfig.supportsNativeTools) {
            return false;
        }
        if (prefersXmlToolProtocol(providerId)) {
            return false;
        }
        VoidPortModelCapabilities.ToolFormat toolFormat =
                VoidPortModelCapabilities.expectedToolFormat(providerId, modelName);
        if (providerConfig.family == ProviderFamily.ANTHROPIC) {
            return toolFormat == VoidPortModelCapabilities.ToolFormat.ANTHROPIC_STYLE;
        }
        return toolFormat == VoidPortModelCapabilities.ToolFormat.OPENAI_STYLE
                || toolFormat == VoidPortModelCapabilities.ToolFormat.GEMINI_STYLE;
    }

    public static boolean prefersXmlToolProtocol(String providerId) {
        if (providerId == null) {
            return false;
        }
        return "gemini".equals(providerId)
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

    public static JSONObject readHeadersJson(String raw) {
        try {
            return raw == null || raw.trim().isEmpty() ? new JSONObject() : new JSONObject(raw);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    public static String normalizeOpenAiLocalUrl(String baseUrl) {
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

    public static String normalizeOllamaUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        // Se for a URL padrão do Ollama Cloud (https://ollama.com/api), usar endpoint nativo
        if (trimmed.equals("https://ollama.com/api")) {
            return trimmed + "/chat";
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
        if (trimmed.endsWith("/api")) {
            return trimmed + "/chat";
        }
        if (trimmed.endsWith("/")) {
            return trimmed + "api/chat";
        }
        return trimmed + "/api/chat";
    }

    public static String normalizeChatCompletionsUrl(String baseUrl) {
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
}
