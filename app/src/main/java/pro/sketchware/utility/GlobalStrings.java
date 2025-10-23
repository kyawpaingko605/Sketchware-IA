package pro.sketchware.utility;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GlobalStrings {

    private static final String TAG = "GlobalStrings";
    private static final String ASSET_PATH = "localization/strings.xml";
    private static final String EXTERNAL_PATH = Environment.getExternalStorageDirectory() + "/sketchware/localization/strings.xml";

    /**
     * Garante que o arquivo existe e retorna o caminho
     */
    public static String ensureFileExists(Context context) {
        File externalFile = new File(EXTERNAL_PATH);

        if (externalFile.exists()) {
            Log.d(TAG, "Arquivo já existe: " + externalFile.getAbsolutePath());
            return externalFile.getAbsolutePath();
        }

        // Criar diretórios
        File parentDir = externalFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                Log.e(TAG, "Falha ao criar diretório: " + parentDir.getAbsolutePath());
                return null;
            }
        }

        // Copiar do assets
        try {
            copyAssetToExternal(context, ASSET_PATH, externalFile);
            Log.d(TAG, "Arquivo copiado com sucesso: " + externalFile.getAbsolutePath());
            return externalFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Erro ao copiar arquivo", e);
            return null;
        }
    }

    /**
     * Carrega TODAS as strings do arquivo externo em um Map
     */
    public static Map<String, String> loadExternalStrings(Context context) {
        String filePath = ensureFileExists(context);
        if (filePath == null) {
            Log.w(TAG, "Arquivo externo não encontrado");
            return new HashMap<>();
        }

        Map<String, String> strings = new HashMap<>();
        File file = new File(filePath);

        try (FileInputStream fis = new FileInputStream(file)) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, null);

            int eventType = parser.getEventType();
            boolean inString = false;
            String name = null;
            StringBuilder text = new StringBuilder();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("string".equals(parser.getName())) {
                            inString = true;
                            name = parser.getAttributeValue(null, "name");
                            text.setLength(0);
                        }
                        break;

                    case XmlPullParser.TEXT:
                        if (inString) {
                            text.append(parser.getText());
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if ("string".equals(parser.getName()) && inString && name != null) {
                            strings.put(name, text.toString().trim());
                            inString = false;
                            name = null;
                        }
                        break;
                }
                eventType = parser.next();
            }

            Log.d(TAG, "Carregadas " + strings.size() + " strings externas");
            return strings;

        } catch (Exception e) {
            Log.e(TAG, "Erro ao ler strings externas", e);
            return new HashMap<>();
        }
    }

    private static void copyAssetToExternal(Context context, String assetPath, File destFile) throws IOException {
        AssetManager assetManager = context.getAssets();

        try (InputStream in = assetManager.open(assetPath);
             FileOutputStream out = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    public static String getExternalPath() {
        return EXTERNAL_PATH;
    }
}
