package pro.sketchware.util;

import android.os.Environment;

import java.io.File;
import java.io.RandomAccessFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilitário para descriptografar arquivos do Sketchware
 * Usa AES/CBC/PKCS5Padding com a chave "sketchwaresecure"
 */
public class SketchwareFileDecryptor {
    private static final String ENCRYPTION_KEY = "sketchwaresecure";
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    
    /**
     * Lê um arquivo do Sketchware (descriptografa se necessário ou lê diretamente se não criptografado)
     * @param filePath Caminho relativo do arquivo (ex: "data/601/file", "mysc/list/601/project")
     * @return Conteúdo do arquivo como String, ou null se houver erro
     */
    public static String decryptFile(String filePath) {
        try {
            // Remover extensões adicionadas incorretamente se o arquivo não tiver extensão original
            // Isso evita que o Groq adicione .json a arquivos criptografados sem extensão
            String normalizedPath = filePath;
            if (normalizedPath.endsWith(".json") || normalizedPath.endsWith(".xml")) {
                // Tentar primeiro sem a extensão (arquivos criptografados geralmente não têm extensão)
                File fileWithoutExt = resolveFilePath(normalizedPath.substring(0, normalizedPath.lastIndexOf(".")));
                if (fileWithoutExt != null && fileWithoutExt.exists()) {
                    // Se existe sem extensão, usar esse caminho
                    normalizedPath = normalizedPath.substring(0, normalizedPath.lastIndexOf("."));
                }
            }
            
            // Resolver o caminho completo
            File file = resolveFilePath(normalizedPath);
            
            if (file == null || !file.exists()) {
                return null;
            }
            
            // Verificar se o arquivo tem extensões que não devem ser criptografadas
            String fileName = file.getName();
            boolean formatKnownAsPlain = false;
            
            if (fileName.contains(".")) {
                String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                if (ext.equals("xml") || ext.equals("json") || ext.equals("txt") || 
                    ext.equals("java") || ext.equals("kt") || ext.equals("gradle") ||
                    ext.equals("properties") || ext.equals("pro") || ext.equals("xml")) {
                    formatKnownAsPlain = true;
                }
            }
            
            byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
            
            // Tentativa 1: Ler como texto puro ou JSON diretamente
            try {
                String possibleText = new String(fileBytes, "UTF-8");
                String trimmed = possibleText.trim();
                
                // Se parece um JSON perfeitamente válido, retornar formatado!
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                     return new org.json.JSONObject(trimmed).toString(4);
                } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                     return new org.json.JSONArray(trimmed).toString(4);
                }
                
                // Se é um formato sabidamente de texto puro ou tem cara de XML/código puro
                if (formatKnownAsPlain || trimmed.startsWith("<?xml") || trimmed.startsWith("<")) {
                    return possibleText;
                }
            } catch (Exception ignored) {
                // Se falhar e não der para ler utf-8, ignorar e seguir pra descriptografia
            }
            
            // Tentativa 2: Descriptografar via AES (padrão de arquivos protegidos do Sketchware/Mods)
            try {
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                byte[] key = ENCRYPTION_KEY.getBytes();
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
                
                byte[] decrypted = cipher.doFinal(fileBytes);
                String decryptedString = new String(decrypted);
                String trimmed = decryptedString.trim();
                
                // Tentar fazer pretty-print se o conteúdo descriptografado for JSON
                try {
                    if (trimmed.startsWith("{")) {
                        return new org.json.JSONObject(trimmed).toString(4);
                    } else if (trimmed.startsWith("[")) {
                        return new org.json.JSONArray(trimmed).toString(4);
                    }
                } catch (Exception ignore) {}
                
                return decryptedString;
            } catch (Exception cryptoEx) {
                // Se a criptografia falhar, e for um arquivo sem extensão que na verdade era texto
                if (!formatKnownAsPlain) {
                    try {
                        return new String(fileBytes, "UTF-8");
                    } catch (Exception ignore) {}
                }
            }
            
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Resolve o caminho relativo para o caminho absoluto do arquivo
     * Aceita qualquer caminho dentro de .sketchware
     */
    private static File resolveFilePath(String relativePath) {
        // Normalizar separadores
        String normalizedPath = relativePath.replace("\\", File.separator);
        
        // Remover separador inicial se existir
        if (normalizedPath.startsWith(File.separator)) {
            normalizedPath = normalizedPath.substring(1);
        }
        
        // Base path do Sketchware
        File baseDir = Environment.getExternalStorageDirectory();
        
        // Construir caminho completo: qualquer caminho relativo dentro de .sketchware
        String path = ".sketchware" + File.separator + normalizedPath;
        return new File(baseDir, path);
    }
    
    /**
     * Verifica se um arquivo existe antes de tentar descriptografar
     */
    public static boolean fileExists(String filePath) {
        File file = resolveFilePath(filePath);
        return file != null && file.exists();
    }
}

