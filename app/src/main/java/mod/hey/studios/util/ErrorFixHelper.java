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
import pro.sketchware.network.GroqClient;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.BlockBean;
import a.a.a.hC;
import a.a.a.eC;
import a.a.a.jC;
import io.noties.markwon.Markwon;

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
                        .setTitle("AI ERRO FIX")
                        .setMessage("No logic found in the project.")
                        .setPositiveButton("aplicar", null)
                        .show();
                return;
            }
            
            // Gerar prompt para Groq e enviar automaticamente
            String groqPrompt = generateGroqPrompt(projectLogic, compileErrorText);
            sendToGroqAndShowResponse(activity, groqPrompt, scId);
        } catch (Exception e) {
            SketchwareUtil.showAnErrorOccurredDialog(activity, e.getMessage());
        }
    }

    /**
     * Gera um prompt em inglês para o cliente Groq, baseado na lógica do projeto e no erro de compilação.
     * @param projectLogic Lógica do projeto em formato JSON
     * @param compileError Erro de compilação (pode ser null)
     * @return Prompt formatado para o Groq
     */
    private static String generateGroqPrompt(String projectLogic, String compileError) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert Android developer specializing in Sketchware app development. ");
        prompt.append("I need you to analyze the project logic and compilation error to provide a fix.\n\n");

        prompt.append("COMPILATION ERROR:\n");
        prompt.append(compileError != null ? compileError : "No compilation error provided.").append("\n\n");

        prompt.append("PROJECT LOGIC:\n");
        prompt.append(projectLogic).append("\n\n");

        prompt.append("Analyze the project logic and compilation error above. ");
        prompt.append("Identify the problematic blocks and provide the corrected code.\n\n");

        prompt.append("REQUIRED RESPONSE FORMAT (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"id\": \"block_id_here\",\n");
        prompt.append("  \"corrected_parameters\": \"java code\"\n");
        prompt.append("}\n\n");

        prompt.append("INSTRUCTIONS:\n");
        prompt.append("Provide your analysis and corrections in the exact JSON format specified above.\n");

        return prompt.toString();
    }

    public static String getLastCompileError() {
        return lastCompileError;
    }

    /**
     * Modifica o bloco com ID "11" para mostrar toast "ola mundo" e salva
     * Agora usa a classe BlockModifier para maior flexibilidade
     */
    private static void modifyBlock11AndSave(Activity activity, String scId) {
        BlockModifier blockModifier = new BlockModifier(activity, scId);
        blockModifier.modifyBlockAndSave("11", "ola mundo");
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


    /**
     * Envia o prompt para o Groq e mostra a resposta em um novo diálogo
     * @param activity Activity atual
     * @param prompt Prompt para enviar ao Groq
     * @param scId ID do projeto
     */
    private static void sendToGroqAndShowResponse(Activity activity, String prompt, String scId) {
        // Mostrar diálogo de carregamento
        var loadingDialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("AI ERRO FIX")
                .setMessage("Enviando projeto para análise...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // Executar em thread separada para não bloquear a UI
        new Thread(() -> {
            try {
                GroqClient groqClient = GroqClient.getInstance();
                String response = groqClient.sendMessage(prompt);
                
                // Voltar para a thread principal para atualizar a UI
                activity.runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    showGroqResponseDialog(activity, response, scId);
                });
                
            } catch (Exception e) {
                // Voltar para a thread principal para mostrar erro
                activity.runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("Erro ao conectar com Groq")
                            .setMessage("Erro: " + e.getMessage() + "\n\nVerifique sua conexão e configurações do Groq.")
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        }).start();
    }

    /**
     * Mostra a resposta do Groq em um diálogo
     * @param activity Activity atual
     * @param response Resposta do Groq
     * @param scId ID do projeto
     */
    private static void showGroqResponseDialog(Activity activity, String response, String scId) {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);

        TextView textView = new TextView(activity);
        // Renderiza Markdown usando Markwon
        Markwon markwon = Markwon.create(activity);
        markwon.setMarkdown(textView, response);
        textView.setTextIsSelectable(true);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        textView.setPadding(pad, pad, pad, pad);

        scrollView.addView(textView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        var dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("AI ERRO FIX - Resposta do Groq")
                .setView(scrollView)
                .setPositiveButton("Aplicar Correções", (d, which) -> {
                    // Tentar aplicar as correções do Groq
                    applyGroqCorrections(activity, response, scId);
                })
                .setNeutralButton("Copy Response", null)
                .setNegativeButton("Close", null)
                .create();

        dialog.setOnShowListener(d -> {
            var btnCopy = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
            btnCopy.setOnClickListener(v -> {
                try {
                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("groq_response", response));
                        SketchwareUtil.toast("Resposta copiada para área de transferência");
                    }
                } catch (Exception ignored) {}
            });
        });

        dialog.show();
    }

    /**
     * Tenta aplicar as correções sugeridas pelo Groq
     * @param activity Activity atual
     * @param response Resposta do Groq em formato JSON
     * @param scId ID do projeto
     */
    private static void applyGroqCorrections(Activity activity, String response, String scId) {
        try {
            // Mostrar loading durante o processamento
            var loadingDialog = new MaterialAlertDialogBuilder(activity)
                    .setTitle("AI ERRO FIX")
                    .setMessage("Aplicando correções...")
                    .setCancelable(false)
                    .create();
            loadingDialog.show();

            // Executar em thread separada
            new Thread(() -> {
                try {
                    // Extrair JSON da resposta da IA
                    String jsonResponse = extractJsonFromResponse(response);
                    
                    if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                        activity.runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            SketchwareUtil.toast("Nenhum JSON válido encontrado na resposta da IA");
                        });
                        return;
                    }

                    // Fazer parse do JSON
                    Gson gson = new Gson();
                    Map<String, Object> responseData = gson.fromJson(jsonResponse, Map.class);
                    
                    if (responseData.containsKey("id") && responseData.containsKey("corrected_parameters")) {
                        String blockId = (String) responseData.get("id");
                        String correctedCode = (String) responseData.get("corrected_parameters");
                        
                        // Validar se o bloco existe antes de tentar modificar
                        BlockModifier blockModifier = new BlockModifier(activity, scId);
                        
                        if (!blockModifier.blockExists(blockId)) {
                            activity.runOnUiThread(() -> {
                                loadingDialog.dismiss();
                                SketchwareUtil.toast("Bloco com ID " + blockId + " não encontrado no projeto");
                            });
                            return;
                        }
                        
                        // Aplicar a correção
                        boolean success = blockModifier.modifyBlockAndSave(blockId, correctedCode);
                        
                        // Voltar para a thread principal
                        activity.runOnUiThread(() -> {
                            try {
                                loadingDialog.dismiss();
                                if (success) {
                                    new MaterialAlertDialogBuilder(activity)
                                            .setTitle("Correção Aplicada!")
                                            .setMessage("Bloco " + blockId + " foi corrigido com sucesso!\n\nCódigo aplicado: " + correctedCode)
                                            .setPositiveButton("OK", null)
                                            .show();
                                } else {
                                    SketchwareUtil.toast("Erro ao aplicar correções no bloco " + blockId);
                                }
                            } catch (Exception uiException) {
                                // Se der erro na UI, pelo menos mostrar toast de sucesso
                                if (success) {
                                    SketchwareUtil.toast("Correção aplicada com sucesso!");
                                }
                            }
                        });
                    } else {
                        activity.runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            SketchwareUtil.toast("Formato JSON inválido: campos 'id' e 'corrected_parameters' são obrigatórios");
                        });
                    }
                } catch (Exception e) {
                    activity.runOnUiThread(() -> {
                        try {
                            loadingDialog.dismiss();
                        } catch (Exception uiException) {
                            // Se der erro na UI, apenas logar
                            e.printStackTrace();
                        }
                    });
                }
            }).start();

        } catch (Exception e) {
            SketchwareUtil.toast("Erro ao iniciar aplicação de correções: " + e.getMessage());
        }
    }

    /**
     * Extrai JSON da resposta da IA (pode estar misturado com texto markdown)
     * @param response Resposta completa da IA
     * @return JSON extraído ou null se não encontrado
     */
    private static String extractJsonFromResponse(String response) {
        if (response == null) return null;
        
        // Procurar por JSON na resposta (pode estar em blocos de código markdown)
        String[] patterns = {
            "```json\\s*\\{.*?\\}\\s*```",  // JSON em bloco de código
            "```\\s*\\{.*?\\}\\s*```",     // JSON em bloco de código sem especificar json
            "\\{.*?\\}"                    // JSON simples
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(response);
            
            if (m.find()) {
                String json = m.group(0);
                // Limpar markdown se necessário
                json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                return json;
            }
        }
        
        // Fallback: procurar por chaves simples
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        
        return null;
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