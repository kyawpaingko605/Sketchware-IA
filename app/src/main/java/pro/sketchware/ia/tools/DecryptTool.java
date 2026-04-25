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
        return "Descriptografa arquivos internos binários do Sketchware, como logic, view, file, resource, library etc.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject props = new JSONObject();
            props.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Arquivo interno do projeto. Ex: logic, view, file, resource ou data/601/logic. Não use .json."));

            params.put("properties", props);
            params.put("required", new JSONArray().put("file_path"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String input = args.optString("file_path", "").trim();

        if (input.isEmpty()) {
            return "Erro: Caminho do arquivo não especificado.\nExemplo correto: logic, view, file, resource";
        }

        // Normaliza entrada
        input = input.replace("\\", "/");
        input = input.replace(".json", "");

        while (input.startsWith("./")) {
            input = input.substring(2);
        }

        // Remove prefixos errados comuns
        input = input.replace("/sdcard/.sketchware/", "");
        input = input.replace("sdcard/.sketchware/", "");

        // Se veio data/601/logic, vira logic
        String prefix = "data/" + scId + "/";
        if (input.startsWith(prefix)) {
            input = input.substring(prefix.length());
        }

        // Se veio mysc/601/config, mantém separado, pois pode não ser criptografado igual
        if (input.startsWith("mysc/")) {
            return "Esse caminho parece ser arquivo de configuração Gradle/mysc, não arquivo interno criptografado do projeto.\n"
                    + "Para descriptografar use exemplos como:\n"
                    + "- logic\n"
                    + "- view\n"
                    + "- file\n"
                    + "- resource\n"
                    + "- library\n"
                    + "- permission";
        }

        // Bloqueia caminhos suspeitos ou inexistentes
        if (input.contains("..")) {
            return "Erro: caminho inválido.";
        }

        String decrypted = SketchwareFileDecryptor.decryptFile(scId, input);

        if (decrypted == null || decrypted.trim().isEmpty()) {
            return "Erro: Não foi possível descriptografar o arquivo '" + input + "'.\n\n"
                    + "Use o nome real sem extensão. Exemplos:\n"
                    + "- logic\n"
                    + "- view\n"
                    + "- file\n"
                    + "- resource\n"
                    + "- library\n"
                    + "- permission\n\n"
                    + "Não use:\n"
                    + "- data.json\n"
                    + "- logic.json\n"
                    + "- /sdcard/.sketchware/601/data/file.json";
        }

        return "Conteúdo descriptografado de " + input + ":\n" + decrypted;
    }
}
