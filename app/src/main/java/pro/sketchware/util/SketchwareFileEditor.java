package pro.sketchware.util;

import pro.sketchware.network.MorphClient;

/**
 * Edits project files through Morph while respecting the current project's scope.
 */
public class SketchwareFileEditor {

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

    public static EditResult editFile(String scId, String filePath, String instructions, String codeEdit) {
        try {
            String initialContent = SketchwareFileDecryptor.decryptFile(scId, filePath);
            if (initialContent == null || initialContent.isEmpty()) {
                return EditResult.error("Erro: Arquivo não encontrado ou vazio: " + filePath);
            }

            String editedContent = MorphClient.getInstance().applyCodeEdit(
                    initialContent,
                    codeEdit.isEmpty() ? initialContent : codeEdit,
                    instructions
            );

            if (editedContent == null || editedContent.isEmpty()) {
                return EditResult.error(initialContent, "Erro: Morph não retornou conteúdo editado para: " + filePath);
            }

            boolean saved = SketchwareFileEncryptor.encryptAndSaveFile(scId, filePath, editedContent);
            if (!saved) {
                return EditResult.error(initialContent, "Erro: Não foi possível salvar o arquivo modificado: " + filePath);
            }

            FileChangeTracker.trackChange(filePath, initialContent, editedContent);
            return EditResult.success(initialContent, editedContent);
        } catch (Exception e) {
            e.printStackTrace();
            return EditResult.error("Erro ao modificar arquivo: " + e.getMessage());
        }
    }

    public static EditResult editFile(String scId, String filePath, String instructions) {
        return editFile(scId, filePath, instructions, "");
    }
}
