package com.besome.sketch.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.content.ClipboardManager;
import android.content.ClipData;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;

import mod.hey.studios.util.CompileLogHelper;
import mod.hey.studios.util.Helper;
import mod.hey.studios.util.ErrorFixHelper;
import mod.jbk.diagnostic.CompileErrorSaver;
import mod.jbk.util.AddMarginOnApplyWindowInsetsListener;
import pro.sketchware.databinding.CompileLogBinding;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.network.GroqClient;
import io.noties.markwon.Markwon;
import android.os.Environment;
import java.io.File;
 
 import mod.hey.studios.project.ProjectTracker;

public class CompileLogActivity extends BaseAppCompatActivity {

    private static final String PREFERENCE_WRAPPED_TEXT = "wrapped_text";
    private static final String PREFERENCE_USE_MONOSPACED_FONT = "use_monospaced_font";
    private static final String PREFERENCE_FONT_SIZE = "font_size";
    private CompileErrorSaver compileErrorSaver;
    private SharedPreferences logViewerPreferences;

    private CompileLogBinding binding;
    // Store sc_id for later use (AI Fix button)
    private String scId;
    // Armazena o erro atual para passar ao ErrorFixHelper
    private String currentErrorText;

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = CompileLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.optionsLayout,
                new AddMarginOnApplyWindowInsetsListener(WindowInsetsCompat.Type.navigationBars(), WindowInsetsCompat.CONSUMED));

        logViewerPreferences = getPreferences(Context.MODE_PRIVATE);

        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));

        if (getIntent().getBooleanExtra("showingLastError", false)) {
            binding.topAppBar.setTitle("Last compile log");
        } else {
            binding.topAppBar.setTitle("Compile log");
        }

        // Resolve sc_id automaticamente (Intent -> ProjectTracker -> scan de diretórios)
        String scIdFromIntent = getIntent().getStringExtra("sc_id");
        this.scId = resolveScId();
        if (scIdFromIntent == null && this.scId != null) {
            SketchwareUtil.toast("ID do projeto detectado: " + this.scId);
        }

        if (this.scId != null) {
            compileErrorSaver = new CompileErrorSaver(this.scId);
        }

        if (compileErrorSaver != null && compileErrorSaver.logFileExists()) {
            binding.clearButton.setOnClickListener(v -> {
                if (compileErrorSaver.logFileExists()) {
                    compileErrorSaver.deleteSavedLogs();
                    getIntent().removeExtra("error");
                    SketchwareUtil.toast("Compile logs have been cleared.");
                } else {
                    SketchwareUtil.toast("No compile logs found.");
                }

                setErrorText();
            });
        }

        final String wrapTextLabel = "Wrap text";
        final String monospacedFontLabel = "Monospaced font";
        final String fontSizeLabel = "Font size";

        PopupMenu options = new PopupMenu(this, binding.formatButton);
        options.getMenu().add(wrapTextLabel).setCheckable(true).setChecked(getWrappedTextPreference());
        options.getMenu().add(monospacedFontLabel).setCheckable(true).setChecked(getMonospacedFontPreference());
        options.getMenu().add(fontSizeLabel);

        options.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getTitle().toString()) {
                case wrapTextLabel -> {
                    menuItem.setChecked(!menuItem.isChecked());
                    toggleWrapText(menuItem.isChecked());
                }
                case monospacedFontLabel -> {
                    menuItem.setChecked(!menuItem.isChecked());
                    toggleMonospacedText(menuItem.isChecked());
                }
                case fontSizeLabel -> changeFontSizeDialog();
                default -> {
                    return false;
                }
            }

            return true;
        });

        binding.formatButton.setOnClickListener(v -> options.show());

        applyLogViewerPreferences();

        setErrorText();

        // AI Explain button: analisa o log via Groq e mostra em diálogo com scroll
        if (binding.aiExplainButton != null) {
            binding.aiExplainButton.setOnClickListener(v -> explainLogWithAI());
        }
        // AI Fix button: delega para ErrorFixHelper
        if (binding.aiFixButton != null) {
            binding.aiFixButton.setOnClickListener(v -> ErrorFixHelper.showProjectLogic(this, scId, currentErrorText));
        }
    }

    private void setErrorText() {
        String error = getIntent().getStringExtra("error");
        if (error == null) error = compileErrorSaver.getLogsFromFile();
        if (error == null) {
            binding.noContentLayout.setVisibility(View.VISIBLE);
            binding.optionsLayout.setVisibility(View.GONE);
            return;
        }

        // guarda o erro atual para uso do ErrorFixHelper
        currentErrorText = error;

        binding.optionsLayout.setVisibility(View.VISIBLE);
        binding.noContentLayout.setVisibility(View.GONE);

        binding.tvCompileLog.setText(CompileLogHelper.getColoredLogs(this, error));
        binding.tvCompileLog.setTextIsSelectable(true);
    }

    private void applyLogViewerPreferences() {
        toggleWrapText(getWrappedTextPreference());
        toggleMonospacedText(getMonospacedFontPreference());
        binding.tvCompileLog.setTextSize(getFontSizePreference());
    }

    private boolean getWrappedTextPreference() {
        return logViewerPreferences.getBoolean(PREFERENCE_WRAPPED_TEXT, false);
    }

    private boolean getMonospacedFontPreference() {
        return logViewerPreferences.getBoolean(PREFERENCE_USE_MONOSPACED_FONT, true);
    }

    private int getFontSizePreference() {
        return logViewerPreferences.getInt(PREFERENCE_FONT_SIZE, 11);
    }

    private void toggleWrapText(boolean isChecked) {
        logViewerPreferences.edit().putBoolean(PREFERENCE_WRAPPED_TEXT, isChecked).apply();

        if (isChecked) {
            binding.errVScroll.removeAllViews();
            if (binding.tvCompileLog.getParent() != null) {
                ((ViewGroup) binding.tvCompileLog.getParent()).removeView(binding.tvCompileLog);
            }
            binding.errVScroll.addView(binding.tvCompileLog);
        } else {
            binding.errVScroll.removeAllViews();
            if (binding.tvCompileLog.getParent() != null) {
                ((ViewGroup) binding.tvCompileLog.getParent()).removeView(binding.tvCompileLog);
            }
            binding.errHScroll.removeAllViews();
            binding.errHScroll.addView(binding.tvCompileLog);
            binding.errVScroll.addView(binding.errHScroll);
        }
    }

    private void toggleMonospacedText(boolean isChecked) {
        logViewerPreferences.edit().putBoolean(PREFERENCE_USE_MONOSPACED_FONT, isChecked).apply();

        if (isChecked) {
            binding.tvCompileLog.setTypeface(Typeface.MONOSPACE);
        } else {
            binding.tvCompileLog.setTypeface(Typeface.DEFAULT);
        }
    }

    private void changeFontSizeDialog() {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(10); //Must not be less than setValue(), which is currently 11 in compile_log.xml
        picker.setMaxValue(70);
        picker.setWrapSelectorWheel(false);
        picker.setValue(getFontSizePreference());

        LinearLayout layout = new LinearLayout(this);
        layout.addView(picker, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select font size")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    logViewerPreferences.edit().putInt(PREFERENCE_FONT_SIZE, picker.getValue()).apply();

                    binding.tvCompileLog.setTextSize((float) picker.getValue());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void explainLogWithAI() {
        CharSequence cs = binding.tvCompileLog.getText();
        String logText = cs != null ? cs.toString().trim() : "";
        if (logText.isEmpty()) {
            SketchwareUtil.toastError("No log to analyze.");
            return;
        }
        if (!SketchwareUtil.isConnected()) {
            SketchwareUtil.toastError("No internet connection.");
            return;
        }

        // Diálogo simples de progresso
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        var progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Analyzing with AI...")
                .setView(progressBar)
                .setCancelable(false)
                .create();
        progressDialog.show();

        String prompt = "Analyze the following Android compilation log and clearly explain the cause(s) of the errors and how to fix them, with examples when appropriate.\n\nLog:\n\n" + logText;

        new Thread(() -> {
            try {
                String response = GroqClient.getInstance().sendMessage(prompt);
                runOnUiThread(() -> {
                    try { progressDialog.dismiss(); } catch (Exception ignored) {}
                    showScrollableDialog("AI Explanation", response);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    try { progressDialog.dismiss(); } catch (Exception ignored) {}
                    SketchwareUtil.showAnErrorOccurredDialog(this, e.getMessage());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    try { progressDialog.dismiss(); } catch (Exception ignored) {}
                    SketchwareUtil.showAnErrorOccurredDialog(this, "Failed to process AI response.");
                });
            }
        }).start();
    }

    private void showScrollableDialog(String title, String content) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        TextView textView = new TextView(this);
        // Renderiza Markdown usando Markwon
        Markwon markwon = Markwon.create(this);
        markwon.setMarkdown(textView, content);
        textView.setTextIsSelectable(true);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        textView.setPadding(pad, pad, pad, pad);

        scrollView.addView(textView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        var dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy", null)
                .create();

        dialog.setOnShowListener(d -> {
            var btnCopy = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
            btnCopy.setOnClickListener(v -> {
                try {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("ia_response", content));
                        SketchwareUtil.toast("Copied");
                    }
                } catch (Exception ignored) {}
            });
        });

        dialog.show();
    }

    // Resolve automaticamente o sc_id atual:
    // 1) Intent extra, 2) ProjectTracker.SC_ID, 3) diretório com compile_log mais recente,
    // 4) diretório com logic mais recente, 5) projeto mais recente em .sketchware/mysc/list
    private String resolveScId() {
        try {
            String fromIntent = getIntent().getStringExtra("sc_id");
            if (fromIntent != null && !fromIntent.trim().isEmpty()) return fromIntent;
        } catch (Exception ignored) {}

        try {
            if (ProjectTracker.SC_ID != null && !ProjectTracker.SC_ID.trim().isEmpty()) return ProjectTracker.SC_ID;
        } catch (Exception ignored) {}

        // Procura em /.sketchware/data/<sc_id>/compile_log
        File dataRoot = new File(Environment.getExternalStorageDirectory(), ".sketchware/data");
        if (dataRoot.isDirectory()) {
            File[] candidates = dataRoot.listFiles(File::isDirectory);
            if (candidates != null && candidates.length > 0) {
                File latest = null;
                long latestMod = -1;
                for (File dir : candidates) {
                    File log = new File(dir, "compile_log");
                    long m = log.exists() ? log.lastModified() : -1;
                    if (m > latestMod) { latestMod = m; latest = dir; }
                }
                if (latest != null && latestMod > 0) return latest.getName();

                // Segunda tentativa: arquivo logic mais recente
                latest = null; latestMod = -1;
                for (File dir : candidates) {
                    File logic = new File(dir, "logic");
                    long m = logic.exists() ? logic.lastModified() : -1;
                    if (m > latestMod) { latestMod = m; latest = dir; }
                }
                if (latest != null && latestMod > 0) return latest.getName();
            }
        }

        // Procura em /.sketchware/mysc/list/<sc_id>/project com último modificado
        File listRoot = new File(Environment.getExternalStorageDirectory(), ".sketchware/mysc/list");
        if (listRoot.isDirectory()) {
            File[] dirs = listRoot.listFiles(File::isDirectory);
            if (dirs != null && dirs.length > 0) {
                File latest = null; long mod = -1;
                for (File dir : dirs) {
                    File proj = new File(dir, "project");
                    long m = proj.exists() ? proj.lastModified() : -1;
                    if (m > mod) { mod = m; latest = dir; }
                }
                if (latest != null && mod > 0) return latest.getName();
            }
        }

        return null;
    }

    // Lógica do botão Error Fix movida para ErrorFixHelper
}
