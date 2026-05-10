package pro.sketchware.activities.chat;

import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pro.sketchware.R;
import pro.sketchware.activities.chat.port.VoidPortScmService;

public class ChatMessagesFragment extends Fragment {
    private RecyclerView recyclerView;
    private LinearLayout runtimeSummary;
    private ChatMessageAdapter adapter;
    private String scId;
    private List<ChatMessage> messages = new ArrayList<>();
    private boolean processing;
    private String statusText = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recycler_view_messages);
        runtimeSummary = view.findViewById(R.id.chat_runtime_summary);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (adapter != null) {
            recyclerView.setAdapter(adapter);
        }
        refreshRuntimeSummary();
    }

    public void setAdapter(ChatMessageAdapter adapter) {
        this.adapter = adapter;
        if (recyclerView != null) {
            recyclerView.setAdapter(adapter);
        }
    }

    public void setRuntimeState(String scId, List<ChatMessage> messages, boolean processing, String statusText) {
        this.scId = scId;
        this.messages = messages == null ? new ArrayList<>() : messages;
        this.processing = processing;
        this.statusText = statusText == null ? "" : statusText;
        refreshRuntimeSummary();
    }

    public void scrollToBottom() {
        if (recyclerView != null && adapter != null && adapter.getItemCount() > 0) {
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void refreshRuntimeSummary() {
        if (runtimeSummary == null || !isAdded()) {
            return;
        }
        runtimeSummary.removeAllViews();
        List<ChatPlanManager.Task> plan = ChatPlanManager.buildPlan(requireContext(), scId, messages, processing, statusText);
        ChatPlanManager.Task activeTask = activeTask(plan);
        ChatToolActivitySummary.Summary toolSummary = ChatToolActivitySummary.summarize(messages);
        int changedFiles = VoidPortScmService.changedFileCount(scId);

        runtimeSummary.addView(makeSummaryCard(
                getString(R.string.chat_inline_plan_title),
                activeTask.title,
                activeTask.detail,
                statusLabel(activeTask.status),
                statusColor(activeTask.status)
        ));
        runtimeSummary.addView(makeSummaryCard(
                getString(R.string.chat_inline_artifacts_title),
                getResources().getQuantityString(R.plurals.chat_inline_artifacts_files, changedFiles, changedFiles),
                toolSummary.total() > 0 ? toolSummary.compactLabel(requireContext()) : getString(R.string.chat_artifacts_no_tools_inline),
                changedFiles > 0 ? getString(R.string.chat_plan_status_done) : getString(R.string.chat_plan_status_pending),
                changedFiles > 0 ? color(R.color.chat_success) : color(R.color.chat_border)
        ));
    }

    private ChatPlanManager.Task activeTask(List<ChatPlanManager.Task> tasks) {
        for (ChatPlanManager.Task task : tasks) {
            if (task.status == ChatPlanManager.STATUS_RUNNING) {
                return task;
            }
        }
        for (int i = tasks.size() - 1; i >= 0; i--) {
            ChatPlanManager.Task task = tasks.get(i);
            if (task.status == ChatPlanManager.STATUS_DONE) {
                return task;
            }
        }
        return tasks.isEmpty()
                ? new ChatPlanManager.Task("", "", ChatPlanManager.STATUS_PENDING)
                : tasks.get(0);
    }

    private View makeSummaryCard(String label, String title, String detail, String status, int accentColor) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(rounded(color(R.color.chat_diff_background), accentColor));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);

        TextView labelView = text(label + " | " + status, 11, color(R.color.chat_text_secondary), true);
        TextView titleView = text(title, 14, color(R.color.chat_text_primary), true);
        TextView detailView = text(detail, 12, color(R.color.chat_text_secondary), false);
        card.addView(labelView);
        card.addView(titleView);
        card.addView(detailView);
        return card;
    }

    private TextView text(String value, int sp, int textColor, boolean bold) {
        TextView textView = new TextView(requireContext());
        textView.setText(value == null ? "" : value);
        textView.setTextColor(textColor);
        textView.setTextSize(sp);
        textView.setMaxLines(2);
        textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (bold) {
            textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return textView;
    }

    private String statusLabel(int status) {
        return switch (status) {
            case ChatPlanManager.STATUS_DONE -> getString(R.string.chat_plan_status_done);
            case ChatPlanManager.STATUS_RUNNING -> getString(R.string.chat_plan_status_running);
            default -> getString(R.string.chat_plan_status_pending);
        };
    }

    private int statusColor(int status) {
        return switch (status) {
            case ChatPlanManager.STATUS_DONE -> color(R.color.chat_success);
            case ChatPlanManager.STATUS_RUNNING -> color(R.color.chat_accent);
            default -> color(R.color.chat_border);
        };
    }

    private GradientDrawable rounded(int backgroundColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int color(int colorRes) {
        return requireContext().getColor(colorRes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
