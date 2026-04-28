package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactTsconfigJson implements SourceAsset {
    public static final BrowserReactTsconfigJson INSTANCE = new BrowserReactTsconfigJson();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n{\n\t\"compilerOptions\": {\n\t\t\"strict\": true,\n\t\t\"exactOptionalPropertyTypes\": false,\n\n\t\t\"jsx\": \"react-jsx\",\n\t\t\"moduleResolution\": \"NodeNext\",\n\t\t\"module\": \"NodeNext\",\n\t\t\"esModuleInterop\": true,\n\t},\n\t\"include\": [\n\t\t// this is just for type checking, so src/ is the correct dir\n\t\t\"./src/**/*.ts\",\n\t\t\"./src/**/*.tsx\"\n\t],\n\t\"plugins\": [\n      {\n        \"name\": \"next\"\n      }\n    ]\n}\n"
    };

    private BrowserReactTsconfigJson() {
    }

    @Override
    public String path() {
        return "browser/react/tsconfig.json";
    }

    @Override
    public String sha256() {
        return "9761d7e71a47f9b42f7e08b702fc56c243c71155087ebb69a5ebf9fa7ad61c8d";
    }

    @Override
    public int originalByteLength() {
        return 705;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
