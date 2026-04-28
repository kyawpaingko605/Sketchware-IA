package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserMarkerCheckServiceTs implements SourceAsset {
    public static final BrowserMarkerCheckServiceTs INSTANCE = new BrowserMarkerCheckServiceTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { Disposable } from '../../../../base/common/lifecycle.js';\nimport { InstantiationType, registerSingleton } from '../../../../platform/instantiation/common/extensions.js';\nimport { createDecorator } from '../../../../platform/instantiation/common/instantiation.js';\nimport { IMarkerService, MarkerSeverity } from '../../../../platform/markers/common/markers.js';\nimport { ILanguageFeaturesService } from '../../../../editor/common/services/languageFeatures.js';\nimport { ITextModelService } from '../../../../editor/common/services/resolverService.js';\nimport { Range } from '../../../../editor/common/core/range.js';\nimport { CancellationToken } from '../../../../base/common/cancellation.js';\nimport { CodeActionContext, CodeActionTriggerType } from '../../../../editor/common/languages.js';\nimport { URI } from '../../../../base/common/uri.js';\nimport * as dom from '../../../../base/browser/dom.js';\n\nexport interface IMarkerCheckService {\n\treadonly _serviceBrand: undefined;\n}\n\nexport const IMarkerCheckService = createDecorator<IMarkerCheckService>('markerCheckService');\n\nclass MarkerCheckService extends Disposable implements IMarkerCheckService {\n\t_serviceBrand: undefined;\n\n\tconstructor(\n\t\t@IMarkerService private readonly _markerService: IMarkerService,\n\t\t@ILanguageFeaturesService private readonly _languageFeaturesService: ILanguageFeaturesService,\n\t\t@ITextModelService private readonly _textModelService: ITextModelService,\n\t) {\n\t\tsuper();\n\t\tconst check = async () => {\n\t\t\tconst allMarkers = this._markerService.read();\n\t\t\tconst errors = allMarkers.filter(marker => marker.severity === MarkerSeverity.Error);\n\n\t\t\tif (errors.length > 0) {\n\t\t\t\tfor (const error of errors) {\n\n\t\t\t\t\tconsole.log(`----------------------------------------------`);\n\n\t\t\t\t\tconsole.log(`${error.resource.fsPath}: ${error.startLineNumber} ${error.message} ${error.severity}`); // ! all errors in the file\n\n\t\t\t\t\ttry {\n\t\t\t\t\t\t// Get the text model for the file\n\t\t\t\t\t\tconst modelReference = await this._textModelService.createModelReference(error.resource);\n\t\t\t\t\t\tconst model = modelReference.object.textEditorModel;\n\n\t\t\t\t\t\t// Create a range from the marker\n\t\t\t\t\t\tconst range = new Range(\n\t\t\t\t\t\t\terror.startLineNumber,\n\t\t\t\t\t\t\terror.startColumn,\n\t\t\t\t\t\t\terror.endLineNumber,\n\t\t\t\t\t\t\terror.endColumn\n\t\t\t\t\t\t);\n\n\t\t\t\t\t\t// Get code action providers for this model\n\t\t\t\t\t\tconst codeActionProvider = this._languageFeaturesService.codeActionProvider;\n\t\t\t\t\t\tconst providers = codeActionProvider.ordered(model);\n\n\t\t\t\t\t\tif (providers.length > 0) {\n\t\t\t\t\t\t\t// Request code actions from each provider\n\t\t\t\t\t\t\tfor (const provider of providers) {\n\t\t\t\t\t\t\t\tconst context: CodeActionContext = {\n\t\t\t\t\t\t\t\t\ttrigger: CodeActionTriggerType.Invoke, // keeping 'trigger' since it works\n\t\t\t\t\t\t\t\t\tonly: 'quickfix'  // adding this to filter for quick fixes\n\t\t\t\t\t\t\t\t};\n\n\t\t\t\t\t\t\t\tconst actions = await provider.provideCodeActions(\n\t\t\t\t\t\t\t\t\tmodel,\n\t\t\t\t\t\t\t\t\trange,\n\t\t\t\t\t\t\t\t\tcontext,\n\t\t\t\t\t\t\t\t\tCancellationToken.None\n\t\t\t\t\t\t\t\t);\n\n\t\t\t\t\t\t\t\tif (actions?.actions?.length) {\n\n\t\t\t\t\t\t\t\t\tconst quickFixes = actions.actions.filter(action => action.isPreferred);  // ! all quickFixes for the error\n\t\t\t\t\t\t\t\t\t// const quickFixesForImports = actions.actions.filter(action => action.isPreferred && action.title.includes('import'));  // ! all possible imports\n\t\t\t\t\t\t\t\t\t// quickFixesForImports\n\n\t\t\t\t\t\t\t\t\tif (quickFixes.length > 0) {\n\t\t\t\t\t\t\t\t\t\tconsole.log('Available Quick Fixes:');\n\t\t\t\t\t\t\t\t\t\tquickFixes.forEach(action => {\n\t\t\t\t\t\t\t\t\t\t\tconsole.log(`- ${action.title}`);\n\t\t\t\t\t\t\t\t\t\t});\n\t\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t\t}\n\t\t\t\t\t\t\t}\n\t\t\t\t\t\t}\n\n\t\t\t\t\t\t// Dispose the model reference\n\t\t\t\t\t\tmodelReference.dispose();\n\t\t\t\t\t} catch (e) {\n\t\t\t\t\t\tconsole.error('Error getting quick fixes:', e);\n\t\t\t\t\t}\n\t\t\t\t}\n\t\t\t}\n\t\t}\n\t\tconst { window } = dom.getActiveWindow()\n\t\twindow.setInterval(check, 5000);\n\t}\n\n\n\n\n\tfixErrorsInFiles(uris: URI[], contextSoFar: []) {\n\t\t// const allMarkers = this._markerService.read();\n\n\n\t\t// check errors in files\n\n\n\t\t// give LLM errors in files\n\n\n\n\t}\n\n\t// private _onMarkersChanged = (changedResources: readonly URI[]): void => {\n\t// \tfor (const resource of changedResources) {\n\t// \t\tconst markers = this._markerService.read({ resource });\n\n\t// \t\tif (markers.length === 0) {\n\t// \t\t\tconsole.log(`${resource.fsPath}: No diagnostics`);\n\t// \t\t\tcontinue;\n\t// \t\t}\n\n\t// \t\tconsole.log(`Diagnostics for ${resource.fsPath}:`);\n\t// \t\tmarkers.forEach(marker => this._logMarker(marker));\n\t// \t}\n\t// };\n\n\n}\n\nregisterSingleton(IMarkerCheckService, MarkerCheckService, InstantiationType.Eager);\n"
    };

    private BrowserMarkerCheckServiceTs() {
    }

    @Override
    public String path() {
        return "browser/_markerCheckService.ts";
    }

    @Override
    public String sha256() {
        return "4ccd51ad28d8b915b75f6f5b15dfffa71f63f6abdb4bf73c8d6966bc8e2be370";
    }

    @Override
    public int originalByteLength() {
        return 4831;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
