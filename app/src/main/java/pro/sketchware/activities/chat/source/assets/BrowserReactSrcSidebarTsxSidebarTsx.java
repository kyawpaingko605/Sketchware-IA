package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcSidebarTsxSidebarTsx implements SourceAsset {
    public static final BrowserReactSrcSidebarTsxSidebarTsx INSTANCE = new BrowserReactSrcSidebarTsxSidebarTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { useIsDark } from '../util/services.js';\n// import { SidebarThreadSelector } from './SidebarThreadSelector.js';\n// import { SidebarChat } from './SidebarChat.js';\n\nimport '../styles.css'\nimport { SidebarChat } from './SidebarChat.js';\nimport ErrorBoundary from './ErrorBoundary.js';\n\nexport const Sidebar = ({ className }: { className: string }) => {\n\n\tconst isDark = useIsDark()\n\treturn <div\n\t\tclassName={`@@void-scope ${isDark ? 'dark' : ''}`}\n\t\tstyle={{ width: '100%', height: '100%' }}\n\t>\n\t\t<div\n\t\t\t// default background + text styles for sidebar\n\t\t\tclassName={`\n\t\t\t\tw-full h-full\n\t\t\t\tbg-void-bg-2\n\t\t\t\ttext-void-fg-1\n\t\t\t`}\n\t\t>\n\n\t\t\t<div className={`w-full h-full`}>\n\t\t\t\t<ErrorBoundary>\n\t\t\t\t\t<SidebarChat />\n\t\t\t\t</ErrorBoundary>\n\n\t\t\t</div>\n\t\t</div>\n\t</div>\n\n\n}\n\n"
    };

    private BrowserReactSrcSidebarTsxSidebarTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/sidebar-tsx/Sidebar.tsx";
    }

    @Override
    public String sha256() {
        return "ecb2077b61fb0e603cdef30119fdc3c5d7c276d9d2406611bd1fd8af64f27200";
    }

    @Override
    public int originalByteLength() {
        return 1104;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
