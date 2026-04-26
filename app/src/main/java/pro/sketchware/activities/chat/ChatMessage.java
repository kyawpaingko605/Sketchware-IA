package pro.sketchware.activities.chat;

import androidx.annotation.Nullable;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;
    public static final int TYPE_TOOL = 2;
    public static final int TYPE_CHECKPOINT = 3;
    public static final int TYPE_AWAITING_USER = 4;

    private String message;
    private int type;
    private long timestamp;

    // Tool specific
    private @Nullable String toolName;
    private @Nullable String toolArgs;
    private boolean isToolRunning;
    private @Nullable String toolResult;
    private boolean isToolError;
    private @Nullable String toolId;

    // Status/UI specific
    private boolean isExpanded;
    private @Nullable String status;
    private boolean requiresApproval;
    private boolean isApproved;
    private boolean isRejected;

    // Reasoning (Void style)
    private @Nullable String reasoning;
    private boolean isReasoningExpanded;

    public ChatMessage() {
        this.message = "";
        this.type = TYPE_USER;
        this.timestamp = 0L;
    }

    public ChatMessage(String message, int type, long timestamp) {
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
    }

    public ChatMessage(String message, boolean isUser, long timestamp) {
        this(message, isUser ? TYPE_USER : TYPE_BOT, timestamp);
    }

    public ChatMessage(String message, int type, long timestamp, @Nullable String status) {
        this(message, type, timestamp);
        this.status = status;
    }

    public ChatMessage(@Nullable String toolName, @Nullable String toolArgs, long timestamp, @Nullable String toolId) {
        this("", TYPE_TOOL, timestamp);
        this.toolName = toolName;
        this.toolArgs = toolArgs;
        this.toolId = toolId;
        this.isToolRunning = true;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Nullable
    public String getToolName() { return toolName; }
    public void setToolName(@Nullable String toolName) { this.toolName = toolName; }

    @Nullable
    public String getToolArgs() { return toolArgs; }
    public void setToolArgs(@Nullable String toolArgs) { this.toolArgs = toolArgs; }

    public boolean isToolRunning() { return isToolRunning; }
    public void setToolRunning(boolean toolRunning) { isToolRunning = toolRunning; }

    @Nullable
    public String getToolResult() { return toolResult; }
    public void setToolResult(@Nullable String toolResult) { this.toolResult = toolResult; }

    public boolean isToolError() { return isToolError; }
    public void setToolError(boolean toolError) { isToolError = toolError; }

    @Nullable
    public String getToolId() { return toolId; }
    public void setToolId(@Nullable String toolId) { this.toolId = toolId; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    @Nullable
    public String getStatus() { return status; }
    public void setStatus(@Nullable String status) { this.status = status; }

    public boolean getRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }

    public boolean isRejected() { return isRejected; }
    public void setRejected(boolean rejected) { isRejected = rejected; }

    @Nullable
    public String getReasoning() { return reasoning; }
    public void setReasoning(@Nullable String reasoning) { this.reasoning = reasoning; }

    public boolean isReasoningExpanded() { return isReasoningExpanded; }
    public void setReasoningExpanded(boolean reasoningExpanded) { isReasoningExpanded = reasoningExpanded; }

    // Helper methods
    public boolean isUser() { return type == TYPE_USER; }
    public boolean isBot() { return type == TYPE_BOT; }
    public boolean isTool() { return type == TYPE_TOOL; }
    public boolean isCheckpoint() { return type == TYPE_CHECKPOINT; }
    public boolean isAwaitingUser() { return type == TYPE_AWAITING_USER; }
}
