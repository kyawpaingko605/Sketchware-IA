package pro.sketchware.activities.chat.port;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.util.CompileErrorCapture;

/**
 * Android diagnostic summary inspired by browser/_markerCheckService.ts.
 */
public final class VoidPortMarkerCheckService {
    private static final int MAX_ERRORS = 8;

    private VoidPortMarkerCheckService() {
    }

    public static class LintError {
        public final String code;
        public final String message;
        public final int startLineNumber;
        public final int endLineNumber;

        public LintError(String code, String message, int startLineNumber, int endLineNumber) {
            this.code = code != null ? code : "";
            this.message = message != null ? message : "";
            this.startLineNumber = startLineNumber;
            this.endLineNumber = endLineNumber;
        }
    }

    public static boolean hasErrors(String scId) {
        return CompileErrorCapture.hasCompileErrors(scId);
    }

    public static String buildErrorContext(String scId) {
        if (!hasErrors(scId)) {
            return "";
        }
        String raw = CompileErrorCapture.getLastCompileErrors(scId);
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }

        String summary = CompileErrorCapture.extractErrorSummary(raw).trim();
        if (summary.isEmpty()) {
            summary = raw.trim();
        }
        String compact = limitLines(summary, MAX_ERRORS);
        return "Recent diagnostics:\n" + compact;
    }

    public static List<String> topErrors(String scId) {
        String context = buildErrorContext(scId);
        List<String> errors = new ArrayList<>();
        if (context.isEmpty()) {
            return errors;
        }
        String[] lines = context.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && errors.size() < MAX_ERRORS) {
                errors.add(trimmed);
            }
        }
        return errors;
    }

    private static String limitLines(String text, int maxLines) {
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length && i < maxLines; i++) {
            builder.append(lines[i]).append('\n');
        }
        if (lines.length > maxLines) {
            builder.append("... diagnostics truncated ...\n");
        }
        return builder.toString().trim();
    }

    public static List<LintError> getLintErrors(String scId, String filePath) {
        List<LintError> errors = new ArrayList<>();
        
        String raw = CompileErrorCapture.getLastCompileErrors(scId);
        if (raw == null || raw.trim().isEmpty()) {
            return errors;
        }

        // Parse error messages to extract line numbers and messages
        Pattern pattern = Pattern.compile("(?i)(?:error|warning|erro|aviso)[^:]*:\\s*([^\\n]+)");
        Matcher matcher = pattern.matcher(raw);
        
        while (matcher.find() && errors.size() < MAX_ERRORS) {
            String message = matcher.group(1).trim();
            
            // Try to extract line number from the error message
            Pattern linePattern = Pattern.compile(":\\s*(\\d+)");
            Matcher lineMatcher = linePattern.matcher(matcher.group(0));
            int lineNumber = 1;
            if (lineMatcher.find()) {
                try {
                    lineNumber = Integer.parseInt(lineMatcher.group(1));
                } catch (NumberFormatException e) {
                    lineNumber = 1;
                }
            }
            
            errors.add(new LintError("compile_error", message, lineNumber, lineNumber));
        }

        return errors;
    }
}
