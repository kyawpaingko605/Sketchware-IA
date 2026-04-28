package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserDummyContribTs implements SourceAsset {
    public static final BrowserDummyContribTs INSTANCE = new BrowserDummyContribTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { KeyCode, KeyMod } from '../../../../base/common/keyCodes.js';\nimport { Disposable } from '../../../../base/common/lifecycle.js';\nimport { ServicesAccessor } from '../../../../editor/browser/editorExtensions.js';\nimport { ICodeEditorService } from '../../../../editor/browser/services/codeEditorService.js';\nimport { localize2 } from '../../../../nls.js';\nimport { Action2, registerAction2 } from '../../../../platform/actions/common/actions.js';\nimport { InstantiationType, registerSingleton } from '../../../../platform/instantiation/common/extensions.js';\nimport { createDecorator } from '../../../../platform/instantiation/common/instantiation.js';\nimport { KeybindingWeight } from '../../../../platform/keybinding/common/keybindingsRegistry.js';\nimport { IWorkbenchContribution, registerWorkbenchContribution2, WorkbenchPhase } from '../../../common/contributions.js';\n\n\n// to change this, just Cmd+Shift+F and replace DummyService with YourServiceName, and create a unique ID below\nexport interface IDummyService {\n\treadonly _serviceBrand: undefined; // services need this, just leave it undefined\n}\n\nexport const IDummyService = createDecorator<IDummyService>('DummyService');\n\n\n\n// An example of an action (delete if you're not using an action):\nregisterAction2(class extends Action2 {\n\tconstructor() {\n\t\tsuper({\n\t\t\tf1: true,\n\t\t\tid: 'void.dummy',\n\t\t\ttitle: localize2('dummy', 'dummy: Init'),\n\t\t\tkeybinding: {\n\t\t\t\tprimary: KeyMod.CtrlCmd | KeyCode.Digit0,\n\t\t\t\tweight: KeybindingWeight.VoidExtension,\n\t\t\t}\n\t\t});\n\t}\n\tasync run(accessor: ServicesAccessor): Promise<void> {\n\t\tconst n = accessor.get(IDummyService)\n\t\tconsole.log('Hi', n._serviceBrand)\n\t}\n})\n\n\nclass DummyService extends Disposable implements IWorkbenchContribution, IDummyService {\n\tstatic readonly ID = 'workbench.contrib.void.dummy' // workbenchContributions need this, services do not\n\t_serviceBrand: undefined;\n\n\tconstructor(\n\t\t@ICodeEditorService codeEditorService: ICodeEditorService,\n\t) {\n\t\tsuper()\n\n\t}\n}\n\n\n// pick one and delete the other:\nregisterSingleton(IDummyService, DummyService, InstantiationType.Eager); // lazily loaded, even if Eager\n\nregisterWorkbenchContribution2(DummyService.ID, DummyService, WorkbenchPhase.BlockRestore); // mounts on start\n"
    };

    private BrowserDummyContribTs() {
    }

    @Override
    public String path() {
        return "browser/_dummyContrib.ts";
    }

    @Override
    public String sha256() {
        return "8cb12960ca4cd221bde6312f53ab235e6e86b693ae02e75c8f323b14ec6564e4";
    }

    @Override
    public int originalByteLength() {
        return 2574;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
