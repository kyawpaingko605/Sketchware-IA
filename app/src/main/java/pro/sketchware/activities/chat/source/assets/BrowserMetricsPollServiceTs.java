package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserMetricsPollServiceTs implements SourceAsset {
    public static final BrowserMetricsPollServiceTs INSTANCE = new BrowserMetricsPollServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Disposable } from '../../../../base/common/lifecycle.js';\nimport { createDecorator } from '../../../../platform/instantiation/common/instantiation.js';\nimport { registerWorkbenchContribution2, WorkbenchPhase } from '../../../common/contributions.js';\n\nimport * as dom from '../../../../base/browser/dom.js';\nimport { IMetricsService } from '../common/metricsService.js';\n\n\n\nexport interface IMetricsPollService {\n\treadonly _serviceBrand: undefined;\n}\n\n\nconst PING_EVERY_MS = 15 * 1000 * 60  // 15 minutes\n\nexport const IMetricsPollService = createDecorator<IMetricsPollService>('voidMetricsPollService');\nclass MetricsPollService extends Disposable implements IMetricsPollService {\n\t_serviceBrand: undefined;\n\n\tstatic readonly ID = 'voidMetricsPollService';\n\n\n\tprivate readonly intervalID: number\n\tconstructor(\n\t\t@IMetricsService private readonly metricsService: IMetricsService,\n\t) {\n\t\tsuper()\n\n\t\t// initial state\n\t\tconst { window } = dom.getActiveWindow()\n\t\tlet i = 1\n\n\t\tthis.intervalID = window.setInterval(() => {\n\t\t\tthis.metricsService.capture('Alive', { iv1: i })\n\t\t\ti += 1\n\t\t}, PING_EVERY_MS)\n\n\n\t}\n\n\toverride dispose() {\n\t\tsuper.dispose()\n\t\tconst { window } = dom.getActiveWindow()\n\t\twindow.clearInterval(this.intervalID)\n\t}\n\n\n}\n\nregisterWorkbenchContribution2(MetricsPollService.ID, MetricsPollService, WorkbenchPhase.BlockRestore);\n"
    };

    private BrowserMetricsPollServiceTs() {
    }

    @Override
    public String path() {
        return "browser/metricsPollService.ts";
    }

    @Override
    public String sha256() {
        return "da0d1b6202c80eb465f03697a8c4b1050b0b7adddb7adbb7ee3bf2f9b6bc2ae9";
    }

    @Override
    public int originalByteLength() {
        return 1682;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
