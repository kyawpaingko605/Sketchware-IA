package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonHelpersColorsTs implements SourceAsset {
    public static final CommonHelpersColorsTs INSTANCE = new CommonHelpersColorsTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Color, RGBA } from '../../../../../base/common/color.js';\nimport { registerColor } from '../../../../../platform/theme/common/colorUtils.js';\n\n// editCodeService colors\nconst sweepBG = new Color(new RGBA(100, 100, 100, .2));\nconst highlightBG = new Color(new RGBA(100, 100, 100, .1));\nconst sweepIdxBG = new Color(new RGBA(100, 100, 100, .5));\n\nconst acceptBG = new Color(new RGBA(155, 185, 85, .1)); // default is RGBA(155, 185, 85, .2)\nconst rejectBG = new Color(new RGBA(255, 0, 0, .1)); // default is RGBA(255, 0, 0, .2)\n\n// Widget colors\nexport const acceptAllBg = 'rgb(30, 133, 56)'\nexport const acceptBg = 'rgb(26, 116, 48)'\nexport const acceptBorder = '1px solid rgb(20, 86, 38)'\n\nexport const rejectAllBg = 'rgb(207, 40, 56)'\nexport const rejectBg = 'rgb(180, 35, 49)'\nexport const rejectBorder = '1px solid rgb(142, 28, 39)'\n\nexport const buttonFontSize = '11px'\nexport const buttonTextColor = 'white'\n\n\n\nconst configOfBG = (color: Color) => {\n\treturn { dark: color, light: color, hcDark: color, hcLight: color, }\n}\n\n// gets converted to --vscode-void-greenBG, see void.css, asCssVariable\nregisterColor('void.greenBG', configOfBG(acceptBG), '', true);\nregisterColor('void.redBG', configOfBG(rejectBG), '', true);\nregisterColor('void.sweepBG', configOfBG(sweepBG), '', true);\nregisterColor('void.highlightBG', configOfBG(highlightBG), '', true);\nregisterColor('void.sweepIdxBG', configOfBG(sweepIdxBG), '', true);\n"
    };

    private CommonHelpersColorsTs() {
    }

    @Override
    public String path() {
        return "common/helpers/colors.ts";
    }

    @Override
    public String sha256() {
        return "8ea296a733a96274fca5cc3882c39554728d9bf9bc05418824849f205fa1fa18";
    }

    @Override
    public int originalByteLength() {
        return 1764;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
