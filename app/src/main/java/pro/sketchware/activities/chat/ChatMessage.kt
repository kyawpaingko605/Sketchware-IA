package pro.sketchware.activities.chat

data class ChatMessage(
    var message: String = "",
    val type: Int = TYPE_USER,
    val timestamp: Long = 0L,
    
    var toolName: String? = null,
    var toolArgs: String? = null,
    var isToolRunning: Boolean = false,
    var toolResult: String? = null,
    var isToolError: Boolean = false,
    var isExpanded: Boolean = false
) {
    constructor(message: String, isUser: Boolean, timestamp: Long) : this(
        message = message,
        type = if (isUser) TYPE_USER else TYPE_BOT,
        timestamp = timestamp
    )
    
    constructor(toolName: String?, toolArgs: String?, timestamp: Long) : this(
        type = TYPE_TOOL,
        timestamp = timestamp,
        toolName = toolName,
        toolArgs = toolArgs,
        isToolRunning = true,
        isExpanded = false,
        message = ""
    )

    fun isUser(): Boolean = type == TYPE_USER

    companion object {
        const val TYPE_USER = 0
        const val TYPE_BOT = 1
        const val TYPE_TOOL = 2
    }
}
