package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class CommonEditCodeServiceTypesTs implements SourceAsset {
    public static final CommonEditCodeServiceTypesTs INSTANCE = new CommonEditCodeServiceTypesTs();

    private static final String[] CHUNKS = new String[] {
            "/*--------------------------------------------------------------------------------------\n *  Copyright 2025 Glass Devtools, Inc. All rights reserved.\n *  Licensed under the Apache License, Version 2.0. See LICENSE.txt for more information.\n *--------------------------------------------------------------------------------------*/\n\nimport { URI } from '../../../../base/common/uri.js';\n\nexport type ComputedDiff = {\n\ttype: 'edit';\n\toriginalCode: string;\n\toriginalStartLine: number;\n\toriginalEndLine: number;\n\tcode: string;\n\tstartLine: number; // 1-indexed\n\tendLine: number;\n} | {\n\ttype: 'insertion';\n\t// originalCode: string;\n\toriginalStartLine: number; // insertion starts on column 0 of this\n\t// originalEndLine: number;\n\tcode: string;\n\tstartLine: number;\n\tendLine: number;\n} | {\n\ttype: 'deletion';\n\toriginalCode: string;\n\toriginalStartLine: number;\n\toriginalEndLine: number;\n\t// code: string;\n\tstartLine: number; // deletion starts on column 0 of this\n\t// endLine: number;\n}\n\n// ---------- Diff types ----------\n\nexport type CommonZoneProps = {\n\tdiffareaid: number;\n\tstartLine: number;\n\tendLine: number;\n\n\t_URI: URI; // typically we get the URI from model\n\n}\n\n\nexport type CtrlKZone = {\n\ttype: 'CtrlKZone';\n\toriginalCode?: undefined;\n\n\teditorId: string; // the editor the input lives on\n\n\t// _ means anything we don't include if we clone it\n\t_mountInfo: null | {\n\t\ttextAreaRef: { current: HTMLTextAreaElement | null }\n\t\tdispose: () => void;\n\t\trefresh: () => void;\n\t}\n\t_linkedStreamingDiffZone: number | null; // diffareaid of the diffZone currently streaming here\n\t_removeStylesFns: Set<Function> // these don't remove diffs or this diffArea, only their styles\n} & CommonZoneProps\n\n\nexport type TrackingZone<T> = {\n\ttype: 'TrackingZone';\n\tmetadata: T;\n\toriginalCode?: undefined;\n\teditorId?: undefined;\n\t_removeStylesFns?: undefined;\n} & CommonZoneProps\n\n\n// called DiffArea for historical purposes, we can rename to something like TextRegion if we want\nexport type DiffArea = CtrlKZone | DiffZone | TrackingZone<any>\n\n\nexport type Diff = {\n\tdiffid: number;\n\tdiffareaid: number; // the diff area this diff belongs to, \"computed\"\n} & ComputedDiff\n\n\nexport type DiffZone = {\n\ttype: 'DiffZone',\n\toriginalCode: string;\n\t_diffOfId: Record<string, Diff>; // diffid -> diff in this DiffArea\n\t_streamState: {\n\t\tisStreaming: true;\n\t\tstreamRequestIdRef: { current: string | null };\n\t\tline: number;\n\t} | {\n\t\tisStreaming: false;\n\t\tstreamRequestIdRef?: undefined;\n\t\tline?: undefined;\n\t};\n\teditorId?: undefined;\n\tlinkedStreamingDiffZone?: undefined;\n\t_removeStylesFns: Set<Function> // these don't remove diffs or this diffArea, only their styles\n} & CommonZoneProps\n\n\nexport const diffAreaSnapshotKeys = [\n\t'type',\n\t'diffareaid',\n\t'originalCode',\n\t'startLine',\n\t'endLine',\n\t'editorId',\n\n] as const satisfies (keyof DiffArea)[]\n\n\n\nexport type DiffAreaSnapshotEntry<DiffAreaType extends DiffArea = DiffArea> = Pick<DiffAreaType, typeof diffAreaSnapshotKeys[number]>\n\nexport type VoidFileSnapshot = {\n\tsnapshottedDiffAreaOfId: Record<string, DiffAreaSnapshotEntry>;\n\tentireFileCode: string;\n}\n\n"
    };

    private CommonEditCodeServiceTypesTs() {
    }

    @Override
    public String path() {
        return "common/editCodeServiceTypes.ts";
    }

    @Override
    public String sha256() {
        return "c5c01b03144380ff7e92f69717e3794b3cbd334f6758f6bdc658fee6dcfd0841";
    }

    @Override
    public int originalByteLength() {
        return 3080;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
