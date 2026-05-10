package pro.sketchware.activities.chat;

import android.content.Context;

import java.util.List;
import java.util.Locale;

import pro.sketchware.R;

public final class ChatToolActivitySummary {
    private ChatToolActivitySummary() {
    }

    public static final class Summary {
        public int readCount;
        public int editCount;
        public int searchCount;
        public int listCount;
        public int runCount;
        public int otherCount;
        public int runningCount;
        public int errorCount;

        public int total() {
            return readCount + editCount + searchCount + listCount + runCount + otherCount;
        }

        public String compactLabel(Context context) {
            StringBuilder builder = new StringBuilder();
            appendPart(context, builder, R.plurals.chat_tool_summary_reads, readCount);
            appendPart(context, builder, R.plurals.chat_tool_summary_edits, editCount);
            appendPart(context, builder, R.plurals.chat_tool_summary_searches, searchCount);
            appendPart(context, builder, R.plurals.chat_tool_summary_lists, listCount);
            appendPart(context, builder, R.plurals.chat_tool_summary_runs, runCount);
            appendPart(context, builder, R.plurals.chat_tool_summary_other, otherCount);
            if (builder.length() == 0) {
                builder.append(context.getString(R.string.chat_tool_summary_empty));
            }
            if (runningCount > 0) {
                builder.append(" | ")
                        .append(context.getResources().getQuantityString(
                                R.plurals.chat_tool_summary_running, runningCount, runningCount));
            }
            if (errorCount > 0) {
                builder.append(" | ")
                        .append(context.getResources().getQuantityString(
                                R.plurals.chat_tool_summary_errors, errorCount, errorCount));
            }
            return builder.toString();
        }

        private void appendPart(Context context, StringBuilder builder, int pluralRes, int count) {
            if (count <= 0) {
                return;
            }
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(context.getResources().getQuantityString(pluralRes, count, count));
        }
    }

    public static Summary summarize(List<ChatMessage> messages) {
        Summary summary = new Summary();
        if (messages == null) {
            return summary;
        }
        for (ChatMessage message : messages) {
            if (message == null || !message.isTool()) {
                continue;
            }
            switch (groupKey(message.getToolName())) {
                case "read" -> summary.readCount++;
                case "edit" -> summary.editCount++;
                case "search" -> summary.searchCount++;
                case "list" -> summary.listCount++;
                case "run" -> summary.runCount++;
                default -> summary.otherCount++;
            }
            if (message.isToolRunning()) {
                summary.runningCount++;
            }
            if (message.isToolError() || message.isRejected()) {
                summary.errorCount++;
            }
        }
        return summary;
    }

    public static String groupLabel(Context context, String toolName) {
        return switch (groupKey(toolName)) {
            case "read" -> context.getString(R.string.chat_tool_group_read);
            case "edit" -> context.getString(R.string.chat_tool_group_edit);
            case "search" -> context.getString(R.string.chat_tool_group_search);
            case "list" -> context.getString(R.string.chat_tool_group_list);
            case "run" -> context.getString(R.string.chat_tool_group_run);
            default -> context.getString(R.string.chat_tool_group_tool);
        };
    }

    private static String groupKey(String toolName) {
        String name = toolName == null ? "" : toolName.toLowerCase(Locale.US);
        if (name.contains("read") || name.contains("decrypt")) {
            return "read";
        }
        if (name.contains("write") || name.contains("edit") || name.contains("rewrite")
                || name.contains("create") || name.contains("delete") || name.contains("encrypt")) {
            return "edit";
        }
        if (name.contains("search") || name.contains("grep") || name.contains("find")) {
            return "search";
        }
        if (name.contains("list") || name.contains("glob") || name.contains("tree")) {
            return "list";
        }
        if (name.contains("run") || name.contains("terminal") || name.contains("command")) {
            return "run";
        }
        return "other";
    }
}
