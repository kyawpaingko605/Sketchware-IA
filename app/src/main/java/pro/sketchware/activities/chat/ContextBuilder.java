package pro.sketchware.activities.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import a.a.a.lC;
import a.a.a.yB;
import pro.sketchware.util.CompileErrorCapture;
import pro.sketchware.util.SemanticFileSearcher;

/**
 * Builds a bounded context payload so the chat behaves more like Void and
 * avoids sending unlimited history to the model.
 */
public class ContextBuilder {
    private static final int TOTAL_BUDGET_TOKENS = 6000;
    private static final int SYSTEM_BUDGET_TOKENS = 1800;
    private static final int HISTORY_BUDGET_TOKENS = 3400;
    private static final int MAX_RELEVANT_FILES = 8;
    private static final int MAX_COMPILE_ERROR_TOKENS = 500;

    private final String scId;
    private final List<ChatMessage> messages;

    public static class Result {
        private final String systemContext;
        private final JSONArray history;
        private final int estimatedTokens;

        public Result(String systemContext, JSONArray history, int estimatedTokens) {
            this.systemContext = systemContext;
            this.history = history;
            this.estimatedTokens = estimatedTokens;
        }

        public String getSystemContext() {
            return systemContext;
        }

        public JSONArray getHistory() {
            return history;
        }

        public int getEstimatedTokens() {
            return estimatedTokens;
        }
    }

    public ContextBuilder(String scId, List<ChatMessage> messages) {
        this.scId = scId;
        this.messages = messages;
    }

    public Result build(String latestUserMessage) {
        String systemContext = buildSystemContext(latestUserMessage);
        JSONArray history = buildHistory(HISTORY_BUDGET_TOKENS);
        int totalEstimate = estimateTokens(systemContext) + estimateTokens(history.toString());
        return new Result(systemContext, history, Math.min(totalEstimate, TOTAL_BUDGET_TOKENS));
    }

    private String buildSystemContext(String latestUserMessage) {
        StringBuilder builder = new StringBuilder();
        builder.append("Identity: You are May, a premium AI coding assistant for Sketchware.\n");
        builder.append("Rules:\n");
        builder.append("- You help users create Android apps using Sketchware.\n");
        builder.append("- You have tools to inspect and modify project files.\n");
        builder.append("- Sketchware internal project files such as logic/view/resource are encrypted.\n");
        builder.append("- Before destructive writes, explain what changed and respect checkpoints.\n\n");

        builder.append("[PROJECT CONTEXT]\n");
        builder.append("- Project ID: ").append(scId).append("\n");

        try {
            java.util.HashMap<String, Object> projectInfo = lC.b(scId);
            if (projectInfo != null) {
                appendBoundedLine(builder, "- Project name: " + yB.c(projectInfo, "my_ws_name") + "\n", SYSTEM_BUDGET_TOKENS);
                appendBoundedLine(builder, "- App name: " + yB.c(projectInfo, "my_app_name") + "\n", SYSTEM_BUDGET_TOKENS);
                appendBoundedLine(builder, "- Package: " + yB.c(projectInfo, "my_sc_pkg_name") + "\n", SYSTEM_BUDGET_TOKENS);
            }
        } catch (Exception ignored) {
        }

        appendBoundedLine(builder, "- Directory Structure:\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "  /data/" + scId + "/ -> logic, view, file, resource, library\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "  /mysc/" + scId + "/app/src/main/ -> Java source and resources\n\n", SYSTEM_BUDGET_TOKENS);

        appendRelevantFiles(builder, latestUserMessage);
        appendCompileErrors(builder);

        return trimToTokens(builder.toString(), SYSTEM_BUDGET_TOKENS);
    }

    private void appendRelevantFiles(StringBuilder builder, String latestUserMessage) {
        if (latestUserMessage == null || latestUserMessage.trim().isEmpty()) {
            return;
        }

        try {
            List<SemanticFileSearcher.SearchResult> relevantFiles =
                    SemanticFileSearcher.searchRelevantFiles(latestUserMessage, scId);
            if (relevantFiles == null || relevantFiles.isEmpty()) {
                return;
            }

            appendBoundedLine(builder, "Relevant files for current query:\n", SYSTEM_BUDGET_TOKENS);
            int appended = 0;
            for (SemanticFileSearcher.SearchResult result : relevantFiles) {
                if (result == null || result.filePath == null || result.filePath.trim().isEmpty()) {
                    continue;
                }
                if (appended >= MAX_RELEVANT_FILES) {
                    break;
                }
                if (!appendBoundedLine(builder, "- " + result.filePath + "\n", SYSTEM_BUDGET_TOKENS)) {
                    break;
                }
                appended++;
            }
            appendBoundedLine(builder, "\n", SYSTEM_BUDGET_TOKENS);
        } catch (Exception ignored) {
        }
    }

    private void appendCompileErrors(StringBuilder builder) {
        try {
            if (!CompileErrorCapture.hasCompileErrors(scId)) {
                return;
            }

            String compileErrors = CompileErrorCapture.getLastCompileErrors(scId);
            if (compileErrors == null || compileErrors.trim().isEmpty()) {
                return;
            }

            String summary = CompileErrorCapture.extractErrorSummary(compileErrors);
            summary = trimToTokens(summary, MAX_COMPILE_ERROR_TOKENS);

            appendBoundedLine(builder, "CURRENT COMPILE ERRORS:\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, summary + "\n\n", SYSTEM_BUDGET_TOKENS);
        } catch (Exception ignored) {
        }
    }

    private JSONArray buildHistory(int historyBudgetTokens) {
        LinkedList<JSONObject> selected = new LinkedList<>();
        int usedTokens = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            List<JSONObject> chunk = toHistoryChunk(message);
            if (chunk.isEmpty()) {
                continue;
            }

            int chunkTokens = estimateChunkTokens(chunk);
            if (usedTokens + chunkTokens > historyBudgetTokens) {
                chunk = trimChunkToBudget(chunk, historyBudgetTokens - usedTokens);
                chunkTokens = estimateChunkTokens(chunk);
            }

            if (chunk.isEmpty() || chunkTokens <= 0 || usedTokens + chunkTokens > historyBudgetTokens) {
                continue;
            }

            for (int j = chunk.size() - 1; j >= 0; j--) {
                selected.addFirst(chunk.get(j));
            }
            usedTokens += chunkTokens;
        }

        JSONArray array = new JSONArray();
        for (JSONObject object : selected) {
            array.put(object);
        }
        return array;
    }

    private List<JSONObject> toHistoryChunk(ChatMessage message) {
        LinkedList<JSONObject> chunk = new LinkedList<>();
        if (message == null || message.isCheckpoint() || message.isAwaitingUser()) {
            return chunk;
        }

        try {
            if (message.isUser()) {
                String content = trimToTokens(safe(message.getMessage()), 500);
                if (content.isEmpty()) {
                    return chunk;
                }
                chunk.add(new JSONObject().put("role", "user").put("content", content));
                return chunk;
            }

            if (message.isBot()) {
                String content = trimToTokens(safe(message.getMessage()), 700);
                if (content.isEmpty()) {
                    return chunk;
                }
                chunk.add(new JSONObject().put("role", "assistant").put("content", content));
                return chunk;
            }

            if (message.isTool()) {
                String toolName = safe(message.getToolName());
                String toolArgs = trimToTokens(safe(message.getToolArgs()), 300);
                String toolResult = trimToTokens(safe(message.getToolResult()), 700);
                if (toolName.isEmpty() || toolResult.isEmpty()) {
                    return chunk;
                }

                JSONObject assistantCall = new JSONObject();
                assistantCall.put("role", "assistant");
                assistantCall.put("content", JSONObject.NULL);

                JSONArray toolCalls = new JSONArray();
                JSONObject toolCall = new JSONObject();
                toolCall.put("id", message.getToolId() != null ? message.getToolId() : "call_" + message.getTimestamp());
                toolCall.put("type", "function");
                JSONObject function = new JSONObject();
                function.put("name", toolName);
                function.put("arguments", toolArgs);
                toolCall.put("function", function);
                toolCalls.put(toolCall);
                assistantCall.put("tool_calls", toolCalls);
                chunk.add(assistantCall);

                JSONObject toolResponse = new JSONObject();
                toolResponse.put("role", "tool");
                toolResponse.put("tool_call_id", toolCall.getString("id"));
                toolResponse.put("name", toolName);
                toolResponse.put("content", toolResult);
                chunk.add(toolResponse);
            }
        } catch (Exception ignored) {
            chunk.clear();
        }
        return chunk;
    }

    private List<JSONObject> trimChunkToBudget(List<JSONObject> chunk, int remainingBudgetTokens) {
        if (chunk.isEmpty() || remainingBudgetTokens <= 0) {
            return new LinkedList<>();
        }

        try {
            JSONObject last = new JSONObject(chunk.get(chunk.size() - 1).toString());
            String content = last.optString("content", "");
            content = trimToTokens(content, Math.max(120, remainingBudgetTokens - 80));
            if (content.isEmpty()) {
                return new LinkedList<>();
            }
            last.put("content", content);

            if (chunk.size() == 1) {
                LinkedList<JSONObject> trimmed = new LinkedList<>();
                trimmed.add(last);
                return trimmed;
            }

            JSONObject first = new JSONObject(chunk.get(0).toString());
            LinkedList<JSONObject> trimmed = new LinkedList<>();
            trimmed.add(first);
            trimmed.add(last);
            return trimmed;
        } catch (Exception ignored) {
            return new LinkedList<>();
        }
    }

    private int estimateChunkTokens(List<JSONObject> chunk) {
        int total = 0;
        for (JSONObject object : chunk) {
            total += estimateTokens(object.toString());
        }
        return total;
    }

    private boolean appendBoundedLine(StringBuilder builder, String line, int maxTokens) {
        if (estimateTokens(builder.toString() + line) > maxTokens) {
            return false;
        }
        builder.append(line);
        return true;
    }

    private static String trimToTokens(String text, int maxTokens) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        int maxChars = Math.max(0, maxTokens * 4);
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxChars - 32)).trim() + "\n...[trimmed for token budget]";
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0d));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
