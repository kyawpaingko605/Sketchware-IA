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
     * Modifica um bloco específico com o ID fornecido e substitui seus parâmetros pelo texto fornecido
     * @param blockId ID do bloco a ser modificado
     * @param newText Novo texto para os parâmetros do bloco
     * @return true se a modificação foi bem-sucedida, false caso contrário
     */
    public boolean modifyBlockAndSave(String blockId, String newText) {
        try {
            if (activity == null) {
                showToast("Erro: Activity não encontrada");
                return false;
            }
            
            if (scId == null || scId.trim().isEmpty()) {
                showToast("Erro: ID do projeto não encontrado");
                return false;
            }
            
            if (blockId == null || blockId.trim().isEmpty()) {
                showToast("Erro: ID do bloco não fornecido");
                return false;
            }
            
            if (newText == null) {
                newText = "";
            }

            // Obter os gerenciadores
            hC projectManager = jC.b(scId);
            eC logicManager = jC.a(scId);
            
            if (projectManager == null || logicManager == null) {
                showToast("Erro: Não foi possível acessar os dados do projeto");
                return false;
            }

            // Obter todos os arquivos do projeto
            ArrayList<ProjectFileBean> projectFiles = projectManager.b();
            if (projectFiles == null || projectFiles.isEmpty()) {
                showToast("Erro: Nenhum arquivo encontrado no projeto");
                return false;
            }

            boolean blockFound = false;

            // Procurar e modificar o bloco com o ID especificado
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
                                if (blockId.equals(String.valueOf(block.id))) {
                                    // Modificar os parâmetros do bloco
                                    block.parameters.clear();
                                    block.parameters.add(newText);
                                    
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
                showToast("Bloco " + blockId + " modificado com sucesso! Texto: " + newText);
                return true;
            } else {
                showToast("Bloco com ID " + blockId + " não encontrado");
                return false;
            }

        } catch (Exception e) {
            showToast("Erro ao modificar bloco: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Método auxiliar para mostrar toast
     * @param message Mensagem a ser exibida
     */
    private void showToast(String message) {
        if (activity != null) {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Verifica se um bloco com o ID especificado existe no projeto
     * @param blockId ID do bloco a ser verificado
     * @return true se o bloco existe, false caso contrário
     */
    public boolean blockExists(String blockId) {
        try {
            if (scId == null || scId.trim().isEmpty()) {
                return false;
            }
            
            hC projectManager = jC.b(scId);
            eC logicManager = jC.a(scId);
            
            if (projectManager == null || logicManager == null) {
                return false;
            }

            ArrayList<ProjectFileBean> projectFiles = projectManager.b();
            if (projectFiles == null || projectFiles.isEmpty()) {
                return false;
            }

            // Procurar o bloco com o ID especificado
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
                                if (blockId.equals(String.valueOf(block.id))) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
