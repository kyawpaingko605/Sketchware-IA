package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcVoidEditorWidgetsTsxIndexTsx implements SourceAsset {
    public static final BrowserReactSrcVoidEditorWidgetsTsxIndexTsx INSTANCE = new BrowserReactSrcVoidEditorWidgetsTsxIndexTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { mountFnGenerator } from '../util/mountFnGenerator.js'\nimport { VoidCommandBarMain } from './VoidCommandBar.js'\nimport { VoidSelectionHelperMain } from './VoidSelectionHelper.js'\n\nexport const mountVoidCommandBar = mountFnGenerator(VoidCommandBarMain)\n\nexport const mountVoidSelectionHelper = mountFnGenerator(VoidSelectionHelperMain)\n\n"
    };

    private BrowserReactSrcVoidEditorWidgetsTsxIndexTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/void-editor-widgets-tsx/index.tsx";
    }

    @Override
    public String sha256() {
        return "1d0309516357d42bb0bbfab0106b7f4979aadbac83fa118c31fcf0c3169dbd1f";
    }

    @Override
    public int originalByteLength() {
        return 676;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
