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
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
    private TextInputEditText editTextMessage;
    private TextInputLayout inputLayout;
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
        inputLayout = findViewById(R.id.input_layout_message);
        textTyping = findViewById(R.id.text_typing);

        messages = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(messages);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messageAdapter);

        // Configurar ícone de enviar e listener
        inputLayout.setEndIconDrawable(R.drawable.ic_mtrl_check);
        inputLayout.setEndIconOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                editTextMessage.setText("");
            }
        });
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
        inputLayout.setEnabled(enabled);
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
        try {
            JSONArray tools = new JSONArray();
            
            // Tool decrypt_file seguindo protocolo MCP
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            
            JSONObject function = new JSONObject();
            function.put("name", "decrypt_file");
            function.put("description", "Lê um arquivo do projeto Sketchware para uso interno do assistente. Esta é a ferramenta PRINCIPAL para leitura de arquivos binários/criptografados da pasta .sketchware. O arquivo será descriptografado automaticamente se necessário, ou lido diretamente se não estiver criptografado. Use sempre que precisar entender o conteúdo real de um arquivo do projeto antes de responder ou propor alterações. IMPORTANTE: Use o caminho EXATO como mostrado em list_path_and_files. NÃO adicione extensões (.json, .xml, etc.) ao caminho - arquivos criptografados geralmente não têm extensão.");
            
            // Parâmetros em formato JSON Schema (MCP)
            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");
            JSONObject properties = new JSONObject();
            
            properties.put("file_path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho relativo do arquivo dentro da pasta .sketchware. Use o caminho EXATO como mostrado em list_path_and_files. NÃO adicione extensões (.json, .xml) - arquivos sem extensão são criptografados e devem ser usados sem extensão. Exemplos: data/601/logic (sem extensão), mysc/list/601/project (sem extensão), ou arquivos com extensão como data/601/file.xml")
            );
            
            parameters.put("properties", properties);
            JSONArray required = new JSONArray();
            required.put("file_path");
            parameters.put("required", required);
            
            function.put("parameters", parameters);
            tool.put("function", function);
            
            tools.put(tool);
            
            // Tool get_item_by_id para buscar item específico no JSON
            JSONObject itemTool = new JSONObject();
            itemTool.put("type", "function");
            
            JSONObject itemFunction = new JSONObject();
            itemFunction.put("name", "get_item_by_id");
            itemFunction.put("description", "Busca um item específico por ID em um arquivo JSON do projeto para análise interna do assistente. Útil para extrair apenas um elemento específico de um arquivo JSON grande, como um componente, view ou lógica específica, sem precisar mostrar todo o arquivo ao usuário.");
            
            JSONObject itemParameters = new JSONObject();
            itemParameters.put("type", "object");
            JSONObject itemProperties = new JSONObject();
            
            itemProperties.put("file_path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho relativo do arquivo JSON dentro da pasta .sketchware")
            );
            
            itemProperties.put("item_id", new JSONObject()
                .put("type", "string")
                .put("description", "ID do item a buscar no JSON. Exemplos: id_11, id_5, ou qualquer outro identificador usado no arquivo")
            );
            
            itemParameters.put("properties", itemProperties);
            JSONArray itemRequired = new JSONArray();
            itemRequired.put("file_path");
            itemRequired.put("item_id");
            itemParameters.put("required", itemRequired);
            
            itemFunction.put("parameters", itemParameters);
            itemTool.put("function", itemFunction);
            
            tools.put(itemTool);
            
            // Tool morph_edit_code para modificar arquivos JSON do projeto
            JSONObject morphTool = new JSONObject();
            morphTool.put("type", "function");
            
            JSONObject morphFunction = new JSONObject();
            morphFunction.put("name", "morph_edit_code");
            morphFunction.put("description", "Modifica arquivos do projeto Sketchware usando o Morph, de forma controlada. Use apenas quando o usuário pedir para MODIFICAR, ALTERAR, EDITAR, MUDAR, CORRIGIR ou ATUALIZAR algo específico. O arquivo será descriptografado (se necessário), modificado pelo Morph e criptografado de volta. Aceita qualquer caminho dentro de .sketchware. Sempre explique ao usuário, em linguagem natural, o que foi alterado.");
            
            JSONObject morphParameters = new JSONObject();
            morphParameters.put("type", "object");
            JSONObject morphProperties = new JSONObject();
            
            morphProperties.put("file_path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho relativo do arquivo a modificar dentro da pasta .sketchware")
            );
            
            morphProperties.put("instructions", new JSONObject()
                .put("type", "string")
                .put("description", "Instruções claras e específicas sobre o que modificar no arquivo. Descreva EXATAMENTE o que deve ser alterado, adicionado ou removido. Exemplo: 'Altere o campo my_ws_name de NewProject para MeuProjeto'")
            );
            
            morphProperties.put("code_edit", new JSONObject()
                .put("type", "string")
                .put("description", "Conteúdo editado ou trecho a ser modificado (opcional). Se vazio, o Morph criará a modificação baseado apenas nas instruções")
            );
            
            morphParameters.put("properties", morphProperties);
            JSONArray morphRequired = new JSONArray();
            morphRequired.put("file_path");
            morphRequired.put("instructions");
            morphParameters.put("required", morphRequired);
            
            morphFunction.put("parameters", morphParameters);
            morphTool.put("function", morphFunction);
            
            tools.put(morphTool);
            
            // Tool list_path_and_files para listar recursivamente pastas e arquivos (incluindo binários)
            JSONObject listPathTool = new JSONObject();
            listPathTool.put("type", "function");
            
            JSONObject listPathFunction = new JSONObject();
            listPathFunction.put("name", "list_path_and_files");
            listPathFunction.put("description", "Lista recursivamente pastas, subpastas e arquivos (incluindo binários/criptografados) dentro da pasta .sketchware. Útil para inspecionar toda a estrutura do projeto ou um caminho específico. Retorna um resumo em texto com indicação de quais arquivos são criptografados. IMPORTANTE: Arquivos sem extensão (sem ponto no nome) são criptografados. Ao usar decrypt_file, use o caminho EXATO como mostrado na lista - NÃO adicione extensões (.json, .xml, etc.).");
            
            JSONObject listPathParameters = new JSONObject();
            listPathParameters.put("type", "object");
            JSONObject listPathProperties = new JSONObject();
            
            listPathProperties.put("path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho base opcional dentro de .sketchware para iniciar a listagem recursiva. Se vazio ou omitido, usa a raiz de .sketchware")
            );
            
            listPathProperties.put("search_pattern", new JSONObject()
                .put("type", "string")
                .put("description", "Padrão opcional para filtrar por nome de arquivo ou caminho. Se informado, a listagem foca apenas em itens cujo nome ou caminho contenha esse padrão")
            );
            
            listPathParameters.put("properties", listPathProperties);
            JSONArray listPathRequired = new JSONArray();
            listPathParameters.put("required", listPathRequired);
            
            listPathFunction.put("parameters", listPathParameters);
            listPathTool.put("function", listPathFunction);
            
            tools.put(listPathTool);
            
            // Tool codebase_search para busca semântica
            JSONObject searchTool = new JSONObject();
            searchTool.put("type", "function");
            
            JSONObject searchFunction = new JSONObject();
            searchFunction.put("name", "codebase_search");
            searchFunction.put("description", "Busca semântica no código do projeto para o assistente localizar arquivos e trechos relevantes baseado em palavras-chave e contexto. Use para entender onde funcionalidades são implementadas e como o código está organizado. Retorna os arquivos mais relevantes com snippets e scores de relevância.");
            
            JSONObject searchParameters = new JSONObject();
            searchParameters.put("type", "object");
            JSONObject searchProperties = new JSONObject();
            
            searchProperties.put("query", new JSONObject()
                .put("type", "string")
                .put("description", "Query de busca semântica. Descreva o que você está procurando usando palavras-chave e contexto. Exemplo: 'activity principal do projeto' ou 'onde são definidas as views'")
            );
            
            searchParameters.put("properties", searchProperties);
            JSONArray searchRequired = new JSONArray();
            searchRequired.put("query");
            searchParameters.put("required", searchRequired);
            
            searchFunction.put("parameters", searchParameters);
            searchTool.put("function", searchFunction);
            
            tools.put(searchTool);
            
            // Tool grep para busca por padrões de texto
            JSONObject grepTool = new JSONObject();
            grepTool.put("type", "function");
            
            JSONObject grepFunction = new JSONObject();
            grepFunction.put("name", "grep");
            grepFunction.put("description", "Busca por padrões de texto no código para o assistente localizar símbolos, funções ou termos específicos. Suporta busca exata de strings, símbolos, funções e regex. Mais rápido que codebase_search para buscas exatas. Pode buscar em um arquivo específico ou em todo o projeto.");
            
            JSONObject grepParameters = new JSONObject();
            grepParameters.put("type", "object");
            JSONObject grepProperties = new JSONObject();
            
            grepProperties.put("pattern", new JSONObject()
                .put("type", "string")
                .put("description", "Padrão de busca. Pode ser texto simples ou regex se use_regex for true. Exemplo: 'MainActivity' ou 'class\\s+\\w+' (regex)")
            );
            
            grepProperties.put("file_path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho opcional de um arquivo específico para buscar. Se omitido, busca em todo o projeto")
            );
            
            grepProperties.put("use_regex", new JSONObject()
                .put("type", "boolean")
                .put("description", "Se true, trata o pattern como expressão regular. Se false, faz busca simples (case-insensitive)")
            );
            
            grepProperties.put("file_pattern", new JSONObject()
                .put("type", "string")
                .put("description", "Padrão opcional para filtrar arquivos antes de buscar. Exemplo: '*.java' para buscar apenas em arquivos Java")
            );
            
            grepParameters.put("properties", grepProperties);
            JSONArray grepRequired = new JSONArray();
            grepRequired.put("pattern");
            grepParameters.put("required", grepRequired);
            
            grepFunction.put("parameters", grepParameters);
            grepTool.put("function", grepFunction);
            
            tools.put(grepTool);
            
            // Tool glob_file_search para busca de arquivos por padrão
            JSONObject globTool = new JSONObject();
            globTool.put("type", "function");
            
            JSONObject globFunction = new JSONObject();
            globFunction.put("name", "glob_file_search");
            globFunction.put("description", "Busca arquivos por padrão glob para o assistente localizar rapidamente arquivos por extensão ou padrão de nome. Retorna arquivos encontrados ordenados por data de modificação (mais recentes primeiro).");
            
            JSONObject globParameters = new JSONObject();
            globParameters.put("type", "object");
            JSONObject globProperties = new JSONObject();
            
            globProperties.put("pattern", new JSONObject()
                .put("type", "string")
                .put("description", "Padrão glob para buscar arquivos. Exemplos: '*.java' para arquivos Java, '**/test/**/*.ts' para arquivos TypeScript em pastas test, 'MainActivity.*' para arquivos relacionados a MainActivity")
            );
            
            globParameters.put("properties", globProperties);
            JSONArray globRequired = new JSONArray();
            globRequired.put("pattern");
            globParameters.put("required", globRequired);
            
            globFunction.put("parameters", globParameters);
            globTool.put("function", globFunction);
            
            tools.put(globTool);
            
            // Tool write para escrita de arquivos
            JSONObject writeTool = new JSONObject();
            writeTool.put("type", "function");
            
            JSONObject writeFunction = new JSONObject();
            writeFunction.put("name", "write");
            writeFunction.put("description", "Cria ou sobrescreve um arquivo no projeto de forma controlada. O arquivo será criptografado automaticamente se necessário (baseado na extensão). Use para criar novos arquivos ou substituir completamente o conteúdo de arquivos existentes somente após o usuário confirmar a intenção. Aceita qualquer caminho dentro de .sketchware.");
            
            JSONObject writeParameters = new JSONObject();
            writeParameters.put("type", "object");
            JSONObject writeProperties = new JSONObject();
            
            writeProperties.put("file_path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho relativo do arquivo dentro da pasta .sketchware. Se o arquivo não existir, será criado. Se existir, será sobrescrito")
            );
            
            writeProperties.put("content", new JSONObject()
                .put("type", "string")
                .put("description", "Conteúdo completo a ser escrito no arquivo. O arquivo será criptografado automaticamente se necessário")
            );
            
            writeParameters.put("properties", writeProperties);
            JSONArray writeRequired = new JSONArray();
            writeRequired.put("file_path");
            writeRequired.put("content");
            writeParameters.put("required", writeRequired);
            
            writeFunction.put("parameters", writeParameters);
            writeTool.put("function", writeFunction);
            
            tools.put(writeTool);
            
            // Tool delete_file para exclusão de arquivos
            JSONObject deleteTool = new JSONObject();
            deleteTool.put("type", "function");
            
            JSONObject deleteFunction = new JSONObject();
            deleteFunction.put("name", "delete_file");
            deleteFunction.put("description", "Exclui um arquivo do projeto. Esta ação é permanente e deve ser usada somente após o usuário confirmar claramente qual arquivo deseja remover. Aceita qualquer caminho dentro de .sketchware. Retorna sucesso se o arquivo foi deletado ou se não existia.");
            
            JSONObject deleteParameters = new JSONObject();
            deleteParameters.put("type", "object");
            JSONObject deleteProperties = new JSONObject();
            
            deleteProperties.put("file_path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho relativo do arquivo a deletar dentro da pasta .sketchware")
            );
            
            deleteParameters.put("properties", deleteProperties);
            JSONArray deleteRequired = new JSONArray();
            deleteRequired.put("file_path");
            deleteParameters.put("required", deleteRequired);
            
            deleteFunction.put("parameters", deleteParameters);
            deleteTool.put("function", deleteFunction);
            
            tools.put(deleteTool);
            
            return tools;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        }
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
                    
                    // Exibir indicador simples de que ferramentas estão sendo executadas
                    runOnUiThread(() -> {
                        StringBuilder toolNames = new StringBuilder();
                        for (int i = 0; i < toolCalls.length(); i++) {
                            try {
                                JSONObject toolCall = toolCalls.getJSONObject(i);
                                JSONObject function = toolCall.getJSONObject("function");
                                String functionName = function.getString("name");
                                if (i > 0) toolNames.append(", ");
                                toolNames.append(functionName);
                            } catch (Exception e) {
                                // Ignorar erros ao extrair nome
                            }
                        }
                        String indicator = "🔧 Executando: " + toolNames.toString();
                        messages.add(new ChatMessage(indicator, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                    
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
    
    /**
     * Processa tool_calls no formato MCP e envia tool_results de volta
     */
    private void processMCPToolCalls(JSONArray toolCalls) throws Exception {
        JSONArray toolResults = new JSONArray();
        StringBuilder allDecryptedContent = new StringBuilder();
        
        // Processar cada tool_call
        for (int i = 0; i < toolCalls.length(); i++) {
            JSONObject toolCall = toolCalls.getJSONObject(i);
            String toolCallId = toolCall.getString("id");
            JSONObject function = toolCall.getJSONObject("function");
            String functionName = function.getString("name");
            String arguments = function.getString("arguments");
            
            if ("decrypt_file".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String filePath = params.getString("file_path");
                
                // Ler arquivo (descriptografa se necessário ou lê diretamente)
                String fileContent = SketchwareFileDecryptor.decryptFile(filePath);
                
                // DEBUG: Exibir resultado no chat (se habilitado)
                if (showDebug) {
                    String debugResult = fileContent != null ? fileContent : "Erro: Não foi possível acessar o conteúdo solicitado.";
                    // Limitar tamanho para exibição (primeiros 500 caracteres)
                    String debugPreview = debugResult.length() > 500 
                        ? debugResult.substring(0, 500) + "\n\n[... conteúdo truncado para debug ...]"
                        : debugResult;
                    runOnUiThread(() -> {
                        String debugMsg = "🔍 DEBUG decrypt_file (" + filePath + "):\n\n" + debugPreview;
                        messages.add(new ChatMessage(debugMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                
                if (fileContent != null) {
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", fileContent)
                    ));
                    // Armazenar conteúdo limpo (sem menções a arquivos criptografados)
                    allDecryptedContent.append(fileContent).append("\n\n");
                } else {
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", "Erro: Não foi possível acessar o conteúdo solicitado.")
                    ));
                }
                toolResults.put(toolResult);
            } else if ("list_path_and_files".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String path = params.optString("path", null);
                String searchPattern = params.optString("search_pattern", null);
                
                // Executar helper utilitário
                String resultText = list_path_and_files.execute(sc_id, path, searchPattern);
                
                // DEBUG: Exibir resultado no chat (se habilitado)
                if (showDebug) {
                    String debugPreview = resultText.length() > 500 
                        ? resultText.substring(0, 500) + "\n\n[... conteúdo truncado para debug ...]"
                        : resultText;
                    runOnUiThread(() -> {
                        String debugMsg = "🔍 DEBUG list_path_and_files:\n\n" + debugPreview;
                        messages.add(new ChatMessage(debugMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                toolResult.put("content", new JSONArray().put(new JSONObject()
                    .put("type", "text")
                    .put("text", resultText)
                ));
                
                toolResults.put(toolResult);
            } else if ("get_item_by_id".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String filePath = params.getString("file_path");
                String itemId = params.getString("item_id");
                
                // Ler arquivo primeiro
                String fileContent = SketchwareFileDecryptor.decryptFile(filePath);
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                
                String resultText = null;
                if (fileContent != null) {
                    // Buscar item específico no JSON
                    String itemContent = extractItemById(fileContent, itemId);
                    if (itemContent != null) {
                        resultText = itemContent;
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", itemContent)
                        ));
                        allDecryptedContent.append(itemContent).append("\n\n");
                    } else {
                        resultText = "Item com ID '" + itemId + "' não encontrado no arquivo " + filePath + ".\n\n" +
                                "Conteúdo completo do arquivo:\n" + fileContent;
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    }
                } else {
                    resultText = "Erro: Não foi possível acessar o arquivo " + filePath;
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", resultText)
                    ));
                }
                
                // DEBUG: Exibir resultado no chat (se habilitado)
                if (showDebug && resultText != null) {
                    String debugPreview = resultText.length() > 500 
                        ? resultText.substring(0, 500) + "\n\n[... conteúdo truncado para debug ...]"
                        : resultText;
                    String finalDebugPreview = debugPreview;
                    runOnUiThread(() -> {
                        String debugMsg = "🔍 DEBUG get_item_by_id (" + filePath + ", " + itemId + "):\n\n" + finalDebugPreview;
                        messages.add(new ChatMessage(debugMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
                
                toolResults.put(toolResult);
            } else if ("morph_edit_code".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String filePath = params.getString("file_path");
                String instructions = params.getString("instructions");
                String codeEdit = params.optString("code_edit", "");
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                
                // Usar SketchwareFileEditor para editar o arquivo
                SketchwareFileEditor.EditResult result = SketchwareFileEditor.editFile(filePath, instructions, codeEdit);
                
                String resultText = null;
                if (result.success) {
                    resultText = "File " + filePath + " modified successfully using Morph.";
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", resultText)
                    ));
                    
                    // Adicionar mensagens no chat mostrando antes (vermelho) e depois (verde)
                    runOnUiThread(() -> {
                        // Mensagem BEFORE em vermelho
                        String beforeMessage = "**BEFORE:**\n\n```json\n" + 
                            result.initialContent + "\n```";
                        messages.add(new ChatMessage(beforeMessage, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        
                        // Mensagem AFTER em verde
                        String afterMessage = "**AFTER:**\n\n```json\n" + 
                            result.editedContent + "\n```";
                        messages.add(new ChatMessage(afterMessage, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        
                        scrollToBottom();
                        saveChatHistory();
                    });
                } else {
                    resultText = result.errorMessage != null ? result.errorMessage : "Erro ao modificar arquivo: " + filePath;
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", resultText)
                    ));
                }
                
                // DEBUG: Exibir resultado no chat (se habilitado)
                if (showDebug && resultText != null) {
                    String finalResultText = resultText;
                    runOnUiThread(() -> {
                        String debugMsg = "🔍 DEBUG morph_edit_code (" + filePath + "):\n\n" + finalResultText;
                        messages.add(new ChatMessage(debugMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
                
                toolResults.put(toolResult);
            } else if ("codebase_search".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String query = params.getString("query");
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                
                String resultText = null;
                try {
                    List<SemanticFileSearcher.SearchResult> results = SemanticFileSearcher.searchRelevantFiles(query, sc_id);
                    
                    if (results.isEmpty()) {
                        resultText = "No relevant files found for query: " + query;
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    } else {
                        StringBuilder resultTextBuilder = new StringBuilder();
                        resultTextBuilder.append("**Semantic search results:**\n\n");
                        
                        for (SemanticFileSearcher.SearchResult result : results) {
                            resultTextBuilder.append("**").append(result.filePath).append("**");
                            resultTextBuilder.append(" (").append(result.relevance).append(", score: ").append(String.format("%.2f", result.score)).append(")\n");
                            if (result.snippet != null && !result.snippet.isEmpty()) {
                                resultTextBuilder.append("```\n").append(result.snippet).append("\n```\n\n");
                            }
                        }
                        
                        resultText = resultTextBuilder.toString();
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    }
                } catch (Exception e) {
                    resultText = "Error in semantic search: " + e.getMessage();
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", resultText)
                    ));
                }
                
                // DEBUG: Exibir resultado no chat (se habilitado)
                if (showDebug && resultText != null) {
                    String debugPreview = resultText.length() > 500 
                        ? resultText.substring(0, 500) + "\n\n[... conteúdo truncado para debug ...]"
                        : resultText;
                    String finalDebugPreview = debugPreview;
                    runOnUiThread(() -> {
                        String debugMsg = "🔍 DEBUG codebase_search (" + query + "):\n\n" + finalDebugPreview;
                        messages.add(new ChatMessage(debugMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
                
                toolResults.put(toolResult);
            } else if ("grep".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String pattern = params.getString("pattern");
                String filePath = params.optString("file_path", null);
                boolean useRegex = params.optBoolean("use_regex", false);
                String filePattern = params.optString("file_pattern", null);
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                
                String resultText = null;
                try {
                    List<CodeGrep.GrepResult> results;
                    
                    if (filePath != null && !filePath.trim().isEmpty()) {
                        // Buscar em arquivo específico
                        results = CodeGrep.searchInFile(filePath, pattern, useRegex);
                    } else {
                        // Buscar em todo o projeto
                        results = CodeGrep.searchInProject(sc_id, pattern, useRegex, filePattern);
                    }
                    
                    if (results.isEmpty()) {
                        resultText = "No matches found for pattern: " + pattern;
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    } else {
                        StringBuilder resultTextBuilder = new StringBuilder();
                        resultTextBuilder.append("**Grep results (").append(results.size()).append(" matches):**\n\n");
                        
                        String currentFile = null;
                        for (CodeGrep.GrepResult result : results) {
                            if (!result.filePath.equals(currentFile)) {
                                if (currentFile != null) {
                                    resultTextBuilder.append("\n");
                                }
                                currentFile = result.filePath;
                                resultTextBuilder.append("**").append(result.filePath).append(":**\n");
                            }
                            resultTextBuilder.append("  ").append(result.lineNumber).append(": ").append(result.lineContent).append("\n");
                        }
                        
                        resultText = resultTextBuilder.toString();
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    }
                } catch (Exception e) {
                    resultText = "Error in grep search: " + e.getMessage();
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", resultText)
                    ));
                }
                
                // DEBUG: Exibir resultado no chat (se habilitado)
                if (showDebug && resultText != null) {
                    String debugPreview = resultText.length() > 500 
                        ? resultText.substring(0, 500) + "\n\n[... conteúdo truncado para debug ...]"
                        : resultText;
                    String finalDebugPreview = debugPreview;
                    String finalPattern = pattern;
                    runOnUiThread(() -> {
                        String debugMsg = "🔍 DEBUG grep (" + finalPattern + "):\n\n" + finalDebugPreview;
                        messages.add(new ChatMessage(debugMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
                
                toolResults.put(toolResult);
            } else if ("glob_file_search".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String pattern = params.getString("pattern");
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                
                String resultText = null;
                try {
                    List<ProjectFileDiscovery.FileInfo> files = GlobFileSearch.search(sc_id, pattern);
                    
                    if (files.isEmpty()) {
                        resultText = "No files found matching pattern: " + pattern;
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    } else {
                        StringBuilder fileList = new StringBuilder();
                        fileList.append("**Files found (").append(files.size()).append(" matches, sorted by modification date):**\n\n");
                        
                        for (ProjectFileDiscovery.FileInfo file : files) {
                            fileList.append("📄 ").append(file.path);
                            fileList.append(" (").append(file.size).append(" bytes");
                            if (file.isEncrypted) {
                                fileList.append(", encrypted");
                            }
                            fileList.append(")\n");
                        }
                        
                        resultText = fileList.toString();
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    }
                } catch (Exception e) {
                    resultText = "Error in glob file search: " + e.getMessage();
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", resultText)
                    ));
                }
                
                // DEBUG: Exibir resultado no chat (se habilitado)
                if (showDebug && resultText != null) {
                    String debugPreview = resultText.length() > 500 
                        ? resultText.substring(0, 500) + "\n\n[... conteúdo truncado para debug ...]"
                        : resultText;
                    String finalDebugPreview = debugPreview;
                    String finalPattern = pattern;
                    runOnUiThread(() -> {
                        String debugMsg = "🔍 DEBUG glob_file_search (" + finalPattern + "):\n\n" + finalDebugPreview;
                        messages.add(new ChatMessage(debugMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
                
                toolResults.put(toolResult);
            } else if ("write".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String filePath = params.getString("file_path");
                String content = params.getString("content");
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                
                String resultText = null;
                try {
                    // Verificar se o arquivo precisa ser criptografado
                    boolean needsEncryption = !SketchwareFileEncryptor.fileExists(filePath) || 
                                             !filePath.contains("."); // Arquivos sem extensão são criptografados
                    
                    boolean saved;
                    if (needsEncryption) {
                        saved = SketchwareFileEncryptor.encryptAndSaveFile(filePath, content);
                    } else {
                        // Salvar como texto plano
                        saved = savePlainTextFile(filePath, content);
                    }
                    
                    if (saved) {
                        resultText = "File written successfully: " + filePath;
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    } else {
                        resultText = "Error: Failed to write file: " + filePath;
                        toolResult.put("content", new JSONArray().put(new JSONObject()
                            .put("type", "text")
                            .put("text", resultText)
                        ));
                    }
                } catch (Exception e) {
                    resultText = "Error writing file: " + e.getMessage();
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", resultText)
                    ));
                }
                
                // DEBUG: Exibir resultado no chat (se habilitado)
                if (showDebug && resultText != null) {
                    String finalResultText = resultText;
                    runOnUiThread(() -> {
                        String debugMsg = "🔍 DEBUG write (" + filePath + "):\n\n" + finalResultText;
                        messages.add(new ChatMessage(debugMsg, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                    });
                }
                
                toolResults.put(toolResult);
            } else if ("delete_file".equals(functionName)) {
                // Extrair parâmetros
                JSONObject params = new JSONObject(arguments);
                String filePath = params.getString("file_path");
                
                // Em vez de deletar diretamente, pedir confirmação explícita para o usuário no chat,
                // semelhante ao comportamento de aprovação de ações perigosas no Void.
                runOnUiThread(() -> {
                    String confirmMessage = "⚠️ Solicitação de exclusão de arquivo recebida do assistente.\n\n" +
                            "Arquivo alvo: `" + filePath + "`\n\n" +
                            "Se você realmente deseja excluir este arquivo (ação permanente), envie uma mensagem deixando claro algo como:\n" +
                            "\"Confirmo que desejo excluir o arquivo " + filePath + "\".\n\n" +
                            "Somente após essa confirmação explícita a ferramenta de exclusão deve ser chamada novamente.";
                    messages.add(new ChatMessage(confirmMessage, false, System.currentTimeMillis()));
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    scrollToBottom();
                    saveChatHistory();
                });
                
                // Retornar um tool_result informando ao modelo que a confirmação do usuário é necessária.
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", "User confirmation required before deleting file: " + filePath)));
                
                toolResults.put(toolResult);
            }
        }
        
        // Enviar tool_results de volta para o Groq (protocolo MCP)
        if (toolResults.length() > 0) {
            // Construir mensagem com tool_results (otimizada para evitar exceder limite de tokens)
            StringBuilder followUpMessage = new StringBuilder();
            followUpMessage.append("Use o conteúdo a seguir para entender melhor o estado atual do projeto Sketchware e produzir uma resposta final de alta qualidade para o usuário.\n");
            followUpMessage.append("Se perceber que ainda falta contexto, você PODE chamar novas ferramentas novamente.\n");
            followUpMessage.append("Não é necessário repetir blocos enormes de código; foque em explicar o que está acontecendo e sugerir próximos passos claros.\n");
            followUpMessage.append("Não mencione nomes internos de ferramentas, apenas descreva as ações realizadas.\n\n");
            
            StringBuilder allContent = new StringBuilder();
            int totalLength = 0;
            final int MAX_CONTENT_LENGTH = 3000; // Limitar tamanho para evitar exceder tokens
            
            for (int i = 0; i < toolResults.length(); i++) {
                JSONObject result = toolResults.getJSONObject(i);
                JSONArray content = result.getJSONArray("content");
                if (content.length() > 0) {
                    JSONObject textContent = content.getJSONObject(0);
                    String contentText = textContent.getString("text");
                    
                    // Limitar tamanho do conteúdo para evitar exceder limite de tokens
                    if (totalLength + contentText.length() > MAX_CONTENT_LENGTH) {
                        int remaining = MAX_CONTENT_LENGTH - totalLength;
                        if (remaining > 100) {
                            contentText = contentText.substring(0, remaining) + "\n\n[... conteúdo truncado para evitar exceder limite de tokens ...]";
                        } else {
                            break; // Não há espaço suficiente
                        }
                    }
                    
                    allContent.append(contentText).append("\n\n");
                    totalLength += contentText.length();
                }
            }
            
            followUpMessage.append(allContent.toString());
            
            followUpMessage.append("Com base nisso, explique para o usuário, em linguagem simples, o que você descobriu e quais ações recomenda seguir.");
            
            // Enviar tool_results para o Groq processar
            executorService.execute(() -> {
                try {
                    // Coletar últimas 3 mensagens do chat (excluindo debug) para contexto
                    JSONArray chatHistory = getLastChatMessages(3);
                    
                    JSONArray tools = createMCPTools();
                    String finalResponse = groqClient.sendMessage(followUpMessage.toString(), tools, chatHistory);
                    
                    // Processar resposta final através do processMCPResponse
                    // Isso garante que tool_calls não sejam exibidos, apenas respostas finais
                    processMCPResponse(finalResponse);
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        isProcessing = false;
                        showProgress(false);
                        setInputEnabled(true);
                        String errorMsg = e.getMessage();
                        
                        // Verificar se é erro de rate limit
                        if (errorMsg != null && (errorMsg.contains("429") || errorMsg.contains("rate limit") || errorMsg.contains("Rate limit") || errorMsg.contains("tokens per minute"))) {
                            // Tentar extrair informações do rate limit
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
                            errorMsg = "Erro ao processar resultados: " + (errorMsg != null ? errorMsg : "Erro desconhecido");
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

