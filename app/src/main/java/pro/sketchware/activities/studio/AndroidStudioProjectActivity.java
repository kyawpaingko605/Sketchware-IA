package pro.sketchware.activities.studio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import mod.hey.studios.code.SrcCodeEditor;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityAndroidStudioProjectBinding;
import pro.sketchware.databinding.ItemStudioFileTreeBinding;
import pro.sketchware.util.ProjectPathResolver;
import pro.sketchware.utility.EditorUtils;
import pro.sketchware.utility.SketchwareUtil;

public class AndroidStudioProjectActivity extends BaseAppCompatActivity {

    public static final String EXTRA_SC_ID = "sc_id";

    private static final int MENU_TREE = 0;
    private static final int MENU_SAVE = 1;
    private static final int MENU_FORMAT = 2;
    private static final int MENU_THEME = 3;
    private static final int MENU_UNDO = 4;
    private static final int MENU_REDO = 5;
    private static final int MENU_BUILD = 6;
    private static final int TAB_CODE = 0;
    private static final int TAB_XML = 1;
    private static final int TAB_OUTPUT = 2;
    private static final int MAX_TREE_NODES = 900;
    private static final int MAX_INITIAL_SCAN_FILES = 1200;
    private static final long MAX_OPEN_BYTES = 1_500_000L;

    private ActivityAndroidStudioProjectBinding binding;
    private FileTreeAdapter fileTreeAdapter;
    private final List<FileNode> visibleNodes = new ArrayList<>();
    private final Set<String> expandedDirs = new HashSet<>();

    private String scId;
    private File projectRoot;
    private File currentFile;
    private String lastSavedContent = "";
    private int discoveredFileCount;
    private boolean changingFile;
    private boolean fileTreeVisible = true;
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
        setupTabs();
        setupEditor();
        setupFileTree();
        loadProject(projectName);
        SketchwareUtil.toast(getString(R.string.studio_project_selected));
    }

    private void setupToolbar(String projectName, String appName) {
        binding.studioToolbar.setTitle(projectName);
        binding.studioToolbar.setSubtitle(appName);
        binding.studioToolbar.setNavigationOnClickListener(v -> closeAfterSave());

        Menu menu = binding.studioToolbar.getMenu();
        addToolbarAction(menu, MENU_TREE, R.string.studio_action_files, R.drawable.ic_mtrl_sidebar);
        addToolbarAction(menu, MENU_UNDO, R.string.studio_action_undo, R.drawable.ic_mtrl_undo);
        addToolbarAction(menu, MENU_REDO, R.string.studio_action_redo, R.drawable.ic_mtrl_redo);
        addToolbarAction(menu, MENU_FORMAT, R.string.studio_action_format, R.drawable.ic_mtrl_formattext);
        addToolbarAction(menu, MENU_THEME, R.string.studio_action_theme, R.drawable.ic_mtrl_palette);
        addToolbarAction(menu, MENU_BUILD, R.string.studio_action_build, R.drawable.ic_mtrl_run);
        addToolbarAction(menu, MENU_SAVE, R.string.studio_action_save, R.drawable.ic_mtrl_save);

        binding.studioToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_TREE) {
                toggleFileTree();
                return true;
            }
            if (item.getItemId() == MENU_SAVE) {
                saveCurrentFile(true);
                return true;
            }
            if (item.getItemId() == MENU_BUILD) {
                buildProject();
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
                    updateXmlPreview();
                }
                return true;
            }
            if (item.getItemId() == MENU_REDO) {
                if (binding.studioEditor.canRedo()) {
                    binding.studioEditor.redo();
                    updateXmlPreview();
                }
                return true;
            }
            return false;
        });
    }

    private void addToolbarAction(Menu menu, int id, int titleRes, int iconRes) {
        MenuItem item = menu.add(Menu.NONE, id, Menu.NONE, titleRes);
        item.setIcon(iconRes);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void setupTabs() {
        binding.studioTabs.addTab(binding.studioTabs.newTab().setText(R.string.studio_tab_code));
        binding.studioTabs.addTab(binding.studioTabs.newTab().setText(R.string.studio_tab_xml));
        binding.studioTabs.addTab(binding.studioTabs.newTab().setText(R.string.studio_tab_output));
        binding.studioTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateStage();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                updateStage();
            }
        });
    }

    private void setupEditor() {
        binding.studioEditor.setTypefaceText(EditorUtils.getTypeface(this));
        binding.studioEditor.setTextSize(14);
        binding.studioEditor.setEditorLanguage(new EmptyLanguage());
        SrcCodeEditor.loadCESettings(this, binding.studioEditor, "studio", true);
        applyDefaultEditorTheme();
    }

    private void setupFileTree() {
        fileTreeAdapter = new FileTreeAdapter();
        binding.studioFileTree.setLayoutManager(new LinearLayoutManager(this));
        binding.studioFileTree.setAdapter(fileTreeAdapter);
    }

    private void loadProject(String projectName) {
        if (scId == null || scId.trim().isEmpty()) {
            showMissingProject();
            return;
        }

        projectRoot = ProjectPathResolver.getAndroidStudioProjectRoot(scId);
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
        appendChildren(projectRoot, 0);
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

    private void openFile(File file, boolean savePrevious) {
        if (file == null || !file.isFile()) {
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
            lastSavedContent = content;
            binding.studioEditor.setText(content);
            applyDefaultEditorTheme();
            applyLanguage(file);
            updateFileHeader(file);
            updateXmlPreview();
            selectTab(TAB_CODE);
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

    private void applyDefaultEditorTheme() {
        SrcCodeEditor.selectTheme(binding.studioEditor, 3);
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
        } catch (Exception ignored) {
            binding.studioEditor.setEditorLanguage(new EmptyLanguage());
        }
    }

    private boolean saveCurrentFile(boolean showToast) {
        if (currentFile == null) {
            return false;
        }
        try {
            if (!isInsideProject(currentFile)) {
                throw new IOException("File is outside the project root");
            }
            String content = binding.studioEditor.getText().toString();
            Files.write(currentFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
            lastSavedContent = content;
            updateXmlPreview();
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
            updateXmlPreview();
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

        selectTab(TAB_OUTPUT);
        setOutput(getString(R.string.studio_build_started) + "...\n" + projectRoot.getAbsolutePath(), false);
        setBuildRunning(true);

        new Thread(() -> {
            try {
                File apk = runStudioBuild();
                appendBuildOutput("");
                appendBuildOutput(getString(R.string.studio_build_finished) + ": " + apk.getAbsolutePath());
                runOnUiThread(() -> SketchwareUtil.toast(getString(R.string.studio_build_finished)));
            } catch (Throwable throwable) {
                appendBuildOutput("");
                appendBuildOutput(getString(R.string.studio_build_failed) + ": " + throwable.getMessage());
                appendBuildOutput(Log.getStackTraceString(throwable));
                runOnUiThread(() -> SketchwareUtil.toast(getString(R.string.studio_build_failed)));
            } finally {
                runOnUiThread(() -> setBuildRunning(false));
            }
        }).start();
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

    private void updateXmlPreview() {
        if (currentFile == null) {
            binding.studioXmlPreview.setText(getString(R.string.studio_no_file_message));
            return;
        }
        String name = currentFile.getName().toLowerCase(Locale.US);
        if (!name.endsWith(".xml")) {
            binding.studioXmlPreview.setText(getString(R.string.studio_no_xml_preview));
            return;
        }
        String xml = binding.studioEditor.getText().toString();
        String formatted = SrcCodeEditor.prettifyXml(xml, 4, null);
        binding.studioXmlPreview.setText(formatted == null ? xml : formatted);
    }

    private void updateStage() {
        int selectedTab = Math.max(binding.studioTabs.getSelectedTabPosition(), TAB_CODE);
        boolean hasFile = currentFile != null;
        boolean showCode = hasFile && selectedTab == TAB_CODE;
        boolean showXml = hasFile && selectedTab == TAB_XML;
        boolean showOutput = selectedTab == TAB_OUTPUT;
        boolean showEmpty = !hasFile && !showOutput;

        binding.studioEditor.setVisibility(showCode ? View.VISIBLE : View.GONE);
        binding.studioXmlContainer.setVisibility(showXml ? View.VISIBLE : View.GONE);
        binding.studioOutputContainer.setVisibility(showOutput ? View.VISIBLE : View.GONE);
        binding.studioEmptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }

    private void setOutput(String message, boolean selectOutput) {
        binding.studioOutput.setText(message);
        if (selectOutput && !changingFile) {
            selectTab(TAB_OUTPUT);
        }
    }

    private void selectTab(int position) {
        TabLayout.Tab tab = binding.studioTabs.getTabAt(position);
        if (tab != null && !tab.isSelected()) {
            tab.select();
        }
        updateStage();
    }

    private void updateStatus(String status) {
        binding.studioStatus.setText(status);
    }

    private boolean hasUnsavedChanges() {
        return currentFile != null && !lastSavedContent.equals(binding.studioEditor.getText().toString());
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

    private void toggleFileTree() {
        fileTreeVisible = !fileTreeVisible;
        binding.studioFilePanel.setVisibility(fileTreeVisible ? View.VISIBLE : View.GONE);
        updateStatus(fileTreeVisible ? "Project tree visible" : "Project tree hidden");
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
        if (name.endsWith(".gradle") || name.endsWith(".properties") || name.endsWith(".toml")) {
            return R.drawable.ic_mtrl_settings;
        }
        return R.drawable.ic_mtrl_file;
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
            holder.binding.getRoot().setPadding(dp(8 + node.depth * 14), dp(6), dp(8), dp(6));
            holder.binding.getRoot().setAlpha(node.directory || canOpen(node.file) ? 1f : 0.55f);
            holder.binding.getRoot().setBackgroundResource(
                    currentFile != null && canonicalPath(currentFile).equals(canonicalPath(node.file))
                            ? R.drawable.bg_studio_file_selected
                            : 0
            );
            holder.binding.getRoot().setOnClickListener(v -> {
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
