package pro.sketchware.activities.studio.layouteditor.tools;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

import pro.sketchware.activities.studio.layouteditor.model.LayoutDocument;
import pro.sketchware.activities.studio.layouteditor.model.WidgetItem;

public class XmlLayoutParser {

    public LayoutDocument parse(String xml) {
        LayoutDocument document = new LayoutDocument();
        if (xml == null || xml.trim().isEmpty()) {
            return document;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xml));
            boolean rootRead = false;
            int rootDepth = -1;
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (!rootRead) {
                        rootRead = true;
                        rootDepth = parser.getDepth();
                        document.rootTag = parser.getName();
                        document.rootAttributes.clear();
                        readAttributes(parser, document.rootAttributes);
                    } else if (parser.getDepth() == rootDepth + 1) {
                        WidgetItem item = new WidgetItem(parser.getName());
                        readAttributes(parser, item.attributes);
                        item.id = cleanId(item.getAttribute("android:id"));
                        item.text = item.getAttribute("android:text");
                        item.hint = item.getAttribute("android:hint");
                        document.children.add(item);
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception ignored) {
            return new LayoutDocument();
        }
        return document;
    }

    private void readAttributes(XmlPullParser parser, java.util.Map<String, String> target) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeName(i);
            if (name == null || name.startsWith("xmlns")) {
                continue;
            }
            if (parser.getAttributePrefix(i) != null && !parser.getAttributePrefix(i).isEmpty()) {
                name = parser.getAttributePrefix(i) + ":" + name;
            } else if (!name.contains(":")) {
                name = "android:" + name;
            }
            target.put(name, parser.getAttributeValue(i));
        }
    }

    private String cleanId(String id) {
        if (id == null) {
            return null;
        }
        int slash = id.lastIndexOf('/');
        return slash >= 0 && slash < id.length() - 1 ? id.substring(slash + 1) : id;
    }
}
