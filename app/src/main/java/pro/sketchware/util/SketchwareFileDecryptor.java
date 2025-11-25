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
     * Descriptografa um arquivo do Sketchware
     * @param filePath Caminho relativo do arquivo (ex: "data/601/file", "mysc/list/601/project")
     * @return Conteúdo descriptografado como String, ou null se houver erro
     */
    public static String decryptFile(String filePath) {
        try {
            // Resolver o caminho completo
            File file = resolveFilePath(filePath);
            
            if (file == null || !file.exists()) {
                return null;
            }
            
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
            return new String(decrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Resolve o caminho relativo para o caminho absoluto do arquivo
     * Suporta os seguintes formatos:
     * - data/601/file
     * - data/601/logic
     * - data/601/resource
     * - data/601/view
     * - mysc/list/601/project
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
        
        // Construir caminho completo
        if (normalizedPath.startsWith("data" + File.separator)) {
            // Formato: data/601/file -> .sketchware/data/601/file
            String path = ".sketchware" + File.separator + normalizedPath;
            return new File(baseDir, path);
        } else if (normalizedPath.startsWith("mysc" + File.separator + "list" + File.separator)) {
            // Formato: mysc/list/601/project -> .sketchware/mysc/list/601/project
            String path = ".sketchware" + File.separator + normalizedPath;
            return new File(baseDir, path);
        } else {
            // Tentar como caminho relativo direto ao .sketchware
            String path = ".sketchware" + File.separator + normalizedPath;
            return new File(baseDir, path);
        }
    }
    
    /**
     * Verifica se um arquivo existe antes de tentar descriptografar
     */
    public static boolean fileExists(String filePath) {
        File file = resolveFilePath(filePath);
        return file != null && file.exists();
    }
    
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
            
            // Criptografar o conteúdo
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] key = ENCRYPTION_KEY.getBytes();
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
            
            byte[] encrypted = cipher.doFinal(content.trim().getBytes());
            
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
}

