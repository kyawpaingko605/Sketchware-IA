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

