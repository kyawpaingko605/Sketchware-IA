package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactBuildJs implements SourceAsset {
    public static final BrowserReactBuildJs INSTANCE = new BrowserReactBuildJs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { execSync } from 'child_process';\nimport { spawn } from 'cross-spawn'\n// Added lines below\nimport fs from 'fs';\nimport path from 'path';\nimport { fileURLToPath } from 'url';\n\nconst __filename = fileURLToPath(import.meta.url);\nconst __dirname = path.dirname(__filename);\n\nfunction doesPathExist(filePath) {\n\ttry {\n\t\tconst stats = fs.statSync(filePath);\n\n\t\treturn stats.isFile();\n\t} catch (err) {\n\t\tif (err.code === 'ENOENT') {\n\t\t\treturn false;\n\t\t}\n\t\tthrow err;\n\t}\n}\n\n/*\n\nThis function finds `globalDesiredPath` given `localDesiredPath` and `currentPath`\n\nDiagram:\n\n...basePath/\n\u2514\u2500\u2500 void/\n\t\u251c\u2500\u2500 ...currentPath/ (defined globally)\n\t\u2514\u2500\u2500 ...localDesiredPath/ (defined locally)\n\n*/\nfunction findDesiredPathFromLocalPath(localDesiredPath, currentPath) {\n\n\t// walk upwards until currentPath + localDesiredPath exists\n\twhile (!doesPathExist(path.join(currentPath, localDesiredPath))) {\n\t\tconst parentDir = path.dirname(currentPath);\n\n\t\tif (parentDir === currentPath) {\n\t\t\treturn undefined;\n\t\t}\n\n\t\tcurrentPath = parentDir;\n\t}\n\n\t// return the `globallyDesiredPath`\n\tconst globalDesiredPath = path.join(currentPath, localDesiredPath)\n\treturn globalDesiredPath;\n}\n\n// hack to refresh styles automatically\nfunction saveStylesFile() {\n\tsetTimeout(() => {\n\t\ttry {\n\t\t\tconst pathToCssFile = findDesiredPathFromLocalPath('./src/vs/workbench/contrib/void/browser/react/src2/styles.css', __dirname);\n\n\t\t\tif (pathToCssFile === undefined) {\n\t\t\t\tconsole.error('[scope-tailwind] Error finding styles.css');\n\t\t\t\treturn;\n\t\t\t}\n\n\t\t\t// Or re-write with the same content:\n\t\t\tconst content = fs.readFileSync(pathToCssFile, 'utf8');\n\t\t\tfs.writeFileSync(pathToCssFile, content, 'utf8');\n\t\t\tconsole.log('[scope-tailwind] Force-saved styles.css');\n\t\t} catch (err) {\n\t\t\tconsole.error('[scope-tailwind] Error saving styles.css:', err);\n\t\t}\n\t}, 6000);\n}\n\nconst args = process.argv.slice(2);\nconst isWatch = args.includes('--watch') || args.includes('-w');\n\nif (isWatch) {\n\t// this just builds it if it doesn't exist instead of waiting for the watcher to trigger\n\t// Check if src2/ exists; if not, do an initial scope-tailwind build\n\tif (!fs.existsSync('src2')) {\n\t\ttry {\n\t\t\tconsole.log('\ud83d\udd28 Running initial scope-tailwind build to create src2 folder...');\n\t\t\texecSync(\n\t\t\t\t'npx scope-tailwind ./src -o src2/ -s void-scope -c styles.css -p \"void-\"',\n\t\t\t\t{ stdio: 'inherit' }\n\t\t\t);\n\t\t\tconsole.log('\u2705 src2/ created successfully.');\n\t\t} catch (err) {\n\t\t\tconsole.error('\u274c Error running initial scope-tailwind build:', err);\n\t\t\tprocess.exit(1);\n\t\t}\n\t}\n\n\t// Watch mode\n\tconst scopeTailwindWatcher = spawn('npx', [\n\t\t'nodemon',\n\t\t'--watch', 'src',\n\t\t'--ext', 'ts,tsx,css',\n\t\t'--exec',\n\t\t'npx scope-tailwind ./src -o src2/ -s void-scope -c styles.css -p \"void-\"'\n\t]);\n\n\tconst tsupWatcher = spawn('npx', [\n\t\t'tsup',\n\t\t'--watch'\n\t]);\n\n\tscopeTailwindWatcher.stdout.on('data', (data) => {\n\t\tconsole.log(`[scope-tailwind] ${data}`);\n\t\t// If the output mentions \"styles.css\", trigger the save:\n\t\tif (data.toString().includes('styles.css')) {\n\t\t\tsaveStylesFile();\n\t\t}\n\t});\n\n\tscopeTailwindWatcher.stderr.on('data', (data) => {\n\t\tconsole.error(`[scope-tailwind] ${data}`);\n\t});\n\n\t// Handle tsup watcher output\n\ttsupWatcher.stdout.on('data', (data) => {\n\t\tconsole.log(`[tsup] ${data}`);\n\t});\n\n\ttsupWatcher.stderr.on('data', (data) => {\n\t\tconsole.error(`[tsup] ${data}`);\n\t});\n\n\t// Handle process termination\n\tprocess.on('SIGINT', () => {\n\t\tscopeTailwindWatcher.kill();\n\t\ttsupWatcher.kill();\n\t\tprocess.exit();\n\t});\n\n\tconsole.log('\ud83d\udd04 Watchers started! Press Ctrl+C to stop both watchers.');\n} else {\n\t// Build mode\n\tconsole.log('\ud83d\udce6 Building...');\n\n\t// Run scope-tailwind once\n\texecSync('npx scope-tailwind ./src -o src2/ -s void-scope -c styles.css -p \"void-\"', { stdio: 'inherit' });\n\n\t// Run tsup once\n\texecSync('npx tsup', { stdio: 'inherit' });\n\n\tconsole.log('\u2705 Build complete!');\n}\n"
    };

    private BrowserReactBuildJs() {
    }

    @Override
    public String path() {
        return "browser/react/build.js";
    }

    @Override
    public String sha256() {
        return "dabf8f9b42c7e7d43d8fd5147121c4eaf22e8a2439cc872d152095669af6ff5e";
    }

    @Override
    public int originalByteLength() {
        return 4195;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
