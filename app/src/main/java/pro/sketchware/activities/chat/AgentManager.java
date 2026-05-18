package pro.sketchware.activities.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.annotation.Nullable;
import android.os.Looper;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import pro.sketchware.R;
import pro.sketchware.activities.chat.port.VoidToolWrapper;
import pro.sketchware.activities.chat.port.VoidPortDiffService;
import pro.sketchware.activities.chat.port.VoidPortMcpChannel;
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

    /** Matches Void {@code CHAT_RETRIES} / {@code RETRY_DELAY} in chatThreadService.ts */
    private static final int MAX_LLM_RETRIES = 3;
    private static final int MAX_PREVIEW_LINES = 48;
    private static final long RETRY_DELAY_MS = 2500L;
    private static final long STREAM_COALESCE_MS = 120L;

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
    private final Handler streamCoalesceHandler;
    private final ChatCheckpointManager checkpointManager;

    private State currentState = State.IDLE;
    private ChatMessage pendingToolMessage;
    private ChatMessage currentStreamingMessage;
    private Thread currentToolThread;
    private int runVersion = 0;
    private int pendingToolLoopStep = -1;
    private String lastToolCallSignature = "";
    private int repeatedToolCallCount = 0;
    private ChatInteractionTrace interactionTrace;
    private ChatMessage pendingStreamMessage;
    private boolean streamUpdateScheduled;
    private String streamingToolName = "";
    private String streamingToolId = "";
    private String streamingMcpServerName;

    public interface AgentListener {
        void onMessageAdded(ChatMessage message);
        void onMessageUpdated(ChatMessage message);
        void onMessageRemoved(ChatMessage message, int index);
        void onStatusChanged(String status);
        void onDebug(String message);
        void onProcessingFinished();
        void onToolExecuted(String toolName, boolean isMutation);
        void onError(String error);
    }

    public AgentManager(Context context, String scId, List<ChatMessage> messages, AgentListener listener) {
        this.context = context.getApplicationContext();
        this.scId = scId;
        this.messages = messages;
        this.listener = listener;
        this.aiService = AiProviderService.getInstance();
        
        this.toolManager = new ToolManager();
        VoidToolWrapper.registerAllVoidTools(this.toolManager);

        this.mainHandler = new Handler(Looper.getMainLooper());
        this.streamCoalesceHandler = new Handler(Looper.getMainLooper());
        this.checkpointManager = new ChatCheckpointManager(context);
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean hasCheckpoint() {
        return checkpointManager.hasCheckpoint(scId);
    }

    public ChatCheckpointManager.RollbackResult rollbackLastCheckpoint() {
        return checkpointManager.rollbackLatestCheckpoint(scId, messages);
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

    public void processUserMessage(String userText, String contextPayload, List<ChatReference> stagingSelections) {
        if (currentState != State.IDLE) {
            return;
        }

        String displayText = userText == null ? "" : userText.trim();
        ChatMessage userMsg = new ChatMessage(displayText, true, System.currentTimeMillis());
        userMsg.setContextPayload(contextPayload);
        userMsg.setStagingSelections(stagingSelections);
        userMsg.setLlmContent(ChatReferenceManager.buildLlmUserContent(displayText, contextPayload));
        messages.add(userMsg);
        listener.onMessageAdded(userMsg);

        int version = ++runVersion;
        beginInteractionTrace(version, displayText, stagingSelections);
        startAgentLoop(version, 0, 0);
    }

    public void continueFromExistingMessage(@Nullable ChatMessage sourceMessage) {
        if (currentState != State.IDLE) {
            return;
        }
        int version = ++runVersion;
        String displayText = sourceMessage == null ? findLatestUserMessage() : sourceMessage.getDisplayContent();
        List<ChatReference> selections = sourceMessage == null ? null : sourceMessage.getStagingSelections();
        beginInteractionTrace(version, displayText, selections);
        startAgentLoop(version, 0, 0);
    }

    public boolean cancelCurrentRun() {
        if (currentState == State.IDLE) {
            return false;
        }

        runVersion++;
        aiService.cancelCurrentStream();
        toolManager.cancelActiveTool();
        streamCoalesceHandler.removeCallbacksAndMessages(null);
        streamUpdateScheduled = false;
        pendingStreamMessage = null;

        Thread toolThread = currentToolThread;
        if (toolThread != null) {
            toolThread.interrupt();
        }
        currentToolThread = null;

        final String interruptedToolName = streamingToolName;
        final String interruptedMcpServer = streamingMcpServerName;
        final boolean hadPendingTool = pendingToolMessage != null;
        final ChatMessage streamingSnapshot = currentStreamingMessage;

        mainHandler.post(() -> {
            if (ChatMessage.hasVisibleText(interruptedToolName)) {
                ChatMessage interrupted = ChatMessage.interruptedStreamingTool(
                        interruptedToolName,
                        interruptedMcpServer,
                        System.currentTimeMillis()
                );
                messages.add(interrupted);
                listener.onMessageAdded(interrupted);
            } else if (pendingToolMessage != null) {
                pendingToolMessage.setToolRunning(false);
                pendingToolMessage.setToolError(true);
                if (currentState == State.AWAITING_APPROVAL) {
                    pendingToolMessage.setToolState("rejected");
                    pendingToolMessage.setRejected(true);
                    pendingToolMessage.setStatus(getString(R.string.chat_tool_status_cancelled));
                    pendingToolMessage.setDisplayContent(getString(R.string.chat_tool_cancelled_message));
                } else {
                    pendingToolMessage.setStatus(getString(R.string.chat_tool_status_cancelled));
                    pendingToolMessage.setDisplayContent(getString(R.string.chat_tool_cancelled_message));
                }
                pendingToolMessage.setToolResult(getString(R.string.chat_tool_cancelled_message));
                listener.onMessageUpdated(pendingToolMessage);
            } else if (streamingSnapshot != null) {
                if (!streamingSnapshot.hasDisplayContent()) {
                    streamingSnapshot.setDisplayContent(getString(R.string.chat_tool_cancelled_message));
                } else if (!streamingSnapshot.getDisplayContent().contains(getString(R.string.chat_cancelled_suffix))) {
                    streamingSnapshot.setDisplayContent(
                            streamingSnapshot.getDisplayContent().trim()
                                    + "\n\n"
                                    + getString(R.string.chat_cancelled_suffix));
                }
                streamingSnapshot.setStatus(getString(R.string.chat_tool_status_cancelled));
                listener.onMessageUpdated(streamingSnapshot);
            }

            if (!hadPendingTool && !ChatMessage.hasVisibleText(interruptedToolName)) {
                // Void adds a user checkpoint after abort when no tool approval is pending.
            }

            clearStreamingToolState();
            finishProcessing();
        });
        return true;
    }

    private void startAgentLoop(final int version, final int loopStep, final int retryCount) {
        if (!isActiveRun(version)) {
            return;
        }
        setState(State.THINKING);
        emitTrace("Agent loop", "step=" + loopStep + ", retry=" + retryCount);

        SharedPreferences prefs = AiChatSettingsHelper.prefs(pro.sketchware.SketchApplication.getContext());
        String chatMode = AiChatSettingsHelper.getChatMode(prefs);
        String providerId = prefs.getString(AiChatSettingsHelper.PREF_CURRENT_PROVIDER, "");

        long contextStartedAt = SystemClock.elapsedRealtime();
        ContextBuilder.Result contextResult = new ContextBuilder(scId, messages, toolManager)
                .build(findLatestUserMessage(), chatMode, providerId);
        long contextMs = SystemClock.elapsedRealtime() - contextStartedAt;
        JSONArray tools = toolManager.getToolsAsMCP(chatMode);
        if ("agent".equalsIgnoreCase(chatMode)) {
            appendMcpTools(tools, VoidPortMcpChannel.getToolsAsMCP(prefs));
        }
        emitTrace(
                "Contexto montado",
                "build=" + contextMs + "ms, msgs=" + messages.size()
                        + ", tools=" + (tools == null ? 0 : tools.length())
                        + ", mode=" + chatMode
                        + ", provider=" + providerId
        );
        final ChatMessage botMsg = createThinkingMessage();
        currentStreamingMessage = botMsg;
        clearStreamingToolState();

        emitTrace("Chamada LLM iniciada");
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
                        botMsg.setStatus("");
                        botMsg.setDisplayContent(contentAccumulator.toString());
                        scheduleStreamUpdate(version, botMsg);
                    }

                    @Override
                    public void onReasoning(String delta) {
                        if (!isActiveRun(version) || !ChatMessage.hasVisibleText(delta)) {
                            return;
                        }
                        reasoningAccumulator.append(delta);
                        botMsg.setReasoning(reasoningAccumulator.toString());
                        scheduleStreamUpdate(version, botMsg);
                    }

                    @Override
                    public void onToolCall(String name, String arguments, String id) {
                        if (!isActiveRun(version)) {
                            return;
                        }
                        if (ChatMessage.hasVisibleText(name)) {
                            toolName = name;
                            streamingToolName = name;
                            streamingMcpServerName = resolveMcpServerName(name);
                        }
                        if (ChatMessage.hasVisibleText(arguments)) {
                            toolArgs = arguments;
                        }
                        if (ChatMessage.hasVisibleText(id)) {
                            toolId = id;
                            streamingToolId = id;
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

                            flushStreamUpdate(version);

                            if (ChatMessage.hasVisibleText(fullContent)) {
                                botMsg.setDisplayContent(fullContent);
                            }
                            if (ChatMessage.hasVisibleText(fullReasoning)) {
                                botMsg.setReasoning(fullReasoning);
                            }
                            botMsg.setStatus("");

                            boolean hasAssistantPayload = botMsg.hasDisplayContent() || botMsg.hasReasoningContent();
                            if (ChatMessage.hasVisibleText(toolName)) {
                                if (hasAssistantPayload) {
                                    listener.onMessageUpdated(botMsg);
                                } else {
                                    removeStreamingPlaceholderIfEmpty(botMsg);
                                }
                                currentStreamingMessage = null;
                                emitTrace("LLM pediu ferramenta", "tool=" + toolName);
                                clearStreamingToolState();
                                handleToolCall(toolName, toolArgs, toolId, version, loopStep, chatMode);
                                return;
                            }

                            clearStreamingToolState();
                            if (!hasAssistantPayload) {
                                removeStreamingPlaceholderIfEmpty(botMsg);
                            } else {
                                listener.onMessageUpdated(botMsg);
                            }
                            emitTraceSummary("resposta final sem ferramenta");
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
                                    && !botMsg.hasDisplayContent()
                                    && !botMsg.hasReasoningContent()
                                    && !ChatMessage.hasVisibleText(toolName);
                            if (canRetry) {
                                removeStreamingPlaceholderIfEmpty(botMsg);
                                mainHandler.postDelayed(() -> startAgentLoop(version, loopStep, retryCount + 1), RETRY_DELAY_MS);
                                return;
                            }
                            removeStreamingPlaceholderIfEmpty(botMsg);
                            emitTrace("Erro LLM", message);
                            listener.onError(message);
                            emitTraceSummary("erro");
                            finishProcessing();
                        });
                    }
                });
    }

    private ChatMessage createThinkingMessage() {
        ChatMessage botMsg = new ChatMessage("", false, System.currentTimeMillis());
        botMsg.setStatus("");
        botMsg.setStreaming(true);
        messages.add(botMsg);
        listener.onMessageAdded(botMsg);
        return botMsg;
    }

    private void handleToolCall(String name, String args, String id, int version, int loopStep, String chatMode) {
        // Anti-loop detection
        String signature = name + ":" + args;
        if (signature.equals(lastToolCallSignature)) {
            repeatedToolCallCount++;
        } else {
            lastToolCallSignature = signature;
            repeatedToolCallCount = 0;
        }

        if (repeatedToolCallCount >= 3) {
            String advice = getString(R.string.chat_tool_loop_detected);
            if ("get_file".equals(name)) {
                advice += " " + getString(R.string.chat_tool_loop_use_read_file);
            } else if ("run_command".equals(name) || "run_persistent_command".equals(name)) {
                advice += " " + getString(R.string.chat_tool_loop_use_edit_tools);
            } else {
                advice += " " + getString(R.string.chat_tool_loop_try_different);
            }
            addUnavailableToolMessage(name, args, id, chatMode, version, loopStep, advice);
            return;
        }

        if ("get_file".equals(name)) {
            addUnavailableToolMessage(name, args, id, chatMode, version, loopStep,
                    "Erro: ferramenta 'get_file' não existe. Use 'read_file' para ler arquivos.");
            return;
        }

        Tool tool = toolManager.getTool(name);
        boolean mcpTool = tool == null && isMcpToolAvailable(name, chatMode);
        if ((!mcpTool && tool == null) || (!mcpTool && !toolManager.hasToolForChatMode(name, chatMode))) {
            addUnavailableToolMessage(name, args, id, chatMode, version, loopStep, null);
            return;
        }

        boolean needsApproval = mcpTool
                ? !VoidPortSettings.isAutoApprovalEnabled(
                        VoidPortSettings.prefs(context),
                        VoidPortSettings.APPROVAL_MCP_TOOLS)
                : VoidPortSettings.requiresApproval(context, tool);

        ChatMessage toolMsg = new ChatMessage(name, args, System.currentTimeMillis(), id);
        toolMsg.setToolState(needsApproval ? "tool_request" : "running_now");
        toolMsg.setRequiresApproval(needsApproval);
        toolMsg.setStatus(needsApproval
                ? getString(R.string.chat_tool_status_waiting_approval)
                : getString(R.string.chat_tool_status_running));
        toolMsg.setDisplayContent(needsApproval
                ? getString(R.string.chat_tool_approval_message_named, name)
                : getString(R.string.chat_tool_running_message));
        toolMsg.setMcpServerName(mcpTool ? resolveMcpServerName(name) : null);
        if (!mcpTool) {
            prepareToolPreview(toolMsg, tool);
        }
        pendingToolMessage = toolMsg;
        pendingToolLoopStep = loopStep;

        mainHandler.post(() -> {
            if (!isActiveRun(version)) {
                return;
            }

            messages.add(toolMsg);
            listener.onMessageAdded(toolMsg);
            emitTrace("Ferramenta na fila", "name=" + name + ", approval=" + needsApproval);

            if (needsApproval) {
                setState(State.AWAITING_APPROVAL);
            } else {
                executeTool(toolMsg, version, loopStep);
            }
        });
    }

    private void addUnavailableToolMessage(String name, String args, String id, String chatMode, int version, int loopStep, String customError) {
        String safeName = name == null ? "" : name.trim();
        String mode = chatMode == null || chatMode.trim().isEmpty() ? "agent" : chatMode.trim();
        String availableTools = toolManager.getToolNamesForChatMode(mode);
        String result = (customError != null) ? customError : "Erro: ferramenta '" + safeName + "' nao esta disponivel no modo '" + mode + "'.";
        if (!availableTools.isEmpty()) {
            result += " Ferramentas disponiveis: " + availableTools + ".";
        }

        ChatMessage toolMsg = new ChatMessage(safeName, args, System.currentTimeMillis(), id);
        toolMsg.setToolRunning(false);
        toolMsg.setToolError(true);
        toolMsg.setToolState("error");
        toolMsg.setStatus(getString(R.string.chat_tool_status_error));
        toolMsg.setDisplayContent(getString(R.string.chat_tool_error_message));
        toolMsg.setToolResult(result);
        pendingToolMessage = null;

        mainHandler.post(() -> {
            if (!isActiveRun(version)) {
                return;
            }
            messages.add(toolMsg);
            listener.onMessageAdded(toolMsg);
            startAgentLoop(version, loopStep + 1, 0);
        });
    }

    public void approveTool() {
        if (currentState != State.AWAITING_APPROVAL || pendingToolMessage == null) {
            return;
        }

        pendingToolMessage.setApproved(true);
        pendingToolMessage.setToolState("running_now");
        pendingToolMessage.setStatus(getString(R.string.chat_tool_status_approved));
        pendingToolMessage.setDisplayContent(getString(R.string.chat_tool_approved_message));
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
        pendingToolMessage.setToolState("rejected");
        pendingToolMessage.setStatus(getString(R.string.chat_tool_status_rejected));
        pendingToolMessage.setDisplayContent(getString(R.string.chat_tool_rejected_message));
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
        toolMsg.setDisplayContent(getString(R.string.chat_tool_running_message));
        listener.onMessageUpdated(toolMsg);

        emitTrace("Ferramenta iniciada", "name=" + toolMsg.getToolName());
        final long toolStartedAt = SystemClock.elapsedRealtime();
        currentToolThread = new Thread(() -> {
            ChatCheckpointManager.CheckpointEntry checkpointEntry = createCheckpointIfNeeded(toolMsg);
            if (checkpointEntry != null) {
                mainHandler.post(() -> {
                    if (!isActiveRun(version)) {
                        return;
                    }
                    ChatMessage checkpointMsg = checkpointEntry.toChatMessage();
                    messages.add(checkpointMsg);
                    listener.onMessageAdded(checkpointMsg);
                });
            }

            String result = executeToolCall(toolMsg);
            boolean isError = looksLikeToolError(result);
            final long toolDurationMs = SystemClock.elapsedRealtime() - toolStartedAt;

            mainHandler.post(() -> {
                currentToolThread = null;
                if (!isActiveRun(version)) {
                    return;
                }
                emitTrace(
                        "Ferramenta concluída",
                        "name=" + toolMsg.getToolName()
                                + ", ok=" + !isError
                                + ", duration=" + toolDurationMs + "ms"
                                + ", resultChars=" + (result == null ? 0 : result.length())
                );

                toolMsg.setToolRunning(false);
                toolMsg.setToolError(isError);
                toolMsg.setToolState(isError ? "error" : "success");
                toolMsg.setToolResult(result);
                toolMsg.setStatus(getString(isError
                        ? R.string.chat_tool_status_error
                        : R.string.chat_tool_status_done));
                toolMsg.setDisplayContent(getString(isError
                        ? R.string.chat_tool_error_message
                        : R.string.chat_tool_done_message));
                toolMsg.setExpanded(isError);
                listener.onMessageUpdated(toolMsg);

                if (!isError) {
                    String toolName = toolMsg.getToolName();
                    boolean isMutation = "rewrite_file".equals(toolName) ||
                            "edit_file".equals(toolName) ||
                            "create_file_or_folder".equals(toolName) ||
                            "delete_file_or_folder".equals(toolName);
                    listener.onToolExecuted(toolName, isMutation);
                }

                clearPendingToolState();
                startAgentLoop(version, loopStep + 1, 0);
            });
        }, "chat-tool-worker");
        currentToolThread.start();
    }

    private String executeToolCall(ChatMessage toolMsg) {
        String toolName = toolMsg.getToolName();
        if (toolName != null && toolName.startsWith("mcp_")) {
            return VoidPortMcpChannel.callTool(
                    VoidPortSettings.prefs(context),
                    toolName,
                    parseToolArgs(toolMsg.getToolArgs())
            );
        }
        return toolManager.executeTool(scId, toolName, toolMsg.getToolArgs());
    }

    private void appendMcpTools(JSONArray target, JSONArray mcpTools) {
        if (target == null || mcpTools == null || mcpTools.length() == 0) {
            return;
        }
        for (int i = 0; i < mcpTools.length(); i++) {
            JSONObject tool = mcpTools.optJSONObject(i);
            if (tool != null) {
                target.put(tool);
            }
        }
    }

    private boolean isMcpToolAvailable(String name, String chatMode) {
        if (!"agent".equalsIgnoreCase(chatMode) || name == null || !name.startsWith("mcp_")) {
            return false;
        }
        JSONArray mcpTools = VoidPortMcpChannel.getToolsAsMCP(VoidPortSettings.prefs(context));
        for (int i = 0; i < mcpTools.length(); i++) {
            JSONObject tool = mcpTools.optJSONObject(i);
            JSONObject function = tool == null ? null : tool.optJSONObject("function");
            if (function != null && name.equals(function.optString("name", ""))) {
                return true;
            }
        }
        return false;
    }

    private void prepareToolPreview(ChatMessage toolMsg, Tool tool) {
        if (toolMsg == null || tool == null || !tool.isDestructive()) {
            return;
        }

        try {
            JSONObject args = parseToolArgs(toolMsg.getToolArgs());
            String filePath = normalizeToolPath(toolPathArg(args));
            String content = args.optString("new_content", "");
            if (content.isEmpty()) {
                content = args.optString("search_replace_blocks", "");
            }
            if (content.isEmpty()) {
                content = args.optString("content", "");
            }
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
            builder.append("No content changes detected.");
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
        if (tool == null || (!tool.isDestructive() && !tool.isFileMutation())) {
            return null;
        }

        try {
            JSONObject args = parseToolArgs(toolMsg.getToolArgs());
            String filePath = normalizeToolPath(toolPathArg(args));
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

    private void scheduleStreamUpdate(int version, ChatMessage message) {
        if (!isActiveRun(version) || message == null) {
            return;
        }
        pendingStreamMessage = message;
        if (streamUpdateScheduled) {
            return;
        }
        streamUpdateScheduled = true;
        streamCoalesceHandler.postDelayed(() -> {
            streamUpdateScheduled = false;
            flushStreamUpdate(version);
        }, STREAM_COALESCE_MS);
    }

    private void flushStreamUpdate(int version) {
        if (!isActiveRun(version)) {
            return;
        }
        ChatMessage message = pendingStreamMessage;
        pendingStreamMessage = null;
        if (message != null) {
            listener.onMessageUpdated(message);
        }
    }

    private void clearStreamingToolState() {
        streamingToolName = "";
        streamingToolId = "";
        streamingMcpServerName = null;
    }

    @Nullable
    private String resolveMcpServerName(String toolName) {
        if (toolName == null || !toolName.startsWith("mcp_")) {
            return null;
        }
        SharedPreferences prefs = VoidPortSettings.prefs(context);
        return VoidPortMcpChannel.resolveServerNameForTool(prefs, toolName);
    }

    private void finishProcessing() {
        streamCoalesceHandler.removeCallbacksAndMessages(null);
        streamUpdateScheduled = false;
        pendingStreamMessage = null;
        clearPendingToolState();
        clearStreamingToolState();
        currentStreamingMessage = null;
        currentToolThread = null;
        setState(State.IDLE);
        if (interactionTrace != null) {
            emitTraceSummary("processamento concluído");
        }
        listener.onProcessingFinished();
    }

    private void beginInteractionTrace(int version, String userText, List<ChatReference> stagingSelections) {
        interactionTrace = new ChatInteractionTrace(version);
        int textChars = userText == null ? 0 : userText.trim().length();
        int selectionCount = stagingSelections == null ? 0 : stagingSelections.size();
        int imageCount = stagingSelections == null ? 0 : ChatReferenceManager.getImageReferences(stagingSelections).size();
        emitTrace("Interação iniciada", "textChars=" + textChars + ", selections=" + selectionCount + ", images=" + imageCount);
    }

    private void emitTrace(String event) {
        emitTrace(event, null);
    }

    private void emitTrace(String event, String detail) {
        if (interactionTrace == null) {
            return;
        }
        String line = interactionTrace.mark(event, detail);
        if (ChatMessage.hasVisibleText(line)) {
            listener.onDebug(line);
        }
    }

    private void emitTraceSummary(String label) {
        if (interactionTrace == null) {
            return;
        }
        String line = interactionTrace.summary(label);
        interactionTrace = null;
        if (ChatMessage.hasVisibleText(line)) {
            listener.onDebug(line);
        }
    }

    private void removeStreamingPlaceholderIfEmpty(ChatMessage botMsg) {
        if (botMsg == null) {
            return;
        }
        if (botMsg.hasDisplayContent() || botMsg.hasReasoningContent()) {
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
        if (result == null) return true;
        String r = result.trim();
        if (r.isEmpty() || r.equals("{}")) return false;

        String lower = r.toLowerCase();
        if (lower.startsWith("error") || lower.startsWith("erro")) return true;
        if (lower.contains("exception")) return true;
        if (lower.contains("\"error\"") || lower.contains("'error'")) return true;
        if (lower.startsWith("blocked:") || lower.startsWith("comando bloqueado")) return true;

        return false;
    }

    private boolean isActiveRun(int version) {
        return version == runVersion;
    }

    private String findLatestUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message != null && message.isUser()) {
                return message.getLlmContent();
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

    private String toolPathArg(JSONObject args) {
        if (args == null) {
            return "";
        }
        String uri = args.optString("uri", "");
        if (!uri.trim().isEmpty()) {
            return uri;
        }
        return args.optString("file_path", "");
    }

    private String normalizeToolPath(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.trim().replace("\\", "/");
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
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
