package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserQuickEditActionsTs implements SourceAsset {
    public static final BrowserQuickEditActionsTs INSTANCE = new BrowserQuickEditActionsTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { KeyCode, KeyMod } from '../../../../base/common/keyCodes.js';\nimport { Action2, registerAction2 } from '../../../../platform/actions/common/actions.js';\nimport { ServicesAccessor } from '../../../../platform/instantiation/common/instantiation.js';\nimport { KeybindingWeight } from '../../../../platform/keybinding/common/keybindingsRegistry.js';\nimport { ICodeEditorService } from '../../../../editor/browser/services/codeEditorService.js';\nimport { IEditCodeService } from './editCodeServiceInterface.js';\nimport { roundRangeToLines } from './sidebarActions.js';\nimport { VOID_CTRL_K_ACTION_ID } from './actionIDs.js';\nimport { localize2 } from '../../../../nls.js';\nimport { IMetricsService } from '../common/metricsService.js';\nimport { ContextKeyExpr } from '../../../../platform/contextkey/common/contextkey.js';\n\nexport type QuickEditPropsType = {\n\tdiffareaid: number,\n\ttextAreaRef: (ref: HTMLTextAreaElement | null) => void;\n\tonChangeHeight: (height: number) => void;\n\tonChangeText: (text: string) => void;\n\tinitText: string | null;\n}\n\nexport type QuickEdit = {\n\tstartLine: number, // 0-indexed\n\tbeforeCode: string,\n\tafterCode?: string,\n\tinstructions?: string,\n\tresponseText?: string, // model can produce a text response too\n}\n\n\nregisterAction2(class extends Action2 {\n\tconstructor(\n\t) {\n\t\tsuper({\n\t\t\tid: VOID_CTRL_K_ACTION_ID,\n\t\t\tf1: true,\n\t\t\ttitle: localize2('voidQuickEditAction', 'Void: Quick Edit'),\n\t\t\tkeybinding: {\n\t\t\t\tprimary: KeyMod.CtrlCmd | KeyCode.KeyK,\n\t\t\t\tweight: KeybindingWeight.VoidExtension,\n\t\t\t\twhen: ContextKeyExpr.deserialize('editorFocus && !terminalFocus'),\n\t\t\t}\n\t\t});\n\t}\n\n\tasync run(accessor: ServicesAccessor): Promise<void> {\n\n\t\tconst editorService = accessor.get(ICodeEditorService)\n\t\tconst metricsService = accessor.get(IMetricsService)\n\t\tmetricsService.capture('Ctrl+K', {})\n\n\t\tconst editor = editorService.getActiveCodeEditor()\n\t\tif (!editor) return;\n\t\tconst model = editor.getModel()\n\t\tif (!model) return;\n\t\tconst selection = roundRangeToLines(editor.getSelection(), { emptySelectionBehavior: 'line' })\n\t\tif (!selection) return;\n\n\n\t\tconst { startLineNumber: startLine, endLineNumber: endLine } = selection\n\n\t\tconst editCodeService = accessor.get(IEditCodeService)\n\t\teditCodeService.addCtrlKZone({ startLine, endLine, editor })\n\t}\n});\n"
    };

    private BrowserQuickEditActionsTs() {
    }

    @Override
    public String path() {
        return "browser/quickEditActions.ts";
    }

    @Override
    public String sha256() {
        return "9f035d600b1605965a15bbda682f5493754d4864d6f9893811a13953df682276";
    }

    @Override
    public int originalByteLength() {
        return 2614;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
