package pro.sketchware.activities.studio;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import a.a.a.lC;
import a.a.a.ProjectBuilder;
import a.a.a.yq;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import mod.hey.studios.compiler.kotlin.KotlinCompilerBridge;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.diagnostic.MissingFileException;
import mod.hey.studios.code.SrcCodeEditor;
import pro.sketchware.activities.chat.port.VoidPortAiAutocompleteLanguage;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityAndroidStudioProjectBinding;
import pro.sketchware.databinding.ItemStudioFileTreeBinding;
import pro.sketchware.util.ProjectPathResolver;
import pro.sketchware.utility.EditorUtils;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class AndroidStudioProjectActivity extends BaseAppCompatActivity {

    public static final String EXTRA_SC_ID = "sc_id";

    private static final int MENU_SAVE = 1;
    private static final int MENU_FORMAT = 2;
    private static final int MENU_THEME = 3;
    private static final int MENU_UNDO = 4;
    private static final int MENU_REDO = 5;
    private static final int MENU_BUILD = 6;
    private static final int MENU_PREVIEW = 7;
    private static final int MENU_TERMINAL = 8;
    private static final int MENU_ERRORS = 9;
    private static final int MENU_NEW_FILE = 10;
    private static final int MENU_RENAME = 11;
    private static final int MENU_DELETE = 12;
    private static final int MENU_ADD_RESOURCE = 13;
    private static final int MENU_ADD_ICON = 14;
    private static final int MENU_TEXT_COLOR = 15;
    private static final int REQUEST_PICK_STUDIO_ICON = 23051;
    private static final int REQUEST_LAYOUT_EDITOR = 23052;
    private static final int MAX_TREE_NODES = 900;
    private static final int MAX_INITIAL_SCAN_FILES = 1200;
    private static final long MAX_OPEN_BYTES = 1_500_000L;

    private ActivityAndroidStudioProjectBinding binding;
    private ActionBarDrawerToggle drawerToggle;
    private FileTreeAdapter fileTreeAdapter;
    private final List<FileNode> visibleNodes = new ArrayList<>();
    private final Set<String> expandedDirs = new HashSet<>();

    private String scId;
    private File projectRoot;
    private File currentFile;
    private File selectedNodeFile;
    private String lastSavedContent = "";
    private String lastBuildErrors = "";
    private String treeQuery = "";
    private int discoveredFileCount;
    private boolean changingFile;
    private boolean currentFileEditable;
    private boolean showingOutput;
    private boolean showingImage;
    private boolean showingTerminal;
    private boolean buildRunning;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scId = getIntent().getStringExtra(EXTRA_SC_ID);
        HashMap<String, Object> project = scId == null ? null : lC.b(scId);
        String projectName = valueOf(project, "my_ws_name", getString(R.string.studio_editor_title));
        String appName = valueOf(project, "my_app_name", projectName);

        binding = ActivityAndroidStudioProjectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupToolbar(projectName, appName);
        setupEditor();
        setupFileTree();
        loadProject(projectName);
        SketchwareUtil.toast(getString(R.string.studio_project_selected));
    }

    private void setupToolbar(String projectName, String appName) {
        binding.studioToolbar.setTitle(projectName);
        binding.studioToolbar.setSubtitle(appName);
        drawerToggle = new ActionBarDrawerToggle(
                this,
                binding.studioDrawerLayout,
                binding.studioToolbar,
                R.string.studio_action_files,
                R.string.studio_action_files
        );
        binding.studioDrawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        Menu menu = binding.studioToolbar.getMenu();
        addToolbarAction(menu, MENU_UNDO, R.string.studio_action_undo, R.drawable.ic_mtrl_undo);
        addToolbarAction(menu, MENU_REDO, R.string.studio_action_redo, R.drawable.ic_mtrl_redo);
        addToolbarAction(menu, MENU_PREVIEW, R.string.studio_action_preview, R.drawable.ic_mtrl_preview);
        addToolbarAction(menu, MENU_TERMINAL, R.string.studio_action_terminal, R.drawable.ic_mtrl_terminal);
        addToolbarAction(menu, MENU_ERRORS, R.string.studio_action_errors, R.drawable.ic_mtrl_warning, MenuItem.SHOW_AS_ACTION_IF_ROOM);
        addToolbarAction(menu, MENU_FORMAT, R.string.studio_action_format, R.drawable.ic_mtrl_formattext);
        addToolbarAction(menu, MENU_THEME, R.string.studio_action_theme, R.drawable.ic_mtrl_palette);
        addToolbarAction(menu, MENU_BUILD, R.string.studio_action_build, R.drawable.ic_mtrl_run);
        addToolbarAction(menu, MENU_SAVE, R.string.studio_action_save, R.drawable.ic_mtrl_save);
        addToolbarAction(menu, MENU_NEW_FILE, R.string.studio_action_new_file, R.drawable.ic_mtrl_add, MenuItem.SHOW_AS_ACTION_NEVER);
        addToolbarAction(menu, MENU_RENAME, R.string.studio_action_rename, R.drawable.ic_mtrl_edit, MenuItem.SHOW_AS_ACTION_NEVER);
        addToolbarAction(menu, MENU_DELETE, R.string.studio_action_delete, R.drawable.ic_mtrl_delete, MenuItem.SHOW_AS_ACTION_NEVER);
        addToolbarAction(menu, MENU_ADD_RESOURCE, R.string.studio_action_add_resource, R.drawable.ic_mtrl_file_present, MenuItem.SHOW_AS_ACTION_NEVER);
        addToolbarAction(menu, MENU_ADD_ICON, R.string.studio_action_add_icon, R.drawable.ic_mtrl_image, MenuItem.SHOW_AS_ACTION_NEVER);
        addToolbarAction(menu, MENU_TEXT_COLOR, R.string.studio_action_text_color, R.drawable.ic_mtrl_pick_color, MenuItem.SHOW_AS_ACTION_NEVER);

        binding.studioToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_SAVE) {
                saveCurrentFile(true);
                return true;
            }
            if (item.getItemId() == MENU_BUILD) {
                buildProject();
                return true;
            }
            if (item.getItemId() == MENU_PREVIEW) {
                showLayoutPreview();
                return true;
            }
            if (item.getItemId() == MENU_TERMINAL) {
                showTerminal();
                return true;
            }
            if (item.getItemId() == MENU_ERRORS) {
                showCompileErrorGuide();
                return true;
            }
            if (item.getItemId() == MENU_NEW_FILE) {
                showCreateFileDialog();
                return true;
            }
            if (item.getItemId() == MENU_RENAME) {
                showRenameDialog();
                return true;
            }
            if (item.getItemId() == MENU_DELETE) {
                showDeleteDialog();
                return true;
            }
            if (item.getItemId() == MENU_ADD_RESOURCE) {
                showAddResourceDialog();
                return true;
            }
            if (item.getItemId() == MENU_ADD_ICON) {
                pickIconResource();
                return true;
            }
            if (item.getItemId() == MENU_TEXT_COLOR) {
                addTextColorToCurrentLayout();
                return true;
            }
            if (item.getItemId() == MENU_FORMAT) {
                formatCurrentFile();
                return true;
            }
            if (item.getItemId() == MENU_THEME) {
                SrcCodeEditor.showSwitchThemeDialog(this, binding.studioEditor, (dialog, which) -> {
                    SrcCodeEditor.selectTheme(binding.studioEditor, which);
                    dialog.dismiss();
                });
                return true;
            }
            if (item.getItemId() == MENU_UNDO) {
                if (binding.studioEditor.canUndo()) {
                    binding.studioEditor.undo();
                }
                return true;
            }
            if (item.getItemId() == MENU_REDO) {
                if (binding.studioEditor.canRedo()) {
                    binding.studioEditor.redo();
                }
                return true;
            }
            return false;
        });
    }

    private void addToolbarAction(Menu menu, int id, int titleRes, int iconRes) {
        addToolbarAction(menu, id, titleRes, iconRes, MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void addToolbarAction(Menu menu, int id, int titleRes, int iconRes, int showAsAction) {
        MenuItem item = menu.add(Menu.NONE, id, Menu.NONE, titleRes);
        item.setIcon(iconRes);
        item.setShowAsAction(showAsAction);
    }

    private void setupEditor() {
        binding.studioEditor.setTypefaceText(EditorUtils.getTypeface(this));
        binding.studioEditor.setTextSize(14);
        binding.studioEditor.setEditorLanguage(new EmptyLanguage());
        SrcCodeEditor.loadCESettings(this, binding.studioEditor, "studio", true);
        applyDefaultEditorTheme();
        binding.studioTerminalRun.setOnClickListener(v -> runTerminalCommand());
        binding.studioTerminalInput.setOnEditorActionListener((v, actionId, event) -> {
            runTerminalCommand();
            return true;
        });
    }

    private void setupFileTree() {
        fileTreeAdapter = new FileTreeAdapter();
        binding.studioFileTree.setLayoutManager(new LinearLayoutManager(this));
        binding.studioFileTree.setAdapter(fileTreeAdapter);
        binding.studioFileSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                treeQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.US);
                rebuildVisibleNodes();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadProject(String projectName) {
        if (scId == null || scId.trim().isEmpty()) {
            showMissingProject();
            return;
        }

        projectRoot = ProjectPathResolver.getAndroidStudioProjectRoot(scId);
        selectedNodeFile = projectRoot;
        binding.studioToolbar.setSubtitle(ProjectPathResolver.toDisplayPath(scId, projectRoot));
        binding.studioProjectPath.setText(ProjectPathResolver.toDisplayPath(scId, projectRoot));

        if (projectRoot == null || !projectRoot.exists() || !projectRoot.isDirectory()) {
            showMissingProject();
            return;
        }

        expandedDirs.clear();
        expandDirectory(projectRoot);
        expandDirectory(new File(projectRoot, "app"));
        expandDirectory(new File(projectRoot, "app/src"));
        expandDirectory(new File(projectRoot, "app/src/main"));
        expandDirectory(new File(projectRoot, "app/src/main/java"));
        expandDirectory(new File(projectRoot, "app/src/main/kotlin"));
        expandDirectory(new File(projectRoot, "app/src/main/res"));
        expandDirectory(new File(projectRoot, "app/src/main/res/layout"));

        rebuildVisibleNodes();
        File initialFile = findInitialFile(projectRoot);
        if (initialFile != null) {
            openFile(initialFile, false);
            return;
        }

        binding.studioSelectedFile.setText(projectName);
        binding.studioSelectedPath.setText(ProjectPathResolver.toDisplayPath(scId, projectRoot));
        setOutput(getString(R.string.studio_no_file_message), false);
        updateStage();
        updateStatus(getString(R.string.studio_no_file_title));
    }

    private void showMissingProject() {
        currentFile = null;
        selectedNodeFile = null;
        currentFileEditable = false;
        visibleNodes.clear();
        discoveredFileCount = 0;
        fileTreeAdapter.submit(new ArrayList<>());
        binding.studioEmptyTitle.setText(R.string.studio_project_missing);
        binding.studioEmptyMessage.setText(ProjectPathResolver.toDisplayPath(scId, projectRoot));
        binding.studioSelectedFile.setText(R.string.studio_project_missing);
        binding.studioSelectedPath.setText("");
        setOutput(getString(R.string.studio_project_missing), false);
        updateStage();
        updateStatus(getString(R.string.studio_project_missing));
        SketchwareUtil.toast(getString(R.string.studio_project_missing));
    }

    private void rebuildVisibleNodes() {
        visibleNodes.clear();
        discoveredFileCount = 0;
        if (treeQuery.isEmpty()) {
            appendChildren(projectRoot, 0);
        } else {
            appendSearchMatches(projectRoot, 0);
        }
        fileTreeAdapter.submit(new ArrayList<>(visibleNodes));
        binding.studioFilePanelTitle.setText(getString(R.string.studio_files) + " (" + discoveredFileCount + ")");
    }

    private void appendChildren(File directory, int depth) {
        if (directory == null || visibleNodes.size() >= MAX_TREE_NODES) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }

        List<File> childList = new ArrayList<>();
        for (File child : children) {
            if (shouldSkip(child)) {
                continue;
            }
            childList.add(child);
        }
        childList.sort(Comparator
                .comparing((File file) -> !file.isDirectory())
                .thenComparing(file -> file.getName().toLowerCase(Locale.US)));

        for (File child : childList) {
            if (!child.isDirectory()) {
                discoveredFileCount++;
            }
            FileNode node = new FileNode(child, depth, child.isDirectory());
            visibleNodes.add(node);
            if (visibleNodes.size() >= MAX_TREE_NODES) {
                return;
            }
            if (child.isDirectory() && expandedDirs.contains(canonicalPath(child))) {
                appendChildren(child, depth + 1);
            }
        }
    }

    private void appendSearchMatches(File directory, int depth) {
        if (directory == null || visibleNodes.size() >= MAX_TREE_NODES) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        List<File> childList = new ArrayList<>();
        for (File child : children) {
            if (shouldSkip(child)) {
                continue;
            }
            childList.add(child);
        }
        childList.sort(Comparator
                .comparing((File file) -> !file.isDirectory())
                .thenComparing(file -> file.getName().toLowerCase(Locale.US)));

        for (File child : childList) {
            if (!child.isDirectory()) {
                discoveredFileCount++;
            }
            String relative = relativePath(child).toLowerCase(Locale.US);
            if (child.getName().toLowerCase(Locale.US).contains(treeQuery) || relative.contains(treeQuery)) {
                visibleNodes.add(new FileNode(child, depth, child.isDirectory()));
                if (visibleNodes.size() >= MAX_TREE_NODES) {
                    return;
                }
            }
            if (child.isDirectory()) {
                appendSearchMatches(child, depth + 1);
            }
        }
    }

    private void openFile(File file, boolean savePrevious) {
        if (file == null || !file.isFile()) {
            return;
        }
        selectedNodeFile = file;
        if (isPreviewableImage(file)) {
            openImageFile(file, savePrevious);
            return;
        }
        if (!canOpen(file)) {
            setOutput(relativePath(file) + "\n\n" + getString(R.string.studio_file_not_editable), true);
            updateStatus(getString(R.string.studio_file_not_editable));
            return;
        }
        if (savePrevious && hasUnsavedChanges()) {
            saveCurrentFile(false);
        }

        changingFile = true;
        binding.studioProgress.setVisibility(View.VISIBLE);
        try {
            if (!isInsideProject(file)) {
                throw new IOException("File is outside the project root");
            }
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            currentFile = file;
            currentFileEditable = true;
            lastSavedContent = content;
            binding.studioEditor.setText(content);
            applyDefaultEditorTheme();
            applyLanguage(file);
            updateFileHeader(file);
            showEditor();
            if (binding.studioDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.studioDrawerLayout.closeDrawer(GravityCompat.START);
            }
            setOutput("Opened " + relativePath(file) + " (" + formatBytes(file.length()) + ")", false);
            updateStatus(relativePath(file) + " - " + countLines(content) + " lines");
            fileTreeAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            setOutput(getString(R.string.studio_open_failed) + ": " + e.getMessage(), true);
            SketchwareUtil.toast(getString(R.string.studio_open_failed));
        } finally {
            binding.studioProgress.setVisibility(View.GONE);
            changingFile = false;
            updateStage();
        }
    }

    private void openImageFile(File file, boolean savePrevious) {
        if (savePrevious && hasUnsavedChanges()) {
            saveCurrentFile(false);
        }
        try {
            if (!isInsideProject(file)) {
                throw new IOException("File is outside the project root");
            }
            currentFile = file;
            currentFileEditable = false;
            lastSavedContent = "";
            showingImage = true;
            showingOutput = false;
            showingTerminal = false;
            binding.studioImagePreview.setImageURI(Uri.fromFile(file));
            updateFileHeader(file);
            updateStage();
            if (binding.studioDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.studioDrawerLayout.closeDrawer(GravityCompat.START);
            }
            setOutput("Preview " + relativePath(file) + " (" + formatBytes(file.length()) + ")", false);
            updateStatus(relativePath(file) + " - " + getString(R.string.studio_image_preview));
            fileTreeAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            setOutput(getString(R.string.studio_open_failed) + ": " + e.getMessage(), true);
            SketchwareUtil.toast(getString(R.string.studio_open_failed));
        }
    }

    private void applyDefaultEditorTheme() {
        SrcCodeEditor.selectTheme(binding.studioEditor, 0);
    }

    private void applyLanguage(File file) {
        String name = file.getName().toLowerCase(Locale.US);
        try {
            if (name.endsWith(".java")) {
                SrcCodeEditor.selectLanguage(binding.studioEditor, 0);
            } else if (name.endsWith(".kt") || name.endsWith(".kts")) {
                SrcCodeEditor.selectLanguage(binding.studioEditor, 1);
            } else if (name.endsWith(".xml")) {
                SrcCodeEditor.selectLanguage(binding.studioEditor, 2);
            } else {
                binding.studioEditor.setEditorLanguage(new EmptyLanguage());
            }
            binding.studioEditor.setEditorLanguage(VoidPortAiAutocompleteLanguage.wrap(
                    this,
                    scId,
                    file == null ? "" : file.getAbsolutePath(),
                    languageNameFor(file),
                    binding.studioEditor.getEditorLanguage()
            ));
        } catch (Exception ignored) {
            binding.studioEditor.setEditorLanguage(new EmptyLanguage());
        }
    }

    private String languageNameFor(File file) {
        if (file == null) {
            return "text";
        }
        String name = file.getName().toLowerCase(Locale.US);
        if (name.endsWith(".java")) {
            return "java";
        }
        if (name.endsWith(".kt") || name.endsWith(".kts")) {
            return "kotlin";
        }
        if (name.endsWith(".xml")) {
            return "xml";
        }
        if (name.endsWith(".gradle")) {
            return "gradle";
        }
        return "text";
    }

    private boolean saveCurrentFile(boolean showToast) {
        if (currentFile == null) {
            return false;
        }
        if (!currentFileEditable) {
            return false;
        }
        try {
            if (!isInsideProject(currentFile)) {
                throw new IOException("File is outside the project root");
            }
            String content = binding.studioEditor.getText().toString();
            Files.write(currentFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
            lastSavedContent = content;
            updateStatus(relativePath(currentFile) + " - saved");
            setOutput("Saved " + relativePath(currentFile), false);
            if (showToast) {
                SketchwareUtil.toast(getString(R.string.studio_saved));
            }
            return true;
        } catch (Exception e) {
            setOutput(getString(R.string.studio_save_failed) + ": " + e.getMessage(), true);
            if (showToast) {
                SketchwareUtil.toast(getString(R.string.studio_save_failed));
            }
            return false;
        }
    }

    private void formatCurrentFile() {
        if (currentFile == null) {
            return;
        }
        if (!currentFile.getName().toLowerCase(Locale.US).endsWith(".xml")) {
            setOutput(getString(R.string.studio_no_xml_preview), true);
            return;
        }
        String formatted = SrcCodeEditor.prettifyXml(binding.studioEditor.getText().toString(), 4, null);
        if (formatted != null) {
            binding.studioEditor.setText(formatted);
            updateStatus(relativePath(currentFile) + " - formatted");
        }
    }

    private void buildProject() {
        if (buildRunning) {
            return;
        }
        if (projectRoot == null || !projectRoot.isDirectory()) {
            setOutput(getString(R.string.studio_project_missing), true);
            return;
        }
        if (hasUnsavedChanges()) {
            saveCurrentFile(false);
        }

        showOutput();
        setOutput(getString(R.string.studio_build_started) + "...\n" + projectRoot.getAbsolutePath(), false);
        setBuildRunning(true);

        new Thread(() -> {
            try {
                File apk = runStudioBuild();
                appendBuildOutput("");
                appendBuildOutput(getString(R.string.studio_build_finished) + ": " + apk.getAbsolutePath());
                lastBuildErrors = "";
                runOnUiThread(() -> {
                    SketchwareUtil.toast(getString(R.string.studio_build_finished));
                    showInstallBuiltApkDialog(apk);
                });
            } catch (Throwable throwable) {
                String failure = describeBuildFailure(throwable) + "\n" + Log.getStackTraceString(throwable);
                lastBuildErrors = failure;
                appendBuildOutput("");
                appendBuildOutput(getString(R.string.studio_build_failed) + ": " + failure);
                runOnUiThread(() -> SketchwareUtil.toast(getString(R.string.studio_build_failed)));
            } finally {
                runOnUiThread(() -> setBuildRunning(false));
            }
        }).start();
    }

    private void showInstallBuiltApkDialog(File apk) {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_mtrl_apk_install)
                .setTitle(R.string.studio_install_apk_title)
                .setMessage(getString(R.string.studio_install_apk_message) + "\n" + apk.getAbsolutePath())
                .setPositiveButton(R.string.studio_install_apk_button, (dialog, which) -> requestPackageInstallerInstall(apk))
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void requestPackageInstallerInstall(File apk) {
        if (apk == null || !apk.exists()) {
            setOutput(getString(R.string.studio_install_failed) + ": " + getString(R.string.studio_apk_missing), true);
            SketchwareUtil.toast(getString(R.string.studio_apk_missing));
            return;
        }
        try {
            Uri apkUri = FileProvider.getUriForFile(
                    getApplicationContext(),
                    getApplicationContext().getPackageName() + ".provider",
                    apk
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            startActivity(intent);
        } catch (Exception e) {
            setOutput(getString(R.string.studio_install_failed) + ": " + e.getMessage(), true);
            SketchwareUtil.toast(getString(R.string.studio_install_failed));
        }
    }

    private File runStudioBuild() throws Throwable {
        HashMap<String, Object> project = lC.b(scId);
        yq workspace = new yq(getApplicationContext(), projectRoot.getAbsolutePath(), project);
        configureStudioBuildWorkspace(workspace);

        BuildProgressReceiver receiver = (progress, step) -> {
            String line = step >= 0 ? "[" + step + "] " + progress : progress;
            appendBuildOutput(line);
        };

        receiver.onProgress("Deleting temporary files...", 1);
        workspace.f();
        workspace.e();
        ensureStudioBuildDirectories(workspace);
        ensureLauncherActivityExported(new File(workspace.androidManifestPath));

        ProjectBuilder builder = new ProjectBuilder(receiver, getApplicationContext(), workspace);
        builder.buildBuiltInLibraryInformation();

        receiver.onProgress("Preparing AAPT2...", 2);
        builder.maybeExtractAapt2();

        receiver.onProgress("Extracting built-in libraries...", 3);
        BuiltInLibraries.extractCompileAssets(receiver);

        receiver.onProgress("AAPT2 is running...", 8);
        builder.compileResources();

        receiver.onProgress("Generating view binding...", 11);
        builder.generateViewBinding();

        KotlinCompilerBridge.compileKotlinCodeIfPossible(receiver, builder);

        receiver.onProgress("Java is compiling...", 13);
        builder.compileJavaCode();

        receiver.onProgress(builder.getDxRunningText(), 17);
        builder.createDexFilesFromClasses();

        receiver.onProgress("Merging DEX files...", 18);
        builder.getDexFilesReady();

        receiver.onProgress("Building APK...", 19);
        builder.buildApk();

        receiver.onProgress("Signing APK...", 20);
        builder.signDebugApk();

        return new File(workspace.finalToInstallApkPath);
    }

    private void ensureLauncherActivityExported(File manifestFile) throws IOException {
        if (manifestFile == null || !manifestFile.isFile()) {
            return;
        }
        String manifest = new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8);
        String updated = addExportedToLauncherActivity(manifest);
        if (!manifest.equals(updated)) {
            Files.write(manifestFile.toPath(), updated.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String addExportedToLauncherActivity(String manifest) {
        int searchFrom = 0;
        while (searchFrom < manifest.length()) {
            int mainAction = manifest.indexOf("android.intent.action.MAIN", searchFrom);
            if (mainAction == -1) {
                return manifest;
            }
            int intentFilterEnd = manifest.indexOf("</intent-filter>", mainAction);
            int launcherCategory = manifest.indexOf("android.intent.category.LAUNCHER", mainAction);
            if (intentFilterEnd != -1 && launcherCategory != -1 && launcherCategory < intentFilterEnd) {
                int activityStart = manifest.lastIndexOf("<activity", mainAction);
                int activityTagEnd = manifest.indexOf(">", activityStart);
                if (activityStart == -1 || activityTagEnd == -1) {
                    return manifest;
                }
                String activityTag = manifest.substring(activityStart, activityTagEnd);
                if (activityTag.contains("android:exported")) {
                    return manifest;
                }
                return manifest.substring(0, activityTagEnd)
                        + "\n            android:exported=\"true\""
                        + manifest.substring(activityTagEnd);
            }
            searchFrom = mainAction + 1;
        }
        return manifest;
    }

    private void ensureStudioBuildDirectories(yq workspace) throws IOException {
        ensureDirectory(workspace.binDirectoryPath);
        ensureDirectory(workspace.compiledClassesPath);
        ensureDirectory(workspace.rJavaDirectoryPath);
        ensureDirectory(workspace.resDirectoryPath);
        ensureDirectory(workspace.importedSoundsPath);
        ensureDirectory(workspace.assetsPath);
        ensureDirectory(workspace.fontsPath);
    }

    private void ensureDirectory(String path) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        File directory = new File(path);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create directory: " + directory.getAbsolutePath());
        }
    }

    private String describeBuildFailure(Throwable throwable) {
        if (throwable instanceof MissingFileException) {
            MissingFileException exception = (MissingFileException) throwable;
            File missingFile = exception.getMissingFile();
            String type = exception.isMissingDirectory() ? "Missing directory" : "Missing file";
            return type + ": " + (missingFile == null ? throwable.getMessage() : missingFile.getAbsolutePath());
        }
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }

    private void configureStudioBuildWorkspace(yq workspace) {
        workspace.N.packageName = workspace.packageName;
        workspace.N.projectName = workspace.applicationName;
        workspace.N.versionCode = workspace.versionCode;
        workspace.N.versionName = workspace.versionName;
        workspace.N.sc_id = scId;
        workspace.N.isDebugBuild = true;
        workspace.N.g = usesAndroidxOrMaterial();
    }

    private boolean usesAndroidxOrMaterial() {
        return fileContains(new File(projectRoot, "app" + File.separator + "build.gradle"), "androidx")
                || fileContains(new File(projectRoot, "app" + File.separator + "build.gradle"), "appcompat")
                || fileContains(new File(projectRoot, "app" + File.separator + "build.gradle"), "material")
                || fileContains(new File(projectRoot, "app" + File.separator + "src" + File.separator + "main" + File.separator + "AndroidManifest.xml"), "AppCompat")
                || fileContains(new File(projectRoot, "app" + File.separator + "src" + File.separator + "main"), "androidx.appcompat")
                || fileContains(new File(projectRoot, "app" + File.separator + "src" + File.separator + "main"), "com.google.android.material");
    }

    private boolean fileContains(File file, String needle) {
        if (file == null || !file.exists() || needle == null) {
            return false;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return false;
            }
            for (File child : children) {
                if (child.isDirectory() && shouldSkip(child)) {
                    continue;
                }
                if (child.isFile() && canOpen(child) && fileContains(child, needle)) {
                    return true;
                }
                if (child.isDirectory() && fileContains(child, needle)) {
                    return true;
                }
            }
            return false;
        }
        if (file.length() > MAX_OPEN_BYTES) {
            return false;
        }
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).contains(needle);
        } catch (IOException ignored) {
            return false;
        }
    }

    private void appendBuildOutput(String message) {
        runOnUiThread(() -> {
            CharSequence current = binding.studioOutput.getText();
            String prefix = current == null || current.length() == 0 ? "" : current + "\n";
            binding.studioOutput.setText(prefix + message);
            binding.studioOutputContainer.post(() -> binding.studioOutputContainer.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void setBuildRunning(boolean running) {
        buildRunning = running;
        binding.studioProgress.setVisibility(running ? View.VISIBLE : View.GONE);
        MenuItem buildItem = binding.studioToolbar.getMenu().findItem(MENU_BUILD);
        if (buildItem != null) {
            buildItem.setEnabled(!running);
        }
        updateStatus(running ? getString(R.string.studio_build_started) : getString(R.string.studio_output_ready));
    }

    private void updateFileHeader(File file) {
        binding.studioSelectedFile.setText(file.getName());
        binding.studioSelectedPath.setText(ProjectPathResolver.toDisplayPath(scId, file));
        binding.studioSelectedIcon.setImageResource(iconFor(file, false));
    }

    private void showEditor() {
        showingOutput = false;
        showingImage = false;
        showingTerminal = false;
        updateStage();
    }

    private void showOutput() {
        showingOutput = true;
        showingImage = false;
        showingTerminal = false;
        updateStage();
    }

    private void showTerminal() {
        showingTerminal = true;
        showingOutput = true;
        showingImage = false;
        if (binding.studioOutput.getText() == null || binding.studioOutput.getText().length() == 0) {
            binding.studioOutput.setText(getString(R.string.studio_terminal_ready));
        }
        updateStage();
    }

    private void updateStage() {
        boolean hasFile = currentFile != null;
        boolean showCode = hasFile && currentFileEditable && !showingOutput && !showingImage;
        boolean showOutput = showingOutput;
        boolean showImage = hasFile && showingImage;
        boolean showEmpty = !hasFile && !showOutput && !showImage;

        binding.studioEditor.setVisibility(showCode ? View.VISIBLE : View.GONE);
        binding.studioImagePreviewContainer.setVisibility(showImage ? View.VISIBLE : View.GONE);
        binding.studioOutputContainer.setVisibility(showOutput ? View.VISIBLE : View.GONE);
        binding.studioTerminalBar.setVisibility(showingTerminal ? View.VISIBLE : View.GONE);
        binding.studioEmptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }

    private void setOutput(String message, boolean selectOutput) {
        binding.studioOutput.setText(message);
        if (selectOutput && !changingFile) {
            showOutput();
        }
    }

    private void updateStatus(String status) {
        binding.studioStatus.setText(status);
    }

    private boolean hasUnsavedChanges() {
        return currentFileEditable && currentFile != null && !lastSavedContent.equals(binding.studioEditor.getText().toString());
    }

    private void showLayoutPreview() {
        if (currentFile == null || !currentFileEditable || !isLayoutXml(currentFile)) {
            setOutput(getString(R.string.studio_layout_preview_unavailable), true);
            return;
        }
        if (hasUnsavedChanges()) {
            saveCurrentFile(false);
        }
        Intent intent = new Intent(getApplicationContext(), StudioLayoutEditorActivity.class);
        intent.putExtra(StudioLayoutEditorActivity.EXTRA_SC_ID, scId);
        intent.putExtra(StudioLayoutEditorActivity.EXTRA_TITLE, currentFile.getName());
        intent.putExtra(StudioLayoutEditorActivity.EXTRA_FILE_PATH, currentFile.getAbsolutePath());
        intent.putExtra(StudioLayoutEditorActivity.EXTRA_XML, binding.studioEditor.getText().toString());
        startActivityForResult(intent, REQUEST_LAYOUT_EDITOR);
    }

    private void runTerminalCommand() {
        String command = binding.studioTerminalInput.getText() == null
                ? ""
                : binding.studioTerminalInput.getText().toString().trim();
        if (command.isEmpty()) {
            return;
        }
        if (projectRoot == null || !projectRoot.isDirectory()) {
            setOutput(getString(R.string.studio_project_missing), true);
            return;
        }
        binding.studioTerminalInput.setText("");
        showTerminal();
        appendBuildOutput("$ " + command);
        new Thread(() -> {
            Process process = null;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
                processBuilder.directory(projectRoot);
                processBuilder.redirectErrorStream(true);
                process = processBuilder.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        appendBuildOutput(line);
                    }
                }
                int exitCode = process.waitFor();
                appendBuildOutput("[exit " + exitCode + "]");
            } catch (Exception e) {
                appendBuildOutput(getString(R.string.studio_terminal_failed) + ": " + e.getMessage());
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }).start();
    }

    private void showCompileErrorGuide() {
        String source = lastBuildErrors;
        CharSequence output = binding.studioOutput.getText();
        if ((source == null || source.trim().isEmpty()) && output != null) {
            source = output.toString();
        }
        String guide = buildErrorGuide(source);
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_mtrl_warning)
                .setTitle(R.string.studio_action_errors)
                .setMessage(guide)
                .setPositiveButton(R.string.common_word_ok, null)
                .show();
    }

    private String buildErrorGuide(String log) {
        if (log == null || log.trim().isEmpty()) {
            return getString(R.string.studio_errors_empty);
        }
        StringBuilder guide = new StringBuilder();
        String[] lines = log.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.US);
            if (lower.contains("error") || lower.contains("exception") || lower.contains("missing") || lower.contains("failed")) {
                guide.append("- ").append(line.trim()).append('\n');
                count++;
                if (count >= 12) {
                    break;
                }
            }
        }
        if (count == 0) {
            guide.append(log.length() > 1600 ? log.substring(0, 1600) : log);
        } else {
            guide.append('\n').append(getString(R.string.studio_errors_hint));
        }
        return guide.toString().trim();
    }

    private void showCreateFileDialog() {
        File directory = getTargetDirectory();
        EditText input = createDialogInput(getString(R.string.studio_new_file_hint));
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_mtrl_add)
                .setTitle(getString(R.string.studio_action_new_file) + "\n" + relativePath(directory))
                .setView(input)
                .setPositiveButton(R.string.common_word_create, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        return;
                    }
                    createFileIn(directory, name, "");
                })
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void showRenameDialog() {
        File target = getSelectedTarget();
        if (target == null || target.equals(projectRoot)) {
            SketchwareUtil.toast(getString(R.string.studio_select_file));
            return;
        }
        EditText input = createDialogInput(target.getName());
        input.setText(target.getName());
        input.setSelectAllOnFocus(true);
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_mtrl_edit)
                .setTitle(R.string.studio_action_rename)
                .setView(input)
                .setPositiveButton(R.string.common_word_ok, (dialog, which) -> renameTarget(target, input.getText().toString().trim()))
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void showDeleteDialog() {
        File target = getSelectedTarget();
        if (target == null || target.equals(projectRoot)) {
            SketchwareUtil.toast(getString(R.string.studio_select_file));
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_mtrl_delete)
                .setTitle(R.string.studio_action_delete)
                .setMessage(getString(R.string.studio_delete_confirm) + "\n" + relativePath(target))
                .setPositiveButton(R.string.common_word_delete, (dialog, which) -> deleteTarget(target))
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void showAddResourceDialog() {
        String[] labels = {
                getString(R.string.studio_resource_layout),
                getString(R.string.studio_resource_drawable),
                getString(R.string.studio_resource_values),
                getString(R.string.studio_resource_raw)
        };
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_mtrl_file_present)
                .setTitle(R.string.studio_action_add_resource)
                .setItems(labels, (dialog, which) -> showResourceNameDialog(which))
                .show();
    }

    private void showResourceNameDialog(int type) {
        EditText input = createDialogInput(type == 0 ? "activity_example.xml" : "resource_name.xml");
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_mtrl_file_present)
                .setTitle(R.string.studio_action_add_resource)
                .setView(input)
                .setPositiveButton(R.string.common_word_create, (dialog, which) -> createResource(type, input.getText().toString().trim()))
                .setNegativeButton(R.string.common_word_cancel, null)
                .show();
    }

    private void pickIconResource() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_STUDIO_ICON);
    }

    private void addTextColorToCurrentLayout() {
        ensureTextColorResources();
        if (currentFile == null || !currentFileEditable || !isLayoutXml(currentFile)) {
            openFile(getValuesFile("colors.xml"), true);
            return;
        }
        String content = binding.studioEditor.getText().toString();
        String updated = addTextColorAttribute(content, "TextView");
        updated = addTextColorAttribute(updated, "Button");
        updated = addTextColorAttribute(updated, "EditText");
        if (!updated.equals(content)) {
            binding.studioEditor.setText(updated);
            updateStatus(relativePath(currentFile) + " - " + getString(R.string.studio_text_color_added));
        } else {
            setOutput(getString(R.string.studio_text_color_exists), true);
        }
    }

    private String addTextColorAttribute(String content, String tagName) {
        int start = content.indexOf("<" + tagName);
        while (start != -1) {
            int end = content.indexOf(">", start);
            if (end == -1) {
                return content;
            }
            String tag = content.substring(start, end);
            if (!tag.contains("android:textColor")) {
                return content.substring(0, end)
                        + "\n        android:textColor=\"@color/textColorPrimary\""
                        + content.substring(end);
            }
            start = content.indexOf("<" + tagName, end);
        }
        return content;
    }

    private void ensureTextColorResources() {
        File colorsFile = getValuesFile("colors.xml");
        File parent = colorsFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        String content = colorsFile.exists() ? FileUtil.readFile(colorsFile.getAbsolutePath()) : "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n";
        if (!content.contains("name=\"textColorPrimary\"")) {
            content = content.replace("</resources>", "    <color name=\"textColorPrimary\">#1C1B1F</color>\n</resources>");
        }
        if (!content.contains("name=\"textColorSecondary\"")) {
            content = content.replace("</resources>", "    <color name=\"textColorSecondary\">#5F5E62</color>\n</resources>");
        }
        FileUtil.writeFile(colorsFile.getAbsolutePath(), content);
        rebuildVisibleNodes();
    }

    private EditText createDialogInput(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        int padding = dp(20);
        input.setPadding(padding, dp(8), padding, dp(8));
        return input;
    }

    private File getSelectedTarget() {
        if (selectedNodeFile != null) {
            return selectedNodeFile;
        }
        if (currentFile != null) {
            return currentFile;
        }
        return projectRoot;
    }

    private File getTargetDirectory() {
        File target = getSelectedTarget();
        if (target == null) {
            return projectRoot;
        }
        return target.isDirectory() ? target : target.getParentFile();
    }

    private void createFileIn(File directory, String name, String content) {
        try {
            if (directory == null || !directory.isDirectory()) {
                directory = projectRoot;
            }
            File target = new File(directory, name);
            if (!isInsideProject(target)) {
                throw new IOException("File is outside the project root");
            }
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!target.exists()) {
                Files.write(target.toPath(), content.getBytes(StandardCharsets.UTF_8));
            }
            selectedNodeFile = target;
            expandDirectory(target.getParentFile());
            rebuildVisibleNodes();
            if (target.isFile()) {
                openFile(target, true);
            }
        } catch (Exception e) {
            setOutput(getString(R.string.studio_create_failed) + ": " + e.getMessage(), true);
        }
    }

    private void createResource(int type, String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return;
        }
        String name = sanitizeResourceFileName(rawName);
        File directory;
        String content;
        switch (type) {
            case 0 -> {
                directory = getResDirectory("layout");
                if (!name.endsWith(".xml")) {
                    name += ".xml";
                }
                content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" />\n";
            }
            case 1 -> {
                directory = getResDirectory("drawable");
                if (!name.endsWith(".xml")) {
                    name += ".xml";
                }
                content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                        + "    <solid android:color=\"@color/colorPrimary\" />\n"
                        + "    <corners android:radius=\"12dp\" />\n"
                        + "</shape>\n";
            }
            case 2 -> {
                directory = getResDirectory("values");
                if (!name.endsWith(".xml")) {
                    name += ".xml";
                }
                content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n";
            }
            default -> {
                directory = getResDirectory("raw");
                content = "";
            }
        }
        createFileIn(directory, name, content);
    }

    private void renameTarget(File target, String newName) {
        if (newName == null || newName.isEmpty() || target == null) {
            return;
        }
        try {
            File renamed = new File(target.getParentFile(), newName);
            if (!isInsideProject(renamed)) {
                throw new IOException("File is outside the project root");
            }
            Files.move(target.toPath(), renamed.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (currentFile != null && canonicalPath(currentFile).equals(canonicalPath(target))) {
                currentFile = renamed;
            }
            selectedNodeFile = renamed;
            rebuildVisibleNodes();
            updateFileHeader(renamed);
            updateStatus(getString(R.string.studio_renamed));
        } catch (Exception e) {
            setOutput(getString(R.string.studio_rename_failed) + ": " + e.getMessage(), true);
        }
    }

    private void deleteTarget(File target) {
        try {
            if (target == null || !isInsideProject(target)) {
                return;
            }
            FileUtil.deleteFile(target.getAbsolutePath());
            if (currentFile != null && canonicalPath(currentFile).equals(canonicalPath(target))) {
                currentFile = null;
                currentFileEditable = false;
                selectedNodeFile = projectRoot;
                setOutput(getString(R.string.studio_deleted), false);
            }
            rebuildVisibleNodes();
            updateStage();
            updateStatus(getString(R.string.studio_deleted));
        } catch (Exception e) {
            setOutput(getString(R.string.studio_delete_failed) + ": " + e.getMessage(), true);
        }
    }

    private File getResDirectory(String name) {
        return new File(projectRoot, "app" + File.separator + "src" + File.separator + "main" + File.separator + "res" + File.separator + name);
    }

    private File getValuesFile(String name) {
        return new File(getResDirectory("values"), name);
    }

    private String sanitizeResourceFileName(String raw) {
        String value = raw.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9_.]+", "_");
        while (value.startsWith("_") || value.startsWith(".")) {
            value = value.substring(1);
        }
        return value.isEmpty() ? "resource.xml" : value;
    }

    private File findInitialFile(File root) {
        List<File> candidates = new ArrayList<>();
        collectCandidateFiles(root, candidates);
        candidates.sort(Comparator
                .comparingInt(this::priorityForInitialFile)
                .thenComparing(this::relativePath));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private void collectCandidateFiles(File directory, List<File> candidates) {
        if (directory == null || candidates.size() >= MAX_INITIAL_SCAN_FILES) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (shouldSkip(child)) {
                continue;
            }
            if (child.isDirectory()) {
                collectCandidateFiles(child, candidates);
            } else if (canOpen(child)) {
                candidates.add(child);
            }
            if (candidates.size() >= MAX_INITIAL_SCAN_FILES) {
                return;
            }
        }
    }

    private int priorityForInitialFile(File file) {
        String relative = relativePath(file).toLowerCase(Locale.US);
        String name = file.getName().toLowerCase(Locale.US);
        if (name.equals("mainactivity.java")) {
            return 0;
        }
        if (relative.contains("/src/main/java/") && name.endsWith(".java")) {
            return 1;
        }
        if (relative.contains("/src/main/kotlin/") && name.endsWith(".kt")) {
            return 2;
        }
        if (name.equals("androidmanifest.xml")) {
            return 3;
        }
        if (relative.contains("/src/main/res/layout/") && name.endsWith(".xml")) {
            return 4;
        }
        if (name.endsWith(".java") || name.endsWith(".kt")) {
            return 5;
        }
        if (name.endsWith(".xml")) {
            return 6;
        }
        return 7;
    }

    private boolean shouldSkip(File file) {
        if (!file.isDirectory()) {
            return false;
        }
        String name = file.getName();
        return name.equals("build")
                || name.equals(".gradle")
                || name.equals(".git")
                || name.equals(".idea")
                || name.equals(".cxx");
    }

    private boolean canOpen(File file) {
        if (file == null || !file.isFile() || file.length() > MAX_OPEN_BYTES) {
            return false;
        }
        String name = file.getName().toLowerCase(Locale.US);
        return hasKnownTextExtension(name) || isLikelyTextFile(file);
    }

    private boolean hasKnownTextExtension(String name) {
        return name.endsWith(".java")
                || name.endsWith(".kt")
                || name.endsWith(".kts")
                || name.endsWith(".xml")
                || name.endsWith(".gradle")
                || name.endsWith(".html")
                || name.endsWith(".htm")
                || name.endsWith(".css")
                || name.endsWith(".scss")
                || name.endsWith(".js")
                || name.endsWith(".jsx")
                || name.endsWith(".ts")
                || name.endsWith(".tsx")
                || name.endsWith(".svg")
                || name.endsWith(".aidl")
                || name.endsWith(".c")
                || name.endsWith(".cpp")
                || name.endsWith(".h")
                || name.endsWith(".hpp")
                || name.endsWith(".sh")
                || name.endsWith(".bat")
                || name.endsWith(".properties")
                || name.endsWith(".pro")
                || name.endsWith(".txt")
                || name.endsWith(".md")
                || name.endsWith(".json")
                || name.endsWith(".yml")
                || name.endsWith(".yaml")
                || name.endsWith(".toml")
                || name.equals(".gitignore");
    }

    private boolean isLikelyTextFile(File file) {
        if (file.length() == 0) {
            return true;
        }
        int sampleSize = (int) Math.min(2048, file.length());
        byte[] buffer = new byte[sampleSize];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int read = inputStream.read(buffer);
            if (read <= 0) {
                return true;
            }
            int controlChars = 0;
            for (int i = 0; i < read; i++) {
                int value = buffer[i] & 0xFF;
                if (value == 0) {
                    return false;
                }
                boolean isAllowedWhitespace = value == '\n' || value == '\r' || value == '\t';
                if (value < 32 && !isAllowedWhitespace) {
                    controlChars++;
                }
            }
            return controlChars < read / 10;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void expandDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            expandedDirs.add(canonicalPath(directory));
        }
    }

    private boolean isInsideProject(File file) throws IOException {
        if (projectRoot == null || file == null) {
            return false;
        }
        String rootPath = projectRoot.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        return filePath.equals(rootPath) || filePath.startsWith(rootPath + File.separator);
    }

    private String relativePath(File file) {
        if (file == null) {
            return "";
        }
        if (projectRoot == null) {
            return file.getName();
        }
        try {
            return projectRoot.toPath().toAbsolutePath().normalize()
                    .relativize(file.toPath().toAbsolutePath().normalize())
                    .toString()
                    .replace(File.separatorChar, '/');
        } catch (Exception ignored) {
            return file.getName();
        }
    }

    private String parentRelativePath(File file) {
        File parent = file.getParentFile();
        if (parent == null || parent.equals(projectRoot)) {
            return "";
        }
        return relativePath(parent);
    }

    private String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException ignored) {
            return file.getAbsolutePath();
        }
    }

    private int iconFor(File file, boolean directory) {
        if (directory) {
            return R.drawable.ic_mtrl_folder_code;
        }
        String name = file.getName().toLowerCase(Locale.US);
        if (name.endsWith(".java")) {
            return R.drawable.ic_mtrl_java;
        }
        if (name.endsWith(".kt") || name.endsWith(".kts")) {
            return R.drawable.ic_mtrl_kotlin;
        }
        if (name.endsWith(".xml")) {
            return R.drawable.ic_mtrl_interface;
        }
        if (isPreviewableImage(file)) {
            return R.drawable.ic_mtrl_image;
        }
        if (name.endsWith(".json")) {
            return R.drawable.ic_mtrl_code;
        }
        if (name.endsWith(".gradle") || name.endsWith(".properties") || name.endsWith(".toml")) {
            return R.drawable.ic_mtrl_settings;
        }
        if (name.endsWith(".bat") || name.endsWith(".sh")) {
            return R.drawable.ic_mtrl_terminal;
        }
        return R.drawable.ic_mtrl_file;
    }

    private boolean isLayoutXml(File file) {
        if (file == null) {
            return false;
        }
        String relative = relativePath(file).toLowerCase(Locale.US);
        return relative.contains("/src/main/res/layout/") && file.getName().toLowerCase(Locale.US).endsWith(".xml");
    }

    private boolean isPreviewableImage(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".webp")
                || name.endsWith(".gif")
                || name.endsWith(".bmp");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_STUDIO_ICON && resultCode == RESULT_OK && data != null && data.getData() != null) {
            savePickedIcon(data.getData());
        } else if (requestCode == REQUEST_LAYOUT_EDITOR && resultCode == RESULT_OK && currentFile != null && currentFile.isFile()) {
            openFile(currentFile, false);
        }
    }

    private void savePickedIcon(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Could not open selected image");
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IOException("Unsupported image");
            }
            String name = sanitizeResourceFileName(nameFromUri(uri));
            if (name.endsWith(".png")) {
                name = name.substring(0, name.length() - 4);
            }
            if (!name.matches("[a-z][a-z0-9_]*")) {
                name = "icon_" + System.currentTimeMillis();
            }
            File target = new File(getResDirectory("drawable"), name + ".png");
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileOutputStream outputStream = new FileOutputStream(target)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            }
            selectedNodeFile = target;
            expandDirectory(target.getParentFile());
            rebuildVisibleNodes();
            openImageFile(target, true);
            SketchwareUtil.toast(getString(R.string.studio_icon_added));
        } catch (Exception e) {
            setOutput(getString(R.string.studio_icon_failed) + ": " + e.getMessage(), true);
        }
    }

    private String nameFromUri(Uri uri) {
        String raw = uri == null ? "" : uri.getLastPathSegment();
        if (raw == null || raw.trim().isEmpty()) {
            return "icon";
        }
        int slash = raw.lastIndexOf('/');
        if (slash >= 0 && slash < raw.length() - 1) {
            raw = raw.substring(slash + 1);
        }
        int colon = raw.lastIndexOf(':');
        if (colon >= 0 && colon < raw.length() - 1) {
            raw = raw.substring(colon + 1);
        }
        int dot = raw.lastIndexOf('.');
        if (dot > 0) {
            raw = raw.substring(0, dot);
        }
        return raw;
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 1;
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        }
        return String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String valueOf(HashMap<String, Object> map, String key, String fallback) {
        if (map == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isEmpty() ? fallback : text;
    }

    private void closeAfterSave() {
        if (hasUnsavedChanges()) {
            saveCurrentFile(false);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        if (binding.studioDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.studioDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        closeAfterSave();
    }

    @Override
    public void onDestroy() {
        if (binding != null) {
            binding.studioEditor.release();
        }
        super.onDestroy();
    }

    private final class FileTreeAdapter extends RecyclerView.Adapter<FileTreeAdapter.FileNodeHolder> {

        private final List<FileNode> nodes = new ArrayList<>();

        void submit(List<FileNode> newNodes) {
            nodes.clear();
            nodes.addAll(newNodes);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FileNodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemStudioFileTreeBinding itemBinding = ItemStudioFileTreeBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new FileNodeHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull FileNodeHolder holder, int position) {
            FileNode node = nodes.get(position);
            holder.binding.fileName.setText(node.file.getName());
            holder.binding.fileMeta.setText(node.directory ? relativePath(node.file) : parentRelativePath(node.file));
            holder.binding.fileIcon.setImageResource(iconFor(node.file, node.directory));
            holder.binding.fileChevron.setVisibility(node.directory ? View.VISIBLE : View.INVISIBLE);
            holder.binding.fileChevron.setImageResource(
                    expandedDirs.contains(canonicalPath(node.file))
                            ? R.drawable.ic_mtrl_arrow_down
                            : R.drawable.ic_mtrl_chevron_right_24
            );
            holder.binding.treeConnector.setVisibility(node.depth > 0 ? View.VISIBLE : View.INVISIBLE);
            holder.binding.getRoot().setPadding(dp(8 + node.depth * 14), dp(6), dp(8), dp(6));
            holder.binding.getRoot().setAlpha(node.directory || canOpen(node.file) || isPreviewableImage(node.file) ? 1f : 0.55f);
            holder.binding.getRoot().setBackgroundResource(
                    currentFile != null && canonicalPath(currentFile).equals(canonicalPath(node.file))
                            ? R.drawable.bg_studio_file_selected
                            : 0
            );
            holder.binding.getRoot().setOnClickListener(v -> {
                selectedNodeFile = node.file;
                if (node.directory) {
                    String key = canonicalPath(node.file);
                    if (expandedDirs.contains(key)) {
                        expandedDirs.remove(key);
                    } else {
                        expandedDirs.add(key);
                    }
                    rebuildVisibleNodes();
                    return;
                }
                openFile(node.file, true);
            });
        }

        @Override
        public int getItemCount() {
            return nodes.size();
        }

        private final class FileNodeHolder extends RecyclerView.ViewHolder {

            private final ItemStudioFileTreeBinding binding;

            private FileNodeHolder(ItemStudioFileTreeBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private static final class FileNode {

        private final File file;
        private final int depth;
        private final boolean directory;

        private FileNode(File file, int depth, boolean directory) {
            this.file = file;
            this.depth = depth;
            this.directory = directory;
        }
    }
}
