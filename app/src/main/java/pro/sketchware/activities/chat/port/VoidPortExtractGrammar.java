package pro.sketchware.activities.chat.port;

import org.json.JSONArray;
import org.json.JSONObject;

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
                String closeTag = "</" + toolName + ">";
                int start = fullContent.indexOf(openTag);
                int end = fullContent.indexOf(closeTag, start + openTag.length());
                if (start < 0 || end < 0 || end < start) {
                    continue;
                }

                String inner = fullContent.substring(start + openTag.length(), end);
                JSONObject params = new JSONObject();
                JSONObject schema = function.optJSONObject("parameters");
                JSONObject properties = schema == null ? null : schema.optJSONObject("properties");
                JSONArray names = properties == null ? null : properties.names();
                for (int j = 0; names != null && j < names.length(); j++) {
                    String paramName = names.optString(j, "").trim();
                    if (paramName.isEmpty()) {
                        continue;
                    }
                    String paramValue = readXmlTag(inner, paramName);
                    if (!paramValue.isEmpty()) {
                        params.put(paramName, paramValue);
                    }
                }

                String cleaned = (fullContent.substring(0, start) + fullContent.substring(end + closeTag.length())).trim();
                return new ToolCallExtraction(
                        cleaned,
                        toolName,
                        params.toString(),
                        "xml_call_" + UUID.randomUUID()
                );
            }
        } catch (Exception ignored) {
        }
        return null;
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
