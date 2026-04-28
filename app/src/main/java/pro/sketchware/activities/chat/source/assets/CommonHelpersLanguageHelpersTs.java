package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonHelpersLanguageHelpersTs implements SourceAsset {
    public static final CommonHelpersLanguageHelpersTs INSTANCE = new CommonHelpersLanguageHelpersTs();

    private static final String[] CHUNKS = new String[] {
            "// /*--------------------------------------------------------------------------------------\n//  *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n//  *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n//  *--------------------------------------------------------------------------------------*/\n\nimport { URI } from '../../../../../base/common/uri.js';\nimport { ILanguageService } from '../../../../../editor/common/languages/language.js';\nimport { separateOutFirstLine } from './util.js';\n\n\n// this works better than model.getLanguageId()\nexport function detectLanguage(languageService: ILanguageService, opts: { uri: URI | null, fileContents: string | undefined }) {\n\tconst firstLine = opts.fileContents ? separateOutFirstLine(opts.fileContents)?.[0] : undefined\n\tconst fullLang = languageService.createByFilepathOrFirstLine(opts.uri, firstLine)\n\treturn fullLang.languageId || 'plaintext'\n}\n\n// --- conversions\nexport const convertToVscodeLang = (languageService: ILanguageService, markdownLang: string) => {\n\tif (markdownLang in markdownLangToVscodeLang)\n\t\treturn markdownLangToVscodeLang[markdownLang]\n\n\tconst { languageId } = languageService.createById(markdownLang)\n\treturn languageId\n}\n\n\n// // eg \"bash\" -> \"shell\"\nconst markdownLangToVscodeLang: { [key: string]: string } = {\n\t// Web Technologies\n\t'html': 'html',\n\t'css': 'css',\n\t'scss': 'scss',\n\t'sass': 'scss',\n\t'less': 'less',\n\t'javascript': 'typescript',\n\t'js': 'typescript', // use more general renderer\n\t'jsx': 'typescriptreact',\n\t'typescript': 'typescript',\n\t'ts': 'typescript',\n\t'tsx': 'typescriptreact',\n\t'json': 'json',\n\t'jsonc': 'json',\n\n\t// Programming Languages\n\t'python': 'python',\n\t'py': 'python',\n\t'java': 'java',\n\t'cpp': 'cpp',\n\t'c++': 'cpp',\n\t'c': 'c',\n\t'csharp': 'csharp',\n\t'cs': 'csharp',\n\t'c#': 'csharp',\n\t'go': 'go',\n\t'golang': 'go',\n\t'rust': 'rust',\n\t'rs': 'rust',\n\t'ruby': 'ruby',\n\t'rb': 'ruby',\n\t'php': 'php',\n\t'shell': 'shellscript', // this is important\n\t'bash': 'shellscript',\n\t'sh': 'shellscript',\n\t'zsh': 'shellscript',\n\n\t// Markup and Config\n\t'markdown': 'markdown',\n\t'md': 'markdown',\n\t'xml': 'xml',\n\t'svg': 'xml',\n\t'yaml': 'yaml',\n\t'yml': 'yaml',\n\t'ini': 'ini',\n\t'toml': 'ini',\n\n\t// Database and Query Languages\n\t'sql': 'sql',\n\t'mysql': 'sql',\n\t'postgresql': 'sql',\n\t'graphql': 'graphql',\n\t'gql': 'graphql',\n\n\t// Others\n\t'dockerfile': 'dockerfile',\n\t'docker': 'dockerfile',\n\t'makefile': 'makefile',\n\t'plaintext': 'plaintext',\n\t'text': 'plaintext'\n};\n\n// // eg \".ts\" -> \"typescript\"\n// const fileExtensionToVscodeLanguage: { [key: string]: string } = {\n// \t// Web\n// \t'html': 'html',\n// \t'htm': 'html',\n// \t'css': 'css',\n// \t'scss': 'scss',\n// \t'less': 'less',\n// \t'js': 'javascript',\n// \t'jsx': 'javascript',\n// \t'ts': 'typescript',\n// \t'tsx': 'typescript',\n// \t'json': 'json',\n// \t'jsonc': 'json',\n\n// \t// Programming Languages\n// \t'py': 'python',\n// \t'java': 'java',\n// \t'cpp': 'cpp',\n// \t'cc': 'cpp',\n// \t'c': 'c',\n// \t'h': 'cpp',\n// \t'hpp': 'cpp',\n// \t'cs': 'csharp',\n// \t'go': 'go',\n// \t'rs': 'rust',\n// \t'rb': 'ruby',\n// \t'php': 'php',\n// \t'sh': 'shell',\n// \t'bash': 'shell',\n// \t'zsh': 'shell',\n\n// \t// Markup/Config\n// \t'md': 'markdown',\n// \t'markdown': 'markdown',\n// \t'xml': 'xml',\n// \t'svg': 'xml',\n// \t'yaml': 'yaml',\n// \t'yml': 'yaml',\n// \t'ini': 'ini',\n// \t'toml': 'ini',\n\n// \t// Other\n// \t'sql': 'sql',\n// \t'graphql': 'graphql',\n// \t'gql': 'graphql',\n// \t'dockerfile': 'dockerfile',\n// \t'docker': 'dockerfile',\n// \t'mk': 'makefile',\n\n// \t// Config Files and Dot Files\n// \t'npmrc': 'ini',\n// \t'env': 'ini',\n// \t'gitignore': 'ignore',\n// \t'dockerignore': 'ignore',\n// \t'eslintrc': 'json',\n// \t'babelrc': 'json',\n// \t'prettierrc': 'json',\n// \t'stylelintrc': 'json',\n// \t'editorconfig': 'ini',\n// \t'htaccess': 'apacheconf',\n// \t'conf': 'ini',\n// \t'config': 'ini',\n\n// \t// Package Files\n// \t'package': 'json',\n// \t'package-lock': 'json',\n// \t'gemfile': 'ruby',\n// \t'podfile': 'ruby',\n// \t'rakefile': 'ruby',\n\n// \t// Build Systems\n// \t'cmake': 'cmake',\n// \t'makefile': 'makefile',\n// \t'gradle': 'groovy',\n\n// \t// Shell Scripts\n// \t'bashrc': 'shell',\n// \t'zshrc': 'shell',\n// \t'fish': 'shell',\n\n// \t// Version Control\n// \t'gitconfig': 'ini',\n// \t'hgrc': 'ini',\n// \t'svnconfig': 'ini',\n\n// \t// Web Server\n// \t'nginx': 'nginx',\n\n// \t// Misc Config\n// \t'properties': 'properties',\n// \t'cfg': 'ini',\n// \t'reg': 'ini'\n// };\n\n\n// export function filenameToVscodeLanguage(filename: string): string | undefined {\n\n\n\n\n// \tconst ext = filename.toLowerCase().split('.').pop();\n// \tif (!ext) return undefined;\n\n// \treturn fileExtensionToVscodeLanguage[ext];\n// }\n"
    };

    private CommonHelpersLanguageHelpersTs() {
    }

    @Override
    public String path() {
        return "common/helpers/languageHelpers.ts";
    }

    @Override
    public String sha256() {
        return "cb0e81cece13c52b0a74bb047086893723d0e7c9266c0a00545a9c90a35ffbae";
    }

    @Override
    public int originalByteLength() {
        return 4598;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
