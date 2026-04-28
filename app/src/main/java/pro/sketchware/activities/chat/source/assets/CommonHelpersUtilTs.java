package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonHelpersUtilTs implements SourceAsset {
    public static final CommonHelpersUtilTs INSTANCE = new CommonHelpersUtilTs();

    private static final String[] CHUNKS = new String[] {
            "\nexport const separateOutFirstLine = (content: string): [string, string] | [string, undefined] => {\n\tconst newLineIdx = content.indexOf('\\r\\n')\n\tif (newLineIdx !== -1) {\n\t\tconst A = content.substring(0, newLineIdx)\n\t\tconst B = content.substring(newLineIdx + 2, Infinity);\n\t\treturn [A, B]\n\t}\n\n\tconst newLineIdx2 = content.indexOf('\\n')\n\tif (newLineIdx2 !== -1) {\n\t\tconst A = content.substring(0, newLineIdx2)\n\t\tconst B = content.substring(newLineIdx2 + 1, Infinity);\n\t\treturn [A, B]\n\t}\n\n\treturn [content, undefined]\n}\n"
    };

    private CommonHelpersUtilTs() {
    }

    @Override
    public String path() {
        return "common/helpers/util.ts";
    }

    @Override
    public String sha256() {
        return "ffb60a5d248622f35f8cd49bfe8653890b2778f9af689abca2bb6158e6e0ad64";
    }

    @Override
    public int originalByteLength() {
        return 517;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
