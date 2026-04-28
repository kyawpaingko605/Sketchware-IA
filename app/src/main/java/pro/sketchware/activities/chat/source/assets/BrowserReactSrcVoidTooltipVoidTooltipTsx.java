package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcVoidTooltipVoidTooltipTsx implements SourceAsset {
    public static final BrowserReactSrcVoidTooltipVoidTooltipTsx INSTANCE = new BrowserReactSrcVoidTooltipVoidTooltipTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport '../styles.css'\nimport { Tooltip } from 'react-tooltip';\nimport 'react-tooltip/dist/react-tooltip.css';\nimport { useIsDark } from '../util/services.js';\n\n/**\n * Creates a configured global tooltip component with consistent styling\n * To use:\n * 1. Mount a Tooltip with some id eg id='void-tooltip'\n * 2. Add data-tooltip-id=\"void-tooltip\" and data-tooltip-content=\"Your tooltip text\" to any element\n */\nexport const VoidTooltip = () => {\n\n\n\tconst isDark = useIsDark()\n\n\treturn (\n\n\t\t// use native colors so we don't have to worry about @@void-scope styles\n\t\t// --void-bg-1: var(--vscode-input-background);\n\t\t// --void-bg-1-alt: var(--vscode-badge-background);\n\t\t// --void-bg-2: var(--vscode-sideBar-background);\n\t\t// --void-bg-2-alt: color-mix(in srgb, var(--vscode-sideBar-background) 30%, var(--vscode-editor-background) 70%);\n\t\t// --void-bg-3: var(--vscode-editor-background);\n\n\t\t// --void-fg-0: color-mix(in srgb, var(--vscode-tab-activeForeground) 90%, black 10%);\n\t\t// --void-fg-1: var(--vscode-editor-foreground);\n\t\t// --void-fg-2: var(--vscode-input-foreground);\n\t\t// --void-fg-3: var(--vscode-input-placeholderForeground);\n\t\t// /* --void-fg-4: var(--vscode-tab-inactiveForeground); */\n\t\t// --void-fg-4: var(--vscode-list-deemphasizedForeground);\n\n\t\t// --void-warning: var(--vscode-charts-yellow);\n\n\t\t// --void-border-1: var(--vscode-commandCenter-activeBorder);\n\t\t// --void-border-2: var(--vscode-commandCenter-border);\n\t\t// --void-border-3: var(--vscode-commandCenter-inactiveBorder);\n\t\t// --void-border-4: var(--vscode-editorGroup-border);\n\n\t\t<>\n\t\t\t<style>\n\t\t\t\t{`\n\t\t\t\t#void-tooltip, #void-tooltip-orange, #void-tooltip-green, #void-tooltip-ollama-settings, #void-tooltip-provider-info {\n\t\t\t\t\tfont-size: 12px;\n\t\t\t\t\tpadding: 0px 8px;\n\t\t\t\t\tborder-radius: 6px;\n\t\t\t\t\tz-index: 999999;\n\t\t\t\t\tmax-width: 300px;\n\t\t\t\t\tword-wrap: break-word;\n\t\t\t\t}\n\n\t\t\t\t#void-tooltip {\n\t\t\t\t\tbackground-color: var(--vscode-editor-background);\n\t\t\t\t\tcolor: var(--vscode-input-foreground);\n\t\t\t\t}\n\n\t\t\t\t#void-tooltip-orange {\n\t\t\t\t\tbackground-color: #F6762A;\n\t\t\t\t\tcolor: white;\n\t\t\t\t}\n\n\t\t\t\t#void-tooltip-green {\n\t\t\t\t\tbackground-color: #228B22;\n\t\t\t\t\tcolor: white;\n\t\t\t\t}\n\n\t\t\t\t#void-tooltip-ollama-settings, #void-tooltip-provider-info {\n\t\t\t\t\tbackground-color: var(--vscode-editor-background);\n\t\t\t\t\tcolor: var(--vscode-input-foreground);\n\t\t\t\t}\n\n\t\t\t\t.react-tooltip-arrow {\n\t\t\t\t\tz-index: -1 !important; /* Keep arrow behind content (somehow this isnt done automatically) */\n\t\t\t\t}\n\t\t\t\t`}\n\t\t\t</style>\n\n\n\t\t\t<Tooltip\n\t\t\t\tid=\"void-tooltip\"\n\t\t\t\t// border='1px solid var(--vscode-editorGroup-border)'\n\t\t\t\tborder='1px solid rgba(100,100,100,.2)'\n\t\t\t\topacity={1}\n\t\t\t\tdelayShow={50}\n\t\t\t/>\n\t\t\t<Tooltip\n\t\t\t\tid=\"void-tooltip-orange\"\n\t\t\t\tborder='1px solid rgba(200,200,200,.3)'\n\t\t\t\topacity={1}\n\t\t\t\tdelayShow={50}\n\t\t\t/>\n\t\t\t<Tooltip\n\t\t\t\tid=\"void-tooltip-green\"\n\t\t\t\tborder='1px solid rgba(200,200,200,.3)'\n\t\t\t\topacity={1}\n\t\t\t\tdelayShow={50}\n\t\t\t/>\n\t\t\t<Tooltip\n\t\t\t\tid=\"void-tooltip-ollama-settings\"\n\t\t\t\tborder='1px solid rgba(100,100,100,.2)'\n\t\t\t\topacity={1}\n\t\t\t\topenEvents={{ mouseover: true, click: true, focus: true }}\n\t\t\t\tplace='right'\n\t\t\t\tstyle={{ pointerEvents: 'all', userSelect: 'text', fontSize: 11 }}\n\t\t\t>\n\t\t\t\t<div style={{ padding: '8px 10px' }}>\n\t\t\t\t\t<div style={{ opacity: 0.8, textAlign: 'center', fontWeight: 'bold', marginBottom: 8 }}>\n\t\t\t\t\t\tGood starter models\n\t\t\t\t\t</div>\n\t\t\t\t\t<div style={{ marginBottom: 4 }}>\n\t\t\t\t\t\t<span style={{ opacity: 0.8 }}>For chat:{` `}</span>\n\t\t\t\t\t\t<span style={{ opacity: 0.8, fontWeight: 'bold' }}>gemma3</span>\n\t\t\t\t\t</div>\n\t\t\t\t\t<div style={{ marginBottom: 4 }}>\n\t\t\t\t\t\t<span style={{ opacity: 0.8 }}>For autocomplete:{` `}</span>\n\t\t\t\t\t\t<span style={{ opacity: 0.8, fontWeight: 'bold' }}>qwen2.5-coder</span>\n\t\t\t\t\t</div>\n\t\t\t\t\t<div style={{ marginBottom: 0 }}>\n\t\t\t\t\t\t<span style={{ opacity: 0.8 }}>Use the largest version of these you can!</span>\n\t\t\t\t\t</div>\n\t\t\t\t</div>\n\t\t\t</Tooltip>\n\n\t\t\t<Tooltip\n\t\t\t\tid=\"void-tooltip-provider-info\"\n\t\t\t\tborder='1px solid rgba(100,100,100,.2)'\n\t\t\t\topacity={1}\n\t\t\t\tdelayShow={50}\n\t\t\t\tstyle={{ pointerEvents: 'all', userSelect: 'text', fontSize: 11, maxWidth: '280px', paddingTop:'8px', paddingBottom:'8px' }}\n\t\t\t/>\n\t\t</>\n\t);\n};\n"
    };

    private BrowserReactSrcVoidTooltipVoidTooltipTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/void-tooltip/VoidTooltip.tsx";
    }

    @Override
    public String sha256() {
        return "a461f96ef05559831a6e151b67cbf1cbe42161030d4574f03d58b4c92b2661fa";
    }

    @Override
    public int originalByteLength() {
        return 4493;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
