package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcQuickEditTsxQuickEditTsx implements SourceAsset {
    public static final BrowserReactSrcQuickEditTsxQuickEditTsx INSTANCE = new BrowserReactSrcQuickEditTsxQuickEditTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport React, { useEffect, useState } from 'react'\nimport { useIsDark } from '../util/services.js'\nimport ErrorBoundary from '../sidebar-tsx/ErrorBoundary.js'\nimport { QuickEditChat } from './QuickEditChat.js'\nimport { QuickEditPropsType } from '../../../quickEditActions.js'\n\nexport const QuickEdit = (props: QuickEditPropsType) => {\n\n\tconst isDark = useIsDark()\n\n\treturn <div className={`@@void-scope ${isDark ? 'dark' : ''}`}>\n\t\t<ErrorBoundary>\n\t\t\t<QuickEditChat {...props} />\n\t\t</ErrorBoundary>\n\t</div>\n\n\n}\n"
    };

    private BrowserReactSrcQuickEditTsxQuickEditTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/quick-edit-tsx/QuickEdit.tsx";
    }

    @Override
    public String sha256() {
        return "ffb7ca9abe5a600ed6603157432f4d17dc68755a48593bde96dceb4fbd6ee629";
    }

    @Override
    public int originalByteLength() {
        return 843;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
