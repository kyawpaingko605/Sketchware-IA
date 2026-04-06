package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import pro.sketchware.util.SketchwareFileDecryptor;

public class DecryptTool implements Tool {
    @Override
    public String getName() {
        return "decrypt_sketchware_file";
    }

    @Override
    public String getDescription() {
        return "Descriptografa um arquivo interno do Sketchware (ex: logic, view, file, data etc) e retorna seu JSON original.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");
            JSONObject props = new JSONObject();
            props.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Caminho relativo do arquivo de projeto a descriptografar (ex: 'data/logic')."));
            params.put("properties", props);
            params.put("required", new JSONArray().put("file_path"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String filePath = args.optString("file_path", "").trim();
        if (filePath.isEmpty()) {
            return "Erro: Caminho do arquivo não especificado.";
        }

        String decrypted = SketchwareFileDecryptor.decryptFile(scId, filePath);
        if (decrypted != null) {
            return "Conteúdo descriptografado de " + filePath + ":\n" + decrypted;
        } else {
            return "Erro: Não foi possível descriptografar o arquivo " + filePath + ". Verifique se ele existe e o ID do projeto está correto.";
        }
    }
}
