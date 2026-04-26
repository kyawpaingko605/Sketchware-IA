package pro.sketchware.activities.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import pro.sketchware.ia.tools.Tool;
import pro.sketchware.ia.tools.ToolManager;
import pro.sketchware.network.AiProviderService;
import pro.sketchware.util.SketchwareFileDecryptor;

/**
 * Orchestrates the chat loop, approval flow, checkpoints, diff previews and
 * cancellation of the active stream/tool execution.
 */
public class AgentManager {

    public enum State {
        IDLE,
        THINKING,
        AWAITING_APPROVAL,
        EXECUTING_TOOL,
        FINISHED,
        ERROR
    }

    private final String scId;
    private final List<ChatMessage> messages;
    private final AgentListener listener;
    private final AiProviderService aiService;
    private final ToolManager toolManager;
    private final Handler mainHandler;
    private final ChatCheckpointManager checkpointManager;

    private State currentState = State.IDLE;
    private ChatMessage pendingToolMessage;
    private ChatMessage pendingAwaitingMessage;
    private ChatMessage currentStreamingMessage;
    private Thread currentToolThread;
    private int runVersion = 0;

    public interface AgentListener {
        void onMessageAdded(ChatMessage message);
        void onMessageUpdated(ChatMessage message);
        void onStatusChanged(String status);
        void onProcessingFinished();
        void onError(String error);
    }

    public AgentManager(Context context, String scId, List<ChatMessage> messages, AgentListener listener) {
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
                statusText = "Pensando...";
                break;
            case AWAITING_APPROVAL:
                statusText = "Aguardando aprovacao...";
                break;
            case EXECUTING_TOOL:
                statusText = "Executando acao...";
                break;
            case IDLE:
                statusText = "";
                break;
        }
        listener.onStatusChanged(statusText);
    }

    public void processUserMessage(String userText) {
        if (currentState != State.IDLE) {
            return;
        }

        ChatMessage userMsg = new ChatMessage(userText, true, System.currentTimeMillis());
        messages.add(userMsg);
        listener.onMessageAdded(userMsg);

        int version = ++runVersion;
        startAgentLoop(version);
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
                }
                pendingToolMessage.setToolResult("Execucao cancelada manualmente pelo usuario.");
                listener.onMessageUpdated(pendingToolMessage);
            } else if (currentStreamingMessage != null) {
                String text = currentStreamingMessage.getMessage();
                if (text == null || text.trim().isEmpty()) {
                    currentStreamingMessage.setMessage("Execucao cancelada manualmente pelo usuario.");
                } else if (!text.contains("[cancelado pelo usuario]")) {
                    currentStreamingMessage.setMessage(text.trim() + "\n\n[cancelado pelo usuario]");
                }
                listener.onMessageUpdated(currentStreamingMessage);
            }

            updateAwaitingMessage("Cancelado", "A execucao em andamento foi cancelada pelo usuario.");
            finishProcessing();
        });
        return true;
    }

    private void startAgentLoop(final int version) {
        if (!isActiveRun(version)) {
            return;
        }

        setState(State.THINKING);

        ContextBuilder.Result contextResult = new ContextBuilder(scId, messages).build(findLatestUserMessage());
        JSONArray tools = toolManager.getToolsAsMCP();
        final ChatMessage botMsg = new ChatMessage("", false, System.currentTimeMillis());
        final boolean[] botMsgAdded = {false};
        currentStreamingMessage = botMsg;

        aiService.sendStreamingMessage("", tools, contextResult.getHistory(), contextResult.getSystemContext(),
                new AiProviderService.StreamListener() {
                    private final StringBuilder contentAccumulator = new StringBuilder();
                    private final StringBuilder reasoningAccumulator = new StringBuilder();
                    private String toolName = "";
                    private String toolArgs = "";
                    private String toolId = "";

                    private void ensureBotMessageVisible() {
                        if (botMsgAdded[0] || !isActiveRun(version)) {
                            return;
                        }
                        botMsgAdded[0] = true;
                        messages.add(botMsg);
                        listener.onMessageAdded(botMsg);
                    }

                    @Override
                    public void onContent(String delta) {
                        if (!isActiveRun(version)) {
                            return;
                        }
                        contentAccumulator.append(delta);
                        mainHandler.post(() -> {
                            if (!isActiveRun(version)) {
                                return;
                            }
                            ensureBotMessageVisible();
                            botMsg.setMessage(contentAccumulator.toString());
                            listener.onMessageUpdated(botMsg);
                        });
                    }

                    @Override
                    public void onReasoning(String delta) {
                        if (!isActiveRun(version)) {
                            return;
                        }
                        reasoningAccumulator.append(delta);
                        mainHandler.post(() -> {
                            if (!isActiveRun(version)) {
                                return;
                            }
                            ensureBotMessageVisible();
                            botMsg.setReasoning(reasoningAccumulator.toString());
                            listener.onMessageUpdated(botMsg);
                        });
                    }

                    @Override
                    public void onToolCall(String name, String arguments, String id) {
                        if (!isActiveRun(version)) {
                            return;
                        }
                        if (!name.isEmpty()) {
                            toolName = name;
                        }
                        if (!arguments.isEmpty()) {
                            toolArgs += arguments;
                        }
                        if (!id.isEmpty()) {
                            toolId = id;
                        }
                    }

                    @Override
                    public void onFinalMessage(String fullContent, String fullReasoning) {
                        if (!isActiveRun(version)) {
                            return;
                        }
                        if (!toolName.isEmpty()) {
                            handleToolCall(toolName, toolArgs, toolId, version);
                        } else {
                            finishProcessing();
                        }
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
                            listener.onError(message);
                            finishProcessing();
                        });
                    }
                });
    }

    private void handleToolCall(String name, String args, String id, int version) {
        Tool tool = toolManager.getTool(name);
        boolean needsApproval = tool != null && tool.requiresApproval();

        ChatMessage toolMsg = new ChatMessage(name, args, System.currentTimeMillis(), id);
        toolMsg.setRequiresApproval(needsApproval);
        prepareToolPreview(toolMsg, tool);
        pendingToolMessage = toolMsg;

        mainHandler.post(() -> {
            if (!isActiveRun(version)) {
                return;
            }

            messages.add(toolMsg);
            listener.onMessageAdded(toolMsg);

            if (needsApproval) {
                pendingAwaitingMessage = new ChatMessage(
                        "A ferramenta '" + name + "' precisa da sua confirmacao antes de rodar.",
                        ChatMessage.TYPE_AWAITING_USER,
                        System.currentTimeMillis(),
                        "Aguardando aprovacao"
                );
                messages.add(pendingAwaitingMessage);
                listener.onMessageAdded(pendingAwaitingMessage);
                setState(State.AWAITING_APPROVAL);
            } else {
                executeTool(toolMsg, version);
            }
        });
    }

    public void approveTool() {
        if (currentState != State.AWAITING_APPROVAL || pendingToolMessage == null) {
            return;
        }

        pendingToolMessage.setApproved(true);
        listener.onMessageUpdated(pendingToolMessage);
        updateAwaitingMessage("Aprovado", "Ferramenta aprovada. Executando agora.");
        executeTool(pendingToolMessage, runVersion);
    }

    public void rejectTool() {
        if (currentState != State.AWAITING_APPROVAL || pendingToolMessage == null) {
            return;
        }

        pendingToolMessage.setRejected(true);
        pendingToolMessage.setToolRunning(false);
        pendingToolMessage.setToolError(true);
        pendingToolMessage.setToolResult("Execucao rejeitada pelo usuario antes de alterar o projeto.");
        listener.onMessageUpdated(pendingToolMessage);
        updateAwaitingMessage("Rejeitado", "A execucao da ferramenta foi rejeitada pelo usuario.");
        finishProcessing();
    }

    private void executeTool(final ChatMessage toolMsg, final int version) {
        if (!isActiveRun(version)) {
            return;
        }

        setState(State.EXECUTING_TOOL);
        updateAwaitingMessage("Executando", "Ferramenta em execucao...");

        currentToolThread = new Thread(() -> {
            ChatCheckpointManager.CheckpointEntry checkpointEntry = createCheckpointIfNeeded(toolMsg);
            if (checkpointEntry != null) {
                mainHandler.post(() -> {
                    if (!isActiveRun(version)) {
                        return;
                    }
                    toolMsg.setStatus("Checkpoint salvo");
                    listener.onMessageUpdated(toolMsg);

                    ChatMessage checkpointMsg = new ChatMessage(
                            "Checkpoint salvo para '" + checkpointEntry.filePath + "' antes da alteracao.",
                            ChatMessage.TYPE_CHECKPOINT,
                            System.currentTimeMillis(),
                            "Checkpoint"
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
                listener.onMessageUpdated(toolMsg);

                if (isError) {
                    updateAwaitingMessage("Erro", "A ferramenta terminou com erro.");
                    finishProcessing();
                    return;
                }

                updateAwaitingMessage("Concluido", "Ferramenta concluida. Continuando a resposta.");
                clearPendingOperationalMessages();
                startAgentLoop(version);
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
            if (filePath.isEmpty() || content.isEmpty()) {
                return;
            }

            boolean existedBefore = SketchwareFileDecryptor.fileExists(scId, filePath);
            String beforeContent = existedBefore ? safe(SketchwareFileDecryptor.decryptFile(scId, filePath)) : "";
            String preview = ChatDiffUtils.buildPreview(filePath, beforeContent, content, existedBefore);
            toolMsg.setToolResult(preview);
            toolMsg.setStatus("Diff pronto");
        } catch (Exception ignored) {
        }
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

    private void updateAwaitingMessage(String status, String message) {
        if (pendingAwaitingMessage == null) {
            return;
        }
        pendingAwaitingMessage.setStatus(status);
        pendingAwaitingMessage.setMessage(message);
        listener.onMessageUpdated(pendingAwaitingMessage);
    }

    private void clearPendingOperationalMessages() {
        pendingToolMessage = null;
        pendingAwaitingMessage = null;
    }

    private void finishProcessing() {
        clearPendingOperationalMessages();
        currentStreamingMessage = null;
        currentToolThread = null;
        setState(State.IDLE);
        listener.onProcessingFinished();
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
                || normalized.contains("exception");
    }

    private boolean isActiveRun(int version) {
        return version == runVersion;
    }

    private String findLatestUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message != null && message.isUser() && message.getMessage() != null) {
                return message.getMessage();
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
}
