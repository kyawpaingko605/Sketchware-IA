package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserEditCodeServiceInterfaceTs implements SourceAsset {
    public static final BrowserEditCodeServiceInterfaceTs INSTANCE = new BrowserEditCodeServiceInterfaceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Event } from '../../../../base/common/event.js';\nimport { URI } from '../../../../base/common/uri.js';\nimport { ICodeEditor } from '../../../../editor/browser/editorBrowser.js';\nimport { createDecorator } from '../../../../platform/instantiation/common/instantiation.js';\nimport { Diff, DiffArea, VoidFileSnapshot } from '../common/editCodeServiceTypes.js';\n\n\nexport type StartBehavior = 'accept-conflicts' | 'reject-conflicts' | 'keep-conflicts'\n\nexport type CallBeforeStartApplyingOpts = {\n\tfrom: 'QuickEdit';\n\tdiffareaid: number; // id of the CtrlK area (contains text selection)\n} | {\n\tfrom: 'ClickApply';\n\turi: 'current' | URI;\n}\n\nexport type StartApplyingOpts = {\n\tfrom: 'QuickEdit';\n\tdiffareaid: number; // id of the CtrlK area (contains text selection)\n\tstartBehavior: StartBehavior;\n} | {\n\tfrom: 'ClickApply';\n\tapplyStr: string;\n\turi: 'current' | URI;\n\tstartBehavior: StartBehavior;\n}\n\nexport type AddCtrlKOpts = {\n\tstartLine: number,\n\tendLine: number,\n\teditor: ICodeEditor,\n}\n\nexport const IEditCodeService = createDecorator<IEditCodeService>('editCodeService');\n\nexport interface IEditCodeService {\n\treadonly _serviceBrand: undefined;\n\n\tprocessRawKeybindingText(keybindingStr: string): string;\n\n\tcallBeforeApplyOrEdit(uri: URI | 'current'): Promise<void>;\n\tstartApplying(opts: StartApplyingOpts): [URI, Promise<void>] | null;\n\tinstantlyApplySearchReplaceBlocks(opts: { uri: URI; searchReplaceBlocks: string }): void;\n\tinstantlyRewriteFile(opts: { uri: URI; newContent: string }): void;\n\taddCtrlKZone(opts: AddCtrlKOpts): number | undefined;\n\tremoveCtrlKZone(opts: { diffareaid: number }): void;\n\n\tdiffAreaOfId: Record<string, DiffArea>;\n\tdiffAreasOfURI: Record<string, Set<string> | undefined>;\n\tdiffOfId: Record<string, Diff>;\n\n\tacceptOrRejectAllDiffAreas(opts: { uri: URI, removeCtrlKs: boolean, behavior: 'reject' | 'accept', _addToHistory?: boolean }): void;\n\tacceptDiff({ diffid }: { diffid: number }): void;\n\trejectDiff({ diffid }: { diffid: number }): void;\n\n\t// events\n\tonDidAddOrDeleteDiffZones: Event<{ uri: URI }>;\n\tonDidChangeDiffsInDiffZoneNotStreaming: Event<{ uri: URI; diffareaid: number }>; // only fires when not streaming!!! streaming would be too much\n\tonDidChangeStreamingInDiffZone: Event<{ uri: URI; diffareaid: number }>;\n\tonDidChangeStreamingInCtrlKZone: Event<{ uri: URI; diffareaid: number }>;\n\n\t// CtrlKZone streaming state\n\tisCtrlKZoneStreaming(opts: { diffareaid: number }): boolean;\n\tinterruptCtrlKStreaming(opts: { diffareaid: number }): void;\n\n\t// // DiffZone codeBoxId streaming state\n\tinterruptURIStreaming(opts: { uri: URI }): void;\n\n\t// testDiffs(): void;\n\tgetVoidFileSnapshot(uri: URI): VoidFileSnapshot;\n\trestoreVoidFileSnapshot(uri: URI, snapshot: VoidFileSnapshot): void;\n}\n"
    };

    private BrowserEditCodeServiceInterfaceTs() {
    }

    @Override
    public String path() {
        return "browser/editCodeServiceInterface.ts";
    }

    @Override
    public String sha256() {
        return "3c5565983892ad937855c7aff2f21f6c6afd541d2119b21c31daf726f465462f";
    }

    @Override
    public int originalByteLength() {
        return 3067;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
