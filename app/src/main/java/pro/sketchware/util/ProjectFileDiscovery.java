package pro.sketchware.util;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Descobre automaticamente a estrutura de arquivos e pastas do projeto
 */
public class ProjectFileDiscovery {
    
    /**
     * Resultado da descoberta de arquivos
     */
    public static class FileInfo {
        public final String path;
        public final String name;
        public final boolean isDirectory;
        public final boolean isEncrypted;
        public final long size;
        
        public FileInfo(String path, String name, boolean isDirectory, boolean isEncrypted, long size) {
            this.path = path;
            this.name = name;
            this.isDirectory = isDirectory;
            this.isEncrypted = isEncrypted;
            this.size = size;
        }
    }
    
    /**
     * Lista todos os arquivos e pastas de um diretório do projeto
     * @param scId ID do projeto (usado apenas quando relativePath é null)
     * @param relativePath Caminho relativo dentro de .sketchware (ou null para listar toda a estrutura)
     * @return Lista de arquivos e pastas encontrados
     */
    public static List<FileInfo> discoverFiles(String scId, String relativePath) {
        List<FileInfo> files = new ArrayList<>();
        
        try {
            File baseDir = Environment.getExternalStorageDirectory();
            File projectBase = new File(baseDir, ".sketchware");
            
            File targetDir;
            if (relativePath == null || relativePath.isEmpty()) {
                // Listar toda a estrutura do .sketchware
                targetDir = projectBase;
            } else {
                // Normalizar caminho
                String normalizedPath = relativePath.replace("\\", File.separator);
                if (normalizedPath.startsWith(File.separator)) {
                    normalizedPath = normalizedPath.substring(1);
                }
                
                // Qualquer caminho relativo dentro de .sketchware
                targetDir = new File(projectBase, normalizedPath);
            }
            
            if (targetDir.exists() && targetDir.isDirectory()) {
                listFilesRecursive(targetDir, projectBase, files, scId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return files;
    }
    
    /**
     * Lista arquivos recursivamente
     */
    private static void listFilesRecursive(File dir, File projectBase, List<FileInfo> files, String scId) {
        try {
            File[] children = dir.listFiles();
            if (children == null) return;
            
            for (File file : children) {
                // Calcular caminho relativo
                String relativePath = projectBase.toPath().relativize(file.toPath()).toString().replace(File.separator, "/");
                
                boolean isEncrypted = isEncryptedFile(file);
                
                FileInfo info = new FileInfo(
                    relativePath,
                    file.getName(),
                    file.isDirectory(),
                    isEncrypted,
                    file.isFile() ? file.length() : 0
                );
                
                files.add(info);
                
                // Se for diretório, listar recursivamente (limitado a 2 níveis de profundidade)
                if (file.isDirectory() && getDepth(file, projectBase) < 3) {
                    listFilesRecursive(file, projectBase, files, scId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Calcula a profundidade de um arquivo em relação à base
     */
    private static int getDepth(File file, File base) {
        int depth = 0;
        File current = file;
        while (current != null && !current.equals(base)) {
            depth++;
            current = current.getParentFile();
        }
        return depth;
    }
    
    /**
     * Verifica se um arquivo está criptografado
     * Arquivos criptografados geralmente não têm extensão ou têm extensão específica
     */
    private static boolean isEncryptedFile(File file) {
        if (file.isDirectory()) {
            return false;
        }
        
        String name = file.getName();
        // Arquivos sem extensão ou com extensões conhecidas do Sketchware são criptografados
        if (!name.contains(".")) {
            return true;
        }
        
        // Arquivos XML, JSON, etc. geralmente não são criptografados
        String ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
        return !ext.equals("xml") && !ext.equals("json") && !ext.equals("txt") && 
               !ext.equals("java") && !ext.equals("kt") && !ext.equals("gradle");
    }
    
    /**
     * Busca arquivos por nome ou padrão
     * @param scId ID do projeto
     * @param pattern Padrão de busca (nome do arquivo ou parte dele)
     * @return Lista de arquivos encontrados
     */
    public static List<FileInfo> searchFiles(String scId, String pattern) {
        List<FileInfo> allFiles = discoverFiles(scId, null);
        List<FileInfo> matches = new ArrayList<>();
        
        String lowerPattern = pattern.toLowerCase();
        
        for (FileInfo file : allFiles) {
            if (file.name.toLowerCase().contains(lowerPattern) || 
                file.path.toLowerCase().contains(lowerPattern)) {
                matches.add(file);
            }
        }
        
        return matches;
    }
    
    /**
     * Obtém informações sobre um arquivo específico
     * @param scId ID do projeto (não usado, mantido para compatibilidade)
     * @param relativePath Caminho relativo do arquivo dentro de .sketchware
     * @return Informações do arquivo ou null se não encontrado
     */
    public static FileInfo getFileInfo(String scId, String relativePath) {
        try {
            File baseDir = Environment.getExternalStorageDirectory();
            File projectBase = new File(baseDir, ".sketchware");
            
            String normalizedPath = relativePath.replace("\\", File.separator);
            if (normalizedPath.startsWith(File.separator)) {
                normalizedPath = normalizedPath.substring(1);
            }
            
            // Qualquer caminho relativo dentro de .sketchware
            File targetFile = new File(projectBase, normalizedPath);
            
            if (targetFile.exists()) {
                boolean isEncrypted = isEncryptedFile(targetFile);
                return new FileInfo(
                    relativePath,
                    targetFile.getName(),
                    targetFile.isDirectory(),
                    isEncrypted,
                    targetFile.isFile() ? targetFile.length() : 0
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}

