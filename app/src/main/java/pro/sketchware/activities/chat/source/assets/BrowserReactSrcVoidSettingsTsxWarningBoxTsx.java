package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcVoidSettingsTsxWarningBoxTsx implements SourceAsset {
    public static final BrowserReactSrcVoidSettingsTsxWarningBoxTsx INSTANCE = new BrowserReactSrcVoidSettingsTsxWarningBoxTsx();

    private static final String[] CHUNKS = new String[] {
            "import { IconWarning } from '../sidebar-tsx/SidebarChat.js';\n\n\nexport const WarningBox = ({ text, onClick, className }: { text: string; onClick?: () => void; className?: string }) => {\n\n\treturn <div\n\t\tclassName={`\n\t\t\ttext-void-warning brightness-90 opacity-90 w-fit\n\t\t\ttext-xs text-ellipsis\n\t\t\t${onClick ? `hover:brightness-75 transition-all duration-200 cursor-pointer` : ''}\n\t\t\tflex items-center flex-nowrap\n\t\t\t${className}\n\t\t`}\n\t\tonClick={onClick}\n\t>\n\t\t<IconWarning\n\t\t\tsize={14}\n\t\t\tclassName='mr-1 flex-shrink-0'\n\t\t/>\n\t\t<span>{text}</span>\n\t</div>\n\t// return <VoidSelectBox\n\t// \toptions={[{ text: 'Please add a model!', value: null }]}\n\t// \tonChangeSelection={() => { }}\n\t// />\n}\n"
    };

    private BrowserReactSrcVoidSettingsTsxWarningBoxTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/void-settings-tsx/WarningBox.tsx";
    }

    @Override
    public String sha256() {
        return "fbda13c842a04b7f1963ef8c89cd329eac0c1954580fff1f536fe2c94e29b16c";
    }

    @Override
    public int originalByteLength() {
        return 683;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
