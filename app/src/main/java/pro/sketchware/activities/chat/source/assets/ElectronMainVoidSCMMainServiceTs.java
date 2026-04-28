package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class ElectronMainVoidSCMMainServiceTs implements SourceAsset {
    public static final ElectronMainVoidSCMMainServiceTs INSTANCE = new ElectronMainVoidSCMMainServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { promisify } from 'util'\nimport { exec as _exec } from 'child_process'\nimport { IVoidSCMService } from '../common/voidSCMTypes.js'\n\ninterface NumStat {\n\tfile: string\n\tadded: number\n\tremoved: number\n}\n\nconst exec = promisify(_exec)\n\n//8000 and 10 were chosen after some experimentation on small-to-moderately sized changes\nconst MAX_DIFF_LENGTH = 8000\nconst MAX_DIFF_FILES = 10\n\nconst git = async (command: string, path: string): Promise<string> => {\n\tconst { stdout, stderr } = await exec(`${command}`, { cwd: path })\n\tif (stderr) {\n\t\tthrow new Error(stderr)\n\t}\n\treturn stdout.trim()\n}\n\nconst getNumStat = async (path: string, useStagedChanges: boolean): Promise<NumStat[]> => {\n\tconst staged = useStagedChanges ? '--staged' : ''\n\tconst output = await git(`git diff --numstat ${staged}`, path)\n\treturn output\n\t\t.split('\\n')\n\t\t.map((line) => {\n\t\t\tconst [added, removed, file] = line.split('\\t')\n\t\t\treturn {\n\t\t\t\tfile,\n\t\t\t\tadded: parseInt(added, 10) || 0,\n\t\t\t\tremoved: parseInt(removed, 10) || 0,\n\t\t\t}\n\t\t})\n}\n\nconst getSampledDiff = async (file: string, path: string, useStagedChanges: boolean): Promise<string> => {\n\tconst staged = useStagedChanges ? '--staged' : ''\n\tconst diff = await git(`git diff --unified=0 --no-color ${staged} -- \"${file}\"`, path)\n\treturn diff.slice(0, MAX_DIFF_LENGTH)\n}\n\nconst hasStagedChanges = async (path: string): Promise<boolean> => {\n\tconst output = await git('git diff --staged --name-only', path)\n\treturn output.length > 0\n}\n\nexport class VoidSCMService implements IVoidSCMService {\n\treadonly _serviceBrand: undefined\n\n\tasync gitStat(path: string): Promise<string> {\n\t\tconst useStagedChanges = await hasStagedChanges(path)\n\t\tconst staged = useStagedChanges ? '--staged' : ''\n\t\treturn git(`git diff --stat ${staged}`, path)\n\t}\n\n\tasync gitSampledDiffs(path: string): Promise<string> {\n\t\tconst useStagedChanges = await hasStagedChanges(path)\n\t\tconst numStatList = await getNumStat(path, useStagedChanges)\n\t\tconst topFiles = numStatList\n\t\t\t.sort((a, b) => (b.added + b.removed) - (a.added + a.removed))\n\t\t\t.slice(0, MAX_DIFF_FILES)\n\t\tconst diffs = await Promise.all(topFiles.map(async ({ file }) => ({ file, diff: await getSampledDiff(file, path, useStagedChanges) })))\n\t\treturn diffs.map(({ file, diff }) => `==== ${file} ====\\n${diff}`).join('\\n\\n')\n\t}\n\n\tgitBranch(path: string): Promise<string> {\n\t\treturn git('git branch --show-current', path)\n\t}\n\n\tgitLog(path: string): Promise<string> {\n\t\treturn git('git log --pretty=format:\"%h|%s|%ad\" --date=short --no-merges -n 5', path)\n\t}\n}\n"
    };

    private ElectronMainVoidSCMMainServiceTs() {
    }

    @Override
    public String path() {
        return "electron-main/voidSCMMainService.ts";
    }

    @Override
    public String sha256() {
        return "9ca09b87905dae2dec957b6a38aee1a7c7d990c2c48bb6f2ad79b26ff7239384";
    }

    @Override
    public int originalByteLength() {
        return 2854;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
