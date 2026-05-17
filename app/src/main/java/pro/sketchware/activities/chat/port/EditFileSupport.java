package pro.sketchware.activities.chat.port;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.SketchApplication;
import pro.sketchware.activities.chat.PromptConstants;
import pro.sketchware.network.MorphApplyService;
import pro.sketchware.util.SketchwareFileDecryptor;
import pro.sketchware.util.SketchwareFileEncryptor;
import pro.sketchware.util.FileChangeTracker;

/**
 * Morph-style {@code edit_file}: resolves LLM arguments and applies edits via Fast Apply or diff blocks.
 */
public final class EditFileSupport {
    private static final Pattern VOID_BLOCK_PATTERN = Pattern.compile(
            "<<<<<<< ORIGINAL[\\s\\t]*\\r?\\n(.*?)\\r?\\n[\\s\\t]*=======[\\s\\t]*\\r?\\n(.*?)\\r?\\n[\\s\\t]*>>>>>>> UPDATED",
            Pattern.DOTALL
    );
    private static final Pattern MORPH_BLOCK_PATTERN = Pattern.compile(
            "<<<<<<< SEARCH[\\s\\t]*\\r?\\n(.*?)\\r?\\n[\\s\\t]*=======[\\s\\t]*\\r?\\n(.*?)\\r?\\n[\\s\\t]*>>>>>>> REPLACE",
            Pattern.DOTALL
    );

    private EditFileSupport() {
    }

    public static final class ResolvedEdit {
        public final String uri;
        public final String instructions;
        public final String codeEdit;
        public final String searchReplaceBlocks;

        ResolvedEdit(String uri, String instructions, String codeEdit, String searchReplaceBlocks) {
            this.uri = uri == null ? "" : uri;
            this.instructions = instructions == null ? "" : instructions;
            this.codeEdit = codeEdit == null ? "" : codeEdit;
            this.searchReplaceBlocks = searchReplaceBlocks == null ? "" : searchReplaceBlocks;
        }

        boolean hasMorphStyleEdit() {
            return !codeEdit.isEmpty();
        }

        boolean hasDiffBlocks() {
            return !searchReplaceBlocks.isEmpty()
                    && (searchReplaceBlocks.contains(PromptConstants.ORIGINAL)
                    || searchReplaceBlocks.contains("<<<<<<< SEARCH"));
        }
    }

    public static ResolvedEdit resolveArgs(JSONObject args) {
        if (args == null) {
            args = new JSONObject();
        }

        String uri = firstNonEmpty(args,
                "uri", "target_file", "targetFile", "file_path", "filePath", "path");
        String instructions = firstNonEmpty(args,
                "instructions", "instruction", "explanation", "description");
        String codeEdit = firstNonEmpty(args,
                "code_edit", "codeEdit", "update", "edit", "patch");
        String blocks = firstNonEmpty(args,
                "search_replace_blocks", "searchReplaceBlocks", "diff", "changes");

        if (codeEdit.isEmpty() && !blocks.isEmpty()) {
            if (looksLikeDiffBlocks(blocks)) {
                // Legacy Void diff — keep for exact / balanced apply.
            } else {
                codeEdit = blocks;
                blocks = "";
            }
        }

        return new ResolvedEdit(uri, instructions, codeEdit, blocks);
    }

    public static String missingFieldsMessage(ResolvedEdit edit) {
        if (edit.uri.isEmpty()) {
            return "Invalid LLM output: uri (or target_file) was missing. "
                    + "Provide the full file path.";
        }
        if (!edit.hasMorphStyleEdit() && !edit.hasDiffBlocks()) {
            return "Invalid LLM output: code_edit (or search_replace_blocks) was missing. "
                    + "Use code_edit with // ... existing code ... markers, or configure Morph in IA settings.";
        }
        return "";
    }

    public static VoidPortToolsService.ToolCallResult apply(String scId, ResolvedEdit edit) {
        String validationError = missingFieldsMessage(edit);
        if (!validationError.isEmpty()) {
            return VoidPortToolsService.ToolCallResult.message(validationError);
        }

        String content = SketchwareFileDecryptor.decryptFile(scId, edit.uri);
        if (content == null) {
            return VoidPortToolsService.ToolCallResult.message("File not found or could not be decrypted: " + edit.uri);
        }

        SharedPreferences prefs = prefs();
        String applyMode = prefs == null
                ? VoidPortSettings.APPLY_MODE_FAST
                : prefs.getString(VoidPortSettings.PREF_APPLY_MODE, VoidPortSettings.APPLY_MODE_FAST);

        try {
            String newContent = mergeContent(content, edit, applyMode, prefs);
            if (newContent == null) {
                return VoidPortToolsService.ToolCallResult.message(
                        "Could not apply edit_file: edit did not change the file or could not be merged.");
            }
            if (newContent.equals(content)) {
                return VoidPortToolsService.ToolCallResult.message(
                        "Could not apply edit_file: result is identical to the original file.");
            }

            if (!SketchwareFileEncryptor.encryptAndSaveFile(scId, edit.uri, newContent)) {
                return VoidPortToolsService.ToolCallResult.message("Cannot write to file: " + edit.uri);
            }

            FileChangeTracker.trackChange(scId, edit.uri, content, newContent);
            return VoidPortToolsService.finishEditWithLint(scId, edit.uri);
        } catch (Exception e) {
            return VoidPortToolsService.ToolCallResult.message("Error applying edit_file: " + e.getMessage());
        }
    }

    private static String mergeContent(String content, ResolvedEdit edit, String applyMode, SharedPreferences prefs)
            throws Exception {
        boolean morphAvailable = MorphApplyService.isConfigured(prefs);
        boolean careful = VoidPortSettings.APPLY_MODE_CAREFUL.equals(applyMode);
        boolean fast = VoidPortSettings.APPLY_MODE_FAST.equals(applyMode);
        boolean balanced = VoidPortSettings.APPLY_MODE_BALANCED.equals(applyMode);

        if (fast && morphAvailable) {
            if (edit.hasMorphStyleEdit()) {
                return MorphApplyService.apply(prefs, content, edit.instructions, edit.codeEdit);
            }
            if (edit.hasDiffBlocks()) {
                return MorphApplyService.apply(
                        prefs, content, edit.instructions, blocksToMorphUpdate(edit.searchReplaceBlocks));
            }
        }

        if (edit.hasDiffBlocks() && !careful) {
            SearchReplaceResult exact = applySearchReplaceBlocks(content, edit.searchReplaceBlocks);
            if (exact.appliedCount == exact.blockCount && exact.blockCount > 0) {
                return exact.content;
            }
            if (balanced && morphAvailable) {
                if (edit.hasMorphStyleEdit()) {
                    return MorphApplyService.apply(prefs, content, edit.instructions, edit.codeEdit);
                }
                return MorphApplyService.apply(
                        prefs, content, edit.instructions, blocksToMorphUpdate(edit.searchReplaceBlocks));
            }
            if (exact.blockCount > 0) {
                throw new Exception("One or more SEARCH/REPLACE blocks did not match the file exactly.");
            }
        }

        if (careful && edit.hasDiffBlocks()) {
            SearchReplaceResult exact = applySearchReplaceBlocks(content, edit.searchReplaceBlocks);
            if (exact.appliedCount != exact.blockCount || exact.blockCount == 0) {
                throw new Exception("Careful mode: SEARCH/REPLACE blocks did not match the file exactly.");
            }
            return exact.content;
        }

        if (edit.hasMorphStyleEdit()) {
            if (morphAvailable) {
                return MorphApplyService.apply(prefs, content, edit.instructions, edit.codeEdit);
            }
            if (edit.hasDiffBlocks()) {
                SearchReplaceResult exact = applySearchReplaceBlocks(content, edit.searchReplaceBlocks);
                if (exact.appliedCount == exact.blockCount && exact.blockCount > 0) {
                    return exact.content;
                }
                throw new Exception("Morph API key not configured and SEARCH/REPLACE blocks did not match.");
            }
            throw new Exception("Morph Fast Apply is not configured. Add your Morph API key in IA settings, "
                    + "or use search_replace_blocks with exact file content.");
        }

        return null;
    }

    static String blocksToMorphUpdate(String blocks) {
        StringBuilder builder = new StringBuilder();
        appendBlocksAsUpdate(builder, VOID_BLOCK_PATTERN, blocks);
        appendBlocksAsUpdate(builder, MORPH_BLOCK_PATTERN, blocks);
        if (builder.length() == 0) {
            return blocks;
        }
        return builder.toString().trim();
    }

    private static void appendBlocksAsUpdate(StringBuilder builder, Pattern pattern, String blocks) {
        Matcher matcher = pattern.matcher(blocks);
        while (matcher.find()) {
            if (builder.length() > 0) {
                builder.append("\n// ... existing code ...\n");
            }
            builder.append(matcher.group(2).trim());
        }
    }

    static boolean looksLikeDiffBlocks(String text) {
        return text.contains("<<<<<<< ORIGINAL")
                || text.contains("<<<<<<< SEARCH")
                || (text.contains(PromptConstants.ORIGINAL) && text.contains(PromptConstants.FINAL));
    }

    static SearchReplaceResult applySearchReplaceBlocks(String content, String searchReplaceBlocks) {
        SearchReplaceResult voidResult = applyWithPattern(content, VOID_BLOCK_PATTERN, searchReplaceBlocks);
        if (voidResult.blockCount > 0) {
            return voidResult;
        }
        return applyWithPattern(content, MORPH_BLOCK_PATTERN, searchReplaceBlocks);
    }

    private static SearchReplaceResult applyWithPattern(String content, Pattern pattern, String searchReplaceBlocks) {
        int blockCount = 0;
        Matcher matcher = pattern.matcher(searchReplaceBlocks);

        java.util.List<String[]> blocks = new java.util.ArrayList<>();
        while (matcher.find()) {
            blockCount++;
            blocks.add(new String[]{matcher.group(1), matcher.group(2)});
        }

        if (blockCount == 0) {
            return new SearchReplaceResult(content, 0, 0);
        }

        for (String[] block : blocks) {
            if (!content.contains(block[0])) {
                return new SearchReplaceResult(content, blockCount, 0);
            }
        }

        String result = content;
        int appliedCount = 0;
        for (String[] block : blocks) {
            result = result.replace(block[0], block[1]);
            appliedCount++;
        }
        return new SearchReplaceResult(result, blockCount, appliedCount);
    }

    static final class SearchReplaceResult {
        final String content;
        final int blockCount;
        final int appliedCount;

        SearchReplaceResult(String content, int blockCount, int appliedCount) {
            this.content = content;
            this.blockCount = blockCount;
            this.appliedCount = appliedCount;
        }
    }

    static String firstNonEmpty(JSONObject args, String... keys) {
        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            Object value = args.opt(key);
            if (value == null || value == JSONObject.NULL) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text) && !"undefined".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return "";
    }

    private static SharedPreferences prefs() {
        Context context = SketchApplication.getContext();
        return context == null ? null : VoidPortSettings.prefs(context);
    }
}
