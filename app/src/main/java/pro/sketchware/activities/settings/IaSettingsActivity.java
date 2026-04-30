package pro.sketchware.activities.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pro.sketchware.R;
import pro.sketchware.activities.chat.AiChatSettingsHelper;
import pro.sketchware.activities.chat.port.VoidPortRefreshModelService;
import pro.sketchware.activities.chat.port.VoidPortSettings;
import pro.sketchware.databinding.ActivityIaSettingsBinding;
import pro.sketchware.utility.TranslationFunction;

public class IaSettingsActivity extends BaseAppCompatActivity {

    private static final String PREFS_NAME = VoidPortSettings.PREFS_NAME;

    private static final String SECTION_MODELS = "models";
    private static final String SECTION_LOCAL_PROVIDERS = "local_providers";
    private static final String SECTION_MAIN_PROVIDERS = "main_providers";
    private static final String SECTION_FEATURE_OPTIONS = "feature_options";
    private static final String SECTION_MCP = "mcp";

    private static final String PREF_CURRENT_PROVIDER = VoidPortSettings.PREF_CURRENT_PROVIDER;
    private static final String PREF_CURRENT_MODEL = VoidPortSettings.PREF_CURRENT_MODEL;
    private static final String PREF_CUSTOM_MODELS = VoidPortSettings.PREF_CUSTOM_MODELS;
    private static final String PREF_MCP_CONFIG = VoidPortSettings.PREF_MCP_CONFIG;
    private static final String DEFAULT_MCP_CONFIG = VoidPortSettings.DEFAULT_MCP_CONFIG;

    private ActivityIaSettingsBinding binding;
    private SharedPreferences prefs;
    private final Map<String, MaterialButton> menuButtons = new LinkedHashMap<>();
    private final Map<String, View> sectionViews = new LinkedHashMap<>();
    private LinearLayout mcpServersContainer;
    private final OnBackPressedCallback closeDrawerCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (binding != null) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);

        binding = ActivityIaSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        getOnBackPressedDispatcher().addCallback(this, closeDrawerCallback);

        setupToolbar();
        setupInsets();
        setupDrawer();
        setupHeader();
        bindSections();
        setupMenu();
        buildAllSections();
        VoidPortRefreshModelService.startAutoRefresh(this);
        selectSection(SECTION_MODELS);
    }

    @Override
    public android.content.res.Resources getResources() {
        return TranslationFunction.wrapResources(this, super.getResources());
    }

    private void setupToolbar() {
        binding.topAppBar.setTitle(R.string.ia_settings_title);
        binding.topAppBar.setNavigationOnClickListener(v -> {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void setupInsets() {
        {
            View view = binding.appBarLayout;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + systemInsets.left, top + systemInsets.top, right + systemInsets.right, bottom);
                return insets;
            });
        }

        {
            View view = binding.settingsBody;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(left + systemInsets.left, top, right + systemInsets.right, bottom + systemInsets.bottom);
                return insets;
            });
        }

        {
            View view = binding.menuScroll;
            int left = view.getPaddingLeft();
            int top = view.getPaddingTop();
            int right = view.getPaddingRight();
            int bottom = view.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(left + systemInsets.left, top + systemInsets.top, right, bottom + systemInsets.bottom);
                return insets;
            });
        }
    }

    private void setupDrawer() {
        binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                closeDrawerCallback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                closeDrawerCallback.setEnabled(false);
            }
        });
    }

    private void setupHeader() {
        Locale locale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? getResources().getConfiguration().getLocales().get(0)
                : getResources().getConfiguration().locale;
        String deviceLanguageName = locale.getDisplayLanguage(locale);
        binding.tvDeviceLanguageNotice.setText(getString(R.string.ia_device_language_notice_template, deviceLanguageName));
    }

    private void bindSections() {
        menuButtons.put(SECTION_MODELS, binding.btnMenuModels);
        menuButtons.put(SECTION_LOCAL_PROVIDERS, binding.btnMenuLocalProviders);
        menuButtons.put(SECTION_MAIN_PROVIDERS, binding.btnMenuMainProviders);
        menuButtons.put(SECTION_FEATURE_OPTIONS, binding.btnMenuFeatureOptions);
        menuButtons.put(SECTION_MCP, binding.btnMenuMcp);

        sectionViews.put(SECTION_MODELS, binding.sectionModels);
        sectionViews.put(SECTION_LOCAL_PROVIDERS, binding.sectionLocalProviders);
        sectionViews.put(SECTION_MAIN_PROVIDERS, binding.sectionMainProviders);
        sectionViews.put(SECTION_FEATURE_OPTIONS, binding.sectionFeatureOptions);
        sectionViews.put(SECTION_MCP, binding.sectionMcp);
    }

    private void setupMenu() {
        for (Map.Entry<String, MaterialButton> entry : menuButtons.entrySet()) {
            String sectionKey = entry.getKey();
            MaterialButton button = entry.getValue();
            button.setOnClickListener(v -> selectSection(sectionKey));
        }
    }

    private void buildAllSections() {
        buildModelsSection();
        buildLocalProvidersSection();
        buildMainProvidersSection();
        buildFeatureOptionsSection();
        buildMcpSection();
        ensureValidCurrentSelection();
    }

    private void selectSection(@NonNull String sectionKey) {
        for (Map.Entry<String, View> entry : sectionViews.entrySet()) {
            entry.getValue().setVisibility(entry.getKey().equals(sectionKey) ? View.VISIBLE : View.GONE);
        }
        updateMenuState(sectionKey);
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
        binding.contentScroll.post(() -> binding.contentScroll.smoothScrollTo(0, 0));
    }

    private void updateMenuState(@NonNull String selectedSection) {
        int selectedBg = MaterialColors.getColor(binding.getRoot(), R.attr.colorPrimary, 0);
        int selectedText = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnPrimary, 0);
        int normalBg = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorSurfaceContainerHigh, 0);
        int normalText = MaterialColors.getColor(binding.getRoot(), com.google.android.material.R.attr.colorOnSurface, 0);

        for (Map.Entry<String, MaterialButton> entry : menuButtons.entrySet()) {
            boolean isSelected = entry.getKey().equals(selectedSection);
            MaterialButton button = entry.getValue();
            button.setBackgroundTintList(ColorStateList.valueOf(isSelected ? selectedBg : normalBg));
            button.setTextColor(isSelected ? selectedText : normalText);
        }
    }

    private void buildModelsSection() {
        LinearLayout container = binding.sectionModels;
        container.removeAllViews();

        addSectionHeader(
                container,
                "Models",
                "Show or hide the models that should appear in Sketchware IA. As in Void, provider credentials are configured in Main Providers and local endpoints stay in Local Providers."
        );

        for (ProviderGroup group : getAllProviderGroups()) {
            addModelGroup(container, group);
        }

        MaterialButton addModelButton = createTextButton("Add a model");
        addModelButton.setOnClickListener(v -> showAddModelDialog());
        container.addView(addModelButton);

        MaterialCardView localCard = createCard();
        LinearLayout content = createCardContent(localCard);
        content.addView(createSubheading("Local model discovery"));
        content.addView(createMutedText("These options mirror the Void-style setup for local engines such as Ollama, vLLM, and LM Studio."));

        MaterialSwitch autoDetectSwitch = createSwitch(
                "Automatically detect local providers and models (Ollama, vLLM, LM Studio).",
                "local_auto_detect_models",
                true
        );
        content.addView(autoDetectSwitch);
        MaterialButton refreshOllama = createTextButton("Refresh Ollama models");
        refreshOllama.setOnClickListener(v -> refreshLocalModels("ollama"));
        MaterialButton refreshVllm = createTextButton("Refresh vLLM models");
        refreshVllm.setOnClickListener(v -> refreshLocalModels("vllm"));
        MaterialButton refreshLmStudio = createTextButton("Refresh LM Studio models");
        refreshLmStudio.setOnClickListener(v -> refreshLocalModels("lm_studio"));
        content.addView(createActionButtonRow(refreshOllama, refreshVllm, refreshLmStudio));
        container.addView(localCard);
    }

    private void addModelGroup(LinearLayout parent, ProviderGroup group) {
        TextView title = createGroupLabel(group.label);
        parent.addView(title);

        if (!isProviderConfigured(group.providerId)) {
            String sectionLabel = group.localProvider ? "Local Providers" : "Main Providers";
            TextView setupText = createMutedText("Configure this provider in " + sectionLabel + " to enable its models.");
            LinearLayout.LayoutParams params = defaultRowLayoutParams();
            params.bottomMargin = dp(8);
            setupText.setLayoutParams(params);
            parent.addView(setupText);
        }

        for (String model : group.models) {
            parent.addView(createModelRow(group, model));
        }
    }

    private View createModelRow(ProviderGroup group, String model) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textColumn.setLayoutParams(textParams);

        TextView modelView = new TextView(this);
        modelView.setText(model);
        modelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        textColumn.addView(modelView);

        boolean providerConfigured = isProviderConfigured(group.providerId);
        boolean hidden = isModelHidden(group.providerId, model);

        if (!providerConfigured) {
            TextView disabledLabel = createMutedText(group.localProvider
                    ? "Configure the local endpoint first"
                    : "Add the provider credentials first");
            textColumn.addView(disabledLabel);
        }

        row.addView(textColumn);

        MaterialSwitch favoriteSwitch = new MaterialSwitch(this);
        favoriteSwitch.setChecked(providerConfigured && !hidden);
        favoriteSwitch.setEnabled(providerConfigured);
        favoriteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setModelHidden(group.providerId, model, !isChecked);
            ensureValidCurrentSelection();
        });
        row.addView(favoriteSwitch);

        row.setAlpha(providerConfigured ? 1f : 0.6f);

        return row;
    }

    private void buildLocalProvidersSection() {
        LinearLayout container = binding.sectionLocalProviders;
        container.removeAllViews();

        addSectionHeader(
                container,
                "Local Providers",
                "Configure the local engines you want Sketchware IA to detect and query. These values stay on your device."
        );

        container.addView(createTextFieldCard(
                "vLLM",
                "Endpoint",
                "local_provider_vllm_url",
                "http://localhost:8000",
                false,
                null,
                "Use the OpenAI-compatible endpoint exposed by your vLLM instance."
        ));

        container.addView(createTextFieldCard(
                "LM Studio",
                "Endpoint",
                "local_provider_lm_studio_url",
                "http://localhost:1234",
                false,
                null,
                "Point this to the local server exposed by LM Studio."
        ));
    }

    private void buildMainProvidersSection() {
        LinearLayout container = binding.sectionMainProviders;
        container.removeAllViews();

        addSectionHeader(
                container,
                "Main Providers",
                "Configure remote providers, API keys, and compatibility endpoints. Supported chat providers already stay wired to the existing backend keys."
        );

        for (VoidPortSettings.ProviderCardSpec provider : VoidPortSettings.getProviderCards()) {
            container.addView(createProviderCard(provider));
        }
    }

    private void buildFeatureOptionsSection() {
        LinearLayout container = binding.sectionFeatureOptions;
        container.removeAllViews();

        addSectionHeader(
                container,
                "Feature Options",
                "Fine-tune the way suggestions, applies, and agent tools behave in the editor."
        );

        container.addView(createTextFieldCard(
                "Custom AI Instructions",
                "Instructions",
                VoidPortSettings.PREF_AI_INSTRUCTIONS,
                "",
                false,
                null,
                "These instructions are injected into the chat system context through the ported Void settings layer."
        ));

        MaterialCardView autocompleteCard = createCard();
        LinearLayout autocompleteContent = createCardContent(autocompleteCard);
        autocompleteContent.addView(createSubheading("Autocomplete"));
        autocompleteContent.addView(createSwitch(
                "Enable autocomplete",
                VoidPortSettings.PREF_ENABLE_AUTOCOMPLETE,
                false
        ));
        autocompleteContent.addView(createMutedText("Experimental. Works best when your selected model supports completion-style behavior."));
        container.addView(autocompleteCard);

        MaterialCardView applyCard = createCard();
        LinearLayout applyContent = createCardContent(applyCard);
        applyContent.addView(createSubheading("Apply"));
        applyContent.addView(createSwitch(
                "Use the same model as chat",
                VoidPortSettings.PREF_SYNC_APPLY_TO_CHAT,
                true
        ));
        MaterialAutoCompleteTextView applyModeInput = createDropdown(
                List.of(VoidPortSettings.APPLY_MODE_FAST, VoidPortSettings.APPLY_MODE_BALANCED, VoidPortSettings.APPLY_MODE_CAREFUL),
                prefs.getString(VoidPortSettings.PREF_APPLY_MODE, VoidPortSettings.APPLY_MODE_FAST)
        );
        TextInputLayout applyModeLayout = createDropdownLayout("Apply mode", applyModeInput);
        applyModeInput.setOnItemClickListener((parentView, view, position, id) -> prefs.edit()
                .putString(VoidPortSettings.PREF_APPLY_MODE, applyModeInput.getText().toString())
                .apply());
        applyContent.addView(applyModeLayout);
        container.addView(applyCard);

        MaterialCardView toolsCard = createCard();
        LinearLayout toolsContent = createCardContent(toolsCard);
        toolsContent.addView(createSubheading("Tools"));
        toolsContent.addView(createSwitch("Auto-approve edits", VoidPortSettings.PREF_TOOLS_AUTO_APPROVE_EDITS, true));
        toolsContent.addView(createSwitch("Auto-approve terminal", VoidPortSettings.PREF_TOOLS_AUTO_APPROVE_TERMINAL, true));
        toolsContent.addView(createSwitch("Auto-approve MCP tools", VoidPortSettings.PREF_TOOLS_AUTO_APPROVE_MCP, false));
        toolsContent.addView(createSwitch("Fix lint errors", VoidPortSettings.PREF_INCLUDE_TOOL_LINT_ERRORS, true));
        toolsContent.addView(createSwitch("Auto-accept LLM changes", VoidPortSettings.PREF_AUTO_ACCEPT_LLM_CHANGES, false));
        container.addView(toolsCard);

        MaterialCardView editorCard = createCard();
        LinearLayout editorContent = createCardContent(editorCard);
        editorContent.addView(createSubheading("Editor"));
        editorContent.addView(createSwitch("Show suggestions on select", VoidPortSettings.PREF_SHOW_INLINE_SUGGESTIONS, true));
        container.addView(editorCard);

        MaterialCardView commitCard = createCard();
        LinearLayout commitContent = createCardContent(commitCard);
        commitContent.addView(createSubheading("Commit Message Generator"));
        commitContent.addView(createSwitch("Use the same model as chat", VoidPortSettings.PREF_SYNC_SCM_TO_CHAT, true));
        container.addView(commitCard);

        MaterialCardView portCard = createCard();
        LinearLayout portContent = createCardContent(portCard);
        portContent.addView(createSubheading("Void Portability"));
        portContent.addView(createMutedText("Controls the Java port layer in pro.sketchware.activities.chat.port. The raw source assets stay in source; these switches decide what becomes active behavior."));
        portContent.addView(createSwitch("Use ported Void source registry", VoidPortSettings.PREF_PORT_SOURCE_ENABLED, true));
        portContent.addView(createSwitch("Use ported Void settings in chat", VoidPortSettings.PREF_PORT_SETTINGS_ENABLED, true));
        portContent.addView(createSwitch("Use ported Void prompt rules", VoidPortSettings.PREF_PORT_PROMPTS_ENABLED, true));
        portContent.addView(createSwitch("Use ported Void tool approval policy", VoidPortSettings.PREF_PORT_TOOL_POLICY_ENABLED, true));
        portContent.addView(createSwitch("Disable extra Void system message", VoidPortSettings.PREF_DISABLE_SYSTEM_MESSAGE, false));
        container.addView(portCard);
    }

    private void buildMcpSection() {
        LinearLayout container = binding.sectionMcp;
        container.removeAllViews();

        addSectionHeader(
                container,
                "MCP",
                "Configure MCP the same way Void does: keep your servers in an mcp.json-style config and enable or disable entries below."
        );

        MaterialCardView card = createCard();
        LinearLayout content = createCardContent(card);

        content.addView(createMutedText("Sketchware IA stores this config locally on the device using the same root shape as Void:\n{\n  \"mcpServers\": {}\n}"));

        MaterialButton openConfigButton = createFilledButton("Open mcp.json");
        openConfigButton.setOnClickListener(v -> showEditMcpConfigDialog());
        content.addView(openConfigButton);

        mcpServersContainer = new LinearLayout(this);
        mcpServersContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(12);
        mcpServersContainer.setLayoutParams(params);

        container.addView(card);
        container.addView(mcpServersContainer);
        renderMcpServers();
    }

    private void renderMcpServers() {
        if (mcpServersContainer == null) {
            return;
        }

        mcpServersContainer.removeAllViews();
        JSONObject servers = readMcpServersObject();
        if (servers.length() == 0) {
            mcpServersContainer.addView(createMutedText("No servers found"));
            return;
        }

        JSONArray names = servers.names();
        if (names == null) {
            mcpServersContainer.addView(createMutedText("No servers found"));
            return;
        }

        for (int i = 0; i < names.length(); i++) {
            String serverName = names.optString(i, "");
            JSONObject server = servers.optJSONObject(serverName);
            if (serverName.isEmpty() || server == null) {
                continue;
            }
            mcpServersContainer.addView(createMcpServerView(serverName, server));
        }
    }

    private View createMcpServerView(String serverName, JSONObject server) {
        MaterialCardView card = createCard();
        LinearLayout content = createCardContent(card);

        TextView title = createSubheading(serverName);
        content.addView(title);

        MaterialSwitch enabledSwitch = new MaterialSwitch(this);
        enabledSwitch.setText("Enabled");
        enabledSwitch.setLayoutParams(defaultRowLayoutParams());
        enabledSwitch.setChecked(server.optBoolean("enabled", true));
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateMcpServerEnabled(serverName, isChecked));
        content.addView(enabledSwitch);

        String command = server.optString("command", "");
        if (!command.isEmpty()) {
            content.addView(createMutedText(command));
        }

        JSONArray args = server.optJSONArray("args");
        if (args != null && args.length() > 0) {
            content.addView(createMutedText(args.toString()));
        }

        String cwd = server.optString("cwd", "");
        if (!cwd.isEmpty()) {
            content.addView(createMutedText("cwd: " + cwd));
        }

        MaterialButton deleteButton = createTextButton("Remove");
        deleteButton.setOnClickListener(v -> removeMcpServer(serverName));
        content.addView(deleteButton);
        return card;
    }

    private void showEditMcpConfigDialog() {
        LinearLayout dialogContent = new LinearLayout(this);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setPadding(dp(24), dp(8), dp(24), 0);

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint("mcp.json");
        inputLayout.setLayoutParams(defaultInputLayoutParams());

        TextInputEditText input = new TextInputEditText(this);
        input.setMinLines(12);
        input.setMaxLines(20);
        input.setTypeface(android.graphics.Typeface.MONOSPACE);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setText(readMcpConfig());
        inputLayout.addView(input);
        dialogContent.addView(inputLayout);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit mcp.json")
                .setView(dialogContent)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String raw = textOf(input);
                    if (raw.isEmpty()) {
                        Toast.makeText(this, "mcp.json cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        JSONObject config = new JSONObject(raw);
                        if (config.optJSONObject("mcpServers") == null) {
                            Toast.makeText(this, "mcp.json must contain an object named mcpServers.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        prefs.edit().putString(PREF_MCP_CONFIG, config.toString(2)).apply();
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    renderMcpServers();
                })
                .show();
    }

    private void showAddModelDialog() {
        LinearLayout dialogContent = new LinearLayout(this);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setPadding(dp(24), dp(8), dp(24), 0);

        List<ProviderGroup> providerGroups = getCatalogProviderGroups();
        List<String> providerLabels = new ArrayList<>();
        for (ProviderGroup group : providerGroups) {
            providerLabels.add(group.label);
        }

        MaterialAutoCompleteTextView providerInput = createDropdown(providerLabels, providerLabels.isEmpty() ? "" : providerLabels.get(0));
        TextInputLayout providerLayout = createDropdownLayout("Provider", providerInput);
        providerLayout.setLayoutParams(defaultInputLayoutParams());
        dialogContent.addView(providerLayout);

        TextInputEditText modelInput = createTextInput(dialogContent, "Model id", "my-model");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add model")
                .setView(dialogContent)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Add", (dialog, which) -> {
                    String providerLabel = providerInput.getText() == null ? "" : providerInput.getText().toString().trim();
                    String model = textOf(modelInput);
                    if (providerLabel.isEmpty() || model.isEmpty()) {
                        Toast.makeText(this, "Provider and model fields are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ProviderGroup selectedProvider = null;
                    for (ProviderGroup group : providerGroups) {
                        if (group.label.equalsIgnoreCase(providerLabel)) {
                            selectedProvider = group;
                            break;
                        }
                    }
                    if (selectedProvider == null) {
                        Toast.makeText(this, "Select a valid provider.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (ProviderGroup group : getAllProviderGroups()) {
                        if (selectedProvider.providerId.equals(group.providerId) && group.models.contains(model)) {
                            Toast.makeText(this, "This model already exists.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    JSONArray models = readJsonArrayPreference(PREF_CUSTOM_MODELS);
                    JSONObject customModel = new JSONObject();
                    try {
                        customModel.put("providerLabel", selectedProvider.label);
                        customModel.put("providerId", selectedProvider.providerId);
                        customModel.put("model", model);
                        models.put(customModel);
                        prefs.edit().putString(PREF_CUSTOM_MODELS, models.toString()).apply();
                    } catch (Exception ignored) {
                    }
                    buildModelsSection();
                })
                .show();
    }

    private void removeMcpServer(String serverName) {
        JSONObject config = readMcpConfigObject();
        JSONObject servers = config.optJSONObject("mcpServers");
        if (servers == null) {
            return;
        }
        servers.remove(serverName);
        saveMcpConfig(config);
        renderMcpServers();
    }

    private void updateMcpServerEnabled(String serverName, boolean enabled) {
        JSONObject config = readMcpConfigObject();
        JSONObject servers = config.optJSONObject("mcpServers");
        if (servers == null) {
            return;
        }
        JSONObject item = servers.optJSONObject(serverName);
        if (item == null) {
            return;
        }
        try {
            item.put("enabled", enabled);
            saveMcpConfig(config);
        } catch (Exception ignored) {
        }
    }

    private MaterialCardView createTextFieldCard(
            String title,
            String fieldLabel,
            String prefKey,
            String defaultValue,
            boolean password,
            @Nullable String enabledKey,
            @Nullable String footer
    ) {
        MaterialCardView card = createCard();
        LinearLayout content = createCardContent(card);
        content.addView(createSubheading(title));
        content.addView(createPreferenceInput(fieldLabel, prefKey, defaultValue, password, enabledKey));
        if (footer != null && !footer.isEmpty()) {
            content.addView(createMutedText(footer));
        }
        return card;
    }

    private MaterialCardView createProviderCard(VoidPortSettings.ProviderCardSpec spec) {
        MaterialCardView card = createCard();
        LinearLayout content = createCardContent(card);
        content.addView(createSubheading(spec.title));
        if (spec.description != null && !spec.description.isEmpty()) {
            content.addView(createMutedText(spec.description));
        }
        for (VoidPortSettings.FieldSpec field : spec.fields) {
            content.addView(createPreferenceInput(
                    field.label,
                    field.prefKey,
                    field.defaultValue,
                    field.password,
                    field.enabledKey
            ));
        }
        if (spec.helpUrl != null && !spec.helpUrl.isEmpty()) {
            MaterialButton button = createTextButton("Open provider page");
            button.setOnClickListener(v -> openUrl(spec.helpUrl));
            content.addView(button);
        }
        return card;
    }

    private View createPreferenceInput(
            String hint,
            String prefKey,
            String defaultValue,
            boolean password,
            @Nullable String enabledKey
    ) {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setHint(hint);
        inputLayout.setLayoutParams(defaultInputLayoutParams());
        if (password) {
            inputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }

        TextInputEditText editText = new TextInputEditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        if (password) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        editText.setText(prefs.getString(prefKey, defaultValue));
        editText.addTextChangedListener(saveWatcher(prefKey, enabledKey));
        inputLayout.addView(editText);
        return inputLayout;
    }

    private MaterialSwitch createSwitch(String label, String prefKey, boolean defaultValue) {
        MaterialSwitch switchMaterial = new MaterialSwitch(this);
        switchMaterial.setText(label);
        switchMaterial.setChecked(prefs.getBoolean(prefKey, defaultValue));
        switchMaterial.setLayoutParams(defaultRowLayoutParams());
        switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean(prefKey, isChecked).apply());
        return switchMaterial;
    }

    private MaterialButton createFilledButton(String text) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setLayoutParams(defaultRowLayoutParams());
        return button;
    }

    private MaterialButton createTextButton(String text) {
        MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        button.setAllCaps(false);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setLayoutParams(defaultRowLayoutParams());
        return button;
    }

    private LinearLayout createActionButtonRow(MaterialButton... buttons) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(defaultRowLayoutParams());
        for (MaterialButton button : buttons) {
            if (!button.hasOnClickListeners()) {
                button.setOnClickListener(v -> Toast.makeText(this, button.getText() + " requested.", Toast.LENGTH_SHORT).show());
            }
            row.addView(button);
        }
        return row;
    }

    private void refreshLocalModels(String providerId) {
        Toast.makeText(this, "Refreshing " + providerId + " models...", Toast.LENGTH_SHORT).show();
        VoidPortRefreshModelService.refreshProviderAsync(this, providerId, true, result -> {
            buildModelsSection();
            ensureValidCurrentSelection();
            if (result.state == VoidPortRefreshModelService.RefreshState.FINISHED) {
                Toast.makeText(this,
                        "Found " + result.models.size() + " " + result.providerId + " model(s).",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Refresh failed: " + result.error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private MaterialCardView createCard() {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(16);
        card.setLayoutParams(params);
        card.setRadius(dp(8));
        card.setCardBackgroundColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurfaceContainerLow, 0));
        card.setStrokeColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutlineVariant, 0));
        card.setStrokeWidth(dp(1));
        return card;
    }

    private LinearLayout createCardContent(MaterialCardView card) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(content);
        return content;
    }

    private void addSectionHeader(LinearLayout container, String title, String subtitle) {
        TextView titleView = new TextView(this);
        titleView.setText(title);
        TextViewCompat.setTextAppearance(titleView, com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall);
        container.addView(titleView);

        TextView subtitleView = createMutedText(subtitle);
        LinearLayout.LayoutParams subtitleParams = defaultRowLayoutParams();
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(20);
        subtitleView.setLayoutParams(subtitleParams);
        container.addView(subtitleView);
    }

    private TextView createSubheading(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        TextViewCompat.setTextAppearance(textView, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        textView.setLayoutParams(defaultRowLayoutParams());
        return textView;
    }

    private TextView createGroupLabel(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        TextViewCompat.setTextAppearance(textView, com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        LinearLayout.LayoutParams params = defaultRowLayoutParams();
        params.topMargin = dp(4);
        params.bottomMargin = dp(6);
        textView.setLayoutParams(params);
        return textView;
    }

    private TextView createMutedText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setTextColor(MaterialColors.getColor(textView, com.google.android.material.R.attr.colorOnSurfaceVariant, 0));
        textView.setLayoutParams(defaultRowLayoutParams());
        return textView;
    }

    private void addBodyText(LinearLayout container, String text) {
        TextView body = createMutedText(text);
        LinearLayout.LayoutParams params = defaultRowLayoutParams();
        params.bottomMargin = dp(16);
        body.setLayoutParams(params);
        container.addView(body);
    }

    private MaterialAutoCompleteTextView createDropdown(List<String> items, String selectedValue) {
        MaterialAutoCompleteTextView input = new MaterialAutoCompleteTextView(this);
        input.setSimpleItems(items.toArray(new String[0]));
        input.setText(selectedValue, false);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return input;
    }

    private TextInputLayout createDropdownLayout(String hint, MaterialAutoCompleteTextView input) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        layout.setLayoutParams(defaultInputLayoutParams());
        layout.addView(input);
        return layout;
    }

    private TextInputEditText createTextInput(LinearLayout parent, String hint, String defaultValue) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setLayoutParams(defaultInputLayoutParams());

        TextInputEditText input = new TextInputEditText(this);
        input.setText(defaultValue);
        layout.addView(input);
        parent.addView(layout);
        return input;
    }

    private LinearLayout.LayoutParams defaultRowLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(10);
        return params;
    }

    private LinearLayout.LayoutParams defaultInputLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        return params;
    }

    private TextWatcher saveWatcher(String key, @Nullable String enabledKey) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                SharedPreferences.Editor editor = prefs.edit().putString(key, s.toString());
                if (enabledKey != null) {
                    editor.putBoolean(enabledKey, s != null && s.toString().trim().length() > 0);
                }
                editor.apply();
                if (sectionViews.containsKey(SECTION_MODELS)) {
                    buildModelsSection();
                    ensureValidCurrentSelection();
                }
            }
        };
    }

    private JSONArray readJsonArrayPreference(String key) {
        String raw = prefs.getString(key, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private String readMcpConfig() {
        JSONObject config = readMcpConfigObject();
        try {
            return config.toString(2);
        } catch (Exception ignored) {
            return DEFAULT_MCP_CONFIG;
        }
    }

    private JSONObject readMcpConfigObject() {
        String raw = prefs.getString(PREF_MCP_CONFIG, DEFAULT_MCP_CONFIG);
        try {
            JSONObject config = new JSONObject(raw);
            if (config.optJSONObject("mcpServers") == null) {
                config.put("mcpServers", new JSONObject());
            }
            return config;
        } catch (Exception ignored) {
            try {
                return new JSONObject(DEFAULT_MCP_CONFIG);
            } catch (Exception impossible) {
                return new JSONObject();
            }
        }
    }

    private JSONObject readMcpServersObject() {
        JSONObject config = readMcpConfigObject();
        JSONObject servers = config.optJSONObject("mcpServers");
        return servers == null ? new JSONObject() : servers;
    }

    private void saveMcpConfig(JSONObject config) {
        try {
            prefs.edit().putString(PREF_MCP_CONFIG, config.toString(2)).apply();
        } catch (Exception ignored) {
        }
    }

    private List<ProviderGroup> getCatalogProviderGroups() {
        List<ProviderGroup> groups = new ArrayList<>();
        for (VoidPortSettings.ProviderGroup group : VoidPortSettings.getCatalogProviderGroups()) {
            groups.add(new ProviderGroup(
                    group.providerId,
                    group.label,
                    group.localProvider,
                    new ArrayList<>(group.models)
            ));
        }
        return groups;
    }

    private List<ProviderGroup> getCustomProviderGroups() {
        List<ProviderGroup> groups = new ArrayList<>();
        for (VoidPortSettings.ProviderGroup group : VoidPortSettings.getCustomProviderGroups(prefs)) {
            groups.add(new ProviderGroup(
                    group.providerId,
                    group.label,
                    group.localProvider,
                    new ArrayList<>(group.models)
            ));
        }
        return groups;
    }

    private boolean isModelHidden(String providerId, String model) {
        return VoidPortSettings.isModelHidden(prefs, providerId, model);
    }

    private void setModelHidden(String providerId, String model, boolean hidden) {
        VoidPortSettings.setModelHidden(prefs, providerId, model, hidden);
    }

    private boolean isProviderConfigured(String providerId) {
        return VoidPortSettings.isProviderConfigured(prefs, providerId);
    }

    private boolean isLocalProvider(String providerId) {
        return VoidPortSettings.isLocalProvider(providerId);
    }

    private void ensureValidCurrentSelection() {
        String currentProvider = prefs.getString(PREF_CURRENT_PROVIDER, "");
        String currentModel = prefs.getString(PREF_CURRENT_MODEL, "");
        if (AiChatSettingsHelper.isCurrentSelectionValid(prefs, currentProvider, currentModel)) {
            return;
        }

        List<AiChatSettingsHelper.ModelOption> options = AiChatSettingsHelper.getVisibleModelOptions(prefs);
        if (options.isEmpty()) {
            return;
        }

        AiChatSettingsHelper.ModelOption fallback = options.get(0);
        prefs.edit()
                .putString(PREF_CURRENT_PROVIDER, fallback.providerId)
                .putString(PREF_CURRENT_MODEL, fallback.model)
                .apply();
    }

    private List<ProviderGroup> getAllProviderGroups() {
        List<ProviderGroup> groups = new ArrayList<>(getCatalogProviderGroups());
        for (ProviderGroup customGroup : getCustomProviderGroups()) {
            boolean merged = false;
            for (ProviderGroup group : groups) {
                if (!group.providerId.equals(customGroup.providerId)) {
                    continue;
                }
                for (String model : customGroup.models) {
                    if (!group.models.contains(model)) {
                        group.models.add(model);
                    }
                }
                merged = true;
                break;
            }
            if (!merged) {
                groups.add(customGroup);
            }
        }
        return groups;
    }

    private String textOf(@Nullable TextInputEditText editText) {
        return editText == null || editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.ia_open_link_error), Toast.LENGTH_SHORT).show();
        }
    }

    private static final class ProviderGroup {
        final String providerId;
        final String label;
        final boolean localProvider;
        final List<String> models;

        ProviderGroup(String providerId, String label, boolean localProvider, List<String> models) {
            this.providerId = providerId;
            this.label = label;
            this.localProvider = localProvider;
            this.models = models;
        }
    }

    private static final class ModelSelection {
        final String providerId;
        final String model;

        ModelSelection(String providerId, String model) {
            this.providerId = providerId;
            this.model = model;
        }
    }

}
