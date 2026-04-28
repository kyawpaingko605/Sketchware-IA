package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonVoidSCMTypesTs implements SourceAsset {
    public static final CommonVoidSCMTypesTs INSTANCE = new CommonVoidSCMTypesTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { createDecorator } from '../../../../platform/instantiation/common/instantiation.js';\n\nexport interface IVoidSCMService {\n\treadonly _serviceBrand: undefined;\n\t/**\n\t * Get git diff --stat\n\t *\n\t * @param path Path to the git repository\n\t */\n\tgitStat(path: string): Promise<string>\n\t/**\n\t * Get git diff --stat for the top 10 most significantly changed files according to lines added/removed\n\t *\n\t * @param path Path to the git repository\n\t */\n\tgitSampledDiffs(path: string): Promise<string>\n\t/**\n\t * Get the current git branch\n\t *\n\t * @param path Path to the git repository\n\t */\n\tgitBranch(path: string): Promise<string>\n\t/**\n\t * Get the last 5 commits excluding merges\n\t *\n\t * @param path Path to the git repository\n\t */\n\tgitLog(path: string): Promise<string>\n}\n\nexport const IVoidSCMService = createDecorator<IVoidSCMService>('voidSCMService')\n"
    };

    private CommonVoidSCMTypesTs() {
    }

    @Override
    public String path() {
        return "common/voidSCMTypes.ts";
    }

    @Override
    public String sha256() {
        return "f64a7c57915510e3ffd50758e320a59000550a48412bacfea8cb09c7f9e4b8c3";
    }

    @Override
    public int originalByteLength() {
        return 1184;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
