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

    private fun getMarkwon(context: android.content.Context): Markwon {
        if (markwon == null) {
            markwon = Markwon.builder(context).build()
        }
        return markwon!!
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return if (msg.type == ChatMessage.TYPE_TOOL) VIEW_TYPE_TOOL
        else if (msg.isUser()) VIEW_TYPE_USER
        else if (msg.isCheckpoint()) VIEW_TYPE_CHECKPOINT
        else if (msg.isAwaitingUser()) VIEW_TYPE_AWAITING
        else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TOOL -> ToolViewHolder(inflater.inflate(R.layout.item_message_tool, parent, false))
            VIEW_TYPE_USER -> MessageViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            VIEW_TYPE_CHECKPOINT -> MessageViewHolder(inflater.inflate(R.layout.item_message_bot, parent, false))
            VIEW_TYPE_AWAITING -> MessageViewHolder(inflater.inflate(R.layout.item_message_bot, parent, false))
            else -> MessageViewHolder(inflater.inflate(R.layout.item_message_bot, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        
        if (holder is MessageViewHolder) {
            val messageText = message.message ?: ""

            holder.textStatusChip?.visibility = View.GONE
            if (message.isCheckpoint()) {
                holder.textStatusChip?.visibility = View.VISIBLE
                holder.textStatusChip?.text = message.status ?: "Checkpoint"
            } else if (message.isAwaitingUser()) {
                holder.textStatusChip?.visibility = View.VISIBLE
                holder.textStatusChip?.text = message.status ?: "Aguardando usuário"
            }
            
            holder.textMessage.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            
            val mk = getMarkwon(holder.itemView.context)
            mk.setMarkdown(holder.textMessage, messageText)
            
            // Reasoning (Void style)
            if (holder.layoutReasoning != null && holder.textReasoning != null) {
                if (!message.reasoning.isNullOrEmpty()) {
                    holder.layoutReasoning.visibility = View.VISIBLE
                    holder.textReasoning.text = message.reasoning
                } else {
                    holder.layoutReasoning.visibility = View.GONE
                }
            }

            // Subtle highlighting for streaming or specific markers
            if (messageText.contains("BEFORE:") || messageText.contains("AFTER:")) {
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
            }
            
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            holder.textTime.text = sdf.format(Date(message.timestamp))
            
        } else if (holder is ToolViewHolder) {
            val context = holder.itemView.context
            val tName = message.toolName
            
            if (tName != null) {
                val iconRes = when {
                    tName.contains("read") || tName.contains("decrypt") -> R.drawable.ic_mtrl_file_present
                    tName.contains("write") || tName.contains("edit") || tName.contains("encrypt") -> R.drawable.ic_mtrl_edit
                    tName.contains("list") || tName.contains("glob") -> R.drawable.ic_mtrl_folder
                    tName.contains("search") || tName.contains("grep") -> R.drawable.ic_mtrl_search
                    else -> R.drawable.ic_mtrl_code
                }
                holder.imgToolIcon.setImageResource(iconRes)
                holder.textToolName.text = tName
            }
            
            holder.textToolArgs.text = message.toolArgs ?: "{}"
            
            // Handle Approval UI
            if (message.requiresApproval && !message.isApproved && !message.isRejected) {
                holder.layoutApproval?.visibility = View.VISIBLE
                holder.progressTool.visibility = View.GONE
                holder.imgToolStatus.visibility = View.GONE
                holder.textToolResult.visibility = View.GONE
                
                holder.btnApprove?.setOnClickListener {
                    (context as? ChatActivity)?.approveTool()
                }
                holder.btnReject?.setOnClickListener {
                    (context as? ChatActivity)?.rejectTool()
                }
            } else {
                holder.layoutApproval?.visibility = View.GONE
                
                if (message.isToolRunning) {
                    holder.progressTool.visibility = View.VISIBLE
                    holder.imgToolStatus.visibility = View.GONE
                    holder.textToolResult.visibility = View.GONE
                } else {
                    holder.progressTool.visibility = View.GONE
                    holder.imgToolStatus.visibility = View.VISIBLE
                    
                    if (message.isToolError || message.isRejected) {
                        holder.imgToolStatus.setImageResource(R.drawable.ic_mtrl_cancel)
                        holder.imgToolStatus.setBackgroundResource(R.drawable.bg_status_error)
                    } else {
                        holder.imgToolStatus.setImageResource(R.drawable.ic_mtrl_check)
                        holder.imgToolStatus.setBackgroundResource(R.drawable.bg_status_success)
                    }
                    
                    if (!message.toolResult.isNullOrEmpty()) {
                        holder.textToolResult.text = message.toolResult
                        holder.textToolResult.visibility = View.VISIBLE
                    } else {
                        holder.textToolResult.setText(if (message.isRejected) "Ação rejeitada pelo usuário" else R.string.chat_tool_finished)
                        holder.textToolResult.visibility = View.VISIBLE
                    }
                }
            }
            
            holder.layoutToolDetails.visibility = if (message.isToolRunning && !message.requiresApproval) View.GONE else View.VISIBLE
        }
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.text_message)
        val textTime: TextView = itemView.findViewById(R.id.text_time)
        val textStatusChip: TextView? = itemView.findViewById(R.id.text_status_chip)
        val layoutReasoning: View? = itemView.findViewById(R.id.layout_reasoning)
        val textReasoning: TextView? = itemView.findViewById(R.id.text_reasoning)
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
        
        val layoutApproval: View? = itemView.findViewById(R.id.layout_approval)
        val btnApprove: View? = itemView.findViewById(R.id.btn_approve_tool)
        val btnReject: View? = itemView.findViewById(R.id.btn_reject_tool)
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_TOOL = 3
        private const val VIEW_TYPE_CHECKPOINT = 4
        private const val VIEW_TYPE_AWAITING = 5
    }
}
