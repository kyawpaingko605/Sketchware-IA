package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcVoidTooltipIndexTsx implements SourceAsset {
    public static final BrowserReactSrcVoidTooltipIndexTsx INSTANCE = new BrowserReactSrcVoidTooltipIndexTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { mountFnGenerator } from '../util/mountFnGenerator.js'\nimport { VoidTooltip } from './VoidTooltip.js'\n\nexport const mountVoidTooltip = mountFnGenerator(VoidTooltip)\n"
    };

    private BrowserReactSrcVoidTooltipIndexTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/void-tooltip/index.tsx";
    }

    @Override
    public String sha256() {
        return "4180a3adb1d3670d383f67772b6cf1cc2606c6647cc033f847fcdc9ea13ebfca";
    }

    @Override
    public int originalByteLength() {
        return 505;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
