package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcSidebarTsxIndexTsx implements SourceAsset {
    public static final BrowserReactSrcSidebarTsxIndexTsx INSTANCE = new BrowserReactSrcSidebarTsxIndexTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { mountFnGenerator } from '../util/mountFnGenerator.js'\nimport { Sidebar } from './Sidebar.js'\n\nexport const mountSidebar = mountFnGenerator(Sidebar)\n\n\n"
    };

    private BrowserReactSrcSidebarTsxIndexTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/sidebar-tsx/index.tsx";
    }

    @Override
    public String sha256() {
        return "e5f6af23e56c5957407e81a277b19f01fce0355e3dcbd628908fd240a099667e";
    }

    @Override
    public int originalByteLength() {
        return 491;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
