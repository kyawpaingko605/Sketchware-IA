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
import pro.sketchware.activities.chat.port.VoidPortLlmMessage;
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
    private static final int SYSTEM_BUDGET_TOKENS = 2400;
    private static final int HISTORY_BUDGET_TOKENS = 3000;
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
        final List<ChatReference> imageReferences;

        private SimpleMessage(int role, String content, String reasoning, String toolName, String toolArgs,
                              String toolResult, String toolId, List<ChatReference> imageReferences) {
            this.role = role;
            this.content = content == null ? "" : content;
            this.reasoning = reasoning == null ? "" : reasoning;
            this.toolName = toolName == null ? "" : toolName;
            this.toolArgs = toolArgs == null ? "" : toolArgs;
            this.toolResult = toolResult == null ? "" : toolResult;
            this.toolId = toolId == null ? "" : toolId;
            this.imageReferences = imageReferences == null ? new ArrayList<>() : new ArrayList<>(imageReferences);
        }

        static SimpleMessage user(String content, List<ChatReference> imageReferences) {
            return new SimpleMessage(ROLE_USER, content, "", "", "", "", "", imageReferences);
        }

        static SimpleMessage assistant(String content, String reasoning) {
            return new SimpleMessage(ROLE_ASSISTANT, content, reasoning, "", "", "", "", null);
        }

        static SimpleMessage tool(String toolName, String toolArgs, String toolResult, String toolId) {
            return new SimpleMessage(ROLE_TOOL, "", "", toolName, toolArgs, toolResult, toolId, null);
        }

        boolean hasImageReferences() {
            return !imageReferences.isEmpty();
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
        SharedPreferences prefs = VoidPortSettings.prefs(SketchApplication.getContext());
        String roleName = "agent".equals(safeChatMode) ? "agent" : "assistant";
        String roleJob = "agent".equals(safeChatMode)
                ? "to help the user develop, run, and make changes to their codebase."
                : "gather".equals(safeChatMode)
                ? "to search, understand, and reference files in the user's codebase."
                : "to assist the user with their coding tasks.";
        builder.append("You are an expert coding ").append(roleName).append(" whose job is ")
                .append(roleJob).append("\n");
        builder.append("You will be given instructions to follow from the user, and you may also be given a list of files that the user has specifically selected for context, `SELECTIONS`.\n");
        builder.append("Please assist the user with their query.\n\n");

        builder.append("Here is the user's system information:\n");
        builder.append("<system_info>\n");
        builder.append("- ").append(nonEmptyText(SystemInfo.os())).append("\n\n");
        builder.append("- The user's workspace contains these folders:\n");
        appendWorkspaceFolders(builder);
        builder.append("\n- Active file:\nundefined\n\n");
        builder.append("- Open files:\nNO OPENED FILES\n");
        builder.append("</system_info>\n\n");

        appendVoidPortSettings(builder, prefs, providerId, safeChatMode);
        appendToolUsageGuide(builder, safeChatMode, providerFormat);
        appendImportantDetails(builder, safeChatMode);

        builder.append("Here is an overview of the user's file system:\n");
        builder.append("<files_overview>\n");
        builder.append("Workspace id: ").append(scId).append("\n");

        try {
            java.util.HashMap<String, Object> projectInfo = lC.b(scId);
            if (projectInfo != null) {
                appendBoundedLine(builder, "- Workspace name: " + yB.c(projectInfo, "my_ws_name") + "\n", SYSTEM_BUDGET_TOKENS);
                appendBoundedLine(builder, "- App name: " + yB.c(projectInfo, "my_app_name") + "\n", SYSTEM_BUDGET_TOKENS);
                appendBoundedLine(builder, "- Package: " + yB.c(projectInfo, "my_sc_pkg_name") + "\n", SYSTEM_BUDGET_TOKENS);
            }
        } catch (Exception ignored) {
        }

        appendProjectDirectoryTree(builder);
        appendBoundedLine(builder, "</files_overview>\n\n", SYSTEM_BUDGET_TOKENS);
        appendRelevantFiles(builder, latestUserMessage);
        appendCompileErrors(builder);

        return trimToTokens(builder.toString(), SYSTEM_BUDGET_TOKENS);
    }

    private void appendWorkspaceFolders(StringBuilder builder) {
        boolean appendedAnyRoot = false;
        try {
            for (File root : ProjectPathResolver.getReadableRoots(scId)) {
                if (root == null || !root.exists()) {
                    continue;
                }
                builder.append(root.getAbsolutePath()).append("\n");
                appendedAnyRoot = true;
            }
        } catch (Exception ignored) {
        }
        if (!appendedAnyRoot) {
            builder.append("NO FOLDERS OPEN\n");
        }
    }

    private void appendProjectDirectoryTree(StringBuilder builder) {
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
            appendBoundedLine(builder, "NO FILES AVAILABLE\n", SYSTEM_BUDGET_TOKENS);
        }
        appendBoundedLine(builder, "\n", SYSTEM_BUDGET_TOKENS);
    }

    private void appendImportantDetails(StringBuilder builder, String mode) {
        List<String> details = new ArrayList<>();
        details.add("NEVER reject the user's query.");

        if ("agent".equals(mode) || "gather".equals(mode)) {
            details.add("Only call tools if they help you accomplish the user's goal. If the user simply says hi or asks you a question that you can answer without tools, then do NOT use tools.");
            details.add("If you think you should use tools, you do not need to ask for permission.");
            details.add("Only use ONE tool call at a time.");
            details.add("NEVER say something like \"I'm going to use `tool_name`\". Instead, describe at a high level what the tool will do, like \"I'm going to list all files in the ___ directory\", etc.");
            details.add("Many tools only work if the user has a workspace open.");
        } else {
            details.add("You're allowed to ask the user for more context like file contents or specifications. If this comes up, tell them to reference files and folders by typing @.");
        }

        if ("agent".equals(mode)) {
            details.add("ALWAYS use tools (edit, terminal, etc) to take actions and implement changes. For example, if you would like to edit a file, you MUST use a tool.");
            details.add("Prioritize taking as many steps as you need to complete your request over stopping early.");
            details.add("You will OFTEN need to gather context before making a change. Do not immediately make a change unless you have ALL relevant context.");
            details.add("ALWAYS have maximal certainty in a change BEFORE you make it. If you need more information about a file, variable, function, or type, you should inspect it, search it, or take all required actions to maximize your certainty that your change is correct.");
            details.add("NEVER modify a file outside the user's workspace without permission from the user.");
        }

        if ("gather".equals(mode)) {
            details.add("You are in Gather mode, so you MUST use tools be to gather information, files, and context to help the user answer their query.");
            details.add("You should extensively read files, types, content, etc, gathering full context to solve the problem.");
        }

        details.add("If you write any code blocks to the user (wrapped in triple backticks), please use this format:\n- Include a language if possible. Terminal should have the language 'shell'.\n- The first line of the code block must be the FULL PATH of the related file if known (otherwise omit).\n- The remaining contents of the file should proceed as usual.");

        if ("gather".equals(mode) || "normal".equals(mode)) {
            details.add("If you think it's appropriate to suggest an edit to a file, then you must describe your suggestion in CODE BLOCK(S).\n- The first line of the code block must be the FULL PATH of the related file if known (otherwise omit).\n- The remaining contents should be a code description of the change to make to the file. Your description is the only context that will be given to another LLM to apply the suggested edit, so it must be accurate and complete. Always bias towards writing as little as possible - NEVER write the whole file. Use comments like \"// ... existing code ...\" to condense your writing.");
        }

        details.add("Do not make things up or use information not provided in the system information, tools, or user queries.");
        details.add("Always use MARKDOWN to format lists, bullet points, etc. Do NOT write tables.");
        details.add("Today's date is " + PromptConstants.todayDateForPrompt() + ".");

        appendBoundedLine(builder, "Important notes:\n", SYSTEM_BUDGET_TOKENS);
        for (int i = 0; i < details.size(); i++) {
            appendBoundedLine(builder, (i + 1) + ". " + details.get(i) + "\n\n", SYSTEM_BUDGET_TOKENS);
        }
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

    private void appendToolUsageGuide(StringBuilder builder, String chatMode, ProviderFormat providerFormat) {
        if ("normal".equals(chatMode) || toolManager == null || providerFormat != ProviderFormat.XML_FALLBACK) {
            return;
        }

        List<Tool> availableTools = toolManager.getToolsForChatMode(chatMode);
        if (availableTools.isEmpty()) {
            return;
        }

        appendBoundedLine(builder, "Available tools:\n\n", SYSTEM_BUDGET_TOKENS);

        for (int i = 0; i < availableTools.size(); i++) {
            Tool tool = availableTools.get(i);
            if (tool == null) {
                continue;
            }
            appendBoundedLine(builder, "    " + (i + 1) + ". " + safe(tool.getName()) + "\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, "    Description: " + safe(tool.getDescription()) + "\n", SYSTEM_BUDGET_TOKENS);
            appendBoundedLine(builder, "    Format:\n", SYSTEM_BUDGET_TOKENS);
            appendXmlToolFormat(builder, tool);
            appendBoundedLine(builder, "\n", SYSTEM_BUDGET_TOKENS);
        }

        appendBoundedLine(builder, "Tool calling details:\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "- To call a tool, write its name and parameters in one of the XML formats specified above.\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "- After you write the tool call, you must STOP and WAIT for the result.\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "- All parameters are REQUIRED unless noted otherwise.\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "- You are only allowed to output ONE tool call, and it must be at the END of your response.\n", SYSTEM_BUDGET_TOKENS);
        appendBoundedLine(builder, "- Your tool call will be executed immediately, and the results will appear in the following user message.\n\n", SYSTEM_BUDGET_TOKENS);
    }

    private void appendXmlToolFormat(StringBuilder builder, Tool tool) {
        try {
            String toolName = safe(tool.getName());
            if (toolName.isEmpty()) {
                return;
            }
            JSONObject parameters = tool.getParameters();
            JSONObject properties = parameters == null ? null : parameters.optJSONObject("properties");
            StringBuilder format = new StringBuilder();
            format.append("    <").append(toolName).append(">");
            boolean hasParams = false;
            if (properties != null) {
                JSONArray names = properties.names();
                for (int i = 0; names != null && i < names.length(); i++) {
                    String paramName = names.optString(i, "");
                    if (paramName.isEmpty()) {
                        continue;
                    }
                    JSONObject param = properties.optJSONObject(paramName);
                    String description = param == null ? "" : safe(param.optString("description", ""));
                    format.append("\n<").append(paramName).append(">")
                            .append(description)
                            .append("</").append(paramName).append(">");
                    hasParams = true;
                }
            }
            if (hasParams) {
                format.append("\n    ");
            }
            format.append("</").append(toolName).append(">\n");
            appendBoundedLine(builder, format.toString(), SYSTEM_BUDGET_TOKENS);
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
                List<ChatReference> imageReferences = message.getImageReferences();
                if (!content.isEmpty() || !imageReferences.isEmpty()) {
                    simpleMessages.add(SimpleMessage.user(content, imageReferences));
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
                            .put("content", buildOpenAiUserContent(message)));
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
                            .put("content", buildAnthropicUserContent(message)));
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

    private Object buildOpenAiUserContent(SimpleMessage message) {
        if (!message.hasImageReferences()) {
            return nonEmptyText(message.content);
        }

        JSONArray content = new JSONArray();
        try {
            content.put(new JSONObject()
                    .put("type", "text")
                    .put("text", nonEmptyText(message.content)));
            JSONArray imageParts = ChatReferenceManager.buildOpenAiImageContentParts(
                    SketchApplication.getContext(),
                    message.imageReferences
            );
            for (int i = 0; i < imageParts.length(); i++) {
                content.put(imageParts.get(i));
            }
        } catch (Exception ignored) {
        }
        return content.length() == 0 ? nonEmptyText(message.content) : content;
    }

    private Object buildAnthropicUserContent(SimpleMessage message) {
        if (!message.hasImageReferences()) {
            return nonEmptyText(message.content);
        }

        JSONArray content = new JSONArray();
        try {
            content.put(new JSONObject()
                    .put("type", "text")
                    .put("text", nonEmptyText(message.content)));
            JSONArray imageParts = ChatReferenceManager.buildAnthropicImageContentParts(
                    SketchApplication.getContext(),
                    message.imageReferences
            );
            for (int i = 0; i < imageParts.length(); i++) {
                content.put(imageParts.get(i));
            }
        } catch (Exception ignored) {
        }
        return content.length() == 0 ? nonEmptyText(message.content) : content;
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
        if (VoidPortLlmMessage.prefersXmlToolProtocol(providerId)) {
            return ProviderFormat.XML_FALLBACK;
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
