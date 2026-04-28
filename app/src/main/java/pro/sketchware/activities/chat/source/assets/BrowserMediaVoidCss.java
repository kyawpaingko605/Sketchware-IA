package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserMediaVoidCss implements SourceAsset {
    public static final BrowserMediaVoidCss INSTANCE = new BrowserMediaVoidCss();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\n.monaco-editor .void-sweepIdxBG {\n\tbackground-color: var(--vscode-void-sweepIdxBG);\n}\n\n.void-sweepBG {\n\tbackground-color: var(--vscode-void-sweepBG);\n}\n\n.void-highlightBG {\n\tbackground-color: var(--vscode-void-highlightBG);\n}\n\n.void-greenBG {\n\tbackground-color: var(--vscode-void-greenBG);\n}\n\n.void-redBG {\n\tbackground-color: var(--vscode-void-redBG);\n}\n\n\n/* Renamed from void-watermark-button to void-openfolder-button */\n.void-openfolder-button {\n    padding: 8px 20px;\n    background-color: #306dce;\n    color: white;\n    border: none;\n    border-radius: 4px;\n    outline: none !important;\n\tbox-shadow: none !important;\n\tcursor: pointer;\n\ttransition: background-color 0.2s ease;\n}\n.void-openfolder-button:hover {\n    background-color: #2563eb;\n}\n.void-openfolder-button:active {\n    background-color: #2563eb;\n}\n\n/* Added for Open SSH button with slightly darker color */\n.void-openssh-button {\n    padding: 8px 20px;\n    background-color: #656565; /* Slightly darker than the #5a5a5a in the TS file */\n    color: white;\n    border: none;\n    border-radius: 4px;\n    outline: none !important;\n\tbox-shadow: none !important;\n    cursor: pointer;\n    transition: background-color 0.2s ease;\n}\n.void-openssh-button:hover {\n    background-color: #474747; /* Darker on hover */\n}\n.void-openssh-button:active {\n    background-color: #474747;\n}\n\n\n.void-settings-watermark-button {\n\tmargin: 8px 0;\n\tpadding: 8px 20px;\n\tbackground-color: var(--vscode-input-background);\n\tcolor: var(--vscode-input-foreground);\n\tborder: none;\n\tborder-radius: 4px;\n\toutline: none !important;\n\tbox-shadow: none !important;\n\tcursor: pointer;\n\ttransition: all 0.2s ease;\n}\n\n.void-settings-watermark-button:hover {\n\tfilter: brightness(1.1);\n}\n\n.void-settings-watermark-button:active {\n\tfilter: brightness(1.1);\n}\n\n.void-link {\n\tcolor: #3b82f6;\n\tcursor: pointer;\n\ttransition: all 0.2s ease;\n}\n\n.void-link:hover {\n\topacity: 80%;\n}\n\n/* styles for all containers used by void */\n.void-scope {\n\t--scrollbar-vertical-width: 8px;\n\t--scrollbar-horizontal-height: 6px;\n}\n\n/* Target both void-scope and all its descendants with scrollbars */\n.void-scope,\n.void-scope * {\n\tscrollbar-width: thin !important;\n\tscrollbar-color: var(--void-bg-1) var(--void-bg-3) !important;\n\t/* For Firefox */\n}\n\n.void-scope::-webkit-scrollbar,\n.void-scope *::-webkit-scrollbar {\n\twidth: var(--scrollbar-vertical-width) !important;\n\theight: var(--scrollbar-horizontal-height) !important;\n\tbackground-color: var(--void-bg-3) !important;\n}\n\n.void-scope::-webkit-scrollbar-thumb,\n.void-scope *::-webkit-scrollbar-thumb {\n\tbackground-color: var(--void-bg-1) !important;\n\tborder-radius: 4px !important;\n\tborder: none !important;\n\t-webkit-box-shadow: none !important;\n\tbox-shadow: none !important;\n}\n\n.void-scope::-webkit-scrollbar-thumb:hover,\n.void-scope *::-webkit-scrollbar-thumb:hover {\n\tbackground-color: var(--void-bg-1) !important;\n\tfilter: brightness(1.1) !important;\n}\n\n.void-scope::-webkit-scrollbar-thumb:active,\n.void-scope *::-webkit-scrollbar-thumb:active {\n\tbackground-color: var(--void-bg-1) !important;\n\tfilter: brightness(1.2) !important;\n}\n\n.void-scope::-webkit-scrollbar-track,\n.void-scope *::-webkit-scrollbar-track {\n\tbackground-color: var(--void-bg-3) !important;\n\tborder: none !important;\n}\n\n.void-scope::-webkit-scrollbar-corner,\n.void-scope *::-webkit-scrollbar-corner {\n\tbackground-color: var(--void-bg-3) !important;\n}\n\n/* Add void-scrollable-element styles to match */\n.void-scrollable-element {\n\tbackground-color: var(--vscode-editor-background);\n\t--scrollbar-vertical-width: 14px;\n\t--scrollbar-horizontal-height: 6px;\n\toverflow: auto;\n\t/* Ensure scrollbars are shown when needed */\n}\n\n.void-scrollable-element,\n.void-scrollable-element * {\n\tscrollbar-width: thin !important;\n\t/* For Firefox */\n\tscrollbar-color: var(--void-bg-1) var(--void-bg-3) !important;\n\t/* For Firefox */\n}\n\n.void-scrollable-element::-webkit-scrollbar,\n.void-scrollable-element *::-webkit-scrollbar {\n\twidth: var(--scrollbar-vertical-width) !important;\n\theight: var(--scrollbar-horizontal-height) !important;\n\tbackground-color: var(--void-bg-3) !important;\n}\n\n.void-scrollable-element::-webkit-scrollbar-thumb,\n.void-scrollable-element *::-webkit-scrollbar-thumb {\n\tbackground-color: var(--void-bg-1) !important;\n\tborder-radius: 4px !important;\n\tborder: none !important;\n\t-webkit-box-shadow: none !important;\n\tbox-shadow: none !important;\n}\n\n.void-scrollable-element::-webkit-scrollbar-thumb:hover,\n.void-scrollable-element *::-webkit-scrollbar-thumb:hover {\n\tbackground-color: var(--void-bg-1) !important;\n\tfilter: brightness(1.1) !important;\n}\n\n.void-scrollable-element::-webkit-scrollbar-thumb:active,\n.void-scrollable-element *::-webkit-scrollbar-thumb:active {\n\tbackground-color: var(--void-bg-1) !important;\n\tfilter: brightness(1.2) !important;\n}\n\n.void-scrollable-element::-webkit-scrollbar-track,\n.void-scrollable-element *::-webkit-scrollbar-track {\n\tbackground-color: var(--void-bg-3) !important;\n\tborder: none !important;\n}\n\n.void-scrollable-element::-webkit-scrollbar-corner,\n.void-scrollable-element *::-webkit-scrollbar-corner {\n\tbackground-color: var(--void-bg-3) !important;\n}\n"
    };

    private BrowserMediaVoidCss() {
    }

    @Override
    public String path() {
        return "browser/media/void.css";
    }

    @Override
    public String sha256() {
        return "396e9fbb47a8bdfb100e15cdb939755f91bd8d4dd21a5295156ee973b95a1d74";
    }

    @Override
    public int originalByteLength() {
        return 5462;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
