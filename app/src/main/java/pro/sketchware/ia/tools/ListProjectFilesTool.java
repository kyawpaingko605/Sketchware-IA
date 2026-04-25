package pro.sketchware.ia.tools;

import org.json.JSONObject;
import java.io.File;
import java.util.List;
import pro.sketchware.util.ProjectPathResolver;

public class ListProjectFilesTool implements Tool {
    @Override
    public String getName() {
        return "list_project_files";
    }

    @Override
    public String getDescription() {
        return "Lista os arquivos de projeto do Sketchware (ex: logic, view, file) que nao possuem extensao e sao criptografados. Util para descobrir quais arquivos existem no projeto antes de usar decrypt_sketchware_file (para ler) ou encrypt_sketchware_file (para escrever/modificar).";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");
            params.put("properties", new JSONObject());
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        if (scId == null || scId.trim().isEmpty()) {
            return "Erro: ID do projeto nao fornecido.";
        }

        List<File> roots = ProjectPathResolver.getReadableRoots(scId);

        StringBuilder encrypted = new StringBuilder();
        StringBuilder normal = new StringBuilder();

        boolean foundAny = false;

        for (File root : roots) {
            if (root == null || !root.exists() || !root.isDirectory()) {
                continue;
            }

            File parent = root.getParentFile();
            String parentName = parent != null ? parent.getName() : "";

            File[] files = root.listFiles();
            if (files == null) continue;

            for (File f : files) {
                if (!f.isFile()) continue;

                foundAny = true;

                if ("data".equals(parentName)) {
                    encrypted.append("- ")
                            .append(f.getName())
                            .append("  → use decrypt_sketchware_file com file_path=\"")
                            .append(f.getName())
                            .append("\"\n");
                } else if ("list".equals(parentName)) {
                    normal.append("- mysc/list/")
                            .append(scId)
                            .append("/")
                            .append(f.getName())
                            .append("\n");
                } else {
                    normal.append("- mysc/")
                            .append(scId)
                            .append("/")
                            .append(f.getName())
                            .append("\n");
                }
            }
        }

        if (!foundAny) {
            return "Nenhum arquivo de projeto encontrado para o ID " + scId + ".";
        }

        return "Arquivos criptografados do projeto Sketchware (ID: " + scId + "):\n"
                + encrypted
                + "\nUse esses nomes diretamente no decrypt/encrypt, sem .json.\n\n"
                + "Outros arquivos nao criptografados/configuracao:\n"
                + normal
                + "\n\nNOTA: Já listamos os arquivos acima. Use decrypt_sketchware_file para acessar o conteúdo de um arquivo específico. Não chame esta ferramenta novamente sem necessidade.";
    }
}
