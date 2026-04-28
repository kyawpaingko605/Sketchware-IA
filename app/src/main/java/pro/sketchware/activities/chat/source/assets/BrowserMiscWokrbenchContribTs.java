package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserMiscWokrbenchContribTs implements SourceAsset {
    public static final BrowserMiscWokrbenchContribTs INSTANCE = new BrowserMiscWokrbenchContribTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Disposable } from '../../../../base/common/lifecycle.js';\nimport { IWorkbenchContribution, registerWorkbenchContribution2, WorkbenchPhase } from '../../../common/contributions.js';\nimport { IExtensionTransferService } from './extensionTransferService.js';\nimport { os } from '../common/helpers/systemInfo.js';\nimport { IStorageService, StorageScope, StorageTarget } from '../../../../platform/storage/common/storage.js';\nimport { timeout } from '../../../../base/common/async.js';\nimport { getActiveWindow } from '../../../../base/browser/dom.js';\n\n// Onboarding contribution that mounts the component at startup\nexport class MiscWorkbenchContribs extends Disposable implements IWorkbenchContribution {\n\tstatic readonly ID = 'workbench.contrib.voidMiscWorkbenchContribs';\n\n\tconstructor(\n\t\t@IExtensionTransferService private readonly extensionTransferService: IExtensionTransferService,\n\t\t@IStorageService private readonly storageService: IStorageService,\n\t) {\n\t\tsuper();\n\t\tthis.initialize();\n\t}\n\n\tprivate initialize(): void {\n\n\t\t// delete blacklisted extensions once (this is for people who already installed them)\n\t\tconst deleteExtensionsStorageId = 'void-deleted-blacklist-2'\n\t\tconst alreadyDeleted = this.storageService.get(deleteExtensionsStorageId, StorageScope.APPLICATION)\n\t\tif (!alreadyDeleted) {\n\t\t\tthis.storageService.store(deleteExtensionsStorageId, 'true', StorageScope.APPLICATION, StorageTarget.MACHINE)\n\t\t\tthis.extensionTransferService.deleteBlacklistExtensions(os)\n\t\t}\n\n\n\t\t// after some time, trigger a resize event for the blank screen error\n\t\ttimeout(5_000).then(() => {\n\t\t\t// Get the active window reference for multi-window support\n\t\t\tconst targetWindow = getActiveWindow();\n\t\t\t// Trigger a window resize event to ensure proper layout calculations\n\t\t\ttargetWindow.dispatchEvent(new Event('resize'))\n\n\t\t})\n\n\t}\n}\n\nregisterWorkbenchContribution2(MiscWorkbenchContribs.ID, MiscWorkbenchContribs, WorkbenchPhase.Eventually);\n"
    };

    private BrowserMiscWokrbenchContribTs() {
    }

    @Override
    public String path() {
        return "browser/miscWokrbenchContrib.ts";
    }

    @Override
    public String sha256() {
        return "ab7a94cc20fe5e509653beda8ad4ee0a1ff99e5f3e4de56ab5377f9a7e84ad78";
    }

    @Override
    public int originalByteLength() {
        return 2281;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
