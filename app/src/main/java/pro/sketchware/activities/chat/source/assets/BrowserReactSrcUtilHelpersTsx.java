package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcUtilHelpersTsx implements SourceAsset {
    public static final BrowserReactSrcUtilHelpersTsx INSTANCE = new BrowserReactSrcUtilHelpersTsx();

    private static final String[] CHUNKS = new String[] {
            "import { useCallback, useEffect, useRef, useState } from 'react'\n\n\n\ntype ReturnType<T> = [\n\t{ readonly current: T },\n\t(t: T) => void\n]\n\n// use this if state might be too slow to catch\nexport const useRefState = <T,>(initVal: T): ReturnType<T> => {\n\t// this actually makes a difference being an int, not a boolean.\n\t// if it's a boolean and changes happen to fast, it goes with old values and leads to *very* weird bugs (like returning JSX, but not actually rendering it)\n\tconst [_s, _setState] = useState(0)\n\n\tconst ref = useRef<T>(initVal)\n\tconst setState = useCallback((newVal: T) => {\n\t\t_setState(n => n + 1) // call rerender\n\t\tref.current = newVal\n\t}, [])\n\treturn [ref, setState]\n}\n\n\nexport const usePromise = <T,>(promise: Promise<T>): T | undefined => {\n\tconst [val, setVal] = useState<T | undefined>(undefined)\n\tuseEffect(() => {\n\t\tpromise.then((v) => setVal(v))\n\t}, [promise])\n\treturn val\n}\n"
    };

    private BrowserReactSrcUtilHelpersTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/util/helpers.tsx";
    }

    @Override
    public String sha256() {
        return "ebe1e7421780142a67c5df2782c6f288ed743c0b8e3abe2b714ea10db6723086";
    }

    @Override
    public int originalByteLength() {
        return 899;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
