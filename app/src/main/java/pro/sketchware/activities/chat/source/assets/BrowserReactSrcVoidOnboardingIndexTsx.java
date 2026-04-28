package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcVoidOnboardingIndexTsx implements SourceAsset {
    public static final BrowserReactSrcVoidOnboardingIndexTsx INSTANCE = new BrowserReactSrcVoidOnboardingIndexTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { mountFnGenerator } from '../util/mountFnGenerator.js'\nimport { VoidOnboarding } from './VoidOnboarding.js'\n\nexport const mountVoidOnboarding = mountFnGenerator(VoidOnboarding)\n"
    };

    private BrowserReactSrcVoidOnboardingIndexTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/void-onboarding/index.tsx";
    }

    @Override
    public String sha256() {
        return "0423fce46af232ebf89a520b15490a8ff9b19b11469ff91cef647f556c70856e";
    }

    @Override
    public int originalByteLength() {
        return 517;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
