package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcSidebarTsxErrorBoundaryTsx implements SourceAsset {
    public static final BrowserReactSrcSidebarTsxErrorBoundaryTsx INSTANCE = new BrowserReactSrcSidebarTsxErrorBoundaryTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport React, { Component, ErrorInfo, ReactNode } from 'react';\nimport { WarningBox } from '../void-settings-tsx/WarningBox.js';\n\ninterface Props {\n\tchildren: ReactNode;\n\tfallback?: ReactNode;\n\tonDismiss?: () => void;\n}\n\ninterface State {\n\thasError: boolean;\n\terror: Error | null;\n\terrorInfo: ErrorInfo | null;\n}\n\nclass ErrorBoundary extends Component<Props, State> {\n\tconstructor(props: Props) {\n\t\tsuper(props);\n\t\tthis.state = {\n\t\t\thasError: false,\n\t\t\terror: null,\n\t\t\terrorInfo: null\n\t\t};\n\t}\n\n\tstatic getDerivedStateFromError(error: Error): Partial<State> {\n\t\treturn {\n\t\t\thasError: true,\n\t\t\terror\n\t\t};\n\t}\n\n\tcomponentDidCatch(error: Error, errorInfo: ErrorInfo): void {\n\t\tthis.setState({\n\t\t\terror,\n\t\t\terrorInfo\n\t\t});\n\t}\n\n\trender(): ReactNode {\n\t\tif (this.state.hasError && this.state.error) {\n\t\t\t// If a custom fallback is provided, use it\n\t\t\tif (this.props.fallback) {\n\t\t\t\treturn this.props.fallback;\n\t\t\t}\n\n\t\t\t// Use ErrorDisplay component as the default error UI\n\t\t\treturn (\n\t\t\t\t<WarningBox text={this.state.error + ''} />\n\t\t\t\t// <ErrorDisplay\n\t\t\t\t// \tmessage={this.state.error + ''}\n\t\t\t\t// \tfullError={this.state.error}\n\t\t\t\t// \tonDismiss={this.props.onDismiss || null}\n\t\t\t\t// />\n\t\t\t);\n\t\t}\n\n\t\treturn this.props.children;\n\t}\n}\n\nexport default ErrorBoundary;\n"
    };

    private BrowserReactSrcSidebarTsxErrorBoundaryTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/sidebar-tsx/ErrorBoundary.tsx";
    }

    @Override
    public String sha256() {
        return "2feff5bda4dcca210cf26cc7a953b7474d15ef93bcf875aabd3b8cf3ad68d37e";
    }

    @Override
    public int originalByteLength() {
        return 1591;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
