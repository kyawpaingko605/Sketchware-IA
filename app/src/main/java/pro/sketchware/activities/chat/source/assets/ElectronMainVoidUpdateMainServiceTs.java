package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class ElectronMainVoidUpdateMainServiceTs implements SourceAsset {
    public static final ElectronMainVoidUpdateMainServiceTs INSTANCE = new ElectronMainVoidUpdateMainServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Disposable } from '../../../../base/common/lifecycle.js';\nimport { IEnvironmentMainService } from '../../../../platform/environment/electron-main/environmentMainService.js';\nimport { IProductService } from '../../../../platform/product/common/productService.js';\nimport { IUpdateService, StateType } from '../../../../platform/update/common/update.js';\nimport { IVoidUpdateService } from '../common/voidUpdateService.js';\nimport { VoidCheckUpdateRespose } from '../common/voidUpdateServiceTypes.js';\n\n\n\nexport class VoidMainUpdateService extends Disposable implements IVoidUpdateService {\n\t_serviceBrand: undefined;\n\n\tconstructor(\n\t\t@IProductService private readonly _productService: IProductService,\n\t\t@IEnvironmentMainService private readonly _envMainService: IEnvironmentMainService,\n\t\t@IUpdateService private readonly _updateService: IUpdateService,\n\t) {\n\t\tsuper()\n\t}\n\n\n\tasync check(explicit: boolean): Promise<VoidCheckUpdateRespose> {\n\n\t\tconst isDevMode = !this._envMainService.isBuilt // found in abstractUpdateService.ts\n\n\t\tif (isDevMode) {\n\t\t\treturn { message: null } as const\n\t\t}\n\n\t\t// if disabled and not explicitly checking, return early\n\t\tif (this._updateService.state.type === StateType.Disabled) {\n\t\t\tif (!explicit)\n\t\t\t\treturn { message: null } as const\n\t\t}\n\n\t\tthis._updateService.checkForUpdates(false) // implicity check, then handle result ourselves\n\n\t\tconsole.log('updateState', this._updateService.state)\n\n\t\tif (this._updateService.state.type === StateType.Uninitialized) {\n\t\t\t// The update service hasn't been initialized yet\n\t\t\treturn { message: explicit ? 'Checking for updates soon...' : null, action: explicit ? 'reinstall' : undefined } as const\n\t\t}\n\n\t\tif (this._updateService.state.type === StateType.Idle) {\n\t\t\t// No updates currently available\n\t\t\treturn { message: explicit ? 'No updates found!' : null, action: explicit ? 'reinstall' : undefined } as const\n\t\t}\n\n\t\tif (this._updateService.state.type === StateType.CheckingForUpdates) {\n\t\t\t// Currently checking for updates\n\t\t\treturn { message: explicit ? 'Checking for updates...' : null } as const\n\t\t}\n\n\t\tif (this._updateService.state.type === StateType.AvailableForDownload) {\n\t\t\t// Update available but requires manual download (mainly for Linux)\n\t\t\treturn { message: 'A new update is available!', action: 'download', } as const\n\t\t}\n\n\t\tif (this._updateService.state.type === StateType.Downloading) {\n\t\t\t// Update is currently being downloaded\n\t\t\treturn { message: explicit ? 'Currently downloading update...' : null } as const\n\t\t}\n\n\t\tif (this._updateService.state.type === StateType.Downloaded) {\n\t\t\t// Update has been downloaded but not yet ready\n\t\t\treturn { message: explicit ? 'An update is ready to be applied!' : null, action: 'apply' } as const\n\t\t}\n\n\t\tif (this._updateService.state.type === StateType.Updating) {\n\t\t\t// Update is being applied\n\t\t\treturn { message: explicit ? 'Applying update...' : null } as const\n\t\t}\n\n\t\tif (this._updateService.state.type === StateType.Ready) {\n\t\t\t// Update is ready\n\t\t\treturn { message: 'Restart Void to update!', action: 'restart' } as const\n\t\t}\n\n\t\tif (this._updateService.state.type === StateType.Disabled) {\n\t\t\treturn await this._manualCheckGHTagIfDisabled(explicit)\n\t\t}\n\t\treturn null\n\t}\n\n\n\n\n\n\n\tprivate async _manualCheckGHTagIfDisabled(explicit: boolean): Promise<VoidCheckUpdateRespose> {\n\t\ttry {\n\t\t\tconst response = await fetch('https://api.github.com/repos/voideditor/binaries/releases/latest');\n\n\t\t\tconst data = await response.json();\n\t\t\tconst version = data.tag_name;\n\n\t\t\tconst myVersion = this._productService.version\n\t\t\tconst latestVersion = version\n\n\t\t\tconst isUpToDate = myVersion === latestVersion // only makes sense if response.ok\n\n\t\t\tlet message: string | null\n\t\t\tlet action: 'reinstall' | undefined\n\n\t\t\t// explicit\n\t\t\tif (explicit) {\n\t\t\t\tif (response.ok) {\n\t\t\t\t\tif (!isUpToDate) {\n\t\t\t\t\t\tmessage = 'A new version of Void is available! Please reinstall (auto-updates are disabled on this OS) - it only takes a second!'\n\t\t\t\t\t\taction = 'reinstall'\n\t\t\t\t\t}\n\t\t\t\t\telse {\n\t\t\t\t\t\tmessage = 'Void is up-to-date!'\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t\telse {\n\t\t\t\t\tmessage = `An error occurred when fetching the latest GitHub release tag. Please try again in ~5 minutes, or reinstall.`\n\t\t\t\t\taction = 'reinstall'\n\t\t\t\t}\n\t\t\t}\n\t\t\t// not explicit\n\t\t\telse {\n\t\t\t\tif (response.ok && !isUpToDate) {\n\t\t\t\t\tmessage = 'A new version of Void is available! Please reinstall (auto-updates are disabled on this OS) - it only takes a second!'\n\t\t\t\t\taction = 'reinstall'\n\t\t\t\t}\n\t\t\t\telse {\n\t\t\t\t\tmessage = null\n\t\t\t\t}\n\t\t\t}\n\t\t\treturn { message, action } as const\n\t\t}\n\t\tcatch (e) {\n\t\t\tif (explicit) {\n\t\t\t\treturn {\n\t\t\t\t\tmessage: `An error occurred when fetching the latest GitHub release tag: ${e}. Please try again in ~5 minutes.`,\n\t\t\t\t\taction: 'reinstall',\n\t\t\t\t}\n\t\t\t}\n\t\t\telse {\n\t\t\t\treturn { message: null } as const\n\t\t\t}\n\t\t}\n\t}\n}\n"
    };

    private ElectronMainVoidUpdateMainServiceTs() {
    }

    @Override
    public String path() {
        return "electron-main/voidUpdateMainService.ts";
    }

    @Override
    public String sha256() {
        return "029dee6386fdd0597413b35255d5a82f5cd63472ac4c73672c2b25d909fac842";
    }

    @Override
    public int originalByteLength() {
        return 5152;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
