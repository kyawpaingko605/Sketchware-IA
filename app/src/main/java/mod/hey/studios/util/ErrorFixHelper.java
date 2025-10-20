package mod.hey.studios.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.RandomAccessFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.noties.markwon.Markwon;
import pro.sketchware.utility.SketchwareUtil;

public final class ErrorFixHelper {

    // Futuras atualizações podem usar este valor
    private static String lastCompileError;

    private ErrorFixHelper() { }

    // Método de conveniência mantendo compatibilidade
    public static void showProjectLogic(Activity activity, String scId) {
        showProjectLogic(activity, scId, null);
    }

    public static void showProjectLogic(Activity activity, String scId, String compileErrorText) {
        if (activity == null) return;
        // Guarda o erro mais recente para usos futuros
        lastCompileError = compileErrorText;

        if (scId == null || scId.trim().isEmpty()) {
            SketchwareUtil.toastError("Project ID not found.");
            return;
        }
        String logicPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/.sketchware/data/" + scId + "/logic";
        File logicFile = new File(logicPath);
        if (!logicFile.exists() || logicFile.length() <= 0) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("Project Logic")
                    .setMessage("Logic file not found.")
                    .setPositiveButton("Close", null)
                    .show();
            return;
        }
        try {
            String content = decryptSketchwareFile(logicFile);
            if (content == null || content.trim().isEmpty()) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Project Logic")
                        .setMessage("Failed to decrypt logic file.")
                        .setPositiveButton("Close", null)
                        .show();
                return;
            }
            showScrollableDialog(activity, "Project Logic", content);
        } catch (Exception e) {
            SketchwareUtil.showAnErrorOccurredDialog(activity, e.getMessage());
        }
    }

    public static String getLastCompileError() {
        return lastCompileError;
    }

    private static void showScrollableDialog(Activity activity, String title, String content) {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);

        TextView textView = new TextView(activity);
        Markwon markwon = Markwon.create(activity);
        markwon.setMarkdown(textView, content);
        textView.setTextIsSelectable(true);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        textView.setPadding(pad, pad, pad, pad);

        scrollView.addView(textView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        var dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy", null)
                .create();

        dialog.setOnShowListener(d -> {
            var btnCopy = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
            btnCopy.setOnClickListener(v -> {
                try {
                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("ia_response", content));
                        SketchwareUtil.toast("Copied");
                    }
                } catch (Exception ignored) {}
            });
        });

        dialog.show();
    }

    private static String decryptSketchwareFile(File file) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] key = "sketchwaresecure".getBytes();
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
        byte[] encrypted;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            encrypted = new byte[(int) raf.length()];
            raf.readFully(encrypted);
        }
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted);
    }
}