package pro.sketchware.activities.chat;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {
    private static final String PREFS_NAME = "chat_history";
    private final Context context;
    private final Gson gson;
    
    public ChatHistoryManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }
    
    /**
     * Salva o histórico de mensagens do chat
     * @param scId ID do projeto
     * @param messages Lista de mensagens
     */
    public void saveHistory(String scId, List<ChatMessage> messages) {
        if (scId == null || messages == null) return;
        
        String key = getHistoryKey(scId);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Converter mensagens para lista serializável
        List<ChatMessageData> messageDataList = new ArrayList<>();
        for (ChatMessage msg : messages) {
            messageDataList.add(new ChatMessageData(msg.getMessage(), msg.isUser(), msg.getTimestamp()));
        }
        
        // Salvar como JSON
        String json = gson.toJson(messageDataList);
        prefs.edit().putString(key, json).apply();
    }
    
    /**
     * Carrega o histórico de mensagens do chat
     * @param scId ID do projeto
     * @return Lista de mensagens
     */
    public List<ChatMessage> loadHistory(String scId) {
        if (scId == null) return new ArrayList<>();
        
        String key = getHistoryKey(scId);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(key, null);
        
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            Type listType = new TypeToken<ArrayList<ChatMessageData>>(){}.getType();
            List<ChatMessageData> messageDataList = gson.fromJson(json, listType);
            
            if (messageDataList == null) {
                return new ArrayList<>();
            }
            
            // Converter de volta para ChatMessage
            List<ChatMessage> messages = new ArrayList<>();
            for (ChatMessageData data : messageDataList) {
                messages.add(new ChatMessage(data.message, data.isUser, data.timestamp));
            }
            
            return messages;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Limpa o histórico de um chat específico
     * @param scId ID do projeto
     */
    public void clearHistory(String scId) {
        if (scId == null) return;
        
        String key = getHistoryKey(scId);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(key).apply();
    }
    
    /**
     * Gera a chave única para o histórico
     */
    private String getHistoryKey(String scId) {
        return "chat_" + scId;
    }
    
    /**
     * Classe auxiliar para serialização
     */
    private static class ChatMessageData {
        public String message;
        public boolean isUser;
        public long timestamp;
        
        public ChatMessageData(String message, boolean isUser, long timestamp) {
            this.message = message;
            this.isUser = isUser;
            this.timestamp = timestamp;
        }
    }
}

