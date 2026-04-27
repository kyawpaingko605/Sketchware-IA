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
import pro.sketchware.databinding.ActivityIaSettingsBinding;
import pro.sketchware.utility.TranslationFunction;

public class IaSettingsActivity extends BaseAppCompatActivity {

    private static final String PREFS_NAME = "ia_settings";

    private static final String SECTION_MODELS = "models";
    private static final String SECTION_LOCAL_PROVIDERS = "local_providers";
    private static final String SECTION_MAIN_PROVIDERS = "main_providers";
    private static final String SECTION_FEATURE_OPTIONS = "feature_options";
    private static final String SECTION_MCP = "mcp";

    private static final String PREF_CURRENT_PROVIDER = "current_ai_provider";
    private static final String PREF_CURRENT_MODEL = "current_ai_model";
    private static final String PREF_CUSTOM_MODELS = "custom_models_json";
    private static final String PREF_MCP_SERVERS = "mcp_servers_json";

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
        content.addView(createActionButtonRow(
                createTextButton("Refresh Ollama models"),
                createTextButton("Refresh vLLM models"),
                createTextButton("Refresh LM Studio models")
        ));
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

        MaterialCardView setupCard = createCard();
        LinearLayout setupContent = createCardContent(setupCard);
        setupContent.addView(createSubheading("Ollama setup"));
        setupContent.addView(createMutedText("1. Install Ollama.\n2. Start the local service.\n3. Pull at least one model so it can be discovered by the app."));
        container.addView(setupCard);

        container.addView(createTextFieldCard(
                "Ollama",
                "Endpoint",
                "local_provider_ollama_url",
                "http://127.0.0.1:11434",
                false,
                null,
                "Read more about custom endpoints in the provider docs."
        ));

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

        List<ProviderCardSpec> providers = new ArrayList<>();
        providers.add(new ProviderCardSpec("Anthropic", "Get your API key here.", "https://console.anthropic.com/settings/keys")
                .addField("API Key", "anthropic_api_key", "", true, null));
        providers.add(new ProviderCardSpec("OpenAI", "Get your API key here.", getString(R.string.url_openai_api_keys))
                .addField("API Key", "openai_api_key", "", true, "openai_enabled"));
        providers.add(new ProviderCardSpec("DeepSeek", "Get your API key here.", "https://platform.deepseek.com/api_keys")
                .addField("API Key", "deepseek_api_key", "", true, null));
        providers.add(new ProviderCardSpec("OpenRouter", "Get your API key here.", "https://openrouter.ai/keys")
                .addField("API Key", "openrouter_api_key", "", true, null));
        providers.add(new ProviderCardSpec("OpenAI-Compatible", "Use this for llama.cpp proxies and other OpenAI-compatible endpoints.", null)
                .addField("Base URL", "openai_compatible_base_url", "https://my-endpoint.example/v1", false, null)
                .addField("API Key", "openai_compatible_api_key", "", true, null)
                .addField("Headers JSON", "openai_compatible_headers", "{}", false, null));
        providers.add(new ProviderCardSpec("Gemini", "Google AI Studio OpenAI-compatible endpoint.", getString(R.string.url_gemini_api_keys))
                .addField("API Key", "gemini_api_key", "", true, "gemini_enabled"));
        providers.add(new ProviderCardSpec("Groq", "Use Groq-hosted OpenAI-compatible models.", getString(R.string.url_groq_api_keys))
                .addField("API Key", "groq_api_key", "", true, "groq_enabled"));
        providers.add(new ProviderCardSpec("Grok (xAI)", "xAI-hosted models and API access.", "https://console.x.ai/")
                .addField("API Key", "grok_xai_api_key", "", true, null));
        providers.add(new ProviderCardSpec("Mistral", "Mistral API access.", "https://console.mistral.ai/api-keys/")
                .addField("API Key", "mistral_api_key", "", true, null));
        providers.add(new ProviderCardSpec("LiteLLM", "Point this to a LiteLLM proxy if you use one.", null)
                .addField("Base URL", "litellm_base_url", "http://localhost:4000", false, null));
        providers.add(new ProviderCardSpec("Google Vertex AI", "Configure region and project before using Vertex-backed models.", null)
                .addField("Region", "vertex_region", "us-west2", false, null)
                .addField("Project", "vertex_project", "my-project", false, null));
        providers.add(new ProviderCardSpec("Microsoft Azure OpenAI", "Azure OpenAI connection values.", null)
                .addField("Resource", "azure_openai_resource", "my-resource", false, null)
                .addField("API Key", "azure_openai_api_key", "", true, null)
                .addField("API Version", "azure_openai_version", "2024-05-01-preview", false, null));
        providers.add(new ProviderCardSpec("AWS Bedrock", "Connect through a Bedrock gateway or compatible proxy.", null)
                .addField("API Key", "bedrock_api_key", "", true, null)
                .addField("Region", "bedrock_region", "us-east-1", false, null)
                .addField("Endpoint", "bedrock_endpoint", "http://localhost:4000/v1", false, null));
        providers.add(new ProviderCardSpec("Morph", "Used by the existing code-editing flow.", getString(R.string.url_morph_api_keys))
                .addField("API Key", "morph_api_key", "", true, "morph_enabled"));

        for (ProviderCardSpec provider : providers) {
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

        MaterialCardView autocompleteCard = createCard();
        LinearLayout autocompleteContent = createCardContent(autocompleteCard);
        autocompleteContent.addView(createSubheading("Autocomplete"));
        autocompleteContent.addView(createSwitch(
                "Enable autocomplete",
                "feature_autocomplete_enabled",
                false
        ));
        autocompleteContent.addView(createMutedText("Experimental. Works best when your selected model supports completion-style behavior."));
        container.addView(autocompleteCard);

        MaterialCardView applyCard = createCard();
        LinearLayout applyContent = createCardContent(applyCard);
        applyContent.addView(createSubheading("Apply"));
        applyContent.addView(createSwitch(
                "Use the same model as chat",
                "feature_apply_same_as_chat_model",
                true
        ));
        MaterialAutoCompleteTextView applyModeInput = createDropdown(
                List.of("Fast Apply", "Balanced", "Careful"),
                prefs.getString("feature_apply_mode", "Fast Apply")
        );
        TextInputLayout applyModeLayout = createDropdownLayout("Apply mode", applyModeInput);
        applyModeInput.setOnItemClickListener((parentView, view, position, id) -> prefs.edit()
                .putString("feature_apply_mode", applyModeInput.getText().toString())
                .apply());
        applyContent.addView(applyModeLayout);
        container.addView(applyCard);

        MaterialCardView toolsCard = createCard();
        LinearLayout toolsContent = createCardContent(toolsCard);
        toolsContent.addView(createSubheading("Tools"));
        toolsContent.addView(createSwitch("Auto-approve edits", "feature_tools_auto_approve_edits", true));
        toolsContent.addView(createSwitch("Auto-approve terminal", "feature_tools_auto_approve_terminal", true));
        toolsContent.addView(createSwitch("Auto-approve MCP tools", "feature_tools_auto_approve_mcp", false));
        toolsContent.addView(createSwitch("Fix lint errors", "feature_tools_fix_lint", true));
        toolsContent.addView(createSwitch("Auto-accept LLM changes", "feature_tools_auto_accept_changes", false));
        container.addView(toolsCard);

        MaterialCardView editorCard = createCard();
        LinearLayout editorContent = createCardContent(editorCard);
        editorContent.addView(createSubheading("Editor"));
        editorContent.addView(createSwitch("Show suggestions on select", "feature_editor_show_suggestions_on_select", true));
        container.addView(editorCard);

        MaterialCardView commitCard = createCard();
        LinearLayout commitContent = createCardContent(commitCard);
        commitContent.addView(createSubheading("Commit Message Generator"));
        commitContent.addView(createSwitch("Use the same model as chat", "feature_commit_same_as_chat_model", true));
        container.addView(commitCard);
    }

    private void buildMcpSection() {
        LinearLayout container = binding.sectionMcp;
        container.removeAllViews();

        addSectionHeader(
                container,
                "MCP",
                "Use Model Context Protocol servers to expose extra tools and context to the agent."
        );

        MaterialCardView card = createCard();
        LinearLayout content = createCardContent(card);

        MaterialButton addServerButton = createFilledButton("Add MCP Server");
        addServerButton.setOnClickListener(v -> showAddMcpDialog());
        content.addView(addServerButton);

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
        JSONArray servers = readJsonArrayPreference(PREF_MCP_SERVERS);
        if (servers.length() == 0) {
            mcpServersContainer.addView(createMutedText("No servers found"));
            return;
        }

        for (int i = 0; i < servers.length(); i++) {
            JSONObject server = servers.optJSONObject(i);
            if (server == null) {
                continue;
            }
            mcpServersContainer.addView(createMcpServerView(server, i));
        }
    }

    private View createMcpServerView(JSONObject server, int index) {
        MaterialCardView card = createCard();
        LinearLayout content = createCardContent(card);

        TextView title = createSubheading(server.optString("name", "Unnamed server"));
        content.addView(title);

        MaterialSwitch enabledSwitch = new MaterialSwitch(this);
        enabledSwitch.setText("Enabled");
        enabledSwitch.setLayoutParams(defaultRowLayoutParams());
        enabledSwitch.setChecked(server.optBoolean("enabled", true));
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateMcpServerEnabled(index, isChecked));
        content.addView(enabledSwitch);

        String command = server.optString("command", "");
        if (!command.isEmpty()) {
            content.addView(createMutedText(command));
        }

        String args = server.optString("args", "");
        if (!args.isEmpty()) {
            content.addView(createMutedText(args));
        }

        MaterialButton deleteButton = createTextButton("Remove");
        deleteButton.setOnClickListener(v -> removeMcpServer(index));
        content.addView(deleteButton);
        return card;
    }

    private void showAddMcpDialog() {
        LinearLayout dialogContent = new LinearLayout(this);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setPadding(dp(24), dp(8), dp(24), 0);

        TextInputEditText nameInput = createTextInput(dialogContent, "Server name", "filesystem");
        TextInputEditText commandInput = createTextInput(dialogContent, "Command", "npx -y @modelcontextprotocol/server-filesystem");
        TextInputEditText argsInput = createTextInput(dialogContent, "Arguments", "C:/Users/kirit/Documents");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add MCP Server")
                .setView(dialogContent)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = textOf(nameInput);
                    String command = textOf(commandInput);
                    String args = textOf(argsInput);

                    if (name.isEmpty() || command.isEmpty()) {
                        Toast.makeText(this, "Name and command are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONArray servers = readJsonArrayPreference(PREF_MCP_SERVERS);
                    JSONObject server = new JSONObject();
                    try {
                        server.put("name", name);
                        server.put("command", command);
                        server.put("args", args);
                        server.put("enabled", true);
                        servers.put(server);
                        prefs.edit().putString(PREF_MCP_SERVERS, servers.toString()).apply();
                    } catch (Exception ignored) {
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

    private void removeMcpServer(int index) {
        JSONArray current = readJsonArrayPreference(PREF_MCP_SERVERS);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < current.length(); i++) {
            if (i == index) {
                continue;
            }
            JSONObject item = current.optJSONObject(i);
            if (item != null) {
                updated.put(item);
            }
        }
        prefs.edit().putString(PREF_MCP_SERVERS, updated.toString()).apply();
        renderMcpServers();
    }

    private void updateMcpServerEnabled(int index, boolean enabled) {
        JSONArray current = readJsonArrayPreference(PREF_MCP_SERVERS);
        JSONObject item = current.optJSONObject(index);
        if (item == null) {
            return;
        }
        try {
            item.put("enabled", enabled);
            prefs.edit().putString(PREF_MCP_SERVERS, current.toString()).apply();
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

    private MaterialCardView createProviderCard(ProviderCardSpec spec) {
        MaterialCardView card = createCard();
        LinearLayout content = createCardContent(card);
        content.addView(createSubheading(spec.title));
        if (spec.description != null && !spec.description.isEmpty()) {
            content.addView(createMutedText(spec.description));
        }
        for (FieldSpec field : spec.fields) {
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
            button.setOnClickListener(v -> Toast.makeText(this, button.getText() + " requested.", Toast.LENGTH_SHORT).show());
            row.addView(button);
        }
        return row;
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

    private List<ProviderGroup> getCatalogProviderGroups() {
        List<ProviderGroup> groups = new ArrayList<>();
        groups.add(new ProviderGroup("ollama", "Ollama", true, new ArrayList<>(List.of(
                "qwen3.5:397b-cloud"
        ))));
        groups.add(new ProviderGroup("vllm", "vLLM", true, new ArrayList<>()));
        groups.add(new ProviderGroup("lm_studio", "LM Studio", true, new ArrayList<>()));
        groups.add(new ProviderGroup("anthropic", "Anthropic", false, new ArrayList<>(List.of(
                "claude-opus-4-0",
                "claude-sonnet-4-0",
                "claude-3-7-sonnet-latest",
                "claude-3-5-sonnet-latest",
                "claude-3-5-haiku-latest",
                "claude-3-opus-latest"
        ))));
        groups.add(new ProviderGroup("openai", "OpenAI", false, new ArrayList<>(getModelsForSupportedProvider("openai"))));
        groups.add(new ProviderGroup("deepseek", "DeepSeek", false, new ArrayList<>(List.of(
                "deepseek-chat",
                "deepseek-reasoner"
        ))));
        groups.add(new ProviderGroup("openrouter", "OpenRouter", false, new ArrayList<>(List.of(
                "anthropic/claude-opus-4",
                "anthropic/claude-sonnet-4",
                "qwen/qwen3-235b-a22b",
                "anthropic/claude-3.7-sonnet",
                "anthropic/claude-3.5-sonnet",
                "deepseek/deepseek-r1",
                "deepseek/deepseek-r1-zero:free",
                "mistralai/devstral-small:free"
        ))));
        groups.add(new ProviderGroup("openai_compatible", "OpenAI-Compatible", false, new ArrayList<>()));
        groups.add(new ProviderGroup("gemini", "Gemini", false, new ArrayList<>(getModelsForSupportedProvider("gemini"))));
        groups.add(new ProviderGroup("groq", "Groq", false, new ArrayList<>(getModelsForSupportedProvider("groq"))));
        groups.add(new ProviderGroup("grok_xai", "Grok (xAI)", false, new ArrayList<>(List.of(
                "grok-2",
                "grok-3",
                "grok-3-mini",
                "grok-3-fast",
                "grok-3-mini-fast"
        ))));
        groups.add(new ProviderGroup("mistral", "Mistral", false, new ArrayList<>(List.of(
                "codestral-latest",
                "devstral-small-latest",
                "mistral-large-latest",
                "mistral-medium-latest",
                "ministral-3b-latest",
                "ministral-8b-latest"
        ))));
        groups.add(new ProviderGroup("litellm", "LiteLLM", false, new ArrayList<>()));
        groups.add(new ProviderGroup("vertex_ai", "Google Vertex AI", false, new ArrayList<>()));
        groups.add(new ProviderGroup("azure_openai", "Microsoft Azure OpenAI", false, new ArrayList<>()));
        groups.add(new ProviderGroup("bedrock", "AWS Bedrock", false, new ArrayList<>()));
        groups.add(new ProviderGroup("morph", "Morph", false, new ArrayList<>()));
        return groups;
    }

    private List<ProviderGroup> getCustomProviderGroups() {
        JSONArray array = readJsonArrayPreference(PREF_CUSTOM_MODELS);
        Map<String, ProviderGroup> groups = new LinkedHashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String providerId = item.optString("providerId", "custom");
            String providerLabel = item.optString("providerLabel", "Custom");
            String model = item.optString("model", "");
            if (model.isEmpty()) {
                continue;
            }
            ProviderGroup group = groups.get(providerId);
            if (group == null) {
                group = new ProviderGroup(providerId, providerLabel, isLocalProvider(providerId), new ArrayList<>());
                groups.put(providerId, group);
            }
            group.models.add(model);
        }
        return new ArrayList<>(groups.values());
    }

    private List<String> getModelsForSupportedProvider(String providerId) {
        return switch (providerId) {
            case "openai" -> List.of(
                    "gpt-4.1",
                    "gpt-4.1-mini",
                    "gpt-4.1-nano",
                    "o3",
                    "o4-mini"
            );
            case "gemini" -> List.of(
                    "gemini-2.5-pro-exp-03-25",
                    "gemini-2.5-flash-preview-04-17",
                    "gemini-2.0-flash",
                    "gemini-2.0-flash-lite",
                    "gemini-2.5-pro-preview-05-06"
            );
            case "groq" -> List.of(
                    "qwen-qwq-32b",
                    "llama-3.3-70b-versatile",
                    "llama-3.1-8b-instant"
            );
            default -> new ArrayList<>();
        };
    }

    private boolean isModelHidden(String providerId, String model) {
        return prefs.getBoolean(modelHiddenKey(providerId, model), false);
    }

    private void setModelHidden(String providerId, String model, boolean hidden) {
        prefs.edit().putBoolean(modelHiddenKey(providerId, model), hidden).apply();
    }

    private String modelHiddenKey(String providerId, String model) {
        return "model_hidden_" + slugify(providerId) + "_" + slugify(model);
    }

    private boolean isProviderConfigured(String providerId) {
        return switch (providerId) {
            case "ollama" -> !getPreferenceValue("local_provider_ollama_url", "http://127.0.0.1:11434").isEmpty();
            case "vllm" -> !getPreferenceValue("local_provider_vllm_url", "http://localhost:8000").isEmpty();
            case "lm_studio" -> !getPreferenceValue("local_provider_lm_studio_url", "http://localhost:1234").isEmpty();
            case "anthropic" -> !getPreferenceValue("anthropic_api_key", "").isEmpty();
            case "openai" -> !getPreferenceValue("openai_api_key", "").isEmpty();
            case "deepseek" -> !getPreferenceValue("deepseek_api_key", "").isEmpty();
            case "openrouter" -> !getPreferenceValue("openrouter_api_key", "").isEmpty();
            case "openai_compatible" -> !getPreferenceValue("openai_compatible_base_url", "https://my-endpoint.example/v1").isEmpty()
                    && !getPreferenceValue("openai_compatible_api_key", "").isEmpty();
            case "gemini" -> !getPreferenceValue("gemini_api_key", "").isEmpty();
            case "groq" -> !getPreferenceValue("groq_api_key", "").isEmpty();
            case "grok_xai" -> !getPreferenceValue("grok_xai_api_key", "").isEmpty();
            case "mistral" -> !getPreferenceValue("mistral_api_key", "").isEmpty();
            case "litellm" -> !getPreferenceValue("litellm_base_url", "http://localhost:4000").isEmpty();
            case "vertex_ai" -> !getPreferenceValue("vertex_project", "my-project").isEmpty();
            case "azure_openai" -> !getPreferenceValue("azure_openai_resource", "my-resource").isEmpty()
                    && !getPreferenceValue("azure_openai_api_key", "").isEmpty();
            case "bedrock" -> !getPreferenceValue("bedrock_api_key", "").isEmpty()
                    && !getPreferenceValue("bedrock_endpoint", "http://localhost:4000/v1").isEmpty();
            case "morph" -> !getPreferenceValue("morph_api_key", "").isEmpty();
            default -> true;
        };
    }

    private boolean isLocalProvider(String providerId) {
        return "ollama".equals(providerId) || "vllm".equals(providerId) || "lm_studio".equals(providerId);
    }

    private String getPreferenceValue(String key, String defaultValue) {
        return prefs.getString(key, defaultValue).trim();
    }

    private void ensureValidCurrentSelection() {
        String currentProvider = prefs.getString(PREF_CURRENT_PROVIDER, "groq");
        String currentModel = prefs.getString(PREF_CURRENT_MODEL, "");
        if (isChatSelectionValid(currentProvider, currentModel)) {
            return;
        }

        ModelSelection fallback = findFirstVisibleChatSelection();
        if (fallback == null) {
            return;
        }

        prefs.edit()
                .putString(PREF_CURRENT_PROVIDER, fallback.providerId)
                .putString(PREF_CURRENT_MODEL, fallback.model)
                .apply();
    }

    private boolean isChatSelectionValid(String providerId, String model) {
        if (!("groq".equals(providerId) || "openai".equals(providerId) || "gemini".equals(providerId))) {
            return false;
        }
        if (!isProviderConfigured(providerId) || model == null || model.trim().isEmpty()) {
            return false;
        }
        for (ProviderGroup group : getAllProviderGroups()) {
            if (providerId.equals(group.providerId) && group.models.contains(model) && !isModelHidden(providerId, model)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ModelSelection findFirstVisibleChatSelection() {
        for (String providerId : List.of("groq", "openai", "gemini")) {
            if (!isProviderConfigured(providerId)) {
                continue;
            }
            for (ProviderGroup group : getAllProviderGroups()) {
                if (!providerId.equals(group.providerId)) {
                    continue;
                }
                for (String model : group.models) {
                    if (!isModelHidden(providerId, model)) {
                        return new ModelSelection(providerId, model);
                    }
                }
            }
        }
        return null;
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

    private String slugify(String value) {
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_");
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

    private static final class ProviderCardSpec {
        final String title;
        final String description;
        final String helpUrl;
        final List<FieldSpec> fields = new ArrayList<>();

        ProviderCardSpec(String title, String description, String helpUrl) {
            this.title = title;
            this.description = description;
            this.helpUrl = helpUrl;
        }

        ProviderCardSpec addField(String label, String prefKey, String defaultValue, boolean password, @Nullable String enabledKey) {
            fields.add(new FieldSpec(label, prefKey, defaultValue, password, enabledKey));
            return this;
        }
    }

    private static final class FieldSpec {
        final String label;
        final String prefKey;
        final String defaultValue;
        final boolean password;
        final String enabledKey;

        FieldSpec(String label, String prefKey, String defaultValue, boolean password, @Nullable String enabledKey) {
            this.label = label;
            this.prefKey = prefKey;
            this.defaultValue = defaultValue;
            this.password = password;
            this.enabledKey = enabledKey;
        }
    }
}
