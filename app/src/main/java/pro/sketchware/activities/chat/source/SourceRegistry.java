package pro.sketchware.activities.chat.source;

import pro.sketchware.activities.chat.source.assets.BrowserDummyContribTs;
import pro.sketchware.activities.chat.source.assets.BrowserMarkerCheckServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserActionIDsTs;
import pro.sketchware.activities.chat.source.assets.BrowserAiRegexServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserAutocompleteServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserChatThreadServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserContextGatheringServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserConvertToLLMMessageServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserConvertToLLMMessageWorkbenchContribTs;
import pro.sketchware.activities.chat.source.assets.BrowserEditCodeServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserEditCodeServiceInterfaceTs;
import pro.sketchware.activities.chat.source.assets.BrowserExtensionTransferServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserExtensionTransferTypesTs;
import pro.sketchware.activities.chat.source.assets.BrowserFileServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserHelpersFindDiffsTs;
import pro.sketchware.activities.chat.source.assets.BrowserHelperServicesConsistentItemServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserMediaVoidCss;
import pro.sketchware.activities.chat.source.assets.BrowserMetricsPollServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserMiscWokrbenchContribTs;
import pro.sketchware.activities.chat.source.assets.BrowserQuickEditActionsTs;
import pro.sketchware.activities.chat.source.assets.BrowserReactGitignoreFile;
import pro.sketchware.activities.chat.source.assets.BrowserReactBuildJs;
import pro.sketchware.activities.chat.source.assets.BrowserReactREADMEMd;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcDiffIndexTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcMarkdownApplyBlockHoverButtonsTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcMarkdownChatMarkdownRenderTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcQuickEditTsxIndexTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcQuickEditTsxQuickEditTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcQuickEditTsxQuickEditChatTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcSidebarTsxErrorBoundaryTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcSidebarTsxErrorDisplayTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcSidebarTsxIndexTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcSidebarTsxSidebarTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcSidebarTsxSidebarChatTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcSidebarTsxSidebarThreadSelectorTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcStylesCss;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcUtilHelpersTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcUtilInputsTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcUtilMountFnGeneratorTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcUtilServicesTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcUtilUseScrollbarStylesTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidEditorWidgetsTsxIndexTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidEditorWidgetsTsxVoidCommandBarTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidEditorWidgetsTsxVoidSelectionHelperTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidOnboardingIndexTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidOnboardingVoidOnboardingTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidSettingsTsxIndexTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidSettingsTsxModelDropdownTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidSettingsTsxSettingsTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidSettingsTsxWarningBoxTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidTooltipIndexTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactSrcVoidTooltipVoidTooltipTsx;
import pro.sketchware.activities.chat.source.assets.BrowserReactTailwindConfigJs;
import pro.sketchware.activities.chat.source.assets.BrowserReactTsconfigJson;
import pro.sketchware.activities.chat.source.assets.BrowserReactTsupConfigJs;
import pro.sketchware.activities.chat.source.assets.BrowserSidebarActionsTs;
import pro.sketchware.activities.chat.source.assets.BrowserSidebarPaneTs;
import pro.sketchware.activities.chat.source.assets.BrowserTerminalToolServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserToolsServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserTooltipServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserVoidContributionTs;
import pro.sketchware.activities.chat.source.assets.BrowserVoidCommandBarServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserVoidOnboardingServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserVoidSCMServiceTs;
import pro.sketchware.activities.chat.source.assets.BrowserVoidSelectionHelperWidgetTs;
import pro.sketchware.activities.chat.source.assets.BrowserVoidSettingsPaneTs;
import pro.sketchware.activities.chat.source.assets.BrowserVoidUpdateActionsTs;
import pro.sketchware.activities.chat.source.assets.CommonChatThreadServiceTypesTs;
import pro.sketchware.activities.chat.source.assets.CommonDirectoryStrServiceTs;
import pro.sketchware.activities.chat.source.assets.CommonDirectoryStrTypesTs;
import pro.sketchware.activities.chat.source.assets.CommonEditCodeServiceTypesTs;
import pro.sketchware.activities.chat.source.assets.CommonHelpersColorsTs;
import pro.sketchware.activities.chat.source.assets.CommonHelpersExtractCodeFromResultTs;
import pro.sketchware.activities.chat.source.assets.CommonHelpersLanguageHelpersTs;
import pro.sketchware.activities.chat.source.assets.CommonHelpersSystemInfoTs;
import pro.sketchware.activities.chat.source.assets.CommonHelpersUtilTs;
import pro.sketchware.activities.chat.source.assets.CommonMcpServiceTs;
import pro.sketchware.activities.chat.source.assets.CommonMcpServiceTypesTs;
import pro.sketchware.activities.chat.source.assets.CommonMetricsServiceTs;
import pro.sketchware.activities.chat.source.assets.CommonModelCapabilitiesTs;
import pro.sketchware.activities.chat.source.assets.CommonPromptPromptsTs;
import pro.sketchware.activities.chat.source.assets.CommonRefreshModelServiceTs;
import pro.sketchware.activities.chat.source.assets.CommonSendLLMMessageServiceTs;
import pro.sketchware.activities.chat.source.assets.CommonSendLLMMessageTypesTs;
import pro.sketchware.activities.chat.source.assets.CommonStorageKeysTs;
import pro.sketchware.activities.chat.source.assets.CommonToolsServiceTypesTs;
import pro.sketchware.activities.chat.source.assets.CommonVoidModelServiceTs;
import pro.sketchware.activities.chat.source.assets.CommonVoidSCMTypesTs;
import pro.sketchware.activities.chat.source.assets.CommonVoidSettingsServiceTs;
import pro.sketchware.activities.chat.source.assets.CommonVoidSettingsTypesTs;
import pro.sketchware.activities.chat.source.assets.CommonVoidUpdateServiceTs;
import pro.sketchware.activities.chat.source.assets.CommonVoidUpdateServiceTypesTs;
import pro.sketchware.activities.chat.source.assets.ElectronMainLlmMessageExtractGrammarTs;
import pro.sketchware.activities.chat.source.assets.ElectronMainLlmMessageSendLLMMessageImplTs;
import pro.sketchware.activities.chat.source.assets.ElectronMainLlmMessageSendLLMMessageTs;
import pro.sketchware.activities.chat.source.assets.ElectronMainMcpChannelTs;
import pro.sketchware.activities.chat.source.assets.ElectronMainMetricsMainServiceTs;
import pro.sketchware.activities.chat.source.assets.ElectronMainSendLLMMessageChannelTs;
import pro.sketchware.activities.chat.source.assets.ElectronMainVoidSCMMainServiceTs;
import pro.sketchware.activities.chat.source.assets.ElectronMainVoidUpdateMainServiceTs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SourceRegistry {
    public static final List<SourceAsset> ALL = Collections.unmodifiableList(Arrays.asList(
            BrowserDummyContribTs.INSTANCE,
            BrowserMarkerCheckServiceTs.INSTANCE,
            BrowserActionIDsTs.INSTANCE,
            BrowserAiRegexServiceTs.INSTANCE,
            BrowserAutocompleteServiceTs.INSTANCE,
            BrowserChatThreadServiceTs.INSTANCE,
            BrowserContextGatheringServiceTs.INSTANCE,
            BrowserConvertToLLMMessageServiceTs.INSTANCE,
            BrowserConvertToLLMMessageWorkbenchContribTs.INSTANCE,
            BrowserEditCodeServiceTs.INSTANCE,
            BrowserEditCodeServiceInterfaceTs.INSTANCE,
            BrowserExtensionTransferServiceTs.INSTANCE,
            BrowserExtensionTransferTypesTs.INSTANCE,
            BrowserFileServiceTs.INSTANCE,
            BrowserHelpersFindDiffsTs.INSTANCE,
            BrowserHelperServicesConsistentItemServiceTs.INSTANCE,
            BrowserMediaVoidCss.INSTANCE,
            BrowserMetricsPollServiceTs.INSTANCE,
            BrowserMiscWokrbenchContribTs.INSTANCE,
            BrowserQuickEditActionsTs.INSTANCE,
            BrowserReactGitignoreFile.INSTANCE,
            BrowserReactBuildJs.INSTANCE,
            BrowserReactREADMEMd.INSTANCE,
            BrowserReactSrcDiffIndexTsx.INSTANCE,
            BrowserReactSrcMarkdownApplyBlockHoverButtonsTsx.INSTANCE,
            BrowserReactSrcMarkdownChatMarkdownRenderTsx.INSTANCE,
            BrowserReactSrcQuickEditTsxIndexTsx.INSTANCE,
            BrowserReactSrcQuickEditTsxQuickEditTsx.INSTANCE,
            BrowserReactSrcQuickEditTsxQuickEditChatTsx.INSTANCE,
            BrowserReactSrcSidebarTsxErrorBoundaryTsx.INSTANCE,
            BrowserReactSrcSidebarTsxErrorDisplayTsx.INSTANCE,
            BrowserReactSrcSidebarTsxIndexTsx.INSTANCE,
            BrowserReactSrcSidebarTsxSidebarTsx.INSTANCE,
            BrowserReactSrcSidebarTsxSidebarChatTsx.INSTANCE,
            BrowserReactSrcSidebarTsxSidebarThreadSelectorTsx.INSTANCE,
            BrowserReactSrcStylesCss.INSTANCE,
            BrowserReactSrcUtilHelpersTsx.INSTANCE,
            BrowserReactSrcUtilInputsTsx.INSTANCE,
            BrowserReactSrcUtilMountFnGeneratorTsx.INSTANCE,
            BrowserReactSrcUtilServicesTsx.INSTANCE,
            BrowserReactSrcUtilUseScrollbarStylesTsx.INSTANCE,
            BrowserReactSrcVoidEditorWidgetsTsxIndexTsx.INSTANCE,
            BrowserReactSrcVoidEditorWidgetsTsxVoidCommandBarTsx.INSTANCE,
            BrowserReactSrcVoidEditorWidgetsTsxVoidSelectionHelperTsx.INSTANCE,
            BrowserReactSrcVoidOnboardingIndexTsx.INSTANCE,
            BrowserReactSrcVoidOnboardingVoidOnboardingTsx.INSTANCE,
            BrowserReactSrcVoidSettingsTsxIndexTsx.INSTANCE,
            BrowserReactSrcVoidSettingsTsxModelDropdownTsx.INSTANCE,
            BrowserReactSrcVoidSettingsTsxSettingsTsx.INSTANCE,
            BrowserReactSrcVoidSettingsTsxWarningBoxTsx.INSTANCE,
            BrowserReactSrcVoidTooltipIndexTsx.INSTANCE,
            BrowserReactSrcVoidTooltipVoidTooltipTsx.INSTANCE,
            BrowserReactTailwindConfigJs.INSTANCE,
            BrowserReactTsconfigJson.INSTANCE,
            BrowserReactTsupConfigJs.INSTANCE,
            BrowserSidebarActionsTs.INSTANCE,
            BrowserSidebarPaneTs.INSTANCE,
            BrowserTerminalToolServiceTs.INSTANCE,
            BrowserToolsServiceTs.INSTANCE,
            BrowserTooltipServiceTs.INSTANCE,
            BrowserVoidContributionTs.INSTANCE,
            BrowserVoidCommandBarServiceTs.INSTANCE,
            BrowserVoidOnboardingServiceTs.INSTANCE,
            BrowserVoidSCMServiceTs.INSTANCE,
            BrowserVoidSelectionHelperWidgetTs.INSTANCE,
            BrowserVoidSettingsPaneTs.INSTANCE,
            BrowserVoidUpdateActionsTs.INSTANCE,
            CommonChatThreadServiceTypesTs.INSTANCE,
            CommonDirectoryStrServiceTs.INSTANCE,
            CommonDirectoryStrTypesTs.INSTANCE,
            CommonEditCodeServiceTypesTs.INSTANCE,
            CommonHelpersColorsTs.INSTANCE,
            CommonHelpersExtractCodeFromResultTs.INSTANCE,
            CommonHelpersLanguageHelpersTs.INSTANCE,
            CommonHelpersSystemInfoTs.INSTANCE,
            CommonHelpersUtilTs.INSTANCE,
            CommonMcpServiceTs.INSTANCE,
            CommonMcpServiceTypesTs.INSTANCE,
            CommonMetricsServiceTs.INSTANCE,
            CommonModelCapabilitiesTs.INSTANCE,
            CommonPromptPromptsTs.INSTANCE,
            CommonRefreshModelServiceTs.INSTANCE,
            CommonSendLLMMessageServiceTs.INSTANCE,
            CommonSendLLMMessageTypesTs.INSTANCE,
            CommonStorageKeysTs.INSTANCE,
            CommonToolsServiceTypesTs.INSTANCE,
            CommonVoidModelServiceTs.INSTANCE,
            CommonVoidSCMTypesTs.INSTANCE,
            CommonVoidSettingsServiceTs.INSTANCE,
            CommonVoidSettingsTypesTs.INSTANCE,
            CommonVoidUpdateServiceTs.INSTANCE,
            CommonVoidUpdateServiceTypesTs.INSTANCE,
            ElectronMainLlmMessageExtractGrammarTs.INSTANCE,
            ElectronMainLlmMessageSendLLMMessageImplTs.INSTANCE,
            ElectronMainLlmMessageSendLLMMessageTs.INSTANCE,
            ElectronMainMcpChannelTs.INSTANCE,
            ElectronMainMetricsMainServiceTs.INSTANCE,
            ElectronMainSendLLMMessageChannelTs.INSTANCE,
            ElectronMainVoidSCMMainServiceTs.INSTANCE,
            ElectronMainVoidUpdateMainServiceTs.INSTANCE
    ));

    private SourceRegistry() {
    }

    public static SourceAsset findByPath(String path) {
        for (SourceAsset asset : ALL) {
            if (asset.path().equals(path)) {
                return asset;
            }
        }
        return null;
    }
}
