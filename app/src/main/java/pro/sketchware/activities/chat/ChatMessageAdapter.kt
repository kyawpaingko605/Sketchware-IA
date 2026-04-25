package pro.sketchware.activities.chat

import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import pro.sketchware.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatMessageAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var markwon: Markwon? = null

    fun setMarkwon(markwon: Markwon?) {
        this.markwon = markwon
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return if (msg.type == ChatMessage.TYPE_TOOL) VIEW_TYPE_TOOL
        else if (msg.isUser()) VIEW_TYPE_USER
        else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TOOL -> ToolViewHolder(inflater.inflate(R.layout.item_message_tool, parent, false))
            VIEW_TYPE_USER -> MessageViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            else -> MessageViewHolder(inflater.inflate(R.layout.item_message_bot, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        if (holder is MessageViewHolder) {
            val messageText = message.message
            
            holder.textMessage.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            
            if (markwon != null) {
                markwon!!.setMarkdown(holder.textMessage, messageText)
            } else {
                val localMarkwon = Markwon.builder(holder.itemView.context).build()
                localMarkwon.setMarkdown(holder.textMessage, messageText)
            }
            
            holder.textMessage.post {
                val text = holder.textMessage.text
                if (text is Spannable) {
                    val textStr = text.toString()
                    val beforeIndex = textStr.indexOf("BEFORE:")
                    if (beforeIndex >= 0) {
                        val endIndex = (beforeIndex + 7).coerceAtMost(textStr.length)
                        text.setSpan(ForegroundColorSpan(Color.RED), beforeIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    val afterIndex = textStr.indexOf("AFTER:")
                    if (afterIndex >= 0) {
                        val endIndex = (afterIndex + 6).coerceAtMost(textStr.length)
                        text.setSpan(ForegroundColorSpan(Color.GREEN), afterIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
            
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            holder.textTime.text = sdf.format(Date(message.timestamp))
            
        } else if (holder is ToolViewHolder) {
            val context = holder.itemView.context
            val tName = message.toolName
            
            if (tName != null) {
                if (tName.contains("read") || tName.contains("get_item") || tName.contains("decrypt")) {
                    holder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_file_present)
                } else if (tName.contains("write") || tName.contains("edit") || tName.contains("encrypt")) {
                    holder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_edit)
                } else if (tName.contains("list") || tName.contains("glob")) {
                    holder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_folder)
                } else if (tName.contains("search") || tName.contains("grep")) {
                    holder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_search)
                } else {
                    holder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_code)
                }
                holder.textToolName.text = context.getString(R.string.chat_tool_using, tName)
            }
            
            holder.textToolArgs.text = message.toolArgs ?: "{}"
            
            if (message.isToolRunning) {
                holder.progressTool.visibility = View.VISIBLE
                holder.imgToolStatus.visibility = View.GONE
                holder.textToolResult.visibility = View.GONE
            } else {
                holder.progressTool.visibility = View.GONE
                holder.imgToolStatus.visibility = View.VISIBLE
                
                if (message.isToolError) {
                    holder.imgToolStatus.setImageResource(R.drawable.ic_mtrl_cancel)
                    holder.imgToolStatus.setBackgroundResource(R.drawable.bg_status_error)
                    holder.textToolResult.setBackgroundResource(R.drawable.bg_error_box)
                } else {
                    holder.imgToolStatus.setImageResource(R.drawable.ic_mtrl_check)
                    holder.imgToolStatus.setBackgroundResource(R.drawable.bg_status_success)
                    holder.textToolResult.setBackgroundResource(R.drawable.bg_success_box)
                }
                
                if (!message.toolResult.isNullOrEmpty()) {
                    holder.textToolResult.text = message.toolResult
                    holder.textToolResult.visibility = View.VISIBLE
                } else {
                    holder.textToolResult.setText(R.string.chat_tool_finished)
                    holder.textToolResult.visibility = View.VISIBLE
                }
            }
            
            // Cards are always expanded in this design
            holder.layoutToolDetails.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.text_message)
        val textTime: TextView = itemView.findViewById(R.id.text_time)
    }
    
    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutToolHeader: LinearLayout = itemView.findViewById(R.id.layout_tool_header)
        val imgToolIcon: ImageView = itemView.findViewById(R.id.img_tool_icon)
        val textToolName: TextView = itemView.findViewById(R.id.text_tool_name)
        val progressTool: ProgressBar = itemView.findViewById(R.id.progress_tool)
        val imgToolStatus: ImageView = itemView.findViewById(R.id.img_tool_status)
        val imgExpand: ImageView = itemView.findViewById(R.id.img_expand)
        val layoutToolDetails: LinearLayout = itemView.findViewById(R.id.layout_tool_details)
        val textToolArgs: TextView = itemView.findViewById(R.id.text_tool_args)
        val textToolResult: TextView = itemView.findViewById(R.id.text_tool_result)
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_TOOL = 3
    }
}
