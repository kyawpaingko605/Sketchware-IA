package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcSidebarTsxErrorDisplayTsx implements SourceAsset {
    public static final BrowserReactSrcSidebarTsxErrorDisplayTsx INSTANCE = new BrowserReactSrcSidebarTsxErrorDisplayTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport React, { useEffect, useState } from 'react';\nimport { AlertCircle, ChevronDown, ChevronUp, X } from 'lucide-react';\nimport { useSettingsState } from '../util/services.js';\nimport { errorDetails } from '../../../../common/sendLLMMessageTypes.js';\n\n\nexport const ErrorDisplay = ({\n\tmessage: message_,\n\tfullError,\n\tonDismiss,\n\tshowDismiss,\n}: {\n\tmessage: string,\n\tfullError: Error | null,\n\tonDismiss: (() => void) | null,\n\tshowDismiss?: boolean,\n}) => {\n\tconst [isExpanded, setIsExpanded] = useState(false);\n\n\tconst details = errorDetails(fullError)\n\tconst isExpandable = !!details\n\n\tconst message = message_ + ''\n\n\treturn (\n\t\t<div className={`rounded-lg border border-red-200 bg-red-50 p-4 overflow-auto`}>\n\t\t\t{/* Header */}\n\t\t\t<div className='flex items-start justify-between'>\n\t\t\t\t<div className='flex gap-3'>\n\t\t\t\t\t<AlertCircle className='h-5 w-5 text-red-600 mt-0.5' />\n\t\t\t\t\t<div className='flex-1'>\n\t\t\t\t\t\t<h3 className='font-semibold text-red-800'>\n\t\t\t\t\t\t\t{/* eg Error */}\n\t\t\t\t\t\t\tError\n\t\t\t\t\t\t</h3>\n\t\t\t\t\t\t<p className='text-red-700 mt-1'>\n\t\t\t\t\t\t\t{/* eg Something went wrong */}\n\t\t\t\t\t\t\t{message}\n\t\t\t\t\t\t</p>\n\t\t\t\t\t</div>\n\t\t\t\t</div>\n\n\t\t\t\t<div className='flex gap-2'>\n\t\t\t\t\t{isExpandable && (\n\t\t\t\t\t\t<button className='text-red-600 hover:text-red-800 p-1 rounded'\n\t\t\t\t\t\t\tonClick={() => setIsExpanded(!isExpanded)}\n\t\t\t\t\t\t>\n\t\t\t\t\t\t\t{isExpanded ? (\n\t\t\t\t\t\t\t\t<ChevronUp className='h-5 w-5' />\n\t\t\t\t\t\t\t) : (\n\t\t\t\t\t\t\t\t<ChevronDown className='h-5 w-5' />\n\t\t\t\t\t\t\t)}\n\t\t\t\t\t\t</button>\n\t\t\t\t\t)}\n\t\t\t\t\t{showDismiss && onDismiss && (\n\t\t\t\t\t\t<button className='text-red-600 hover:text-red-800 p-1 rounded'\n\t\t\t\t\t\t\tonClick={onDismiss}\n\t\t\t\t\t\t>\n\t\t\t\t\t\t\t<X className='h-5 w-5' />\n\t\t\t\t\t\t</button>\n\t\t\t\t\t)}\n\t\t\t\t</div>\n\t\t\t</div>\n\n\t\t\t{/* Expandable Details */}\n\t\t\t{isExpanded && details && (\n\t\t\t\t<div className='mt-4 space-y-3 border-t border-red-200 pt-3 overflow-auto'>\n\t\t\t\t\t<div>\n\t\t\t\t\t\t<span className='font-semibold text-red-800'>Full Error: </span>\n\t\t\t\t\t\t<pre className='text-red-700'>{details}</pre>\n\t\t\t\t\t</div>\n\t\t\t\t</div>\n\t\t\t)}\n\t\t</div>\n\t);\n};\n"
    };

    private BrowserReactSrcSidebarTsxErrorDisplayTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/sidebar-tsx/ErrorDisplay.tsx";
    }

    @Override
    public String sha256() {
        return "34135f375240273fadd93b42b86751a9d76cbbeb09ba7a98a9f14440c8aed974";
    }

    @Override
    public int originalByteLength() {
        return 2351;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
