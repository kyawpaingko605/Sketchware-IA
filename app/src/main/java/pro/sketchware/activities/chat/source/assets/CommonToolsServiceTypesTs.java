package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonToolsServiceTypesTs implements SourceAsset {
    public static final CommonToolsServiceTypesTs INSTANCE = new CommonToolsServiceTypesTs();

    private static final String[] CHUNKS = new String[] {
            "import { URI } from '../../../../base/common/uri.js'\nimport { RawMCPToolCall } from './mcpServiceTypes.js';\nimport { builtinTools } from './prompt/prompts.js';\nimport { RawToolParamsObj } from './sendLLMMessageTypes.js';\n\n\n\nexport type TerminalResolveReason = { type: 'timeout' } | { type: 'done', exitCode: number }\n\nexport type LintErrorItem = { code: string, message: string, startLineNumber: number, endLineNumber: number }\n\n// Partial of IFileStat\nexport type ShallowDirectoryItem = {\n\turi: URI;\n\tname: string;\n\tisDirectory: boolean;\n\tisSymbolicLink: boolean;\n}\n\n\nexport const approvalTypeOfBuiltinToolName: Partial<{ [T in BuiltinToolName]?: 'edits' | 'terminal' | 'MCP tools' }> = {\n\t'create_file_or_folder': 'edits',\n\t'delete_file_or_folder': 'edits',\n\t'rewrite_file': 'edits',\n\t'edit_file': 'edits',\n\t'run_command': 'terminal',\n\t'run_persistent_command': 'terminal',\n\t'open_persistent_terminal': 'terminal',\n\t'kill_persistent_terminal': 'terminal',\n}\n\n\nexport type ToolApprovalType = NonNullable<(typeof approvalTypeOfBuiltinToolName)[keyof typeof approvalTypeOfBuiltinToolName]>;\n\n\nexport const toolApprovalTypes = new Set<ToolApprovalType>([\n\t...Object.values(approvalTypeOfBuiltinToolName),\n\t'MCP tools',\n])\n\n\n\n\n// PARAMS OF TOOL CALL\nexport type BuiltinToolCallParams = {\n\t'read_file': { uri: URI, startLine: number | null, endLine: number | null, pageNumber: number },\n\t'ls_dir': { uri: URI, pageNumber: number },\n\t'get_dir_tree': { uri: URI },\n\t'search_pathnames_only': { query: string, includePattern: string | null, pageNumber: number },\n\t'search_for_files': { query: string, isRegex: boolean, searchInFolder: URI | null, pageNumber: number },\n\t'search_in_file': { uri: URI, query: string, isRegex: boolean },\n\t'read_lint_errors': { uri: URI },\n\t// ---\n\t'rewrite_file': { uri: URI, newContent: string },\n\t'edit_file': { uri: URI, searchReplaceBlocks: string },\n\t'create_file_or_folder': { uri: URI, isFolder: boolean },\n\t'delete_file_or_folder': { uri: URI, isRecursive: boolean, isFolder: boolean },\n\t// ---\n\t'run_command': { command: string; cwd: string | null, terminalId: string },\n\t'open_persistent_terminal': { cwd: string | null },\n\t'run_persistent_command': { command: string; persistentTerminalId: string },\n\t'kill_persistent_terminal': { persistentTerminalId: string },\n}\n\n// RESULT OF TOOL CALL\nexport type BuiltinToolResultType = {\n\t'read_file': { fileContents: string, totalFileLen: number, totalNumLines: number, hasNextPage: boolean },\n\t'ls_dir': { children: ShallowDirectoryItem[] | null, hasNextPage: boolean, hasPrevPage: boolean, itemsRemaining: number },\n\t'get_dir_tree': { str: string, },\n\t'search_pathnames_only': { uris: URI[], hasNextPage: boolean },\n\t'search_for_files': { uris: URI[], hasNextPage: boolean },\n\t'search_in_file': { lines: number[]; },\n\t'read_lint_errors': { lintErrors: LintErrorItem[] | null },\n\t// ---\n\t'rewrite_file': Promise<{ lintErrors: LintErrorItem[] | null }>,\n\t'edit_file': Promise<{ lintErrors: LintErrorItem[] | null }>,\n\t'create_file_or_folder': {},\n\t'delete_file_or_folder': {},\n\t// ---\n\t'run_command': { result: string; resolveReason: TerminalResolveReason; },\n\t'run_persistent_command': { result: string; resolveReason: TerminalResolveReason; },\n\t'open_persistent_terminal': { persistentTerminalId: string },\n\t'kill_persistent_terminal': {},\n}\n\n\nexport type ToolCallParams<T extends BuiltinToolName | (string & {})> = T extends BuiltinToolName ? BuiltinToolCallParams[T] : RawToolParamsObj\nexport type ToolResult<T extends BuiltinToolName | (string & {})> = T extends BuiltinToolName ? BuiltinToolResultType[T] : RawMCPToolCall\n\nexport type BuiltinToolName = keyof BuiltinToolResultType\n\ntype BuiltinToolParamNameOfTool<T extends BuiltinToolName> = keyof (typeof builtinTools)[T]['params']\nexport type BuiltinToolParamName = { [T in BuiltinToolName]: BuiltinToolParamNameOfTool<T> }[BuiltinToolName]\n\n\nexport type ToolName = BuiltinToolName | (string & {})\nexport type ToolParamName<T extends ToolName> = T extends BuiltinToolName ? BuiltinToolParamNameOfTool<T> : string\n"
    };

    private CommonToolsServiceTypesTs() {
    }

    @Override
    public String path() {
        return "common/toolsServiceTypes.ts";
    }

    @Override
    public String sha256() {
        return "11f53dbe677e8445e0886aa46b3e7bc899bfe18174131b29a9954f3d1d7ccfb7";
    }

    @Override
    public int originalByteLength() {
        return 4046;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
