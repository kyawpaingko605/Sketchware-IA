package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserActionIDsTs implements SourceAsset {
    public static final BrowserActionIDsTs INSTANCE = new BrowserActionIDsTs();

    private static final String[] CHUNKS = new String[] {
            "// Normally you'd want to put these exports in the files that register them, but if you do that you'll get an import order error if you import them in certain cases.\n// (importing them runs the whole file to get the ID, causing an import error). I guess it's best practice to separate out IDs, pretty annoying...\n\nexport const VOID_CTRL_L_ACTION_ID = 'void.ctrlLAction'\n\nexport const VOID_CTRL_K_ACTION_ID = 'void.ctrlKAction'\n\nexport const VOID_ACCEPT_DIFF_ACTION_ID = 'void.acceptDiff'\n\nexport const VOID_REJECT_DIFF_ACTION_ID = 'void.rejectDiff'\n\nexport const VOID_GOTO_NEXT_DIFF_ACTION_ID = 'void.goToNextDiff'\n\nexport const VOID_GOTO_PREV_DIFF_ACTION_ID = 'void.goToPrevDiff'\n\nexport const VOID_GOTO_NEXT_URI_ACTION_ID = 'void.goToNextUri'\n\nexport const VOID_GOTO_PREV_URI_ACTION_ID = 'void.goToPrevUri'\n\nexport const VOID_ACCEPT_FILE_ACTION_ID = 'void.acceptFile'\n\nexport const VOID_REJECT_FILE_ACTION_ID = 'void.rejectFile'\n\nexport const VOID_ACCEPT_ALL_DIFFS_ACTION_ID = 'void.acceptAllDiffs'\n\nexport const VOID_REJECT_ALL_DIFFS_ACTION_ID = 'void.rejectAllDiffs'\n"
    };

    private BrowserActionIDsTs() {
    }

    @Override
    public String path() {
        return "browser/actionIDs.ts";
    }

    @Override
    public String sha256() {
        return "6dc0afc4e5c36bc7fb5504c2d98e4ea0e5ff4ccbb4bbde9d8b577c45313ea348";
    }

    @Override
    public int originalByteLength() {
        return 1071;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
