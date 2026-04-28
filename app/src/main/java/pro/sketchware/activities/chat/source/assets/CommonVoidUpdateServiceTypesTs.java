package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonVoidUpdateServiceTypesTs implements SourceAsset {
    public static final CommonVoidUpdateServiceTypesTs INSTANCE = new CommonVoidUpdateServiceTypesTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nexport type VoidCheckUpdateRespose = {\n\tmessage: string,\n\taction?: 'reinstall' | 'restart' | 'download' | 'apply'\n} | {\n\tmessage: null,\n\tactions?: undefined,\n} | null\n\n\n"
    };

    private CommonVoidUpdateServiceTypesTs() {
    }

    @Override
    public String path() {
        return "common/voidUpdateServiceTypes.ts";
    }

    @Override
    public String sha256() {
        return "08a662d065804f622404a7c824d2d902357c93b5806fbdeee743847c1db913bc";
    }

    @Override
    public int originalByteLength() {
        return 501;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
