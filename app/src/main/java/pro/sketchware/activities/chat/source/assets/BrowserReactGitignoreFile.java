package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactGitignoreFile implements SourceAsset {
    public static final BrowserReactGitignoreFile INSTANCE = new BrowserReactGitignoreFile();

    private static final String[] CHUNKS = new String[] {
            "out/\nsrc2/\n"
    };

    private BrowserReactGitignoreFile() {
    }

    @Override
    public String path() {
        return "browser/react/.gitignore";
    }

    @Override
    public String sha256() {
        return "f71637a84d06f9e9d45208a1126484c5697d250e2c468130b5c6635ee184272c";
    }

    @Override
    public int originalByteLength() {
        return 11;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
