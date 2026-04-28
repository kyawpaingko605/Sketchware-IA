package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonHelpersSystemInfoTs implements SourceAsset {
    public static final CommonHelpersSystemInfoTs INSTANCE = new CommonHelpersSystemInfoTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { isLinux, isMacintosh, isWindows } from '../../../../../base/common/platform.js';\n\n// import { OS, OperatingSystem } from '../../../../../base/common/platform.js';\n// alternatively could use ^ and OS === OperatingSystem.Windows ? ...\n\n\n\nexport const os = isWindows ? 'windows' : isMacintosh ? 'mac' : isLinux ? 'linux' : null\n\n"
    };

    private CommonHelpersSystemInfoTs() {
    }

    @Override
    public String path() {
        return "common/helpers/systemInfo.ts";
    }

    @Override
    public String sha256() {
        return "94ce4a25a5e915bc280a927ac0df3ba65c529dab7be02d37b26760515f1c2a2f";
    }

    @Override
    public int originalByteLength() {
        return 667;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
