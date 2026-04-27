package pro.sketchware.activities.chat;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;
import android.widget.EditText;
import android.content.Intent;
import android.speech.RecognizerIntent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.Locale;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a.a.a.lC;
import a.a.a.yB;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.network.GroqClient;
import pro.sketchware.network.MorphClient;
import pro.sketchware.util.SketchwareFileDecryptor;
import pro.sketchware.util.SketchwareFileEditor;
import pro.sketchware.util.SketchwareFileEncryptor;
import pro.sketchware.util.FileChangeTracker;
import pro.sketchware.util.SemanticFileSearcher;
import pro.sketchware.util.CompileErrorCapture;
import pro.sketchware.util.CodeGrep;
import pro.sketchware.util.GlobFileSearch;
import pro.sketchware.util.ProjectFileDiscovery;
import pro.sketchware.util.list_path_and_files;
import pro.sketchware.ia.tools.ToolManager;
import pro.sketchware.utility.TranslationFunction;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class ChatActivity extends AppCompatActivity {
    private String sc_id;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private View btnSend;
    private View btnChatMode;
    private View btnModelSelector;
    private TextView textChatMode;
    private TextView textCurrentModel;
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;
    private GroqClient groqClient;
    private ExecutorService executorService;
    private long lastMessageTime = 0; // Timestamp do último envio de mensagem
    private static final long MIN_MESSAGE_INTERVAL_MS = 2000; // Intervalo mínimo de 2 segundos entre mensagens
    private boolean isProcessing = false; // Flag para indicar se está processando uma mensagem
    private ChatHistoryManager historyManager;
    private boolean showDebug = false; // Flag para controlar exibição de mensagens de debug
    private AgentManager agentManager;

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

        groqClient = GroqClient.getInstance();
        executorService = Executors.newSingleThreadExecutor();
        historyManager = new ChatHistoryManager(this);

        // Carregar preferência de debug
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
                });
            }

            @Override
            public void onMessageRemoved(ChatMessage message, int index) {
                runOnUiThread(() -> {
                    messageAdapter.notifyItemRemoved(index);
                    saveChatHistory();
                });
            }

            @Override
            public void onStatusChanged(String status) {
            }

            @Override
            public void onProcessingFinished() {
                runOnUiThread(() -> {
                    isProcessing = false;
                    showProgress(false);
                    setInputEnabled(true);
                    saveChatHistory();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, error, Toast.LENGTH_LONG).show();
                    ChatMessage errorMsg = new ChatMessage("Error: " + error, false, System.currentTimeMillis());
                    messages.add(errorMsg);
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                    saveChatHistory();
                });
            }
        });

        // Carregar histórico do chat
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

        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.btn_send);
        editTextMessage.setHint(R.string.chat_input_hint);

        messages = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(messages);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        // Configurar ícone de enviar e listener
        btnSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                editTextMessage.setText("");
            }
        });

        // Configurar Speech-to-Text
        // Configuração do Seletor de Modelo
        btnChatMode = findViewById(R.id.btn_chat_mode);
        btnModelSelector = findViewById(R.id.btn_model_selector);
        textChatMode = findViewById(R.id.text_chat_mode);
        textCurrentModel = findViewById(R.id.text_current_model);

        SharedPreferences prefs = AiChatSettingsHelper.prefs(this);
        AiChatSettingsHelper.ensureValidCurrentSelection(prefs);
        updateChatModeUI();
        updateModelUI();

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
        // Carregar histórico salvo
        List<ChatMessage> savedMessages = historyManager.loadHistory(sc_id);

        if (savedMessages != null && !savedMessages.isEmpty()) {
            // Se já tem histórico, usar ele
            messages.addAll(savedMessages);
            messageAdapter.notifyDataSetChanged();
            scrollToBottom();
        } else {
            // Se não tem histórico, adicionar mensagem de boas-vindas
            addWelcomeMessage();
        }
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
        // Verificar se já está processando uma mensagem
        if (isProcessing) {
            Toast.makeText(this, R.string.chat_wait_processing, Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar rate limiting - intervalo mínimo entre mensagens
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMessage = currentTime - lastMessageTime;

        if (timeSinceLastMessage < MIN_MESSAGE_INTERVAL_MS && lastMessageTime > 0) {
            long remainingSeconds = (MIN_MESSAGE_INTERVAL_MS - timeSinceLastMessage) / 1000 + 1;
            Toast.makeText(this, getString(R.string.chat_wait_before_sending, remainingSeconds), Toast.LENGTH_SHORT).show();
            return;
        }

        // Atualizar timestamp do último envio
        lastMessageTime = currentTime;
        isProcessing = true;

        // Desabilitar input enquanto processa
        setInputEnabled(false);
        showProgress(true);

        // Delegar para AgentManager (Streaming e Agêntico)
        agentManager.processUserMessage(message);
    }

    private void setInputEnabled(boolean enabled) {
        editTextMessage.setEnabled(enabled);
        if (btnSend != null) btnSend.setEnabled(enabled);
    }

    private void showProgress(boolean show) {
    }

    private void scrollToBottom() {
        if (messages.size() > 0) {
            recyclerViewMessages.scrollToPosition(messages.size() - 1);
        }
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
        } else if (item.getItemId() == R.id.menu_cancel_run) {
            cancelCurrentRun();
            return true;
        } else if (item.getItemId() == R.id.menu_rollback_checkpoint) {
            rollbackLastCheckpoint();
            return true;
        } else if (item.getItemId() == R.id.menu_toggle_debug) {
            // Toggle debug
            showDebug = !showDebug;
            item.setChecked(showDebug);
            item.setTitle(showDebug ? R.string.chat_menu_hide_debug : R.string.chat_menu_show_debug);

            // Salvar preferência
            SharedPreferences prefs = getSharedPreferences("chat_settings", MODE_PRIVATE);
            prefs.edit().putBoolean("show_debug", showDebug).apply();

            Toast.makeText(this, showDebug ? R.string.chat_debug_enabled : R.string.chat_debug_disabled, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearChat() {
        // Limpar histórico salvo
        if (historyManager != null && sc_id != null) {
            historyManager.clearHistory(sc_id);
        }

        // Limpar lista de mensagens
        int messageCount = messages.size();
        messages.clear();
        messageAdapter.notifyItemRangeRemoved(0, messageCount);

        // Adicionar mensagem de boas-vindas novamente
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
    protected void onDestroy() {
        if (agentManager != null) {
            agentManager.cancelCurrentRun();
        }
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }



    /**
     * Coleta as últimas N mensagens do chat (excluindo mensagens de debug)
     * para enviar como contexto para o Groq
     * @param count Número de mensagens a coletar
     * @return JSONArray com as mensagens no formato {role: "user"/"assistant", content: "..."}
     */
    private JSONArray getLastChatMessages(int count) {
        JSONArray history = new JSONArray();

        // Coletar mensagens de trás para frente, excluindo debug
        int collected = 0;
        for (int i = messages.size() - 1; i >= 0 && collected < count; i--) {
            ChatMessage msg = messages.get(i);

            try {
                JSONObject historyMsg = new JSONObject();

                if (msg.isUser()) {
                    historyMsg.put("role", "user");
                    historyMsg.put("content", msg.getMessage());
                } else if (msg.getType() == ChatMessage.TYPE_TOOL) {
                    // Mensagens de ferramenta são especiais: se já concluídas, enviamos como conteúdo do assistente
                    // ou mantemos o fluxo se o provedor suportar. No Groq, o ideal é o fluxo tool_calls -> tool results.
                    // Para histórico simples, enviamos o resultado se disponível.
                    historyMsg.put("role", "assistant");
                    String toolInfo = "Tool used: " + msg.getToolName() + "\nResult: " + (msg.getToolResult() != null ? msg.getToolResult() : "Running...");
                    historyMsg.put("content", toolInfo);
                } else {
                    historyMsg.put("role", "assistant");
                    historyMsg.put("content", msg.getMessage());
                }

                String content = historyMsg.optString("content", "");
                if (content != null && !content.isEmpty() && !content.startsWith("🔍 DEBUG")) {
                    history.put(0, historyMsg); // Inserir no início para manter ordem cronológica
                    collected++;
                }
            } catch (Exception e) {
                // Ignorar erros ao criar JSON
            }
        }

        return history;
    }

    /**
     * Extrai um item específico por ID do JSON descriptografado
     * @param jsonContent Conteúdo JSON descriptografado
     * @param itemId ID do item a buscar (ex: "id_11", "11", etc)
     * @return JSON do item encontrado ou null se não encontrado
     */
    /**
     * Salva um arquivo como texto plano (não criptografado)
     */
    private boolean savePlainTextFile(String filePath, String content) {
        try {
            File file = resolveFilePath(filePath);
            if (file == null) {
                return false;
            }

            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            java.nio.file.Files.write(file.toPath(), content.getBytes("UTF-8"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Resolve o caminho completo do arquivo
     * Aceita qualquer caminho dentro de .sketchware
     */
    private File resolveFilePath(String relativePath) {
        String normalizedPath = relativePath.replace("\\", File.separator);

        if (normalizedPath.startsWith(File.separator)) {
            normalizedPath = normalizedPath.substring(1);
        }

        File baseDir = Environment.getExternalStorageDirectory();

        // Qualquer caminho relativo dentro de .sketchware
        String path = ".sketchware" + File.separator + normalizedPath;
        return new File(baseDir, path);
    }

    /**
     * Deleta um arquivo do projeto
     */
    private boolean deleteProjectFile(String filePath) {
        try {
            File file = resolveFilePath(filePath);
            if (file != null && file.exists()) {
                return file.delete();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String extractItemById(String jsonContent, String itemId) {
        try {
            // Limpar o ID (remover espaços, normalizar)
            String cleanId = itemId.trim();

            // Parsear o JSON
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
            HashMap<String, Object> jsonData = gson.fromJson(jsonContent.trim(), type);

            // Tentar encontrar o item por diferentes formatos de ID
            Object foundItem = null;

            // Tentar buscar diretamente pela chave
            if (jsonData.containsKey(cleanId)) {
                foundItem = jsonData.get(cleanId);
            } else {
                // Tentar buscar em arrays/objetos aninhados
                foundItem = searchItemInJson(jsonData, cleanId);
            }

            if (foundItem != null) {
                // Converter o item encontrado de volta para JSON
                return gson.toJson(foundItem);
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Busca recursivamente um item por ID no JSON
     */
    private Object searchItemInJson(Object jsonObj, String itemId) {
        if (jsonObj == null) {
            return null;
        }

        // Se for um Map, buscar pela chave ou recursivamente
        if (jsonObj instanceof HashMap) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> map = (HashMap<String, Object>) jsonObj;

            // Verificar se a chave existe
            if (map.containsKey(itemId)) {
                return map.get(itemId);
            }

            // Verificar se algum valor tem uma propriedade "id" que corresponde
            for (Object value : map.values()) {
                if (value instanceof HashMap) {
                    @SuppressWarnings("unchecked")
                    HashMap<String, Object> nestedMap = (HashMap<String, Object>) value;
                    Object idValue = nestedMap.get("id");
                    if (idValue != null && idValue.toString().equals(itemId)) {
                        return value;
                    }
                }
            }

            // Buscar recursivamente em valores que são Maps ou Lists
            for (Object value : map.values()) {
                Object found = searchItemInJson(value, itemId);
                if (found != null) {
                    return found;
                }
            }
        }
        // Se for uma List, buscar recursivamente em cada item
        else if (jsonObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) jsonObj;
            for (Object item : list) {
                Object found = searchItemInJson(item, itemId);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

}
