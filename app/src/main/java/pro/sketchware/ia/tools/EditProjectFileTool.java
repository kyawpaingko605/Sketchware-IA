package pro.sketchware.ia.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import pro.sketchware.util.FileChangeTracker;
import pro.sketchware.util.CompileErrorCapture;
import pro.sketchware.util.SketchwareFileEditor;
import pro.sketchware.activities.chat.ExtractCodeFromResult;

public class EditProjectFileTool implements Tool {

    @Override
    public String getName() {
        return "edit_project_file";
    }

    @Override
    public String getDescription() {
        return "Edits a project file using natural-language instructions, useful for targeted code changes without rewriting the whole file manually.";
    }

    @Override
    public JSONObject getParameters() {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            JSONObject properties = new JSONObject();
            properties.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Project-relative file path to edit."));
            properties.put("instructions", new JSONObject()
                    .put("type", "string")
                    .put("description", "Precise edit instructions describing the desired change."));
            properties.put("code_edit", new JSONObject()
                    .put("type", "string")
                    .put("description", "Optional replacement draft or target code snippet to help the editor."));

            params.put("properties", properties);
            params.put("required", new JSONArray().put("file_path").put("instructions"));
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        String filePath = args.optString("file_path", "").trim();
        String instructions = args.optString("instructions", "").trim();
        String rawCodeEdit = args.optString("code_edit", "");
        String codeEdit = ExtractCodeFromResult.extractCodeFromRegular(rawCodeEdit, rawCodeEdit.length()).fullText;

        if (filePath.isEmpty()) {
            return "Error: file_path is required.";
        }
        if (instructions.isEmpty()) {
            return "Error: instructions are required.";
        }

        SketchwareFileEditor.EditResult result = SketchwareFileEditor.editFile(scId, filePath, instructions, codeEdit);
        if (result == null) {
            return "Error: editor returned no result for '" + filePath + "'.";
        }
        if (!result.success) {
            return result.errorMessage == null ? "Error: edit failed." : result.errorMessage;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Successfully edited '")
                .append(filePath)
                .append("'.");

        FileChangeTracker.FileChange change = FileChangeTracker.getLastChange(filePath);
        String diffSummary = change == null ? null : change.generateDiff();
        if (diffSummary != null && !diffSummary.trim().isEmpty()) {
            builder.append("\nDiff summary:\n")
                    .append(diffSummary.trim());
        }

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
