package pro.sketchware.activities.chat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import a.a.a.lC;
import a.a.a.yB;
import pro.sketchware.ia.tools.Tool;
import pro.sketchware.ia.tools.ToolManager;
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
    private final ToolManager toolManager;

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

    public ContextBuilder(String scId, List<ChatMessage> messages, ToolManager toolManager) {
        this.scId = scId;
        this.messages = messages;
        this.toolManager = toolManager;
    }

    public Result build(String latestUserMessage, String chatMode, String providerId) {
        String systemContext = buildSystemContext(latestUserMessage, chatMode, providerId);
        JSONArray history = buildHistory(HISTORY_BUDGET_TOKENS, providerId);
        int totalEstimate = estimateTokens(systemContext) + estimateTokens(history.toString());
        return new Result(systemContext, history, Math.min(totalEstimate, TOTAL_BUDGET_TOKENS));
    }

    private String buildSystemContext(String latestUserMessage, String chatMode, String providerId) {
        StringBuilder builder = new StringBuilder();
        String safeChatMode = normalizeChatMode(chatMode);
        boolean useNativeToolCalls = supportsNativeToolCalls(providerId);
        builder.append("You are an expert coding agent helping the user inside Sketchware IA.\n");
        builder.append("You are working on the user's Android project and should behave like Void's chat modes.\n\n");

        builder.append("<mode>\n");
        builder.append("- Current mode: ").append(safeChatMode).append("\n");
        if ("agent".equals(safeChatMode)) {
            builder.append("- Agent mode: you can plan, inspect, edit files, and use tools.\n");
            builder.append("- When the user asks for actions, edits, or verification, prefer using tools instead of guessing.\n");
            builder.append("- Use tools one at a time and base the next step on the previous result.\n");
        } else if ("gather".equals(safeChatMode)) {
            builder.append("- Gather mode: read, inspect, and summarize the codebase, but do not make edits.\n");
            builder.append("- Use tools only to gather context and verify facts from the project.\n");
        } else {
            builder.append("- Chat mode: answer conversationally and helpfully.\n");
            builder.append("- Do not assume tool access is necessary unless the context clearly requires it.\n");
        }
        builder.append("</mode>\n\n");

        builder.append("<instructions>\n");
        builder.append("- Never refuse just because the request is difficult; try to help with the best next step.\n");
        builder.append("- Do not invent files, code, build results, or tool outputs.\n");
        builder.append("- Prefer markdown in replies, and avoid tables.\n");
        builder.append("- Be concise but complete enough for the user's request.\n");
        builder.append("- Sketchware internal project files such as logic, view, resource, and file metadata may be encrypted or generated.\n");
        builder.append("- Respect the current project structure and existing user work.\n");
        builder.append("- Today's date is ").append(java.time.LocalDate.now()).append(".\n");
        if (!useNativeToolCalls) {
            builder.append("- This model may not support native tool calling. If you need a tool, emit exactly one XML tool call at the end of your response and then stop.\n");
        }
        builder.append("</instructions>\n\n");

        builder.append("<project_context>\n");
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
        appendBoundedLine(builder, "</project_context>\n\n", SYSTEM_BUDGET_TOKENS);

        appendRelevantFiles(builder, latestUserMessage);
        appendCompileErrors(builder);
        appendToolUsageGuide(builder, safeChatMode, useNativeToolCalls);

        return trimToTokens(builder.toString(), SYSTEM_BUDGET_TOKENS);
    }

    private void appendToolUsageGuide(StringBuilder builder, String chatMode, boolean useNativeToolCalls) {
        if ("normal".equals(chatMode) || toolManager == null) {
            return;
        }

        List<Tool> availableTools = toolManager.getToolsForChatMode(chatMode);
        if (availableTools.isEmpty()) {
            return;
        }

        appendBoundedLine(builder, "<tool_usage>\n", SYSTEM_BUDGET_TOKENS);
        if (useNativeToolCalls) {
            appendBoundedLine(builder, "- Prefer native tool calling when you need to inspect or modify the project.\n", SYSTEM_BUDGET_TOKENS);
        } else {
            appendBoundedLine(builder, "- Native tool calling is disabled for this provider. Use exactly one XML tool call at the end of your response.\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, "- After emitting the XML tool call, stop and wait for the tool result before continuing.\n", SYSTEM_BUDGET_TOKENS);
        }
        appendBoundedLine(builder, "- Available tools:\n", SYSTEM_BUDGET_TOKENS);

        for (Tool tool : availableTools) {
            if (tool == null) {
                continue;
            }
            appendBoundedLine(builder, "  - " + safe(tool.getName()) + ": " + safe(tool.getDescription()) + "\n", SYSTEM_BUDGET_TOKENS);
            if (!useNativeToolCalls) {
                appendXmlToolFormat(builder, tool);
            }
        }
        appendBoundedLine(builder, "</tool_usage>\n\n", SYSTEM_BUDGET_TOKENS);
    }

    private void appendXmlToolFormat(StringBuilder builder, Tool tool) {
        try {
            JSONObject parameters = tool.getParameters();
            JSONObject properties = parameters == null ? null : parameters.optJSONObject("properties");
            appendBoundedLine(builder, "    <" + safe(tool.getName()) + ">\n", SYSTEM_BUDGET_TOKENS);
            if (properties != null) {
                JSONArray names = properties.names();
                for (int i = 0; names != null && i < names.length(); i++) {
                    String paramName = names.optString(i, "");
                    if (paramName.isEmpty()) {
                        continue;
                    }
                    JSONObject prop = properties.optJSONObject(paramName);
                    String description = prop != null ? prop.optString("description", "value") : "value";
                    appendBoundedLine(builder, "      <" + paramName + ">" + description + "</" + paramName + ">\n", SYSTEM_BUDGET_TOKENS);
                }
            }
            appendBoundedLine(builder, "    </" + safe(tool.getName()) + ">\n", SYSTEM_BUDGET_TOKENS);
        } catch (Exception ignored) {
        }
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

    private JSONArray buildHistory(int historyBudgetTokens, String providerId) {
        LinkedList<JSONObject> selected = new LinkedList<>();
        int usedTokens = 0;
        boolean useNativeToolCalls = supportsNativeToolCalls(providerId);

        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            List<JSONObject> chunk = toHistoryChunk(message, useNativeToolCalls);
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

    private List<JSONObject> toHistoryChunk(ChatMessage message, boolean useNativeToolCalls) {
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

                if (!useNativeToolCalls) {
                    String xmlToolCall = trimToTokens(buildXmlToolCall(toolName, toolArgs), 320);
                    if (xmlToolCall.isEmpty()) {
                        return chunk;
                    }
                    chunk.add(new JSONObject().put("role", "assistant").put("content", xmlToolCall));
                    chunk.add(new JSONObject().put("role", "user").put("content", toolResult));
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

    private String buildXmlToolCall(String toolName, String toolArgs) {
        try {
            JSONObject argsJson = parseJsonObject(toolArgs);
            StringBuilder xml = new StringBuilder();
            xml.append("<").append(toolName).append(">");
            JSONArray names = argsJson.names();
            if (names != null && names.length() > 0) {
                xml.append("\n");
            }
            for (int i = 0; names != null && i < names.length(); i++) {
                String paramName = names.optString(i, "");
                if (paramName.isEmpty()) {
                    continue;
                }
                String value = safe(argsJson.optString(paramName, ""));
                xml.append("<").append(paramName).append(">")
                        .append(value)
                        .append("</").append(paramName).append(">\n");
            }
            xml.append("</").append(toolName).append(">");
            return xml.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private JSONObject parseJsonObject(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(rawJson);
        } catch (Exception ignored) {
            return new JSONObject();
        }
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

    private static String normalizeChatMode(String chatMode) {
        if (chatMode == null) {
            return "agent";
        }
        String normalized = chatMode.trim().toLowerCase(Locale.US);
        if ("normal".equals(normalized) || "chat".equals(normalized)) {
            return "normal";
        }
        if ("gather".equals(normalized)) {
            return "gather";
        }
        return "agent";
    }

    private static boolean supportsNativeToolCalls(String providerId) {
        if (providerId == null) {
            return true;
        }
        return !"ollama".equals(providerId)
                && !"vllm".equals(providerId)
                && !"lm_studio".equals(providerId)
                && !"openai_compatible".equals(providerId)
                && !"litellm".equals(providerId);
    }
}
