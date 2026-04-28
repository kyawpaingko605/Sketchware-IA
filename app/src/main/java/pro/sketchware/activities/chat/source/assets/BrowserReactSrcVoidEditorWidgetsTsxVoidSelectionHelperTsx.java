package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcVoidEditorWidgetsTsxVoidSelectionHelperTsx implements SourceAsset {
    public static final BrowserReactSrcVoidEditorWidgetsTsxVoidSelectionHelperTsx INSTANCE = new BrowserReactSrcVoidEditorWidgetsTsxVoidSelectionHelperTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\n\nimport { useAccessor, useActiveURI, useIsDark, useSettingsState } from '../util/services.js';\n\nimport '../styles.css'\nimport { VOID_CTRL_K_ACTION_ID, VOID_CTRL_L_ACTION_ID } from '../../../actionIDs.js';\nimport { Circle, MoreVertical } from 'lucide-react';\nimport { useEffect, useState } from 'react';\n\nimport { VoidSelectionHelperProps } from '../../../../../../contrib/void/browser/voidSelectionHelperWidget.js';\nimport { VOID_OPEN_SETTINGS_ACTION_ID } from '../../../voidSettingsPane.js';\n\n\nexport const VoidSelectionHelperMain = (props: VoidSelectionHelperProps) => {\n\n\tconst isDark = useIsDark()\n\n\treturn <div\n\t\tclassName={`@@void-scope ${isDark ? 'dark' : ''}`}\n\t>\n\t\t<VoidSelectionHelper {...props} />\n\t</div>\n}\n\n\n\nconst VoidSelectionHelper = ({ rerenderKey }: VoidSelectionHelperProps) => {\n\n\n\tconst accessor = useAccessor()\n\tconst keybindingService = accessor.get('IKeybindingService')\n\tconst commandService = accessor.get('ICommandService')\n\n\tconst ctrlLKeybind = keybindingService.lookupKeybinding(VOID_CTRL_L_ACTION_ID)\n\tconst ctrlKKeybind = keybindingService.lookupKeybinding(VOID_CTRL_K_ACTION_ID)\n\n\tconst dividerHTML = <div className='w-[0.5px] bg-void-border-3'></div>\n\n\tconst [reactRerenderCount, setReactRerenderKey] = useState(rerenderKey)\n\tconst [clickState, setClickState] = useState<'init' | 'clickedOption' | 'clickedMore'>('init')\n\n\tuseEffect(() => {\n\t\tconst disposable = commandService.onWillExecuteCommand(e => {\n\t\t\tif (e.commandId === VOID_CTRL_L_ACTION_ID || e.commandId === VOID_CTRL_K_ACTION_ID) {\n\t\t\t\tsetClickState('clickedOption')\n\t\t\t}\n\t\t});\n\n\t\treturn () => {\n\t\t\tdisposable.dispose();\n\t\t};\n\t}, [commandService, setClickState]);\n\n\n\t// rerender when the key changes\n\tif (reactRerenderCount !== rerenderKey) {\n\t\tsetReactRerenderKey(rerenderKey)\n\t\tsetClickState('init')\n\t}\n\t// useEffect(() => {\n\t// }, [rerenderKey, reactRerenderCount, setReactRerenderKey, setClickState])\n\n\t// if the user selected an option, close\n\n\n\tif (clickState === 'clickedOption') {\n\t\treturn null\n\t}\n\n\tconst defaultHTML = <>\n\t\t{ctrlLKeybind &&\n\t\t\t<div\n\t\t\t\tclassName='\n\t\t\t\t\tflex items-center px-2 py-1.5\n\t\t\t\t\tcursor-pointer\n\t\t\t\t'\n\t\t\t\tonClick={() => {\n\t\t\t\t\tcommandService.executeCommand(VOID_CTRL_L_ACTION_ID)\n\t\t\t\t\tsetClickState('clickedOption');\n\t\t\t\t}}\n\t\t\t>\n\t\t\t\t<span>Add to Chat</span>\n\t\t\t\t<span className='ml-1 px-1 rounded bg-[var(--vscode-keybindingLabel-background)] text-[var(--vscode-keybindingLabel-foreground)] border border-[var(--vscode-keybindingLabel-border)]'>\n\t\t\t\t\t{ctrlLKeybind.getLabel()}\n\t\t\t\t</span>\n\t\t\t</div>\n\t\t}\n\t\t{ctrlLKeybind && ctrlKKeybind &&\n\t\t\tdividerHTML\n\t\t}\n\t\t{ctrlKKeybind &&\n\t\t\t<div\n\t\t\t\tclassName='\n\t\t\t\t\tflex items-center px-2 py-1.5\n\t\t\t\t\tcursor-pointer\n\t\t\t\t'\n\t\t\t\tonClick={() => {\n\t\t\t\t\tcommandService.executeCommand(VOID_CTRL_K_ACTION_ID)\n\t\t\t\t\tsetClickState('clickedOption');\n\t\t\t\t}}\n\t\t\t>\n\t\t\t\t<span className='ml-1'>Edit Inline</span>\n\t\t\t\t<span className='ml-1 px-1 rounded bg-[var(--vscode-keybindingLabel-background)] text-[var(--vscode-keybindingLabel-foreground)] border border-[var(--vscode-keybindingLabel-border)]'>\n\t\t\t\t\t{ctrlKKeybind.getLabel()}\n\t\t\t\t</span>\n\t\t\t</div>\n\t\t}\n\n\t\t{dividerHTML}\n\n\t\t<div\n\t\t\tclassName='\n\t\t\t\tflex items-center px-0.5\n\t\t\t\tcursor-pointer\n\t\t\t'\n\t\t\tonClick={() => {\n\t\t\t\tsetClickState('clickedMore');\n\t\t\t}}\n\t\t>\n\t\t\t<MoreVertical className=\"w-4\" />\n\t\t</div>\n\t</>\n\n\n\tconst moreOptionsHTML = <>\n\t\t<div\n\t\t\tclassName='\n\t\t\t\tflex items-center px-2 py-1.5\n\t\t\t\tcursor-pointer\n\t\t\t'\n\t\t\tonClick={() => {\n\t\t\t\tcommandService.executeCommand(VOID_OPEN_SETTINGS_ACTION_ID);\n\t\t\t\tsetClickState('clickedOption');\n\t\t\t}}\n\t\t>\n\t\t\tDisable Suggestions?\n\t\t</div>\n\n\t\t{dividerHTML}\n\n\t\t<div\n\t\t\tclassName='\n\t\t\t\tflex items-center px-0.5\n\t\t\t\tcursor-pointer\n\t\t\t'\n\t\t\tonClick={() => {\n\t\t\t\tsetClickState('init');\n\t\t\t}}\n\t\t>\n\t\t\t<MoreVertical className=\"w-4\" />\n\t\t</div>\n\t</>\n\n\treturn <div className='\n\t\tpointer-events-auto select-none\n\t\tz-[1000]\n\t\trounded-sm shadow-md flex flex-nowrap text-nowrap\n\t\tborder border-void-border-3 bg-void-bg-2\n\t\ttransition-all duration-200\n\t'>\n\t\t{clickState === 'init' ? defaultHTML\n\t\t\t: clickState === 'clickedMore' ? moreOptionsHTML\n\t\t\t\t: <></>\n\t\t}\n\t</div>\n}\n"
    };

    private BrowserReactSrcVoidEditorWidgetsTsxVoidSelectionHelperTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/void-editor-widgets-tsx/VoidSelectionHelper.tsx";
    }

    @Override
    public String sha256() {
        return "f714a17dd1f0a0abf9bccef9b4fb954eeeb5d3f5220b8e4b44360a1d1006ddf5";
    }

    @Override
    public int originalByteLength() {
        return 4440;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
