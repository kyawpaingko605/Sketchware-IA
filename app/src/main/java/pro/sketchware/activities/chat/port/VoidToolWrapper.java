package pro.sketchware.activities.chat.port;

import org.json.JSONArray;
import org.json.JSONObject;

import pro.sketchware.SketchApplication;
import pro.sketchware.ia.tools.Tool;
import pro.sketchware.ia.tools.ToolManager;

/**
 * Wrapper that adapts VoidPortToolsService builtin tools to the Tool interface.
 * All Void tools are prioritized over legacy Sketchware-IA tools.
 */
public class VoidToolWrapper implements Tool {
    private final String toolName;
    private final String description;
    private final JSONObject parameters;
    private final boolean requiresApproval;
    private final boolean isDestructive;

    public VoidToolWrapper(String toolName, String description, JSONObject parameters, 
                          boolean requiresApproval, boolean isDestructive) {
        this.toolName = toolName;
        this.description = description;
        this.parameters = parameters;
        this.requiresApproval = requiresApproval;
        this.isDestructive = isDestructive;
    }

    @Override
    public String getName() {
        return toolName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public JSONObject getParameters() {
        return parameters;
    }

    @Override
    public String execute(String scId, JSONObject args) throws Exception {
        return VoidPortToolsService.executeTool(scId, toolName, args);
    }

    @Override
    public boolean requiresApproval() {
        return requiresApproval;
    }

    @Override
    public boolean isDestructive() {
        return isDestructive;
    }

    public static void registerAllVoidTools(ToolManager manager) {
        if (registerVoidToolDefinitions(manager)) {
            return;
        }

        // File tools - read operations (no approval required)
        manager.registerTool(new VoidToolWrapper(
            "read_file",
            "Lê o conteúdo de um arquivo. Suporta paginação e seleção de linhas.",
            createParams(new String[]{"uri"}, new String[][]{
                {"startLine", "number", "Linha inicial (opcional)"},
                {"endLine", "number", "Linha final (opcional)"},
                {"pageNumber", "number", "Número da página (default: 1)"}
            }),
            false,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "ls_dir",
            "Lista arquivos e pastas em um diretório. Suporta paginação.",
            createParams(new String[]{"uri"}, new String[][]{
                {"pageNumber", "number", "Número da página (default: 1)"}
            }),
            false,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "get_dir_tree",
            "Retorna uma árvore de diretórios em formato de string.",
            createParams(new String[]{"uri"}, null),
            false,
            false
        ));

        // Search tools (no approval required)
        manager.registerTool(new VoidToolWrapper(
            "search_pathnames_only",
            "Busca arquivos por nome (somente pathnames).",
            createParams(new String[]{"query"}, new String[][]{
                {"includePattern", "string", "Padrão de inclusão (opcional)"},
                {"pageNumber", "number", "Número da página (default: 1)"}
            }),
            false,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "search_for_files",
            "Busca arquivos por conteúdo. Suporta regex.",
            createParams(new String[]{"query"}, new String[][]{
                {"isRegex", "boolean", "Se true, trata query como regex"},
                {"searchInFolder", "string", "Pasta para limitar busca (opcional)"},
                {"pageNumber", "number", "Número da página (default: 1)"}
            }),
            false,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "search_in_file",
            "Busca por uma string ou regex dentro de um arquivo específico.",
            createParams(new String[]{"uri", "query"}, new String[][]{
                {"isRegex", "boolean", "Se true, trata query como regex"}
            }),
            false,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "read_lint_errors",
            "Lê erros de lint/diagnóstico de um arquivo.",
            createParams(new String[]{"uri"}, null),
            false,
            false
        ));

        // Edit tools - require approval (destructive operations)
        manager.registerTool(new VoidToolWrapper(
            "rewrite_file",
            "Reescreve completamente o conteúdo de um arquivo.",
            createParams(new String[]{"uri", "newContent"}, null),
            true,
            true
        ));

        manager.registerTool(new VoidToolWrapper(
            "edit_file",
            "Aplica edições em um arquivo usando blocos SEARCH/REPLACE. Formato: <<<<<<< ORIGINAL\n...\n=======\n...\n>>>>>>> UPDATED",
            createParams(new String[]{"uri", "searchReplaceBlocks"}, null),
            true,
            true
        ));

        manager.registerTool(new VoidToolWrapper(
            "create_file_or_folder",
            "Cria um arquivo ou pasta.",
            createParams(new String[]{"uri", "isFolder"}, null),
            true,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "delete_file_or_folder",
            "Deleta um arquivo ou pasta.",
            createParams(new String[]{"uri", "isRecursive", "isFolder"}, null),
            true,
            true
        ));

        // Terminal tools - require approval
        manager.registerTool(new VoidToolWrapper(
            "run_command",
            "Executa um comando shell e retorna o resultado.",
            createParams(new String[]{"command", "terminalId"}, new String[][]{
                {"cwd", "string", "Diretório de trabalho (opcional)"}
            }),
            true,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "open_persistent_terminal",
            "Abre um terminal persistente em background.",
            createParams(new String[]{}, new String[][]{
                {"cwd", "string", "Diretório de trabalho (opcional)"}
            }),
            true,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "run_persistent_command",
            "Executa um comando em um terminal persistente.",
            createParams(new String[]{"command", "persistentTerminalId"}, null),
            true,
            false
        ));

        manager.registerTool(new VoidToolWrapper(
            "kill_persistent_terminal",
            "Fecha um terminal persistente.",
            createParams(new String[]{"persistentTerminalId"}, null),
            true,
            false
        ));
    }

    private static boolean registerVoidToolDefinitions(ToolManager manager) {
        if (manager == null) {
            return false;
        }
        JSONArray toolDefinitions = VoidPortToolsService.getAllToolsAsMCP();
        boolean registeredAny = false;
        for (int i = 0; toolDefinitions != null && i < toolDefinitions.length(); i++) {
            JSONObject toolObject = toolDefinitions.optJSONObject(i);
            JSONObject function = toolObject == null ? null : toolObject.optJSONObject("function");
            if (function == null) {
                continue;
            }
            String name = function.optString("name", "").trim();
            if (name.isEmpty()) {
                continue;
            }
            JSONObject parameters = function.optJSONObject("parameters");
            manager.registerTool(new VoidToolWrapper(
                    name,
                    function.optString("description", ""),
                    parameters == null ? new JSONObject() : parameters,
                    requiresApprovalFor(name),
                    isDestructiveTool(name)
            ));
            registeredAny = true;
        }
        return registeredAny;
    }

    private static boolean requiresApprovalFor(String toolName) {
        return switch (toolName) {
            case "rewrite_file", "edit_file", "create_file_or_folder", "delete_file_or_folder",
                    "run_command", "open_persistent_terminal", "run_persistent_command",
                    "kill_persistent_terminal" -> true;
            default -> false;
        };
    }

    private static boolean isDestructiveTool(String toolName) {
        return switch (toolName) {
            case "rewrite_file", "edit_file", "delete_file_or_folder" -> true;
            default -> false;
        };
    }

    /**
     * Creates parameter schema with required and optional parameters.
     * @param optional Array of [name, type, description] for optional parameters
     */
    private static JSONObject createParams(String[] required, String[][] optional) {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");
            
            JSONObject properties = new JSONObject();
            
            // Add required parameters
            for (String param : required) {
                JSONObject prop = new JSONObject();
                prop.put("type", "string");
                prop.put("description", "Parâmetro obrigatório: " + param);
                properties.put(param, prop);
            }
            
            // Add optional parameters
            if (optional != null) {
                for (String[] opt : optional) {
                    JSONObject prop = new JSONObject();
                    prop.put("type", opt[1]);
                    prop.put("description", opt[2]);
                    properties.put(opt[0], prop);
                }
            }
            
            params.put("properties", properties);
            
            JSONArray requiredArray = new JSONArray();
            for (String param : required) {
                requiredArray.put(param);
            }
            params.put("required", requiredArray);
            
            return params;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }
}
