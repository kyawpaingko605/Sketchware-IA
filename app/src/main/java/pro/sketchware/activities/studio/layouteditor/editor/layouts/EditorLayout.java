package pro.sketchware.activities.studio.layouteditor.editor.layouts;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import pro.sketchware.R;
import pro.sketchware.activities.studio.layouteditor.model.LayoutDocument;
import pro.sketchware.activities.studio.layouteditor.model.WidgetItem;

public class EditorLayout extends LinearLayout {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(WidgetItem item);
    }

    private final List<WidgetItem> widgets = new ArrayList<>();
    private final LinkedHashMap<String, String> rootAttributes = new LinkedHashMap<>();
    private String rootTag = "LinearLayout";
    private WidgetItem selectedItem;
    private OnSelectionChangedListener selectionChangedListener;
    private int nextId = 1;

    public EditorLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setMinimumHeight(dp(520));
        setPadding(dp(8), dp(8), dp(8), dp(8));
        setBackground(buildBackground(
                themeColor(com.google.android.material.R.attr.colorSurface),
                themeColor(com.google.android.material.R.attr.colorOutlineVariant),
                1,
                dp(6)
        ));
        setOnDragListener((view, event) -> handleDrop(event));
        rootAttributes.put("android:layout_width", "match_parent");
        rootAttributes.put("android:layout_height", "match_parent");
        rootAttributes.put("android:orientation", "vertical");
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        selectionChangedListener = listener;
    }

    public void setDocument(LayoutDocument document) {
        widgets.clear();
        rootAttributes.clear();
        rootTag = document.rootTag == null || document.rootTag.trim().isEmpty() ? "LinearLayout" : document.rootTag;
        rootAttributes.putAll(document.rootAttributes);
        widgets.addAll(document.children);
        for (WidgetItem item : widgets) {
            normalizeWidget(item);
        }
        selectedItem = null;
        rebuild();
    }

    public List<WidgetItem> getWidgets() {
        return new ArrayList<>(widgets);
    }

    public String getRootTag() {
        return rootTag;
    }

    public LinkedHashMap<String, String> getRootAttributes() {
        return rootAttributes;
    }

    public WidgetItem getSelectedItem() {
        return selectedItem;
    }

    public void addWidget(String type) {
        WidgetItem item = new WidgetItem(type);
        normalizeWidget(item);
        widgets.add(item);
        selectedItem = item;
        rebuild();
    }

    public boolean deleteSelected() {
        if (selectedItem == null) {
            return false;
        }
        widgets.remove(selectedItem);
        selectedItem = null;
        rebuild();
        return true;
    }

    public boolean hasWidgets() {
        return !widgets.isEmpty();
    }

    private boolean handleDrop(DragEvent event) {
        if (event.getAction() != DragEvent.ACTION_DROP) {
            return true;
        }
        int index = getDropIndex(event.getY());
        Object state = event.getLocalState();
        if (state instanceof WidgetItem) {
            WidgetItem item = (WidgetItem) state;
            int oldIndex = widgets.indexOf(item);
            if (oldIndex >= 0) {
                widgets.remove(oldIndex);
                if (oldIndex < index) {
                    index--;
                }
            }
            widgets.add(Math.max(0, Math.min(index, widgets.size())), item);
            selectedItem = item;
            rebuild();
            return true;
        }
        String type = null;
        if (state instanceof String) {
            type = (String) state;
        } else if (event.getClipData() != null && event.getClipData().getItemCount() > 0) {
            CharSequence text = event.getClipData().getItemAt(0).getText();
            type = text == null ? null : text.toString();
        }
        if (type != null) {
            WidgetItem item = new WidgetItem(type);
            normalizeWidget(item);
            widgets.add(Math.max(0, Math.min(index, widgets.size())), item);
            selectedItem = item;
            rebuild();
        }
        return true;
    }

    private int getDropIndex(float y) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (y < child.getY() + child.getHeight() / 2f) {
                return i;
            }
        }
        return getChildCount();
    }

    private void rebuild() {
        removeAllViews();
        for (WidgetItem item : widgets) {
            addView(createRow(item), rowParams());
        }
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedItem);
        }
    }

    private FrameLayout createRow(WidgetItem item) {
        FrameLayout row = new FrameLayout(getContext());
        row.setTag(item);
        row.setPadding(dp(5), dp(5), dp(5), dp(5));
        int border = item == selectedItem
                ? themeColor(R.attr.colorPrimary)
                : themeColor(com.google.android.material.R.attr.colorOutlineVariant);
        int fill = item == selectedItem
                ? themeColor(com.google.android.material.R.attr.colorPrimaryContainer)
                : themeColor(com.google.android.material.R.attr.colorSurfaceContainerLow);
        row.setBackground(buildBackground(fill, border, item == selectedItem ? 2 : 1, dp(5)));
        row.setOnClickListener(v -> select(item));
        row.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("widget", item.type);
            v.startDragAndDrop(data, new View.DragShadowBuilder(v), item, 0);
            return true;
        });
        row.addView(createPreview(item), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, previewHeight(item)));
        return row;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        return params;
    }

    private View createPreview(WidgetItem item) {
        String type = item.type;
        if ("Button".equals(type)) {
            Button button = new Button(getContext());
            button.setAllCaps(false);
            button.setText(textValue(item, "Button"));
            return button;
        }
        if ("EditText".equals(type)) {
            EditText editText = new EditText(getContext());
            editText.setSingleLine(true);
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            editText.setHint(hintValue(item));
            editText.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface));
            editText.setHintTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            return editText;
        }
        if ("CheckBox".equals(type)) {
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(textValue(item, "CheckBox"));
            checkBox.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface));
            return checkBox;
        }
        if ("RadioButton".equals(type)) {
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setText(textValue(item, "RadioButton"));
            radioButton.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface));
            return radioButton;
        }
        if ("Switch".equals(type)) {
            Switch switchView = new Switch(getContext());
            switchView.setText(textValue(item, "Switch"));
            switchView.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface));
            return switchView;
        }
        if ("SeekBar".equals(type)) {
            SeekBar seekBar = new SeekBar(getContext());
            seekBar.setProgress(40);
            return seekBar;
        }
        if ("ProgressBar".equals(type)) {
            ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setProgress(50);
            progressBar.setIndeterminate(false);
            return progressBar;
        }
        if ("Spinner".equals(type)) {
            Spinner spinner = new Spinner(getContext());
            spinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, new String[]{"Item 1", "Item 2"}));
            return spinner;
        }
        if ("ListView".equals(type)) {
            ListView listView = new ListView(getContext());
            listView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new String[]{"Item 1", "Item 2", "Item 3"}));
            return listView;
        }
        if ("WebView".equals(type)) {
            TextView webViewPlaceholder = label("WebView", 13, false);
            webViewPlaceholder.setGravity(Gravity.CENTER);
            webViewPlaceholder.setBackground(buildBackground(
                    themeColor(com.google.android.material.R.attr.colorSurfaceContainer),
                    themeColor(com.google.android.material.R.attr.colorOutlineVariant),
                    1,
                    dp(4)
            ));
            return webViewPlaceholder;
        }
        if ("CalendarView".equals(type)) {
            CalendarView calendarView = new CalendarView(getContext());
            calendarView.setShowWeekNumber(false);
            return calendarView;
        }
        if ("ImageView".equals(type)) {
            ImageView image = new ImageView(getContext());
            image.setImageResource(R.drawable.ic_mtrl_image);
            image.setColorFilter(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            image.setScaleType(ImageView.ScaleType.CENTER);
            image.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceContainer));
            return image;
        }
        if ("LinearLayout".equals(type)) {
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            layout.setPadding(dp(10), 0, dp(10), 0);
            layout.setBackground(buildBackground(
                    themeColor(com.google.android.material.R.attr.colorSurface),
                    themeColor(com.google.android.material.R.attr.colorOutlineVariant),
                    1,
                    dp(4)
            ));
            TextView label = label("LinearLayout", 13, false);
            layout.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return layout;
        }
        if ("Space".equals(type) || "View".equals(type)) {
            Space space = new Space(getContext());
            space.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorOutlineVariant));
            return space;
        }
        return label(textValue(item, "TextView"), 16, true);
    }

    private TextView label(String text, int size, boolean primary) {
        TextView view = new TextView(getContext());
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(8), 0, dp(8), 0);
        view.setText(text);
        view.setTextColor(themeColor(primary ? com.google.android.material.R.attr.colorOnSurface : com.google.android.material.R.attr.colorOnSurfaceVariant));
        view.setTextSize(size);
        if (primary) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private int previewHeight(WidgetItem item) {
        if ("ImageView".equals(item.type)) {
            return dp(96);
        }
        if ("ListView".equals(item.type) || "WebView".equals(item.type)) {
            return dp(120);
        }
        if ("CalendarView".equals(item.type)) {
            return dp(220);
        }
        if ("Space".equals(item.type) || "View".equals(item.type)) {
            return dp(28);
        }
        if ("LinearLayout".equals(item.type)) {
            return dp(72);
        }
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private void select(WidgetItem item) {
        selectedItem = item;
        rebuild();
    }

    private void normalizeWidget(WidgetItem item) {
        item.type = WidgetItem.normalize(item.type);
        if (item.id == null || item.id.trim().isEmpty()) {
            item.id = cleanId(item.getAttribute("android:id"));
        }
        if (item.id == null || item.id.trim().isEmpty()) {
            item.id = nextId(item.type);
        }
        item.attributes.put("android:id", "@+id/" + item.id);
        if (!item.attributes.containsKey("android:layout_width")) {
            item.attributes.put("android:layout_width", "match_parent");
        }
        if (!item.attributes.containsKey("android:layout_height")) {
            item.attributes.put("android:layout_height", defaultHeight(item));
        }
        if ("LinearLayout".equals(item.type) && !item.attributes.containsKey("android:orientation")) {
            item.attributes.put("android:orientation", "vertical");
        }
        item.text = textValue(item, item.type);
        item.hint = hintValue(item);
    }

    private String textValue(WidgetItem item, String fallback) {
        String value = item.text;
        if (value == null || value.trim().isEmpty()) {
            value = item.getAttribute("android:text");
        }
        if (value == null || value.trim().isEmpty()) {
            value = fallback;
        }
        return value;
    }

    private String hintValue(WidgetItem item) {
        String value = item.hint;
        if (value == null || value.trim().isEmpty()) {
            value = item.getAttribute("android:hint");
        }
        return value == null || value.trim().isEmpty() ? "Text" : value;
    }

    private String defaultHeight(WidgetItem item) {
        if ("ImageView".equals(item.type)) {
            return "96dp";
        }
        if ("ListView".equals(item.type) || "WebView".equals(item.type)) {
            return "120dp";
        }
        if ("CalendarView".equals(item.type)) {
            return "220dp";
        }
        if ("Space".equals(item.type) || "View".equals(item.type)) {
            return "28dp";
        }
        return "wrap_content";
    }

    private String nextId(String type) {
        String prefix = type.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.US);
        prefix = prefix.replaceAll("[^a-z0-9_]", "_");
        if (prefix.isEmpty() || !Character.isLetter(prefix.charAt(0))) {
            prefix = "view";
        }
        return prefix + "_" + nextId++;
    }

    private String cleanId(String id) {
        if (id == null) {
            return null;
        }
        int slash = id.lastIndexOf('/');
        return slash >= 0 && slash < id.length() - 1 ? id.substring(slash + 1) : id;
    }

    private GradientDrawable buildBackground(int fill, int stroke, int strokeWidth, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setStroke(dp(strokeWidth), stroke);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int color(int colorRes) {
        try {
            return getResources().getColor(colorRes, getContext().getTheme());
        } catch (Throwable ignored) {
            return Color.TRANSPARENT;
        }
    }

    private int themeColor(int attr) {
        TypedValue value = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attr, value, true)) {
            return value.data;
        }
        return Color.TRANSPARENT;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return super.onInterceptTouchEvent(event);
    }
}
