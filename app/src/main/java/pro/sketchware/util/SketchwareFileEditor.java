package pro.sketchware.util;

import pro.sketchware.network.MorphClient;

/**
 * Utilitário para editar arquivos do Sketchware usando Morph
 * Descriptografa, edita com Morph e criptografa de volta
 */
public class SketchwareFileEditor {
    
    /**
     * Resultado da edição de arquivo
     */
    public static class EditResult {
        public final boolean success;
        public final String initialContent;
        public final String editedContent;
        public final String errorMessage;
        
        private EditResult(boolean success, String initialContent, String editedContent, String errorMessage) {
            this.success = success;
            this.initialContent = initialContent;
            this.editedContent = editedContent;
            this.errorMessage = errorMessage;
        }
        
        public static EditResult success(String initialContent, String editedContent) {
            return new EditResult(true, initialContent, editedContent, null);
        }
        
        public static EditResult error(String errorMessage) {
            return new EditResult(false, null, null, errorMessage);
        }
        
        public static EditResult error(String initialContent, String errorMessage) {
            return new EditResult(false, initialContent, null, errorMessage);
        }
    }
    
    /**
     * Edita um arquivo do Sketchware usando Morph
     * @param filePath Caminho relativo do arquivo (ex: "data/601/logic", "mysc/list/601/project")
     * @param instructions Instruções claras sobre o que modificar
     * @param codeEdit JSON editado ou trecho a ser modificado (opcional, se vazio usa initialContent)
     * @return EditResult com o resultado da edição
     */
    public static EditResult editFile(String filePath, String instructions, String codeEdit) {
        try {
            // Descriptografar arquivo JSON atual
            String initialContent = SketchwareFileDecryptor.decryptFile(filePath);
            
            if (initialContent == null || initialContent.isEmpty()) {
                return EditResult.error("Erro: Arquivo não encontrado ou vazio: " + filePath);
            }
            
            // Usar Morph para editar o JSON
            String editedContent = MorphClient.getInstance().applyCodeEdit(
                initialContent,
                codeEdit.isEmpty() ? initialContent : codeEdit,
                instructions
            );
            
            if (editedContent == null || editedContent.isEmpty()) {
                return EditResult.error(initialContent, "Erro: Morph não retornou conteúdo editado para: " + filePath);
            }
            
            // Criptografar e salvar arquivo editado
            boolean saved = SketchwareFileEncryptor.encryptAndSaveFile(filePath, editedContent);
            
            if (!saved) {
                return EditResult.error(initialContent, "Erro: Não foi possível salvar o arquivo modificado: " + filePath);
            }
            
            // Rastrear mudança para gerar diffs
            FileChangeTracker.trackChange(filePath, initialContent, editedContent);
            
            return EditResult.success(initialContent, editedContent);
            
        } catch (Exception e) {
            e.printStackTrace();
            return EditResult.error("Erro ao modificar arquivo: " + e.getMessage());
        }
    }
    
    /**
     * Edita um arquivo do Sketchware usando Morph (versão simplificada sem codeEdit)
     * @param filePath Caminho relativo do arquivo
     * @param instructions Instruções claras sobre o que modificar
     * @return EditResult com o resultado da edição
     */
    public static EditResult editFile(String filePath, String instructions) {
        return editFile(filePath, instructions, "");
    }
}

