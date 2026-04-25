package pro.sketchware.activities.chat;

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
 * AgentManager - Orchestrates the agêntic loop and state.
 * Handles: History, Selections, Tool Execution, and Streaming logic.
 */
public class AgentManager {
    private static final String TAG = "AgentManager";

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

    private State currentState = State.IDLE;
    private String currentSystemContext = "";
    private ChatMessage pendingToolMessage = null;
    private ChatMessage pendingAwaitingMessage = null;

    public interface AgentListener {
        void onMessageAdded(ChatMessage message);
        void onMessageUpdated(ChatMessage message);
        void onStatusChanged(String status);
        void onProcessingFinished();
        void onError(String error);
    }

    public AgentManager(String scId, List<ChatMessage> messages, AgentListener listener) {
        this.scId = scId;
        this.messages = messages;
        this.listener = listener;
        this.aiService = AiProviderService.getInstance();
        this.toolManager = new ToolManager();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setSystemContext(String context) {
        this.currentSystemContext = context;
    }

    public State getCurrentState() {
        return currentState;
    }

    private void setState(State state) {
        this.currentState = state;
        String statusText = "";
        switch (state) {
            case THINKING: statusText = "Pensando..."; break;
            case AWAITING_APPROVAL: statusText = "Aguardando aprovação..."; break;
            case EXECUTING_TOOL: statusText = "Executando ação..."; break;
            case IDLE: statusText = ""; break;
        }
        listener.onStatusChanged(statusText);
    }

    public void processUserMessage(String userText) {
        if (currentState != State.IDLE) return;

        ChatMessage userMsg = new ChatMessage(userText, true, System.currentTimeMillis());
        messages.add(userMsg);
        listener.onMessageAdded(userMsg);

        startAgentLoop();
    }

    private void startAgentLoop() {
        setState(State.THINKING);

        JSONArray chatHistory = prepareHistory(30);
        JSONArray tools = toolManager.getToolsAsMCP();
        final ChatMessage botMsg = new ChatMessage("", false, System.currentTimeMillis());
        final boolean[] botMsgAdded = { false };

        aiService.sendStreamingMessage("", tools, chatHistory, currentSystemContext, new AiProviderService.StreamListener() {
            private StringBuilder contentAccumulator = new StringBuilder();
            private StringBuilder reasoningAccumulator = new StringBuilder();
            private String toolName = "";
            private String toolArgs = "";
            private String toolId = "";

            private void ensureBotMessageVisible() {
                if (botMsgAdded[0]) return;
                botMsgAdded[0] = true;
                messages.add(botMsg);
                listener.onMessageAdded(botMsg);
            }

            @Override
            public void onContent(String delta) {
                contentAccumulator.append(delta);
                mainHandler.post(() -> {
                    ensureBotMessageVisible();
                    botMsg.setMessage(contentAccumulator.toString());
                    listener.onMessageUpdated(botMsg);
                });
            }

            @Override
            public void onReasoning(String delta) {
                reasoningAccumulator.append(delta);
                mainHandler.post(() -> {
                    ensureBotMessageVisible();
                    botMsg.setReasoning(reasoningAccumulator.toString());
                    listener.onMessageUpdated(botMsg);
                });
            }

            @Override
            public void onToolCall(String name, String arguments, String id) {
                if (!name.isEmpty()) toolName = name;
                if (!arguments.isEmpty()) toolArgs += arguments;
                if (!id.isEmpty()) toolId = id;
            }

            @Override
            public void onFinalMessage(String fullContent, String fullReasoning) {
                if (!toolName.isEmpty()) {
                    handleToolCall(toolName, toolArgs, toolId);
                } else {
                    finishProcessing();
                }
            }

            @Override
            public void onError(String message, Throwable t) {
                setState(State.ERROR);
                mainHandler.post(() -> {
                    listener.onError(message);
                    finishProcessing();
                });
            }
        });
    }

    private void handleToolCall(String name, String args, String id) {
        Tool tool = toolManager.getTool(name);
        boolean needsApproval = tool != null && tool.requiresApproval();

        ChatMessage toolMsg = new ChatMessage(name, args, System.currentTimeMillis(), id);
        toolMsg.setRequiresApproval(needsApproval);
        pendingToolMessage = toolMsg;

        mainHandler.post(() -> {
            messages.add(toolMsg);
            listener.onMessageAdded(toolMsg);

            if (needsApproval) {
                pendingAwaitingMessage = new ChatMessage(
                        "A ferramenta '" + name + "' precisa da sua confirmação antes de rodar.",
                        ChatMessage.TYPE_AWAITING_USER,
                        System.currentTimeMillis(),
                        "Aguardando aprovação"
                );
                messages.add(pendingAwaitingMessage);
                listener.onMessageAdded(pendingAwaitingMessage);
                setState(State.AWAITING_APPROVAL);
            } else {
                executeTool(toolMsg);
            }
        });
    }

    public void approveTool() {
        if (currentState != State.AWAITING_APPROVAL || pendingToolMessage == null) return;

        pendingToolMessage.setApproved(true);
        listener.onMessageUpdated(pendingToolMessage);
        updateAwaitingMessage("Aprovado", "Ferramenta aprovada. Executando agora.");
        executeTool(pendingToolMessage);
    }

    public void rejectTool() {
        if (currentState != State.AWAITING_APPROVAL || pendingToolMessage == null) return;

        pendingToolMessage.setRejected(true);
        pendingToolMessage.setToolRunning(false);
        pendingToolMessage.setToolError(true);
        pendingToolMessage.setToolResult("Execução rejeitada pelo usuário antes de alterar o projeto.");
        listener.onMessageUpdated(pendingToolMessage);
        updateAwaitingMessage("Rejeitado", "A execução da ferramenta foi rejeitada pelo usuário.");
        clearPendingOperationalMessages();
        finishProcessing();
    }

    private void executeTool(ChatMessage toolMsg) {
        setState(State.EXECUTING_TOOL);
        updateAwaitingMessage("Executando", "Ferramenta em execução...");
        createCheckpointIfNeeded(toolMsg);

        new Thread(() -> {
            String result = toolManager.executeTool(scId, toolMsg.getToolName(), toolMsg.getToolArgs());
            boolean isError = looksLikeToolError(result);

            mainHandler.post(() -> {
                toolMsg.setToolRunning(false);
                toolMsg.setToolError(isError);
                toolMsg.setToolResult(result);
                listener.onMessageUpdated(toolMsg);

                if (isError) {
                    updateAwaitingMessage("Erro", "A ferramenta terminou com erro.");
                    clearPendingOperationalMessages();
                    finishProcessing();
                    return;
                }

                updateAwaitingMessage("Concluído", "Ferramenta concluída. Continuando a resposta.");
                clearPendingOperationalMessages();
                startAgentLoop();
            });
        }).start();
    }

    private void updateAwaitingMessage(String status, String message) {
        if (pendingAwaitingMessage == null) return;
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
        setState(State.IDLE);
        listener.onProcessingFinished();
    }

    private boolean looksLikeToolError(String result) {
        if (result == null) return true;
        String normalized = result.trim().toLowerCase();
        return normalized.startsWith("erro")
                || normalized.startsWith("falha")
                || normalized.contains("comando bloqueado")
                || normalized.contains("não foi possível")
                || normalized.contains("nao foi possivel");
    }

    private void createCheckpointIfNeeded(ChatMessage toolMsg) {
        Tool tool = toolManager.getTool(toolMsg.getToolName());
        if (tool == null || !tool.isDestructive()) return;

        String checkpointText = "Checkpoint criado antes de executar '" + toolMsg.getToolName() + "'.";
        try {
            JSONObject args = toolMsg.getToolArgs() == null || toolMsg.getToolArgs().trim().isEmpty()
                    ? new JSONObject()
                    : new JSONObject(toolMsg.getToolArgs());
            String filePath = args.optString("file_path", "").trim();
            if (!filePath.isEmpty()) {
                String currentContent = SketchwareFileDecryptor.decryptFile(scId, normalizeSketchwarePath(filePath));
                if (currentContent != null && !currentContent.trim().isEmpty()) {
                    checkpointText = "Checkpoint criado para '" + filePath + "' antes da alteração.";
                }
            }
        } catch (Exception ignored) {
        }

        ChatMessage checkpointMsg = new ChatMessage(
                checkpointText,
                ChatMessage.TYPE_CHECKPOINT,
                System.currentTimeMillis(),
                "Checkpoint"
        );
        messages.add(checkpointMsg);
        listener.onMessageAdded(checkpointMsg);
    }

    private String normalizeSketchwarePath(String input) {
        if (input == null) return "";
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

    private JSONArray prepareHistory(int count) {
        JSONArray history = new JSONArray();
        int start = Math.max(0, messages.size() - count);

        for (int i = start; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            try {
                JSONObject obj = new JSONObject();
                if (msg.isUser()) {
                    if (msg.getMessage() == null || msg.getMessage().trim().isEmpty()) continue;
                    obj.put("role", "user");
                    obj.put("content", msg.getMessage());
                } else if (msg.isBot()) {
                    boolean hasContent = msg.getMessage() != null && !msg.getMessage().trim().isEmpty();
                    boolean hasReasoning = msg.getReasoning() != null && !msg.getReasoning().trim().isEmpty();
                    if (!hasContent && !hasReasoning) continue;
                    obj.put("role", "assistant");
                    obj.put("content", hasContent ? msg.getMessage() : "");
                } else if (msg.isTool()) {
                    if (msg.getToolResult() == null || msg.getToolResult().trim().isEmpty()) continue;
                    JSONObject assistantCall = new JSONObject();
                    assistantCall.put("role", "assistant");
                    assistantCall.put("content", null);

                    JSONArray toolCalls = new JSONArray();
                    JSONObject toolCall = new JSONObject();
                    toolCall.put("id", msg.getToolId() != null ? msg.getToolId() : "call_" + msg.getTimestamp());
                    toolCall.put("type", "function");
                    JSONObject function = new JSONObject();
                    function.put("name", msg.getToolName());
                    function.put("arguments", msg.getToolArgs());
                    toolCall.put("function", function);
                    toolCalls.put(toolCall);
                    assistantCall.put("tool_calls", toolCalls);
                    history.put(assistantCall);

                    JSONObject toolResponse = new JSONObject();
                    toolResponse.put("role", "tool");
                    toolResponse.put("tool_call_id", toolCall.getString("id"));
                    toolResponse.put("name", msg.getToolName());
                    toolResponse.put("content", msg.getToolResult());
                    history.put(toolResponse);

                    continue;
                }

                if (obj.has("role")) {
                    history.put(obj);
                }
            } catch (Exception ignored) {}
        }
        return history;
    }
}
