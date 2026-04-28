package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserTooltipServiceTs implements SourceAsset {
    public static final BrowserTooltipServiceTs INSTANCE = new BrowserTooltipServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Disposable, toDisposable } from '../../../../base/common/lifecycle.js';\nimport { IInstantiationService } from '../../../../platform/instantiation/common/instantiation.js';\nimport { IWorkbenchContribution, registerWorkbenchContribution2, WorkbenchPhase } from '../../../common/contributions.js';\nimport { ServicesAccessor } from '../../../../editor/browser/editorExtensions.js';\nimport { mountVoidTooltip } from './react/out/void-tooltip/index.js';\nimport { h, getActiveWindow } from '../../../../base/browser/dom.js';\n\n// Tooltip contribution that mounts the component at startup\nexport class TooltipContribution extends Disposable implements IWorkbenchContribution {\n\tstatic readonly ID = 'workbench.contrib.voidTooltip';\n\n\tconstructor(\n\t\t@IInstantiationService private readonly instantiationService: IInstantiationService,\n\t) {\n\t\tsuper();\n\t\tthis.initializeTooltip();\n\t}\n\n\tprivate initializeTooltip(): void {\n\t\t// Get the active window reference for multi-window support\n\t\tconst targetWindow = getActiveWindow();\n\n\t\t// Find the monaco-workbench element using the proper window reference\n\t\tconst workbench = targetWindow.document.querySelector('.monaco-workbench');\n\n\t\tif (workbench) {\n\t\t\t// Create a container element for the tooltip using h function\n\t\t\tconst tooltipContainer = h('div.void-tooltip-container').root;\n\t\t\tworkbench.appendChild(tooltipContainer);\n\n\t\t\t// Mount the React component\n\t\t\tthis.instantiationService.invokeFunction((accessor: ServicesAccessor) => {\n\t\t\t\tconst result = mountVoidTooltip(tooltipContainer, accessor);\n\t\t\t\tif (result && typeof result.dispose === 'function') {\n\t\t\t\t\tthis._register(toDisposable(result.dispose));\n\t\t\t\t}\n\t\t\t});\n\n\t\t\t// Register cleanup for the DOM element\n\t\t\tthis._register(toDisposable(() => {\n\t\t\t\tif (tooltipContainer.parentElement) {\n\t\t\t\t\ttooltipContainer.parentElement.removeChild(tooltipContainer);\n\t\t\t\t}\n\t\t\t}));\n\t\t}\n\t}\n}\n\n// Register the contribution to be initialized during the AfterRestored phase\nregisterWorkbenchContribution2(TooltipContribution.ID, TooltipContribution, WorkbenchPhase.AfterRestored);\n"
    };

    private BrowserTooltipServiceTs() {
    }

    @Override
    public String path() {
        return "browser/tooltipService.ts";
    }

    @Override
    public String sha256() {
        return "ccda39fa89428c15734228bb345c80246c63005edceb5aaa475752e2ce02717f";
    }

    @Override
    public int originalByteLength() {
        return 2402;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
