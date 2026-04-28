package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcQuickEditTsxIndexTsx implements SourceAsset {
    public static final BrowserReactSrcQuickEditTsxIndexTsx INSTANCE = new BrowserReactSrcQuickEditTsxIndexTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { mountFnGenerator } from '../util/mountFnGenerator.js'\nimport { QuickEdit } from './QuickEdit.js'\n\n\nexport const mountCtrlK = mountFnGenerator(QuickEdit)\n\n\n"
    };

    private BrowserReactSrcQuickEditTsxIndexTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/quick-edit-tsx/index.tsx";
    }

    @Override
    public String sha256() {
        return "94098baf9151c69bcb16fdf89afac7c4639f00eab8c811779aa0479bf0f703a1";
    }

    @Override
    public int originalByteLength() {
        return 496;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
