package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import pro.sketchware.util.SketchwareFileEncryptor;

public class EncryptTool implements Tool {
    @Override
    public String getName() {
        return "encrypt_sketchware_file";
    }

    @Override
    public String getDescription() {
        return "Criptografa e salva conteúdo JSON em um arquivo do Sketchware, mantendo a compatibilidade do formato.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");
            JSONObject props = new JSONObject();
            props.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Caminho relativo do arquivo de projeto onde salvar (ex: 'data/logic')."));
            props.put("content", new JSONObject()
                    .put("type", "string")
                    .put("description", "O conteúdo JSON em formato string para ser criptografado e salvo."));
            params.put("properties", props);
            params.put("required", new JSONArray().put("file_path").put("content"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String filePath = args.optString("file_path", "").trim();
        String content = args.optString("content", "");

        if (filePath.isEmpty()) {
            return "Erro: Caminho do arquivo não especificado.";
        }

        boolean success = SketchwareFileEncryptor.encryptAndSaveFile(scId, filePath, content);
        if (success) {
            return "Sucesso: O arquivo " + filePath + " foi criptografado e salvo corretamente.";
        } else {
            return "Erro: Não foi possível criptografar ou salvar o arquivo " + filePath + ". Verifique se as permissões de escrita estão corretas.";
        }
    }
}
