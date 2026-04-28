package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserExtensionTransferTypesTs implements SourceAsset {
    public static final BrowserExtensionTransferTypesTs INSTANCE = new BrowserExtensionTransferTypesTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { URI } from '../../../../base/common/uri.js'\n\nexport type TransferEditorType = 'VS Code' | 'Cursor' | 'Windsurf'\n// https://github.com/VSCodium/vscodium/blob/master/docs/index.md#migrating-from-visual-studio-code-to-vscodium\n// https://code.visualstudio.com/docs/editor/extension-marketplace#_where-are-extensions-installed\nexport type TransferFilesInfo = { from: URI, to: URI, isExtensions?: boolean }[]\n"
    };

    private BrowserExtensionTransferTypesTs() {
    }

    @Override
    public String path() {
        return "browser/extensionTransferTypes.ts";
    }

    @Override
    public String sha256() {
        return "cbb738bf806a8ab84517f6eca8a7bca371d436ee900a25e11872b205aa9781bf";
    }

    @Override
    public int originalByteLength() {
        return 745;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
