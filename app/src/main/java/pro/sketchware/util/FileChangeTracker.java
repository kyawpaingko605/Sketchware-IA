package pro.sketchware.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Rastreia mudanças nos arquivos do projeto
 * Armazena versões antes/depois para gerar diffs
 */
public class FileChangeTracker {
    private static final Map<String, FileChange> changes = new HashMap<>();
    
    /**
     * Representa uma mudança em um arquivo
     */
    public static class FileChange {
        public final String filePath;
        public final String beforeContent;
        public final String afterContent;
        public final long timestamp;
        
        public FileChange(String filePath, String beforeContent, String afterContent) {
            this.filePath = filePath;
            this.beforeContent = beforeContent;
            this.afterContent = afterContent;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Gera um diff simples do arquivo
         */
        public String generateDiff() {
            if (beforeContent == null || afterContent == null) {
                return "No diff available";
            }
            
            if (beforeContent.equals(afterContent)) {
                return "No changes";
            }
            
            // Diff simples linha por linha
            String[] beforeLines = beforeContent.split("\n");
            String[] afterLines = afterContent.split("\n");
            
            StringBuilder diff = new StringBuilder();
            diff.append("--- ").append(filePath).append(" (before)\n");
            diff.append("+++ ").append(filePath).append(" (after)\n");
            
            int maxLines = Math.max(beforeLines.length, afterLines.length);
            for (int i = 0; i < maxLines; i++) {
                if (i < beforeLines.length && i < afterLines.length) {
                    if (!beforeLines[i].equals(afterLines[i])) {
                        diff.append("- ").append(beforeLines[i]).append("\n");
                        diff.append("+ ").append(afterLines[i]).append("\n");
                    }
                } else if (i < beforeLines.length) {
                    diff.append("- ").append(beforeLines[i]).append("\n");
                } else if (i < afterLines.length) {
                    diff.append("+ ").append(afterLines[i]).append("\n");
                }
            }
            
            return diff.toString();
        }
    }
    
    /**
     * Registra uma mudança em um arquivo
     */
    public static void trackChange(String filePath, String beforeContent, String afterContent) {
        changes.put(filePath, new FileChange(filePath, beforeContent, afterContent));
    }
    
    /**
     * Obtém a última mudança de um arquivo
     */
    public static FileChange getLastChange(String filePath) {
        return changes.get(filePath);
    }
    
    /**
     * Obtém todas as mudanças recentes
     */
    public static Map<String, FileChange> getAllRecentChanges() {
        return new HashMap<>(changes);
    }
    
    /**
     * Limpa o histórico de mudanças
     */
    public static void clearChanges() {
        changes.clear();
    }
    
    /**
     * Gera um resumo de todas as mudanças recentes
     */
    public static String generateChangesSummary() {
        if (changes.isEmpty()) {
            return "No recent changes";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("**Recent File Changes:**\n\n");
        
        for (FileChange change : changes.values()) {
            summary.append("**File:** ").append(change.filePath).append("\n");
            summary.append("**Time:** ").append(new java.util.Date(change.timestamp)).append("\n");
            summary.append("**Diff:**\n```\n");
            summary.append(change.generateDiff());
            summary.append("\n```\n\n");
        }
        
        return summary.toString();
    }
}

