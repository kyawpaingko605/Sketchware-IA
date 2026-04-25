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
        return "Criptografa e salva conteúdo JSON em arquivos internos binários do Sketchware, como logic, view, file, resource, library etc.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject props = new JSONObject();
            props.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Arquivo interno do projeto. Ex: logic, view, file, resource, library. Não use .json."));

            props.put("content", new JSONObject()
                    .put("type", "string")
                    .put("description", "JSON descriptografado em formato string para criptografar e salvar."));

            params.put("properties", props);
            params.put("required", new JSONArray().put("file_path").put("content"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String input = args.optString("file_path", "").trim();
        String content = args.optString("content", "");

        if (input.isEmpty()) {
            return "Erro: Caminho do arquivo não especificado.\nExemplo correto: logic, view, file, resource";
        }

        if (content.trim().isEmpty()) {
            return "Erro: Conteúdo não especificado.";
        }

        // Valida se realmente é JSON
        try {
            String trimmed = content.trim();
            if (trimmed.startsWith("{")) {
                new JSONObject(trimmed);
            } else if (trimmed.startsWith("[")) {
                new JSONArray(trimmed);
            } else {
                return "Erro: O conteúdo precisa ser JSON válido começando com { ou [.";
            }
        } catch (Exception e) {
            return "Erro: JSON inválido. Corrija o conteúdo antes de salvar.\nDetalhe: " + e.getMessage();
        }

        // Normaliza entrada
        input = input.replace("\\", "/");
        input = input.replace(".json", "");

        while (input.startsWith("./")) {
            input = input.substring(2);
        }

        input = input.replace("/sdcard/.sketchware/", "");
        input = input.replace("sdcard/.sketchware/", "");

        String prefix = "data/" + scId + "/";
        if (input.startsWith(prefix)) {
            input = input.substring(prefix.length());
        }

        if (input.startsWith("mysc/")) {
            return "Erro: arquivos em mysc/ não devem ser criptografados por esta ferramenta.\n"
                    + "Use esta ferramenta apenas para arquivos internos do projeto, como:\n"
                    + "- logic\n"
                    + "- view\n"
                    + "- file\n"
                    + "- resource\n"
                    + "- library\n"
                    + "- permission";
        }

        if (input.contains("..") || input.startsWith("/")) {
            return "Erro: caminho inválido.";
        }

        boolean success = SketchwareFileEncryptor.encryptAndSaveFile(scId, input, content);

        if (success) {
            return "Sucesso: o arquivo '" + input + "' foi criptografado e salvo corretamente.";
        }

        return "Erro: Não foi possível criptografar ou salvar o arquivo '" + input + "'.\n"
                + "Verifique permissões de escrita e se o arquivo pertence ao projeto " + scId + ".";
    }
}
