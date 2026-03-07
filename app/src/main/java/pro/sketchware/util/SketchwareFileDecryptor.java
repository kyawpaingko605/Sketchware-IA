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
            
            // Verificar se o arquivo está criptografado
            // Arquivos com extensões conhecidas (xml, json, txt, java, kt, gradle) geralmente não são criptografados
            String fileName = file.getName();
            boolean isEncrypted = true;
            
            if (fileName.contains(".")) {
                String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                // Arquivos com essas extensões geralmente não são criptografados
                if (ext.equals("xml") || ext.equals("json") || ext.equals("txt") || 
                    ext.equals("java") || ext.equals("kt") || ext.equals("gradle") ||
                    ext.equals("properties") || ext.equals("pro")) {
                    isEncrypted = false;
                }
            }
            
            if (!isEncrypted) {
                // Tentar ler como texto direto (arquivo não criptografado)
                try {
                    byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
                    return new String(content, "UTF-8");
                } catch (Exception e) {
                    // Se falhar, tentar descriptografar
                    isEncrypted = true;
                }
            }
            
            if (isEncrypted) {
                // Descriptografar o arquivo
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                byte[] key = ENCRYPTION_KEY.getBytes();
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
                
                byte[] encrypted;
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    encrypted = new byte[(int) raf.length()];
                    raf.readFully(encrypted);
                }
                
                byte[] decrypted = cipher.doFinal(encrypted);
                String decryptedString = new String(decrypted);
                
                // Tentar fazer pretty-print se for JSON
                try {
                    String trimmed = decryptedString.trim();
                    if (trimmed.startsWith("{")) {
                        return new org.json.JSONObject(trimmed).toString(4);
                    } else if (trimmed.startsWith("[")) {
                        return new org.json.JSONArray(trimmed).toString(4);
                    }
                } catch (Exception e) {
                    // Se não for JSON válido, retornar como texto normal
                }
                
                return decryptedString;
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

