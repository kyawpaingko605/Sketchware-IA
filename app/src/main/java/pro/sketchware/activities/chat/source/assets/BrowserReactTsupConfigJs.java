package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactTsupConfigJs implements SourceAsset {
    public static final BrowserReactTsupConfigJs INSTANCE = new BrowserReactTsupConfigJs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { defineConfig } from 'tsup'\n\nexport default defineConfig({\n\tentry: [\n\t\t'./src2/void-editor-widgets-tsx/index.tsx',\n\t\t'./src2/sidebar-tsx/index.tsx',\n\t\t'./src2/void-settings-tsx/index.tsx',\n\t\t'./src2/void-tooltip/index.tsx',\n\t\t'./src2/void-onboarding/index.tsx',\n\t\t'./src2/quick-edit-tsx/index.tsx',\n\t\t'./src2/diff/index.tsx',\n\t],\n\toutDir: './out',\n\tformat: ['esm'],\n\tsplitting: false,\n\n\t// dts: true,\n\t// sourcemap: true,\n\n\tclean: false,\n\tplatform: 'browser', // 'node'\n\ttarget: 'esnext',\n\tinjectStyle: true, // bundle css into the output file\n\toutExtension: () => ({ js: '.js' }),\n\t// default behavior is to take local files and make them internal (bundle them) and take imports like 'react' and leave them external (don't bundle them), we want the opposite in many ways\n\tnoExternal: [ // noExternal means we should take these things and make them not external (bundle them into the output file) - anything that doesn't start with a \".\" needs to be force-flagged as not external\n\t\t/^(?!\\.).*$/\n\t],\n\texternal: [ // these imports should be kept external ../../../ are external (this is just an optimization so the output file doesn't re-implement functions)\n\t\tnew RegExp('../../../*.js'\n\t\t\t.replaceAll('.', '\\\\.')\n\t\t\t.replaceAll('*', '.*'))\n\t],\n\ttreeshake: true,\n\tesbuildOptions(options) {\n\t\toptions.outbase = 'src2'  // tries copying the folder hierarchy starting at src2\n\t}\n})\n"
    };

    private BrowserReactTsupConfigJs() {
    }

    @Override
    public String path() {
        return "browser/react/tsup.config.js";
    }

    @Override
    public String sha256() {
        return "ba47bf796881c5430d37d17854ca70d449a3ce1bfee231e9067a8b0afbfa0190";
    }

    @Override
    public int originalByteLength() {
        return 1718;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
