package pro.sketchware.activities.chat;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import a.a.a.lC;
import a.a.a.yB;
import pro.sketchware.SketchApplication;
import pro.sketchware.activities.chat.port.VoidPortAutocompleteService;
import pro.sketchware.activities.chat.port.VoidPortContextGatheringService;
import pro.sketchware.activities.chat.port.VoidPortConvertToLlmMessageService;
import pro.sketchware.activities.chat.port.VoidPortMarkerCheckService;
import pro.sketchware.activities.chat.port.VoidPortMcpChannel;
import pro.sketchware.activities.chat.port.VoidPortModelCapabilities;
import pro.sketchware.activities.chat.port.VoidPortScmService;
import pro.sketchware.activities.chat.port.VoidPortSettings;
import pro.sketchware.ia.tools.Tool;
import pro.sketchware.ia.tools.ToolManager;
import pro.sketchware.util.ProjectPathResolver;

/**
 * Builds a bounded provider-aware request context so the chat can preserve
 * tool history across OpenAI-style, Anthropic-style and XML fallback flows.
 */
public class ContextBuilder {
    private static final int TOTAL_BUDGET_TOKENS = 6000;
    private static final int SYSTEM_BUDGET_TOKENS = 1800;
    private static final int HISTORY_BUDGET_TOKENS = 3400;
    private static final int MAX_RELEVANT_FILES = 8;
    private static final int MAX_COMPILE_ERROR_TOKENS = 500;
    private static final String EMPTY_MESSAGE = VoidPortConvertToLlmMessageService.EMPTY_MESSAGE;

    public enum ProviderFormat {
        OPENAI,
        ANTHROPIC,
        XML_FALLBACK
    }

    private static final class SimpleMessage {
        static final int ROLE_USER = 0;
        static final int ROLE_ASSISTANT = 1;
        static final int ROLE_TOOL = 2;

        final int role;
        final String content;
        final String reasoning;
        final String toolName;
        final String toolArgs;
        final String toolResult;
        final String toolId;

        private SimpleMessage(int role, String content, String reasoning, String toolName, String toolArgs, String toolResult, String toolId) {
            this.role = role;
            this.content = content == null ? "" : content;
            this.reasoning = reasoning == null ? "" : reasoning;
            this.toolName = toolName == null ? "" : toolName;
            this.toolArgs = toolArgs == null ? "" : toolArgs;
            this.toolResult = toolResult == null ? "" : toolResult;
            this.toolId = toolId == null ? "" : toolId;
        }

        static SimpleMessage user(String content) {
            return new SimpleMessage(ROLE_USER, content, "", "", "", "", "");
        }

        static SimpleMessage assistant(String content, String reasoning) {
            return new SimpleMessage(ROLE_ASSISTANT, content, reasoning, "", "", "", "");
        }

        static SimpleMessage tool(String toolName, String toolArgs, String toolResult, String toolId) {
            return new SimpleMessage(ROLE_TOOL, "", "", toolName, toolArgs, toolResult, toolId);
        }
    }

    public static class Result {
        private final String systemContext;
        private final JSONArray messages;
        private final int estimatedTokens;
        private final ProviderFormat providerFormat;

        public Result(String systemContext, JSONArray messages, int estimatedTokens, ProviderFormat providerFormat) {
            this.systemContext = systemContext;
            this.messages = messages;
            this.estimatedTokens = estimatedTokens;
            this.providerFormat = providerFormat;
        }

        public String getSystemContext() {
            return systemContext;
        }

        public JSONArray getMessages() {
            return messages;
        }

        public JSONArray getHistory() {
            return messages;
        }

        public int getEstimatedTokens() {
            return estimatedTokens;
        }

        public ProviderFormat getProviderFormat() {
            return providerFormat;
        }
    }

    private final String scId;
    private final List<ChatMessage> messages;
    private final ToolManager toolManager;

    public ContextBuilder(String scId, List<ChatMessage> messages, ToolManager toolManager) {
        this.scId = scId;
        this.messages = messages;
        this.toolManager = toolManager;
    }

    public Result build(String latestUserMessage, String chatMode, String providerId) {
        SharedPreferences prefs = VoidPortSettings.prefs(SketchApplication.getContext());
        String currentModel = prefs.getString(VoidPortSettings.PREF_CURRENT_MODEL, "");
        ProviderFormat providerFormat = resolveProviderFormat(providerId, currentModel);
        String systemContext = buildSystemContext(latestUserMessage, chatMode, providerId, providerFormat);
        JSONArray providerMessages = buildProviderMessages(HISTORY_BUDGET_TOKENS, providerFormat, providerId);
        int totalEstimate = estimateTokens(systemContext) + estimateTokens(providerMessages.toString());
        return new Result(systemContext, providerMessages, Math.min(totalEstimate, TOTAL_BUDGET_TOKENS), providerFormat);
    }

    private String buildSystemContext(String latestUserMessage, String chatMode, String providerId, ProviderFormat providerFormat) {
        StringBuilder builder = new StringBuilder();
        String safeChatMode = normalizeChatMode(chatMode);
        boolean useNativeToolCalls = providerFormat != ProviderFormat.XML_FALLBACK;
        SharedPreferences prefs = VoidPortSettings.prefs(SketchApplication.getContext());
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
        builder.append("- Runtime OS: ").append(nonEmptyText(SystemInfo.os())).append(".\n");
        builder.append("- Today's date is ").append(PromptConstants.todayDateForPrompt()).append(".\n");
        if (!useNativeToolCalls) {
            builder.append("- This model may not support native tool calling. If you need a tool, emit exactly one XML tool call at the end of your response and then stop.\n");
        }
        builder.append("</instructions>\n\n");

        appendVoidPortSettings(builder, prefs, providerId, safeChatMode);

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

        appendProjectDirectoryTree(builder);
        appendBoundedLine(builder, "</project_context>\n\n", SYSTEM_BUDGET_TOKENS);

        appendVoidEditGuide(builder, prefs);
        appendRelevantFiles(builder, latestUserMessage);
        appendCompileErrors(builder);
        appendToolUsageGuide(builder, safeChatMode, providerFormat);

        return trimToTokens(builder.toString(), SYSTEM_BUDGET_TOKENS);
    }

    private void appendProjectDirectoryTree(StringBuilder builder) {
        appendBoundedLine(builder, "- Project directory tree:\n", SYSTEM_BUDGET_TOKENS);
        boolean appendedAnyRoot = false;
        try {
            for (File root : ProjectPathResolver.getReadableRoots(scId)) {
                if (root == null || !root.exists()) {
                    continue;
                }
                String tree = DirectoryTreeService.getDirectoryStrTool(root);
                tree = trimToTokens(tree, 360);
                if (!tree.isEmpty() && appendBoundedLine(builder, tree + "\n", SYSTEM_BUDGET_TOKENS)) {
                    appendedAnyRoot = true;
                }
            }
        } catch (Exception ignored) {
        }

        if (!appendedAnyRoot) {
            appendBoundedLine(builder, "  /data/" + scId + "/ -> logic, view, file, resource, library\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, "  /mysc/" + scId + "/app/src/main/ -> Java source and resources\n", SYSTEM_BUDGET_TOKENS);
        }
        appendBoundedLine(builder, "\n", SYSTEM_BUDGET_TOKENS);
    }

    private void appendVoidPortSettings(StringBuilder builder, SharedPreferences prefs, String providerId, String chatMode) {
        if (prefs == null || prefs.getBoolean(VoidPortSettings.PREF_DISABLE_SYSTEM_MESSAGE, false)) {
            return;
        }

        String settings = VoidPortSettings.buildSystemPromptSettings(prefs, providerId, chatMode);
        StringBuilder portSummary = new StringBuilder();
        if (!settings.isEmpty()) {
            portSummary.append(settings).append("\n");
        }
        portSummary.append(VoidPortMcpChannel.buildPromptSummary(prefs)).append("\n");
        portSummary.append(VoidPortAutocompleteService.buildPromptSummary(prefs)).append("\n");
        portSummary.append("SCM branch: ").append(VoidPortScmService.gitBranch(scId)).append("\n");
        portSummary.append("SCM status: ").append(VoidPortScmService.gitStat(scId)).append("\n");

        String summary = portSummary.toString().trim();
        if (summary.isEmpty()) {
            return;
        }

        String boundedSettings = trimToTokens(summary, 420);
        appendBoundedLine(builder, "<void_port>\n" + boundedSettings + "\n</void_port>\n\n", SYSTEM_BUDGET_TOKENS);
    }

    private void appendVoidEditGuide(StringBuilder builder, SharedPreferences prefs) {
        if (prefs != null && !VoidPortSettings.isPortedPromptsEnabled(prefs)) {
            return;
        }

        appendBoundedLine(builder, "<void_editing>\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "- For targeted replacements, use Void search/replace blocks with these exact markers:\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "  " + PromptConstants.ORIGINAL + "\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "  " + PromptConstants.DIVIDER + "\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "  " + PromptConstants.FINAL + "\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "- UI action ids: accept diff=" + ActionIds.VOID_ACCEPT_DIFF_ACTION_ID
                + ", reject diff=" + ActionIds.VOID_REJECT_DIFF_ACTION_ID
                + ", accept file=" + ActionIds.VOID_ACCEPT_FILE_ACTION_ID
                + ", reject file=" + ActionIds.VOID_REJECT_FILE_ACTION_ID + ".\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "- Embedded Void source registry assets were removed; use the Android port services as the source of truth.\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "</void_editing>\n\n", SYSTEM_BUDGET_TOKENS);
    }

    private void appendToolUsageGuide(StringBuilder builder, String chatMode, ProviderFormat providerFormat) {
        if ("normal".equals(chatMode) || toolManager == null) {
            return;
        }

        List<Tool> availableTools = toolManager.getToolsForChatMode(chatMode);
        if (availableTools.isEmpty()) {
            return;
        }

        appendBoundedLine(builder, "<tool_usage>\n", SYSTEM_BUDGET_TOKENS);
        if (providerFormat == ProviderFormat.OPENAI) {
            appendBoundedLine(builder, "- Prefer native OpenAI-style tool calling when you need to inspect or modify the project.\n", SYSTEM_BUDGET_TOKENS);
        } else if (providerFormat == ProviderFormat.ANTHROPIC) {
            appendBoundedLine(builder, "- Prefer native Anthropic-style tool calling when you need to inspect or modify the project.\n", SYSTEM_BUDGET_TOKENS);
        } else {
            appendBoundedLine(builder, "- Native tool calling is disabled for this provider. Use exactly one XML tool call at the end of your response.\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, "- After emitting the XML tool call, stop and wait for the tool result before continuing.\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, "- Do not merely describe the tool you want. The app only executes the final XML tool tag.\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, "- Do not put tool calls in reasoning/thinking text. Put the XML tag in the final assistant content.\n", SYSTEM_BUDGET_TOKENS);
        }
        appendBoundedLine(builder, "- Available tools:\n", SYSTEM_BUDGET_TOKENS);

        for (Tool tool : availableTools) {
            if (tool == null) {
                continue;
            }
            appendBoundedLine(builder, "  - " + safe(tool.getName()) + ": " + safe(tool.getDescription()) + "\n", SYSTEM_BUDGET_TOKENS);
            if (providerFormat == ProviderFormat.XML_FALLBACK) {
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
            List<VoidPortContextGatheringService.Snippet> relevantFiles =
                    VoidPortContextGatheringService.gatherRelevantSnippets(scId, latestUserMessage, MAX_RELEVANT_FILES);
            if (relevantFiles == null || relevantFiles.isEmpty()) {
                return;
            }

            appendBoundedLine(builder, "Relevant files for current query:\n", SYSTEM_BUDGET_TOKENS);
            int appended = 0;
            for (VoidPortContextGatheringService.Snippet result : relevantFiles) {
                if (result == null || result.filePath == null || result.filePath.trim().isEmpty()) {
                    continue;
                }
                if (appended >= MAX_RELEVANT_FILES) {
                    break;
                }
                String snippetPreview = safe(result.preview).trim();
                String line = "- " + result.filePath + " [" + result.language + "]";
                if (!safe(result.relevance).trim().isEmpty()) {
                    line += " - " + safe(result.relevance).trim();
                }
                if (!snippetPreview.isEmpty()) {
                    line += " - " + trimToTokens(snippetPreview, 32).replace("\n", " ");
                }
                if (!appendBoundedLine(builder, line + "\n", SYSTEM_BUDGET_TOKENS)) {
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
            String summary = VoidPortMarkerCheckService.buildErrorContext(scId);
            summary = trimToTokens(summary, MAX_COMPILE_ERROR_TOKENS);
            if (summary.isEmpty()) {
                return;
            }

            appendBoundedLine(builder, "CURRENT COMPILE ERRORS:\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, summary + "\n\n", SYSTEM_BUDGET_TOKENS);
        } catch (Exception ignored) {
        }
    }

    private JSONArray buildProviderMessages(int historyBudgetTokens, ProviderFormat providerFormat, String providerId) {
        List<SimpleMessage> simpleMessages = toSimpleMessages();
        JSONArray providerMessages;
        if (providerFormat == ProviderFormat.ANTHROPIC) {
            providerMessages = buildAnthropicMessages(simpleMessages);
        } else if (providerFormat == ProviderFormat.OPENAI) {
            providerMessages = buildOpenAiMessages(simpleMessages, providerId);
        } else {
            providerMessages = buildXmlFallbackMessages(simpleMessages);
        }
        return trimProviderMessages(providerMessages, historyBudgetTokens);
    }

    private List<SimpleMessage> toSimpleMessages() {
        List<SimpleMessage> simpleMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message == null || message.isCheckpoint() || message.isAwaitingUser()) {
                continue;
            }

            if (message.isUser()) {
                String content = trimToTokens(safe(message.getPromptContent()), 900);
                if (!content.isEmpty()) {
                    simpleMessages.add(SimpleMessage.user(content));
                }
                continue;
            }

            if (message.isBot()) {
                String content = trimToTokens(safe(message.getMessage()), 700);
                String reasoning = trimToTokens(safe(message.getReasoning()), 500);
                if (!content.isEmpty() || !reasoning.isEmpty()) {
                    simpleMessages.add(SimpleMessage.assistant(content, reasoning));
                }
                continue;
            }

            if (message.isTool()) {
                String toolName = safe(message.getToolName());
                String toolArgs = trimToTokens(safe(message.getToolArgs()), 320);
                String toolResult = trimToTokens(safe(message.getToolResult()), 800);
                if (!toolName.isEmpty() && !toolResult.isEmpty()) {
                    simpleMessages.add(SimpleMessage.tool(
                            toolName,
                            toolArgs,
                            toolResult,
                            message.getToolId() != null ? message.getToolId() : "call_" + message.getTimestamp()
                    ));
                }
            }
        }
        return simpleMessages;
    }

    private JSONArray buildOpenAiMessages(List<SimpleMessage> simpleMessages, String providerId) {
        JSONArray array = new JSONArray();
        boolean ollamaNative = "ollama".equals(providerId);

        for (SimpleMessage message : simpleMessages) {
            try {
                if (message.role == SimpleMessage.ROLE_USER) {
                    array.put(new JSONObject()
                            .put("role", "user")
                            .put("content", nonEmptyText(message.content)));
                    continue;
                }

                if (message.role == SimpleMessage.ROLE_ASSISTANT) {
                    array.put(new JSONObject()
                            .put("role", "assistant")
                            .put("content", nonEmptyText(buildAssistantContent(message, false))));
                    continue;
                }

                if (message.role == SimpleMessage.ROLE_TOOL) {
                    JSONObject assistant = findPreviousAssistant(array);
                    if (assistant == null) {
                        assistant = new JSONObject()
                                .put("role", "assistant")
                                .put("content", EMPTY_MESSAGE);
                        array.put(assistant);
                    }

                    JSONArray toolCalls = assistant.optJSONArray("tool_calls");
                    if (toolCalls == null) {
                        toolCalls = new JSONArray();
                        assistant.put("tool_calls", toolCalls);
                    }

                    String toolId = safeToolId(message.toolId);
                    JSONObject function = new JSONObject();
                    function.put("name", message.toolName);
                    if (ollamaNative) {
                        function.put("arguments", parseJsonObject(message.toolArgs));
                    } else {
                        function.put("arguments", normalizedJsonString(message.toolArgs));
                    }

                    JSONObject toolCall = new JSONObject();
                    if (!ollamaNative) {
                        toolCall.put("id", toolId);
                        toolCall.put("type", "function");
                    }
                    toolCall.put("function", function);
                    toolCalls.put(toolCall);

                    JSONObject toolMessage = new JSONObject()
                            .put("role", "tool")
                            .put("content", nonEmptyText(message.toolResult));
                    if (ollamaNative) {
                        toolMessage.put("tool_name", message.toolName);
                    } else {
                        toolMessage.put("tool_call_id", toolId);
                        toolMessage.put("name", message.toolName);
                    }
                    array.put(toolMessage);
                }
            } catch (Exception ignored) {
            }
        }

        return array;
    }

    private JSONArray buildAnthropicMessages(List<SimpleMessage> simpleMessages) {
        JSONArray array = new JSONArray();

        for (SimpleMessage message : simpleMessages) {
            try {
                if (message.role == SimpleMessage.ROLE_USER) {
                    array.put(new JSONObject()
                            .put("role", "user")
                            .put("content", nonEmptyText(message.content)));
                    continue;
                }

                if (message.role == SimpleMessage.ROLE_ASSISTANT) {
                    array.put(new JSONObject()
                            .put("role", "assistant")
                            .put("content", buildAnthropicAssistantContent(message)));
                    continue;
                }

                if (message.role == SimpleMessage.ROLE_TOOL) {
                    JSONObject assistant = findPreviousAssistant(array);
                    if (assistant == null) {
                        assistant = new JSONObject()
                                .put("role", "assistant")
                                .put("content", new JSONArray().put(new JSONObject()
                                        .put("type", "text")
                                        .put("text", EMPTY_MESSAGE)));
                        array.put(assistant);
                    }

                    JSONArray assistantContent = ensureAnthropicContentArray(assistant);
                    assistantContent.put(new JSONObject()
                            .put("type", "tool_use")
                            .put("id", safeToolId(message.toolId))
                            .put("name", message.toolName)
                            .put("input", parseJsonObject(message.toolArgs)));

                    JSONArray userContent = new JSONArray();
                    userContent.put(new JSONObject()
                            .put("type", "tool_result")
                            .put("tool_use_id", safeToolId(message.toolId))
                            .put("content", nonEmptyText(message.toolResult)));
                    array.put(new JSONObject()
                            .put("role", "user")
                            .put("content", userContent));
                }
            } catch (Exception ignored) {
            }
        }

        return array;
    }

    private JSONArray buildXmlFallbackMessages(List<SimpleMessage> simpleMessages) {
        JSONArray array = new JSONArray();
        JSONObject pendingUser = null;

        for (int i = 0; i < simpleMessages.size(); i++) {
            SimpleMessage message = simpleMessages.get(i);
            try {
                if (message.role == SimpleMessage.ROLE_ASSISTANT) {
                    if (pendingUser != null) {
                        array.put(pendingUser);
                        pendingUser = null;
                    }

                    String content = buildAssistantContent(message, true);
                    SimpleMessage next = i + 1 < simpleMessages.size() ? simpleMessages.get(i + 1) : null;
                    if (next != null && next.role == SimpleMessage.ROLE_TOOL) {
                        String xmlToolCall = buildXmlToolCall(next.toolName, next.toolArgs);
                        if (!xmlToolCall.isEmpty()) {
                            if (!content.isEmpty()) {
                                content += "\n\n";
                            }
                            content += xmlToolCall;
                        }
                    }

                    array.put(new JSONObject()
                            .put("role", "assistant")
                            .put("content", nonEmptyText(content)));
                    continue;
                }

                if (pendingUser == null) {
                    pendingUser = new JSONObject()
                            .put("role", "user")
                            .put("content", "");
                }

                String addition = message.role == SimpleMessage.ROLE_USER
                        ? nonEmptyText(message.content)
                        : buildXmlToolResult(message.toolName, message.toolResult);

                String existing = pendingUser.optString("content", "");
                if (existing.isEmpty()) {
                    pendingUser.put("content", addition);
                } else {
                    pendingUser.put("content", existing + "\n\n" + addition);
                }
            } catch (Exception ignored) {
            }
        }

        if (pendingUser != null) {
            array.put(pendingUser);
        }
        return array;
    }

    private JSONArray buildAnthropicAssistantContent(SimpleMessage message) {
        JSONArray content = new JSONArray();
        String reasoning = safe(message.reasoning).trim();
        if (!reasoning.isEmpty()) {
            try {
                content.put(new JSONObject()
                        .put("type", "text")
                        .put("text", "<thinking>\n" + reasoning + "\n</thinking>"));
            } catch (Exception ignored) {
            }
        }

        String text = safe(message.content).trim();
        if (!text.isEmpty()) {
            try {
                content.put(new JSONObject()
                        .put("type", "text")
                        .put("text", text));
            } catch (Exception ignored) {
            }
        }

        if (content.length() == 0) {
            try {
                content.put(new JSONObject()
                        .put("type", "text")
                        .put("text", EMPTY_MESSAGE));
            } catch (Exception ignored) {
            }
        }
        return content;
    }

    private JSONArray ensureAnthropicContentArray(JSONObject assistantMessage) {
        Object rawContent = assistantMessage.opt("content");
        if (rawContent instanceof JSONArray) {
            return (JSONArray) rawContent;
        }

        JSONArray content = new JSONArray();
        String text = rawContent == null || rawContent == JSONObject.NULL ? "" : String.valueOf(rawContent);
        try {
            content.put(new JSONObject()
                    .put("type", "text")
                    .put("text", nonEmptyText(text)));
        } catch (Exception ignored) {
        }
        try {
            assistantMessage.put("content", content);
        } catch (Exception ignored) {
        }
        return content;
    }

    private JSONObject findPreviousAssistant(JSONArray array) {
        for (int i = array.length() - 1; i >= 0; i--) {
            JSONObject candidate = array.optJSONObject(i);
            if (candidate != null && "assistant".equals(candidate.optString("role", ""))) {
                return candidate;
            }
        }
        return null;
    }

    private JSONArray trimProviderMessages(JSONArray providerMessages, int historyBudgetTokens) {
        JSONArray trimmed = cloneArray(providerMessages);
        while (trimmed.length() > 1 && estimateTokens(trimmed.toString()) > historyBudgetTokens) {
            trimmed.remove(0);
        }

        if (estimateTokens(trimmed.toString()) <= historyBudgetTokens) {
            return trimmed;
        }

        try {
            JSONObject last = trimmed.optJSONObject(trimmed.length() - 1);
            if (last != null && last.has("content")) {
                Object content = last.opt("content");
                if (content instanceof String) {
                    last.put("content", nonEmptyText(trimToTokens((String) content, Math.max(120, historyBudgetTokens / 2))));
                } else if (content instanceof JSONArray) {
                    trimAnthropicContent((JSONArray) content, Math.max(120, historyBudgetTokens / 2));
                }
            }
        } catch (Exception ignored) {
        }
        return trimmed;
    }

    private void trimAnthropicContent(JSONArray content, int tokenBudget) {
        int remaining = tokenBudget;
        for (int i = 0; i < content.length(); i++) {
            JSONObject block = content.optJSONObject(i);
            if (block == null) {
                continue;
            }
            String type = block.optString("type", "");
            if (!"text".equals(type)) {
                continue;
            }
            String text = trimToTokens(block.optString("text", ""), remaining);
            try {
                block.put("text", nonEmptyText(text));
            } catch (Exception ignored) {
            }
            remaining = Math.max(80, remaining / 2);
        }
    }

    private JSONArray cloneArray(JSONArray source) {
        try {
            return new JSONArray(source.toString());
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private String buildAssistantContent(SimpleMessage message, boolean includeReasoning) {
        return VoidPortConvertToLlmMessageService.buildAssistantContent(
                message.content,
                message.reasoning,
                includeReasoning
        );
    }

    private String buildXmlToolCall(String toolName, String toolArgs) {
        try {
            JSONObject argsJson = parseJsonObject(toolArgs);
            Map<String, String> params = new LinkedHashMap<>();
            JSONArray names = argsJson.names();
            for (int i = 0; names != null && i < names.length(); i++) {
                String paramName = names.optString(i, "");
                if (paramName.isEmpty()) {
                    continue;
                }
                params.put(paramName, safe(argsJson.optString(paramName, "")));
            }
            return PromptConstants.reParsedToolXmlString(toolName, params).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String buildXmlToolResult(String toolName, String toolResult) {
        return VoidPortConvertToLlmMessageService.buildXmlToolResult(toolName, toolResult);
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

    private String normalizedJsonString(String rawJson) {
        return parseJsonObject(rawJson).toString();
    }

    private String safeToolId(String toolId) {
        String safeId = safe(toolId).trim();
        return safeId.isEmpty() ? "call_" + System.currentTimeMillis() : safeId;
    }

    private boolean appendBoundedLine(StringBuilder builder, String line, int maxTokens) {
        if (estimateTokens(builder.toString() + line) > maxTokens) {
            return false;
        }
        builder.append(line);
        return true;
    }

    private static String trimToTokens(String text, int maxTokens) {
        return VoidPortConvertToLlmMessageService.trimToApproxTokens(text, maxTokens);
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0d));
    }

    private static String nonEmptyText(String value) {
        return VoidPortConvertToLlmMessageService.nonEmptyText(value);
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

    public static ProviderFormat resolveProviderFormat(String providerId) {
        return resolveProviderFormat(providerId, null);
    }

    public static ProviderFormat resolveProviderFormat(String providerId, String modelName) {
        if (providerId == null) {
            return ProviderFormat.OPENAI;
        }
        if ("anthropic".equals(providerId)) {
            return ProviderFormat.ANTHROPIC;
        }
        VoidPortModelCapabilities.ToolFormat toolFormat =
                VoidPortModelCapabilities.expectedToolFormat(providerId, modelName == null ? "" : modelName);
        if (toolFormat == VoidPortModelCapabilities.ToolFormat.OPENAI_STYLE
                || toolFormat == VoidPortModelCapabilities.ToolFormat.GEMINI_STYLE) {
            return ProviderFormat.OPENAI;
        }
        if (toolFormat == VoidPortModelCapabilities.ToolFormat.ANTHROPIC_STYLE) {
            return ProviderFormat.ANTHROPIC;
        }
        if ("ollama".equals(providerId)
                || "vllm".equals(providerId)
                || "lm_studio".equals(providerId)
                || "openai_compatible".equals(providerId)
                || "litellm".equals(providerId)) {
            return ProviderFormat.XML_FALLBACK;
        }
        return ProviderFormat.OPENAI;
    }
}
