package pro.sketchware.activities.chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a.a.a.lC;
import a.a.a.yB;
import pro.sketchware.R;
import pro.sketchware.activities.chat.port.VoidPortChatThreadService;
import pro.sketchware.activities.chat.port.VoidPortScmService;
import pro.sketchware.utility.TranslationFunction;

public class ChatActivity extends AppCompatActivity {
    private static final int REQUEST_PICK_REFERENCE_IMAGE = 9102;
    private static final int MAX_PENDING_REFERENCES = 8;

    private String sc_id;
    private ViewPager chatViewPager;
    private EditText editTextMessage;
    private View btnSend;
    private View btnAttach;
    private View btnChatMode;
    private View btnModelSelector;
    private ImageView btnCancelRun;
    private ImageView btnMicrophone;
    private TextView textChatMode;
    private TextView textCurrentModel;
    private TextView textFilesChanged;
    private TextView textSelectedContext;
    private TabLayout chatPageTabs;
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;
    private final List<ChatReference> pendingReferences = new ArrayList<>();
    private ExecutorService executorService;
    private long lastMessageTime = 0; // Timestamp do Ãºltimo envio de mensagem
    private static final long MIN_MESSAGE_INTERVAL_MS = 2000; // Intervalo mÃ­nimo de 2 segundos entre mensagens
    private boolean isProcessing = false; // Flag para indicar se estÃ¡ processando uma mensagem
    private ChatHistoryManager historyManager;
    private boolean showDebug = false; // Flag para controlar exibiÃ§Ã£o de mensagens de debug
    private boolean suppressMentionWatcher = false;
    private AgentManager agentManager;
    private ChatMessage currentDebugMessage;
    private ChatMessagesFragment chatMessagesFragment;
    private ChatDiffFragment chatDiffFragment;

    @Override
    public Resources getResources() {
        return TranslationFunction.wrapResources(this, super.getResources());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sc_id = getIntent().getStringExtra("sc_id");
        if (sc_id == null || sc_id.isEmpty()) {
            Toast.makeText(this, R.string.chat_project_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        executorService = Executors.newSingleThreadExecutor();
        historyManager = new ChatHistoryManager(this);

        // Carregar preferÃªncia de debug
        SharedPreferences prefs = getSharedPreferences("chat_settings", MODE_PRIVATE);
        showDebug = prefs.getBoolean("show_debug", false);

        setupViews();
        loadProjectInfo();

        // Inicializar AgentManager (Void-style logic)
        agentManager = new AgentManager(this, sc_id, messages, new AgentManager.AgentListener() {
            @Override
            public void onMessageAdded(ChatMessage message) {
                runOnUiThread(() -> {
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                    if (historyManager != null && sc_id != null) {
                        historyManager.saveMessage(sc_id, message);
                    }
                    updateThreadSummary();
                    updateChangedFilesSummary();
                });
            }

            @Override
            public void onMessageUpdated(ChatMessage message) {
                runOnUiThread(() -> {
                    int index = messages.indexOf(message);
                    if (index != -1) {
                        messageAdapter.notifyItemChanged(index);
                    }
                    saveChatHistory();
                    updateThreadSummary();
                    updateChangedFilesSummary();
                });
            }

            @Override
            public void onMessageRemoved(ChatMessage message, int index) {
                runOnUiThread(() -> {
                    messageAdapter.notifyItemRemoved(index);
                    saveChatHistory();
                    updateThreadSummary();
                });
            }

            @Override
            public void onStatusChanged(String status) {
                runOnUiThread(() -> updateRunStatus(status));
            }

            @Override
            public void onDebug(String message) {
                runOnUiThread(() -> appendDebugMessage(message));
            }

            @Override
            public void onProcessingFinished() {
                runOnUiThread(() -> {
                    isProcessing = false;
                    currentDebugMessage = null;
                    showProgress(false);
                    setInputEnabled(true);
                    saveChatHistory();
                    updateRunStatus("");
                    updateChangedFilesSummary();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    currentDebugMessage = null;
                    Toast.makeText(ChatActivity.this, error, Toast.LENGTH_LONG).show();
                    ChatMessage errorMsg = new ChatMessage("Error: " + error, false, System.currentTimeMillis());
                    messages.add(errorMsg);
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                    saveChatHistory();
                });
            }
        });

        // Carregar histÃ³rico do chat
        loadChatHistory();
    }



    public void approveTool() {
        if (agentManager != null) {
            agentManager.approveTool();
        }
    }

    public void rejectTool() {
        if (agentManager != null) {
            agentManager.rejectTool();
        }
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        chatViewPager = findViewById(R.id.chat_view_pager);
        chatPageTabs = findViewById(R.id.chat_page_tabs);
        editTextMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.btn_send);
        btnAttach = findViewById(R.id.btn_attach);
        btnCancelRun = findViewById(R.id.btn_cancel_run);
        btnMicrophone = findViewById(R.id.btn_microphone);
        textFilesChanged = findViewById(R.id.text_files_changed);
        textSelectedContext = findViewById(R.id.text_selected_context);
        editTextMessage.setHint(R.string.chat_input_hint);

        messages = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(messages);
        chatMessagesFragment = new ChatMessagesFragment();
        chatDiffFragment = ChatDiffFragment.newInstance(sc_id);
        chatMessagesFragment.setAdapter(messageAdapter);
        chatViewPager.setAdapter(new ChatPagerAdapter(this, chatMessagesFragment, chatDiffFragment));
        chatViewPager.setOffscreenPageLimit(2);
        if (chatPageTabs != null) {
            chatPageTabs.setupWithViewPager(chatViewPager);
        }

        // Configurar ícone de enviar e listener
        btnSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                editTextMessage.setText("");
            }
        });

        if (btnCancelRun != null) {
            btnCancelRun.setOnClickListener(v -> cancelCurrentRun());
        }

        if (btnMicrophone != null) {
            btnMicrophone.setOnClickListener(v -> startVoiceInput());
        }

        if (textFilesChanged != null) {
            textFilesChanged.setOnClickListener(v -> showRecentChangesDialog());
        }
        if (btnAttach != null) {
            btnAttach.setOnClickListener(this::showAttachMenu);
        }
        if (textSelectedContext != null) {
            textSelectedContext.setOnClickListener(v -> clearPendingReferences());
        }
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (suppressMentionWatcher || isProcessing || editable == null) {
                    return;
                }
                int cursor = editTextMessage.getSelectionStart();
                if (cursor > 0 && cursor <= editable.length() && editable.charAt(cursor - 1) == '@') {
                    showReferencePicker(true);
                }
            }
        });

        // Configurar Speech-to-Text
        // ConfiguraÃ§Ã£o do Seletor de Modelo
        btnChatMode = findViewById(R.id.btn_chat_mode);
        btnModelSelector = findViewById(R.id.btn_model_selector);
        textChatMode = findViewById(R.id.text_chat_mode);
        textCurrentModel = findViewById(R.id.text_current_model);

        SharedPreferences prefs = AiChatSettingsHelper.prefs(this);
        AiChatSettingsHelper.ensureValidCurrentSelection(prefs);
        updateChatModeUI();
        updateModelUI();
        updateRunStatus("");
        updateChangedFilesSummary();
        updateThreadSummary();
        updatePendingReferencesUi();

        if (btnChatMode != null) {
            btnChatMode.setOnClickListener(v -> showChatModeMenu(prefs));
        }

        btnModelSelector.setOnClickListener(v -> showModelSelectorMenu(prefs));
    }

    private void updateModelUI() {
        if (textCurrentModel != null) {
            SharedPreferences prefs = AiChatSettingsHelper.prefs(this);
            String currentModel = prefs.getString("current_ai_model", "");
            String currentProvider = prefs.getString("current_ai_provider", "");
            if (currentModel != null
                    && !currentModel.trim().isEmpty()
                    && AiChatSettingsHelper.isCurrentSelectionValid(prefs, currentProvider, currentModel)) {
                textCurrentModel.setText(currentModel);
                return;
            }
            textCurrentModel.setText(R.string.chat_no_models_available_short);
        }
    }

    private void updateChatModeUI() {
        if (textChatMode == null) {
            return;
        }
        SharedPreferences prefs = AiChatSettingsHelper.prefs(this);
        String chatMode = AiChatSettingsHelper.getChatMode(prefs);
        if ("normal".equals(chatMode)) {
            textChatMode.setText(R.string.chat_mode_chat);
        } else if ("gather".equals(chatMode)) {
            textChatMode.setText(R.string.chat_mode_gather);
        } else {
            textChatMode.setText(R.string.chat_mode_agent);
        }
    }

    private void showModelSelectorMenu(SharedPreferences prefs) {
        List<AiChatSettingsHelper.ModelOption> options = AiChatSettingsHelper.getVisibleModelOptions(prefs);
        if (options.isEmpty()) {
            Toast.makeText(this, R.string.chat_no_models_available, Toast.LENGTH_LONG).show();
            return;
        }

        PopupMenu popup = new PopupMenu(this, btnModelSelector);
        for (int i = 0; i < options.size(); i++) {
            AiChatSettingsHelper.ModelOption option = options.get(i);
            popup.getMenu().add(0, i + 1, i, option.getDisplayLabel());
        }

        popup.setOnMenuItemClickListener(item -> {
            int index = item.getItemId() - 1;
            if (index < 0 || index >= options.size()) {
                return false;
            }
            AiChatSettingsHelper.ModelOption selected = options.get(index);
            prefs.edit()
                    .putString(AiChatSettingsHelper.PREF_CURRENT_PROVIDER, selected.providerId)
                    .putString(AiChatSettingsHelper.PREF_CURRENT_MODEL, selected.model)
                    .apply();
            updateModelUI();
            Toast.makeText(this, getString(R.string.chat_model_changed, selected.getDisplayLabel()), Toast.LENGTH_SHORT).show();
            return true;
        });
        popup.show();
    }

    private void showChatModeMenu(SharedPreferences prefs) {
        PopupMenu popup = new PopupMenu(this, btnChatMode);
        popup.getMenu().add(0, 1, 0, getString(R.string.chat_mode_chat));
        popup.getMenu().add(0, 2, 1, getString(R.string.chat_mode_gather));
        popup.getMenu().add(0, 3, 2, getString(R.string.chat_mode_agent));

        popup.setOnMenuItemClickListener(item -> {
            String mode = "agent";
            int descriptionRes = R.string.chat_mode_detail_agent;
            if (item.getItemId() == 1) {
                mode = "normal";
                descriptionRes = R.string.chat_mode_detail_chat;
            } else if (item.getItemId() == 2) {
                mode = "gather";
                descriptionRes = R.string.chat_mode_detail_gather;
            }
            AiChatSettingsHelper.setChatMode(prefs, mode);
            updateChatModeUI();
            Toast.makeText(this, getString(descriptionRes), Toast.LENGTH_SHORT).show();
            return true;
        });
        popup.show();
    }

    private void loadProjectInfo() {
        HashMap<String, Object> projectInfo = lC.b(sc_id);
        if (projectInfo != null) {
            String projectName = yB.c(projectInfo, "my_ws_name");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.chat_title_with_project, projectName));
            }
        }
    }

    private void loadChatHistory() {
        // Carregar histÃ³rico salvo
        List<ChatMessage> savedMessages = historyManager.loadHistory(sc_id);

        if (savedMessages != null && !savedMessages.isEmpty()) {
            // Se jÃ¡ tem histÃ³rico, usar ele
            messages.addAll(savedMessages);
            messageAdapter.notifyDataSetChanged();
            scrollToBottom();
        } else {
            // Se nÃ£o tem histÃ³rico, adicionar mensagem de boas-vindas
            addWelcomeMessage();
        }
        updateThreadSummary();
        updateChangedFilesSummary();
    }

    private void addWelcomeMessage() {
        HashMap<String, Object> projectInfo = lC.b(sc_id);
        String projectName = projectInfo != null ? yB.c(projectInfo, "my_ws_name") : getString(R.string.chat_default_project_name);
        String welcomeMessage = getString(R.string.chat_welcome_message, projectName);
        ChatMessage welcomeMsg = new ChatMessage(welcomeMessage, false, System.currentTimeMillis());
        messages.add(welcomeMsg);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        if (historyManager != null && sc_id != null) {
            historyManager.saveMessage(sc_id, welcomeMsg);
        }
    }

    private void saveChatHistory() {
        if (historyManager != null && sc_id != null) {
            historyManager.saveHistory(sc_id, messages);
        }
    }

    private void sendMessage(String message) {
        // Verificar se jÃ¡ estÃ¡ processando uma mensagem
        if (isProcessing) {
            Toast.makeText(this, R.string.chat_wait_processing, Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar rate limiting - intervalo mÃ­nimo entre mensagens
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMessage = currentTime - lastMessageTime;

        if (timeSinceLastMessage < MIN_MESSAGE_INTERVAL_MS && lastMessageTime > 0) {
            long remainingSeconds = (MIN_MESSAGE_INTERVAL_MS - timeSinceLastMessage) / 1000 + 1;
            Toast.makeText(this, getString(R.string.chat_wait_before_sending, remainingSeconds), Toast.LENGTH_SHORT).show();
            return;
        }

        // Atualizar timestamp do Ãºltimo envio
        lastMessageTime = currentTime;
        isProcessing = true;

        // Desabilitar input enquanto processa
        setInputEnabled(false);
        showProgress(true);
        currentDebugMessage = null;

        // Delegar para AgentManager (Streaming e AgÃªntico)
        String contextPayload = ChatReferenceManager.buildContextPayload(this, pendingReferences);
        agentManager.processUserMessage(message, contextPayload);
        clearPendingReferences();
    }

    private void setInputEnabled(boolean enabled) {
        editTextMessage.setEnabled(enabled);
        if (btnSend != null) btnSend.setEnabled(enabled);
        if (btnSend != null) btnSend.setAlpha(enabled ? 1f : 0.55f);
        if (btnAttach != null) btnAttach.setEnabled(enabled);
        if (btnAttach != null) btnAttach.setAlpha(enabled ? 1f : 0.55f);
    }

    private void showAttachMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, getString(R.string.chat_attach_project_reference));
        popup.getMenu().add(0, 2, 1, getString(R.string.chat_attach_reference_image));
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showReferencePicker(false);
                return true;
            }
            if (item.getItemId() == 2) {
                pickReferenceImage();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showReferencePicker(boolean replaceAtTrigger) {
        List<ChatReferenceManager.ReferenceOption> allOptions = ChatReferenceManager.getProjectReferenceOptions(sc_id);
        if (allOptions.isEmpty()) {
            Toast.makeText(this, R.string.chat_reference_none, Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        container.setPadding(padding, dp(8), padding, 0);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint(R.string.chat_reference_search_hint);
        container.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ListView listView = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        List<ChatReferenceManager.ReferenceOption> visibleOptions = new ArrayList<>();
        listView.setAdapter(adapter);
        container.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(360)
        ));

        Runnable refresh = () -> {
            String query = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase();
            adapter.clear();
            visibleOptions.clear();
            for (ChatReferenceManager.ReferenceOption option : allOptions) {
                if (query.isEmpty() || option.filterText.contains(query)) {
                    visibleOptions.add(option);
                    adapter.add(option.displayText);
                }
            }
            adapter.notifyDataSetChanged();
        };
        refresh.run();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.chat_reference_picker_title)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= visibleOptions.size()) {
                return;
            }
            ChatReference reference = visibleOptions.get(position).reference;
            if (addPendingReference(reference)) {
                insertMention(reference, replaceAtTrigger);
            }
            dialog.dismiss();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh.run();
            }
        });

        dialog.show();
    }

    private void pickReferenceImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_PICK_REFERENCE_IMAGE);
        } catch (Exception firstFailure) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            fallback.setType("image/*");
            fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(fallback, getString(R.string.chat_attach_reference_image)), REQUEST_PICK_REFERENCE_IMAGE);
        }
    }

    private boolean addPendingReference(ChatReference reference) {
        if (reference == null) {
            return false;
        }
        for (ChatReference pending : pendingReferences) {
            if (pending != null && pending.stableKey().equals(reference.stableKey())) {
                updatePendingReferencesUi();
                return true;
            }
        }
        if (pendingReferences.size() >= MAX_PENDING_REFERENCES) {
            Toast.makeText(this, R.string.chat_reference_limit, Toast.LENGTH_SHORT).show();
            return false;
        }
        pendingReferences.add(reference);
        updatePendingReferencesUi();
        return true;
    }

    private void insertMention(ChatReference reference, boolean replaceAtTrigger) {
        if (reference == null || editTextMessage == null) {
            return;
        }
        Editable editable = editTextMessage.getText();
        int cursor = Math.max(0, editTextMessage.getSelectionStart());
        cursor = Math.min(cursor, editable.length());
        int start = replaceAtTrigger ? findAtTrigger(editable, cursor) : cursor;
        String insertion = reference.mentionText() + " ";
        suppressMentionWatcher = true;
        try {
            editable.replace(start, cursor, insertion);
            editTextMessage.setSelection(Math.min(start + insertion.length(), editable.length()));
        } finally {
            suppressMentionWatcher = false;
        }
    }

    private int findAtTrigger(Editable editable, int cursor) {
        if (editable != null && cursor > 0 && cursor <= editable.length() && editable.charAt(cursor - 1) == '@') {
            return cursor - 1;
        }
        return cursor;
    }

    private void clearPendingReferences() {
        pendingReferences.clear();
        updatePendingReferencesUi();
    }

    private void updatePendingReferencesUi() {
        if (textSelectedContext == null) {
            return;
        }
        if (pendingReferences.isEmpty()) {
            textSelectedContext.setVisibility(View.GONE);
            textSelectedContext.setText("");
            return;
        }
        textSelectedContext.setVisibility(View.VISIBLE);
        textSelectedContext.setText(getString(
                R.string.chat_reference_context_label,
                ChatReferenceManager.summarizeReferences(pendingReferences)
        ));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showProgress(boolean show) {
        updateRunStatus(show ? getString(R.string.chat_processing) : "");
        if (btnCancelRun != null) {
            btnCancelRun.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnSend != null) {
            btnSend.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale algo...");
        try {
            startActivityForResult(intent, 1001);
        } catch (Exception e) {
            Toast.makeText(this, "Reconhecimento de voz não suportado", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateRunStatus(String status) {
        String safeStatus = status == null ? "" : status.trim();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(safeStatus.isEmpty() ? null : safeStatus);
        }
    }

    public void updateChangedFilesSummary() {
        if (textFilesChanged == null) {
            return;
        }
        int count = VoidPortScmService.changedFileCount(sc_id);
        textFilesChanged.setText(VoidPortChatThreadService.changedFilesLabel(count));
        textFilesChanged.setAlpha(count > 0 ? 1f : 0.7f);
        if (chatDiffFragment != null) {
            chatDiffFragment.refreshDiffs();
        }
    }

    private void updateThreadSummary() {
    }

    private void showRecentChangesDialog() {
        if (chatDiffFragment != null) {
            chatDiffFragment.refreshDiffs();
        }
        if (chatViewPager != null) {
            chatViewPager.setCurrentItem(1, true);
        }
    }

    private void scrollToBottom() {
        if (messages.size() > 0 && chatMessagesFragment != null) {
            chatMessagesFragment.scrollToBottom();
        }
    }

    private void appendDebugMessage(String debugLine) {
        if (!showDebug || !ChatMessage.hasVisibleText(debugLine)) {
            return;
        }

        String formattedLine = "- [" + new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date()) + "] " + debugLine.trim();

        if (currentDebugMessage == null || !messages.contains(currentDebugMessage)) {
            currentDebugMessage = new ChatMessage(
                    formattedLine,
                    ChatMessage.TYPE_CHECKPOINT,
                    System.currentTimeMillis(),
                    "Debug"
            );
            messages.add(currentDebugMessage);
            messageAdapter.notifyItemInserted(messages.size() - 1);
        } else {
            String previousText = currentDebugMessage.getMessage();
            if (ChatMessage.hasVisibleText(previousText)) {
                currentDebugMessage.setMessage(previousText + "\n" + formattedLine);
            } else {
                currentDebugMessage.setMessage(formattedLine);
            }
            currentDebugMessage.setTimestamp(System.currentTimeMillis());
            int index = messages.indexOf(currentDebugMessage);
            if (index != -1) {
                messageAdapter.notifyItemChanged(index);
            }
        }

        scrollToBottom();
        saveChatHistory();
        updateThreadSummary();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        // Atualizar estado do checkbox de debug
        MenuItem debugItem = menu.findItem(R.id.menu_toggle_debug);
        if (debugItem != null) {
            debugItem.setChecked(showDebug);
            debugItem.setTitle(showDebug ? R.string.chat_menu_hide_debug : R.string.chat_menu_show_debug);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.menu_clear_chat) {
            clearChat();
            return true;
        } else if (item.getItemId() == R.id.menu_rollback_checkpoint) {
            rollbackLastCheckpoint();
            return true;
        } else if (item.getItemId() == R.id.menu_toggle_debug) {
            showDebug = !showDebug;
            item.setChecked(showDebug);
            item.setTitle(showDebug ? R.string.chat_menu_hide_debug : R.string.chat_menu_show_debug);
            if (!showDebug) {
                currentDebugMessage = null;
            }
            SharedPreferences prefs = getSharedPreferences("chat_settings", MODE_PRIVATE);
            prefs.edit().putBoolean("show_debug", showDebug).apply();
            Toast.makeText(this, showDebug ? R.string.chat_debug_enabled : R.string.chat_debug_disabled, Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.menu_export_chat) {
            exportChatToTxt();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearChat() {
        if (historyManager != null && sc_id != null) {
            historyManager.clearHistory(sc_id);
        }
        int messageCount = messages.size();
        messages.clear();
        messageAdapter.notifyItemRangeRemoved(0, messageCount);
        addWelcomeMessage();
        Toast.makeText(this, R.string.chat_cleared, Toast.LENGTH_SHORT).show();
    }

    public void cancelCurrentRun() {
        if (agentManager == null || !agentManager.cancelCurrentRun()) {
            Toast.makeText(this, R.string.chat_nothing_to_cancel, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.chat_run_cancelled, Toast.LENGTH_SHORT).show();
    }

    private void exportChatToTxt() {
        if (messages == null || messages.isEmpty()) {
            Toast.makeText(this, R.string.chat_recent_changes_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (ChatMessage msg : messages) {
            if (msg.isCheckpoint() || msg.isAwaitingUser()) continue;

            String date = sdf.format(new Date(msg.getTimestamp()));
            String role;
            if (msg.isUser()) {
                role = "USER";
            } else if (msg.isBot()) {
                role = "AI";
            } else if (msg.isTool()) {
                role = "TOOL (" + msg.getToolName() + ")";
            } else {
                role = "SYSTEM";
            }

            sb.append("[").append(date).append("] ").append(role).append(":\n");
            
            if (msg.isTool()) {
                sb.append("Args: ").append(msg.getToolArgs()).append("\n");
                if (msg.getToolResult() != null) {
                    sb.append("Result: ").append(msg.getToolResult()).append("\n");
                }
            } else {
                sb.append(msg.getMessage()).append("\n");
            }

            if (msg.getReasoning() != null && !msg.getReasoning().isEmpty()) {
                sb.append("\nReasoning:\n").append(msg.getReasoning()).append("\n");
            }
            sb.append("\n------------------\n\n");
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Sketchware IA Chat Export - " + sc_id);
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, getString(R.string.chat_menu_export_chat)));
    }

    public void rollbackLastCheckpoint() {
        if (isProcessing) {
            Toast.makeText(this, R.string.chat_wait_processing, Toast.LENGTH_SHORT).show();
            return;
        }
        if (agentManager == null) {
            return;
        }

        ChatCheckpointManager.RollbackResult result = agentManager.rollbackLastCheckpoint();
        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
        if (!result.success) {
            return;
        }

        ChatMessage rollbackMsg = new ChatMessage(
                result.message,
                ChatMessage.TYPE_CHECKPOINT,
                System.currentTimeMillis(),
                "Rollback"
        );
        messages.add(rollbackMsg);
        messageAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        saveChatHistory();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_REFERENCE_IMAGE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    getContentResolver().takePersistableUriPermission(uri, flags);
                } catch (Exception ignored) {
                }
                ChatReference reference = ChatReferenceManager.fromImageUri(this, uri);
                if (addPendingReference(reference)) {
                    insertMention(reference, false);
                    Toast.makeText(this, R.string.chat_reference_image_added, Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String spokenText = result.get(0);
                    editTextMessage.setText(spokenText);
                    editTextMessage.setSelection(spokenText.length());
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        if (agentManager != null) {
            agentManager.cancelCurrentRun();
        }
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
