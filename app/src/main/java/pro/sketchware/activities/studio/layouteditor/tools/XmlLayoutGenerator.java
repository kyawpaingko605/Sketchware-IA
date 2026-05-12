package pro.sketchware.activities.studio.layouteditor.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import pro.sketchware.activities.studio.layouteditor.editor.layouts.EditorLayout;
import pro.sketchware.activities.studio.layouteditor.model.WidgetItem;

public class XmlLayoutGenerator {

    public String generate(EditorLayout editorLayout) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        String rootTag = editorLayout.getRootTag();
        xml.append("<").append(rootTag).append(" xmlns:android=\"http://schemas.android.com/apk/res/android\"");
        if (!"LinearLayout".equals(rootTag) && !rootTag.startsWith("android.")) {
            xml.append("\n    xmlns:app=\"http://schemas.android.com/apk/res-auto\"");
        }
        appendAttributes(xml, editorLayout.getRootAttributes(), true);
        if (editorLayout.getWidgets().isEmpty()) {
            xml.append(" />\n");
            return xml.toString();
        }
        xml.append(">\n\n");
        for (WidgetItem item : editorLayout.getWidgets()) {
            appendWidget(xml, item);
        }
        xml.append("</").append(rootTag).append(">\n");
        return xml.toString();
    }

    private void appendWidget(StringBuilder xml, WidgetItem item) {
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>(item.getAttributes());
        ensureDefaults(item, attributes);
        xml.append("    <").append(item.xmlTag());
        appendAttributes(xml, attributes, false);
        xml.append(" />\n\n");
    }

    private void ensureDefaults(WidgetItem item, LinkedHashMap<String, String> attributes) {
        if (item.id != null && !item.id.trim().isEmpty()) {
            attributes.put("android:id", "@+id/" + item.id);
        }
        putIfMissing(attributes, "android:layout_width", "match_parent");
        if ("ImageView".equals(item.type)) {
            putIfMissing(attributes, "android:layout_height", "96dp");
            putIfMissing(attributes, "android:scaleType", "center");
        } else if ("ListView".equals(item.type) || "WebView".equals(item.type)) {
            putIfMissing(attributes, "android:layout_height", "120dp");
        } else if ("CalendarView".equals(item.type)) {
            putIfMissing(attributes, "android:layout_height", "220dp");
        } else if ("Space".equals(item.type) || "View".equals(item.type)) {
            putIfMissing(attributes, "android:layout_height", "28dp");
        } else {
            putIfMissing(attributes, "android:layout_height", "wrap_content");
        }
        if ("LinearLayout".equals(item.type)) {
            putIfMissing(attributes, "android:orientation", "vertical");
        } else if ("EditText".equals(item.type)) {
            putIfMissing(attributes, "android:hint", item.hint == null ? "Text" : item.hint);
        } else if ("TextView".equals(item.type)
                || "Button".equals(item.type)
                || "CheckBox".equals(item.type)
                || "RadioButton".equals(item.type)
                || "Switch".equals(item.type)) {
            putIfMissing(attributes, "android:text", item.text == null ? item.type : item.text);
        }
    }

    private void appendAttributes(StringBuilder xml, Map<String, String> attributes, boolean root) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getKey().startsWith("xmlns")) {
                continue;
            }
            xml.append(root ? "\n    " : "\n        ")
                    .append(entry.getKey())
                    .append("=\"")
                    .append(escape(entry.getValue()))
                    .append("\"");
        }
    }

    private void putIfMissing(LinkedHashMap<String, String> attributes, String key, String value) {
        if (!attributes.containsKey(key)) {
            attributes.put(key, value);
        }
    }

    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
