package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonChatThreadServiceTypesTs implements SourceAsset {
    public static final CommonChatThreadServiceTypesTs INSTANCE = new CommonChatThreadServiceTypesTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { URI } from '../../../../base/common/uri.js';\nimport { VoidFileSnapshot } from './editCodeServiceTypes.js';\nimport { AnthropicReasoning, RawToolParamsObj } from './sendLLMMessageTypes.js';\nimport { ToolCallParams, ToolName, ToolResult } from './toolsServiceTypes.js';\n\nexport type ToolMessage<T extends ToolName> = {\n\trole: 'tool';\n\tcontent: string; // give this result to LLM (string of value)\n\tid: string;\n\trawParams: RawToolParamsObj;\n\tmcpServerName: string | undefined; // the server name at the time of the call\n} & (\n\t\t// in order of events:\n\t\t| { type: 'invalid_params', result: null, name: T, }\n\n\t\t| { type: 'tool_request', result: null, name: T, params: ToolCallParams<T>, }  // params were validated, awaiting user\n\n\t\t| { type: 'running_now', result: null, name: T, params: ToolCallParams<T>, }\n\n\t\t| { type: 'tool_error', result: string, name: T, params: ToolCallParams<T>, } // error when tool was running\n\t\t| { type: 'success', result: Awaited<ToolResult<T>>, name: T, params: ToolCallParams<T>, }\n\t\t| { type: 'rejected', result: null, name: T, params: ToolCallParams<T> }\n\t) // user rejected\n\nexport type DecorativeCanceledTool = {\n\trole: 'interrupted_streaming_tool';\n\tname: ToolName;\n\tmcpServerName: string | undefined; // the server name at the time of the call\n}\n\n\n// checkpoints\nexport type CheckpointEntry = {\n\trole: 'checkpoint';\n\ttype: 'user_edit' | 'tool_edit';\n\tvoidFileSnapshotOfURI: { [fsPath: string]: VoidFileSnapshot | undefined };\n\n\tuserModifications: {\n\t\tvoidFileSnapshotOfURI: { [fsPath: string]: VoidFileSnapshot | undefined };\n\t};\n}\n\n\n// WARNING: changing this format is a big deal!!!!!! need to migrate old format to new format on users' computers so people don't get errors.\nexport type ChatMessage =\n\t| {\n\t\trole: 'user';\n\t\tcontent: string; // content displayed to the LLM on future calls - allowed to be '', will be replaced with (empty)\n\t\tdisplayContent: string; // content displayed to user  - allowed to be '', will be ignored\n\t\tselections: StagingSelectionItem[] | null; // the user's selection\n\t\tstate: {\n\t\t\tstagingSelections: StagingSelectionItem[];\n\t\t\tisBeingEdited: boolean;\n\t\t}\n\t} | {\n\t\trole: 'assistant';\n\t\tdisplayContent: string; // content received from LLM  - allowed to be '', will be replaced with (empty)\n\t\treasoning: string; // reasoning from the LLM, used for step-by-step thinking\n\n\t\tanthropicReasoning: AnthropicReasoning[] | null; // anthropic reasoning\n\t}\n\t| ToolMessage<ToolName>\n\t| DecorativeCanceledTool\n\t| CheckpointEntry\n\n\n// one of the square items that indicates a selection in a chat bubble\nexport type StagingSelectionItem = {\n\ttype: 'File';\n\turi: URI;\n\tlanguage: string;\n\tstate: { wasAddedAsCurrentFile: boolean; };\n} | {\n\ttype: 'CodeSelection';\n\trange: [number, number];\n\turi: URI;\n\tlanguage: string;\n\tstate: { wasAddedAsCurrentFile: boolean; };\n} | {\n\ttype: 'Folder';\n\turi: URI;\n\tlanguage?: undefined;\n\tstate?: undefined;\n}\n\n\n// a link to a symbol (an underlined link to a piece of code)\nexport type CodespanLocationLink = {\n\turi: URI, // we handle serialization for this\n\tdisplayText: string,\n\tselection?: { // store as JSON so dont have to worry about serialization\n\t\tstartLineNumber: number\n\t\tstartColumn: number,\n\t\tendLineNumber: number\n\t\tendColumn: number,\n\t} | undefined\n} | null\n"
    };

    private CommonChatThreadServiceTypesTs() {
    }

    @Override
    public String path() {
        return "common/chatThreadServiceTypes.ts";
    }

    @Override
    public String sha256() {
        return "83eb5e83e734322093430c209d30ae24a5c5569c2ffe2fe7487206c211155bb5";
    }

    @Override
    public int originalByteLength() {
        return 3596;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
