package pro.sketchware.util;

import android.os.Environment;

import java.io.File;
import java.io.RandomAccessFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilitário para criptografar e salvar arquivos do Sketchware
 * Usa AES/CBC/PKCS5Padding com a chave "sketchwaresecure"
 */
public class SketchwareFileEncryptor {
    private static final String ENCRYPTION_KEY = "sketchwaresecure";
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    
    /**
     * Criptografa e salva um arquivo do Sketchware
     * @param filePath Caminho relativo do arquivo (ex: "data/601/logic", "mysc/list/601/project")
     * @param content Conteúdo a ser criptografado e salvo
     * @return true se salvou com sucesso, false caso contrário
     */
    public static boolean encryptAndSaveFile(String filePath, String content) {
        try {
            // Resolver o caminho completo
            File file = resolveFilePath(filePath);
            
            if (file == null) {
                return false;
            }
            
            // Criar diretório pai se não existir
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            String contentToEncrypt = content.trim();
            // Tentar minificar se for JSON (remover formatação/espaços adicionados pela IA)
            try {
                if (contentToEncrypt.startsWith("{")) {
                    contentToEncrypt = new org.json.JSONObject(contentToEncrypt).toString();
                } else if (contentToEncrypt.startsWith("[")) {
                    contentToEncrypt = new org.json.JSONArray(contentToEncrypt).toString();
                }
            } catch (Exception e) {
                // Se não for JSON válido, manter texto original
            }
            
            // Verificar se o formato é de texto plano conhecido
            String fileName = file.getName();
            boolean formatKnownAsPlain = false;
            if (fileName.contains(".")) {
                String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                if (ext.equals("xml") || ext.equals("json") || ext.equals("txt") || 
                    ext.equals("java") || ext.equals("kt") || ext.equals("gradle") ||
                    ext.equals("properties") || ext.equals("pro") || ext.equals("html")) {
                    formatKnownAsPlain = true;
                }
            }
            
            // Decidir se o arquivo PRECISA de criptografia AES
            boolean needsEncryption = false;
            
            if (formatKnownAsPlain) {
                needsEncryption = false;
            } else if (file.exists()) {
                try {
                    byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                    if (fileBytes.length == 0) {
                        needsEncryption = !fileName.contains(".") && (filePath.contains("mysc/") || filePath.contains("data/"));
                    } else {
                        String possibleText = new String(fileBytes, "UTF-8").trim();
                        // Se o conteúdo real do arquivo já parecer texto puro ou JSON legível, NÃO criptografamos
                        boolean looksLikeText = (possibleText.startsWith("{") && possibleText.endsWith("}")) || 
                                                (possibleText.startsWith("[") && possibleText.endsWith("]")) ||
                                                possibleText.startsWith("<?xml") || possibleText.startsWith("<");
                        needsEncryption = !looksLikeText;
                    }
                } catch (Exception e) {
                    needsEncryption = true; // Se falhou ler como UTF-8, assume que está binário/criptografado
                }
            } else {
                // Se o arquivo é novo e não tem extensão, costuma ser project file protegido 
                needsEncryption = !fileName.contains(".") && (filePath.contains("mysc/") || filePath.contains("data/"));
            }
            
            if (!needsEncryption) {
                // Salvar de volta como texto puro para evitar corromper projetos que não usam AES nativamente
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    raf.setLength(0);
                    // Não forçar toString formatado para arquivos Java (senão ele minificava acima)
                    raf.write(content.getBytes("UTF-8"));
                }
                return true;
            }
            
            // Criptografar o conteúdo
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] key = ENCRYPTION_KEY.getBytes();
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
            
            byte[] encrypted = cipher.doFinal(contentToEncrypt.getBytes("UTF-8"));
            
            // Salvar arquivo criptografado
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                raf.setLength(0);
                raf.write(encrypted);
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
     * Verifica se um arquivo existe antes de tentar salvar
     */
    public static boolean fileExists(String filePath) {
        File file = resolveFilePath(filePath);
        return file != null && file.exists();
    }
}

