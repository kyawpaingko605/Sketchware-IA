package pro.sketchware.util;

import java.util.HashMap;
import java.util.Map;

import pro.sketchware.activities.chat.port.VoidPortDiffService;

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

            return VoidPortDiffService.toUnifiedDiff(filePath, beforeContent, afterContent, 8000);
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

