package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserFileServiceTs implements SourceAsset {
    public static final BrowserFileServiceTs INSTANCE = new BrowserFileServiceTs();

    private static final String[] CHUNKS = new String[] {
            "import { localize2 } from '../../../../nls.js';\nimport { URI } from '../../../../base/common/uri.js';\nimport { Action2, registerAction2, MenuId } from '../../../../platform/actions/common/actions.js';\nimport { ServicesAccessor } from '../../../../editor/browser/editorExtensions.js';\nimport { INotificationService } from '../../../../platform/notification/common/notification.js';\nimport { IFileService } from '../../../../platform/files/common/files.js';\nimport { IClipboardService } from '../../../../platform/clipboard/common/clipboardService.js';\nimport { IDirectoryStrService } from '../common/directoryStrService.js';\nimport { messageOfSelection } from '../common/prompt/prompts.js';\nimport { IVoidModelService } from '../common/voidModelService.js';\n\n\n\nclass FilePromptActionService extends Action2 {\n\tprivate static readonly VOID_COPY_FILE_PROMPT_ID = 'void.copyfileprompt'\n\n\tconstructor() {\n\t\tsuper({\n\t\t\tid: FilePromptActionService.VOID_COPY_FILE_PROMPT_ID,\n\t\t\ttitle: localize2('voidCopyPrompt', 'Void: Copy Prompt'),\n\t\t\tmenu: [{\n\t\t\t\tid: MenuId.ExplorerContext,\n\t\t\t\tgroup: '8_void',\n\t\t\t\torder: 1,\n\t\t\t}]\n\t\t});\n\t}\n\n\tasync run(accessor: ServicesAccessor, uri: URI): Promise<void> {\n\t\ttry {\n\t\t\tconst fileService = accessor.get(IFileService);\n\t\t\tconst clipboardService = accessor.get(IClipboardService)\n\t\t\tconst directoryStrService = accessor.get(IDirectoryStrService)\n\t\t\tconst voidModelService = accessor.get(IVoidModelService)\n\n\t\t\tconst stat = await fileService.stat(uri)\n\n\t\t\tconst folderOpts = {\n\t\t\t\tmaxChildren: 1000,\n\t\t\t\tmaxCharsPerFile: 2_000_000,\n\t\t\t} as const\n\n\t\t\tlet m: string = 'No contents detected'\n\t\t\tif (stat.isFile) {\n\t\t\t\tm = await messageOfSelection({\n\t\t\t\t\ttype: 'File',\n\t\t\t\t\turi,\n\t\t\t\t\tlanguage: (await voidModelService.getModelSafe(uri)).model?.getLanguageId() || '',\n\t\t\t\t\tstate: { wasAddedAsCurrentFile: false, },\n\t\t\t\t}, {\n\t\t\t\t\tfolderOpts,\n\t\t\t\t\tdirectoryStrService,\n\t\t\t\t\tfileService,\n\t\t\t\t})\n\t\t\t}\n\n\t\t\tif (stat.isDirectory) {\n\t\t\t\tm = await messageOfSelection({\n\t\t\t\t\ttype: 'Folder',\n\t\t\t\t\turi,\n\t\t\t\t}, {\n\t\t\t\t\tfolderOpts,\n\t\t\t\t\tfileService,\n\t\t\t\t\tdirectoryStrService,\n\t\t\t\t})\n\t\t\t}\n\n\t\t\tawait clipboardService.writeText(m)\n\n\t\t} catch (error) {\n\t\t\tconst notificationService = accessor.get(INotificationService)\n\t\t\tnotificationService.error(error + '')\n\t\t}\n\t}\n\n}\n\nregisterAction2(FilePromptActionService)\n"
    };

    private BrowserFileServiceTs() {
    }

    @Override
    public String path() {
        return "browser/fileService.ts";
    }

    @Override
    public String sha256() {
        return "ac0cc5b9bd22c6aaa390a4b09f0ffd2b937bc6fa54efc5dadd92ee848dfbeae0";
    }

    @Override
    public int originalByteLength() {
        return 2314;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
