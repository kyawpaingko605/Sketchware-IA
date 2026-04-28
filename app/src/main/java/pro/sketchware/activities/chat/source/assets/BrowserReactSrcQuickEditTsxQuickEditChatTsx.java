package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcQuickEditTsxQuickEditChatTsx implements SourceAsset {
    public static final BrowserReactSrcQuickEditTsxQuickEditChatTsx INSTANCE = new BrowserReactSrcQuickEditTsxQuickEditChatTsx();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport React, { useCallback, useEffect, useRef, useState } from 'react';\nimport { useSettingsState, useAccessor, useCtrlKZoneStreamingState } from '../util/services.js';\nimport { TextAreaFns, VoidInputBox2 } from '../util/inputs.js';\nimport { QuickEditPropsType } from '../../../quickEditActions.js';\nimport { ButtonStop, ButtonSubmit, IconX, VoidChatArea } from '../sidebar-tsx/SidebarChat.js';\nimport { VOID_CTRL_K_ACTION_ID } from '../../../actionIDs.js';\nimport { useRefState } from '../util/helpers.js';\nimport { isFeatureNameDisabled } from '../../../../../../../workbench/contrib/void/common/voidSettingsTypes.js';\n\n\n\n\nexport const QuickEditChat = ({\n\tdiffareaid,\n\tonChangeHeight,\n\tonChangeText: onChangeText_,\n\ttextAreaRef: textAreaRef_,\n\tinitText\n}: QuickEditPropsType) => {\n\n\tconst accessor = useAccessor()\n\tconst editCodeService = accessor.get('IEditCodeService')\n\tconst sizerRef = useRef<HTMLDivElement | null>(null)\n\tconst textAreaRef = useRef<HTMLTextAreaElement | null>(null)\n\tconst textAreaFnsRef = useRef<TextAreaFns | null>(null)\n\n\tuseEffect(() => {\n\t\tconst inputContainer = sizerRef.current\n\t\tif (!inputContainer) return;\n\t\t// only observing 1 element\n\t\tlet resizeObserver: ResizeObserver | undefined\n\t\tresizeObserver = new ResizeObserver((entries) => {\n\t\t\tconst height = entries[0].borderBoxSize[0].blockSize\n\t\t\tonChangeHeight(height)\n\t\t})\n\t\tresizeObserver.observe(inputContainer);\n\t\treturn () => { resizeObserver?.disconnect(); };\n\t}, [onChangeHeight]);\n\n\n\tconst settingsState = useSettingsState()\n\n\t// state of current message\n\tconst [instructionsAreEmpty, setInstructionsAreEmpty] = useState(!(initText ?? '')) // the user's instructions\n\tconst isDisabled = instructionsAreEmpty || !!isFeatureNameDisabled('Ctrl+K', settingsState)\n\n\n\tconst [isStreamingRef, setIsStreamingRef] = useRefState(editCodeService.isCtrlKZoneStreaming({ diffareaid }))\n\tuseCtrlKZoneStreamingState(useCallback((diffareaid2, isStreaming) => {\n\t\tif (diffareaid !== diffareaid2) return\n\t\tsetIsStreamingRef(isStreaming)\n\t}, [diffareaid, setIsStreamingRef]))\n\n\tconst loadingIcon = <div\n\t\tclassName=\"@@codicon @@codicon-loading @@codicon-modifier-spin @@codicon-no-default-spin text-void-fg-3\"\n\t/>\n\n\tconst onSubmit = useCallback(async () => {\n\t\tif (isDisabled) return\n\t\tif (isStreamingRef.current) return\n\t\ttextAreaFnsRef.current?.disable()\n\n\t\tconst opts = {\n\t\t\tfrom: 'QuickEdit',\n\t\t\tdiffareaid,\n\t\t\tstartBehavior: 'keep-conflicts',\n\t\t} as const\n\n\t\tawait editCodeService.callBeforeApplyOrEdit(opts)\n\t\tconst [newApplyingUri, applyDonePromise] = editCodeService.startApplying(opts) ?? []\n\t\t// catch any errors by interrupting the stream\n\t\tapplyDonePromise?.catch(e => { if (newApplyingUri) editCodeService.interruptCtrlKStreaming({ diffareaid }) })\n\n\n\t}, [isStreamingRef, isDisabled, editCodeService, diffareaid])\n\n\tconst onInterrupt = useCallback(() => {\n\t\tif (!isStreamingRef.current) return\n\t\teditCodeService.interruptCtrlKStreaming({ diffareaid })\n\t\ttextAreaFnsRef.current?.enable()\n\t}, [isStreamingRef, editCodeService])\n\n\n\tconst onX = useCallback(() => {\n\t\tonInterrupt()\n\t\teditCodeService.removeCtrlKZone({ diffareaid })\n\t}, [editCodeService, diffareaid])\n\n\tconst keybindingString = accessor.get('IKeybindingService').lookupKeybinding(VOID_CTRL_K_ACTION_ID)?.getLabel()\n\n\tconst chatAreaRef = useRef<HTMLDivElement | null>(null)\n\treturn <div ref={sizerRef} style={{ maxWidth: 450 }} className={`py-2 w-full`}>\n\t\t<VoidChatArea\n\t\t\tfeatureName='Ctrl+K'\n\t\t\tdivRef={chatAreaRef}\n\t\t\tonSubmit={onSubmit}\n\t\t\tonAbort={onInterrupt}\n\t\t\tonClose={onX}\n\t\t\tisStreaming={isStreamingRef.current}\n\t\t\tloadingIcon={loadingIcon}\n\t\t\tisDisabled={isDisabled}\n\t\t\tonClickAnywhere={() => { textAreaRef.current?.focus() }}\n\t\t>\n\t\t\t<VoidInputBox2\n\t\t\t\tclassName='px-1'\n\t\t\t\tinitValue={initText}\n\t\t\t\tref={useCallback((r: HTMLTextAreaElement | null) => {\n\t\t\t\t\ttextAreaRef.current = r\n\t\t\t\t\ttextAreaRef_(r)\n\t\t\t\t\tr?.addEventListener('keydown', (e) => {\n\t\t\t\t\t\tif (e.key === 'Escape')\n\t\t\t\t\t\t\tonX()\n\t\t\t\t\t})\n\t\t\t\t}, [textAreaRef_, onX])}\n\t\t\t\tfnsRef={textAreaFnsRef}\n\t\t\t\tplaceholder=\"Enter instructions...\"\n\t\t\t\tonChangeText={useCallback((newStr: string) => {\n\t\t\t\t\tsetInstructionsAreEmpty(!newStr)\n\t\t\t\t\tonChangeText_(newStr)\n\t\t\t\t}, [onChangeText_])}\n\t\t\t\tonKeyDown={(e) => {\n\t\t\t\t\tif (e.key === 'Enter' && !e.shiftKey) {\n\t\t\t\t\t\tonSubmit()\n\t\t\t\t\t\treturn\n\t\t\t\t\t}\n\t\t\t\t}}\n\t\t\t\tmultiline={true}\n\t\t\t/>\n\t\t</VoidChatArea>\n\t</div>\n\n\n}\n"
    };

    private BrowserReactSrcQuickEditTsxQuickEditChatTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/quick-edit-tsx/QuickEditChat.tsx";
    }

    @Override
    public String sha256() {
        return "3a4244b3c54a872c053a6d9338fc29f7ef575def0744f51b39e3b50b7c3cf1cc";
    }

    @Override
    public int originalByteLength() {
        return 4701;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
