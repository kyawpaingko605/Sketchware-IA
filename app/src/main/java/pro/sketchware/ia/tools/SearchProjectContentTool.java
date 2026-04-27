package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import pro.sketchware.util.CodeGrep;

public class SearchProjectContentTool implements Tool {

    @Override
    public String getName() {
        return "search_project_content";
    }

    @Override
    public String getDescription() {
        return "Searches for text or regex patterns inside files of the current Sketchware project.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("query", new JSONObject()
                    .put("type", "string")
                    .put("description", "Text or regex pattern to search for."));
            properties.put("is_regex", new JSONObject()
                    .put("type", "string")
                    .put("description", "Optional true/false. When true, query is treated as regex."));
            properties.put("file_pattern", new JSONObject()
                    .put("type", "string")
                    .put("description", "Optional glob filter like *.java or app/src/main/res/*.xml."));

            params.put("properties", properties);
            params.put("required", new JSONArray().put("query"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String query = args.optString("query", "").trim();
        if (query.isEmpty()) {
            return "Error: query is required.";
        }

        boolean isRegex = "true".equalsIgnoreCase(args.optString("is_regex", "false"));
        String filePattern = args.optString("file_pattern", "").trim();
        if (filePattern.isEmpty()) {
            filePattern = null;
        }

        List<CodeGrep.GrepResult> results = CodeGrep.searchInProject(scId, query, isRegex, filePattern);
        if (results == null || results.isEmpty()) {
            return "No matches found.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Search matches:\n");
        int count = 0;
        for (CodeGrep.GrepResult result : results) {
            if (result == null) {
                continue;
            }
            if (++count > 120) {
                builder.append("...truncated after 120 matches");
                break;
            }
            builder.append(result.filePath)
                    .append(":")
                    .append(result.lineNumber)
                    .append(" -> ")
                    .append(result.lineContent)
                    .append("\n");
        }
        return builder.toString().trim();
    }
}
