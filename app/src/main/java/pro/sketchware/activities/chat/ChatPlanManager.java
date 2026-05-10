package pro.sketchware.activities.chat;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import pro.sketchware.R;
import pro.sketchware.activities.chat.port.VoidPortScmService;

public final class ChatPlanManager {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_DONE = 2;

    private ChatPlanManager() {
    }

    public static final class Task {
        public final String title;
        public final String detail;
        public final int status;

        public Task(String title, String detail, int status) {
            this.title = title == null ? "" : title;
            this.detail = detail == null ? "" : detail;
            this.status = status;
        }
    }

    public static List<Task> buildPlan(Context context, String scId, List<ChatMessage> messages,
                                       boolean processing, String statusText) {
        List<Task> tasks = new ArrayList<>();
        int latestUserIndex = latestUserIndex(messages);
        if (latestUserIndex < 0) {
            tasks.add(new Task(context.getString(R.string.chat_plan_task_request),
                    context.getString(R.string.chat_plan_request_empty), STATUS_PENDING));
            tasks.add(new Task(context.getString(R.string.chat_plan_task_context),
                    context.getString(R.string.chat_plan_context_empty), STATUS_PENDING));
            return tasks;
        }

        int toolCount = 0;
        int runningTools = 0;
        int errorTools = 0;
        boolean hasAssistantAfterUser = false;
        for (int i = latestUserIndex + 1; messages != null && i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message == null) {
                continue;
            }
            if (message.isTool()) {
                toolCount++;
                if (message.isToolRunning()) {
                    runningTools++;
                }
                if (message.isToolError() || message.isRejected()) {
                    errorTools++;
                }
            }
            if (message.isBot() && (message.hasMessageContent() || message.hasReasoningContent())) {
                hasAssistantAfterUser = true;
            }
        }

        int changedFiles = VoidPortScmService.changedFileCount(scId);
        String safeStatus = statusText == null ? "" : statusText.trim();
        tasks.add(new Task(context.getString(R.string.chat_plan_task_request),
                compactUserText(context, messages.get(latestUserIndex)), STATUS_DONE));
        tasks.add(new Task(context.getString(R.string.chat_plan_task_context), toolCount > 0
                ? context.getString(R.string.chat_plan_context_tools_used, toolCount)
                : context.getString(R.string.chat_plan_context_waiting),
                toolCount > 0 ? STATUS_DONE : (processing ? STATUS_RUNNING : STATUS_PENDING)));
        tasks.add(new Task(context.getString(R.string.chat_plan_task_tools), toolCount > 0
                ? ChatToolActivitySummary.summarize(messages).compactLabel(context)
                : context.getString(R.string.chat_plan_tools_empty),
                runningTools > 0 ? STATUS_RUNNING : (toolCount > 0 ? STATUS_DONE : STATUS_PENDING)));
        tasks.add(new Task(context.getString(R.string.chat_plan_task_artifacts), changedFiles > 0
                ? context.getResources().getQuantityString(R.plurals.chat_inline_artifacts_files, changedFiles, changedFiles)
                : context.getString(R.string.chat_plan_artifacts_empty),
                changedFiles > 0 ? STATUS_DONE : (processing ? STATUS_RUNNING : STATUS_PENDING)));
        tasks.add(new Task(context.getString(R.string.chat_plan_task_finish), errorTools > 0
                ? context.getResources().getQuantityString(R.plurals.chat_plan_tools_errors, errorTools, errorTools)
                : (hasAssistantAfterUser ? context.getString(R.string.chat_plan_finish_answer_ready)
                : (safeStatus.isEmpty() ? context.getString(R.string.chat_plan_finish_waiting) : safeStatus)),
                processing ? STATUS_RUNNING : (hasAssistantAfterUser ? STATUS_DONE : STATUS_PENDING)));
        return tasks;
    }

    private static int latestUserIndex(List<ChatMessage> messages) {
        if (messages == null) {
            return -1;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message != null && message.isUser()) {
                return i;
            }
        }
        return -1;
    }

    private static String compactUserText(Context context, ChatMessage message) {
        String text = message == null ? "" : message.getMessage();
        if (!ChatMessage.hasVisibleText(text)) {
            return context.getString(R.string.chat_plan_request_references_only);
        }
        String trimmed = text.trim().replace('\n', ' ');
        return trimmed.length() > 120 ? trimmed.substring(0, 120) + "..." : trimmed;
    }
}
