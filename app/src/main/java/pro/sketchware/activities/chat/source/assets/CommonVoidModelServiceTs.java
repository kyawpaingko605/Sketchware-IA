package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonVoidModelServiceTs implements SourceAsset {
    public static final CommonVoidModelServiceTs INSTANCE = new CommonVoidModelServiceTs();

    private static final String[] CHUNKS = new String[] {
            "import { Disposable, IReference } from '../../../../base/common/lifecycle.js';\nimport { URI } from '../../../../base/common/uri.js';\nimport { ITextModel } from '../../../../editor/common/model.js';\nimport { IResolvedTextEditorModel, ITextModelService } from '../../../../editor/common/services/resolverService.js';\nimport { registerSingleton, InstantiationType } from '../../../../platform/instantiation/common/extensions.js';\nimport { createDecorator } from '../../../../platform/instantiation/common/instantiation.js';\nimport { ITextFileService } from '../../../services/textfile/common/textfiles.js';\n\ntype VoidModelType = {\n\tmodel: ITextModel | null;\n\teditorModel: IResolvedTextEditorModel | null;\n};\n\nexport interface IVoidModelService {\n\treadonly _serviceBrand: undefined;\n\tinitializeModel(uri: URI): Promise<void>;\n\tgetModel(uri: URI): VoidModelType;\n\tgetModelFromFsPath(fsPath: string): VoidModelType;\n\tgetModelSafe(uri: URI): Promise<VoidModelType>;\n\tsaveModel(uri: URI): Promise<void>;\n\n}\n\nexport const IVoidModelService = createDecorator<IVoidModelService>('voidVoidModelService');\n\nclass VoidModelService extends Disposable implements IVoidModelService {\n\t_serviceBrand: undefined;\n\tstatic readonly ID = 'voidVoidModelService';\n\tprivate readonly _modelRefOfURI: Record<string, IReference<IResolvedTextEditorModel>> = {};\n\n\tconstructor(\n\t\t@ITextModelService private readonly _textModelService: ITextModelService,\n\t\t@ITextFileService private readonly _textFileService: ITextFileService,\n\t) {\n\t\tsuper();\n\t}\n\n\tsaveModel = async (uri: URI) => {\n\t\tawait this._textFileService.save(uri, { // we want [our change] -> [save] so it's all treated as one change.\n\t\t\tskipSaveParticipants: true // avoid triggering extensions etc (if they reformat the page, it will add another item to the undo stack)\n\t\t})\n\t}\n\n\tinitializeModel = async (uri: URI) => {\n\t\ttry {\n\t\t\tif (uri.fsPath in this._modelRefOfURI) return;\n\t\t\tconst editorModelRef = await this._textModelService.createModelReference(uri);\n\t\t\t// Keep a strong reference to prevent disposal\n\t\t\tthis._modelRefOfURI[uri.fsPath] = editorModelRef;\n\t\t}\n\t\tcatch (e) {\n\t\t\tconsole.log('InitializeModel error:', e)\n\t\t}\n\t};\n\n\tgetModelFromFsPath = (fsPath: string): VoidModelType => {\n\t\tconst editorModelRef = this._modelRefOfURI[fsPath];\n\t\tif (!editorModelRef) {\n\t\t\treturn { model: null, editorModel: null };\n\t\t}\n\n\t\tconst model = editorModelRef.object.textEditorModel;\n\n\t\tif (!model) {\n\t\t\treturn { model: null, editorModel: editorModelRef.object };\n\t\t}\n\n\t\treturn { model, editorModel: editorModelRef.object };\n\t};\n\n\tgetModel = (uri: URI) => {\n\t\treturn this.getModelFromFsPath(uri.fsPath)\n\t}\n\n\n\tgetModelSafe = async (uri: URI): Promise<VoidModelType> => {\n\t\tif (!(uri.fsPath in this._modelRefOfURI)) await this.initializeModel(uri);\n\t\treturn this.getModel(uri);\n\n\t};\n\n\toverride dispose() {\n\t\tsuper.dispose();\n\t\tfor (const ref of Object.values(this._modelRefOfURI)) {\n\t\t\tref.dispose(); // release reference to allow disposal\n\t\t}\n\t}\n}\n\nregisterSingleton(IVoidModelService, VoidModelService, InstantiationType.Eager);\n"
    };

    private CommonVoidModelServiceTs() {
    }

    @Override
    public String path() {
        return "common/voidModelService.ts";
    }

    @Override
    public String sha256() {
        return "9d3de6f99a76b079b352e6e9ca8c0bb007fd84768eb7c9cf9b5cf35a034b70ea";
    }

    @Override
    public int originalByteLength() {
        return 3053;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
