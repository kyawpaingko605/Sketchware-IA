package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class ElectronMainLlmMessageSendLLMMessageTs implements SourceAsset {
    public static final ElectronMainLlmMessageSendLLMMessageTs INSTANCE = new ElectronMainLlmMessageSendLLMMessageTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { SendLLMMessageParams, OnText, OnFinalMessage, OnError } from '../../common/sendLLMMessageTypes.js';\nimport { IMetricsService } from '../../common/metricsService.js';\nimport { displayInfoOfProviderName } from '../../common/voidSettingsTypes.js';\nimport { sendLLMMessageToProviderImplementation } from './sendLLMMessage.impl.js';\n\n\nexport const sendLLMMessage = async ({\n\tmessagesType,\n\tmessages: messages_,\n\tonText: onText_,\n\tonFinalMessage: onFinalMessage_,\n\tonError: onError_,\n\tabortRef: abortRef_,\n\tlogging: { loggingName, loggingExtras },\n\tsettingsOfProvider,\n\tmodelSelection,\n\tmodelSelectionOptions,\n\toverridesOfModel,\n\tchatMode,\n\tseparateSystemMessage,\n\tmcpTools,\n}: SendLLMMessageParams,\n\n\tmetricsService: IMetricsService\n) => {\n\n\n\tconst { providerName, modelName } = modelSelection\n\n\t// only captures number of messages and message \"shape\", no actual code, instructions, prompts, etc\n\tconst captureLLMEvent = (eventId: string, extras?: object) => {\n\n\n\t\tmetricsService.capture(eventId, {\n\t\t\tproviderName,\n\t\t\tmodelName,\n\t\t\tcustomEndpointURL: settingsOfProvider[providerName]?.endpoint,\n\t\t\tnumModelsAtEndpoint: settingsOfProvider[providerName]?.models?.length,\n\t\t\t...messagesType === 'chatMessages' ? {\n\t\t\t\tnumMessages: messages_?.length,\n\t\t\t} : messagesType === 'FIMMessage' ? {\n\t\t\t\tprefixLength: messages_.prefix.length,\n\t\t\t\tsuffixLength: messages_.suffix.length,\n\t\t\t} : {},\n\t\t\t...loggingExtras,\n\t\t\t...extras,\n\t\t})\n\t}\n\tconst submit_time = new Date()\n\n\tlet _fullTextSoFar = ''\n\tlet _aborter: (() => void) | null = null\n\tlet _setAborter = (fn: () => void) => { _aborter = fn }\n\tlet _didAbort = false\n\n\tconst onText: OnText = (params) => {\n\t\tconst { fullText } = params\n\t\tif (_didAbort) return\n\t\tonText_(params)\n\t\t_fullTextSoFar = fullText\n\t}\n\n\tconst onFinalMessage: OnFinalMessage = (params) => {\n\t\tconst { fullText, fullReasoning, toolCall } = params\n\t\tif (_didAbort) return\n\t\tcaptureLLMEvent(`${loggingName} - Received Full Message`, { messageLength: fullText.length, reasoningLength: fullReasoning?.length, duration: new Date().getMilliseconds() - submit_time.getMilliseconds(), toolCallName: toolCall?.name })\n\t\tonFinalMessage_(params)\n\t}\n\n\tconst onError: OnError = ({ message: errorMessage, fullError }) => {\n\t\tif (_didAbort) return\n\t\tconsole.error('sendLLMMessage onError:', errorMessage)\n\n\t\t// handle failed to fetch errors, which give 0 information by design\n\t\tif (errorMessage === 'TypeError: fetch failed')\n\t\t\terrorMessage = `Failed to fetch from ${displayInfoOfProviderName(providerName).title}. This likely means you specified the wrong endpoint in Void's Settings, or your local model provider like Ollama is powered off.`\n\n\t\tcaptureLLMEvent(`${loggingName} - Error`, { error: errorMessage })\n\t\tonError_({ message: errorMessage, fullError })\n\t}\n\n\t// we should NEVER call onAbort internally, only from the outside\n\tconst onAbort = () => {\n\t\tcaptureLLMEvent(`${loggingName} - Abort`, { messageLengthSoFar: _fullTextSoFar.length })\n\t\ttry { _aborter?.() } // aborter sometimes automatically throws an error\n\t\tcatch (e) { }\n\t\t_didAbort = true\n\t}\n\tabortRef_.current = onAbort\n\n\n\tif (messagesType === 'chatMessages')\n\t\tcaptureLLMEvent(`${loggingName} - Sending Message`, {})\n\telse if (messagesType === 'FIMMessage')\n\t\tcaptureLLMEvent(`${loggingName} - Sending FIM`, { prefixLen: messages_?.prefix?.length, suffixLen: messages_?.suffix?.length })\n\n\n\ttry {\n\t\tconst implementation = sendLLMMessageToProviderImplementation[providerName]\n\t\tif (!implementation) {\n\t\t\tonError({ message: `Error: Provider \"${providerName}\" not recognized.`, fullError: null })\n\t\t\treturn\n\t\t}\n\t\tconst { sendFIM, sendChat } = implementation\n\t\tif (messagesType === 'chatMessages') {\n\t\t\tawait sendChat({ messages: messages_, onText, onFinalMessage, onError, settingsOfProvider, modelSelectionOptions, overridesOfModel, modelName, _setAborter, providerName, separateSystemMessage, chatMode, mcpTools })\n\t\t\treturn\n\t\t}\n\t\tif (messagesType === 'FIMMessage') {\n\t\t\tif (sendFIM) {\n\t\t\t\tawait sendFIM({ messages: messages_, onText, onFinalMessage, onError, settingsOfProvider, modelSelectionOptions, overridesOfModel, modelName, _setAborter, providerName, separateSystemMessage })\n\t\t\t\treturn\n\t\t\t}\n\t\t\tonError({ message: `Error running Autocomplete with ${providerName} - ${modelName}.`, fullError: null })\n\t\t\treturn\n\t\t}\n\t\tonError({ message: `Error: Message type \"${messagesType}\" not recognized.`, fullError: null })\n\t\treturn\n\t}\n\n\tcatch (error) {\n\t\tif (error instanceof Error) { onError({ message: error + '', fullError: error }) }\n\t\telse { onError({ message: `Unexpected Error in sendLLMMessage: ${error}`, fullError: error }); }\n\t\t// ; (_aborter as any)?.()\n\t\t// _didAbort = true\n\t}\n\n\n\n}\n\n"
    };

    private ElectronMainLlmMessageSendLLMMessageTs() {
    }

    @Override
    public String path() {
        return "electron-main/llmMessage/sendLLMMessage.ts";
    }

    @Override
    public String sha256() {
        return "da4ef949812e2264a175c42fabdd5971bed108325924eedd153637a1c1f391ae";
    }

    @Override
    public int originalByteLength() {
        return 5001;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
