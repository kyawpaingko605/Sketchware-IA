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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;

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

import mod.hey.studios.util.Helper;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);

        binding = ActivityIaSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupToolbar();
        setupInsets();
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
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
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
    }

    private void selectSection(@NonNull String sectionKey) {
        for (Map.Entry<String, View> entry : sectionViews.entrySet()) {
            entry.getValue().setVisibility(entry.getKey().equals(sectionKey) ? View.VISIBLE : View.GONE);
        }
        updateMenuState(sectionKey);
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
                "Choose the active chat model for supported providers and keep a catalog of the models you want visible in this workspace."
        );

        addCurrentModelCard(container);
        addBodyText(container, "Tap a supported row to make it the active chat model. Switches below work like a quick favorites catalog.");

        for (ProviderGroup group : getCatalogProviderGroups()) {
            addModelGroup(container, group);
        }

        for (ProviderGroup customGroup : getCustomProviderGroups()) {
            addModelGroup(container, customGroup);
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

    private void addCurrentModelCard(LinearLayout parent) {
        String currentProvider = normalizeSupportedProvider(prefs.getString(PREF_CURRENT_PROVIDER, "groq"));
        String currentModel = prefs.getString(PREF_CURRENT_MODEL, getModelsForSupportedProvider(currentProvider).get(0));

        MaterialCardView card = createCard();
        LinearLayout content = createCardContent(card);
        content.addView(createSubheading("Current chat model"));
        content.addView(createMutedText("This is the provider and model used by the chat tool right now."));

        MaterialAutoCompleteTextView providerInput = createDropdown(
                supportedProviderLabels(),
                supportedProviderLabelForId(currentProvider)
        );
        TextInputLayout providerLayout = createDropdownLayout("Provider", providerInput);
        content.addView(providerLayout);

        List<String> models = getModelsForSupportedProvider(currentProvider);
        if (!models.contains(currentModel)) {
            currentModel = models.get(0);
        }

        MaterialAutoCompleteTextView modelInput = createDropdown(models, currentModel);
        TextInputLayout modelLayout = createDropdownLayout("Model", modelInput);
        content.addView(modelLayout);

        providerInput.setOnItemClickListener((parentView, view, position, id) -> {
            String selectedProviderId = supportedProviderIdForLabel(providerInput.getText().toString());
            List<String> updatedModels = getModelsForSupportedProvider(selectedProviderId);
            String selectedModel = updatedModels.get(0);
            prefs.edit()
                    .putString(PREF_CURRENT_PROVIDER, selectedProviderId)
                    .putString(PREF_CURRENT_MODEL, selectedModel)
                    .apply();
            buildModelsSection();
        });

        modelInput.setOnItemClickListener((parentView, view, position, id) -> prefs.edit()
                .putString(PREF_CURRENT_PROVIDER, supportedProviderIdForLabel(providerInput.getText().toString()))
                .putString(PREF_CURRENT_MODEL, modelInput.getText().toString())
                .apply());

        parent.addView(card);
    }

    private void addModelGroup(LinearLayout parent, ProviderGroup group) {
        TextView title = createGroupLabel(group.label);
        parent.addView(title);

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

        boolean isActive = isCurrentSelection(group.providerId, model) && group.supportedForSelection;
        if (isActive) {
            TextView activeLabel = createMutedText("Active chat model");
            textColumn.addView(activeLabel);
        } else if (!group.supportedForSelection) {
            TextView hintLabel = createMutedText("Catalog only");
            textColumn.addView(hintLabel);
        }

        row.addView(textColumn);

        MaterialSwitch favoriteSwitch = new MaterialSwitch(this);
        favoriteSwitch.setChecked(isActive || prefs.getBoolean(modelFavoriteKey(group.providerId, model), false));
        favoriteSwitch.setEnabled(!isActive);
        favoriteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit()
                .putBoolean(modelFavoriteKey(group.providerId, model), isChecked)
                .apply());
        row.addView(favoriteSwitch);

        if (group.supportedForSelection) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                prefs.edit()
                        .putString(PREF_CURRENT_PROVIDER, group.providerId)
                        .putString(PREF_CURRENT_MODEL, model)
                        .apply();
                buildModelsSection();
            });
        }

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

        TextInputEditText providerInput = createTextInput(dialogContent, "Provider label", "Custom");
        TextInputEditText providerIdInput = createTextInput(dialogContent, "Provider id", "custom");
        TextInputEditText modelInput = createTextInput(dialogContent, "Model id", "my-model");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add model")
                .setView(dialogContent)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Add", (dialog, which) -> {
                    String providerLabel = textOf(providerInput);
                    String providerId = textOf(providerIdInput);
                    String model = textOf(modelInput);
                    if (providerLabel.isEmpty() || providerId.isEmpty() || model.isEmpty()) {
                        Toast.makeText(this, "Provider and model fields are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONArray models = readJsonArrayPreference(PREF_CUSTOM_MODELS);
                    JSONObject customModel = new JSONObject();
                    try {
                        customModel.put("providerLabel", providerLabel);
                        customModel.put("providerId", providerId);
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
        groups.add(new ProviderGroup("ollama", "Ollama", false, List.of(
                "qwen3.5:397b-cloud"
        )));
        groups.add(new ProviderGroup("anthropic", "Anthropic", false, List.of(
                "claude-opus-4-0",
                "claude-sonnet-4-0",
                "claude-3-7-sonnet-latest",
                "claude-3-5-sonnet-latest",
                "claude-3-5-haiku-latest",
                "claude-3-opus-latest"
        )));
        groups.add(new ProviderGroup("openai", "OpenAI", true, getModelsForSupportedProvider("openai")));
        groups.add(new ProviderGroup("deepseek", "DeepSeek", false, List.of(
                "deepseek-chat",
                "deepseek-reasoner"
        )));
        groups.add(new ProviderGroup("openrouter", "OpenRouter", false, List.of(
                "anthropic/claude-opus-4",
                "anthropic/claude-sonnet-4",
                "qwen/qwen3-235b-a22b",
                "anthropic/claude-3.7-sonnet",
                "anthropic/claude-3.5-sonnet",
                "deepseek/deepseek-r1",
                "deepseek/deepseek-r1-zero:free",
                "mistralai/devstral-small:free"
        )));
        groups.add(new ProviderGroup("gemini", "Gemini", true, getModelsForSupportedProvider("gemini")));
        groups.add(new ProviderGroup("groq", "Groq", true, getModelsForSupportedProvider("groq")));
        groups.add(new ProviderGroup("grok_xai", "Grok (xAI)", false, List.of(
                "grok-2",
                "grok-3",
                "grok-3-mini",
                "grok-3-fast",
                "grok-3-mini-fast"
        )));
        groups.add(new ProviderGroup("mistral", "Mistral", false, List.of(
                "codestral-latest",
                "devstral-small-latest",
                "mistral-large-latest",
                "mistral-medium-latest",
                "ministral-3b-latest",
                "ministral-8b-latest"
        )));
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
                group = new ProviderGroup(providerId, providerLabel, false, new ArrayList<>());
                groups.put(providerId, group);
            }
            group.models.add(model);
        }
        return new ArrayList<>(groups.values());
    }

    private List<String> supportedProviderLabels() {
        return List.of("Groq", "OpenAI", "Gemini");
    }

    private String supportedProviderLabelForId(String providerId) {
        return switch (normalizeSupportedProvider(providerId)) {
            case "openai" -> "OpenAI";
            case "gemini" -> "Gemini";
            default -> "Groq";
        };
    }

    private String supportedProviderIdForLabel(String label) {
        if ("OpenAI".equalsIgnoreCase(label)) {
            return "openai";
        }
        if ("Gemini".equalsIgnoreCase(label)) {
            return "gemini";
        }
        return "groq";
    }

    private String normalizeSupportedProvider(String providerId) {
        if ("openai".equals(providerId) || "gemini".equals(providerId) || "groq".equals(providerId)) {
            return providerId;
        }
        return "groq";
    }

    private List<String> getModelsForSupportedProvider(String providerId) {
        return switch (normalizeSupportedProvider(providerId)) {
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
            default -> List.of(
                    "qwen-qwq-32b",
                    "llama-3.3-70b-versatile",
                    "llama-3.1-8b-instant"
            );
        };
    }

    private boolean isCurrentSelection(String providerId, String model) {
        String currentProvider = normalizeSupportedProvider(prefs.getString(PREF_CURRENT_PROVIDER, "groq"));
        String currentModel = prefs.getString(PREF_CURRENT_MODEL, "");
        return currentProvider.equals(providerId) && model.equals(currentModel);
    }

    private String modelFavoriteKey(String providerId, String model) {
        return "catalog_model_" + slugify(providerId) + "_" + slugify(model);
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
        final boolean supportedForSelection;
        final List<String> models;

        ProviderGroup(String providerId, String label, boolean supportedForSelection, List<String> models) {
            this.providerId = providerId;
            this.label = label;
            this.supportedForSelection = supportedForSelection;
            this.models = models;
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
