package pro.sketchware.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Busca semântica de arquivos e trechos relevantes
 * Encontra arquivos relacionados sem precisar enviar tudo
 */
public class SemanticFileSearcher {
    
    /**
     * Resultado de uma busca semântica
     */
    public static class SearchResult {
        public final String filePath;
        public final String relevance;
        public final String snippet;
        public final double score;
        
        public SearchResult(String filePath, String relevance, String snippet, double score) {
            this.filePath = filePath;
            this.relevance = relevance;
            this.snippet = snippet;
            this.score = score;
        }
    }
    
    /**
     * Busca arquivos relevantes baseado em palavras-chave
     * @param query Query de busca
     * @param scId ID do projeto
     * @return Lista de arquivos relevantes
     */
    public static List<SearchResult> searchRelevantFiles(String query, String scId) {
        List<SearchResult> results = new ArrayList<>();
        
        // Normalizar query
        String normalizedQuery = query.toLowerCase();
        String[] keywords = normalizedQuery.split("\\s+");
        
        // Lista de arquivos comuns do projeto
        String[] commonFiles = {
            "mysc/list/" + scId + "/project",
            "data/" + scId + "/logic",
            "data/" + scId + "/view",
            "data/" + scId + "/file",
            "data/" + scId + "/library",
            "data/" + scId + "/resource"
        };
        
        // Buscar em cada arquivo
        for (String filePath : commonFiles) {
            try {
                String content = SketchwareFileDecryptor.decryptFile(filePath);
                if (content != null && !content.isEmpty()) {
                    double score = calculateRelevanceScore(content, keywords);
                    if (score > 0) {
                        String snippet = extractRelevantSnippet(content, keywords);
                        String relevance = determineRelevance(normalizedQuery, content);
                        results.add(new SearchResult(filePath, relevance, snippet, score));
                    }
                }
            } catch (Exception e) {
                // Ignorar erros de arquivos não encontrados
            }
        }
        
        // Ordenar por relevância (maior score primeiro)
        results.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Retornar apenas os top 5 mais relevantes
        return results.size() > 5 ? results.subList(0, 5) : results;
    }
    
    /**
     * Calcula o score de relevância baseado em palavras-chave
     */
    private static double calculateRelevanceScore(String content, String[] keywords) {
        String lowerContent = content.toLowerCase();
        double score = 0.0;
        
        for (String keyword : keywords) {
            if (keyword.length() < 2) continue;
            
            // Contar ocorrências
            int count = countOccurrences(lowerContent, keyword);
            score += count * 1.0;
            
            // Bonus para palavras-chave importantes
            if (isImportantKeyword(keyword)) {
                score += 5.0;
            }
        }
        
        return score;
    }
    
    /**
     * Extrai um trecho relevante do conteúdo
     */
    private static String extractRelevantSnippet(String content, String[] keywords) {
        String lowerContent = content.toLowerCase();
        int bestIndex = -1;
        int bestScore = 0;
        
        // Procurar a melhor posição (onde mais keywords aparecem)
        for (int i = 0; i < content.length() - 200; i += 50) {
            String window = lowerContent.substring(i, Math.min(i + 200, content.length()));
            int score = 0;
            for (String keyword : keywords) {
                if (window.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        
        if (bestIndex >= 0) {
            int start = Math.max(0, bestIndex - 50);
            int end = Math.min(content.length(), bestIndex + 250);
            String snippet = content.substring(start, end);
            return snippet.length() > 200 ? "..." + snippet.substring(0, 200) + "..." : snippet;
        }
        
        // Fallback: primeiras 200 caracteres
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }
    
    /**
     * Determina a relevância do arquivo para a query
     */
    private static String determineRelevance(String query, String content) {
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("project") || lowerQuery.contains("nome") || lowerQuery.contains("package")) {
            if (content.contains("my_ws_name") || content.contains("my_sc_pkg_name")) {
                return "Project configuration";
            }
        }
        
        if (lowerQuery.contains("logic") || lowerQuery.contains("java") || lowerQuery.contains("code")) {
            if (content.contains("parameters") || content.contains("opCode")) {
                return "Logic/Java code";
            }
        }
        
        if (lowerQuery.contains("view") || lowerQuery.contains("layout") || lowerQuery.contains("xml")) {
            if (content.contains("<") && content.contains(">")) {
                return "View/Layout XML";
            }
        }
        
        if (lowerQuery.contains("library") || lowerQuery.contains("dependency")) {
            if (content.contains("library") || content.contains("dependency")) {
                return "Library/Dependencies";
            }
        }
        
        return "General content";
    }
    
    /**
     * Conta ocorrências de uma substring
     */
    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    /**
     * Verifica se é uma palavra-chave importante
     */
    private static boolean isImportantKeyword(String keyword) {
        String[] important = {
            "activity", "fragment", "view", "layout", "component",
            "function", "method", "class", "package", "import",
            "error", "exception", "compile", "build"
        };
        
        for (String imp : important) {
            if (keyword.contains(imp) || imp.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Busca arquivos relacionados a um arquivo específico
     * Por exemplo, se está editando MainActivity, encontra layouts relacionados
     */
    public static List<String> findRelatedFiles(String filePath, String scId) {
        List<String> related = new ArrayList<>();
        
        // Extrair nome do arquivo
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        String baseName = fileName.replaceAll("\\.[^.]+$", "");
        
        // Buscar arquivos com nomes similares
        String[] searchPaths = {
            "data/" + scId + "/view",
            "data/" + scId + "/file",
            "data/" + scId + "/logic"
        };
        
        for (String searchPath : searchPaths) {
            try {
                String content = SketchwareFileDecryptor.decryptFile(searchPath);
                if (content != null && content.contains(baseName)) {
                    related.add(searchPath);
                }
            } catch (Exception e) {
                // Ignorar
            }
        }
        
        return related;
    }
}

