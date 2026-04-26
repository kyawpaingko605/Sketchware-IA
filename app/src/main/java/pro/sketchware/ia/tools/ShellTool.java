package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import android.os.Environment;

public class ShellTool implements Tool {
    private volatile Process activeProcess;

    @Override
    public String getName() {
        return "run_shell_command";
    }

    @Override
    public String getDescription() {
        return "Executa comandos shell locais no Android. Use apenas para listar/verificar arquivos. "
                + "Arquivos internos do Sketchware como logic, view, file, resource e library NAO possuem extensao "
                + "e sao binarios criptografados. NAO use cat, echo, sed, grep, cp, mv, rm ou redirecionamento > neles. "
                + "Para ler use decrypt_sketchware_file. Para modificar use encrypt_sketchware_file. "
                + "Para listar arquivos do projeto use list_project_files.";
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

        String lower = command.toLowerCase();

        String[] blocked = {
                "cat ",
                "echo ",
                "sed ",
                "grep ",
                "rm ",
                "mv ",
                "cp ",
                "chmod ",
                "chown ",
                "dd ",
                ">",
                ">>"
        };

        for (String b : blocked) {
            if (lower.contains(b)) {
                return "Comando bloqueado por segurança.\n"
                        + "Arquivos do Sketchware são criptografados e não devem ser alterados via shell.\n"
                        + "Use:\n"
                        + "- list_project_files para listar\n"
                        + "- decrypt_sketchware_file para ler\n"
                        + "- encrypt_sketchware_file para salvar alterações";
            }
        }

        try {
            File sketchwareDir = new File(Environment.getExternalStorageDirectory(), ".sketchware");
            
            if (!sketchwareDir.exists()) {
                return "Erro: pasta .sketchware não encontrada em " + sketchwareDir.getAbsolutePath();
            }

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.directory(sketchwareDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeProcess = process;

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
        } finally {
            activeProcess = null;
        }
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public void cancelExecution() {
        Process process = activeProcess;
        if (process != null) {
            process.destroyForcibly();
        }
    }
}
