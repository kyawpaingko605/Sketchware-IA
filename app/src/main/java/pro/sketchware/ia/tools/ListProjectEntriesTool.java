package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import pro.sketchware.util.ProjectFileDiscovery;

public class ListProjectEntriesTool implements Tool {

    @Override
    public String getName() {
        return "list_project_entries";
    }

    @Override
    public String getDescription() {
        return "Lists files and folders inside the current Sketchware project, including Android source files and encrypted project data.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Optional project-relative folder or file path, such as app/src/main/java or logic."));
            properties.put("query", new JSONObject()
                    .put("type", "string")
                    .put("description", "Optional case-insensitive text filter for names or paths."));

            params.put("properties", properties);
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String path = args.optString("path", "").trim();
        String query = args.optString("query", "").trim();

        List<ProjectFileDiscovery.FileInfo> files;
        if (!query.isEmpty()) {
            files = ProjectFileDiscovery.searchFiles(scId, query);
        } else {
            files = ProjectFileDiscovery.discoverFiles(scId, path);
        }

        if (files == null || files.isEmpty()) {
            return "No project entries found.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Project entries:\n");
        int count = 0;
        for (ProjectFileDiscovery.FileInfo file : files) {
            if (file == null) {
                continue;
            }
            if (++count > 200) {
                builder.append("...truncated after 200 entries");
                break;
            }
            builder.append(file.isDirectory ? "[dir] " : "[file] ")
                    .append(file.path);
            if (!file.isDirectory) {
                builder.append(" (")
                        .append(file.isEncrypted ? "encrypted" : "plain")
                        .append(", ")
                        .append(file.size)
                        .append(" bytes)");
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }
}
