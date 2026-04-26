package pro.sketchware.activities.chat;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
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

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private static final int VIEW_TYPE_TOOL = 3;
    private static final int VIEW_TYPE_CHECKPOINT = 4;
    private static final int VIEW_TYPE_AWAITING = 5;

    private final List<ChatMessage> messages;
    private Markwon markwon;

    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    private Markwon getMarkwon(Context context) {
        if (markwon == null) {
            markwon = Markwon.builder(context).build();
        }
        return markwon;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        if (msg.getType() == ChatMessage.TYPE_TOOL) return VIEW_TYPE_TOOL;
        if (msg.isUser()) return VIEW_TYPE_USER;
        if (msg.isCheckpoint()) return VIEW_TYPE_CHECKPOINT;
        if (msg.isAwaitingUser()) return VIEW_TYPE_AWAITING;
        return VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_TOOL:
                return new ToolViewHolder(inflater.inflate(R.layout.item_message_tool, parent, false));
            case VIEW_TYPE_USER:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_user, parent, false));
            case VIEW_TYPE_CHECKPOINT:
            case VIEW_TYPE_AWAITING:
            default:
                return new MessageViewHolder(inflater.inflate(R.layout.item_message_bot, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof MessageViewHolder) {
            MessageViewHolder msgHolder = (MessageViewHolder) holder;
            String messageText = message.getMessage() != null ? message.getMessage() : "";

            if (msgHolder.textStatusChip != null) {
                msgHolder.textStatusChip.setVisibility(View.GONE);
                if (message.isCheckpoint()) {
                    msgHolder.textStatusChip.setVisibility(View.VISIBLE);
                    msgHolder.textStatusChip.setText(message.getStatus() != null ? message.getStatus() : "Checkpoint");
                } else if (message.isAwaitingUser()) {
                    msgHolder.textStatusChip.setVisibility(View.VISIBLE);
                    msgHolder.textStatusChip.setText(message.getStatus() != null ? message.getStatus() : "Aguardando usuário");
                }
            }

            msgHolder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());

            Markwon mk = getMarkwon(holder.itemView.getContext());
            mk.setMarkdown(msgHolder.textMessage, messageText);

            // Reasoning (Void style)
            if (msgHolder.layoutReasoning != null && msgHolder.textReasoning != null) {
                if (message.getReasoning() != null && !message.getReasoning().isEmpty()) {
                    msgHolder.layoutReasoning.setVisibility(View.VISIBLE);
                    msgHolder.textReasoning.setText(message.getReasoning());
                } else {
                    msgHolder.layoutReasoning.setVisibility(View.GONE);
                }
            }

            // Subtle highlighting for streaming or specific markers
            if (messageText.contains("BEFORE:") || messageText.contains("AFTER:")) {
                msgHolder.textMessage.post(() -> {
                    CharSequence text = msgHolder.textMessage.getText();
                    if (text instanceof Spannable) {
                        Spannable spannable = (Spannable) text;
                        String textStr = text.toString();
                        int beforeIndex = textStr.indexOf("BEFORE:");
                        if (beforeIndex >= 0) {
                            int endIndex = Math.min(beforeIndex + 7, textStr.length());
                            spannable.setSpan(new ForegroundColorSpan(Color.RED), beforeIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        int afterIndex = textStr.indexOf("AFTER:");
                        if (afterIndex >= 0) {
                            int endIndex = Math.min(afterIndex + 6, textStr.length());
                            spannable.setSpan(new ForegroundColorSpan(Color.GREEN), afterIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                });
            }

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            msgHolder.textTime.setText(sdf.format(new Date(message.getTimestamp())));

        } else if (holder instanceof ToolViewHolder) {
            ToolViewHolder toolHolder = (ToolViewHolder) holder;
            Context context = toolHolder.itemView.getContext();
            String tName = message.getToolName();

            if (tName != null) {
                int iconRes;
                if (tName.contains("read") || tName.contains("decrypt")) {
                    iconRes = R.drawable.ic_mtrl_file_present;
                } else if (tName.contains("write") || tName.contains("edit") || tName.contains("encrypt")) {
                    iconRes = R.drawable.ic_mtrl_edit;
                } else if (tName.contains("list") || tName.contains("glob")) {
                    iconRes = R.drawable.ic_mtrl_folder;
                } else if (tName.contains("search") || tName.contains("grep")) {
                    iconRes = R.drawable.ic_mtrl_search;
                } else {
                    iconRes = R.drawable.ic_mtrl_code;
                }
                toolHolder.imgToolIcon.setImageResource(iconRes);
                toolHolder.textToolName.setText(tName);
            }

            toolHolder.textToolArgs.setText(message.getToolArgs() != null ? message.getToolArgs() : "{}");

            // Handle Approval UI
            if (message.getRequiresApproval() && !message.isApproved() && !message.isRejected()) {
                if (toolHolder.layoutApproval != null) toolHolder.layoutApproval.setVisibility(View.VISIBLE);
                toolHolder.progressTool.setVisibility(View.GONE);
                toolHolder.imgToolStatus.setVisibility(View.GONE);
                toolHolder.textToolResult.setVisibility(View.GONE);

                if (toolHolder.btnApprove != null) {
                    toolHolder.btnApprove.setOnClickListener(v -> {
                        if (context instanceof ChatActivity) {
                            ((ChatActivity) context).approveTool();
                        }
                    });
                }
                if (toolHolder.btnReject != null) {
                    toolHolder.btnReject.setOnClickListener(v -> {
                        if (context instanceof ChatActivity) {
                            ((ChatActivity) context).rejectTool();
                        }
                    });
                }
            } else {
                if (toolHolder.layoutApproval != null) toolHolder.layoutApproval.setVisibility(View.GONE);

                if (message.isToolRunning()) {
                    toolHolder.progressTool.setVisibility(View.VISIBLE);
                    toolHolder.imgToolStatus.setVisibility(View.GONE);
                    toolHolder.textToolResult.setVisibility(View.GONE);
                } else {
                    toolHolder.progressTool.setVisibility(View.GONE);
                    toolHolder.imgToolStatus.setVisibility(View.VISIBLE);

                    if (message.isToolError() || message.isRejected()) {
                        toolHolder.imgToolStatus.setImageResource(R.drawable.ic_mtrl_cancel);
                        toolHolder.imgToolStatus.setBackgroundResource(R.drawable.bg_status_error);
                    } else {
                        toolHolder.imgToolStatus.setImageResource(R.drawable.ic_mtrl_check);
                        toolHolder.imgToolStatus.setBackgroundResource(R.drawable.bg_status_success);
                    }

                    if (message.getToolResult() != null && !message.getToolResult().isEmpty()) {
                        toolHolder.textToolResult.setText(message.getToolResult());
                        toolHolder.textToolResult.setVisibility(View.VISIBLE);
                    } else {
                        toolHolder.textToolResult.setText(
                                message.isRejected() ? "Ação rejeitada pelo usuário"
                                        : toolHolder.itemView.getContext().getString(R.string.chat_tool_finished)
                        );
                        toolHolder.textToolResult.setVisibility(View.VISIBLE);
                    }
                }
            }

            toolHolder.layoutToolDetails.setVisibility(message.isToolRunning() && !message.getRequiresApproval() ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView textMessage;
        final TextView textTime;
        final TextView textStatusChip;
        final View layoutReasoning;
        final TextView textReasoning;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
            textStatusChip = itemView.findViewById(R.id.text_status_chip);
            layoutReasoning = itemView.findViewById(R.id.layout_reasoning);
            textReasoning = itemView.findViewById(R.id.text_reasoning);
        }
    }

    public static class ToolViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout layoutToolHeader;
        final ImageView imgToolIcon;
        final TextView textToolName;
        final ProgressBar progressTool;
        final ImageView imgToolStatus;
        final ImageView imgExpand;
        final LinearLayout layoutToolDetails;
        final TextView textToolArgs;
        final TextView textToolResult;

        final View layoutApproval;
        final View btnApprove;
        final View btnReject;

        public ToolViewHolder(@NonNull View itemView) {
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

            layoutApproval = itemView.findViewById(R.id.layout_approval);
            btnApprove = itemView.findViewById(R.id.btn_approve_tool);
            btnReject = itemView.findViewById(R.id.btn_reject_tool);
        }
    }
}
