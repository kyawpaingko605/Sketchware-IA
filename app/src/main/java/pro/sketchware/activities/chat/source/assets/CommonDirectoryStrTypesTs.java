package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonDirectoryStrTypesTs implements SourceAsset {
    public static final CommonDirectoryStrTypesTs INSTANCE = new CommonDirectoryStrTypesTs();

    private static final String[] CHUNKS = new String[] {
            "import { URI } from '../../../../base/common/uri.js';\n\nexport type VoidDirectoryItem = {\n\turi: URI;\n\tname: string;\n\tisSymbolicLink: boolean;\n\tchildren: VoidDirectoryItem[] | null;\n\tisDirectory: boolean;\n\tisGitIgnoredDirectory: false | { numChildren: number }; // if directory is gitignored, we ignore children\n}\n"
    };

    private CommonDirectoryStrTypesTs() {
    }

    @Override
    public String path() {
        return "common/directoryStrTypes.ts";
    }

    @Override
    public String sha256() {
        return "0a734efbefe443af34bfee241169a7a3ef2a67a7323ea51eb9d308dfdd914f72";
    }

    @Override
    public int originalByteLength() {
        return 312;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
