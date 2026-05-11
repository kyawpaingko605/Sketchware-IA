package pro.sketchware.activities.chat.port;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static final int MAX_FILE_CHARS_PAGE = 24000;
    private static final int MAX_CHILDREN_URIS_PAGE = 50;
    private static final int MAX_TERMINAL_BG_COMMAND_TIME = 30000;
    private static final int MAX_TERMINAL_INACTIVE_TIME = 60000;
    private static final int LINT_ERROR_TIMEOUT = 1000;

    private static final Map<String, Process> activeTerminals = new ConcurrentHashMap<>();
    private static final Map<String, StringBuilder> terminalOutputs = new ConcurrentHashMap<>();

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
            String uriStr = validateStr("uri", uriObj);
            int pageNumber = validatePageNum(pageNumberObj);

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
            if (files == null) {
                return new ToolCallResult("[]");
            }

            int fromIdx = MAX_CHILDREN_URIS_PAGE * (pageNumber - 1);
            int toIdx = MAX_CHILDREN_URIS_PAGE * pageNumber - 1;

            JSONArray resultArray = new JSONArray();
            for (int i = fromIdx; i <= Math.min(toIdx, files.length - 1); i++) {
                File f = files[i];
                JSONObject item = new JSONObject();
                item.put("uri", f.getAbsolutePath());
                item.put("name", f.getName());
                item.put("isDirectory", f.isDirectory());
                item.put("isSymbolicLink", false);
                resultArray.put(item);
            }

            boolean hasNextPage = (files.length - 1) - toIdx >= 1;
            boolean hasPrevPage = pageNumber > 1;
            int itemsRemaining = Math.max(0, files.length - (toIdx + 1));

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
            
            // Simulate lint errors check (in real implementation, integrate with Android lint)
            JSONArray lintErrorsArray = new JSONArray();
            
            // Try to get lint errors from VoidPortMarkerCheckService if available
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

            String newContent = applySearchReplaceBlocks(content, searchReplaceBlocks);

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

            ProjectPathResolver.ResolvedPath resolved = ProjectPathResolver.resolveForWrite(scId, uriStr);
            if (resolved == null) {
                return new ToolCallResult("Cannot delete outside editable project scope: " + uriStr);
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

            // Security check - block dangerous commands on Sketchware files
            String lower = command.toLowerCase();
            String[] blocked = {"cat ", "echo ", "sed ", "grep ", "rm ", "mv ", "cp ", "chmod ", "chown ", "dd ", ">", ">>"};
            for (String b : blocked) {
                if (lower.contains(b)) {
                    return new ToolCallResult("Comando bloqueado por segurança.\n"
                            + "Arquivos do Sketchware são criptografados e não devem ser alterados via shell.\n"
                            + "Use:\n"
                            + "- ls_dir ou get_dir_tree para listar\n"
                            + "- read_file para ler\n"
                            + "- rewrite_file ou edit_file para salvar alteracoes");
                }
            }

            File workingDir = resolveCommandWorkingDir(scId, cwd);
            if (workingDir == null) {
                return new ToolCallResult("Erro: pasta de trabalho fora do escopo do projeto: " + (cwd == null ? "" : cwd));
            }

            if (!workingDir.exists()) {
                return new ToolCallResult("Erro: pasta de trabalho não encontrada: " + workingDir.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeTerminals.put(terminalId, process);
            terminalOutputs.put(terminalId, new StringBuilder());

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                activeTerminals.remove(terminalId);
                terminalOutputs.remove(terminalId);
                return new ToolCallResult("O comando excedeu o tempo maximo de execucao.");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = terminalOutputs.get(terminalId);
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.exitValue();
            activeTerminals.remove(terminalId);
            
            String finalOutput = output.toString().trim();
            String normalizedOutput = finalOutput.isEmpty() ? "(sem saida)" : 
                    (finalOutput.length() > 3000 ? finalOutput.substring(0, 3000) : finalOutput);

            JSONObject resultObj = new JSONObject();
            resultObj.put("result", normalizedOutput);
            resultObj.put("exitCode", exitCode);
            resultObj.put("resolveReason", "done");

            return new ToolCallResult(resultObj.toString());
        } catch (Exception e) {
            return new ToolCallResult("Falha ao executar comando: " + e.getMessage());
        }
    }

    public static ToolCallResult openPersistentTerminal(String scId, Object cwdObj) {
        try {
            String cwd = validateOptionalStr("cwd", cwdObj);
            String terminalId = java.util.UUID.randomUUID().toString();

            File workingDir = resolveCommandWorkingDir(scId, cwd);
            if (workingDir == null) {
                return new ToolCallResult("Erro: pasta de trabalho fora do escopo do projeto: " + (cwd == null ? "" : cwd));
            }

            // Create a persistent shell process
            ProcessBuilder pb = new ProcessBuilder("sh", "-i");
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            activeTerminals.put(terminalId, process);
            terminalOutputs.put(terminalId, new StringBuilder());

            JSONObject resultObj = new JSONObject();
            resultObj.put("persistentTerminalId", terminalId);
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

            // Send command to the persistent terminal
            try (java.io.OutputStream os = process.getOutputStream()) {
                os.write((command + "\n").getBytes());
                os.flush();
            }

            // Wait for command to complete (with timeout)
            Thread.sleep(5000);

            StringBuilder output = terminalOutputs.get(terminalId);
            String result = output != null ? output.toString() : "";
            
            // Clear the output buffer
            if (output != null) {
                output.setLength(0);
            }

            JSONObject resultObj = new JSONObject();
            resultObj.put("result", result);
            resultObj.put("resolveReason", "done");
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

    private static File resolveCommandWorkingDir(String scId, String cwd) {
        if (cwd == null || cwd.trim().isEmpty()) {
            return ProjectPathResolver.getDefaultWorkingRoot(scId);
        }
        ProjectPathResolver.ResolvedPath resolved = ProjectPathResolver.resolveForRead(scId, cwd);
        if (resolved == null) {
            return null;
        }
        File file = resolved.getFile();
        return file.isDirectory() ? file : file.getParentFile();
    }

    private static String applySearchReplaceBlocks(String content, String searchReplaceBlocks) throws IOException {
        String result = content;
        boolean appliedAnyBlock = false;
        
        // Parse search/replace blocks
        Pattern pattern = Pattern.compile(
            "<<<<<<< ORIGINAL\\s*\\n(.*?)\\s*=======\\s*\\n(.*?)\\s*>>>>>>> UPDATED",
            Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(searchReplaceBlocks);
        
        while (matcher.find()) {
            String search = matcher.group(1);
            String replace = matcher.group(2);
            int index = result.indexOf(search);
            if (index < 0) {
                throw new IOException("Original block not found. No file changes were saved.");
            }
            result = result.substring(0, index) + replace + result.substring(index + search.length());
            appliedAnyBlock = true;
        }

        if (!appliedAnyBlock) {
            throw new IOException("No valid search/replace blocks were provided. No file changes were saved.");
        }
        
        return result;
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
        
        // File tools
        array.put(createToolMCP("read_file", 
            "Lê o conteúdo de um arquivo. Suporta paginação e seleção de linhas.",
            new String[]{"uri", "start_line", "end_line", "page_number"}));
        
        array.put(createToolMCP("ls_dir", 
            "Lista arquivos e pastas em um diretório. Suporta paginação.",
            new String[]{"uri", "page_number"}));
        
        array.put(createToolMCP("get_dir_tree", 
            "Retorna uma árvore de diretórios em formato de string.",
            new String[]{"uri"}));
        
        // Search tools
        array.put(createToolMCP("search_pathnames_only", 
            "Busca arquivos por nome (somente pathnames).",
            new String[]{"query", "include_pattern", "page_number"}));
        
        array.put(createToolMCP("search_for_files", 
            "Busca arquivos por conteúdo. Suporta regex.",
            new String[]{"query", "is_regex", "search_in_folder", "page_number"}));
        
        array.put(createToolMCP("search_in_file", 
            "Busca por uma string ou regex dentro de um arquivo específico.",
            new String[]{"uri", "query", "is_regex"}));
        
        array.put(createToolMCP("read_lint_errors", 
            "Lê erros de lint de um arquivo.",
            new String[]{"uri"}));
        
        // Edit tools
        array.put(createToolMCP("rewrite_file", 
            "Reescreve completamente o conteúdo de um arquivo.",
            new String[]{"uri", "new_content"}));
        
        array.put(createToolMCP("edit_file", 
            "Aplica edições em um arquivo usando blocos SEARCH/REPLACE.",
            new String[]{"uri", "search_replace_blocks"}));
        
        array.put(createToolMCP("create_file_or_folder", 
            "Cria um arquivo ou pasta. Se o path terminar com / ou \\, é uma pasta.",
            new String[]{"uri"}));
        
        array.put(createToolMCP("delete_file_or_folder", 
            "Deleta um arquivo ou pasta.",
            new String[]{"uri", "is_recursive"}));
        
        // Terminal tools
        array.put(createToolMCP("run_command", 
            "Executa um comando shell e retorna o resultado.",
            new String[]{"command", "cwd"}));
        
        array.put(createToolMCP("open_persistent_terminal", 
            "Abre um terminal persistente em background.",
            new String[]{"cwd"}));
        
        array.put(createToolMCP("run_persistent_command", 
            "Executa um comando em um terminal persistente.",
            new String[]{"command", "persistent_terminal_id"}));
        
        array.put(createToolMCP("kill_persistent_terminal", 
            "Fecha um terminal persistente.",
            new String[]{"persistent_terminal_id"}));
        
        return array;
    }

    private static JSONObject createToolMCP(String name, String description, String[] requiredParams) {
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
                properties.put(param, prop);
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
