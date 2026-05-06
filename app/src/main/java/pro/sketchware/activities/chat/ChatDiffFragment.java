package pro.sketchware.activities.chat;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pro.sketchware.R;
import pro.sketchware.activities.chat.port.VoidPortDiffService;
import pro.sketchware.util.FileChangeTracker;

public class ChatDiffFragment extends Fragment {
    private static final String ARG_SC_ID = "sc_id";
    private static final int MAX_DIFF_FILES = 10;
    private static final int MAX_DIFF_ROWS = 360;

    private String scId;
    private TextView textDiffSummary;
    private LinearLayout diffFilesContainer;

    public static ChatDiffFragment newInstance(String scId) {
        ChatDiffFragment fragment = new ChatDiffFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SC_ID, scId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        scId = args != null ? args.getString(ARG_SC_ID) : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_diffs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        textDiffSummary = view.findViewById(R.id.text_diff_summary);
        diffFilesContainer = view.findViewById(R.id.layout_diff_files);
        refreshDiffs();
    }

    public void refreshDiffs() {
        if (textDiffSummary == null || diffFilesContainer == null) {
            return;
        }

        Map<String, FileChangeTracker.FileChange> allChanges = FileChangeTracker.getAllRecentChanges();
        List<FileChangeTracker.FileChange> changes = new ArrayList<>(allChanges.values());
        changes.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        int count = changes.size();
        textDiffSummary.setText(getString(R.string.chat_diff_summary, count));
        diffFilesContainer.removeAllViews();

        if (count <= 0) {
            diffFilesContainer.addView(makeEmptyView());
            return;
        }

        int rendered = 0;
        for (FileChangeTracker.FileChange change : changes) {
            if (rendered >= MAX_DIFF_FILES) {
                break;
            }
            diffFilesContainer.addView(makeFileDiffView(change));
            rendered++;
        }
    }

    private View makeEmptyView() {
        TextView empty = new TextView(requireContext());
        empty.setText(R.string.chat_diff_empty);
        empty.setTextColor(color(R.color.chat_text_secondary));
        empty.setTextSize(13f);
        empty.setPadding(dp(10), dp(12), dp(10), dp(12));
        return empty;
    }

    private View makeFileDiffView(FileChangeTracker.FileChange change) {
        LinearLayout fileBlock = new LinearLayout(requireContext());
        fileBlock.setOrientation(LinearLayout.VERTICAL);
        fileBlock.setBackground(makeRoundedBackground(color(R.color.chat_diff_background), color(R.color.chat_border)));

        LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        blockParams.setMargins(0, 0, 0, dp(12));
        fileBlock.setLayoutParams(blockParams);

        VoidPortDiffService.DiffStats stats = VoidPortDiffService.stats(change.beforeContent, change.afterContent);
        TextView header = makeHeader(change.filePath, stats);
        fileBlock.addView(header);

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(requireContext());
        horizontalScrollView.setFillViewport(true);
        horizontalScrollView.setHorizontalScrollBarEnabled(true);

        LinearLayout codeRows = new LinearLayout(requireContext());
        codeRows.setOrientation(LinearLayout.VERTICAL);
        horizontalScrollView.addView(codeRows, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT
        ));

        appendDiffRows(codeRows, change);
        fileBlock.addView(horizontalScrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return fileBlock;
    }

    private TextView makeHeader(String filePath, VoidPortDiffService.DiffStats stats) {
        TextView header = makeCodeText();
        header.setText(String.format(Locale.US, "%s   +%d -%d", filePath, stats.added, stats.removed));
        header.setTextColor(color(R.color.chat_diff_line_text));
        header.setTextSize(12f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setBackgroundColor(color(R.color.chat_diff_header));
        header.setPadding(dp(10), dp(8), dp(10), dp(8));
        return header;
    }

    private void appendDiffRows(LinearLayout codeRows, FileChangeTracker.FileChange change) {
        List<VoidPortDiffService.ComputedDiff> diffs =
                VoidPortDiffService.findDiffs(change.beforeContent, change.afterContent);
        if (diffs.isEmpty()) {
            codeRows.addView(makeDiffRow("", "", " ", "No changes", R.color.chat_diff_background));
            return;
        }

        int rows = 0;
        for (VoidPortDiffService.ComputedDiff diff : diffs) {
            if (rows >= MAX_DIFF_ROWS) {
                codeRows.addView(makeDiffRow("", "", "...", "diff truncated", R.color.chat_diff_hunk));
                return;
            }
            String hunk = String.format(Locale.US, "@@ -%d,%d +%d,%d @@ %s",
                    diff.originalStartLine,
                    diff.removedLines(),
                    diff.startLine,
                    diff.addedLines(),
                    diff.type);
            codeRows.addView(makeDiffRow("", "", "", hunk, R.color.chat_diff_hunk));
            rows++;

            int oldLine = diff.originalStartLine;
            for (String line : splitLines(diff.originalCode)) {
                if (rows >= MAX_DIFF_ROWS) {
                    codeRows.addView(makeDiffRow("", "", "...", "diff truncated", R.color.chat_diff_hunk));
                    return;
                }
                codeRows.addView(makeDiffRow(String.valueOf(oldLine), "", "-", line, R.color.chat_diff_removed));
                oldLine++;
                rows++;
            }

            int newLine = diff.startLine;
            for (String line : splitLines(diff.code)) {
                if (rows >= MAX_DIFF_ROWS) {
                    codeRows.addView(makeDiffRow("", "", "...", "diff truncated", R.color.chat_diff_hunk));
                    return;
                }
                codeRows.addView(makeDiffRow("", String.valueOf(newLine), "+", line, R.color.chat_diff_added));
                newLine++;
                rows++;
            }
        }
    }

    private TextView makeDiffRow(String oldLine, String newLine, String marker, String code, int backgroundColorRes) {
        TextView row = makeCodeText();
        row.setText(String.format(Locale.US, "%4s %4s  %-3s %s",
                oldLine == null ? "" : oldLine,
                newLine == null ? "" : newLine,
                marker == null ? "" : marker,
                code == null ? "" : code));
        row.setTextColor(color(R.color.chat_diff_line_text));
        row.setBackgroundColor(color(backgroundColorRes));
        row.setPadding(dp(8), dp(3), dp(10), dp(3));
        row.setMinWidth(getResources().getDisplayMetrics().widthPixels - dp(24));
        row.setSingleLine(false);
        return row;
    }

    private TextView makeCodeText() {
        TextView textView = new TextView(requireContext());
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(11f);
        textView.setIncludeFontPadding(false);
        return textView;
    }

    private String[] splitLines(String value) {
        if (value == null || value.isEmpty()) {
            return new String[0];
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
            String[] trimmed = new String[lines.length - 1];
            System.arraycopy(lines, 0, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return lines;
    }

    private GradientDrawable makeRoundedBackground(int fillColor, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setStroke(dp(1), strokeColor);
        drawable.setCornerRadius(dp(6));
        return drawable;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(requireContext(), colorRes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
