package mod.hey.studios.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import mod.hey.studios.util.Helper;
import pro.sketchware.utility.SketchwareUtil;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.BlockBean;
import a.a.a.hC;
import a.a.a.eC;
import a.a.a.jC;

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
        
        try {
            // Usar os recursos do LogicEditorActivity para processar toda a lógica do projeto
            String projectLogic = processAllProjectLogic(scId);
            if (projectLogic.trim().isEmpty() || projectLogic.equals("{}")) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Project Logic")
                        .setMessage("No logic found in the project.")
                        .setPositiveButton("Close", null)
                        .show();
                return;
            }
            
            // Mostrar JSON diretamente no diálogo
            showScrollableDialog(activity, "Project Logic - JSON", projectLogic, scId);
        } catch (Exception e) {
            SketchwareUtil.showAnErrorOccurredDialog(activity, e.getMessage());
        }
    }

    public static String getLastCompileError() {
        return lastCompileError;
    }

    /**
     * Modifica o bloco com ID "11" para mostrar toast "ola mundo" e salva
     */
    private static void modifyBlock11AndSave(Activity activity, String scId) {
        try {
            if (scId == null || scId.trim().isEmpty()) {
                Toast.makeText(activity, "Erro: ID do projeto não encontrado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obter os gerenciadores
            hC projectManager = jC.b(scId);
            eC logicManager = jC.a(scId);
            
            if (projectManager == null || logicManager == null) {
                Toast.makeText(activity, "Erro: Não foi possível acessar os dados do projeto", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obter todos os arquivos do projeto
            ArrayList<ProjectFileBean> projectFiles = projectManager.b();
            if (projectFiles == null || projectFiles.isEmpty()) {
                Toast.makeText(activity, "Erro: Nenhum arquivo encontrado no projeto", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean blockFound = false;

            // Procurar e modificar o bloco com ID "11"
            for (ProjectFileBean projectFile : projectFiles) {
                String javaName = projectFile.getJavaName();
                if (javaName == null || javaName.trim().isEmpty()) {
                    continue;
                }

                Map<String, ArrayList<BlockBean>> fileLogic = logicManager.b(javaName);
                if (fileLogic != null && !fileLogic.isEmpty()) {
                    for (Map.Entry<String, ArrayList<BlockBean>> entry : fileLogic.entrySet()) {
                        ArrayList<BlockBean> blocks = entry.getValue();
                        
                        if (blocks != null && !blocks.isEmpty()) {
                            for (BlockBean block : blocks) {
                                if ("11".equals(String.valueOf(block.id))) {
                                    // Modificar os parâmetros para mostrar toast "ola mundo"
                                    block.parameters.clear();
                                    block.parameters.add("ola mundo");
                                    
                                    blockFound = true;
                                    break;
                                }
                            }
                        }
                        if (blockFound) break;
                    }
                }
                if (blockFound) break;
            }

            if (blockFound) {
                // Salvar as alterações
                logicManager.a(); // Salvar as mudanças
                Toast.makeText(activity, "Bloco 11 modificado com sucesso! Toast: ola mundo", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity, "Bloco com ID 11 não encontrado", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(activity, "Erro ao modificar bloco: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    /**
     * Processa toda a lógica do projeto usando os recursos do LogicEditorActivity
     * @param scId ID do projeto
     * @return JSON bruto contendo toda a lógica do projeto
     */
    private static String processAllProjectLogic(String scId) {
        try {
            // Usar os recursos do LogicEditorActivity para obter todos os arquivos do projeto
            hC projectManager = jC.b(scId);
            eC logicManager = jC.a(scId);
            
            if (projectManager == null || logicManager == null) {
                return "{}";
            }
            
            ArrayList<ProjectFileBean> projectFiles = projectManager.b();
            if (projectFiles == null || projectFiles.isEmpty()) {
                return "{}";
            }
            
            Map<String, Object> projectData = new HashMap<>();
            
            // Processar cada arquivo do projeto
            for (ProjectFileBean projectFile : projectFiles) {
                String javaName = projectFile.getJavaName();
                if (javaName == null || javaName.trim().isEmpty()) {
                    continue;
                }
                
                Map<String, Object> fileData = new HashMap<>();
                
                // Obter todos os eventos/blocos para este arquivo
                Map<String, ArrayList<BlockBean>> fileLogic = logicManager.b(javaName);
                if (fileLogic != null && !fileLogic.isEmpty()) {
                    for (Map.Entry<String, ArrayList<BlockBean>> entry : fileLogic.entrySet()) {
                        String eventName = entry.getKey();
                        ArrayList<BlockBean> blocks = entry.getValue();
                        
                        if (blocks != null && !blocks.isEmpty()) {
                            ArrayList<Map<String, Object>> eventBlocks = new ArrayList<>();
                            
                            // Processar cada bloco - apenas id e parameters
                            for (BlockBean block : blocks) {
                                Map<String, Object> blockData = new HashMap<>();
                                blockData.put("id", block.id);
                                blockData.put("parameters", block.parameters);
                                
                                eventBlocks.add(blockData);
                            }
                            
                            fileData.put(eventName, eventBlocks);
                        }
                    }
                }
                
                projectData.put(javaName, fileData);
            }
            
            // Converter para JSON bruto
            Gson gson = new Gson();
            return gson.toJson(projectData);
            
        } catch (Exception e) {
            return "{}";
        }
    }

    private static void showScrollableDialog(Activity activity, String title, String content, String scId) {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);

        TextView textView = new TextView(activity);
        textView.setText(content);
        textView.setTextIsSelectable(true);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        textView.setPadding(pad, pad, pad, pad);

        scrollView.addView(textView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        var dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton("Close", (d, which) -> {
                    // Modificar bloco com ID "11" e salvar
                    modifyBlock11AndSave(activity, scId);
                })
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