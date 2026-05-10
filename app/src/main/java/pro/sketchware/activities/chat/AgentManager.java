package pro.sketchware.activities.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import pro.sketchware.R;
import pro.sketchware.activities.chat.port.VoidPortDiffService;
import pro.sketchware.activities.chat.port.VoidPortSettings;
import pro.sketchware.ia.tools.Tool;
import pro.sketchware.ia.tools.ToolManager;
import pro.sketchware.network.AiProviderService;
import pro.sketchware.util.SketchwareFileDecryptor;

/**
 * Orchestrates the chat loop, approval flow, checkpoints, diff previews and
 * cancellation of the active stream/tool execution.
 */
public class AgentManager {

    private static final int MAX_AGENT_STEPS = 50;
    private static final int MAX_LLM_RETRIES = 3;
    private static final int MAX_PREVIEW_LINES = 48;
    private static final long RETRY_DELAY_MS = 1500L;

    public enum State {
        IDLE,
        THINKING,
        AWAITING_APPROVAL,
        EXECUTING_TOOL,
        FINISHED,
        ERROR
    }

    private final Context context;
    private final String scId;
    private final List<ChatMessage> messages;
    private final AgentListener listener;
    private final AiProviderService aiService;
    private final ToolManager toolManager;
    private final Handler mainHandler;
    private final ChatCheckpointManager checkpointManager;

    private State currentState = State.IDLE;
    private ChatMessage pendingToolMessage;
    private ChatMessage currentStreamingMessage;
    private Thread currentToolThread;
    private int runVersion = 0;
    private int pendingToolLoopStep = -1;

    public interface AgentListener {
        void onMessageAdded(ChatMessage message);
        void onMessageUpdated(ChatMessage message);
        void onMessageRemoved(ChatMessage message, int index);
        void onStatusChanged(String status);
        void onDebug(String message);
        void onProcessingFinished();
        void onError(String error);
    }

    public AgentManager(Context context, String scId, List<ChatMessage> messages, AgentListener listener) {
        this.context = context.getApplicationContext();
        this.scId = scId;
        this.messages = messages;
        this.listener = listener;
        this.aiService = AiProviderService.getInstance();
        this.toolManager = new ToolManager();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.checkpointManager = new ChatCheckpointManager(context);
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean hasCheckpoint() {
        return checkpointManager.hasCheckpoint(scId);
    }

    public ChatCheckpointManager.RollbackResult rollbackLastCheckpoint() {
        return checkpointManager.rollbackLatestCheckpoint(scId);
    }

    private void setState(State state) {
        this.currentState = state;
        String statusText = "";
        switch (state) {
            case THINKING:
                statusText = getString(R.string.chat_status_thinking);
                break;
            case AWAITING_APPROVAL:
                statusText = getString(R.string.chat_tool_status_waiting_approval);
                break;
            case EXECUTING_TOOL:
                statusText = getString(R.string.chat_tool_status_running);
                break;
            case IDLE:
                statusText = "";
                break;
        }
        listener.onStatusChanged(statusText);
    }

    public void processUserMessage(String userText) {
        processUserMessage(userText, null);
    }

    public void processUserMessage(String userText, String contextPayload) {
        processUserMessage(userText, contextPayload, null);
    }

    public void processUserMessage(String userText, String contextPayload, List<ChatReference> imageReferences) {
        if (currentState != State.IDLE) {
            return;
        }

        ChatMessage userMsg = new ChatMessage(userText, true, System.currentTimeMillis());
        userMsg.setContextPayload(contextPayload);
        userMsg.setImageReferences(imageReferences);
        messages.add(userMsg);
        listener.onMessageAdded(userMsg);

        int version = ++runVersion;
        startAgentLoop(version, 0, 0);
    }

    public boolean cancelCurrentRun() {
        if (currentState == State.IDLE) {
            return false;
        }

        runVersion++;
        aiService.cancelCurrentStream();
        toolManager.cancelActiveTool();

        Thread toolThread = currentToolThread;
        if (toolThread != null) {
            toolThread.interrupt();
        }
        currentToolThread = null;

        mainHandler.post(() -> {
            if (pendingToolMessage != null) {
                pendingToolMessage.setToolRunning(false);
                pendingToolMessage.setToolError(true);
                if (currentState == State.AWAITING_APPROVAL) {
                    pendingToolMessage.setRejected(true);
                    pendingToolMessage.setStatus(getString(R.string.chat_tool_status_cancelled));
                    pendingToolMessage.setMessage(getString(R.string.chat_tool_cancelled_message));
                } else {
                    pendingToolMessage.setStatus(getString(R.string.chat_tool_status_cancelled));
                    pendingToolMessage.setMessage(getString(R.string.chat_tool_cancelled_message));
                }
                pendingToolMessage.setToolResult(getString(R.string.chat_tool_cancelled_message));
                listener.onMessageUpdated(pendingToolMessage);
            } else if (currentStreamingMessage != null) {
                if (!currentStreamingMessage.hasMessageContent()) {
                    currentStreamingMessage.setMessage(getString(R.string.chat_tool_cancelled_message));
                } else if (!currentStreamingMessage.getMessage().contains("[cancelado pelo usuário]")) {
                    currentStreamingMessage.setMessage(
                            currentStreamingMessage.getMessage().trim() + "\n\n[cancelado pelo usuário]");
                }
                currentStreamingMessage.setStatus(getString(R.string.chat_tool_status_cancelled));
                listener.onMessageUpdated(currentStreamingMessage);
            }

            finishProcessing();
        });
        return true;
    }

    private void startAgentLoop(final int version, final int loopStep, final int retryCount) {
        if (!isActiveRun(version)) {
            return;
        }
        if (loopStep >= MAX_AGENT_STEPS) {
            mainHandler.post(() -> {
                if (!isActiveRun(version)) {
                    return;
                }
                ChatMessage maxStepMsg = new ChatMessage(
                        "Stopped after " + MAX_AGENT_STEPS + " agent steps to avoid an endless loop. Ask me to continue if you want me to keep going.",
                        false,
                        System.currentTimeMillis()
                );
                messages.add(maxStepMsg);
                listener.onMessageAdded(maxStepMsg);
                finishProcessing();
            });
            return;
        }

        setState(State.THINKING);

        SharedPreferences prefs = AiChatSettingsHelper.prefs(pro.sketchware.SketchApplication.getContext());
        String chatMode = AiChatSettingsHelper.getChatMode(prefs);
        String providerId = prefs.getString(AiChatSettingsHelper.PREF_CURRENT_PROVIDER, "");

        ContextBuilder.Result contextResult = new ContextBuilder(scId, messages, toolManager)
                .build(findLatestUserMessage(), chatMode, providerId);
        JSONArray tools = toolManager.getToolsAsMCP(chatMode);
        final ChatMessage botMsg = createThinkingMessage();
        currentStreamingMessage = botMsg;

        aiService.sendStreamingMessage(contextResult, tools, chatMode,
                new AiProviderService.StreamListener() {
                    private final StringBuilder contentAccumulator = new StringBuilder();
                    private final StringBuilder reasoningAccumulator = new StringBuilder();
                    private String toolName = "";
                    private String toolArgs = "";
                    private String toolId = "";

                    @Override
                    public void onContent(String delta) {
                        if (!isActiveRun(version) || !ChatMessage.hasVisibleText(delta)) {
                            return;
                        }
                        contentAccumulator.append(delta);
                        mainHandler.post(() -> {
                            if (!isActiveRun(version)) {
                                return;
                            }
                            botMsg.setStatus("");
                            botMsg.setMessage(contentAccumulator.toString());
                            listener.onMessageUpdated(botMsg);
                        });
                    }

                    @Override
                    public void onReasoning(String delta) {
                        if (!isActiveRun(version) || !ChatMessage.hasVisibleText(delta)) {
                            return;
                        }
                        reasoningAccumulator.append(delta);
                        mainHandler.post(() -> {
                            if (!isActiveRun(version)) {
                                return;
                            }
                            botMsg.setReasoning(reasoningAccumulator.toString());
                            listener.onMessageUpdated(botMsg);
                        });
                    }

                    @Override
                    public void onToolCall(String name, String arguments, String id) {
                        if (!isActiveRun(version)) {
                            return;
                        }
                        if (ChatMessage.hasVisibleText(name)) {
                            toolName = name;
                        }
                        if (ChatMessage.hasVisibleText(arguments)) {
                            toolArgs += arguments;
                        }
                        if (ChatMessage.hasVisibleText(id)) {
                            toolId = id;
                        }
                    }

                    @Override
                    public void onDebug(String message) {
                        if (!isActiveRun(version) || !ChatMessage.hasVisibleText(message)) {
                            return;
                        }
                        mainHandler.post(() -> {
                            if (!isActiveRun(version)) {
                                return;
                            }
                            listener.onDebug(message);
                        });
                    }

                    @Override
                    public void onFinalMessage(String fullContent, String fullReasoning) {
                        if (!isActiveRun(version)) {
                            return;
                        }
                        mainHandler.post(() -> {
                            if (!isActiveRun(version)) {
                                return;
                            }

                            if (ChatMessage.hasVisibleText(fullContent)) {
                                botMsg.setMessage(fullContent);
                            }
                            if (ChatMessage.hasVisibleText(fullReasoning)) {
                                botMsg.setReasoning(fullReasoning);
                            }
                            botMsg.setStatus("");

                            boolean hasAssistantPayload = botMsg.hasMessageContent() || botMsg.hasReasoningContent();
                            if (ChatMessage.hasVisibleText(toolName)) {
                                if (hasAssistantPayload) {
                                    listener.onMessageUpdated(botMsg);
                                } else {
                                    removeStreamingPlaceholderIfEmpty(botMsg);
                                }
                                currentStreamingMessage = null;
                                handleToolCall(toolName, toolArgs, toolId, version, loopStep, chatMode);
                                return;
                            }

                            if (!hasAssistantPayload) {
                                removeStreamingPlaceholderIfEmpty(botMsg);
                            } else {
                                listener.onMessageUpdated(botMsg);
                            }
                            finishProcessing();
                        });
                    }

                    @Override
                    public void onError(String message, Throwable t) {
                        if (!isActiveRun(version) || "cancelled".equalsIgnoreCase(message)) {
                            return;
                        }
                        setState(State.ERROR);
                        mainHandler.post(() -> {
                            if (!isActiveRun(version)) {
                                return;
                            }
                            boolean canRetry = retryCount + 1 < MAX_LLM_RETRIES
                                    && !botMsg.hasMessageContent()
                                    && !botMsg.hasReasoningContent()
                                    && !ChatMessage.hasVisibleText(toolName);
                            if (canRetry) {
                                removeStreamingPlaceholderIfEmpty(botMsg);
                                mainHandler.postDelayed(() -> startAgentLoop(version, loopStep, retryCount + 1), RETRY_DELAY_MS);
                                return;
                            }
                            removeStreamingPlaceholderIfEmpty(botMsg);
                            listener.onError(message);
                            finishProcessing();
                        });
                    }
                });
    }

    private ChatMessage createThinkingMessage() {
        ChatMessage botMsg = new ChatMessage("", false, System.currentTimeMillis());
        botMsg.setStatus(getString(R.string.chat_status_thinking));
        messages.add(botMsg);
        listener.onMessageAdded(botMsg);
        return botMsg;
    }

    private void handleToolCall(String name, String args, String id, int version, int loopStep, String chatMode) {
        Tool tool = toolManager.getTool(name);
        if (tool == null || !toolManager.hasToolForChatMode(name, chatMode)) {
            addUnavailableToolMessage(name, args, id, chatMode, version);
            return;
        }

        boolean needsApproval = VoidPortSettings.requiresApproval(context, tool);

        ChatMessage toolMsg = new ChatMessage(name, args, System.currentTimeMillis(), id);
        toolMsg.setRequiresApproval(needsApproval);
        toolMsg.setStatus(needsApproval
                ? getString(R.string.chat_tool_status_waiting_approval)
                : getString(R.string.chat_tool_status_running));
        toolMsg.setMessage(needsApproval
                ? getString(R.string.chat_tool_approval_message_named, name)
                : getString(R.string.chat_tool_running_message));
        prepareToolPreview(toolMsg, tool);
        pendingToolMessage = toolMsg;
        pendingToolLoopStep = loopStep;

        mainHandler.post(() -> {
            if (!isActiveRun(version)) {
                return;
            }

            messages.add(toolMsg);
            listener.onMessageAdded(toolMsg);

            if (needsApproval) {
                setState(State.AWAITING_APPROVAL);
            } else {
                executeTool(toolMsg, version, loopStep);
            }
        });
    }

    private void addUnavailableToolMessage(String name, String args, String id, String chatMode, int version) {
        String safeName = name == null ? "" : name.trim();
        String mode = chatMode == null || chatMode.trim().isEmpty() ? "agent" : chatMode.trim();
        String availableTools = toolManager.getToolNamesForChatMode(mode);
        String result = getString(R.string.chat_tool_unavailable_result, safeName, mode);
        if (!availableTools.isEmpty()) {
            result += " " + getString(R.string.chat_tool_available_tools_result, availableTools);
        }

        ChatMessage toolMsg = new ChatMessage(safeName, args, System.currentTimeMillis(), id);
        toolMsg.setToolRunning(false);
        toolMsg.setToolError(true);
        toolMsg.setStatus(getString(R.string.chat_tool_status_error));
        toolMsg.setMessage(getString(R.string.chat_tool_error_message));
        toolMsg.setToolResult(result);
        pendingToolMessage = null;

        mainHandler.post(() -> {
            if (!isActiveRun(version)) {
                return;
            }
            messages.add(toolMsg);
            listener.onMessageAdded(toolMsg);
            finishProcessing();
        });
    }

    public void approveTool() {
        if (currentState != State.AWAITING_APPROVAL || pendingToolMessage == null) {
            return;
        }

        pendingToolMessage.setApproved(true);
        pendingToolMessage.setStatus(getString(R.string.chat_tool_status_approved));
        pendingToolMessage.setMessage(getString(R.string.chat_tool_approved_message));
        listener.onMessageUpdated(pendingToolMessage);
        executeTool(pendingToolMessage, runVersion, pendingToolLoopStep);
    }

    public void rejectTool() {
        if (currentState != State.AWAITING_APPROVAL || pendingToolMessage == null) {
            return;
        }

        pendingToolMessage.setRejected(true);
        pendingToolMessage.setToolRunning(false);
        pendingToolMessage.setToolError(true);
        pendingToolMessage.setStatus(getString(R.string.chat_tool_status_rejected));
        pendingToolMessage.setMessage(getString(R.string.chat_tool_rejected_message));
        pendingToolMessage.setToolResult(getString(R.string.chat_tool_rejected_message));
        listener.onMessageUpdated(pendingToolMessage);
        finishProcessing();
    }

    private void executeTool(final ChatMessage toolMsg, final int version, final int loopStep) {
        if (!isActiveRun(version)) {
            return;
        }

        setState(State.EXECUTING_TOOL);
        toolMsg.setStatus(getString(R.string.chat_tool_status_running));
        toolMsg.setMessage(getString(R.string.chat_tool_running_message));
        listener.onMessageUpdated(toolMsg);

        currentToolThread = new Thread(() -> {
            ChatCheckpointManager.CheckpointEntry checkpointEntry = createCheckpointIfNeeded(toolMsg);
            if (checkpointEntry != null) {
                mainHandler.post(() -> {
                    if (!isActiveRun(version)) {
                        return;
                    }
                    ChatMessage checkpointMsg = new ChatMessage(
                            getString(R.string.chat_checkpoint_saved_message, checkpointEntry.filePath),
                            ChatMessage.TYPE_CHECKPOINT,
                            System.currentTimeMillis(),
                            getString(R.string.chat_checkpoint_status)
                    );
                    messages.add(checkpointMsg);
                    listener.onMessageAdded(checkpointMsg);
                });
            }

            String result = toolManager.executeTool(scId, toolMsg.getToolName(), toolMsg.getToolArgs());
            boolean isError = looksLikeToolError(result);

            mainHandler.post(() -> {
                currentToolThread = null;
                if (!isActiveRun(version)) {
                    return;
                }

                toolMsg.setToolRunning(false);
                toolMsg.setToolError(isError);
                toolMsg.setToolResult(result);
                toolMsg.setStatus(getString(isError
                        ? R.string.chat_tool_status_error
                        : R.string.chat_tool_status_done));
                toolMsg.setMessage(getString(isError
                        ? R.string.chat_tool_error_message
                        : R.string.chat_tool_done_message));
                toolMsg.setExpanded(isError);
                listener.onMessageUpdated(toolMsg);

                clearPendingToolState();
                startAgentLoop(version, loopStep + 1, 0);
            });
        }, "chat-tool-worker");
        currentToolThread.start();
    }

    private void prepareToolPreview(ChatMessage toolMsg, Tool tool) {
        if (toolMsg == null || tool == null || !tool.isDestructive()) {
            return;
        }

        try {
            JSONObject args = parseToolArgs(toolMsg.getToolArgs());
            String filePath = normalizeSketchwarePath(args.optString("file_path", ""));
            String content = args.optString("content", "");
            if (content.isEmpty()) {
                content = args.optString("code_edit", "");
            }
            if (filePath.isEmpty() || content.isEmpty()) {
                return;
            }

            boolean existedBefore = SketchwareFileDecryptor.fileExists(scId, filePath);
            String beforeContent = existedBefore ? safe(SketchwareFileDecryptor.decryptFile(scId, filePath)) : "";
            String preview = buildVoidPreview(filePath, beforeContent, content, existedBefore);
            toolMsg.setToolResult(preview);
        } catch (Exception ignored) {
        }
    }

    private String buildVoidPreview(String filePath, String beforeContent, String generatedContent, boolean existedBefore) {
        String cleanedContent = extractRegularCode(generatedContent);
        List<ExtractCodeFromResult.ExtractedSearchReplaceBlock> blocks =
                ExtractCodeFromResult.extractSearchReplaceBlocks(cleanedContent);
        if (!blocks.isEmpty()) {
            return buildSearchReplacePreview(filePath, cleanedContent, blocks);
        }
        return buildWholeFilePreview(filePath, beforeContent, cleanedContent, existedBefore);
    }

    private String buildSearchReplacePreview(String filePath, String content,
                                             List<ExtractCodeFromResult.ExtractedSearchReplaceBlock> blocks) {
        String language = LanguageHelpers.detectLanguage(filePath, content);
        StringBuilder builder = new StringBuilder();
        builder.append("VOID SEARCH/REPLACE PREVIEW\n");
        builder.append("File: ").append(filePath).append("\n");
        builder.append("Language: ").append(language).append("\n");
        builder.append("Actions: ")
                .append(ActionIds.VOID_ACCEPT_DIFF_ACTION_ID)
                .append(" / ")
                .append(ActionIds.VOID_REJECT_DIFF_ACTION_ID)
                .append("\n\n");

        int printed = 0;
        for (int i = 0; i < blocks.size() && printed < MAX_PREVIEW_LINES; i++) {
            ExtractCodeFromResult.ExtractedSearchReplaceBlock block = blocks.get(i);
            builder.append("Block ").append(i + 1).append(" - ").append(block.state).append("\n");
            builder.append(PromptConstants.TRIPLE_TICK.get(0)).append(language).append("\n");
            builder.append(PromptConstants.ORIGINAL).append("\n");
            printed = appendPreviewLines(builder, block.orig, printed);
            builder.append(PromptConstants.DIVIDER).append("\n");
            printed = appendPreviewLines(builder, block.fin, printed);
            builder.append(PromptConstants.FINAL).append("\n");
            builder.append(PromptConstants.TRIPLE_TICK.get(1)).append("\n\n");
        }
        if (printed >= MAX_PREVIEW_LINES) {
            builder.append("... preview truncated ...\n");
        }
        return builder.toString().trim();
    }

    private String buildWholeFilePreview(String filePath, String beforeContent, String afterContent, boolean existedBefore) {
        String safeBefore = safe(beforeContent);
        String safeAfter = safe(afterContent);
        String language = LanguageHelpers.detectLanguage(filePath, safeAfter);
        List<VoidPortDiffService.ComputedDiff> diffs =
                VoidPortDiffService.findDiffs(safeBefore, safeAfter);

        StringBuilder builder = new StringBuilder();
        builder.append("VOID DIFF PREVIEW\n");
        builder.append("File: ").append(filePath).append("\n");
        builder.append("Mode: ").append(existedBefore ? "update" : "create").append("\n");
        builder.append("Language: ").append(language).append("\n");
        builder.append("Actions: ")
                .append(ActionIds.VOID_ACCEPT_FILE_ACTION_ID)
                .append(" / ")
                .append(ActionIds.VOID_REJECT_FILE_ACTION_ID)
                .append("\n\n");

        if (diffs.isEmpty()) {
            builder.append(getString(R.string.chat_diff_no_content_changes));
            return builder.toString();
        }

        int printed = 0;
        for (int i = 0; i < diffs.size() && printed < MAX_PREVIEW_LINES; i++) {
            VoidPortDiffService.ComputedDiff diff = diffs.get(i);
            builder.append("Change ")
                    .append(i + 1)
                    .append(" - ")
                    .append(diff.type)
                    .append(" original lines ")
                    .append(formatLineRange(diff.originalStartLine, diff.originalEndLine))
                    .append(" -> new lines ")
                    .append(formatLineRange(diff.startLine, diff.endLine))
                    .append("\n");
            builder.append(PromptConstants.TRIPLE_TICK.get(0)).append(language).append("\n");
            builder.append(PromptConstants.ORIGINAL).append("\n");
            printed = appendPreviewLines(builder, diff.originalCode, printed);
            builder.append(PromptConstants.DIVIDER).append("\n");
            printed = appendPreviewLines(builder, diff.code, printed);
            builder.append(PromptConstants.FINAL).append("\n");
            builder.append(PromptConstants.TRIPLE_TICK.get(1)).append("\n\n");
        }

        if (printed >= MAX_PREVIEW_LINES) {
            builder.append("... preview truncated ...\n");
        }
        return builder.toString().trim();
    }

    private String formatLineRange(int startLine, int endLine) {
        if (startLine <= 0 || endLine < startLine) {
            return "none";
        }
        if (startLine == endLine) {
            return String.valueOf(startLine);
        }
        return startLine + "-" + endLine;
    }

    private String extractRegularCode(String content) {
        ExtractCodeFromResult.Extraction extraction =
                ExtractCodeFromResult.extractCodeFromRegular(content, content == null ? 0 : content.length());
        return extraction.fullText;
    }

    private int appendPreviewLines(StringBuilder builder, String content, int printed) {
        return appendLineRange(builder, splitLines(safe(content)), 0, splitLines(safe(content)).length, printed);
    }

    private int appendLineRange(StringBuilder builder, String[] lines, int start, int end, int printed) {
        for (int i = start; i < end && printed < MAX_PREVIEW_LINES; i++) {
            builder.append(lines[i]).append("\n");
            printed++;
        }
        return printed;
    }

    private String[] splitLines(String content) {
        if (content == null || content.isEmpty()) {
            return new String[0];
        }
        return content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private ChatCheckpointManager.CheckpointEntry createCheckpointIfNeeded(ChatMessage toolMsg) {
        Tool tool = toolManager.getTool(toolMsg.getToolName());
        if (tool == null || !tool.isDestructive()) {
            return null;
        }

        try {
            JSONObject args = parseToolArgs(toolMsg.getToolArgs());
            String filePath = normalizeSketchwarePath(args.optString("file_path", ""));
            if (filePath.isEmpty()) {
                return null;
            }

            boolean existedBefore = SketchwareFileDecryptor.fileExists(scId, filePath);
            String beforeContent = existedBefore ? safe(SketchwareFileDecryptor.decryptFile(scId, filePath)) : "";
            return checkpointManager.createCheckpoint(
                    scId,
                    toolMsg.getToolId() != null ? toolMsg.getToolId() : "",
                    safe(toolMsg.getToolName()),
                    filePath,
                    beforeContent,
                    existedBefore
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private void clearPendingToolState() {
        pendingToolMessage = null;
        pendingToolLoopStep = -1;
    }

    private void finishProcessing() {
        clearPendingToolState();
        currentStreamingMessage = null;
        currentToolThread = null;
        setState(State.IDLE);
        listener.onProcessingFinished();
    }

    private void removeStreamingPlaceholderIfEmpty(ChatMessage botMsg) {
        if (botMsg == null) {
            return;
        }
        if (botMsg.hasMessageContent() || botMsg.hasReasoningContent()) {
            return;
        }
        removeMessage(botMsg);
    }

    private void removeMessage(ChatMessage message) {
        int index = messages.indexOf(message);
        if (index < 0) {
            return;
        }
        messages.remove(index);
        listener.onMessageRemoved(message, index);
    }

    private boolean looksLikeToolError(String result) {
        if (result == null) {
            return true;
        }
        String normalized = result.trim().toLowerCase();
        return normalized.startsWith("erro")
                || normalized.startsWith("falha")
                || normalized.contains("comando bloqueado")
                || normalized.contains("nao foi possivel")
                || normalized.contains("não foi possível")
                || normalized.contains("exception")
                || normalized.contains("api error")
                || normalized.contains("failed")
                || normalized.contains("unsupported")
                || normalized.contains("error:");
    }

    private boolean isActiveRun(int version) {
        return version == runVersion;
    }

    private String findLatestUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message != null && message.isUser() && message.getMessage() != null) {
                return message.getPromptContent();
            }
        }
        return "";
    }

    private JSONObject parseToolArgs(String toolArgs) {
        try {
            if (toolArgs == null || toolArgs.trim().isEmpty() || "null".equals(toolArgs.trim())) {
                return new JSONObject();
            }
            return new JSONObject(toolArgs);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private String normalizeSketchwarePath(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.replace("\\", "/").replace(".json", "");
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        normalized = normalized.replace("/sdcard/.sketchware/", "");
        normalized = normalized.replace("sdcard/.sketchware/", "");
        String prefix = "data/" + scId + "/";
        if (normalized.startsWith(prefix)) {
            normalized = normalized.substring(prefix.length());
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String getString(int resId) {
        return context.getString(resId);
    }

    private String getString(int resId, Object... args) {
        return context.getString(resId, args);
    }
}
