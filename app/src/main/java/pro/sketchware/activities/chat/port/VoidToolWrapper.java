package pro.sketchware.activities.chat.port;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import pro.sketchware.ia.tools.Tool;
import pro.sketchware.ia.tools.ToolManager;

/**
 * Wrapper that adapts Void builtin tools to the Tool interface.
 */
public class VoidToolWrapper implements Tool {
    private static final String SEARCH_REPLACE_BLOCK_TEMPLATE =
            "<<<<<<< ORIGINAL\n"
                    + "// ... original code goes here\n"
                    + "=======\n"
                    + "// ... final code goes here\n"
                    + ">>>>>>> UPDATED\n\n"
                    + "<<<<<<< ORIGINAL\n"
                    + "// ... original code goes here\n"
                    + "=======\n"
                    + "// ... final code goes here\n"
                    + ">>>>>>> UPDATED";

    private static final String REPLACE_TOOL_DESCRIPTION =
            "A string of SEARCH/REPLACE block(s) which will be applied to the given file.\n"
                    + "Your SEARCH/REPLACE blocks string must be formatted as follows:\n"
                    + SEARCH_REPLACE_BLOCK_TEMPLATE + "\n\n"
                    + "## Guidelines:\n\n"
                    + "1. You may output multiple search replace blocks if needed.\n\n"
                    + "2. The ORIGINAL code in each SEARCH/REPLACE block must EXACTLY match lines in the original file. Do not add or remove any whitespace or comments from the original code.\n\n"
                    + "3. Each ORIGINAL text must be large enough to uniquely identify the change. However, bias towards writing as little as possible.\n\n"
                    + "4. Each ORIGINAL text must be DISJOINT from all other ORIGINAL text.\n\n"
                    + "5. This field is a STRING (not an array).";

    private static final String TERMINAL_DESC_HELPER =
            "You can use this tool to run any command: sed, grep, etc. Do not edit any files with this tool; use edit_file instead. When working with git and other tools that open an editor (e.g. git diff), you should pipe to cat to get all results and not get stuck in vim.";

    private static final String CWD_HELPER =
            "Optional. The directory in which to run the command. Defaults to the first workspace folder.";

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
        manager.registerTool(new VoidToolWrapper(
                "read_file",
                "Returns full contents of a given file.",
                createParams(new String[][]{
                        {"uri", "string", "The FULL path to the file."},
                        {"start_line", "number", "Optional. Do NOT fill this field in unless you were specifically given exact line numbers to search. Defaults to the beginning of the file."},
                        {"end_line", "number", "Optional. Do NOT fill this field in unless you were specifically given exact line numbers to search. Defaults to the end of the file."},
                        {"page_number", "number", "Optional. The page number of the result. Default is 1."}
                }, new String[]{"uri"}),
                false,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "ls_dir",
                "Lists all files and folders in the given URI.",
                createParams(new String[][]{
                        {"uri", "string", "Optional. The FULL path to the folder. Leave this as empty or \"\" to search all folders."},
                        {"page_number", "number", "Optional. The page number of the result. Default is 1."}
                }, new String[]{}),
                false,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "get_dir_tree",
                "This is a very effective way to learn about the user's codebase. Returns a tree diagram of all the files and folders in the given folder. ",
                createParams(new String[][]{
                        {"uri", "string", "The FULL path to the folder."}
                }, new String[]{"uri"}),
                false,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "search_pathnames_only",
                "Returns all pathnames that match a given query (searches ONLY file names). You should use this when looking for a file with a specific name or path.",
                createParams(new String[][]{
                        {"query", "string", "Your query for the search."},
                        {"include_pattern", "string", "Optional. Only fill this in if you need to limit your search because there were too many results."},
                        {"page_number", "number", "Optional. The page number of the result. Default is 1."}
                }, new String[]{"query"}),
                false,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "search_for_files",
                "Returns a list of file names whose content matches the given query. The query can be any substring or regex.",
                createParams(new String[][]{
                        {"query", "string", "Your query for the search."},
                        {"search_in_folder", "string", "Optional. Leave as blank by default. ONLY fill this in if your previous search with the same query was truncated. Searches descendants of this folder only."},
                        {"is_regex", "boolean", "Optional. Default is false. Whether the query is a regex."},
                        {"page_number", "number", "Optional. The page number of the result. Default is 1."}
                }, new String[]{"query"}),
                false,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "search_in_file",
                "Returns an array of all the start line numbers where the content appears in the file.",
                createParams(new String[][]{
                        {"uri", "string", "The FULL path to the file."},
                        {"query", "string", "The string or regex to search for in the file."},
                        {"is_regex", "boolean", "Optional. Default is false. Whether the query is a regex."}
                }, new String[]{"uri", "query"}),
                false,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "read_lint_errors",
                "Use this tool to view all the lint errors on a file.",
                createParams(new String[][]{
                        {"uri", "string", "The FULL path to the file."}
                }, new String[]{"uri"}),
                false,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "create_file_or_folder",
                "Create a file or folder at the given path. To create a folder, the path MUST end with a trailing slash.",
                createParams(new String[][]{
                        {"uri", "string", "The FULL path to the file or folder."}
                }, new String[]{"uri"}),
                true,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "delete_file_or_folder",
                "Delete a file or folder at the given path.",
                createParams(new String[][]{
                        {"uri", "string", "The FULL path to the file or folder."},
                        {"is_recursive", "boolean", "Optional. Return true to delete recursively."}
                }, new String[]{"uri"}),
                true,
                true
        ));

        manager.registerTool(new VoidToolWrapper(
                "edit_file",
                "Edit the contents of a file. You must provide the file's URI as well as a SINGLE string of SEARCH/REPLACE block(s) that will be used to apply the edit.",
                createParams(new String[][]{
                        {"uri", "string", "The FULL path to the file."},
                        {"search_replace_blocks", "string", REPLACE_TOOL_DESCRIPTION}
                }, new String[]{"uri", "search_replace_blocks"}),
                true,
                true
        ));

        manager.registerTool(new VoidToolWrapper(
                "rewrite_file",
                "Edits a file, deleting all the old contents and replacing them with your new contents. Use this tool if you want to edit a file you just created.",
                createParams(new String[][]{
                        {"uri", "string", "The FULL path to the file."},
                        {"new_content", "string", "The new contents of the file. Must be a string."}
                }, new String[]{"uri", "new_content"}),
                true,
                true
        ));

        manager.registerTool(new VoidToolWrapper(
                "run_command",
                "Runs a terminal command and waits for the result (times out after 8s of inactivity). " + TERMINAL_DESC_HELPER,
                createParams(new String[][]{
                        {"command", "string", "The terminal command to run."},
                        {"cwd", "string", CWD_HELPER}
                }, new String[]{"command"}),
                true,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "run_persistent_command",
                "Runs a terminal command in the persistent terminal that you created with open_persistent_terminal (results after 5 are returned, and command continues running in background). " + TERMINAL_DESC_HELPER,
                createParams(new String[][]{
                        {"command", "string", "The terminal command to run."},
                        {"persistent_terminal_id", "string", "The ID of the terminal created using open_persistent_terminal."}
                }, new String[]{"command", "persistent_terminal_id"}),
                true,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "open_persistent_terminal",
                "Use this tool when you want to run a terminal command indefinitely, like a dev server (eg `npm run dev`), a background listener, etc. Opens a new terminal in the user's environment which will not awaited for or killed.",
                createParams(new String[][]{
                        {"cwd", "string", CWD_HELPER}
                }, new String[]{}),
                true,
                false
        ));

        manager.registerTool(new VoidToolWrapper(
                "kill_persistent_terminal",
                "Interrupts and closes a persistent terminal that you opened with open_persistent_terminal.",
                createParams(new String[][]{
                        {"persistent_terminal_id", "string", "The ID of the persistent terminal."}
                }, new String[]{"persistent_terminal_id"}),
                true,
                false
        ));
    }

    private static JSONObject createParams(String[][] specs, String[] requiredNames) {
        try {
            JSONObject params = new JSONObject();
            params.put("type", "object");

            Set<String> requiredSet = new HashSet<>(Arrays.asList(requiredNames));
            JSONObject properties = new JSONObject();
            for (String[] spec : specs) {
                JSONObject prop = new JSONObject();
                prop.put("type", spec[1]);
                prop.put("description", spec[2]);
                properties.put(spec[0], prop);
            }
            params.put("properties", properties);

            JSONArray required = new JSONArray();
            for (String name : requiredSet) {
                required.put(name);
            }
            params.put("required", required);
            return params;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
