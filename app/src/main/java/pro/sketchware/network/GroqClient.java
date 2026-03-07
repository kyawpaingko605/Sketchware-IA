package pro.sketchware.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pro.sketchware.R;

public class GroqClient {
    private static final String TAG = "GroqClient";
    private static final String BASE_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static GroqClient instance;

    private final Context context;
    private final OkHttpClient client;
    private String apiKey;

    private GroqClient() {
        this.context = pro.sketchware.SketchApplication.getContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static synchronized GroqClient getInstance() {
        if (instance == null) {
            instance = new GroqClient();
        }
        return instance;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private String getDeviceLanguage() {
        try {
            Resources resources = context.getResources();
            Configuration configuration = resources.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return configuration.getLocales().get(0).getLanguage();
            } else {
                //noinspection deprecation
                return configuration.locale.getLanguage();
            }
        } catch (Throwable t) {
            return Locale.getDefault().getLanguage();
        }
    }

    private void navigateToSettings() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            try {
                Toast.makeText(context, context.getString(R.string.groq_api_key_not_configured_message), Toast.LENGTH_SHORT).show();
                android.content.Intent intent = new android.content.Intent(context, pro.sketchware.activities.settings.IaSettingsActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open AI settings", e);
            }
        });
    }
    
    private String buildSystemPrompt(String deviceLanguage) {
        StringBuilder prompt = new StringBuilder();

        // Idioma e papel principal
        prompt.append("Você é um assistente de código especializado em projetos Sketchware e Android.\n");
        prompt.append("Responda sempre no idioma do usuário (language: ").append(deviceLanguage).append(").\n\n");

        prompt.append("Seu comportamento deve ser semelhante ao do agente Void em um editor de código:\n");
        prompt.append("- Entender bem o contexto do projeto antes de sugerir mudanças.\n");
        prompt.append("- Ajudar o usuário a desenvolver, depurar e melhorar o código, passo a passo.\n");
        prompt.append("- Completar as tarefas até o fim, em vez de parar cedo demais.\n\n");

        prompt.append("## Regras gerais sobre ferramentas internas (MCP)\n\n");
        prompt.append("1. Não mencionar nomes internos de ferramentas ao usuário (fale apenas sobre a ação: ler arquivo, listar arquivos, buscar, editar, etc.).\n");
        prompt.append("2. Chamar ferramentas apenas quando isso ajudar claramente a atingir o objetivo do usuário.\n");
        prompt.append("3. Use no máximo uma ferramenta por vez, aguardando o resultado antes de decidir o próximo passo.\n");
        prompt.append("4. Antes de modificar ou deletar arquivos, sempre leia e entenda o conteúdo atual.\n");
        prompt.append("5. Nunca remova funções ou funcionalidades existentes do projeto sem o usuário pedir isso de forma explícita.\n");
        prompt.append("6. Para ações destrutivas (como deletar arquivo ou sobrescrever conteúdo importante), peça confirmação explícita do usuário antes de prosseguir.\n\n");

        prompt.append("## Estratégia recomendada de uso de ferramentas\n\n");
        prompt.append("- Primeiro, descubra e liste arquivos relevantes (ferramentas de listagem e busca de arquivos).\n");
        prompt.append("- Depois, leia os arquivos necessários (ferramentas de leitura/descrifração).\n");
        prompt.append("- Em seguida, faça buscas específicas (ferramentas de busca semântica ou por padrão de texto).\n");
        prompt.append("- Só então proponha modificações, e apenas quando o usuário indicar que deseja alterar, corrigir ou atualizar algo.\n\n");

        prompt.append("### Arquivos criptografados e leitura com decrypt_file\n\n");
        prompt.append("- Muitos arquivos importantes do Sketchware (lógicas, layouts, configurações, bibliotecas) são armazenados em formato binário/criptografado dentro da pasta .sketchware.\n");
        prompt.append("- Sempre que precisar LER o conteúdo real de um arquivo do projeto (por exemplo, para entender a lógica, um layout ou uma configuração), priorize o uso de uma ferramenta de leitura que faça descriptografia automática.\n");
        prompt.append("- Em especial, quando o usuário mencionar caminhos, IDs de componentes, eventos, blocos de lógica ou arquivos dentro de .sketchware, considere chamar a ferramenta de leitura/descriptografia ANTES de tentar responder apenas de memória.\n");
        prompt.append("- Depois de usar essa ferramenta de leitura, use o conteúdo retornado como base principal da sua análise e das explicações para o usuário.\n\n");

        prompt.append("### Busca e leitura\n\n");
        prompt.append("- Ferramentas de busca semântica devem ser usadas para localizar trechos de código e arquivos relevantes a uma dúvida ou erro.\n");
        prompt.append("- Ferramentas de busca por padrão de texto devem ser usadas para encontrar símbolos, nomes de classes, funções ou IDs específicos.\n");
        prompt.append("- Ferramentas de leitura de arquivo (especialmente as que descriptografam automaticamente) devem ser usadas para inspecionar o conteúdo real antes de sugerir qualquer edição.\n\n");

        prompt.append("### Edição de código\n\n");
        prompt.append("- Antes de editar, leia sempre o arquivo ou a seção relevante.\n");
        prompt.append("- Se a edição foi feita por você e gerar erros, tente corrigi-los.\n");
        prompt.append("- Dê preferência a editar arquivos existentes; crie novos arquivos apenas quando isso fizer sentido claro para o usuário.\n");
        prompt.append("- Quando o usuário pedir para MODIFICAR, ALTERAR, EDITAR, MUDAR, CORRIGIR ou ATUALIZAR, você pode usar ferramentas internas de edição.\n");
        prompt.append("- Nunca oculte do usuário que uma mudança significante será feita; explique em linguagem natural o que foi ou será alterado.\n\n");

        prompt.append("### Gerenciamento de arquivos\n\n");
        prompt.append("- Para escrita de arquivos, certifique-se de que o conteúdo final atenda exatamente ao que o usuário pediu.\n");
        prompt.append("- Para exclusão de arquivos, confirme com o usuário qual arquivo será removido e as consequências dessa ação.\n");
        prompt.append("- Se houver dúvida sobre o caminho ou tipo de arquivo, use ferramentas de listagem e leitura primeiro.\n\n");

        prompt.append("### Organização do trabalho\n\n");
        prompt.append("- Para tarefas com vários passos (por exemplo, analisar erro, localizar código, propor correção e aplicar mudança), organize o raciocínio em etapas claras.\n");
        prompt.append("- Em tarefas puramente explicativas ou simples, privilegie resposta direta sem usar ferramentas.\n");
        prompt.append("- Sempre explique ao usuário, em linguagem natural, o que você concluiu a partir dos resultados de qualquer ferramenta.\n\n");

        prompt.append("### Citação de código\n\n");
        prompt.append("- Ao mostrar código ao usuário, use blocos de código bem formatados apenas quando isso ajudar na compreensão.\n");
        prompt.append("- Evite repetir grandes blocos de código sem necessidade; foque nas partes importantes para a dúvida atual.\n");

        prompt.append("\n---\n\n");
        prompt.append("Siga todas as regras acima, mantenha o foco em ajudar no projeto Sketchware do usuário, ");
        prompt.append("e produza respostas claras, objetivas e úteis, sempre respeitando as confirmações e decisões do usuário.\n");

        return prompt.toString();
    }

    public String sendMessage(String message) throws IOException {
        return sendMessage(message, null, null);
    }

    public String sendMessage(String message, JSONArray tools) throws IOException {
        return sendMessage(message, tools, null);
    }

    public String sendMessage(String message, JSONArray tools, JSONArray chatHistory) throws IOException {
        String deviceLanguage = getDeviceLanguage();
        String systemPrompt = buildSystemPrompt(deviceLanguage);

        JSONArray messages = new JSONArray();
        try {
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            
            // Adicionar histórico do chat se fornecido
            if (chatHistory != null && chatHistory.length() > 0) {
                for (int i = 0; i < chatHistory.length(); i++) {
                    JSONObject historyMsg = chatHistory.getJSONObject(i);
                    messages.put(new JSONObject().put("role", historyMsg.getString("role")).put("content", historyMsg.optString("content", "")));
                }
            }
            
            // Adicionar mensagem atual do usuário
            messages.put(new JSONObject().put("role", "user").put("content", message));
            
            return sendRawMessages(messages, tools);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting messages", e);
            throw new IOException("Error formatting messages", e);
        }
    }

    /**
     * Envia um array de mensagens JSON completo para a API, suportando o fluxo nativo de Tool Calling
     */
    public String sendRawMessages(JSONArray messages, JSONArray tools) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences("ia_settings", Context.MODE_PRIVATE);
        boolean groqEnabled = prefs.getBoolean("groq_enabled", false);
        if (!groqEnabled) {
            navigateToSettings();
            throw new IOException(context.getString(R.string.groq_api_key_not_configured_title));
        }

        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = prefs.getString("groq_api_key", "");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            navigateToSettings();
            throw new IOException(context.getString(R.string.groq_api_key_not_configured_title));
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "llama-3.1-8b-instant");
            jsonBody.put("messages", messages);
            jsonBody.put("temperature", 0.7);
            jsonBody.put("max_tokens", 4000);
            
            // Adicionar tools se fornecido
            if (tools != null && tools.length() > 0) {
                jsonBody.put("tools", tools);
                jsonBody.put("tool_choice", "auto");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating JSON for request", e);
            throw new IOException("Error preparing request", e);
        }

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .build();

        // Configurações de retry baseadas nas melhores práticas do Void
        int maxRetries = 5; // Aumentado de 3 para 5 (similar ao Void que usa até 10)
        long baseBackoffMs = 1000L; // Base de 1 segundo
        java.util.Random random = new java.util.Random();

        IOException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    int code = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (attempt < maxRetries && (code == 429 || (code >= 500 && code < 600))) {
                        long waitTime = baseBackoffMs;
                        
                        // Para rate limit (429), tentar extrair o tempo sugerido pelo Groq
                        if (code == 429) {
                            try {
                                JSONObject errorJson = new JSONObject(responseBody);
                                if (errorJson.has("error")) {
                                    JSONObject error = errorJson.getJSONObject("error");
                                    String errorMessage = error.optString("message", "");
                                    // Extrair tempo sugerido: "Please try again in 1.44s"
                                    if (errorMessage.contains("Please try again in")) {
                                        String timeStr = errorMessage.substring(errorMessage.indexOf("Please try again in") + 20);
                                        timeStr = timeStr.substring(0, timeStr.indexOf("s")).trim();
                                        double seconds = Double.parseDouble(timeStr);
                                        // Converter para milissegundos e adicionar margem de segurança
                                        waitTime = (long) (seconds * 1000) + 500; // +500ms de margem
                                        Log.i(TAG, "Extracted wait time from Groq: " + waitTime + "ms");
                                    }
                                }
                            } catch (Exception e) {
                                // Se não conseguir extrair, usar exponential backoff com jitter
                                Log.w(TAG, "Could not extract wait time from error, using exponential backoff with jitter", e);
                            }
                            
                            // Se não extraiu do erro, usar exponential backoff com jitter (baseado no Void)
                            if (waitTime == baseBackoffMs) {
                                // Fórmula similar ao Void: Math.floor((Math.random() * 200) + (50 * Math.pow(1.5, run)))
                                // Adaptado para Java: (random * 200) + (50 * Math.pow(1.5, attempt))
                                double exponentialDelay = 50 * Math.pow(1.5, attempt + 1);
                                long jitter = random.nextInt(200); // 0-199ms de jitter
                                waitTime = (long) (exponentialDelay + jitter);
                                // Limitar máximo a 30 segundos
                                waitTime = Math.min(waitTime, 30000L);
                            } else {
                                // Se extraiu do erro, garantir mínimo de 2 segundos
                                waitTime = Math.max(waitTime, 2000L);
                            }
                        } else {
                            // Para outros erros (500-599), usar exponential backoff simples
                            waitTime = (long) (baseBackoffMs * Math.pow(2, attempt));
                            // Adicionar jitter aleatório (0-200ms)
                            waitTime += random.nextInt(200);
                            // Limitar máximo a 30 segundos
                            waitTime = Math.min(waitTime, 30000L);
                        }
                        
                        Log.w(TAG, "Rate limit or server error (" + code + "), waiting " + waitTime + "ms before retry " + (attempt + 1) + "/" + maxRetries);
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ignored) { }
                        continue;
                    }
                    throw new IOException("Request error: " + code + " - " + responseBody);
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JSONObject jsonResponse = new JSONObject(responseBody);
                JSONArray choices = jsonResponse.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    Log.e(TAG, "Response missing choices: " + responseBody);
                    throw new IOException("Invalid response from Groq API");
                }
                JSONObject firstChoice = choices.getJSONObject(0);
                
                if (firstChoice.has("message")) {
                    JSONObject messageObj = firstChoice.getJSONObject("message");
                    
                    // DETECÇÃO DE TOOL_CALLS: Verificar se há tool_calls na resposta da API do Groq
                    // Esta é a ÚNICA detecção de tool_calls - feita diretamente na resposta da API
                    if (messageObj.has("tool_calls") && !messageObj.isNull("tool_calls")) {
                        JSONArray toolCalls = messageObj.getJSONArray("tool_calls");
                        if (toolCalls.length() > 0) {
                            // Retornar JSON estruturado com tool_calls (será processado pelo ChatActivity)
                            JSONObject mcpResponse = new JSONObject();
                            mcpResponse.put("type", "tool_calls");
                            mcpResponse.put("tool_calls", toolCalls);
                            // Incluir content se houver
                            if (messageObj.has("content") && !messageObj.isNull("content")) {
                                mcpResponse.put("content", messageObj.optString("content", ""));
                            }
                            return mcpResponse.toString();
                        }
                    }
                    
                    // Resposta normal com content
                    String content = messageObj.optString("content", null);
                    if (content != null && !content.isEmpty()) {
                        return content;
                    }
                }
                
                // Fallback para formato antigo
                String content = firstChoice.optString("text", null);
                if (content == null) {
                    Log.e(TAG, "Could not extract content: " + responseBody);
                    throw new IOException("AI response content is empty");
                }
                return content;
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    // Verificar se é erro de rate limit na mensagem
                    String errorMsg = e.getMessage();
                    boolean isRateLimit = errorMsg != null && (errorMsg.contains("429") || errorMsg.contains("rate limit"));
                    
                    // Usar exponential backoff com jitter (baseado no Void)
                    // Fórmula: (random * 200) + (50 * Math.pow(1.5, attempt))
                    double exponentialDelay = 50 * Math.pow(1.5, attempt + 1);
                    long jitter = random.nextInt(200); // 0-199ms de jitter
                    long waitTime = (long) (exponentialDelay + jitter);
                    
                    // Para rate limit, garantir mínimo maior
                    if (isRateLimit) {
                        waitTime = Math.max(waitTime, 2000L);
                    }
                    
                    // Limitar máximo a 30 segundos
                    waitTime = Math.min(waitTime, 30000L);
                    
                    Log.w(TAG, "IO error" + (isRateLimit ? " (rate limit)" : "") + ", waiting " + waitTime + "ms before retry " + (attempt + 1) + "/" + maxRetries);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ignored) { }
                    continue;
                }
                Log.e(TAG, "Error processing response", e);
                throw e;
            } catch (Exception e) {
                Log.e(TAG, "Error processing response", e);
                throw new IOException("Error processing API response", e);
            }
        }

        if (lastException != null) {
            throw lastException;
        } else {
            throw new IOException("Unknown error contacting Groq API");
        }
    }
}

