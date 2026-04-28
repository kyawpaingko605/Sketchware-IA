package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactREADMEMd implements SourceAsset {
    public static final BrowserReactREADMEMd INSTANCE = new BrowserReactREADMEMd();

    private static final String[] CHUNKS = new String[] {
            "\nRun `node build.js` to compile the React into `out/`.\n\nA couple things to remember:\n\n- Make sure to add .js at the end of any external imports used in here, e.g. ../../../../../my_file.js. If you don't do this, you will get untraceable errors.\n\n- src/ needs to be shallow (1 folder deep) so the detection of externals works properly (see tsup.config.js).\n\n\n"
    };

    private BrowserReactREADMEMd() {
    }

    @Override
    public String path() {
        return "browser/react/README.md";
    }

    @Override
    public String sha256() {
        return "27354042844c24c346c6a45e6b16f4480b0d59e730d21fdb88bba98f8bfb45a7";
    }

    @Override
    public int originalByteLength() {
        return 358;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
