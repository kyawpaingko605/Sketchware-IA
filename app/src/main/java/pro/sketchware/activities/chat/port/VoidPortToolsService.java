package pro.sketchware.activities.chat.port;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.activities.chat.DirectoryTreeService;
import pro.sketchware.activities.chat.LanguageHelpers;
import pro.sketchware.activities.chat.StringHelpers;
import pro.sketchware.util.ProjectPathResolver;
import pro.sketchware.util.SemanticFileSearcher;
import pro.sketchware.util.SketchwareFileDecryptor;
import pro.sketchware.util.SketchwareFileEncryptor;
import pro.sketchware.util.FileChangeTracker;

/**
 * Android port of browser/toolsService.ts
 * Provides all builtin tools from Void for use in Sketchware-IA chat.
 */
public final class VoidPortToolsService {

    private static final int MAX_FILE_CHARS_PAGE = 500000;
    private static final int MAX_CHILDREN_URIS_PAGE = 500;
    private static final int MAX_TERMINAL_BG_COMMAND_TIME_SECONDS = 5;
    private static final int MAX_TERMINAL_INACTIVE_TIME_SECONDS = 8;
    private static final int LINT_ERROR_TIMEOUT = 1000;

    private static final Map<String, Process> activeTerminals = new ConcurrentHashMap<>();
    private static final Map<String, StringBuilder> terminalOutputs = new ConcurrentHashMap<>();
    private static final Map<String, BufferedReader> terminalReaders = new ConcurrentHashMap<>();

    private VoidPortToolsService() {
    }

    // ============================================
    // VALIDATION HELPERS
    // ============================================

    private static boolean isFalsy(Object value) {
        return value == null || "null".equals(String.valueOf(value)) || "undefined".equals(String.valueOf(value));
    }

    private static String validateStr(String argName, Object value) throws Exception {
        if (value == null) {
            throw new Exception("Invalid LLM output: " + argName + " was null.");
        }
        if (!(value instanceof String)) {
            throw new Exception("Invalid LLM output format: " + argName + " must be a string, but its type is \"" + (value != null ? value.getClass().getSimpleName() : "null") + "\". Full value: " + String.valueOf(value));
        }
        return (String) value;
    }

    private static String validateOptionalStr(String argName, Object value) {
        if (isFalsy(value)) return null;
        try {
            return validateStr(argName, value);
        } catch (Exception e) {
            return null;
        }
    }

    private static int validatePageNum(Object pageNumberUnknown) {
        if (pageNumberUnknown == null) return 1;
        try {
            int parsed = Integer.parseInt(String.valueOf(pageNumberUnknown));
            if (parsed < 1) return 1;
            return parsed;
        } catch (Exception e) {
            return 1;
        }
    }

    private static Integer validateNumber(Object numStr, Integer defaultVal) {
        if (numStr == null) return defaultVal;
        if (numStr instanceof Number) return ((Number) numStr).intValue();
        try {
            return Integer.parseInt(String.valueOf(numStr));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static boolean validateBoolean(Object b, boolean defaultVal) {
        if (b instanceof Boolean) return (Boolean) b;
        if (b instanceof String) {
            if ("true".equals(b)) return true;
            if ("false".equals(b)) return false;
        }
        return defaultVal;
    }

    private static boolean checkIfIsFolder(String uriStr) {
        if (uriStr == null) return false;
        uriStr = uriStr.trim();
        return uriStr.endsWith("/") || uriStr.endsWith("\\");
    }

    // ============================================
    // TOOL CALL RESULTS
    // ============================================

    public static class ToolCallResult {
        public final String result;
        public final boolean hasNextPage;
        public final boolean hasPrevPage;
        public final int itemsRemaining;
        public final int totalFileLen;
        public final int totalNumLines;

        private ToolCallResult(String result) {
            this.result = result;
            this.hasNextPage = false;
            this.hasPrevPage = false;
            this.itemsRemaining = 0;
            this.totalFileLen = 0;
            this.totalNumLines = 0;
        }

        private ToolCallResult(String result, boolean hasNextPage) {
            this.result = result;
            this.hasNextPage = hasNextPage;
            this.hasPrevPage = false;
            this.itemsRemaining = 0;
            this.totalFileLen = 0;
            this.totalNumLines = 0;
        }

        private ToolCallResult(String result, boolean hasNextPage, boolean hasPrevPage, int itemsRemaining) {
            this.result = result;
            this.hasNextPage = hasNextPage;
            this.hasPrevPage = hasPrevPage;
            this.itemsRemaining = itemsRemaining;
            this.totalFileLen = 0;
            this.totalNumLines = 0;
        }

        private ToolCallResult(String result, int totalFileLen, int totalNumLines, boolean hasNextPage) {
            this.result = result;
            this.hasNextPage = hasNextPage;
            this.hasPrevPage = false;
            this.itemsRemaining = 0;
            this.totalFileLen = totalFileLen;
            this.totalNumLines = totalNumLines;
        }
    }

    // ============================================
    // FILE TOOLS
    // ============================================

    public static ToolCallResult readFile(String scId, Object uriObj, Object startLineObj, Object endLineObj, Object pageNumberObj) {
        try {
            String uriStr = validateStr("uri", uriObj);
            int pageNumber = validatePageNum(pageNumberObj);
            Integer startLine = validateNumber(startLineObj, null);
            Integer endLine = validateNumber(endLineObj, null);

            if (startLine != null && startLine < 1) startLine = null;
            if (endLine != null && endLine < 1) endLine = null;

            ProjectPathResolver.ResolvedPath resolved = ProjectPathResolver.resolveForRead(scId, uriStr);
            if (resolved == null) {
                return new ToolCallResult("File not found or outside project scope: " + uriStr);
            }

            String content = SketchwareFileDecryptor.decryptFile(scId, uriStr);
            if (content == null) {
                return new ToolCallResult("File not found or could not be decrypted: " + uriStr);
            }

            String selected = sliceLines(content, startLine, endLine);
            int totalFileLen = content.length();
            int totalNumLines = content.split("\n", -1).length;

            int fromIdx = MAX_FILE_CHARS_PAGE * (pageNumber - 1);
            int toIdx = MAX_FILE_CHARS_PAGE * pageNumber - 1;
            String fileContents;
            if (fromIdx >= selected.length()) {
                fileContents = "";
            } else {
                fileContents = selected.substring(fromIdx, Math.min(toIdx + 1, selected.length()));
            }
            boolean hasNextPage = (selected.length() - 1) - toIdx >= 1;

            JSONObject resultObj = new JSONObject();
            resultObj.put("fileContents", fileContents);
            resultObj.put("totalFileLen", totalFileLen);
            resultObj.put("totalNumLines", totalNumLines);
            resultObj.put("hasNextPage", hasNextPage);

            return new ToolCallResult(resultObj.toString(), totalFileLen, totalNumLines, hasNextPage);
        } catch (Exception e) {
            return new ToolCallResult("Error reading file: " + e.getMessage());
        }
    }

    public static ToolCallResult lsDir(String scId, Object uriObj, Object pageNumberObj) {
        try {
            String uriStr = validateOptionalStr("uri", uriObj);
            if (uriStr == null) {
                uriStr = "";
            }
            int pageNumber = validatePageNum(pageNumberObj);

            List<File> entries = new ArrayList<>();
            if (uriStr.trim().isEmpty()) {
                for (File root : ProjectPathResolver.getReadableRoots(scId)) {
                    if (root != null && root.exists()) {
                        entries.add(root);
                    }
                }
            } else {
                ProjectPathResolver.ResolvedPath resolved = ProjectPathResolver.resolveForRead(scId, uriStr);
                if (resolved == null) {
                    return new ToolCallResult("[]");
                }

                File folder = resolved.getFile();
                if (!folder.exists()) {
                    return new ToolCallResult("Directory not found: " + uriStr);
                }
                if (!folder.isDirectory()) {
                    return new ToolCallResult("The path is a file, not a directory. Use read_file to view its contents: " + uriStr);
                }

                File[] files = folder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        entries.add(file);
                    }
                }
            }

            if (entries.isEmpty()) {
                return new ToolCallResult("[]");
            }

            int fromIdx = MAX_CHILDREN_URIS_PAGE * (pageNumber - 1);
            int toIdx = MAX_CHILDREN_URIS_PAGE * pageNumber - 1;

            JSONArray resultArray = new JSONArray();
            for (int i = fromIdx; i <= Math.min(toIdx, entries.size() - 1); i++) {
                File f = entries.get(i);
                JSONObject item = new JSONObject();
                item.put("uri", f.getAbsolutePath());
                item.put("name", f.getName());
                item.put("isDirectory", f.isDirectory());
                item.put("isSymbolicLink", false);
                resultArray.put(item);
            }

            boolean hasNextPage = (entries.size() - 1) - toIdx >= 1;
            boolean hasPrevPage = pageNumber > 1;
            int itemsRemaining = Math.max(0, entries.size() - (toIdx + 1));

            JSONObject resultObj = new JSONObject();
            resultObj.put("children", resultArray);
            resultObj.put("hasNextPage", hasNextPage);
            resultObj.put("hasPrevPage", hasPrevPage);
            resultObj.put("itemsRemaining", itemsRemaining);

            return new ToolCallResult(resultObj.toString(), hasNextPage, hasPrevPage, itemsRemaining);
        } catch (Exception e) {
            return new ToolCallResult("[]");
        }
    }

    public static ToolCallResult getDirTree(String scId, Object uriObj) {
        try {
            String uriStr = validateStr("uri", uriObj);

            ProjectPathResolver.ResolvedPath resolved = ProjectPathResolver.resolveForRead(scId, uriStr);
            if (resolved == null) {
                return new ToolCallResult("Directory not found: " + uriStr);
            }

            File folder = resolved.getFile();
            if (!folder.exists()) {
                return new ToolCallResult("Directory not found: " + uriStr);
            }
            if (!folder.isDirectory()) {
                return new ToolCallResult("The path is a file, not a directory. Use read_file instead: " + uriStr);
            }

            String tree = DirectoryTreeService.getDirectoryStrTool(folder);
            JSONObject resultObj = new JSONObject();
            resultObj.put("str", tree);
            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("Error getting directory tree: " + e.getMessage());
        }
    }

    // ============================================
    // SEARCH TOOLS
    // ============================================

    public static ToolCallResult searchPathnamesOnly(String scId, Object queryObj, Object includePatternObj, Object pageNumberObj) {
        try {
            String queryStr = validateStr("query", queryObj);
            int pageNumber = validatePageNum(pageNumberObj);
            String includePattern = validateOptionalStr("include_pattern", includePatternObj);

            List<SemanticFileSearcher.SearchResult> results = SemanticFileSearcher.searchByFilename(queryStr, scId);
            results = filterSearchResults(results, includePattern, null);
            
            int fromIdx = MAX_CHILDREN_URIS_PAGE * (pageNumber - 1);
            int toIdx = MAX_CHILDREN_URIS_PAGE * pageNumber - 1;

            JSONArray urisArray = new JSONArray();
            for (int i = fromIdx; i <= Math.min(toIdx, results.size() - 1); i++) {
                urisArray.put(results.get(i).filePath);
            }

            boolean hasNextPage = (results.size() - 1) - toIdx >= 1;

            JSONObject resultObj = new JSONObject();
            resultObj.put("uris", urisArray);
            resultObj.put("hasNextPage", hasNextPage);

            return new ToolCallResult(resultObj.toString(), hasNextPage);
        } catch (Exception e) {
            return new ToolCallResult("{\"uris\":[],\"hasNextPage\":false}");
        }
    }

    public static ToolCallResult searchForFiles(String scId, Object queryObj, Object isRegexObj, Object searchInFolderObj, Object pageNumberObj) {
        try {
            String queryStr = validateStr("query", queryObj);
            boolean isRegex = validateBoolean(isRegexObj, false);
            int pageNumber = validatePageNum(pageNumberObj);
            String searchInFolder = validateOptionalStr("search_in_folder", searchInFolderObj);

            List<SemanticFileSearcher.SearchResult> results;
            if (isRegex) {
                results = SemanticFileSearcher.searchByContentRegex(queryStr, scId);
            } else {
                results = SemanticFileSearcher.searchByContent(queryStr, scId);
            }
            results = filterSearchResults(results, null, searchInFolder);

            int fromIdx = MAX_CHILDREN_URIS_PAGE * (pageNumber - 1);
            int toIdx = MAX_CHILDREN_URIS_PAGE * pageNumber - 1;

            JSONArray urisArray = new JSONArray();
            for (int i = fromIdx; i <= Math.min(toIdx, results.size() - 1); i++) {
                urisArray.put(results.get(i).filePath);
            }

            boolean hasNextPage = (results.size() - 1) - toIdx >= 1;

            JSONObject resultObj = new JSONObject();
            resultObj.put("uris", urisArray);
            resultObj.put("hasNextPage", hasNextPage);

            return new ToolCallResult(resultObj.toString(), hasNextPage);
        } catch (Exception e) {
            return new ToolCallResult("{\"uris\":[],\"hasNextPage\":false}");
        }
    }

    public static ToolCallResult searchInFile(String scId, Object uriObj, Object queryObj, Object isRegexObj) {
        try {
            String uriStr = validateStr("uri", uriObj);
            String query = validateStr("query", queryObj);
            boolean isRegex = validateBoolean(isRegexObj, false);

            String content = SketchwareFileDecryptor.decryptFile(scId, uriStr);
            if (content == null) {
                JSONArray linesArray = new JSONArray();
                JSONObject resultObj = new JSONObject();
                resultObj.put("lines", linesArray);
                return new ToolCallResult(resultObj.toString());
            }

            String[] lines = content.split("\n", -1);
            JSONArray linesArray = new JSONArray();
            Pattern regex = isRegex ? Pattern.compile(query) : null;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                boolean matches = isRegex ? (regex != null && regex.matcher(line).find()) : line.contains(query);
                if (matches) {
                    linesArray.put(i + 1);
                }
            }

            JSONObject resultObj = new JSONObject();
            resultObj.put("lines", linesArray);
            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("{\"lines\":[]}");
        }
    }

    public static ToolCallResult readLintErrors(String scId, Object uriObj) {
        try {
            String uriStr = validateStr("uri", uriObj);
            
            JSONArray lintErrorsArray = new JSONArray();
            List<VoidPortMarkerCheckService.LintError> lintErrors = VoidPortMarkerCheckService.getLintErrors(scId, uriStr);
            for (VoidPortMarkerCheckService.LintError error : lintErrors) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("code", error.code);
                errorObj.put("message", error.message);
                errorObj.put("startLineNumber", error.startLineNumber);
                errorObj.put("endLineNumber", error.endLineNumber);
                lintErrorsArray.put(errorObj);
            }

            JSONObject resultObj = new JSONObject();
            resultObj.put("lintErrors", lintErrorsArray);
            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("{\"lintErrors\":[]}");
        }
    }

    // ============================================
    // EDIT TOOLS
    // ============================================

    public static ToolCallResult rewriteFile(String scId, Object uriObj, Object newContentObj) {
        try {
            String uriStr = validateStr("uri", uriObj);
            String newContent = validateStr("newContent", newContentObj);

            String oldContent = SketchwareFileDecryptor.decryptFile(scId, uriStr);
            if (oldContent == null) {
                oldContent = "";
            }

            if (!SketchwareFileEncryptor.encryptAndSaveFile(scId, uriStr, newContent)) {
                return new ToolCallResult("Cannot write to file: " + uriStr);
            }

            FileChangeTracker.trackChange(scId, uriStr, oldContent, newContent);

            // Get lint errors after write
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            List<VoidPortMarkerCheckService.LintError> lintErrors = VoidPortMarkerCheckService.getLintErrors(scId, uriStr);
            JSONArray lintErrorsArray = new JSONArray();
            for (VoidPortMarkerCheckService.LintError error : lintErrors) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("code", error.code);
                errorObj.put("message", error.message);
                errorObj.put("startLineNumber", error.startLineNumber);
                errorObj.put("endLineNumber", error.endLineNumber);
                lintErrorsArray.put(errorObj);
            }

            JSONObject resultObj = new JSONObject();
            resultObj.put("lintErrors", lintErrorsArray);
            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("Error rewriting file: " + e.getMessage());
        }
    }

    public static ToolCallResult editFile(String scId, Object uriObj, Object searchReplaceBlocksObj) {
        try {
            String uriStr = validateStr("uri", uriObj);
            String searchReplaceBlocks = validateStr("searchReplaceBlocks", searchReplaceBlocksObj);

            String content = SketchwareFileDecryptor.decryptFile(scId, uriStr);
            if (content == null) {
                return new ToolCallResult("File not found or could not be decrypted: " + uriStr);
            }

            SearchReplaceResult replaceResult = applySearchReplaceBlocks(content, searchReplaceBlocks);
            if (replaceResult.blockCount == 0) {
                return new ToolCallResult("Invalid SEARCH/REPLACE blocks: no valid blocks found.");
            }
            if (replaceResult.appliedCount != replaceResult.blockCount) {
                return new ToolCallResult("Could not apply edit_file: one or more ORIGINAL blocks did not match the file exactly.");
            }
            String newContent = replaceResult.content;

            if (!SketchwareFileEncryptor.encryptAndSaveFile(scId, uriStr, newContent)) {
                return new ToolCallResult("Cannot write to file: " + uriStr);
            }

            FileChangeTracker.trackChange(scId, uriStr, content, newContent);

            // Get lint errors after edit
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            List<VoidPortMarkerCheckService.LintError> lintErrors = VoidPortMarkerCheckService.getLintErrors(scId, uriStr);
            JSONArray lintErrorsArray = new JSONArray();
            for (VoidPortMarkerCheckService.LintError error : lintErrors) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("code", error.code);
                errorObj.put("message", error.message);
                errorObj.put("startLineNumber", error.startLineNumber);
                errorObj.put("endLineNumber", error.endLineNumber);
                lintErrorsArray.put(errorObj);
            }

            JSONObject resultObj = new JSONObject();
            resultObj.put("lintErrors", lintErrorsArray);
            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("Error editing file: " + e.getMessage());
        }
    }

    public static ToolCallResult createFileOrFolder(String scId, Object uriObj) {
        try {
            String uriStr = validateStr("uri", uriObj);
            boolean isFolder = checkIfIsFolder(uriStr);

            ProjectPathResolver.ResolvedPath resolved = ProjectPathResolver.resolveForWrite(scId, uriStr);
            if (resolved == null) {
                return new ToolCallResult("Cannot create: " + uriStr);
            }

            File file = resolved.getFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            if (isFolder) {
                if (!file.exists()) {
                    file.mkdirs();
                }
            } else {
                if (!file.exists()) {
                    file.createNewFile();
                }
            }

            return new ToolCallResult("{}");
        } catch (Exception e) {
            return new ToolCallResult("Error creating file/folder: " + e.getMessage());
        }
    }

    public static ToolCallResult deleteFileOrFolder(String scId, Object uriObj, Object isRecursiveObj) {
        try {
            String uriStr = validateStr("uri", uriObj);
            boolean isRecursive = validateBoolean(isRecursiveObj, false);

            ProjectPathResolver.ResolvedPath resolved = ProjectPathResolver.resolveForRead(scId, uriStr);
            if (resolved == null) {
                return new ToolCallResult("File/folder not found: " + uriStr);
            }

            File file = resolved.getFile();
            if (!file.exists()) {
                return new ToolCallResult("File/folder not found: " + uriStr);
            }

            if (file.isDirectory() && !isRecursive && file.list().length > 0) {
                return new ToolCallResult("Cannot delete non-empty directory without is_recursive=true");
            }

            deleteRecursive(file);
            return new ToolCallResult("{}");
        } catch (Exception e) {
            return new ToolCallResult("Error deleting file/folder: " + e.getMessage());
        }
    }

    // ============================================
    // TERMINAL TOOLS
    // ============================================

    public static ToolCallResult runCommand(String scId, Object commandObj, Object cwdObj, Object terminalIdObj) {
        try {
            String command = validateStr("command", commandObj);
            String cwd = validateOptionalStr("cwd", cwdObj);
            String terminalId = terminalIdObj != null ? String.valueOf(terminalIdObj) : java.util.UUID.randomUUID().toString();

            if (command.trim().isEmpty()) {
                return new ToolCallResult("Nenhum comando foi executado porque o texto do comando veio vazio.");
            }

            File workingDir = resolveTerminalWorkingDir(scId, cwd);
            String shell = androidShellPath();

            if (!workingDir.exists()) {
                return new ToolCallResult("Erro: pasta de trabalho não encontrada: " + workingDir.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(shell, "-c", command);
            configureAndroidProcess(pb, workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeTerminals.put(terminalId, process);
            terminalOutputs.put(terminalId, new StringBuilder());
            terminalReaders.put(terminalId, new BufferedReader(new InputStreamReader(process.getInputStream())));

            boolean finished = process.waitFor(MAX_TERMINAL_INACTIVE_TIME_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                String output = drainTerminalOutput(terminalId, false);
                process.destroyForcibly();
                activeTerminals.remove(terminalId);
                terminalOutputs.remove(terminalId);
                terminalReaders.remove(terminalId);

                JSONObject resolveReason = new JSONObject();
                resolveReason.put("type", "timeout");
                JSONObject resultObj = new JSONObject();
                resultObj.put("result", trimTerminalOutput(output));
                resultObj.put("resolveReason", resolveReason);
                resultObj.put("terminal", androidTerminalInfo(shell, workingDir));
                return new ToolCallResult(resultObj.toString());
            }

            String output = drainTerminalOutput(terminalId, true);

            int exitCode = process.exitValue();
            activeTerminals.remove(terminalId);
            terminalOutputs.remove(terminalId);
            terminalReaders.remove(terminalId);
            
            String normalizedOutput = trimTerminalOutput(output);

            JSONObject resultObj = new JSONObject();
            JSONObject resolveReason = new JSONObject();
            resolveReason.put("type", "done");
            resolveReason.put("exitCode", exitCode);
            resultObj.put("result", normalizedOutput);
            resultObj.put("resolveReason", resolveReason);
            resultObj.put("terminal", androidTerminalInfo(shell, workingDir));

            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("Falha ao executar comando: " + e.getMessage());
        }
    }

    public static ToolCallResult openPersistentTerminal(String scId, Object cwdObj) {
        try {
            String cwd = validateOptionalStr("cwd", cwdObj);
            String terminalId = java.util.UUID.randomUUID().toString();

            File workingDir = resolveTerminalWorkingDir(scId, cwd);
            String shell = androidShellPath();

            ProcessBuilder pb = new ProcessBuilder(shell, "-i");
            configureAndroidProcess(pb, workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeTerminals.put(terminalId, process);
            terminalOutputs.put(terminalId, new StringBuilder());
            terminalReaders.put(terminalId, new BufferedReader(new InputStreamReader(process.getInputStream())));

            JSONObject resultObj = new JSONObject();
            resultObj.put("persistentTerminalId", terminalId);
            resultObj.put("terminal", androidTerminalInfo(shell, workingDir));
            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("Erro ao abrir terminal persistente: " + e.getMessage());
        }
    }

    public static ToolCallResult runPersistentCommand(String scId, Object commandObj, Object persistentTerminalIdObj) {
        try {
            String command = validateStr("command", commandObj);
            String terminalId = validateStr("persistent_terminal_id", persistentTerminalIdObj);

            Process process = activeTerminals.get(terminalId);
            if (process == null) {
                return new ToolCallResult("Terminal não encontrado: " + terminalId);
            }

            java.io.OutputStream os = process.getOutputStream();
            os.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            os.flush();

            // Wait for command to complete (with timeout)
            Thread.sleep(TimeUnit.SECONDS.toMillis(MAX_TERMINAL_BG_COMMAND_TIME_SECONDS));

            String result = trimTerminalOutput(drainTerminalOutput(terminalId, false));
            
            StringBuilder output = terminalOutputs.get(terminalId);
            if (output != null) {
                output.setLength(0);
            }

            JSONObject resultObj = new JSONObject();
            JSONObject resolveReason = new JSONObject();
            resolveReason.put("type", "timeout");
            resultObj.put("result", result);
            resultObj.put("resolveReason", resolveReason);
            resultObj.put("terminal", new JSONObject()
                    .put("platform", "android")
                    .put("persistentTerminalId", terminalId));
            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("Erro ao executar comando persistente: " + e.getMessage());
        }
    }

    public static ToolCallResult killPersistentTerminal(String scId, Object persistentTerminalIdObj) {
        try {
            String terminalId = validateStr("persistent_terminal_id", persistentTerminalIdObj);

            Process process = activeTerminals.remove(terminalId);
            terminalOutputs.remove(terminalId);
            terminalReaders.remove(terminalId);
            
            if (process != null) {
                process.destroyForcibly();
            }

            return new ToolCallResult("{}");
        } catch (Exception e) {
            return new ToolCallResult("Erro ao fechar terminal: " + e.getMessage());
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private static File resolveTerminalWorkingDir(String scId, String cwd) throws IOException {
        File workingDir = cwd != null && !cwd.trim().isEmpty()
                ? new File(cwd.trim())
                : ProjectPathResolver.getDefaultWorkingRoot(scId);
        if (workingDir == null) {
            throw new IOException("Android terminal working directory could not be resolved.");
        }
        if (!workingDir.exists()) {
            throw new IOException("Android terminal working directory not found: " + workingDir.getAbsolutePath());
        }
        if (!workingDir.isDirectory()) {
            throw new IOException("Android terminal cwd is not a directory: " + workingDir.getAbsolutePath());
        }
        return workingDir;
    }

    private static String androidShellPath() {
        File systemShell = new File("/system/bin/sh");
        if (systemShell.exists() && systemShell.canExecute()) {
            return systemShell.getAbsolutePath();
        }
        File vendorShell = new File("/vendor/bin/sh");
        if (vendorShell.exists() && vendorShell.canExecute()) {
            return vendorShell.getAbsolutePath();
        }
        return "sh";
    }

    private static void configureAndroidProcess(ProcessBuilder processBuilder, File workingDir) {
        processBuilder.directory(workingDir);
        Map<String, String> env = processBuilder.environment();
        env.put("PWD", workingDir.getAbsolutePath());
        env.put("HOME", workingDir.getAbsolutePath());
        env.put("TERM", "xterm-256color");
        env.put("ANDROID_TERMINAL", "1");
    }

    private static JSONObject androidTerminalInfo(String shell, File workingDir) throws Exception {
        JSONObject info = new JSONObject();
        info.put("platform", "android");
        info.put("shell", shell);
        info.put("cwd", workingDir == null ? "" : workingDir.getAbsolutePath());
        info.put("sdk", Build.VERSION.SDK_INT);
        info.put("device", (Build.MANUFACTURER + " " + Build.MODEL).trim());
        return info;
    }

    private static String sliceLines(String content, Integer startLine, Integer endLine) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        if (startLine == null && endLine == null) {
            return content;
        }
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int from = Math.max(1, startLine == null ? 1 : startLine);
        int to = endLine == null ? lines.length : Math.min(endLine, lines.length);
        if (to < from) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = from; i <= to; i++) {
            builder.append(lines[i - 1]);
            if (i < to) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private static String trimTerminalOutput(String output) {
        String normalized = output == null || output.trim().isEmpty() ? "(sem saida)" : output.trim();
        return normalized.length() > 100000 ? normalized.substring(0, 100000) : normalized;
    }

    private static String drainTerminalOutput(String terminalId, boolean readUntilEnd) throws IOException {
        BufferedReader reader = terminalReaders.get(terminalId);
        StringBuilder output = terminalOutputs.get(terminalId);
        if (reader == null || output == null) {
            return "";
        }

        while (readUntilEnd || reader.ready()) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            output.append(line).append("\n");
        }
        return output.toString();
    }

    private static List<SemanticFileSearcher.SearchResult> filterSearchResults(
            List<SemanticFileSearcher.SearchResult> results,
            String includePattern,
            String searchInFolder) {
        if ((includePattern == null || includePattern.trim().isEmpty())
                && (searchInFolder == null || searchInFolder.trim().isEmpty())) {
            return results;
        }

        List<SemanticFileSearcher.SearchResult> filtered = new ArrayList<>();
        String normalizedFolder = normalizePathFilter(searchInFolder);
        Pattern includeRegex = compileGlobPattern(includePattern);
        for (SemanticFileSearcher.SearchResult result : results) {
            String normalizedPath = normalizePathFilter(result.filePath);
            if (normalizedFolder != null && !normalizedPath.startsWith(normalizedFolder)) {
                continue;
            }
            if (includeRegex != null && !includeRegex.matcher(normalizedPath).find()) {
                continue;
            }
            filtered.add(result);
        }
        return filtered;
    }

    private static String normalizePathFilter(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        String normalized = path.replace('\\', '/').trim().toLowerCase();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static Pattern compileGlobPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizePathFilter(pattern);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '*') {
                regex.append(".*");
            } else if (c == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString());
    }

    private static final class SearchReplaceResult {
        final String content;
        final int blockCount;
        final int appliedCount;

        SearchReplaceResult(String content, int blockCount, int appliedCount) {
            this.content = content;
            this.blockCount = blockCount;
            this.appliedCount = appliedCount;
        }
    }

    private static SearchReplaceResult applySearchReplaceBlocks(String content, String searchReplaceBlocks) {
        String result = content;
        int blockCount = 0;
        int appliedCount = 0;
        
        Pattern pattern = Pattern.compile(
            "<<<<<<< ORIGINAL\\s*\\n(.*?)\\s*=======\\s*\\n(.*?)\\s*>>>>>>> UPDATED",
            Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(searchReplaceBlocks);
        
        while (matcher.find()) {
            blockCount++;
            String search = matcher.group(1);
            String replace = matcher.group(2);
            if (result.contains(search)) {
                result = result.replace(search, replace);
                appliedCount++;
            }
        }
        
        return new SearchReplaceResult(result, blockCount, appliedCount);
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    // ============================================
    // TOOL REGISTRY FOR MCP
    // ============================================

    public static JSONArray getAllToolsAsMCP() {
        JSONArray array = new JSONArray();
        if (useVoidToolDescriptions()) {
            String terminalDescHelper = "This runs inside the Android app environment using the available Android shell, not a desktop VS Code terminal. Prefer Android-available commands such as sh, ls, cat, grep, sed, find, logcat, and project-local Gradle scripts when present. Do not edit files with this tool; use edit_file instead. When working with git and other tools that open an editor (e.g. git diff), pipe to cat to get all results and not get stuck in an editor.";

            array.put(createToolMCP("read_file",
                    "Returns full contents of a given file.",
                    new String[]{"uri"}, new String[]{"start_line", "end_line", "page_number"}));
            array.put(createToolMCP("ls_dir",
                    "Lists all files and folders in the given URI.",
                    new String[]{}, new String[]{"uri", "page_number"}));
            array.put(createToolMCP("get_dir_tree",
                    "This is a very effective way to learn about the user's codebase. Returns a tree diagram of all the files and folders in the given folder. ",
                    new String[]{"uri"}, null));
            array.put(createToolMCP("search_pathnames_only",
                    "Returns all pathnames that match a given query (searches ONLY file names). You should use this when looking for a file with a specific name or path.",
                    new String[]{"query"}, new String[]{"include_pattern", "page_number"}));
            array.put(createToolMCP("search_for_files",
                    "Returns a list of file names whose content matches the given query. The query can be any substring or regex.",
                    new String[]{"query"}, new String[]{"is_regex", "search_in_folder", "page_number"}));
            array.put(createToolMCP("search_in_file",
                    "Returns an array of all the start line numbers where the content appears in the file.",
                    new String[]{"uri", "query"}, new String[]{"is_regex"}));
            array.put(createToolMCP("read_lint_errors",
                    "Use this tool to view all the lint errors on a file.",
                    new String[]{"uri"}, null));
            array.put(createToolMCP("rewrite_file",
                    "Edits a file, deleting all the old contents and replacing them with your new contents. Use this tool if you want to edit a file you just created.",
                    new String[]{"uri", "new_content"}, null));
            array.put(createToolMCP("edit_file",
                    "Edit the contents of a file. You must provide the file's URI as well as a SINGLE string of SEARCH/REPLACE block(s) that will be used to apply the edit.",
                    new String[]{"uri", "search_replace_blocks"}, null));
            array.put(createToolMCP("create_file_or_folder",
                    "Create a file or folder at the given path. To create a folder, the path MUST end with a trailing slash.",
                    new String[]{"uri"}, null));
            array.put(createToolMCP("delete_file_or_folder",
                    "Delete a file or folder at the given path.",
                    new String[]{"uri"}, new String[]{"is_recursive"}));
            array.put(createToolMCP("run_command",
                    "Runs a terminal command and waits for the result (times out after 8s of inactivity). " + terminalDescHelper,
                    new String[]{"command"}, new String[]{"cwd"}));
            array.put(createToolMCP("open_persistent_terminal",
                    "Use this tool when you want to run a terminal command indefinitely, like a dev server (eg `npm run dev`), a background listener, etc. Opens a new terminal in the user's environment which will not awaited for or killed.",
                    new String[]{}, new String[]{"cwd"}));
            array.put(createToolMCP("run_persistent_command",
                    "Runs a terminal command in the persistent terminal that you created with open_persistent_terminal (results after 5 are returned, and command continues running in background). " + terminalDescHelper,
                    new String[]{"command", "persistent_terminal_id"}, null));
            array.put(createToolMCP("kill_persistent_terminal",
                    "Interrupts and closes a persistent terminal that you opened with open_persistent_terminal.",
                    new String[]{"persistent_terminal_id"}, null));
            return array;
        }
        
        // File tools
        array.put(createToolMCP("read_file", 
            "Lê o conteúdo de um arquivo. Suporta paginação e seleção de linhas.",
            new String[]{"uri"}, new String[]{"start_line", "end_line", "page_number"}));
        
        array.put(createToolMCP("ls_dir", 
            "Lista arquivos e pastas em um diretório. Suporta paginação.",
            new String[]{}, new String[]{"uri", "page_number"}));
        
        array.put(createToolMCP("get_dir_tree", 
            "Retorna uma árvore de diretórios em formato de string.",
            new String[]{"uri"}, null));
        
        // Search tools
        array.put(createToolMCP("search_pathnames_only", 
            "Busca arquivos por nome (somente pathnames).",
            new String[]{"query"}, new String[]{"include_pattern", "page_number"}));
        
        array.put(createToolMCP("search_for_files", 
            "Busca arquivos por conteúdo. Suporta regex.",
            new String[]{"query"}, new String[]{"is_regex", "search_in_folder", "page_number"}));
        
        array.put(createToolMCP("search_in_file", 
            "Busca por uma string ou regex dentro de um arquivo específico.",
            new String[]{"uri", "query"}, new String[]{"is_regex"}));
        
        array.put(createToolMCP("read_lint_errors", 
            "Lê erros de lint de um arquivo.",
            new String[]{"uri"}, null));
        
        // Edit tools
        array.put(createToolMCP("rewrite_file", 
            "Reescreve completamente o conteúdo de um arquivo.",
            new String[]{"uri", "new_content"}, null));
        
        array.put(createToolMCP("edit_file", 
            "Aplica edições em um arquivo usando blocos SEARCH/REPLACE.",
            new String[]{"uri", "search_replace_blocks"}, null));
        
        array.put(createToolMCP("create_file_or_folder", 
            "Cria um arquivo ou pasta. Se o path terminar com / ou \\, é uma pasta.",
            new String[]{"uri"}, null));
        
        array.put(createToolMCP("delete_file_or_folder", 
            "Deleta um arquivo ou pasta.",
            new String[]{"uri"}, new String[]{"is_recursive"}));
        
        // Terminal tools
        array.put(createToolMCP("run_command", 
            "Executa um comando shell e retorna o resultado.",
            new String[]{"command"}, new String[]{"cwd"}));
        
        array.put(createToolMCP("open_persistent_terminal", 
            "Abre um terminal persistente em background.",
            new String[]{}, new String[]{"cwd"}));
        
        array.put(createToolMCP("run_persistent_command", 
            "Executa um comando em um terminal persistente.",
            new String[]{"command", "persistent_terminal_id"}, null));
        
        array.put(createToolMCP("kill_persistent_terminal", 
            "Fecha um terminal persistente.",
            new String[]{"persistent_terminal_id"}, null));
        
        return array;
    }

    private static JSONObject createToolMCP(String name, String description, String[] requiredParams, String[] optionalParams) {
        try {
            JSONObject toolObj = new JSONObject();
            JSONObject function = new JSONObject();
            
            function.put("name", name);
            function.put("description", description);
            
            JSONObject params = new JSONObject();
            params.put("type", "object");
            
            JSONObject properties = new JSONObject();
            for (String param : requiredParams) {
                JSONObject prop = new JSONObject();
                prop.put("type", "string");
                prop.put("description", toolParamDescription(name, param));
                properties.put(param, prop);
            }
            if (optionalParams != null) {
                for (String param : optionalParams) {
                    JSONObject prop = new JSONObject();
                    prop.put("type", "string");
                    prop.put("description", toolParamDescription(name, param));
                    properties.put(param, prop);
                }
            }
            
            params.put("properties", properties);
            
            JSONArray required = new JSONArray();
            for (String param : requiredParams) {
                required.put(param);
            }
            params.put("required", required);
            
            function.put("parameters", params);
            toolObj.put("type", "function");
            toolObj.put("function", function);
            
            return toolObj;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static boolean useVoidToolDescriptions() {
        return true;
    }

    private static String toolParamDescription(String toolName, String paramName) {
        if ("uri".equals(paramName)) {
            if ("ls_dir".equals(toolName)) {
                return "Optional. The FULL path to the folder. Leave this as empty or \"\" to search all folders.";
            }
            if ("get_dir_tree".equals(toolName)) {
                return "The FULL path to the folder.";
            }
            if ("create_file_or_folder".equals(toolName) || "delete_file_or_folder".equals(toolName)) {
                return "The FULL path to the file or folder.";
            }
            return "The FULL path to the file.";
        }
        if ("start_line".equals(paramName)) {
            return "Optional. Do NOT fill this field in unless you were specifically given exact line numbers to search. Defaults to the beginning of the file.";
        }
        if ("end_line".equals(paramName)) {
            return "Optional. Do NOT fill this field in unless you were specifically given exact line numbers to search. Defaults to the end of the file.";
        }
        if ("page_number".equals(paramName)) {
            return "Optional. The page number of the result. Default is 1.";
        }
        if ("query".equals(paramName)) {
            return "search_in_file".equals(toolName)
                    ? "The string or regex to search for in the file."
                    : "Your query for the search.";
        }
        if ("include_pattern".equals(paramName)) {
            return "Optional. Only fill this in if you need to limit your search because there were too many results.";
        }
        if ("search_in_folder".equals(paramName)) {
            return "Optional. Leave as blank by default. ONLY fill this in if your previous search with the same query was truncated. Searches descendants of this folder only.";
        }
        if ("is_regex".equals(paramName)) {
            return "Optional. Default is false. Whether the query is a regex.";
        }
        if ("is_recursive".equals(paramName)) {
            return "Optional. Return true to delete recursively.";
        }
        if ("search_replace_blocks".equals(paramName)) {
            return "A string of SEARCH/REPLACE block(s) which will be applied to the given file. The ORIGINAL code in each SEARCH/REPLACE block must EXACTLY match lines in the original file. This field is a STRING (not an array).";
        }
        if ("new_content".equals(paramName)) {
            return "The new contents of the file. Must be a string.";
        }
        if ("command".equals(paramName)) {
            return "The terminal command to run.";
        }
        if ("cwd".equals(paramName)) {
            return "Optional. The directory in which to run the command. Defaults to the first workspace folder.";
        }
        if ("persistent_terminal_id".equals(paramName)) {
            return "kill_persistent_terminal".equals(toolName)
                    ? "The ID of the persistent terminal."
                    : "The ID of the terminal created using open_persistent_terminal.";
        }
        return "";
    }

    // ============================================
    // MAIN TOOL EXECUTOR
    // ============================================

    public static String executeTool(String scId, String toolName, JSONObject args) {
        try {
            ToolCallResult result;
            
            switch (toolName) {
                case "read_file":
                    result = readFile(scId, 
                        args.opt("uri"),
                        args.opt("start_line"),
                        args.opt("end_line"),
                        args.opt("page_number"));
                    break;
                    
                case "ls_dir":
                    result = lsDir(scId,
                        args.opt("uri"),
                        args.opt("page_number"));
                    break;
                    
                case "get_dir_tree":
                    result = getDirTree(scId, args.opt("uri"));
                    break;
                    
                case "search_pathnames_only":
                    result = searchPathnamesOnly(scId,
                        args.opt("query"),
                        args.opt("include_pattern"),
                        args.opt("page_number"));
                    break;
                    
                case "search_for_files":
                    result = searchForFiles(scId,
                        args.opt("query"),
                        args.opt("is_regex"),
                        args.opt("search_in_folder"),
                        args.opt("page_number"));
                    break;
                    
                case "search_in_file":
                    result = searchInFile(scId,
                        args.opt("uri"),
                        args.opt("query"),
                        args.opt("is_regex"));
                    break;
                    
                case "read_lint_errors":
                    result = readLintErrors(scId, args.opt("uri"));
                    break;
                    
                case "rewrite_file":
                    result = rewriteFile(scId,
                        args.opt("uri"),
                        args.opt("new_content"));
                    break;
                    
                case "edit_file":
                    result = editFile(scId,
                        args.opt("uri"),
                        args.opt("search_replace_blocks"));
                    break;
                    
                case "create_file_or_folder":
                    result = createFileOrFolder(scId, args.opt("uri"));
                    break;
                    
                case "delete_file_or_folder":
                    result = deleteFileOrFolder(scId,
                        args.opt("uri"),
                        args.opt("is_recursive"));
                    break;
                    
                case "run_command":
                    result = runCommand(scId,
                        args.opt("command"),
                        args.opt("cwd"),
                        args.opt("terminal_id"));
                    break;
                    
                case "open_persistent_terminal":
                    result = openPersistentTerminal(scId, args.opt("cwd"));
                    break;
                    
                case "run_persistent_command":
                    result = runPersistentCommand(scId,
                        args.opt("command"),
                        args.opt("persistent_terminal_id"));
                    break;
                    
                case "kill_persistent_terminal":
                    result = killPersistentTerminal(scId, args.opt("persistent_terminal_id"));
                    break;
                    
                default:
                    return "Ferramenta não encontrada: " + toolName;
            }
            
            return result.result;
            
        } catch (Exception e) {
            return "Erro ao executar ferramenta " + toolName + ": " + e.getMessage();
        }
    }
}
