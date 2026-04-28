package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonMetricsServiceTs implements SourceAsset {
    public static final CommonMetricsServiceTs INSTANCE = new CommonMetricsServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { createDecorator, ServicesAccessor } from '../../../../platform/instantiation/common/instantiation.js';\nimport { ProxyChannel } from '../../../../base/parts/ipc/common/ipc.js';\nimport { registerSingleton, InstantiationType } from '../../../../platform/instantiation/common/extensions.js';\nimport { IMainProcessService } from '../../../../platform/ipc/common/mainProcessService.js';\nimport { localize2 } from '../../../../nls.js';\nimport { registerAction2, Action2 } from '../../../../platform/actions/common/actions.js';\nimport { INotificationService } from '../../../../platform/notification/common/notification.js';\n\nexport interface IMetricsService {\n\treadonly _serviceBrand: undefined;\n\tcapture(event: string, params: Record<string, any>): void;\n\tsetOptOut(val: boolean): void;\n\tgetDebuggingProperties(): Promise<object>;\n}\n\nexport const IMetricsService = createDecorator<IMetricsService>('metricsService');\n\n\n// implemented by calling channel\nexport class MetricsService implements IMetricsService {\n\n\treadonly _serviceBrand: undefined;\n\tprivate readonly metricsService: IMetricsService;\n\n\tconstructor(\n\t\t@IMainProcessService mainProcessService: IMainProcessService // (only usable on client side)\n\t) {\n\t\t// creates an IPC proxy to use metricsMainService.ts\n\t\tthis.metricsService = ProxyChannel.toService<IMetricsService>(mainProcessService.getChannel('void-channel-metrics'));\n\t}\n\n\t// call capture on the channel\n\tcapture(...params: Parameters<IMetricsService['capture']>) {\n\t\tthis.metricsService.capture(...params);\n\t}\n\n\tsetOptOut(...params: Parameters<IMetricsService['setOptOut']>) {\n\t\tthis.metricsService.setOptOut(...params);\n\t}\n\n\n\t// anything transmitted over a channel must be async even if it looks like it doesn't have to be\n\tasync getDebuggingProperties(): Promise<object> {\n\t\treturn this.metricsService.getDebuggingProperties()\n\t}\n}\n\nregisterSingleton(IMetricsService, MetricsService, InstantiationType.Eager);\n\n\n// debugging action\nregisterAction2(class extends Action2 {\n\tconstructor() {\n\t\tsuper({\n\t\t\tid: 'voidDebugInfo',\n\t\t\tf1: true,\n\t\t\ttitle: localize2('voidMetricsDebug', 'Void: Log Debug Info'),\n\t\t});\n\t}\n\tasync run(accessor: ServicesAccessor): Promise<void> {\n\t\tconst metricsService = accessor.get(IMetricsService)\n\t\tconst notifService = accessor.get(INotificationService)\n\n\t\tconst debugProperties = await metricsService.getDebuggingProperties()\n\t\tconsole.log('Metrics:', debugProperties)\n\t\tnotifService.info(`Void Debug info:\\n${JSON.stringify(debugProperties, null, 2)}`)\n\t}\n})\n"
    };

    private CommonMetricsServiceTs() {
    }

    @Override
    public String path() {
        return "common/metricsService.ts";
    }

    @Override
    public String sha256() {
        return "66580c8bbc5f5c992f837b812fa3df0acf66cf63c29e7f650a799105749621cf";
    }

    @Override
    public int originalByteLength() {
        return 2844;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
