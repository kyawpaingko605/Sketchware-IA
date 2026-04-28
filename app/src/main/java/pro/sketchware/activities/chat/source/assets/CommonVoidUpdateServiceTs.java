package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonVoidUpdateServiceTs implements SourceAsset {
    public static final CommonVoidUpdateServiceTs INSTANCE = new CommonVoidUpdateServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { ProxyChannel } from '../../../../base/parts/ipc/common/ipc.js';\nimport { registerSingleton, InstantiationType } from '../../../../platform/instantiation/common/extensions.js';\nimport { createDecorator } from '../../../../platform/instantiation/common/instantiation.js';\nimport { IMainProcessService } from '../../../../platform/ipc/common/mainProcessService.js';\nimport { VoidCheckUpdateRespose } from './voidUpdateServiceTypes.js';\n\n\n\nexport interface IVoidUpdateService {\n\treadonly _serviceBrand: undefined;\n\tcheck: (explicit: boolean) => Promise<VoidCheckUpdateRespose>;\n}\n\n\nexport const IVoidUpdateService = createDecorator<IVoidUpdateService>('VoidUpdateService');\n\n\n// implemented by calling channel\nexport class VoidUpdateService implements IVoidUpdateService {\n\n\treadonly _serviceBrand: undefined;\n\tprivate readonly voidUpdateService: IVoidUpdateService;\n\n\tconstructor(\n\t\t@IMainProcessService mainProcessService: IMainProcessService, // (only usable on client side)\n\t) {\n\t\t// creates an IPC proxy to use metricsMainService.ts\n\t\tthis.voidUpdateService = ProxyChannel.toService<IVoidUpdateService>(mainProcessService.getChannel('void-channel-update'));\n\t}\n\n\n\t// anything transmitted over a channel must be async even if it looks like it doesn't have to be\n\tcheck: IVoidUpdateService['check'] = async (explicit) => {\n\t\tconst res = await this.voidUpdateService.check(explicit)\n\t\treturn res\n\t}\n}\n\nregisterSingleton(IVoidUpdateService, VoidUpdateService, InstantiationType.Eager);\n\n\n"
    };

    private CommonVoidUpdateServiceTs() {
    }

    @Override
    public String path() {
        return "common/voidUpdateService.ts";
    }

    @Override
    public String sha256() {
        return "d88e314fb1ec3e7a55d082b873e7527cd960c49f88d1779b05cb8f1de1a84e71";
    }

    @Override
    public int originalByteLength() {
        return 1826;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
