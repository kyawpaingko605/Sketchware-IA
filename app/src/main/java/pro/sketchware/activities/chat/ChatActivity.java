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

import org.json.JSONArray;
import org.json.JSONObject;

import io.noties.markwon.Markwon;
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
    private Markwon markwon;
    private String projectContext; // Contexto do projeto carregado uma vez
    private long lastMessageTime = 0; // Timestamp do último envio de mensagem
    private static final long MIN_MESSAGE_INTERVAL_MS = 2000; // Intervalo mínimo de 2 segundos entre mensagens
    private boolean isProcessing = false; // Flag para indicar se está processando uma mensagem
    private ChatHistoryManager historyManager;
    private static final String WELCOME_MESSAGE_PREFIX = "Hello! How can I help you with";

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
        
        // Inicializar Markwon para formatação markdown com configuração completa
        markwon = Markwon.builder(this)
                .build();
        
        setupToolbar();
        setupViews();
        loadProjectInfo();
        
        // Carregar histórico do chat
        loadChatHistory();
        
        // Carregar contexto do projeto em background
        loadProjectContext();
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
        // Configurar Markwon no adapter para formatação markdown
        messageAdapter.setMarkwon(markwon);
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
                // Construir contexto da conversa para o Groq
                HashMap<String, Object> projectInfo = lC.b(sc_id);
                String projectName = projectInfo != null ? yB.c(projectInfo, "my_ws_name") : "projeto";
                
                // Construir mensagem com contexto do projeto e histórico recente
                StringBuilder fullMessage = new StringBuilder();
                fullMessage.append("Você é um assistente especializado no projeto Android '").append(projectName).append("' (ID: ").append(sc_id).append(").\n\n");
                
                // Adicionar contexto do projeto se já foi carregado
                if (projectContext != null && !projectContext.isEmpty()) {
                    fullMessage.append("**Informações do Projeto:**\n");
                    fullMessage.append(projectContext).append("\n\n");
                }
                
                fullMessage.append("**FERRAMENTAS DISPONÍVEIS:**\n\n");
                fullMessage.append("1. 'decrypt_file': Use APENAS para LER arquivos quando o usuário pedir informações ou fizer perguntas.\n");
                fullMessage.append("   Arquivos: mysc/list/").append(sc_id).append("/project, data/").append(sc_id).append("/logic, data/").append(sc_id).append("/view, data/").append(sc_id).append("/file, data/").append(sc_id).append("/library, data/").append(sc_id).append("/resource\n\n");
                
                fullMessage.append("2. 'morph_edit_code': Use SEMPRE que o usuário pedir MODIFICAR, ALTERAR, EDITAR, MUDAR, CORRIGIR ou ATUALIZAR.\n");
                fullMessage.append("   Modifica arquivos: project, logic, view, file, library, resource\n\n");
                
                fullMessage.append("**REGRAS CRÍTICAS:**\n");
                fullMessage.append("- MODIFICAÇÃO → Execute 'morph_edit_code' DIRETO. NÃO mostre conteúdo, NÃO explique, apenas EXECUTE e confirme.\n");
                fullMessage.append("- INFORMAÇÃO → Use 'decrypt_file' e explique APENAS o que está no arquivo fornecido.\n");
                fullMessage.append("- NÃO invente informações que não estão nos arquivos fornecidos pelo MCP.\n");
                fullMessage.append("- Baseie-se EXCLUSIVAMENTE no conteúdo retornado pelas ferramentas MCP.\n");
                fullMessage.append("- Seja DIRETO e objetivo. Não dê informações que o usuário não pediu.\n\n");
                
                // Adicionar últimas mensagens para contexto (últimas 6 mensagens para manter contexto sem ser muito longo)
                int startIndex = Math.max(0, messages.size() - 6);
                if (startIndex < messages.size()) {
                    fullMessage.append("Histórico recente da conversa:\n");
                    for (int i = startIndex; i < messages.size(); i++) {
                        ChatMessage msg = messages.get(i);
                        fullMessage.append(msg.isUser() ? "Usuário: " : "Assistente: ");
                        fullMessage.append(msg.getMessage()).append("\n");
                    }
                }
                
                // Criar definição da tool no formato MCP (JSON Schema)
                JSONArray tools = createMCPTools();
                
                // Enviar para Groq com tools MCP
                String response = groqClient.sendMessage(fullMessage.toString(), tools);
                
                // Processar resposta MCP (pode ser tool_calls ou texto normal)
                processMCPResponse(response);

            } catch (IOException e) {
                runOnUiThread(() -> {
                    isProcessing = false;
                    showProgress(false);
                    setInputEnabled(true);
                    
                    String errorMessage = e.getMessage();
                    // Verificar se é erro de rate limit
                    if (errorMessage != null && (errorMessage.contains("429") || errorMessage.contains("rate limit") || errorMessage.contains("Rate limit"))) {
                        errorMessage = "Rate limit exceeded. Please wait a few seconds before sending another message.";
                        Toast.makeText(this, "Rate limit exceeded. Please wait before sending another message.", Toast.LENGTH_LONG).show();
                    } else {
                        errorMessage = "Error sending message: " + errorMessage;
                        Toast.makeText(this, "Error communicating with Groq", Toast.LENGTH_SHORT).show();
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
     * Cria as definições de tools no formato MCP (JSON Schema)
     */
    private JSONArray createMCPTools() {
        try {
            JSONArray tools = new JSONArray();
            
            // Tool decrypt_file seguindo protocolo MCP
            JSONObject tool = new JSONObject();
            tool.put("type", "function");
            
            JSONObject function = new JSONObject();
            function.put("name", "decrypt_file");
            function.put("description", "Acessa um arquivo do projeto Sketchware e opcionalmente busca um item específico por ID. " +
                    "Arquivos disponíveis: mysc/list/{sc_id}/project (informações do projeto), " +
                    "data/{sc_id}/logic (lógica/eventos/funções Java - contém componentes @Activity.java_components e blocos de lógica @Activity.java_onCreate_initializeLogic com campos: componentId, param1-3, type, color, id, nextBlock, opCode, parameters, spec, subStack1-2, typeName), data/{sc_id}/view (layouts XML), " +
                    "data/{sc_id}/library (bibliotecas/dependências), data/{sc_id}/file (Activities e Custom Views: fileName, fileType, keyboardSetting, options, orientation, theme). " +
                    "Os arquivos retornados são JSON. Use item_id para buscar um item específico (ex: id_11).");
            
            // Parâmetros em formato JSON Schema (MCP)
            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");
            JSONObject properties = new JSONObject();
            
            properties.put("file_path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho relativo do arquivo do projeto. " +
                        "Disponíveis: mysc/list/{sc_id}/project (nome, pacote, versão, cores), " +
                        "data/{sc_id}/logic (eventos e blocos e funções Java - contém componentes @Activity.java_components: componentId, param1-3, type; blocos de lógica @Activity.java_onCreate_initializeLogic: color, id, nextBlock, opCode, parameters, spec, subStack1-2, type, typeName - pode ser modificado livremente), " +
                        "data/{sc_id}/view (layouts XML das telas), " +
                        "data/{sc_id}/library (bibliotecas e dependências), " +
                        "data/{sc_id}/file (Activities e Custom Views: fileName, fileType, keyboardSetting, options, orientation, theme). " +
                        "data/{sc_id}/resource recursos do projeto. " +
                        "Exemplo: data/601/logic ou mysc/list/601/project")
            );
            
            properties.put("item_id", new JSONObject()
                .put("type", "string")
                .put("description", "ID opcional do item específico a buscar no JSON. " +
                        "Se fornecido, retorna apenas o item com esse ID ao invés do arquivo completo. " +
                        "Exemplo: id_11, id_5, etc. Deixe vazio ou omita para retornar o arquivo completo.")
            );
            
            parameters.put("properties", properties);
            JSONArray required = new JSONArray();
            required.put("file_path");
            parameters.put("required", required);
            
            function.put("parameters", parameters);
            tool.put("function", function);
            
            tools.put(tool);
            
            // Tool morph_edit_code para modificar arquivos JSON do projeto
            JSONObject morphTool = new JSONObject();
            morphTool.put("type", "function");
            
            JSONObject morphFunction = new JSONObject();
            morphFunction.put("name", "morph_edit_code");
            morphFunction.put("description", "FERRAMENTA PARA MODIFICAR ARQUIVOS DO PROJETO. " +
                    "Use SEMPRE que o usuário pedir para MODIFICAR, ALTERAR, EDITAR, MUDAR, CORRIGIR, ATUALIZAR ou fazer QUALQUER ESCRITA nos arquivos. " +
                    "Modifica arquivos JSON do projeto (logic, view, project, library, resource, file) usando Morph. " +
                    "O arquivo será descriptografado, modificado pelo Morph e criptografado de volta. " +
                    "Todos os arquivos listados são modificáveis, incluindo arquivos em subpastas. " +
                    "EXEMPLOS de quando usar: 'Mude o nome', 'Altere a versão', 'Modifique o pacote', 'Edite a lógica', 'Corrija o layout', etc.");
            
            JSONObject morphParameters = new JSONObject();
            morphParameters.put("type", "object");
            JSONObject morphProperties = new JSONObject();
            
            morphProperties.put("file_path", new JSONObject()
                .put("type", "string")
                .put("description", "Caminho relativo do arquivo do projeto. " +
                        "Disponíveis: " +
                        "mysc/list/{sc_id}/project (contém TODAS as informações do projeto: nome do app, nome do projeto, pacote, versão, código da versão, data de criação, versão do Sketchware, cores primária/accent/controle, configurações de ícone customizado/adaptativo - TODAS essas informações podem ser modificadas livremente), " +
                        "data/{sc_id}/logic (eventos e blocos e funções Java - contém componentes @Activity.java_components: componentId, param1-3, type; blocos de lógica @Activity.java_onCreate_initializeLogic: color, id, nextBlock, opCode, parameters, spec, subStack1-2, type, typeName - pode ser modificado livremente), " +
                        "data/{sc_id}/view (layouts XML das telas), " +
                        "data/{sc_id}/library (bibliotecas e dependências), " +
                        "data/{sc_id}/file (Activities e Custom Views - contém informações sobre cada Activity/Custom View: fileName, fileType, keyboardSetting, options, orientation, theme - pode ser modificado livremente), " +
                        "data/{sc_id}/resource (recursos do projeto). " +
                        "IMPORTANTE: Todos esses arquivos são modificáveis, incluindo arquivos em subpastas. " +
                        "O arquivo project contém: my_ws_name, my_app_name, my_sc_pkg_name, sc_ver_name, sc_ver_code, my_sc_reg_dt, sketchware_ver, color_primary, color_accent, color_control_normal, color_control_highlight, custom_icon, isIconAdaptive, etc. " +
                        "Exemplo: data/601/logic, mysc/list/601/project, data/601/resource/layout/activity_main.xml, etc.")
            );
            
            morphProperties.put("instructions", new JSONObject()
                .put("type", "string")
                .put("description", "Instruções claras e específicas sobre o que modificar no arquivo JSON. " +
                        "Descreva EXATAMENTE o que deve ser alterado, adicionado ou removido. " +
                        "Exemplo: 'Altere o campo my_ws_name de NewProject para MeuProjeto' ou 'Mude o valor de sc_ver_code de 1 para 2'. " +
                        "Seja específico sobre qual campo modificar e qual o novo valor.")
            );
            
            morphProperties.put("code_edit", new JSONObject()
                .put("type", "string")
                .put("description", "JSON editado ou trecho a ser modificado (opcional). " +
                        "Se vazio, o Morph criará a modificação baseado apenas nas instruções.")
            );
            
            morphParameters.put("properties", morphProperties);
            JSONArray morphRequired = new JSONArray();
            morphRequired.put("file_path");
            morphRequired.put("instructions");
            morphParameters.put("required", morphRequired);
            
            morphFunction.put("parameters", morphParameters);
            morphTool.put("function", morphFunction);
            
            tools.put(morphTool);
            return tools;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }
    
    /**
     * Processa a resposta do Groq seguindo protocolo MCP
     */
    private void processMCPResponse(String response) throws Exception {
        // Verificar se é uma resposta com tool_calls (formato MCP)
        if (response.trim().startsWith("{")) {
            try {
                JSONObject mcpResponse = new JSONObject(response);
                if ("tool_calls".equals(mcpResponse.optString("type"))) {
                    // Processar tool_calls no formato MCP
                    JSONArray toolCalls = mcpResponse.getJSONArray("tool_calls");
                    String content = mcpResponse.optString("content", "");
                    
                    // Mostrar conteúdo se houver
                    if (!content.isEmpty()) {
                        runOnUiThread(() -> {
                            messages.add(new ChatMessage(content, false, System.currentTimeMillis()));
                            messageAdapter.notifyItemInserted(messages.size() - 1);
                            scrollToBottom();
                            saveChatHistory();
                        });
                    }
                    
                    // Processar tool_calls e enviar tool_results
                    processMCPToolCalls(toolCalls);
                    return;
                }
            } catch (Exception e) {
                // Não é JSON MCP, tratar como resposta normal
            }
        }
        
        // Resposta normal de texto
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
                String itemId = params.optString("item_id", null);
                
                // Descriptografar arquivo
                String decryptedContent = SketchwareFileDecryptor.decryptFile(filePath);
                
                // Criar tool_result no formato MCP
                JSONObject toolResult = new JSONObject();
                toolResult.put("tool_call_id", toolCallId);
                
                if (decryptedContent != null) {
                    String finalContent = decryptedContent;
                    
                    // Se item_id foi fornecido, buscar item específico no JSON
                    if (itemId != null && !itemId.trim().isEmpty()) {
                        String itemContent = extractItemById(decryptedContent, itemId);
                        if (itemContent != null) {
                            finalContent = itemContent;
                        } else {
                            finalContent = "Item com ID '" + itemId + "' não encontrado.\n\n" +
                                    "Conteúdo completo:\n" + decryptedContent;
                        }
                    }
                    
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", finalContent)
                    ));
                    // Armazenar conteúdo limpo (sem menções a arquivos criptografados)
                    allDecryptedContent.append(finalContent).append("\n\n");
                } else {
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", "Erro: Não foi possível acessar o conteúdo solicitado.")
                    ));
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
                
                if (result.success) {
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
                    
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", "File " + filePath + " modified successfully using Morph.")
                    ));
                } else {
                    toolResult.put("content", new JSONArray().put(new JSONObject()
                        .put("type", "text")
                        .put("text", result.errorMessage != null ? result.errorMessage : "Erro ao modificar arquivo: " + filePath)
                    ));
                }
                
                toolResults.put(toolResult);
            }
        }
        
        // Enviar tool_results de volta para o Groq (protocolo MCP)
        if (toolResults.length() > 0) {
            // Construir mensagem com tool_results
            StringBuilder followUpMessage = new StringBuilder();
            followUpMessage.append("Aqui está o conteúdo solicitado:\n\n");
            
            boolean hasMixedContent = false;
            StringBuilder allContent = new StringBuilder();
            
            for (int i = 0; i < toolResults.length(); i++) {
                JSONObject result = toolResults.getJSONObject(i);
                JSONArray content = result.getJSONArray("content");
                if (content.length() > 0) {
                    JSONObject textContent = content.getJSONObject(0);
                    String contentText = textContent.getString("text");
                    allContent.append(contentText).append("\n\n");
                    
                    // Detectar se há conteúdo misto (identificadores @ seguidos de JSON)
                    if (contentText.contains("@") && (contentText.contains("{") || contentText.contains("["))) {
                        hasMixedContent = true;
                    }
                }
            }
            
            followUpMessage.append(allContent.toString());
            
            followUpMessage.append("Analise o conteúdo acima e forneça uma resposta clara ao usuário.\n\n");
            followUpMessage.append("**REGRAS CRÍTICAS - LEIA COM ATENÇÃO:**\n\n");
            followUpMessage.append("1. Baseie-se EXCLUSIVAMENTE no conteúdo fornecido acima. NÃO invente informações.\n");
            followUpMessage.append("2. NÃO adicione informações que não estão nos arquivos fornecidos pelo MCP.\n");
            followUpMessage.append("3. Se algo não estiver no conteúdo, diga que não foi encontrado ou não está disponível.\n");
            followUpMessage.append("4. NÃO faça suposições ou inferências sobre informações não presentes.\n");
            followUpMessage.append("5. Seja DIRETO e objetivo. Foque no que o usuário pediu.\n");
            followUpMessage.append("6. NÃO mencione arquivos criptografados, descriptografia ou ferramentas MCP.\n");
            followUpMessage.append("7. NÃO adicione informações genéricas ou exemplos que não estão no conteúdo fornecido.");
            
            // Enviar tool_results para o Groq processar
            executorService.execute(() -> {
                try {
                    JSONArray tools = createMCPTools();
                    String finalResponse = groqClient.sendMessage(followUpMessage.toString(), tools);
                    
                    runOnUiThread(() -> {
                        isProcessing = false;
                        showProgress(false);
                        setInputEnabled(true);
                        messages.add(new ChatMessage(finalResponse, false, System.currentTimeMillis()));
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        scrollToBottom();
                        saveChatHistory();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        isProcessing = false;
                        showProgress(false);
                        setInputEnabled(true);
                        String errorMsg = "Error processing results: " + e.getMessage();
                        // Verificar se é erro de rate limit
                        if (errorMsg.contains("429") || errorMsg.contains("rate limit") || errorMsg.contains("Rate limit")) {
                            errorMsg = "Rate limit exceeded. Please wait a few seconds before sending another message.";
                            Toast.makeText(this, "Rate limit exceeded. Please wait before sending another message.", Toast.LENGTH_LONG).show();
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
     * Carrega o contexto do projeto descriptografando o arquivo project
     */
    private void loadProjectContext() {
        executorService.execute(() -> {
            try {
                String projectPath = "mysc/list/" + sc_id + "/project";
                String decryptedContent = SketchwareFileDecryptor.decryptFile(projectPath);
                
                if (decryptedContent != null && !decryptedContent.trim().isEmpty()) {
                    // Parsear JSON e formatar informações principais
                    try {
                        Gson gson = new Gson();
                        Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
                        HashMap<String, Object> projectData = gson.fromJson(decryptedContent.trim(), type);
                        
                        // Formatar informações principais do projeto
                        StringBuilder context = new StringBuilder();
                        
                        if (projectData.containsKey("my_ws_name")) {
                            context.append("- **Nome do Projeto:** ").append(projectData.get("my_ws_name")).append("\n");
                        }
                        if (projectData.containsKey("my_app_name")) {
                            context.append("- **Nome do App:** ").append(projectData.get("my_app_name")).append("\n");
                        }
                        if (projectData.containsKey("my_sc_pkg_name")) {
                            context.append("- **Pacote:** ").append(projectData.get("my_sc_pkg_name")).append("\n");
                        }
                        if (projectData.containsKey("sc_ver_name")) {
                            context.append("- **Versão:** ").append(projectData.get("sc_ver_name"));
                            if (projectData.containsKey("sc_ver_code")) {
                                context.append(" (").append(projectData.get("sc_ver_code")).append(")");
                            }
                            context.append("\n");
                        }
                        if (projectData.containsKey("sketchware_ver")) {
                            context.append("- **Versão Sketchware:** ").append(projectData.get("sketchware_ver")).append("\n");
                        }
                        
                        // Cores do tema
                        if (projectData.containsKey("colorPrimary") || projectData.containsKey("colorAccent")) {
                            context.append("- **Cores do Tema:**\n");
                            if (projectData.containsKey("colorPrimary")) {
                                context.append("  - Primária: #").append(String.format("%06X", ((Number) projectData.get("colorPrimary")).intValue() & 0xFFFFFF)).append("\n");
                            }
                            if (projectData.containsKey("colorAccent")) {
                                context.append("  - Accent: #").append(String.format("%06X", ((Number) projectData.get("colorAccent")).intValue() & 0xFFFFFF)).append("\n");
                            }
                        }
                        
                        projectContext = context.toString();
                    } catch (Exception e) {
                        // Se não conseguir parsear, usar conteúdo bruto formatado
                        projectContext = "Conteúdo do projeto carregado com sucesso.";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                projectContext = null;
            }
        });
    }

    /**
     * Extrai um item específico por ID do JSON descriptografado
     * @param jsonContent Conteúdo JSON descriptografado
     * @param itemId ID do item a buscar (ex: "id_11", "11", etc)
     * @return JSON do item encontrado ou null se não encontrado
     */
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

