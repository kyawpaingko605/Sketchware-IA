package pro.sketchware.activities.studio.layouteditor.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class WidgetItem {
    public String type;
    public String id;
    public String text;
    public String hint;
    public final LinkedHashMap<String, String> attributes = new LinkedHashMap<>();

    public WidgetItem(String type) {
        this.type = normalize(type);
    }

    public static String normalize(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "View";
        }
        String value = type.trim();
        String lower = value.toLowerCase();
        if (lower.contains("textview")) {
            return "TextView";
        }
        if (lower.contains("edittext")) {
            return "EditText";
        }
        if (lower.contains("imageview")) {
            return "ImageView";
        }
        if (lower.contains("linearlayout")) {
            return "LinearLayout";
        }
        if (lower.contains("checkbox")) {
            return "CheckBox";
        }
        if (lower.contains("radiobutton")) {
            return "RadioButton";
        }
        if (lower.equals("switch") || lower.endsWith(".switch")) {
            return "Switch";
        }
        if (lower.contains("seekbar")) {
            return "SeekBar";
        }
        if (lower.contains("progressbar")) {
            return "ProgressBar";
        }
        if (lower.contains("spinner")) {
            return "Spinner";
        }
        if (lower.contains("listview")) {
            return "ListView";
        }
        if (lower.contains("webview")) {
            return "WebView";
        }
        if (lower.contains("calendarview")) {
            return "CalendarView";
        }
        if (lower.contains("button")) {
            return "Button";
        }
        if ("space".equals(lower)) {
            return "Space";
        }
        if ("view".equals(lower)) {
            return "View";
        }
        int dot = value.lastIndexOf('.');
        return dot >= 0 && dot < value.length() - 1 ? value.substring(dot + 1) : value;
    }

    public String xmlTag() {
        return "Space".equals(type) ? "View" : type;
    }

    public void setAttribute(String name, String value) {
        if (name != null && value != null) {
            attributes.put(name, value);
        }
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
