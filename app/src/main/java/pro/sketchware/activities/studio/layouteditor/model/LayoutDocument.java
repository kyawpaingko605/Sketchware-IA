package pro.sketchware.activities.studio.layouteditor.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class LayoutDocument {
    public String rootTag = "LinearLayout";
    public final LinkedHashMap<String, String> rootAttributes = new LinkedHashMap<>();
    public final List<WidgetItem> children = new ArrayList<>();

    public LayoutDocument() {
        rootAttributes.put("android:layout_width", "match_parent");
        rootAttributes.put("android:layout_height", "match_parent");
        rootAttributes.put("android:orientation", "vertical");
    }
}
