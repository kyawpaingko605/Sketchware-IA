package pro.sketchware.activities.studio;

import android.content.ClipData;
import android.os.Bundle;
import android.text.InputType;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class StudioLayoutEditorActivity extends BaseAppCompatActivity {

    public static final String EXTRA_SC_ID = "sc_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_FILE_PATH = "file_path";
    public static final String EXTRA_XML = "xml";

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final int MENU_SAVE = 1;
    private static final int MENU_DELETE = 2;
    private static final int MENU_SOURCE = 3;

    private final List<WidgetSpec> widgetSpecs = new ArrayList<>();
    private LinearLayout paletteList;
    private LinearLayout canvas;
    private TextView emptyState;
    private TextView status;
    private View selectedWidget;
    private File layoutFile;
    private int generatedIdCounter = 1;
    private boolean dirty;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_studio_layout_editor);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String filePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        layoutFile = filePath == null ? null : new File(filePath);

        MaterialToolbar toolbar = findViewById(R.id.layout_editor_toolbar);
        toolbar.setTitle(R.string.studio_layout_editor_title);
        toolbar.setSubtitle(title == null ? "" : title);
        setupToolbar(toolbar);

        paletteList = findViewById(R.id.layout_editor_palette_list);
        canvas = findViewById(R.id.layout_editor_canvas);
        emptyState = findViewById(R.id.layout_editor_empty);
        status = findViewById(R.id.layout_editor_status);

        definePalette();
        setupPalette();
        setupDropTarget(findViewById(R.id.layout_editor_canvas_holder));
        setupDropTarget(canvas);
        setupDropTarget(emptyState);
        loadInitialXml();
        updateEmptyState();
    }

    private void setupToolbar(MaterialToolbar toolbar) {
        Menu menu = toolbar.getMenu();
        menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.studio_layout_editor_delete)
                .setIcon(R.drawable.ic_mtrl_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_SOURCE, Menu.NONE, R.string.studio_layout_editor_source)
                .setIcon(R.drawable.ic_mtrl_code)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_SAVE, Menu.NONE, R.string.studio_action_save)
                .setIcon(R.drawable.ic_mtrl_save)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_SAVE) {
                saveLayout();
                return true;
            }
            if (item.getItemId() == MENU_DELETE) {
                deleteSelectedWidget();
                return true;
            }
            if (item.getItemId() == MENU_SOURCE) {
                showSource();
                return true;
            }
            return false;
        });
    }

    private void definePalette() {
        widgetSpecs.add(new WidgetSpec("TextView", R.string.studio_widget_textview, R.drawable.ic_mtrl_formattext));
        widgetSpecs.add(new WidgetSpec("Button", R.string.studio_widget_button, R.drawable.ic_mtrl_button_click));
        widgetSpecs.add(new WidgetSpec("EditText", R.string.studio_widget_edittext, R.drawable.ic_mtrl_edittext));
        widgetSpecs.add(new WidgetSpec("ImageView", R.string.studio_widget_imageview, R.drawable.ic_mtrl_image));
        widgetSpecs.add(new WidgetSpec("LinearLayout", R.string.studio_widget_linear_layout, R.drawable.ic_mtrl_view_vertical));
        widgetSpecs.add(new WidgetSpec("Space", R.string.studio_widget_space, R.drawable.ic_mtrl_drag));
    }

    private void setupPalette() {
        for (WidgetSpec spec : widgetSpecs) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(android.view.Gravity.CENTER);
            item.setPadding(dp(6), dp(10), dp(6), dp(10));
            item.setTag(spec.tag);

            ImageView icon = new ImageView(this);
            icon.setImageResource(spec.iconRes);
            icon.setColorFilter(resolveColor(android.R.attr.textColorSecondary));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
            item.addView(icon, iconParams);

            TextView label = new TextView(this);
            label.setGravity(android.view.Gravity.CENTER);
            label.setText(spec.labelRes);
            label.setTextColor(resolveColor(android.R.attr.textColorPrimary));
            label.setTextSize(11);
            label.setSingleLine(false);
            label.setPadding(0, dp(5), 0, 0);
            item.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            item.setOnClickListener(v -> addPaletteWidget((String) v.getTag()));
            item.setOnLongClickListener(v -> {
                String tag = (String) v.getTag();
                ClipData data = ClipData.newPlainText("widget", tag);
                return v.startDragAndDrop(data, new View.DragShadowBuilder(v), tag, 0);
            });
            paletteList.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void setupDropTarget(View view) {
        view.setOnDragListener((target, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                ClipData data = event.getClipData();
                if (data != null && data.getItemCount() > 0 && data.getItemAt(0).getText() != null) {
                    addPaletteWidget(data.getItemAt(0).getText().toString());
                }
            }
            return true;
        });
    }

    private void loadInitialXml() {
        String xml = getIntent().getStringExtra(EXTRA_XML);
        if ((xml == null || xml.trim().isEmpty()) && layoutFile != null && layoutFile.isFile()) {
            xml = FileUtil.readFile(layoutFile.getAbsolutePath());
        }
        if (xml == null || xml.trim().isEmpty()) {
            return;
        }
        try {
            parseLayoutXml(xml);
            dirty = false;
            status.setText(R.string.studio_layout_editor_ready);
        } catch (Exception e) {
            status.setText(R.string.studio_layout_editor_parse_failed);
        }
    }

    private void parseLayoutXml(String xml) throws XmlPullParserException, IOException {
        XmlPullParser parser = android.util.Xml.newPullParser();
        parser.setInput(new StringReader(xml));
        String rootTag = null;
        WidgetState rootState = null;
        int rootDepth = -1;
        List<WidgetState> states = new ArrayList<>();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = normalizeTag(parser.getName());
                if (rootTag == null) {
                    rootTag = tag;
                    rootDepth = parser.getDepth();
                    rootState = readWidgetState(parser, tag);
                } else if (parser.getDepth() == rootDepth + 1 && isSupportedWidget(tag)) {
                    states.add(readWidgetState(parser, tag));
                }
            }
            eventType = parser.next();
        }

        if (states.isEmpty() && rootState != null && isSupportedWidget(rootTag) && !"LinearLayout".equals(rootTag)) {
            states.add(rootState);
        }
        for (WidgetState state : states) {
            addWidget(state, false);
        }
    }

    private WidgetState readWidgetState(XmlPullParser parser, String tag) {
        WidgetState state = new WidgetState(tag);
        state.id = cleanId(parser.getAttributeValue(ANDROID_NS, "id"));
        state.text = parser.getAttributeValue(ANDROID_NS, "text");
        state.hint = parser.getAttributeValue(ANDROID_NS, "hint");
        if (state.id == null || state.id.trim().isEmpty()) {
            state.id = nextViewId(tag);
        }
        if (state.text == null || state.text.trim().isEmpty()) {
            state.text = defaultText(tag);
        }
        if (state.hint == null || state.hint.trim().isEmpty()) {
            state.hint = "Text";
        }
        return state;
    }

    private void addPaletteWidget(String tag) {
        addWidget(new WidgetState(tag), true);
    }

    private void addWidget(WidgetState state, boolean markDirty) {
        if (state.id == null || state.id.trim().isEmpty()) {
            state.id = nextViewId(state.tag);
        }
        if (state.text == null || state.text.trim().isEmpty()) {
            state.text = defaultText(state.tag);
        }
        if (state.hint == null || state.hint.trim().isEmpty()) {
            state.hint = "Text";
        }
        View view = createWidgetView(state);
        view.setTag(state);
        view.setOnClickListener(v -> selectWidget(v));
        LinearLayout.LayoutParams params = widgetLayoutParams(state.tag);
        canvas.addView(view, params);
        selectWidget(view);
        if (markDirty) {
            dirty = true;
            status.setText(state.tag + " added");
        }
        updateEmptyState();
    }

    private View createWidgetView(WidgetState state) {
        if ("Button".equals(state.tag)) {
            Button button = new Button(this);
            button.setText(state.text);
            button.setAllCaps(false);
            return button;
        }
        if ("EditText".equals(state.tag)) {
            EditText editText = new EditText(this);
            editText.setHint(state.hint);
            editText.setSingleLine(true);
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            return editText;
        }
        if ("ImageView".equals(state.tag)) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(R.drawable.ic_mtrl_image);
            imageView.setColorFilter(resolveColor(android.R.attr.textColorSecondary));
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setBackgroundColor(0xFFECEFF1);
            return imageView;
        }
        if ("LinearLayout".equals(state.tag)) {
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp(10), dp(10), dp(10), dp(10));
            container.setBackgroundColor(0x11FFFFFF);
            TextView label = new TextView(this);
            label.setText("LinearLayout");
            label.setTextColor(resolveColor(android.R.attr.textColorSecondary));
            label.setTextSize(13);
            container.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return container;
        }
        if ("Space".equals(state.tag) || "View".equals(state.tag)) {
            Space space = new Space(this);
            space.setBackgroundColor(0xFFE0E0E0);
            return space;
        }
        TextView textView = new TextView(this);
        textView.setText(state.text);
        textView.setTextColor(resolveColor(android.R.attr.textColorPrimary));
        textView.setTextSize(16);
        textView.setGravity(android.view.Gravity.CENTER_VERTICAL);
        textView.setMinHeight(dp(42));
        textView.setPadding(dp(8), 0, dp(8), 0);
        return textView;
    }

    private LinearLayout.LayoutParams widgetLayoutParams(String tag) {
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if ("ImageView".equals(tag)) {
            height = dp(96);
        } else if ("Space".equals(tag) || "View".equals(tag)) {
            height = dp(28);
        } else if ("LinearLayout".equals(tag)) {
            height = dp(72);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        params.setMargins(dp(6), dp(5), dp(6), dp(5));
        return params;
    }

    private void selectWidget(View view) {
        if (selectedWidget != null) {
            selectedWidget.setAlpha(1f);
        }
        selectedWidget = view;
        selectedWidget.setAlpha(0.72f);
        Object tag = view.getTag();
        if (tag instanceof WidgetState) {
            WidgetState state = (WidgetState) tag;
            status.setText(state.tag + "  " + state.id);
        }
    }

    private void deleteSelectedWidget() {
        if (selectedWidget == null) {
            SketchwareUtil.toast(getString(R.string.studio_layout_editor_no_selection));
            return;
        }
        canvas.removeView(selectedWidget);
        selectedWidget = null;
        dirty = true;
        status.setText(R.string.studio_layout_editor_delete);
        updateEmptyState();
    }

    private void saveLayout() {
        if (layoutFile == null) {
            status.setText(R.string.studio_layout_editor_save_failed);
            return;
        }
        try {
            FileUtil.writeFile(layoutFile.getAbsolutePath(), buildXml());
            dirty = false;
            setResult(RESULT_OK);
            SketchwareUtil.toast(getString(R.string.studio_layout_editor_saved));
            status.setText(R.string.studio_layout_editor_saved);
        } catch (Exception e) {
            status.setText(getString(R.string.studio_layout_editor_save_failed) + ": " + e.getMessage());
        }
    }

    private String buildXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
        xml.append("    android:layout_width=\"match_parent\"\n");
        xml.append("    android:layout_height=\"match_parent\"\n");
        xml.append("    android:orientation=\"vertical\">\n\n");
        for (int i = 0; i < canvas.getChildCount(); i++) {
            Object tag = canvas.getChildAt(i).getTag();
            if (tag instanceof WidgetState) {
                appendWidgetXml(xml, (WidgetState) tag);
            }
        }
        xml.append("</LinearLayout>\n");
        return xml.toString();
    }

    private void appendWidgetXml(StringBuilder xml, WidgetState state) {
        String tag = "Space".equals(state.tag) ? "View" : state.tag;
        xml.append("    <").append(tag).append("\n");
        xml.append("        android:id=\"@+id/").append(escapeXml(state.id)).append("\"\n");
        xml.append("        android:layout_width=\"match_parent\"\n");
        if ("ImageView".equals(state.tag)) {
            xml.append("        android:layout_height=\"96dp\"\n");
            xml.append("        android:background=\"#ECEFF1\"\n");
            xml.append("        android:scaleType=\"center\" />\n\n");
            return;
        }
        if ("Space".equals(state.tag) || "View".equals(state.tag)) {
            xml.append("        android:layout_height=\"28dp\" />\n\n");
            return;
        }
        if ("LinearLayout".equals(state.tag)) {
            xml.append("        android:layout_height=\"wrap_content\"\n");
            xml.append("        android:orientation=\"vertical\" />\n\n");
            return;
        }
        xml.append("        android:layout_height=\"wrap_content\"\n");
        if ("EditText".equals(state.tag)) {
            xml.append("        android:hint=\"").append(escapeXml(state.hint)).append("\" />\n\n");
            return;
        }
        xml.append("        android:text=\"").append(escapeXml(state.text)).append("\" />\n\n");
    }

    private void showSource() {
        TextView source = new TextView(this);
        source.setText(buildXml());
        source.setTextIsSelectable(true);
        source.setTextSize(12);
        source.setPadding(dp(16), dp(12), dp(16), dp(12));
        source.setTypeface(android.graphics.Typeface.MONOSPACE);
        FrameLayout container = new FrameLayout(this);
        container.addView(source, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.studio_layout_editor_source)
                .setView(container)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void updateEmptyState() {
        emptyState.setVisibility(canvas.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private String normalizeTag(String rawTag) {
        if (rawTag == null) {
            return "View";
        }
        String lower = rawTag.toLowerCase(Locale.US);
        if (lower.contains("textview")) {
            return "TextView";
        }
        if (lower.contains("button")) {
            return "Button";
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
        if (lower.endsWith(".space") || "space".equals(lower)) {
            return "Space";
        }
        if ("view".equals(lower)) {
            return "View";
        }
        return rawTag;
    }

    private boolean isSupportedWidget(String tag) {
        return "TextView".equals(tag)
                || "Button".equals(tag)
                || "EditText".equals(tag)
                || "ImageView".equals(tag)
                || "LinearLayout".equals(tag)
                || "Space".equals(tag)
                || "View".equals(tag);
    }

    private String defaultText(String tag) {
        if ("Button".equals(tag)) {
            return "Button";
        }
        if ("EditText".equals(tag)) {
            return "";
        }
        if ("ImageView".equals(tag)) {
            return "";
        }
        if ("LinearLayout".equals(tag)) {
            return "";
        }
        if ("Space".equals(tag) || "View".equals(tag)) {
            return "";
        }
        return "TextView";
    }

    private String nextViewId(String tag) {
        String prefix = tag == null ? "view" : tag.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.US);
        prefix = prefix.replaceAll("[^a-z0-9_]", "_");
        if (prefix.isEmpty() || !Character.isLetter(prefix.charAt(0))) {
            prefix = "view";
        }
        return prefix + "_" + generatedIdCounter++;
    }

    private String cleanId(String rawId) {
        if (rawId == null) {
            return null;
        }
        int slash = rawId.lastIndexOf('/');
        if (slash >= 0 && slash < rawId.length() - 1) {
            return rawId.substring(slash + 1);
        }
        return rawId.replace("@+id/", "").replace("@id/", "");
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private int resolveColor(int attr) {
        android.util.TypedValue value = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (!dirty) {
            super.onBackPressed();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.studio_layout_editor_title)
                .setMessage(R.string.studio_layout_editor_unsaved)
                .setPositiveButton(R.string.studio_action_save, (dialog, which) -> {
                    saveLayout();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static class WidgetSpec {
        final String tag;
        final int labelRes;
        final int iconRes;

        WidgetSpec(String tag, int labelRes, int iconRes) {
            this.tag = tag;
            this.labelRes = labelRes;
            this.iconRes = iconRes;
        }
    }

    private static class WidgetState {
        final String tag;
        String id;
        String text;
        String hint;

        WidgetState(String tag) {
            this.tag = tag;
        }
    }
}
