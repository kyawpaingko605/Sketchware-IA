package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserVoidOnboardingServiceTs implements SourceAsset {
    public static final BrowserVoidOnboardingServiceTs INSTANCE = new BrowserVoidOnboardingServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Disposable, toDisposable } from '../../../../base/common/lifecycle.js';\nimport { IInstantiationService } from '../../../../platform/instantiation/common/instantiation.js';\nimport { IWorkbenchContribution, registerWorkbenchContribution2, WorkbenchPhase } from '../../../common/contributions.js';\nimport { ServicesAccessor } from '../../../../editor/browser/editorExtensions.js';\nimport { mountVoidOnboarding } from './react/out/void-onboarding/index.js'\nimport { h, getActiveWindow } from '../../../../base/browser/dom.js';\n\n// Onboarding contribution that mounts the component at startup\nexport class OnboardingContribution extends Disposable implements IWorkbenchContribution {\n\tstatic readonly ID = 'workbench.contrib.voidOnboarding';\n\n\tconstructor(\n\t\t@IInstantiationService private readonly instantiationService: IInstantiationService,\n\t) {\n\t\tsuper();\n\t\tthis.initialize();\n\t}\n\n\tprivate initialize(): void {\n\t\t// Get the active window reference for multi-window support\n\t\tconst targetWindow = getActiveWindow();\n\n\t\t// Find the monaco-workbench element using the proper window reference\n\t\tconst workbench = targetWindow.document.querySelector('.monaco-workbench');\n\n\t\tif (workbench) {\n\n\t\t\tconst onboardingContainer = h('div.void-onboarding-container').root;\n\t\t\tworkbench.appendChild(onboardingContainer);\n\t\t\tthis.instantiationService.invokeFunction((accessor: ServicesAccessor) => {\n\t\t\t\tconst result = mountVoidOnboarding(onboardingContainer, accessor);\n\t\t\t\tif (result && typeof result.dispose === 'function') {\n\t\t\t\t\tthis._register(toDisposable(result.dispose));\n\t\t\t\t}\n\t\t\t});\n\t\t\t// Register cleanup for the DOM element\n\t\t\tthis._register(toDisposable(() => {\n\t\t\t\tif (onboardingContainer.parentElement) {\n\t\t\t\t\tonboardingContainer.parentElement.removeChild(onboardingContainer);\n\t\t\t\t}\n\t\t\t}));\n\t\t}\n\t}\n}\n\n// Register the contribution to be initialized during the AfterRestored phase\nregisterWorkbenchContribution2(OnboardingContribution.ID, OnboardingContribution, WorkbenchPhase.AfterRestored);\n"
    };

    private BrowserVoidOnboardingServiceTs() {
    }

    @Override
    public String path() {
        return "browser/voidOnboardingService.ts";
    }

    @Override
    public String sha256() {
        return "791d24152267d8e52701e92ee8f38addc0f42026cc65619c0d78528868ba0f45";
    }

    @Override
    public int originalByteLength() {
        return 2333;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
