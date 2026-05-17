package pro.sketchware.activities.chat.port;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoidPortExtractGrammar {
    private static final String THINK_OPEN = "<think>";
    private static final String THINK_CLOSE = "</think>";

    private VoidPortExtractGrammar() {
    }

    public static final class ReasoningExtraction {
        public final String fullText;
        public final String fullReasoning;

        public ReasoningExtraction(String fullText, String fullReasoning) {
            this.fullText = fullText == null ? "" : fullText;
            this.fullReasoning = fullReasoning == null ? "" : fullReasoning;
        }
    }

    public static final class ToolCallExtraction {
        public final String cleanedContent;
        public final String toolName;
        public final String toolArguments;
        public final String toolId;

        public ToolCallExtraction(String cleanedContent, String toolName, String toolArguments, String toolId) {
            this.cleanedContent = cleanedContent == null ? "" : cleanedContent;
            this.toolName = toolName == null ? "" : toolName;
            this.toolArguments = toolArguments == null ? "{}" : toolArguments;
            this.toolId = toolId == null ? "" : toolId;
        }
    }

    public static final class XmlToolStreamStep {
        public final String visibleDelta;
        public final String visibleText;
        public final ToolCallExtraction toolCall;

        XmlToolStreamStep(String visibleDelta, String visibleText, ToolCallExtraction toolCall) {
            this.visibleDelta = visibleDelta == null ? "" : visibleDelta;
            this.visibleText = visibleText == null ? "" : visibleText;
            this.toolCall = toolCall;
        }
    }

    public static final class XmlToolStreamParser {
        private final List<String> toolOpenTags = new ArrayList<>();
        private final Map<String, JSONObject> toolFunctions = new HashMap<>();
        private final StringBuilder trueFullText = new StringBuilder();
        private String visibleText = "";
        private String openToolTagBuffer = "";
        private String foundToolName = "";
        private int foundOpenIdx = -1;
        private final String toolId = "xml_call_" + UUID.randomUUID();
        private ToolCallExtraction latestToolCall;

        public XmlToolStreamParser(JSONArray tools) {
            for (int i = 0; tools != null && i < tools.length(); i++) {
                JSONObject tool = tools.optJSONObject(i);
                JSONObject function = tool == null ? null : tool.optJSONObject("function");
                String toolName = function == null ? "" : function.optString("name", "").trim();
                if (toolName.isEmpty()) {
                    continue;
                }
                toolOpenTags.add("<" + toolName + ">");
                toolFunctions.put(toolName, function);
            }
        }

        public boolean isEnabled() {
            return !toolOpenTags.isEmpty();
        }

        public XmlToolStreamStep accept(String newText) {
            String chunk = newText == null ? "" : newText;
            String previousVisible = visibleText;
            trueFullText.append(chunk);

            if (foundToolName.isEmpty()) {
                String newFullText = openToolTagBuffer + chunk;
                if (endsWithAnyToolPrefix(newFullText)) {
                    openToolTagBuffer += chunk;
                } else {
                    String candidate = visibleText + openToolTagBuffer + chunk;
                    openToolTagBuffer = "";
                    FoundToolTag found = findIndexOfAnyToolTag(candidate);
                    if (found != null) {
                        foundToolName = found.toolName;
                        String openTag = "<" + foundToolName + ">";
                        foundOpenIdx = trueFullText.indexOf(openTag);
                        visibleText = candidate.substring(0, found.idx);
                    } else {
                        visibleText = candidate;
                    }
                }
            }

            if (!foundToolName.isEmpty() && foundOpenIdx >= 0) {
                JSONObject function = toolFunctions.get(foundToolName);
                latestToolCall = parseXmlPrefixToToolCall(
                        foundToolName,
                        toolId,
                        trueFullText.substring(foundOpenIdx),
                        function,
                        visibleText
                );
            }

            String delta = "";
            if (visibleText.length() > previousVisible.length()
                    && visibleText.startsWith(previousVisible)) {
                delta = visibleText.substring(previousVisible.length());
            }
            return new XmlToolStreamStep(delta, visibleText, latestToolCall);
        }

        public String getVisibleText() {
            return visibleText;
        }

        public ToolCallExtraction getLatestToolCall() {
            return latestToolCall;
        }

        private boolean endsWithAnyToolPrefix(String text) {
            for (String tag : toolOpenTags) {
                if (endsWithAnyPrefixOf(text, tag)) {
                    return true;
                }
            }
            return false;
        }

        private FoundToolTag findIndexOfAnyToolTag(String text) {
            FoundToolTag best = null;
            for (String tag : toolOpenTags) {
                int idx = text.indexOf(tag);
                if (idx < 0) {
                    continue;
                }
                if (best == null || idx < best.idx) {
                    best = new FoundToolTag(idx, tag.substring(1, tag.length() - 1));
                }
            }
            return best;
        }
    }

    private static final class FoundToolTag {
        final int idx;
        final String toolName;

        FoundToolTag(int idx, String toolName) {
            this.idx = idx;
            this.toolName = toolName;
        }
    }

    public static String readReasoningText(JSONObject jsonObject) {
        String[] keys = new String[] {"reasoning_content", "reasoning", "thinking"};
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            String value = readStreamText(jsonObject, key);
            if (!value.isEmpty()) {
                builder.append(value);
            }
        }
        return builder.toString();
    }

    public static ReasoningExtraction extractThinkTaggedReasoning(String fullContent) {
        String text = fullContent == null ? "" : fullContent;
        int open = text.indexOf(THINK_OPEN);
        if (open < 0) {
            return new ReasoningExtraction(text, "");
        }

        int close = text.indexOf(THINK_CLOSE, open + THINK_OPEN.length());
        if (close < 0) {
            String visible = text.substring(0, open).trim();
            String reasoning = text.substring(open + THINK_OPEN.length()).trim();
            return new ReasoningExtraction(visible, reasoning);
        }

        String before = text.substring(0, open);
        String reasoning = text.substring(open + THINK_OPEN.length(), close);
        String after = text.substring(close + THINK_CLOSE.length());
        return new ReasoningExtraction((before + after).trim(), reasoning.trim());
    }

    public static ToolCallExtraction extractXmlToolCall(String fullContent, JSONArray tools) {
        if (fullContent == null || fullContent.trim().isEmpty() || tools == null || tools.length() == 0) {
            return null;
        }

        try {
            for (int i = 0; i < tools.length(); i++) {
                JSONObject tool = tools.optJSONObject(i);
                JSONObject function = tool == null ? null : tool.optJSONObject("function");
                String toolName = function == null ? "" : function.optString("name", "").trim();
                if (toolName.isEmpty()) {
                    continue;
                }

                String openTag = "<" + toolName + ">";
                int start = fullContent.indexOf(openTag);
                if (start < 0) {
                    continue;
                }

                String closeTag = "</" + toolName + ">";
                int end = fullContent.indexOf(closeTag, start + openTag.length());
                String cleaned = end >= 0
                        ? (fullContent.substring(0, start) + fullContent.substring(end + closeTag.length())).trim()
                        : fullContent.substring(0, start).trim();
                return parseXmlPrefixToToolCall(
                        toolName,
                        "xml_call_" + UUID.randomUUID(),
                        fullContent.substring(start),
                        function,
                        cleaned
                );
            }

            // Check for known incorrect tools like get_file
            if (fullContent.contains("<get_file>")) {
                int start = fullContent.indexOf("<get_file>");
                int end = fullContent.indexOf("</get_file>", start);
                String cleaned = end >= 0 
                    ? (fullContent.substring(0, start) + fullContent.substring(end + 11)).trim()
                    : fullContent.substring(0, start).trim();
                
                return new ToolCallExtraction(
                    cleaned,
                    "get_file",
                    "{}",
                    "error_call_" + UUID.randomUUID()
                );
            }

            // Fallback: Check for naked Search/Replace blocks if no XML tags found
            if ((fullContent.contains("<<<<<<< ORIGINAL") && fullContent.contains(">>>>>>> UPDATED"))
                    || (fullContent.contains("<<<<<<< SEARCH") && fullContent.contains(">>>>>>> REPLACE"))) {
                String uri = findUriInText(fullContent);
                if (!uri.isEmpty()) {
                    int start = fullContent.indexOf("<<<<<<< ORIGINAL");
                    int end = fullContent.lastIndexOf(">>>>>>> UPDATED") + ">>>>>>> UPDATED".length();
                    String blocks = fullContent.substring(start, end);
                    String cleaned = (fullContent.substring(0, start) + fullContent.substring(end)).trim();
                    
                    JSONObject params = new JSONObject();
                    params.put("uri", uri);
                    params.put("search_replace_blocks", blocks);
                    finalizeEditFileParams(params, fullContent.substring(start, end));

                    return new ToolCallExtraction(
                        cleaned,
                        "edit_file",
                        params.toString(),
                        "naked_edit_" + UUID.randomUUID()
                    );
                }
            }

        } catch (Exception ignored) {
        }
        return null;
    }

    private static ToolCallExtraction parseXmlPrefixToToolCall(String toolName, String toolId, String xmlPrefix,
                                                               JSONObject function, String cleanedContent) {
        JSONObject params = new JSONObject();
        try {
            String openTag = "<" + toolName + ">";
            String closeTag = "</" + toolName + ">";
            int start = xmlPrefix == null ? -1 : xmlPrefix.indexOf(openTag);
            if (start < 0) {
                return new ToolCallExtraction(cleanedContent, toolName, params.toString(), toolId);
            }
            int end = xmlPrefix.lastIndexOf(closeTag);
            String inner = end >= 0 && end >= start
                    ? xmlPrefix.substring(start + openTag.length(), end)
                    : xmlPrefix.substring(start + openTag.length());

            JSONObject schema = function == null ? null : function.optJSONObject("parameters");
            JSONObject properties = schema == null ? null : schema.optJSONObject("properties");
            JSONArray names = properties == null ? null : properties.names();
            for (int j = 0; names != null && j < names.length(); j++) {
                String paramName = names.optString(j, "").trim();
                if (paramName.isEmpty()) {
                    continue;
                }
                String paramValue = readXmlTagPrefix(inner, paramName);
                if (!paramValue.isEmpty()) {
                    params.put(paramName, paramValue);
                }
            }

            if (params.length() == 0 && !inner.trim().isEmpty() && names != null && names.length() > 0) {
                if ("edit_file".equals(toolName)) {
                    String uri = findUriInText(inner);
                    if (!uri.isEmpty()) {
                        params.put("uri", uri);
                        params.put("search_replace_blocks", inner.replace(uri, "").trim());
                    } else {
                        params.put(names.optString(0, "uri"), inner.trim());
                    }
                } else {
                    String firstParam = names.optString(0, "");
                    if (!firstParam.isEmpty()) {
                        params.put(firstParam, inner.trim());
                    }
                }
            }

            if ("edit_file".equals(toolName)) {
                finalizeEditFileParams(params, inner);
            }
        } catch (Exception ignored) {
        }
        return new ToolCallExtraction(cleanedContent, toolName, params.toString(), toolId);
    }

    private static void finalizeEditFileParams(JSONObject params, String inner) {
        if (params == null) {
            return;
        }
        try {
            finalizeEditFileParamsInternal(params, inner);
        } catch (Exception ignored) {
        }
    }

    private static void finalizeEditFileParamsInternal(JSONObject params, String inner) throws Exception {
        String innerText = inner == null ? "" : inner;

        if (!hasNonEmptyParam(params, "uri", "target_file", "file_path", "path")) {
            String uri = findUriInText(innerText);
            if (!uri.isEmpty()) {
                params.put("uri", uri);
            }
        }

        if (hasNonEmptyParam(params, "code_edit", "codeEdit", "update")) {
            return;
        }
        if (hasNonEmptyParam(params, "search_replace_blocks", "searchReplaceBlocks", "diff")) {
            return;
        }

        String blocks = extractSearchReplacePayload(innerText);
        if (!blocks.isEmpty()) {
            params.put("search_replace_blocks", blocks);
            return;
        }

        String codeEdit = extractTaggedValue(innerText, "code_edit");
        if (codeEdit.isEmpty()) {
            codeEdit = extractTaggedValue(innerText, "update");
        }
        if (!codeEdit.isEmpty()) {
            params.put("code_edit", codeEdit);
            return;
        }

        String uri = params.optString("uri", "");
        String remainder = stripKnownEditFileTags(innerText, uri);
        if (!remainder.isEmpty()) {
            if (EditFileSupport.looksLikeDiffBlocks(remainder)) {
                params.put("search_replace_blocks", remainder);
            } else {
                params.put("code_edit", remainder);
            }
        }
    }

    private static boolean hasNonEmptyParam(JSONObject params, String... keys) {
        for (String key : keys) {
            String value = params.optString(key, "").trim();
            if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static String extractSearchReplacePayload(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int original = text.indexOf("<<<<<<< ORIGINAL");
        int search = text.indexOf("<<<<<<< SEARCH");
        int start = original >= 0 ? original : search;
        if (start < 0) {
            return "";
        }
        int updatedEnd = text.indexOf(">>>>>>> UPDATED", start);
        int replaceEnd = text.indexOf(">>>>>>> REPLACE", start);
        int end = Math.max(updatedEnd, replaceEnd);
        if (end < 0) {
            return text.substring(start).trim();
        }
        int endMarkerLen = updatedEnd >= 0 && updatedEnd >= start ? ">>>>>>> UPDATED".length() : ">>>>>>> REPLACE".length();
        return text.substring(start, end + endMarkerLen).trim();
    }

    private static String extractTaggedValue(String inner, String tagName) {
        String tagged = readXmlTag(inner, tagName);
        return tagged == null ? "" : tagged.trim();
    }

    private static String stripKnownEditFileTags(String inner, String uri) {
        String remainder = inner == null ? "" : inner;
        for (String tag : new String[]{
                "uri", "target_file", "file_path", "instructions", "instruction",
                "code_edit", "update", "search_replace_blocks", "explanation"
        }) {
            remainder = removeXmlTagBlock(remainder, tag);
        }
        if (uri != null && !uri.isEmpty()) {
            remainder = remainder.replace(uri, "");
        }
        return remainder.trim();
    }

    private static String removeXmlTagBlock(String text, String tagName) {
        if (text == null || text.isEmpty() || tagName == null || tagName.isEmpty()) {
            return text == null ? "" : text;
        }
        String open = "<" + tagName + ">";
        String close = "</" + tagName + ">";
        int start = text.indexOf(open);
        while (start >= 0) {
            int end = text.indexOf(close, start + open.length());
            if (end < 0) {
                text = text.substring(0, start) + text.substring(start + open.length());
                start = text.indexOf(open);
                continue;
            }
            text = text.substring(0, start) + text.substring(end + close.length());
            start = text.indexOf(open);
        }
        return text;
    }

    private static String findUriInText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(/storage/emulated/0/[\\w./-]+|\\.sketchware/[\\w./-]+|(?:files|src|java|res)/[\\w./-]+\\.[\\w]+)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        String lastUri = "";
        while (matcher.find()) {
            lastUri = matcher.group(1);
        }
        return lastUri;
    }

    public static String readXmlTag(String xml, String tagName) {
        if (xml == null || tagName == null || tagName.trim().isEmpty()) {
            return "";
        }
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = xml.indexOf(openTag);
        int end = xml.indexOf(closeTag, start + openTag.length());
        if (start < 0 || end < 0 || end < start) {
            return "";
        }
        return trimBeforeAndAfterNewLines(xml.substring(start + openTag.length(), end));
    }

    private static String readXmlTagPrefix(String xml, String tagName) {
        if (xml == null || tagName == null || tagName.trim().isEmpty()) {
            return "";
        }
        String openTag = "<" + tagName + ">";
        String closeTag = "</" + tagName + ">";
        int start = xml.indexOf(openTag);
        if (start < 0) {
            return "";
        }
        int contentStart = start + openTag.length();
        int end = xml.indexOf(closeTag, contentStart);
        if (end < 0) {
            end = xml.length();
        }
        return trimBeforeAndAfterNewLines(xml.substring(contentStart, end));
    }

    private static boolean endsWithAnyPrefixOf(String text, String value) {
        if (text == null || value == null || text.isEmpty() || value.isEmpty()) {
            return false;
        }
        for (int i = value.length(); i >= 1; i--) {
            if (text.endsWith(value.substring(0, i))) {
                return true;
            }
        }
        return false;
    }

    private static String readStreamText(JSONObject jsonObject, String key) {
        if (jsonObject == null || key == null || !jsonObject.has(key) || jsonObject.isNull(key)) {
            return "";
        }
        return sanitizeStreamValue(jsonObject.opt(key));
    }

    private static String sanitizeStreamValue(Object value) {
        if (value == null || value == JSONObject.NULL) {
            return "";
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text.trim()) ? "" : text;
    }

    private static String trimBeforeAndAfterNewLines(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String s = value;
        int firstNewLine = s.indexOf('\n');
        if (firstNewLine != -1 && s.substring(0, firstNewLine).trim().isEmpty()) {
            s = s.substring(firstNewLine + 1);
        }
        int lastNewLine = s.lastIndexOf('\n');
        if (lastNewLine != -1 && s.substring(lastNewLine + 1).trim().isEmpty()) {
            s = s.substring(0, lastNewLine);
        }
        return s.trim();
    }
}
