package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserVoidContributionTs implements SourceAsset {
    public static final BrowserVoidContributionTs INSTANCE = new BrowserVoidContributionTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\n\n// register inline diffs\nimport './editCodeService.js'\n\n// register Sidebar pane, state, actions (keybinds, menus) (Ctrl+L)\nimport './sidebarActions.js'\nimport './sidebarPane.js'\n\n// register quick edit (Ctrl+K)\nimport './quickEditActions.js'\n\n\n// register Autocomplete\nimport './autocompleteService.js'\n\n// register Context services\n// import './contextGatheringService.js'\n// import './contextUserChangesService.js'\n\n// settings pane\nimport './voidSettingsPane.js'\n\n// register css\nimport './media/void.css'\n\n// update (frontend part, also see platform/)\nimport './voidUpdateActions.js'\n\nimport './convertToLLMMessageWorkbenchContrib.js'\n\n// tools\nimport './toolsService.js'\nimport './terminalToolService.js'\n\n// register Thread History\nimport './chatThreadService.js'\n\n// ping\nimport './metricsPollService.js'\n\n// helper services\nimport './helperServices/consistentItemService.js'\n\n// register selection helper\nimport './voidSelectionHelperWidget.js'\n\n// register tooltip service\nimport './tooltipService.js'\n\n// register onboarding service\nimport './voidOnboardingService.js'\n\n// register misc service\nimport './miscWokrbenchContrib.js'\n\n// register file service (for explorer context menu)\nimport './fileService.js'\n\n// register source control management\nimport './voidSCMService.js'\n\n// ---------- common (unclear if these actually need to be imported, because they're already imported wherever they're used) ----------\n\n// llmMessage\nimport '../common/sendLLMMessageService.js'\n\n// voidSettings\nimport '../common/voidSettingsService.js'\n\n// refreshModel\nimport '../common/refreshModelService.js'\n\n// metrics\nimport '../common/metricsService.js'\n\n// updates\nimport '../common/voidUpdateService.js'\n\n// model service\nimport '../common/voidModelService.js'\n"
    };

    private BrowserVoidContributionTs() {
    }

    @Override
    public String path() {
        return "browser/void.contribution.ts";
    }

    @Override
    public String sha256() {
        return "f03636cae7fc3f7d0ac1f1fb0555d1ea1d53e3b0d38428157c21f968bff71fbc";
    }

    @Override
    public int originalByteLength() {
        return 2094;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
