package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class ElectronMainMetricsMainServiceTs implements SourceAsset {
    public static final ElectronMainMetricsMainServiceTs INSTANCE = new ElectronMainMetricsMainServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Disposable } from '../../../../base/common/lifecycle.js';\nimport { isLinux, isMacintosh, isWindows } from '../../../../base/common/platform.js';\nimport { generateUuid } from '../../../../base/common/uuid.js';\nimport { IEnvironmentMainService } from '../../../../platform/environment/electron-main/environmentMainService.js';\nimport { IProductService } from '../../../../platform/product/common/productService.js';\nimport { StorageTarget, StorageScope } from '../../../../platform/storage/common/storage.js';\nimport { IApplicationStorageMainService } from '../../../../platform/storage/electron-main/storageMainService.js';\n\nimport { IMetricsService } from '../common/metricsService.js';\nimport { PostHog } from 'posthog-node'\nimport { OPT_OUT_KEY } from '../common/storageKeys.js';\n\n\nconst os = isWindows ? 'windows' : isMacintosh ? 'mac' : isLinux ? 'linux' : null\nconst _getOSInfo = () => {\n\ttry {\n\t\tconst { platform, arch } = process // see platform.ts\n\t\treturn { platform, arch }\n\t}\n\tcatch (e) {\n\t\treturn { osInfo: { platform: '??', arch: '??' } }\n\t}\n}\nconst osInfo = _getOSInfo()\n\n// we'd like to use devDeviceId on telemetryService, but that gets sanitized by the time it gets here as 'someValue.devDeviceId'\n\n\n\nexport class MetricsMainService extends Disposable implements IMetricsService {\n\t_serviceBrand: undefined;\n\n\tprivate readonly client: PostHog\n\n\tprivate _initProperties: object = {}\n\n\n\t// helper - looks like this is stored in a .vscdb file in ~/Library/Application Support/Void\n\tprivate _memoStorage(key: string, target: StorageTarget, setValIfNotExist?: string) {\n\t\tconst currVal = this._appStorage.get(key, StorageScope.APPLICATION)\n\t\tif (currVal !== undefined) return currVal\n\t\tconst newVal = setValIfNotExist ?? generateUuid()\n\t\tthis._appStorage.store(key, newVal, StorageScope.APPLICATION, target)\n\t\treturn newVal\n\t}\n\n\n\t// this is old, eventually we can just delete this since all the keys will have been transferred over\n\t// returns 'NULL' or the old key\n\tprivate get oldId() {\n\t\t// check new storage key first\n\t\tconst newKey = 'void.app.oldMachineId'\n\t\tconst newOldId = this._appStorage.get(newKey, StorageScope.APPLICATION)\n\t\tif (newOldId) return newOldId\n\n\t\t// put old key into new key if didn't already\n\t\tconst oldValue = this._appStorage.get('void.machineId', StorageScope.APPLICATION) ?? 'NULL' // the old way of getting the key\n\t\tthis._appStorage.store(newKey, oldValue, StorageScope.APPLICATION, StorageTarget.MACHINE)\n\t\treturn oldValue\n\n\t\t// in a few weeks we can replace above with this\n\t\t// private get oldId() {\n\t\t// \treturn this._memoStorage('void.app.oldMachineId', StorageTarget.MACHINE, 'NULL')\n\t\t// }\n\t}\n\n\n\t// the main id\n\tprivate get distinctId() {\n\t\tconst oldId = this.oldId\n\t\tconst setValIfNotExist = oldId === 'NULL' ? undefined : oldId\n\t\treturn this._memoStorage('void.app.machineId', StorageTarget.MACHINE, setValIfNotExist)\n\t}\n\n\t// just to see if there are ever multiple machineIDs per userID (instead of this, we should just track by the user's email)\n\tprivate get userId() {\n\t\treturn this._memoStorage('void.app.userMachineId', StorageTarget.USER)\n\t}\n\n\tconstructor(\n\t\t@IProductService private readonly _productService: IProductService,\n\t\t@IEnvironmentMainService private readonly _envMainService: IEnvironmentMainService,\n\t\t@IApplicationStorageMainService private readonly _appStorage: IApplicationStorageMainService,\n\t) {\n\t\tsuper()\n\t\tthis.client = new PostHog('phc_UanIdujHiLp55BkUTjB1AuBXcasVkdqRwgnwRlWESH2', {\n\t\t\thost: 'https://us.i.posthog.com',\n\t\t})\n\n\t\tthis.initialize() // async\n\t}\n\n\tasync initialize() {\n\t\t// very important to await whenReady!\n\t\tawait this._appStorage.whenReady\n\n\t\tconst { commit, version, voidVersion, release, quality } = this._productService\n\n\t\tconst isDevMode = !this._envMainService.isBuilt // found in abstractUpdateService.ts\n\n\t\t// custom properties we identify\n\t\tthis._initProperties = {\n\t\t\tcommit,\n\t\t\tvscodeVersion: version,\n\t\t\tvoidVersion: voidVersion,\n\t\t\trelease,\n\t\t\tos,\n\t\t\tquality,\n\t\t\tdistinctId: this.distinctId,\n\t\t\tdistinctIdUser: this.userId,\n\t\t\toldId: this.oldId,\n\t\t\tisDevMode,\n\t\t\t...osInfo,\n\t\t}\n\n\t\tconst identifyMessage = {\n\t\t\tdistinctId: this.distinctId,\n\t\t\tproperties: this._initProperties,\n\t\t}\n\n\t\tconst didOptOut = this._appStorage.getBoolean(OPT_OUT_KEY, StorageScope.APPLICATION, false)\n\n\t\tconsole.log('User is opted out of basic Void metrics?', didOptOut)\n\t\tif (didOptOut) {\n\t\t\tthis.client.optOut()\n\t\t}\n\t\telse {\n\t\t\tthis.client.optIn()\n\t\t\tthis.client.identify(identifyMessage)\n\t\t}\n\n\n\t\tconsole.log('Void posthog metrics info:', JSON.stringify(identifyMessage, null, 2))\n\t}\n\n\n\tcapture: IMetricsService['capture'] = (event, params) => {\n\t\tconst capture = { distinctId: this.distinctId, event, properties: params } as const\n\t\t// console.log('full capture:', this.distinctId)\n\t\tthis.client.capture(capture)\n\t}\n\n\tsetOptOut: IMetricsService['setOptOut'] = (newVal: boolean) => {\n\t\tif (newVal) {\n\t\t\tthis._appStorage.store(OPT_OUT_KEY, 'true', StorageScope.APPLICATION, StorageTarget.MACHINE)\n\t\t}\n\t\telse {\n\t\t\tthis._appStorage.remove(OPT_OUT_KEY, StorageScope.APPLICATION)\n\t\t}\n\t}\n\n\tasync getDebuggingProperties() {\n\t\treturn this._initProperties\n\t}\n}\n\n\n"
    };

    private ElectronMainMetricsMainServiceTs() {
    }

    @Override
    public String path() {
        return "electron-main/metricsMainService.ts";
    }

    @Override
    public String sha256() {
        return "3d3bc67098ad6024a95115c04c1c7845d3425c626776dbeb46bdf4a1a52eed85";
    }

    @Override
    public int originalByteLength() {
        return 5479;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
