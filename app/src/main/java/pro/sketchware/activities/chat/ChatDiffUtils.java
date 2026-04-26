package pro.sketchware.activities.chat;

public final class ChatDiffUtils {
    private static final int MAX_BODY_LINES = 48;

    private ChatDiffUtils() {
    }

    public static String buildPreview(String filePath, String beforeContent, String afterContent, boolean existedBefore) {
        String safeBefore = beforeContent == null ? "" : beforeContent;
        String safeAfter = afterContent == null ? "" : afterContent;

        String[] beforeLines = splitLines(safeBefore);
        String[] afterLines = splitLines(safeAfter);

        int prefix = 0;
        int maxPrefix = Math.min(beforeLines.length, afterLines.length);
        while (prefix < maxPrefix && beforeLines[prefix].equals(afterLines[prefix])) {
            prefix++;
        }

        int suffix = 0;
        while (suffix < beforeLines.length - prefix
                && suffix < afterLines.length - prefix
                && beforeLines[beforeLines.length - 1 - suffix].equals(afterLines[afterLines.length - 1 - suffix])) {
            suffix++;
        }

        int removedStart = prefix;
        int removedEnd = Math.max(prefix, beforeLines.length - suffix);
        int addedStart = prefix;
        int addedEnd = Math.max(prefix, afterLines.length - suffix);

        StringBuilder builder = new StringBuilder();
        builder.append("DIFF PREVIEW\n");
        builder.append("File: ").append(filePath).append("\n");
        builder.append("Mode: ").append(existedBefore ? "update" : "create").append("\n");
        builder.append("Removed lines: ").append(Math.max(0, removedEnd - removedStart)).append("\n");
        builder.append("Added lines: ").append(Math.max(0, addedEnd - addedStart)).append("\n\n");

        if (safeBefore.equals(safeAfter)) {
            builder.append("No content changes detected.");
            return builder.toString();
        }

        builder.append("@@ -").append(prefix + 1).append(",").append(Math.max(0, removedEnd - removedStart))
                .append(" +").append(prefix + 1).append(",").append(Math.max(0, addedEnd - addedStart)).append(" @@\n");

        int printed = 0;
        printed = appendContext(builder, beforeLines, Math.max(0, prefix - 2), prefix, " ", printed);
        printed = appendRange(builder, beforeLines, removedStart, removedEnd, "-", printed);
        printed = appendRange(builder, afterLines, addedStart, addedEnd, "+", printed);
        appendContext(builder, afterLines, addedEnd, Math.min(afterLines.length, addedEnd + 2), " ", printed);

        if (printed >= MAX_BODY_LINES) {
            builder.append("... diff truncated ...\n");
        }

        return builder.toString().trim();
    }

    private static int appendContext(StringBuilder builder, String[] lines, int start, int end, String prefix, int printed) {
        return appendRange(builder, lines, start, end, prefix, printed);
    }

    private static int appendRange(StringBuilder builder, String[] lines, int start, int end, String prefix, int printed) {
        for (int i = start; i < end && printed < MAX_BODY_LINES; i++) {
            builder.append(prefix).append(lines[i]).append("\n");
            printed++;
        }
        return printed;
    }

    private static String[] splitLines(String content) {
        if (content == null || content.isEmpty()) {
            return new String[0];
        }
        return content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }
}
