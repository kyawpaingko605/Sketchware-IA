package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcDiffIndexTsx implements SourceAsset {
    public static final BrowserReactSrcDiffIndexTsx INSTANCE = new BrowserReactSrcDiffIndexTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { diffLines, Change } from 'diff';\n\nexport { diffLines, Change }\n"
    };

    private BrowserReactSrcDiffIndexTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/diff/index.tsx";
    }

    @Override
    public String sha256() {
        return "42950eb1959de67200154b5218855c1acb2be4adc10dac9d2c81755a2cc52b8b";
    }

    @Override
    public int originalByteLength() {
        return 404;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
