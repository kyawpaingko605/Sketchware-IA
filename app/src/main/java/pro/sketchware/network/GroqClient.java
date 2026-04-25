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
    // Base URLs removidas como constantes para serem definidas dinamicamente

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
        String now = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());

        prompt.append("You are May, a premium AI coding assistant for Sketchware projects, inspired by the Void editor.\n");
        prompt.append("Your goal is to help users develop, debug, and understand their Sketchware applications.\n");
        prompt.append("Current time: ").append(now).append("\n\n");
        
        prompt.append("## CORE RULES:\n");
        prompt.append("1. ALWAYS stay within the .sketchware ecosystem. Do not explore unrelated system directories.\n");
        prompt.append("2. PREFER using dedicated tools over generic shell commands when available.\n");
        prompt.append("3. After using a tool, CLEARLY summarize the result (e.g., 'I updated the logic in Activity X').\n");
        prompt.append("4. If permission is missing, explain it simply to the user.\n");
        prompt.append("5. ALWAYS use decrypt_sketchware_file/encrypt_sketchware_file for encrypted project files.\n");
        prompt.append("6. Be concise and professional. Do not hallucinate file contents.\n");
        prompt.append("7. If you find a bug, explain it and suggest a fix using the available tools.\n");

        return prompt.toString();
    }

    public String sendMessage(String message) throws IOException {
        return sendMessage(message, null, null, null);
    }

    public String sendMessage(String message, JSONArray tools) throws IOException {
        return sendMessage(message, tools, null, null);
    }

    public String sendMessage(String message, JSONArray tools, JSONArray chatHistory) throws IOException {
        return sendMessage(message, tools, chatHistory, null);
    }

    public String sendMessage(String message, JSONArray tools, JSONArray chatHistory, String dynamicSystemContext) throws IOException {
        String deviceLanguage = getDeviceLanguage();
        String systemPrompt = buildSystemPrompt(deviceLanguage);

        JSONArray messages = new JSONArray();
        try {
            // 1. Mensagem principal do sistema (Personalidade e Regras Gerais)
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            
            // 2. Contexto dinâmico do sistema (Arquivos relevantes, erros, etc - Estilo Void)
            if (dynamicSystemContext != null && !dynamicSystemContext.isEmpty()) {
                messages.put(new JSONObject().put("role", "system").put("content", "ADDITIONAL CONTEXT:\n" + dynamicSystemContext));
            }
            
            // 3. Adicionar histórico do chat se fornecido
            if (chatHistory != null && chatHistory.length() > 0) {
                for (int i = 0; i < chatHistory.length(); i++) {
                    JSONObject historyMsg = chatHistory.getJSONObject(i);
                    messages.put(new JSONObject()
                        .put("role", historyMsg.getString("role"))
                        .put("content", historyMsg.optString("content", ""))
                    );
                }
            }
            
            // 4. Adicionar mensagem atual do usuário
            messages.put(new JSONObject().put("role", "user").put("content", message));
            
        } catch (Exception e) {
            Log.e(TAG, "Error formatting messages", e);
            throw new IOException("Error formatting messages", e);
        }
        
        return sendRawMessages(messages, tools);
    }

    /**
     * Envia um array de mensagens JSON completo para a API, suportando o fluxo nativo de Tool Calling
     */
    public String sendRawMessages(JSONArray messages, JSONArray tools) throws IOException {
        SharedPreferences prefs = context.getSharedPreferences("ia_settings", Context.MODE_PRIVATE);
        
        // Obter provedor e modelo selecionados no chat
        String currentProvider = prefs.getString("current_ai_provider", "groq");
        String currentModel = prefs.getString("current_ai_model", "llama-3.1-8b-instant");
        
        String baseUrl = "";
        String providerApiKey = "";
        boolean isEnabled = false;
        
        // Configurar as variáveis baseadas no provedor
        switch (currentProvider) {
            case "openai":
                baseUrl = "https://api.openai.com/v1/chat/completions";
                providerApiKey = prefs.getString("openai_api_key", "");
                isEnabled = prefs.getBoolean("openai_enabled", false);
                break;
            case "gemini":
                // Google AI Studio OpenAI Compatibility Endpoint
                baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
                providerApiKey = prefs.getString("gemini_api_key", "");
                isEnabled = prefs.getBoolean("gemini_enabled", false);
                break;
            case "groq":
            default:
                baseUrl = "https://api.groq.com/openai/v1/chat/completions";
                providerApiKey = prefs.getString("groq_api_key", "");
                isEnabled = prefs.getBoolean("groq_enabled", false);
                break;
        }

        if (!isEnabled) {
            navigateToSettings();
            throw new IOException("O provedor selecionado (" + currentProvider + ") não está ativado nas configurações.");
        }

        if (providerApiKey == null || providerApiKey.isEmpty()) {
            navigateToSettings();
            throw new IOException("A chave da API para o provedor " + currentProvider + " não está configurada.");
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", currentModel);
            jsonBody.put("messages", messages);
            jsonBody.put("temperature", 0.7);
            
            // max_tokens is smaller for standard endpoints, Groq uses up to 8k
            if (currentProvider.equals("groq")) {
                jsonBody.put("max_tokens", 4000);
            }
            
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
                .url(baseUrl)
                .addHeader("Authorization", "Bearer " + providerApiKey)
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

