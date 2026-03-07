package pro.sketchware.activities.chat;

import android.content.SharedPreferences;
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

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class ChatActivity extends AppCompatActivity {
    private String sc_id;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageView btnSend;
    private ImageView btnMic;
    private View btnModelSelector;
    private TextView textCurrentModel;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;
    private GroqClient groqClient;
    private ExecutorService executorService;
    private TextView textTyping;
    private long lastMessageTime = 0; // Timestamp do último envio de mensagem
    private static final long MIN_MESSAGE_INTERVAL_MS = 2000; // Intervalo mínimo de 2 segundos entre mensagens
    private boolean isProcessing = false; // Flag para indicar se está processando uma mensagem
    private ChatHistoryManager historyManager;
    private static final String WELCOME_MESSAGE_PREFIX = "Hello! How can I help you with";
    private boolean showDebug = false; // Flag para controlar exibição de mensagens de debug

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        sc_id = getIntent().getStringExtra("sc_id");
        if (sc_id == null || sc_id.isEmpty()) {
            Toast.makeText(this, "ID do projeto não encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        groqClient = GroqClient.getInstance();
        executorService = Executors.newSingleThreadExecutor();
        historyManager = new ChatHistoryManager(this);
        
        // Carregar preferência de debug
        SharedPreferences prefs = getSharedPreferences("chat_settings", MODE_PRIVATE);
        showDebug = prefs.getBoolean("show_debug", false);
        
        setupToolbar();
        setupViews();
        loadProjectInfo();
        
        // Carregar histórico do chat
        loadChatHistory();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chat");
        }
    }

    private void setupViews() {
        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.btn_send);
        btnMic = findViewById(R.id.btn_mic);
        textTyping = findViewById(R.id.text_typing);

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
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> resultList = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (resultList != null && !resultList.isEmpty()) {
                            String spokenText = resultList.get(0);
                            String currentText = editTextMessage.getText().toString();
                            if (!currentText.isEmpty()) {
                                editTextMessage.setText(currentText + " " + spokenText);
                            } else {
                                editTextMessage.setText(spokenText);
                            }
                            editTextMessage.setSelection(editTextMessage.getText().length());
                        }
                    }
                }
        );
        
        btnMic.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale agora...");
            try {
                speechRecognizerLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Seu dispositivo não suporta entrada de voz", Toast.LENGTH_SHORT).show();
            }
        });
        // Configuração do Seletor de Modelo
        btnModelSelector = findViewById(R.id.btn_model_selector);
        textCurrentModel = findViewById(R.id.text_current_model);
        
        SharedPreferences prefs = getSharedPreferences("ia_settings", MODE_PRIVATE);
        String currentProvider = prefs.getString("current_ai_provider", "groq");
        updateModelUI(currentProvider);
        
        btnModelSelector.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(ChatActivity.this, btnModelSelector);
            popup.getMenu().add(0, 1, 0, "Groq (Llama 3.1 8B)");
            popup.getMenu().add(0, 2, 0, "OpenAI (GPT-4o Mini)");
            popup.getMenu().add(0, 3, 0, "Google Gemini (1.5 Pro)");
            
            popup.setOnMenuItemClickListener(item -> {
                SharedPreferences.Editor editor = prefs.edit();
                String provider = "groq";
                String model = "llama-3.1-8b-instant";
                
                switch (item.getItemId()) {
                    case 2:
                        provider = "openai";
                        model = "gpt-4o-mini";
                        break;
                    case 3:
                        provider = "gemini";
                        model = "gemini-1.5-pro";
                        break;
                }
                
                editor.putString("current_ai_provider", provider);
                editor.putString("current_ai_model", model);
                editor.apply();
                
                updateModelUI(provider);
                Toast.makeText(ChatActivity.this, "Modelo alterado para " + item.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });
    }

    private void updateModelUI(String provider) {
        if (textCurrentModel != null) {
            switch (provider) {
                case "openai":
                    textCurrentModel.setText("GPT-4o Mini");
                    break;
                case "gemini":
                    textCurrentModel.setText("Gemini 1.5 Pro");
                    break;
                case "groq":
                default:
                    textCurrentModel.setText("Llama 3.1 8B");
                    break;
            }
        }
    }

    private void loadProjectInfo() {
        HashMap<String, Object> projectInfo = lC.b(sc_id);
        if (projectInfo != null) {
            String projectName = yB.c(projectInfo, "my_ws_name");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Chat - " + projectName);
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
        String projectName = projectInfo != null ? yB.c(projectInfo, "my_ws_name") : "this project";
        String welcomeMessage = "Hello! How can I help you with " + projectName + "?";
        messages.add(new ChatMessage(welcomeMessage, false, System.currentTimeMillis()));
        messageAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        saveChatHistory();
    }
    
    private void saveChatHistory() {
        if (historyManager != null && sc_id != null) {
            historyManager.saveHistory(sc_id, messages);
        }
    }

    private void sendMessage(String message) {
        // Verificar se já está processando uma mensagem
        if (isProcessing) {
            Toast.makeText(this, "Please wait for the current message to be processed", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Verificar rate limiting - intervalo mínimo entre mensagens
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMessage = currentTime - lastMessageTime;
        
        if (timeSinceLastMessage < MIN_MESSAGE_INTERVAL_MS && lastMessageTime > 0) {
            long remainingSeconds = (MIN_MESSAGE_INTERVAL_MS - timeSinceLastMessage) / 1000 + 1;
            Toast.makeText(this, "Please wait " + remainingSeconds + " second(s) before sending another message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Atualizar timestamp do último envio
        lastMessageTime = currentTime;
        isProcessing = true;
        
        // Adicionar mensagem do usuário
        messages.add(new ChatMessage(message, true, System.currentTimeMillis()));
        messageAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        saveChatHistory();

        // Desabilitar input enquanto processa
        setInputEnabled(false);
        showProgress(true);

        // Enviar para Groq em thread separada
        executorService.execute(() -> {
            try {
                // Coletar últimas 3 mensagens do chat (excluindo debug) para contexto
                JSONArray chatHistory = getLastChatMessages(3);
                
                // Construir contexto dinâmico baseado na mensagem do usuário
                StringBuilder contextMessage = new StringBuilder();
                contextMessage.append(message);
                
                // 1. Busca semântica - encontrar arquivos relevantes
                List<SemanticFileSearcher.SearchResult> relevantFiles = SemanticFileSearcher.searchRelevantFiles(message, sc_id);
                if (!relevantFiles.isEmpty()) {
                    contextMessage.append("\n\n**Relevant files found:**\n");
                    for (SemanticFileSearcher.SearchResult result : relevantFiles) {
                        contextMessage.append("- ").append(result.filePath).append(" (").append(result.relevance).append(")\n");
                        if (result.snippet != null && !result.snippet.isEmpty()) {
                            contextMessage.append("  Snippet: ").append(result.snippet.substring(0, Math.min(100, result.snippet.length()))).append("...\n");
                        }
                    }
                }
                
                // 2. Erros de compilação - se houver
                if (CompileErrorCapture.hasCompileErrors(sc_id)) {
                    String compileErrors = CompileErrorCapture.getLastCompileErrors(sc_id);
                    if (compileErrors != null && !compileErrors.trim().isEmpty()) {
                        contextMessage.append("\n\n").append(CompileErrorCapture.extractErrorSummary(compileErrors));
                    }
                }
                
                // 3. Mudanças recentes - diffs das últimas edições
                String changesSummary = FileChangeTracker.generateChangesSummary();
                if (!changesSummary.equals("No recent changes")) {
                    contextMessage.append("\n\n").append(changesSummary);
                }
                
                // Criar definição da tool no formato MCP (JSON Schema)
                JSONArray tools = createMCPTools();
                
                // Enviar para Groq com tools MCP - mensagem + contexto dinâmico + histórico
                String response = groqClient.sendMessage(contextMessage.toString(), tools, chatHistory);
                
                // Processar resposta MCP (pode ser tool_calls ou texto normal)
                processMCPResponse(response);

            } catch (IOException e) {
                runOnUiThread(() -> {
                    isProcessing = false;
                    showProgress(false);
                    setInputEnabled(true);
                    
                    String errorMessage = e.getMessage();
                    // Verificar se é erro de rate limit
                    if (errorMessage != null && (errorMessage.contains("429") || errorMessage.contains("rate limit") || errorMessage.contains("Rate limit") || errorMessage.contains("tokens per minute"))) {
                        // Tentar extrair informações do rate limit
                        String waitTime = "";
                        try {
                            if (errorMessage.contains("Please try again in")) {
                                int start = errorMessage.indexOf("Please try again in") + 20;
                                int end = errorMessage.indexOf("s", start);
                                if (end > start) {
                                    waitTime = errorMessage.substring(start, end).trim();
                                }
                            }
                        } catch (Exception ignored) {}
                        
                        if (!waitTime.isEmpty()) {
                            errorMessage = "Limite de taxa atingido. Por favor, aguarde " + waitTime + " segundos antes de tentar novamente.";
                        } else {
                            errorMessage = "Limite de taxa atingido. Por favor, aguarde alguns segundos antes de enviar outra mensagem.";
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    } else {
                        errorMessage = "Erro ao enviar mensagem: " + (errorMessage != null ? errorMessage : "Erro desconhecido");
                        Toast.makeText(this, "Erro ao comunicar com Groq", Toast.LENGTH_SHORT).show();
                    }
                    
                    messages.add(new ChatMessage(errorMessage, false, System.currentTimeMillis()));
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                    saveChatHistory();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    isProcessing = false;
                    showProgress(false);
                    setInputEnabled(true);
                    String errorMessage = "Error: " + e.getMessage();
                    messages.add(new ChatMessage(errorMessage, false, System.currentTimeMillis()));
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                    saveChatHistory();
                });
            }
        });
    }

    private void setInputEnabled(boolean enabled) {
        editTextMessage.setEnabled(enabled);
        if (btnSend != null) btnSend.setEnabled(enabled);
        if (btnMic != null) btnMic.setEnabled(enabled);
    }

    private void showProgress(boolean show) {
        if (textTyping != null) {
            textTyping.setVisibility(show ? View.VISIBLE : View.GONE);
        }
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
            debugItem.setTitle(showDebug ? "Hide Debug" : "Show Debug");
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
        } else if (item.getItemId() == R.id.menu_toggle_debug) {
            // Toggle debug
            showDebug = !showDebug;
            item.setChecked(showDebug);
            item.setTitle(showDebug ? "Hide Debug" : "Show Debug");
            
            // Salvar preferência
            SharedPreferences prefs = getSharedPreferences("chat_settings", MODE_PRIVATE);
            prefs.edit().putBoolean("show_debug", showDebug).apply();
            
            Toast.makeText(this, showDebug ? "Debug messages enabled" : "Debug messages disabled", Toast.LENGTH_SHORT).show();
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
        
        Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
    }

    /**
     * Cria as definições de tools no formato MCP (JSON Schema).
     * Não remove nenhuma ferramenta existente; apenas organiza e documenta o uso interno pelo agente.
     */
    private JSONArray createMCPTools() {
        JSONArray tools = new JSONArray();
        try {
            // 1. Tool: list_project_files
            JSONObject listTool = new JSONObject();
            listTool.put("type", "function");
            JSONObject listFunc = new JSONObject();
            listFunc.put("name", "list_project_files");
            listFunc.put("description", "Lists all files inside the current Sketchware project recursively. Optionally pass a glob pattern to filter results.");
            JSONObject listParams = new JSONObject();
            listParams.put("type", "object");
            JSONObject listProps = new JSONObject();
            listProps.put("pattern", new JSONObject()
                    .put("type", "string")
                    .put("description", "Glob pattern to filter files. Examples: '**/*.java' or 'data/**/*.json'. Default is '**/*'."));
            listParams.put("properties", listProps);
            listFunc.put("parameters", listParams);
            listTool.put("function", listFunc);
            tools.put(listTool);
            
            // 2. Tool: read_file
            JSONObject readTool = new JSONObject();
            readTool.put("type", "function");
            JSONObject readFunc = new JSONObject();
            readFunc.put("name", "read_file");
            readFunc.put("description", "Reads the contents of a file in the project. Automatically handles Sketchware's native file encryption. Pass a relative path like 'data/project.json' or 'app/src/main/java/MainActivity.java'.");
            JSONObject readParams = new JSONObject();
            readParams.put("type", "object");
            JSONObject readProps = new JSONObject();
            readProps.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Relative path to the file to read."));
            readParams.put("properties", readProps);
            readParams.put("required", new JSONArray().put("file_path"));
            readFunc.put("parameters", readParams);
            readTool.put("function", readFunc);
            tools.put(readTool);

            // 3. Tool: write_file
            JSONObject writeTool = new JSONObject();
            writeTool.put("type", "function");
            JSONObject writeFunc = new JSONObject();
            writeFunc.put("name", "write_file");
            writeFunc.put("description", "Writes or overwrites a file with new content. Automatically encrypts the file back if necessary.");
            JSONObject writeParams = new JSONObject();
            writeParams.put("type", "object");
            JSONObject writeProps = new JSONObject();
            writeProps.put("file_path", new JSONObject()
                    .put("type", "string")
                    .put("description", "Relative path to the file to create or overwrite."));
            writeProps.put("content", new JSONObject()
                    .put("type", "string")
                    .put("description", "The complete new content of the file."));
            writeParams.put("properties", writeProps);
            writeParams.put("required", new JSONArray().put("file_path").put("content"));
            writeFunc.put("parameters", writeParams);
            writeTool.put("function", writeFunc);
            tools.put(writeTool);

            // 4. Tool: search_project
            JSONObject searchTool = new JSONObject();
            searchTool.put("type", "function");
            JSONObject searchFunc = new JSONObject();
            searchFunc.put("name", "search_project");
            searchFunc.put("description", "Searches for a specific keyword or pattern across all text files in the project. Returns files and matched lines.");
            JSONObject searchParams = new JSONObject();
            searchParams.put("type", "object");
            JSONObject searchProps = new JSONObject();
            searchProps.put("query", new JSONObject()
                    .put("type", "string")
                    .put("description", "The string or regex to search for."));
            searchProps.put("use_regex", new JSONObject()
                    .put("type", "boolean")
                    .put("description", "True if the query is a regular expression, false for literal match. Default is false."));
            searchParams.put("properties", searchProps);
            searchParams.put("required", new JSONArray().put("query"));
            searchFunc.put("parameters", searchParams);
            searchTool.put("function", searchFunc);
            tools.put(searchTool);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tools;
    }
    
    /**
     * Processa a resposta do Groq seguindo protocolo MCP.
     * IMPORTANTE: A detecção de tool_calls é feita APENAS no GroqClient quando a resposta
     * chega da API do Groq. Este método apenas processa o resultado já detectado.
     * 
     * REGRA: Tool_calls NÃO são exibidos no chat. Apenas respostas finais (sem tool_calls)
     * são exibidas. Quando detecta tool_calls, executa as ferramentas e retorna os resultados
     * de volta para o Groq, que então retorna uma resposta final para exibir.
     */
    private void processMCPResponse(String response) throws Exception {
        // A resposta do GroqClient já vem formatada como JSON quando detecta tool_calls
        // Verificar se é uma resposta com tool_calls (formato MCP retornado pelo GroqClient)
        if (response.trim().startsWith("{")) {
            try {
                JSONObject mcpResponse = new JSONObject(response);
                // Verificar se o tipo é "tool_calls" (já detectado pelo GroqClient na resposta da API)
                if ("tool_calls".equals(mcpResponse.optString("type"))) {
                    // Tool_calls detectados: executar ferramentas e retornar resultados para o Groq
                    JSONArray toolCalls = mcpResponse.getJSONArray("tool_calls");
                    
                    // Exibir indicador simples de que ferramentas estão sendo executadas removido
                    // As UI ricas de ferramentas cuidarão disso agora.
                    
                    // Processar tool_calls: executar ferramentas e enviar resultados de volta para o Groq
                    processMCPToolCalls(toolCalls);
                    return;
                }
            } catch (Exception e) {
                // Não é JSON MCP formatado pelo GroqClient, tratar como resposta normal de texto
            }
        }
        
        // Resposta final de texto (sem tool_calls) - EXIBIR no chat
        runOnUiThread(() -> {
            isProcessing = false;
            showProgress(false);
            setInputEnabled(true);
            messages.add(new ChatMessage(response, false, System.currentTimeMillis()));
            messageAdapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            saveChatHistory();
        });
    }
    
    private void processMCPToolCalls(JSONArray toolCalls) throws Exception {
        JSONArray toolResults = new JSONArray();
        
        // Processar cada tool_call
        for (int i = 0; i < toolCalls.length(); i++) {
            JSONObject toolCall = toolCalls.optJSONObject(i);
            if (toolCall == null) continue;
            
            String toolCallId = toolCall.optString("id", "");
            JSONObject function = toolCall.optJSONObject("function");
            
            String functionName = "";
            String arguments = "{}";
            
            if (function != null) {
                functionName = function.optString("name", "");
                arguments = function.optString("arguments", "{}");
                
                // Tratar caso em que Groq retorna string "null" ou vazio
                if (arguments == null || arguments.equals("null") || arguments.trim().isEmpty()) {
                    arguments = "{}";
                }
            }
            
            // Criar a mensagem UI da ferramenta (AntiGravity style)
            final ChatMessage toolMsg = new ChatMessage(functionName, arguments, System.currentTimeMillis());
            
            // Exibir a UI da ferramenta rodando
            runOnUiThread(() -> {
                messages.add(toolMsg);
                messageAdapter.notifyItemInserted(messages.size() - 1);
                scrollToBottom();
            });
            
            JSONObject toolResult = new JSONObject();
            toolResult.put("tool_call_id", toolCallId);
            String resultText = "";
            
            try {
                if ("list_project_files".equals(functionName)) {
                    JSONObject params = new JSONObject(arguments);
                    String pattern = params.optString("pattern", "**/*");
                    
                    List<ProjectFileDiscovery.FileInfo> files = GlobFileSearch.search(sc_id, pattern);
                    if (files.isEmpty()) {
                        resultText = "No files found matching: " + pattern;
                    } else {
                        StringBuilder sb = new StringBuilder("Found " + files.size() + " files:\n");
                        for (ProjectFileDiscovery.FileInfo f : files) {
                            sb.append(f.path).append(" (").append(f.size).append(" bytes");
                            if (f.isEncrypted) sb.append(", encrypted");
                            sb.append(")\n");
                        }
                        resultText = sb.toString();
                    }
                } else if ("read_file".equals(functionName)) {
                    JSONObject params = new JSONObject(arguments);
                    String filePath = params.getString("file_path");
                    
                    String content = SketchwareFileDecryptor.decryptFile(filePath);
                    if (content != null) {
                        resultText = content;
                    } else {
                        resultText = "Error: File not found or could not read " + filePath;
                    }
                } else if ("write_file".equals(functionName)) {
                    JSONObject params = new JSONObject(arguments);
                    String filePath = params.getString("file_path");
                    String content = params.getString("content");
                    
                    // Remover extensões incorretas adicionadas por IAs
                    if ((filePath.startsWith("data/") || filePath.startsWith("mysc/")) && 
                        (filePath.endsWith(".json") || filePath.endsWith(".xml"))) {
                        filePath = filePath.substring(0, filePath.lastIndexOf("."));
                    }
                    
                    // O próprio SketchwareFileEncryptor agora decide se usa AES de forma inteligente
                    // baseado no conteúdo existente ou caminho do arquivo
                    boolean saved = SketchwareFileEncryptor.encryptAndSaveFile(filePath, content);
                    
                    resultText = saved ? "File written successfully: " + filePath : "Error: Failed to write file " + filePath;
                } else if ("search_project".equals(functionName)) {
                    JSONObject params = new JSONObject(arguments);
                    String query = params.getString("query");
                    boolean useRegex = params.optBoolean("use_regex", false);
                    
                    List<CodeGrep.GrepResult> results = CodeGrep.searchInProject(sc_id, query, useRegex, null);
                    if (results.isEmpty()) {
                        resultText = "No matches found for query: " + query;
                    } else {
                        StringBuilder sb = new StringBuilder("Found " + results.size() + " matches:\n");
                        String currentFile = null;
                        for (CodeGrep.GrepResult r : results) {
                            if (!r.filePath.equals(currentFile)) {
                                currentFile = r.filePath;
                                sb.append("\n**").append(r.filePath).append("**:\n");
                            }
                            sb.append(r.lineNumber).append(": ").append(r.lineContent).append("\n");
                        }
                        resultText = sb.toString();
                    }
                } else {
                    resultText = "Error: Tool '" + functionName + "' is not supported.";
                }
            } catch (Exception e) {
                resultText = "Error executing tool: " + e.getMessage();
            }
            
            toolResult.put("content", new JSONArray().put(new JSONObject()
                .put("type", "text")
                .put("text", resultText)
            ));
            toolResults.put(toolResult);
            
            // Atualizar UI da ferramenta concluída
            final String finalResult = resultText;
            runOnUiThread(() -> {
                toolMsg.setToolRunning(false);
                
                // Truncar para a UI não travar com arquivos gigantes
                if (finalResult.length() > 600) {
                    toolMsg.setToolResult(finalResult.substring(0, 600) + "\n... [output truncated for UI view, but full output sent to AI]");
                } else {
                    toolMsg.setToolResult(finalResult);
                }
                
                int index = messages.indexOf(toolMsg);
                if (index != -1) {
                    messageAdapter.notifyItemChanged(index);
                }
                scrollToBottom();
            });
        }
        
        // Enviar tool_results de volta para o Groq (protocolo MCP/Oficial)
        if (toolResults.length() > 0) {
            executorService.execute(() -> {
                try {
                    // Montar uma Array de mensagens limpa na especificação ofical OpenAI/Groq Tool Calling
                    JSONArray messagesArray = new JSONArray();
                    
                    // 1. Pegar histórico recente do chat
                    JSONArray chatHistory = getLastChatMessages(6); // Aumentar histórico para melhor contexto
                    if (chatHistory != null) {
                        for (int i = 0; i < chatHistory.length(); i++) {
                            messagesArray.put(chatHistory.getJSONObject(i));
                        }
                    }
                    
                    // 2. Adicionar a "Assistant Message" que gerou as chamadas (obrigatório pelo protocolo)
                    JSONObject assistantMsg = new JSONObject();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", JSONObject.NULL); // Normalmente null quando há tool_calls
                    assistantMsg.put("tool_calls", toolCalls);
                    messagesArray.put(assistantMsg);
                    
                    // 3. Adicionar cada "Tool Message" com seus respectivos IDs e resultados
                    final int MAX_CONTENT_LENGTH = 15000; // Limite flexível para tokens
                    
                    for (int i = 0; i < toolResults.length(); i++) {
                        JSONObject result = toolResults.getJSONObject(i);
                        String tCallId = result.getString("tool_call_id");
                        JSONArray contentArray = result.getJSONArray("content");
                        
                        String finalContentText = "";
                        if (contentArray.length() > 0) {
                            JSONObject textContent = contentArray.getJSONObject(0);
                            String rawText = textContent.getString("text");
                            
                            // Truncar preventivamente se explodir o limite
                            if (rawText.length() > MAX_CONTENT_LENGTH) {
                                rawText = rawText.substring(0, MAX_CONTENT_LENGTH) + "\n\n[... output truncado devido ao limite de tokens ...]";
                            }
                            finalContentText = rawText;
                        }
                        
                        // Tool Msg
                        JSONObject toolMsg = new JSONObject();
                        toolMsg.put("role", "tool");
                        toolMsg.put("tool_call_id", tCallId);
                        toolMsg.put("content", finalContentText);
                        messagesArray.put(toolMsg);
                    }
                    
                    // 4. Executar chamada recursiva/agêntica (Groq recebe os tool_results e decide o próximo passo)
                    JSONArray tools = createMCPTools();
                    
                    // Chamando o novo método do GroqClient que aceita mensagens raw
                    String finalResponse = groqClient.sendRawMessages(messagesArray, tools);
                    
                    // Processar a resposta final (se houver novas tools, ele vai fazer recursão automática)
                    processMCPResponse(finalResponse);
                    
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        isProcessing = false;
                        showProgress(false);
                        setInputEnabled(true);
                        String errorMsg = e.getMessage();
                        
                        // Verificar se é erro de limite de taxa (rate limit / 429)
                        if (errorMsg != null && (errorMsg.contains("429") || errorMsg.contains("rate limit") || errorMsg.contains("Rate limit") || errorMsg.contains("tokens per minute"))) {
                            String waitTime = "";
                            try {
                                if (errorMsg.contains("Please try again in")) {
                                    int start = errorMsg.indexOf("Please try again in") + 20;
                                    int end = errorMsg.indexOf("s", start);
                                    if (end > start) {
                                        waitTime = errorMsg.substring(start, end).trim();
                                    }
                                }
                            } catch (Exception ignored) {}
                            
                            if (!waitTime.isEmpty()) {
                                errorMsg = "Limite de taxa atingido. Por favor, aguarde " + waitTime + " segundos antes de tentar novamente.";
                            } else {
                                errorMsg = "Limite de taxa atingido. Por favor, aguarde alguns segundos antes de enviar outra mensagem.";
                            }
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        } else {
                            errorMsg = "Erro ao processar resultados (Tool Loop): " + (errorMsg != null ? errorMsg : "Erro desconhecido");
                        }
                        
                        messages.add(new ChatMessage(errorMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                        saveChatHistory();
                    });
                    e.printStackTrace();
                }
            });
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
            String messageText = msg.getMessage();
            
            // Filtrar mensagens de debug
            if (messageText != null && 
                !messageText.startsWith("🔍 DEBUG") && 
                !messageText.startsWith("🔧 Executando:")) {
                
                try {
                    JSONObject historyMsg = new JSONObject();
                    historyMsg.put("role", msg.isUser() ? "user" : "assistant");
                    historyMsg.put("content", messageText);
                    history.put(0, historyMsg); // Inserir no início para manter ordem cronológica
                    collected++;
                } catch (Exception e) {
                    // Ignorar erros ao criar JSON
                }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

