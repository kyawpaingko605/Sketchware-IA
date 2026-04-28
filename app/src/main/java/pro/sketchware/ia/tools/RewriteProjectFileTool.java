package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import pro.sketchware.util.FileChangeTracker;
import pro.sketchware.util.CompileErrorCapture;
import pro.sketchware.util.SketchwareFileDecryptor;
import pro.sketchware.util.SketchwareFileEncryptor;
import pro.sketchware.activities.chat.ExtractCodeFromResult;

public class RewriteProjectFileTool implements Tool {

    @Override
    public String getName() {
        return "rewrite_project_file";
    }

    @Override
    public String getDescription() {
        return "Rewrites an entire project file in the current Sketchware project. Works for encrypted internal files and plain Android source files.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Project-relative file path to rewrite."));
            properties.put("content", new JSONObject()
                    .put("type", "string")
                    .put("description", "Full replacement content for the file."));

            params.put("properties", properties);
            params.put("required", new JSONArray().put("file_path").put("content"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String filePath = args.optString("file_path", "").trim();
        String rawContent = args.optString("content", "");
        String content = ExtractCodeFromResult.extractCodeFromRegular(rawContent, rawContent.length()).fullText;
        if (filePath.isEmpty()) {
            return "Error: file_path is required.";
        }

        String before = SketchwareFileDecryptor.decryptFile(scId, filePath);
        boolean saved = SketchwareFileEncryptor.encryptAndSaveFile(scId, filePath, content);
        if (!saved) {
            return "Error: failed to rewrite '" + filePath + "'.";
        }

        FileChangeTracker.trackChange(filePath, before == null ? "" : before, content);

        StringBuilder builder = new StringBuilder();
        builder.append("Successfully rewrote '")
                .append(filePath)
                .append("'.");

        appendCompileErrorSummary(builder, scId);
        return builder.toString().trim();
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public boolean isDestructive() {
        return true;
    }

    private void appendCompileErrorSummary(StringBuilder builder, String scId) {
        try {
            if (!CompileErrorCapture.hasCompileErrors(scId)) {
                return;
            }

            String compileErrors = CompileErrorCapture.getLastCompileErrors(scId);
            if (compileErrors == null || compileErrors.trim().isEmpty()) {
                return;
            }

            String summary = CompileErrorCapture.extractErrorSummary(compileErrors).trim();
            if (summary.isEmpty()) {
                return;
            }

            builder.append("\nRecent compile error summary:\n")
                    .append(summary);
        } catch (Exception ignored) {
        }
    }
}
