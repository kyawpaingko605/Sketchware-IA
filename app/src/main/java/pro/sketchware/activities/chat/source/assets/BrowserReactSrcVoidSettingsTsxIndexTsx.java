package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcVoidSettingsTsxIndexTsx implements SourceAsset {
    public static final BrowserReactSrcVoidSettingsTsxIndexTsx INSTANCE = new BrowserReactSrcVoidSettingsTsxIndexTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { mountFnGenerator } from '../util/mountFnGenerator.js'\nimport { Settings } from './Settings.js'\n\nexport const mountVoidSettings = mountFnGenerator(Settings)\n\n\n"
    };

    private BrowserReactSrcVoidSettingsTsxIndexTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/void-settings-tsx/index.tsx";
    }

    @Override
    public String sha256() {
        return "4c991f57efa1b6622f30be97a95a8eec4f6741031efa851144d4ed5be05ff853";
    }

    @Override
    public int originalByteLength() {
        return 499;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
