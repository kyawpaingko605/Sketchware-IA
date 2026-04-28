package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcStylesCss implements SourceAsset {
    public static final BrowserReactSrcStylesCss INSTANCE = new BrowserReactSrcStylesCss();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\n@tailwind base;\n@tailwind components;\n@tailwind utilities;\n\n& {\n\t--void-bg-1: var(--vscode-input-background);\n\t--void-bg-1-alt: var(--vscode-badge-background);\n\t--void-bg-2: var(--vscode-sideBar-background);\n\t--void-bg-2-alt: color-mix(in srgb, var(--vscode-editor-background) 30%, var(--vscode-sideBar-background) 70%);\n\t--void-bg-2-hover: color-mix(in srgb, var(--vscode-editor-foreground) 2%, var(--vscode-sideBar-background) 98%);\n\t--void-bg-3: var(--vscode-editor-background);\n\n\t--void-fg-0: color-mix(in srgb, var(--vscode-tab-activeForeground) 90%, black 10%);\n\t--void-fg-1: var(--vscode-editor-foreground);\n\t--void-fg-2: var(--vscode-input-foreground);\n\t--void-fg-3: var(--vscode-input-placeholderForeground);\n\t/* --void-fg-4: var(--vscode-tab-inactiveForeground); */\n\t--void-fg-4: var(--vscode-list-deemphasizedForeground);\n\n\t--void-warning: var(--vscode-charts-yellow);\n\n\t--void-border-1: var(--vscode-commandCenter-activeBorder);\n\t--void-border-2: var(--vscode-commandCenter-border);\n\t--void-border-3: var(--vscode-commandCenter-inactiveBorder);\n\t--void-border-4: var(--vscode-editorGroup-border);\n\n\t--void-ring-color: #007FD4;\n\t--void-link-color: #007FD4;\n}\n\n.select-child-restyle select {\n\ttext-overflow: ellipsis;\n\twhite-space: nowrap;\n\tpadding-right: 24px;\n}\n\n.void-force-child-placeholder-void-fg-1 ::placeholder {\n\tcolor: var(--void-fg-3);\n}\n\n* {\n\toutline: none !important;\n}\n\n\n\n.inherit-bg-all-restyle > * {\n\tbackground-color: inherit !important;\n}\n\n\n.bg-editor-style-override {\n\t--vscode-sideBar-background: var(--vscode-editor-background);\n}\n\n\n\n/* html {\n\tfont-size: var(--vscode-font-size);\n}\n\n.btn {\n\t@apply cursor-pointer transition-colors;\n\n\t&.btn-primary {\n\t\t@apply bg-vscode-button-bg text-vscode-button-fg;\n\n\t\t&:not(:disabled) {\n\t\t\t@apply hover:bg-vscode-button-hoverBg;\n\t\t}\n\t}\n\n\t&.btn-sm {\n\t\t@apply px-3 py-1 text-sm;\n\t}\n\n\t&.btn-secondary {\n\t\t@apply bg-vscode-button-secondary-bg text-vscode-button-secondary-fg;\n\n\t\t&:not(:disabled) {\n\t\t\t@apply hover:bg-vscode-button-secondary-hoverBg;\n\t\t}\n\t}\n\n\t&:disabled {\n\t\t@apply opacity-75 cursor-not-allowed;\n\t}\n}\n\n.input {\n\t@apply bg-vscode-input-bg text-vscode-input-fg border-vscode-input-border focus:outline-vscode-focus-border;\n}\n\n.dropdown {\n\t@apply bg-vscode-dropdown-bg text-vscode-dropdown-foreground border-vscode-dropdown-border focus:outline-vscode-focus-border;\n} */\n"
    };

    private BrowserReactSrcStylesCss() {
    }

    @Override
    public String path() {
        return "browser/react/src/styles.css";
    }

    @Override
    public String sha256() {
        return "40e814b0d5f1cff692b6113ed44d2019df22718a2e57b40b023de7215b11ace8";
    }

    @Override
    public int originalByteLength() {
        return 2681;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
