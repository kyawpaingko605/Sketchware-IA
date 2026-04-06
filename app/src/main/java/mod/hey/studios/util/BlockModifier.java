package mod.hey.studios.util;

import android.app.Activity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.BlockBean;
import a.a.a.hC;
import a.a.a.eC;
import a.a.a.jC;

/**
 * Classe para modificar blocos específicos em projetos Sketchware
 * Permite alterar os parâmetros de qualquer bloco por ID
 */
public class BlockModifier {
    
    private Activity activity;
    private String scId;
    
    public BlockModifier(Activity activity, String scId) {
        this.activity = activity;
        this.scId = scId;
    }
    
    /**
     * Modifica um bloco específico com o ID fornecido em um arquivo e evento específicos.
     * @param javaName Nome do arquivo Java (ex: MainActivity.java)
     * @param eventName Nome do contexto/evento (ex: onCreate)
     * @param blockId ID do bloco a ser modificado
     * @param newParameters Lista de novos parâmetros para o bloco
     * @return true se a modificação foi bem-sucedida, false caso contrário
     */
    public boolean modifyBlockAndSave(String javaName, String eventName, String blockId, java.util.List<String> newParameters) {
        try {
            if (activity == null) {
                showToast("Erro: Activity não encontrada");
                return false;
            }
            
            if (scId == null || scId.trim().isEmpty()) {
                showToast("Erro: ID do projeto não encontrado");
                return false;
            }
            
            if (javaName == null || eventName == null || blockId == null) {
                showToast("Erro: Parâmetros de localização do bloco incompletos");
                return false;
            }

            // Obter os gerenciadores
            hC projectManager = jC.b(scId);
            eC logicManager = jC.a(scId);
            
            if (projectManager == null || logicManager == null) {
                showToast("Erro: Não foi possível acessar os dados do projeto");
                return false;
            }

            Map<String, ArrayList<BlockBean>> fileLogic = logicManager.b(javaName);
            if (fileLogic == null || fileLogic.isEmpty()) {
                showToast("Erro: Lógica do arquivo " + javaName + " não encontrada");
                return false;
            }

            // Busca flexível de evento (Sketchware usa o formato ID_nomeEvento)
            ArrayList<BlockBean> blocks = fileLogic.get(eventName);
            String actualEventKey = eventName;
            
            if (blocks == null) {
                // Tenta encontrar uma chave que termine com _eventName ou contenha eventName
                for (String key : fileLogic.keySet()) {
                    if (key.endsWith("_" + eventName) || key.equals(eventName)) {
                        blocks = fileLogic.get(key);
                        actualEventKey = key;
                        break;
                    }
                }
            }

            if (blocks == null) {
                showToast("Erro: Evento '" + eventName + "' não encontrado em " + javaName);
                return false;
            }

            boolean blockFound = false;
            for (BlockBean block : blocks) {
                if (blockId.equals(String.valueOf(block.id))) {
                    // Modificar os parâmetros do bloco de forma inteligente
                    if (newParameters != null) {
                        // Se recebemos uma lista, substituímos os parâmetros correspondentes
                        for (int i = 0; i < newParameters.size(); i++) {
                            if (i < block.parameters.size()) {
                                block.parameters.set(i, newParameters.get(i));
                            } else {
                                block.parameters.add(newParameters.get(i));
                            }
                        }
                    }
                    
                    blockFound = true;
                    break;
                }
            }

            if (blockFound) {
                // Registrar no logicManager o evento alterado
                logicManager.a(javaName, actualEventKey, blocks);
                
                // Salvar as alterações no disco
                logicManager.a(); 
                
                showToast("Bloco " + blockId + " em " + javaName + " (" + eventName + ") modificado!");
                return true;
            } else {
                showToast("Bloco ID " + blockId + " não encontrado em " + eventName);
                return false;
            }

        } catch (Exception e) {
            showToast("Erro ao modificar bloco: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mantém compatibilidade com chamadas antigas que usam apenas ID e texto único
     */
    public boolean modifyBlockAndSave(String blockId, String newText) {
        // Tenta encontrar o bloco em qualquer lugar (comportamento antigo, mas desencorajado)
        java.util.List<String> params = new java.util.ArrayList<>();
        params.add(newText);
        
        try {
            hC pm = jC.b(scId);
            ArrayList<ProjectFileBean> files = pm.b();
            for (ProjectFileBean file : files) {
                String jName = file.getJavaName();
                Map<String, ArrayList<BlockBean>> logic = jC.a(scId).b(jName);
                for (String event : logic.keySet()) {
                    for (BlockBean b : logic.get(event)) {
                        if (blockId.equals(String.valueOf(b.id))) {
                            return modifyBlockAndSave(jName, event, blockId, params);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
    
    /**
     * Método auxiliar para mostrar toast
     * @param message Mensagem a ser exibida
     */
    private void showToast(final String message) {
        if (activity != null) {
            activity.runOnUiThread(() -> {
                try {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {}
            });
        }
    }
    
    /**
     * Verifica se um bloco existe em um local específico
     */
    public boolean blockExists(String javaName, String eventName, String blockId) {
        try {
            if (scId == null || javaName == null || eventName == null || blockId == null) return false;
            
            eC logicManager = jC.a(scId);
            Map<String, ArrayList<BlockBean>> fileLogic = logicManager.b(javaName);
            if (fileLogic == null) return false;
            
            ArrayList<BlockBean> blocks = fileLogic.get(eventName);
            if (blocks == null) return false;
            
            for (BlockBean block : blocks) {
                if (blockId.equals(String.valueOf(block.id))) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean blockExists(String blockId) {
        // Fallback para busca global
        try {
            hC pm = jC.b(scId);
            for (ProjectFileBean f : pm.b()) {
                Map<String, ArrayList<BlockBean>> logic = jC.a(scId).b(f.getJavaName());
                for (String event : logic.keySet()) {
                    for (BlockBean b : logic.get(event)) {
                        if (blockId.equals(String.valueOf(b.id))) return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
