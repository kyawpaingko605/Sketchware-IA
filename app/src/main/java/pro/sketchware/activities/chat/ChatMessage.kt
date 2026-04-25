package pro.sketchware.activities.chat

data class ChatMessage(
    var message: String = "",
    val type: Int = TYPE_USER,
    val timestamp: Long = 0L,
    
    // Tool specific
    var toolName: String? = null,
    var toolArgs: String? = null,
    var isToolRunning: Boolean = false,
    var toolResult: String? = null,
    var isToolError: Boolean = false,
    var toolId: String? = null,
    
    // Status/UI specific
    var isExpanded: Boolean = false,
    var status: String? = null,
    var requiresApproval: Boolean = false,
    var isApproved: Boolean = false,
    var isRejected: Boolean = false,
    
    // Reasoning (Void style)
    var reasoning: String? = null,
    var isReasoningExpanded: Boolean = false
) {
    constructor(message: String, isUser: Boolean, timestamp: Long) : this(
        message = message,
        type = if (isUser) TYPE_USER else TYPE_BOT,
        timestamp = timestamp
    )

    constructor(message: String, type: Int, timestamp: Long, status: String?) : this(
        message = message,
        type = type,
        timestamp = timestamp,
        status = status
    )
    
    constructor(toolName: String?, toolArgs: String?, timestamp: Long, toolId: String? = null) : this(
        type = TYPE_TOOL,
        timestamp = timestamp,
        toolName = toolName,
        toolArgs = toolArgs,
        isToolRunning = true,
        isExpanded = false,
        toolId = toolId,
        message = ""
    )

    fun isUser(): Boolean = type == TYPE_USER
    fun isBot(): Boolean = type == TYPE_BOT
    fun isTool(): Boolean = type == TYPE_TOOL
    fun isCheckpoint(): Boolean = type == TYPE_CHECKPOINT
    fun isAwaitingUser(): Boolean = type == TYPE_AWAITING_USER

    companion object {
        const val TYPE_USER = 0
        const val TYPE_BOT = 1
        const val TYPE_TOOL = 2
        const val TYPE_CHECKPOINT = 3
        const val TYPE_AWAITING_USER = 4
    }
}
