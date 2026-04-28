package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcUtilMountFnGeneratorTsx implements SourceAsset {
    public static final BrowserReactSrcUtilMountFnGeneratorTsx INSTANCE = new BrowserReactSrcUtilMountFnGeneratorTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport React, { useEffect, useState } from 'react';\nimport * as ReactDOM from 'react-dom/client'\nimport { _registerServices } from './services.js';\n\n\nimport { ServicesAccessor } from '../../../../../../../editor/browser/editorExtensions.js';\n\nexport const mountFnGenerator = (Component: (params: any) => React.ReactNode) => (rootElement: HTMLElement, accessor: ServicesAccessor, props?: any) => {\n\tif (typeof document === 'undefined') {\n\t\tconsole.error('index.tsx error: document was undefined')\n\t\treturn\n\t}\n\n\tconst disposables = _registerServices(accessor)\n\n\tconst root = ReactDOM.createRoot(rootElement)\n\n\tconst rerender = (props?: any) => {\n\t\troot.render(<Component {...props} />); // tailwind dark theme indicator\n\t}\n\tconst dispose = () => {\n\t\troot.unmount();\n\t\tdisposables.forEach(d => d.dispose());\n\t}\n\n\trerender(props)\n\n\tconst returnVal = {\n\t\trerender,\n\t\tdispose,\n\t}\n\treturn returnVal\n}\n"
    };

    private BrowserReactSrcUtilMountFnGeneratorTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/util/mountFnGenerator.tsx";
    }

    @Override
    public String sha256() {
        return "6258b45584698add0b9c9db9d545d13662e02d359258e4b607f388f42dc71a84";
    }

    @Override
    public int originalByteLength() {
        return 1226;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
