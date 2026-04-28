package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserConvertToLLMMessageWorkbenchContribTs implements SourceAsset {
    public static final BrowserConvertToLLMMessageWorkbenchContribTs INSTANCE = new BrowserConvertToLLMMessageWorkbenchContribTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Disposable } from '../../../../base/common/lifecycle.js';\nimport { URI } from '../../../../base/common/uri.js';\nimport { IWorkspaceContextService } from '../../../../platform/workspace/common/workspace.js';\nimport { IWorkbenchContribution, registerWorkbenchContribution2, WorkbenchPhase } from '../../../common/contributions.js';\nimport { IVoidModelService } from '../common/voidModelService.js';\n\nclass ConvertContribWorkbenchContribution extends Disposable implements IWorkbenchContribution {\n\tstatic readonly ID = 'workbench.contrib.void.convertcontrib'\n\t_serviceBrand: undefined;\n\n\tconstructor(\n\t\t@IVoidModelService private readonly voidModelService: IVoidModelService,\n\t\t@IWorkspaceContextService private readonly workspaceContext: IWorkspaceContextService,\n\t) {\n\t\tsuper()\n\n\t\tconst initializeURI = (uri: URI) => {\n\t\t\tthis.workspaceContext.getWorkspace()\n\t\t\tconst voidRulesURI = URI.joinPath(uri, '.voidrules')\n\t\t\tthis.voidModelService.initializeModel(voidRulesURI)\n\t\t}\n\n\t\t// call\n\t\tthis._register(this.workspaceContext.onDidChangeWorkspaceFolders((e) => {\n\t\t\t[...e.changed, ...e.added].forEach(w => { initializeURI(w.uri) })\n\t\t}))\n\t\tthis.workspaceContext.getWorkspace().folders.forEach(w => { initializeURI(w.uri) })\n\t}\n}\n\n\nregisterWorkbenchContribution2(ConvertContribWorkbenchContribution.ID, ConvertContribWorkbenchContribution, WorkbenchPhase.BlockRestore);\n"
    };

    private BrowserConvertToLLMMessageWorkbenchContribTs() {
    }

    @Override
    public String path() {
        return "browser/convertToLLMMessageWorkbenchContrib.ts";
    }

    @Override
    public String sha256() {
        return "907bf8cbe269f208b69ede73bce446ee5afa592255cbf111e5739fd67d2a8ec9";
    }

    @Override
    public int originalByteLength() {
        return 1708;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
