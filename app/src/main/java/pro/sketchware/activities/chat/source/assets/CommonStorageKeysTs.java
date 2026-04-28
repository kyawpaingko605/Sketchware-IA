package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonStorageKeysTs implements SourceAsset {
    public static final CommonStorageKeysTs INSTANCE = new CommonStorageKeysTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\n// past values:\n// 'void.settingsServiceStorage'\n// 'void.settingsServiceStorageI' // 1.0.2\n\n// 1.0.3\nexport const VOID_SETTINGS_STORAGE_KEY = 'void.settingsServiceStorageII'\n\n\n// past values:\n// 'void.chatThreadStorage'\n// 'void.chatThreadStorageI' // 1.0.2\n\n// 1.0.3\nexport const THREAD_STORAGE_KEY = 'void.chatThreadStorageII'\n\n\n\nexport const OPT_OUT_KEY = 'void.app.optOutAll'\n"
    };

    private CommonStorageKeysTs() {
    }

    @Override
    public String path() {
        return "common/storageKeys.ts";
    }

    @Override
    public String sha256() {
        return "a6d402b9bdb76ee278b201f0735cbcd2979c2d4ac26d5bc3c984f3dec109f170";
    }

    @Override
    public int originalByteLength() {
        return 713;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
