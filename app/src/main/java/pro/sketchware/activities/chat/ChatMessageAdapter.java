package pro.sketchware.activities.chat;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.noties.markwon.Markwon;
import pro.sketchware.R;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<ChatMessage> messages;
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private static final int VIEW_TYPE_TOOL = 3;
    private Markwon markwon;

    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public void setMarkwon(Markwon markwon) {
        this.markwon = markwon;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        if (msg.getType() == ChatMessage.TYPE_TOOL) {
            return VIEW_TYPE_TOOL;
        }
        return msg.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_TOOL) {
            View view = inflater.inflate(R.layout.item_message_tool, parent, false);
            return new ToolViewHolder(view);
        } else if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new MessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_bot, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (holder instanceof MessageViewHolder) {
            MessageViewHolder msgHolder = (MessageViewHolder) holder;
            String messageText = message.getMessage();
            
            // Aplicar formatação markdown
            if (msgHolder.textMessage != null) {
                // Garantir que o TextView está configurado para renderizar markdown
                msgHolder.textMessage.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                
                if (markwon != null) {
                    // Usar Markwon configurado
                    markwon.setMarkdown(msgHolder.textMessage, messageText);
                } else {
                    // Criar Markwon localmente se não foi configurado
                    Markwon localMarkwon = Markwon.builder(msgHolder.itemView.getContext())
                            .build();
                    localMarkwon.setMarkdown(msgHolder.textMessage, messageText);
                }
                
                // Aplicar cores para BEFORE (vermelho) e AFTER (verde) após o Markwon renderizar
                msgHolder.textMessage.post(() -> {
                    CharSequence text = msgHolder.textMessage.getText();
                    if (text instanceof Spannable) {
                        Spannable spannable = (Spannable) text;
                        String textStr = text.toString();
                        
                        // Procurar por "BEFORE:" e aplicar cor vermelha
                        int beforeIndex = textStr.indexOf("BEFORE:");
                        if (beforeIndex >= 0) {
                            int endIndex = Math.min(beforeIndex + 7, textStr.length());
                            spannable.setSpan(new ForegroundColorSpan(Color.RED), beforeIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        
                        // Procurar por "AFTER:" e aplicar cor verde
                        int afterIndex = textStr.indexOf("AFTER:");
                        if (afterIndex >= 0) {
                            int endIndex = Math.min(afterIndex + 6, textStr.length());
                            spannable.setSpan(new ForegroundColorSpan(Color.GREEN), afterIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                });
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            msgHolder.textTime.setText(sdf.format(new Date(message.getTimestamp())));
            
        } else if (holder instanceof ToolViewHolder) {
            ToolViewHolder toolHolder = (ToolViewHolder) holder;
            
            // Icon logic based on tool name
            String tName = message.getToolName();
            if (tName != null) {
                if (tName.contains("read") || tName.contains("get_item") || tName.contains("decrypt")) {
                    toolHolder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_file_present);
                } else if (tName.contains("write") || tName.contains("edit") || tName.contains("morph")) {
                    toolHolder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_edit);
                } else if (tName.contains("list") || tName.contains("glob")) {
                    toolHolder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_folder);
                } else if (tName.contains("search") || tName.contains("grep")) {
                    toolHolder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_search);
                } else {
                    toolHolder.imgToolIcon.setImageResource(R.drawable.ic_mtrl_code);
                }
                
                // Format name
                toolHolder.textToolName.setText("Using " + tName);
            }
            
            // Set arguments
            toolHolder.textToolArgs.setText(message.getToolArgs() != null ? message.getToolArgs() : "{}");
            
            // Handle loading/result state
            if (message.isToolRunning()) {
                toolHolder.progressTool.setVisibility(View.VISIBLE);
                toolHolder.imgToolStatus.setVisibility(View.GONE);
                toolHolder.textToolResult.setVisibility(View.GONE);
            } else {
                toolHolder.progressTool.setVisibility(View.GONE);
                toolHolder.imgToolStatus.setVisibility(View.VISIBLE);
                
                // Show result if available
                if (message.getToolResult() != null && !message.getToolResult().isEmpty()) {
                    toolHolder.textToolResult.setText("Result:\n" + message.getToolResult());
                    toolHolder.textToolResult.setVisibility(View.VISIBLE);
                } else {
                    toolHolder.textToolResult.setText("Finished.");
                    toolHolder.textToolResult.setVisibility(View.VISIBLE);
                }
            }
            
            // Expanded content visibility
            toolHolder.layoutToolDetails.setVisibility(message.isExpanded() ? View.VISIBLE : View.GONE);
            toolHolder.imgExpand.setRotation(message.isExpanded() ? 180f : 0f);
            
            // Click listener for expanding/collapsing
            toolHolder.layoutToolHeader.setOnClickListener(v -> {
                boolean expanded = !message.isExpanded();
                message.setExpanded(expanded);
                toolHolder.layoutToolDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
                toolHolder.imgExpand.animate().rotation(expanded ? 180f : 0f).setDuration(200).start();
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
        }
    }
    
    static class ToolViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutToolHeader;
        ImageView imgToolIcon;
        TextView textToolName;
        ProgressBar progressTool;
        ImageView imgToolStatus;
        ImageView imgExpand;
        LinearLayout layoutToolDetails;
        TextView textToolArgs;
        TextView textToolResult;

        ToolViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutToolHeader = itemView.findViewById(R.id.layout_tool_header);
            imgToolIcon = itemView.findViewById(R.id.img_tool_icon);
            textToolName = itemView.findViewById(R.id.text_tool_name);
            progressTool = itemView.findViewById(R.id.progress_tool);
            imgToolStatus = itemView.findViewById(R.id.img_tool_status);
            imgExpand = itemView.findViewById(R.id.img_expand);
            layoutToolDetails = itemView.findViewById(R.id.layout_tool_details);
            textToolArgs = itemView.findViewById(R.id.text_tool_args);
            textToolResult = itemView.findViewById(R.id.text_tool_result);
        }
    }
}

