package pro.sketchware.activities.chat;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_TOOL = 2;

    private String message;
    private int type;
    private long timestamp;
    
    // Tool-specific fields
    private String toolName;
    private String toolArgs;
    private boolean isToolRunning;
    private String toolResult;
    private boolean isExpanded;

    public ChatMessage(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.type = isUser ? TYPE_USER : TYPE_BOT;
        this.timestamp = timestamp;
    }
    
    public ChatMessage(String toolName, String toolArgs, long timestamp) {
        this.type = TYPE_TOOL;
        this.toolName = toolName;
        this.toolArgs = toolArgs;
        this.timestamp = timestamp;
        this.isToolRunning = true;
        this.isExpanded = false;
        this.message = "";
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return type == TYPE_USER;
    }
    
    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolArgs() {
        return toolArgs;
    }

    public boolean isToolRunning() {
        return isToolRunning;
    }

    public void setToolRunning(boolean toolRunning) {
        isToolRunning = toolRunning;
    }

    public String getToolResult() {
        return toolResult;
    }

    public void setToolResult(String toolResult) {
        this.toolResult = toolResult;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}

