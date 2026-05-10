package pro.sketchware.activities.chat;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
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
            bindMessage((MessageViewHolder) holder, message);
        } else if (holder instanceof ToolViewHolder) {
            bindTool((ToolViewHolder) holder, message);
        }
    }

    private void bindMessage(@NonNull MessageViewHolder holder, @NonNull ChatMessage message) {
        String messageText = sanitizeText(message.getMessage());
        String statusText = sanitizeText(message.getStatus());
        String reasoningText = sanitizeText(message.getReasoning());

        if (holder.textStatusChip != null) {
            holder.textStatusChip.setVisibility(View.GONE);
            if (message.isCheckpoint()) {
                holder.textStatusChip.setVisibility(View.VISIBLE);
                holder.textStatusChip.setText(ChatMessage.hasVisibleText(statusText)
                        ? statusText
                        : holder.itemView.getContext().getString(R.string.chat_checkpoint_status));
            } else if (message.isAwaitingUser()) {
                holder.textStatusChip.setVisibility(View.VISIBLE);
                holder.textStatusChip.setText(ChatMessage.hasVisibleText(statusText)
                        ? statusText
                        : holder.itemView.getContext().getString(R.string.chat_status_waiting_user));
            }
        }

        String displayText = messageText;
        if (!ChatMessage.hasVisibleText(displayText) && message.isBot() && ChatMessage.hasVisibleText(statusText)) {
            displayText = statusText;
        }

        holder.textMessage.setMovementMethod(LinkMovementMethod.getInstance());
        holder.textMessage.setAlpha(ChatMessage.hasVisibleText(messageText) ? 1f : 0.78f);

        if (ChatMessage.hasVisibleText(displayText)) {
            holder.textMessage.setVisibility(View.VISIBLE);
            getMarkwon(holder.itemView.getContext()).setMarkdown(holder.textMessage, displayText);
        } else {
            holder.textMessage.setText("");
            holder.textMessage.setVisibility(View.GONE);
        }
        bindMessageImages(holder, message);

        if (holder.layoutReasoning != null && holder.textReasoning != null) {
            if (ChatMessage.hasVisibleText(reasoningText)) {
                holder.layoutReasoning.setVisibility(View.VISIBLE);
                holder.textReasoning.setText(reasoningText);
            } else {
                holder.layoutReasoning.setVisibility(View.GONE);
                holder.textReasoning.setText("");
            }
        }

        if (ChatMessage.hasVisibleText(displayText)
                && (displayText.contains(PromptConstants.ORIGINAL)
                || displayText.contains(PromptConstants.FINAL)
                || displayText.contains(PromptConstants.DIVIDER))) {
            holder.textMessage.post(() -> {
                CharSequence text = holder.textMessage.getText();
                if (!(text instanceof Spannable)) {
                    return;
                }
                Spannable spannable = (Spannable) text;
                String textStr = text.toString();
                applyMarkerSpan(spannable, textStr, PromptConstants.ORIGINAL, parseRgbColor(VoidColors.REJECT_BG, Color.RED));
                applyMarkerSpan(spannable, textStr, PromptConstants.FINAL, parseRgbColor(VoidColors.ACCEPT_BG, Color.GREEN));
                applyMarkerSpan(spannable, textStr, PromptConstants.DIVIDER, Color.GRAY);
            });
        }

        holder.textTime.setText(formatTime(message.getTimestamp()));
    }

    private void bindMessageImages(@NonNull MessageViewHolder holder, @NonNull ChatMessage message) {
        if (holder.messageImageScroll == null || holder.layoutMessageImages == null) {
            return;
        }
        holder.layoutMessageImages.removeAllViews();
        List<ChatReference> images = message.getImageReferences();
        if (images.isEmpty()) {
            holder.messageImageScroll.setVisibility(View.GONE);
            return;
        }
        holder.messageImageScroll.setVisibility(View.VISIBLE);
        Context context = holder.itemView.getContext();
        for (ChatReference reference : images) {
            FrameLayout frame = new FrameLayout(context);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(dp(context, 96), dp(context, 96));
            frameParams.setMarginEnd(dp(context, 8));
            frame.setLayoutParams(frameParams);
            frame.setPadding(dp(context, 2), dp(context, 2), dp(context, 2), dp(context, 2));
            frame.setBackgroundResource(R.drawable.bg_round_outline);

            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (reference != null && reference.getUri() != null) {
                try {
                    image.setImageURI(reference.getUri());
                } catch (Exception ignored) {
                    image.setImageResource(R.drawable.ic_mtrl_image);
                }
            } else {
                image.setImageResource(R.drawable.ic_mtrl_image);
            }
            frame.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            holder.layoutMessageImages.addView(frame);
        }
    }

    private void bindTool(@NonNull ToolViewHolder holder, @NonNull ChatMessage message) {
        Context context = holder.itemView.getContext();
        String toolName = sanitizeText(message.getToolName());
        String toolArgs = sanitizeText(message.getToolArgs());
        String toolResult = sanitizeText(message.getToolResult());
        String toolStatus = sanitizeText(message.getStatus());
        String toolNotice = sanitizeText(message.getMessage());
        String toolGroup = ChatToolActivitySummary.groupLabel(context, toolName);

        holder.textToolName.setText(ChatMessage.hasVisibleText(toolName)
                ? toolGroup + ": " + toolName
                : context.getString(R.string.chat_tool_unknown));
        holder.textToolArgs.setText(ChatMessage.hasVisibleText(toolArgs) ? toolArgs : "{}");

        holder.textToolStatus.setVisibility(View.VISIBLE);
        holder.textToolStatus.setText(ChatMessage.hasVisibleText(toolStatus)
                ? toolGroup + " | " + toolStatus
                : toolGroup);

        boolean awaitingApproval = message.getRequiresApproval() && !message.isApproved() && !message.isRejected();
        boolean showCancel = message.isToolRunning() && !awaitingApproval;
        boolean hasResult = ChatMessage.hasVisibleText(toolResult);
        boolean hasNotice = ChatMessage.hasVisibleText(toolNotice);

        if (message.isToolRunning() && !hasResult) {
            holder.textResultLabel.setVisibility(View.GONE);
            holder.textToolResult.setVisibility(View.GONE);
        } else {
            holder.textResultLabel.setVisibility(View.VISIBLE);
            holder.textToolResult.setVisibility(View.VISIBLE);
            if (hasResult) {
                holder.textToolResult.setText(toolResult);
            } else if (message.isRejected()) {
                holder.textToolResult.setText(R.string.chat_tool_rejected_message);
            } else if (message.isToolError()) {
                holder.textToolResult.setText(R.string.chat_tool_error_state);
            } else {
                holder.textToolResult.setText(R.string.chat_tool_finished);
            }
        }
        holder.textToolResult.setBackgroundResource(message.isToolError() || message.isRejected()
                ? R.drawable.bg_error_box
                : R.drawable.bg_success_box);

        holder.textToolNotice.setVisibility(hasNotice ? View.VISIBLE : View.GONE);
        holder.textToolNotice.setText(toolNotice);

        if (awaitingApproval) {
            holder.progressTool.setVisibility(View.GONE);
            holder.imgToolStatus.setVisibility(View.GONE);
            holder.layoutApproval.setVisibility(View.VISIBLE);
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnApprove.setText(R.string.chat_tool_approve);
            holder.btnApprove.setContentDescription(ActionIds.VOID_ACCEPT_DIFF_ACTION_ID);
            holder.btnApprove.setOnClickListener(v -> {
                if (context instanceof ChatActivity) {
                    ((ChatActivity) context).approveTool();
                }
            });
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnReject.setText(R.string.chat_tool_reject);
            holder.btnReject.setContentDescription(ActionIds.VOID_REJECT_DIFF_ACTION_ID);
            holder.btnReject.setOnClickListener(v -> {
                if (context instanceof ChatActivity) {
                    ((ChatActivity) context).rejectTool();
                }
            });
        } else if (showCancel) {
            holder.progressTool.setVisibility(View.VISIBLE);
            holder.imgToolStatus.setVisibility(View.GONE);
            holder.layoutApproval.setVisibility(View.VISIBLE);
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnReject.setText(R.string.chat_tool_cancel);
            holder.btnReject.setContentDescription(ActionIds.VOID_REJECT_FILE_ACTION_ID);
            holder.btnReject.setOnClickListener(v -> {
                if (context instanceof ChatActivity) {
                    ((ChatActivity) context).cancelCurrentRun();
                }
            });
        } else {
            holder.layoutApproval.setVisibility(View.GONE);
            holder.progressTool.setVisibility(View.GONE);
            holder.imgToolStatus.setVisibility(View.VISIBLE);
            if (message.isToolError() || message.isRejected()) {
                holder.imgToolStatus.setImageResource(R.drawable.ic_mtrl_cancel);
                holder.imgToolStatus.setBackgroundResource(R.drawable.bg_status_error);
            } else {
                holder.imgToolStatus.setImageResource(R.drawable.ic_mtrl_check);
                holder.imgToolStatus.setBackgroundResource(R.drawable.bg_status_success);
            }
        }

        int iconRes;
        if (toolName.contains("read") || toolName.contains("decrypt")) {
            iconRes = R.drawable.ic_mtrl_file_present;
        } else if (toolName.contains("write") || toolName.contains("edit") || toolName.contains("encrypt")) {
            iconRes = R.drawable.ic_mtrl_edit;
        } else if (toolName.contains("list") || toolName.contains("glob")) {
            iconRes = R.drawable.ic_mtrl_folder;
        } else if (toolName.contains("search") || toolName.contains("grep")) {
            iconRes = R.drawable.ic_mtrl_search;
        } else {
            iconRes = R.drawable.ic_mtrl_code;
        }
        holder.imgToolIcon.setImageResource(iconRes);

        boolean canExpand = hasExpandableDetails(message, hasResult, hasNotice);
        boolean forceExpanded = awaitingApproval || message.isToolRunning();
        boolean expanded = forceExpanded || message.isExpanded();

        holder.layoutToolDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.imgExpand.setVisibility(canExpand ? View.VISIBLE : View.GONE);
        holder.imgExpand.setImageResource(expanded ? R.drawable.ic_mtrl_arrow_up : R.drawable.ic_mtrl_arrow_down);

        holder.layoutToolHeader.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            ChatMessage currentMessage = messages.get(adapterPosition);
            boolean currentAwaiting = currentMessage.getRequiresApproval() && !currentMessage.isApproved() && !currentMessage.isRejected();
            if (currentAwaiting || currentMessage.isToolRunning() || !hasExpandableDetails(currentMessage,
                    ChatMessage.hasVisibleText(sanitizeText(currentMessage.getToolResult())),
                    ChatMessage.hasVisibleText(sanitizeText(currentMessage.getMessage())))) {
                return;
            }
            currentMessage.setExpanded(!currentMessage.isExpanded());
            notifyItemChanged(adapterPosition);
        });
    }

    private void applyMarkerSpan(Spannable spannable, String text, String marker, int color) {
        int index = text.indexOf(marker);
        while (index >= 0) {
            int endIndex = Math.min(index + marker.length(), text.length());
            spannable.setSpan(new ForegroundColorSpan(color), index, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = text.indexOf(marker, endIndex);
        }
    }

    private int parseRgbColor(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        try {
            if (trimmed.startsWith("rgb(") && trimmed.endsWith(")")) {
                String[] parts = trimmed.substring(4, trimmed.length() - 1).split(",");
                if (parts.length == 3) {
                    int red = Integer.parseInt(parts[0].trim());
                    int green = Integer.parseInt(parts[1].trim());
                    int blue = Integer.parseInt(parts[2].trim());
                    return Color.rgb(red, green, blue);
                }
            }
            return Color.parseColor(trimmed);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean hasExpandableDetails(ChatMessage message, boolean hasResult, boolean hasNotice) {
        return ChatMessage.hasVisibleText(sanitizeText(message.getToolArgs()))
                || hasResult
                || hasNotice
                || message.getRequiresApproval();
    }

    private String sanitizeText(String value) {
        if (!ChatMessage.hasVisibleText(value)) {
            return "";
        }
        return value;
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
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
        final View messageImageScroll;
        final LinearLayout layoutMessageImages;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
            textStatusChip = itemView.findViewById(R.id.text_status_chip);
            layoutReasoning = itemView.findViewById(R.id.layout_reasoning);
            textReasoning = itemView.findViewById(R.id.text_reasoning);
            messageImageScroll = itemView.findViewById(R.id.message_image_scroll);
            layoutMessageImages = itemView.findViewById(R.id.layout_message_images);
        }
    }

    public static class ToolViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout layoutToolHeader;
        final ImageView imgToolIcon;
        final TextView textToolName;
        final TextView textToolStatus;
        final ProgressBar progressTool;
        final ImageView imgToolStatus;
        final ImageView imgExpand;
        final LinearLayout layoutToolDetails;
        final TextView textToolArgs;
        final TextView textResultLabel;
        final TextView textToolResult;
        final TextView textToolNotice;
        final LinearLayout layoutApproval;
        final Button btnApprove;
        final Button btnReject;

        public ToolViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutToolHeader = itemView.findViewById(R.id.layout_tool_header);
            imgToolIcon = itemView.findViewById(R.id.img_tool_icon);
            textToolName = itemView.findViewById(R.id.text_tool_name);
            textToolStatus = itemView.findViewById(R.id.text_tool_status);
            progressTool = itemView.findViewById(R.id.progress_tool);
            imgToolStatus = itemView.findViewById(R.id.img_tool_status);
            imgExpand = itemView.findViewById(R.id.img_expand);
            layoutToolDetails = itemView.findViewById(R.id.layout_tool_details);
            textToolArgs = itemView.findViewById(R.id.text_tool_args);
            textResultLabel = itemView.findViewById(R.id.text_result_label);
            textToolResult = itemView.findViewById(R.id.text_tool_result);
            textToolNotice = itemView.findViewById(R.id.text_tool_notice);
            layoutApproval = itemView.findViewById(R.id.layout_approval);
            btnApprove = itemView.findViewById(R.id.btn_approve_tool);
            btnReject = itemView.findViewById(R.id.btn_reject_tool);
        }
    }
}
