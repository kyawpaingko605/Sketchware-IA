package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import android.os.Environment;

public class ShellTool implements Tool {
    @Override
    public String getName() {
        return "run_shell_command";
    }

    @Override
    public String getDescription() {
        return "Executa um comando shell local no ambiente Android quando as outras ferramentas do assistente nao cobrirem a solicitacao.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");
            JSONObject props = new JSONObject();
            props.put("command", new JSONObject()
                    .put("type", "string")
                    .put("description", "Comando shell a executar no dispositivo."));
            params.put("properties", props);
            params.put("required", new JSONArray().put("command"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String command = args.optString("command", "").trim();
        if (command.isEmpty()) {
            return "Nenhum comando foi executado porque o texto do comando veio vazio.";
        }

        try {
            File sketchwareDir = new File(Environment.getExternalStorageDirectory(), ".sketchware");
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.directory(sketchwareDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "O comando excedeu o tempo maximo de execucao.";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            int exitCode = process.exitValue();
            String finalOutput = output.toString().trim();
            String normalizedOutput = finalOutput.isEmpty() ? "(sem saida)" : (finalOutput.length() > 3000 ? finalOutput.substring(0, 3000) : finalOutput);

            return "Comando executado: " + command + "\nExit code: " + exitCode + "\nSaida:\n" + normalizedOutput;
        } catch (Exception e) {
            return "Falha ao executar shell: " + e.getMessage();
        }
    }
}
