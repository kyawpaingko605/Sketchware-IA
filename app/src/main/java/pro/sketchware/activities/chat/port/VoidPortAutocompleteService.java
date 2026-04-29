package pro.sketchware.activities.chat.port;

import android.content.SharedPreferences;

/**
 * Small Android helper for the useful, chat-safe parts of browser/autocompleteService.ts.
 */
public final class VoidPortAutocompleteService {
    private VoidPortAutocompleteService() {
    }

    public static boolean isEnabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(VoidPortSettings.PREF_ENABLE_AUTOCOMPLETE, false);
    }

    public static String sanitizeCompletion(String value) {
        if (value == null) {
            return "";
        }
        String result = stripCodeFence(value);
        boolean leadingSpace = result.startsWith(" ");
        boolean trailingSpace = result.endsWith(" ");
        result = result.trim();
        return (leadingSpace ? " " : "") + result + (trailingSpace ? " " : "");
    }

    public static String buildFimInstructionBlock(String prefix, String suffix) {
        return "Autocomplete/FIM context\n"
                + "<prefix>\n"
                + safe(prefix)
                + "\n</prefix>\n"
                + "<suffix>\n"
                + safe(suffix)
                + "\n</suffix>\n"
                + "Return only the missing middle code.";
    }

    public static String buildPromptSummary(SharedPreferences prefs) {
        if (!isEnabled(prefs)) {
            return "Autocomplete: disabled";
        }
        return "Autocomplete: enabled; use concise FIM-style middle-code completions when asked.";
    }

    private static String stripCodeFence(String value) {
        String text = value.trim();
        if (!text.startsWith("```")) {
            return value;
        }
        int firstNewLine = text.indexOf('\n');
        int lastFence = text.lastIndexOf("```");
        if (firstNewLine >= 0 && lastFence > firstNewLine) {
            return text.substring(firstNewLine + 1, lastFence);
        }
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
