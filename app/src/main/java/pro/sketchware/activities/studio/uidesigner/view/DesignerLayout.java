package pro.sketchware.activities.studio.uidesigner.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import pro.sketchware.R;
import pro.sketchware.activities.studio.uidesigner.DesignerApp;
import pro.sketchware.activities.studio.uidesigner.model.LayoutDocument;
import pro.sketchware.activities.studio.uidesigner.model.WidgetItem;
import pro.sketchware.activities.studio.uidesigner.util.DesigerUtil;
import pro.sketchware.activities.studio.uidesigner.widget.Widget;
import pro.sketchware.activities.studio.uidesigner.widget.WidgetButton;
import pro.sketchware.activities.studio.uidesigner.widget.WidgetImageView;
import pro.sketchware.activities.studio.uidesigner.widget.WidgetLinearLayout;
import pro.sketchware.activities.studio.uidesigner.widget.WidgetTextView;

public class DesignerLayout extends FrameLayout {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(WidgetItem item);
    }

    public interface OnLayoutChangedListener {
        void onLayoutChanged();
    }

    protected boolean isEditMode = true;

    public static View view_location;
    public static int defaultIndex;
    public static int index;
    public static boolean addedInLayout;
    public static View mView;
    public static View mWidget;

    private final List<WidgetItem> widgets = new ArrayList<>();
    private final LinkedHashMap<String, String> rootAttributes = new LinkedHashMap<>();
    private String rootTag = "LinearLayout";
    private WidgetItem selectedItem;
    private OnSelectionChangedListener selectionChangedListener;
    private OnLayoutChangedListener layoutChangedListener;
    private int nextId = 1;

    public DesignerLayout(Context context) {
        super(context);
        init(null);
    }

    public DesignerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public DesignerLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    protected void init(@Nullable AttributeSet attrs) {
        DesignerApp.install(getContext());
        setWillNotDraw(false);
        setMinimumHeight(dp(520));
        setPadding(dp(8), dp(8), dp(8), dp(8));
        setBackground(buildBackground(
                themeColor(com.google.android.material.R.attr.colorSurface),
                themeColor(com.google.android.material.R.attr.colorOutlineVariant),
                1,
                dp(6)
        ));
        setOnDragListener(onDragListener);

        view_location = new View(getContext());
        view_location.setBackgroundColor(color(R.color.studio_accent_soft));
        view_location.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        rootAttributes.put("android:layout_width", "match_parent");
        rootAttributes.put("android:layout_height", "match_parent");
        rootAttributes.put("android:orientation", "vertical");
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        selectionChangedListener = listener;
    }

    public void setOnLayoutChangedListener(OnLayoutChangedListener listener) {
        layoutChangedListener = listener;
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

    public boolean isEmpty() {
        return getChildCount() <= 0;
    }

    public void setEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;
    }

    public void toggleEditMode() {
        isEditMode = !isEditMode;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void addWidget(String type) {
        WidgetItem item = new WidgetItem(type);
        normalizeWidget(item);
        widgets.add(item);
        selectedItem = item;
        rebuild();
        notifyLayoutChanged();
    }

    public boolean deleteSelected() {
        if (selectedItem == null) {
            return false;
        }
        widgets.remove(selectedItem);
        selectedItem = null;
        rebuild();
        notifyLayoutChanged();
        return true;
    }

    public boolean hasWidgets() {
        return !widgets.isEmpty();
    }

    public LinearLayout createWidgetContainerFromType(String type) {
        WidgetItem item = new WidgetItem(type);
        normalizeWidget(item);
        return createWidgetContainer(item, true);
    }

    @Override
    protected void onDraw(@NonNull android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < getChildCount(); i++) {
            changeLongClick(getChildAt(i));
        }
    }

    private void rebuild() {
        removeAllViews();
        for (WidgetItem item : widgets) {
            addView(createWidgetContainer(item, false), rowParams(item));
        }
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedItem);
        }
    }

    private LinearLayout createWidgetContainer(WidgetItem item, boolean transientDrag) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setTag(item);
        row.setPadding(dp(5), dp(5), dp(5), dp(5));
        row.setOnClickListener(v -> select(item));
        row.setOnDragListener(onDragListener);
        row.setBackground(buildBackground(
                item == selectedItem ? themeColor(com.google.android.material.R.attr.colorPrimaryContainer) : themeColor(com.google.android.material.R.attr.colorSurfaceContainerLow),
                item == selectedItem ? themeColor(R.attr.colorPrimary) : themeColor(com.google.android.material.R.attr.colorOutlineVariant),
                item == selectedItem ? 2 : 1,
                dp(5)
        ));

        Widget widget = createWidget(item);
        widget.setTag(item);
        widget.setOnClickListener(v -> select(item));
        widget.setOnLongClickListener(onLongClickListener);
        row.addView(widget, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, previewHeight(item)));
        if (transientDrag) {
            row.setLayoutParams(rowParams(item));
        }
        return row;
    }

    private LinearLayout.LayoutParams rowParams(WidgetItem item) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        return params;
    }

    private Widget createWidget(WidgetItem item) {
        Widget widget;
        if ("Button".equals(item.type)) {
            WidgetButton button = new WidgetButton(getContext());
            button.getTextView().setText(textValue(item, "Button"));
            widget = button;
        } else if ("TextView".equals(item.type)) {
            WidgetTextView textView = new WidgetTextView(getContext());
            textView.getTextView().setText(textValue(item, "TextView"));
            textView.getTextView().setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            textView.getTextView().setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface));
            widget = textView;
        } else if ("ImageView".equals(item.type)) {
            WidgetImageView imageView = new WidgetImageView(getContext());
            imageView.getImageView().setImageResource(R.drawable.ic_mtrl_image);
            imageView.getImageView().setColorFilter(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            imageView.getImageView().setScaleType(ImageView.ScaleType.CENTER);
            widget = imageView;
        } else if ("LinearLayout".equals(item.type)) {
            WidgetLinearLayout linearLayout = new WidgetLinearLayout(getContext());
            linearLayout.setOrientation("horizontal".equalsIgnoreCase(item.getAttribute("android:orientation")) ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            TextView label = label("LinearLayout", 13, false);
            linearLayout.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            widget = linearLayout;
        } else {
            widget = new Widget(getContext());
            widget.addView(createPreviewView(item), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        widget.setWidgetId(item.id);
        widget.setWidgetType(item.type);
        return widget;
    }

    private View createPreviewView(WidgetItem item) {
        String type = item.type;
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
            WebView webView = new WebView(getContext());
            webView.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceContainer));
            webView.loadData("<html><body style='font-family:sans-serif;color:#777;text-align:center;'>WebView</body></html>", "text/html", "UTF-8");
            return webView;
        }
        if ("CalendarView".equals(type)) {
            CalendarView calendarView = new CalendarView(getContext());
            calendarView.setShowWeekNumber(false);
            return calendarView;
        }
        if ("Space".equals(type) || "View".equals(type)) {
            Space space = new Space(getContext());
            space.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorOutlineVariant));
            return space;
        }
        return label(textValue(item, type), 16, true);
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

    private void select(WidgetItem item) {
        selectedItem = item;
        rebuild();
    }

    private void changeLongClick(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof Widget) {
            view.setOnLongClickListener(onLongClickListener);
        }
        if (view instanceof WidgetLinearLayout) {
            view.setOnDragListener(onDragListener);
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                changeLongClick(vg.getChildAt(i));
            }
        }
    }

    public View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            ViewGroup vg = (ViewGroup) v.getParent();
            Object tag = v.getTag() instanceof WidgetItem ? v.getTag() : vg.getTag();
            if (tag instanceof WidgetItem) {
                selectedItem = (WidgetItem) tag;
            }
            view_location.getLayoutParams().width = v.getWidth();
            view_location.getLayoutParams().height = v.getHeight();

            if (DesignerLayout.this == vg.getParent()) {
                for (int i = 0; i < getChildCount(); i++) {
                    if (vg == getChildAt(i)) {
                        defaultIndex = i;
                    }
                }
            } else {
                defaultIndex = -1;
            }

            mWidget = vg;
            if (v.startDrag(null, new View.DragShadowBuilder(v), vg, 0)) {
                if (DesignerLayout.this == vg.getParent()) {
                    removeView(vg);
                }
                DesigerUtil.vibrate();
            } else {
                mWidget = null;
            }
            return true;
        }
    };

    public View.OnDragListener onDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    index = getChildCount();
                    addedInLayout = false;
                    mView = null;
                    break;
                case DragEvent.ACTION_DROP:
                    if (view_location.getParent() != null) {
                        addWidgetInLayout(mWidget, index);
                    } else if (mWidget != null && mWidget.getParent() == null) {
                        addWidgetInLayout(mWidget, Math.max(0, Math.min(index, getChildCount())));
                    }
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    if (mView == v) {
                        break;
                    }

                    mView = v;

                    if (mView != DesignerLayout.this) {
                        for (int i = 0; i < getChildCount(); i++) {
                            if (v == getChildAt(i)) {
                                index = i;
                            }
                        }
                    }

                    try {
                        removeView(view_location);
                    } catch (Exception ignored) {
                    }

                    try {
                        addView(view_location, Math.max(0, Math.min(index, getChildCount())));
                    } catch (Exception ignored) {
                    }
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    try {
                        removeView(view_location);
                    } catch (Exception ignored) {
                    }
                    if (mView == DesignerLayout.this) {
                        index = getChildCount();
                    }
                    mView = null;
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (defaultIndex != -1 && !addedInLayout && mWidget != null && mWidget.getParent() == null) {
                        addWidgetInLayout(mWidget, defaultIndex);
                    }

                    try {
                        removeView(view_location);
                    } catch (Exception ignored) {
                    }
                    break;
            }
            return true;
        }
    };

    private void addWidgetInLayout(View v, int index) {
        if (v == null) {
            return;
        }
        if (v.getParent() instanceof ViewGroup) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
        Object tag = v.getTag();
        if (tag instanceof WidgetItem && !widgets.contains(tag)) {
            widgets.add(Math.max(0, Math.min(index, widgets.size())), (WidgetItem) tag);
            selectedItem = (WidgetItem) tag;
        } else if (tag instanceof WidgetItem && widgets.contains(tag)) {
            widgets.remove(tag);
            widgets.add(Math.max(0, Math.min(index, widgets.size())), (WidgetItem) tag);
            selectedItem = (WidgetItem) tag;
        } else {
            return;
        }
        addedInLayout = true;
        rebuild();
        notifyLayoutChanged();
    }

    private void notifyLayoutChanged() {
        if (layoutChangedListener != null) {
            layoutChangedListener.onLayoutChanged();
        }
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
