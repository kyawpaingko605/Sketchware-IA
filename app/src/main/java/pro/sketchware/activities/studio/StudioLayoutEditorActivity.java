package pro.sketchware.activities.studio;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import pro.sketchware.R;
import pro.sketchware.activities.studio.uidesigner.DesignerApp;
import pro.sketchware.activities.studio.uidesigner.adapter.WidgetsAdapter;
import pro.sketchware.activities.studio.uidesigner.model.LayoutDocument;
import pro.sketchware.activities.studio.uidesigner.tools.XmlLayoutGenerator;
import pro.sketchware.activities.studio.uidesigner.tools.XmlLayoutParser;
import pro.sketchware.activities.studio.uidesigner.util.Const;
import pro.sketchware.activities.studio.uidesigner.view.DesignerLayout;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class StudioLayoutEditorActivity extends BaseAppCompatActivity {

    public static final String EXTRA_SC_ID = "sc_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_FILE_PATH = "file_path";
    public static final String EXTRA_XML = "xml";

    private static final int MENU_SAVE = 1;
    private static final int MENU_DELETE = 2;
    private static final int MENU_SOURCE = 3;

    private DesignerLayout designerLayout;
    private TextView statusView;
    private TextView emptyView;
    private File layoutFile;
    private boolean dirty;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DesignerApp.install(getApplicationContext());

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String filePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        layoutFile = filePath == null ? null : new File(filePath);

        setContentView(createContent(title == null ? "" : title));
        loadLayout();
    }

    private View createContent(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle(R.string.studio_layout_editor_title);
        toolbar.setSubtitle(title);
        toolbar.setTitleTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface));
        toolbar.setSubtitleTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        toolbar.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface));
        setupToolbar(toolbar);
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, toolbarHeight()));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        body.addView(createCanvasStage(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        body.addView(createPalettePanel(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(168)));

        statusView = new TextView(this);
        statusView.setSingleLine(true);
        statusView.setEllipsize(TextUtils.TruncateAt.END);
        statusView.setGravity(Gravity.CENTER_VERTICAL);
        statusView.setPadding(dp(12), 0, dp(12), 0);
        statusView.setText(R.string.studio_layout_editor_ready);
        statusView.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        statusView.setTextSize(11);
        statusView.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceContainer));
        root.addView(statusView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        return root;
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
                deleteSelected();
                return true;
            }
            if (item.getItemId() == MENU_SOURCE) {
                showSource();
                return true;
            }
            return false;
        });
    }

    private View createCanvasStage() {
        FrameLayout stage = new FrameLayout(this);
        stage.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceContainerLowest));
        stage.setPadding(dp(12), dp(12), dp(12), dp(12));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout center = new LinearLayout(this);
        center.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        center.setPadding(0, dp(8), 0, dp(24));
        center.setOrientation(LinearLayout.VERTICAL);

        designerLayout = new DesignerLayout(this);
        designerLayout.setOnSelectionChangedListener(item -> {
            updateEmptyState();
            if (item == null) {
                statusView.setText(R.string.studio_layout_editor_ready);
            } else {
                statusView.setText(item.type + "  " + item.id);
            }
        });
        designerLayout.setOnLayoutChangedListener(() -> {
            dirty = true;
            updateEmptyState();
        });
        int editorWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(420));
        center.addView(designerLayout, new LinearLayout.LayoutParams(editorWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollView.addView(center, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        stage.addView(scrollView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        emptyView = new TextView(this);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setText(R.string.studio_layout_editor_empty);
        emptyView.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        emptyView.setTextSize(14);
        emptyView.setPadding(dp(24), dp(24), dp(24), dp(24));
        emptyView.setOnDragListener((view, event) -> designerLayout.dispatchDragEvent(event));
        stage.addView(emptyView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return stage;
    }

    private View createPalettePanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceContainer));

        TextView title = new TextView(this);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(12), 0, dp(8), 0);
        title.setText(R.string.studio_layout_editor_palette);
        title.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface));
        title.setTextSize(13);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(dp(8), 0, dp(8), dp(8));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new WidgetsAdapter(designerLayout, Const.getWidgetsGroup()));
        panel.addView(recyclerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return panel;
    }

    private void loadLayout() {
        String xml = getIntent().getStringExtra(EXTRA_XML);
        if ((xml == null || xml.trim().isEmpty()) && layoutFile != null && layoutFile.isFile()) {
            xml = FileUtil.readFile(layoutFile.getAbsolutePath());
        }
        LayoutDocument document = new XmlLayoutParser().parse(xml);
        designerLayout.setDocument(document);
        dirty = false;
        updateEmptyState();
    }

    private void deleteSelected() {
        if (!designerLayout.deleteSelected()) {
            SketchwareUtil.toast(getString(R.string.studio_layout_editor_no_selection));
            return;
        }
        dirty = true;
        updateEmptyState();
    }

    private void saveLayout() {
        if (layoutFile == null) {
            statusView.setText(R.string.studio_layout_editor_save_failed);
            return;
        }
        try {
            FileUtil.writeFile(layoutFile.getAbsolutePath(), new XmlLayoutGenerator().generate(designerLayout));
            dirty = false;
            setResult(RESULT_OK);
            statusView.setText(R.string.studio_layout_editor_saved);
            SketchwareUtil.toast(getString(R.string.studio_layout_editor_saved));
        } catch (Exception e) {
            statusView.setText(getString(R.string.studio_layout_editor_save_failed) + ": " + e.getMessage());
        }
    }

    private void showSource() {
        EditText xmlView = new EditText(this);
        xmlView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        xmlView.setText(new XmlLayoutGenerator().generate(designerLayout));
        xmlView.setTextIsSelectable(true);
        xmlView.setSingleLine(false);
        xmlView.setMinLines(12);
        xmlView.setTextSize(12);
        xmlView.setTypeface(android.graphics.Typeface.MONOSPACE);
        xmlView.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface));
        xmlView.setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface));
        xmlView.setPadding(dp(12), dp(10), dp(12), dp(10));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.studio_layout_editor_source)
                .setView(xmlView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void updateEmptyState() {
        if (emptyView != null) {
            emptyView.setVisibility(designerLayout != null && designerLayout.hasWidgets() ? View.GONE : View.VISIBLE);
        }
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

    private int toolbarHeight() {
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true)) {
            return TypedValue.complexToDimensionPixelSize(value.data, getResources().getDisplayMetrics());
        }
        return dp(56);
    }

    private int themeColor(int attr) {
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                return getResources().getColor(value.resourceId, getTheme());
            }
            return value.data;
        }
        return Color.TRANSPARENT;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
