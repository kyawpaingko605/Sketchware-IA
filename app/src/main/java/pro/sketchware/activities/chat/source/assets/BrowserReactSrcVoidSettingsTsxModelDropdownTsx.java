package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcVoidSettingsTsxModelDropdownTsx implements SourceAsset {
    public static final BrowserReactSrcVoidSettingsTsxModelDropdownTsx INSTANCE = new BrowserReactSrcVoidSettingsTsxModelDropdownTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { useCallback, useEffect, useMemo, useRef, useState } from 'react'\nimport { FeatureName, featureNames, isFeatureNameDisabled, ModelSelection, modelSelectionsEqual, ProviderName, providerNames, SettingsOfProvider } from '../../../../../../../workbench/contrib/void/common/voidSettingsTypes.js'\nimport { useSettingsState, useRefreshModelState, useAccessor } from '../util/services.js'\nimport { _VoidSelectBox, VoidCustomDropdownBox } from '../util/inputs.js'\nimport { SelectBox } from '../../../../../../../base/browser/ui/selectBox/selectBox.js'\nimport { IconWarning } from '../sidebar-tsx/SidebarChat.js'\nimport { VOID_OPEN_SETTINGS_ACTION_ID, VOID_TOGGLE_SETTINGS_ACTION_ID } from '../../../voidSettingsPane.js'\nimport { modelFilterOfFeatureName, ModelOption } from '../../../../../../../workbench/contrib/void/common/voidSettingsService.js'\nimport { WarningBox } from './WarningBox.js'\nimport ErrorBoundary from '../sidebar-tsx/ErrorBoundary.js'\n\nconst optionsEqual = (m1: ModelOption[], m2: ModelOption[]) => {\n\tif (m1.length !== m2.length) return false\n\tfor (let i = 0; i < m1.length; i++) {\n\t\tif (!modelSelectionsEqual(m1[i].selection, m2[i].selection)) return false\n\t}\n\treturn true\n}\n\nconst ModelSelectBox = ({ options, featureName, className }: { options: ModelOption[], featureName: FeatureName, className: string }) => {\n\tconst accessor = useAccessor()\n\tconst voidSettingsService = accessor.get('IVoidSettingsService')\n\n\tconst selection = voidSettingsService.state.modelSelectionOfFeature[featureName]\n\tconst selectedOption = selection ? voidSettingsService.state._modelOptions.find(v => modelSelectionsEqual(v.selection, selection))! : options[0]\n\n\tconst onChangeOption = useCallback((newOption: ModelOption) => {\n\t\tvoidSettingsService.setModelSelectionOfFeature(featureName, newOption.selection)\n\t}, [voidSettingsService, featureName])\n\n\treturn <VoidCustomDropdownBox\n\t\toptions={options}\n\t\tselectedOption={selectedOption}\n\t\tonChangeOption={onChangeOption}\n\t\tgetOptionDisplayName={(option) => option.selection.modelName}\n\t\tgetOptionDropdownName={(option) => option.selection.modelName}\n\t\tgetOptionDropdownDetail={(option) => option.selection.providerName}\n\t\tgetOptionsEqual={(a, b) => optionsEqual([a], [b])}\n\t\tclassName={className}\n\t\tmatchInputWidth={false}\n\t/>\n}\n\n\nconst MemoizedModelDropdown = ({ featureName, className }: { featureName: FeatureName, className: string }) => {\n\tconst settingsState = useSettingsState()\n\tconst oldOptionsRef = useRef<ModelOption[]>([])\n\tconst [memoizedOptions, setMemoizedOptions] = useState(oldOptionsRef.current)\n\n\tconst { filter, emptyMessage } = modelFilterOfFeatureName[featureName]\n\n\tuseEffect(() => {\n\t\tconst oldOptions = oldOptionsRef.current\n\t\tconst newOptions = settingsState._modelOptions.filter((o) => filter(o.selection, { chatMode: settingsState.globalSettings.chatMode, overridesOfModel: settingsState.overridesOfModel }))\n\n\t\tif (!optionsEqual(oldOptions, newOptions)) {\n\t\t\tsetMemoizedOptions(newOptions)\n\t\t}\n\t\toldOptionsRef.current = newOptions\n\t}, [settingsState._modelOptions, filter])\n\n\tif (memoizedOptions.length === 0) { // Pretty sure this will never be reached unless filter is enabled\n\t\treturn <WarningBox text={emptyMessage?.message || 'No models available'} />\n\t}\n\n\treturn <ModelSelectBox featureName={featureName} options={memoizedOptions} className={className} />\n\n}\n\nexport const ModelDropdown = ({ featureName, className }: { featureName: FeatureName, className: string }) => {\n\tconst settingsState = useSettingsState()\n\n\tconst accessor = useAccessor()\n\tconst commandService = accessor.get('ICommandService')\n\n\tconst openSettings = () => { commandService.executeCommand(VOID_OPEN_SETTINGS_ACTION_ID); };\n\n\n\tconst { emptyMessage } = modelFilterOfFeatureName[featureName]\n\n\tconst isDisabled = isFeatureNameDisabled(featureName, settingsState)\n\tif (isDisabled)\n\t\treturn <WarningBox onClick={openSettings} text={\n\t\t\temptyMessage && emptyMessage.priority === 'always' ? emptyMessage.message :\n\t\t\t\tisDisabled === 'needToEnableModel' ? 'Enable a model'\n\t\t\t\t\t: isDisabled === 'addModel' ? 'Add a model'\n\t\t\t\t\t\t: (isDisabled === 'addProvider' || isDisabled === 'notFilledIn' || isDisabled === 'providerNotAutoDetected') ? 'Provider required'\n\t\t\t\t\t\t\t: 'Provider required'\n\t\t} />\n\n\treturn <ErrorBoundary>\n\t\t<MemoizedModelDropdown featureName={featureName} className={className} />\n\t</ErrorBoundary>\n}\n"
    };

    private BrowserReactSrcVoidSettingsTsxModelDropdownTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/void-settings-tsx/ModelDropdown.tsx";
    }

    @Override
    public String sha256() {
        return "3158b47a5f1fecbea6b0949cfd625b09fcda99a7733fa77fc09e8cff9a2586be";
    }

    @Override
    public int originalByteLength() {
        return 4703;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
