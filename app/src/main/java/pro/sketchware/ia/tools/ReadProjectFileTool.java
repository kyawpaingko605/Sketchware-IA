package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import pro.sketchware.util.SketchwareFileDecryptor;

public class ReadProjectFileTool implements Tool {

    @Override
    public String getName() {
        return "read_project_file";
    }

    @Override
    public String getDescription() {
        return "Reads a project file from the current Sketchware project. Supports both encrypted Sketchware files and plain Android source files.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Project-relative file path, for example app/src/main/java/.../MainActivity.java or logic."));
            properties.put("start_line", new JSONObject()
                    .put("type", "string")
                    .put("description", "Optional 1-based start line."));
            properties.put("end_line", new JSONObject()
                    .put("type", "string")
                    .put("description", "Optional 1-based end line."));

            params.put("properties", properties);
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
            return "Error: file_path is required.";
        }

        String content = SketchwareFileDecryptor.decryptFile(scId, filePath);
        if (content == null || content.isEmpty()) {
            return "Error: could not read file '" + filePath + "'.";
        }

        Integer startLine = parsePositiveInt(args.optString("start_line", ""));
        Integer endLine = parsePositiveInt(args.optString("end_line", ""));

        String[] lines = content.replace("\r\n", "\n").split("\n", -1);
        int from = startLine == null ? 1 : Math.min(startLine, lines.length);
        int to = endLine == null ? lines.length : Math.min(endLine, lines.length);
        if (to < from) {
            return "Error: end_line must be greater than or equal to start_line.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(filePath)
                .append("\n");
        for (int i = from; i <= to; i++) {
            builder.append(i)
                    .append(": ")
                    .append(lines[i - 1])
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private Integer parsePositiveInt(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
