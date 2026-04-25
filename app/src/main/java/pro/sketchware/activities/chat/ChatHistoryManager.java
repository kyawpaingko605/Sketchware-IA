package pro.sketchware.activities.chat;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {
    private final ChatDatabaseHelper dbHelper;
    
    public ChatHistoryManager(Context context) {
        this.dbHelper = new ChatDatabaseHelper(context);
    }
    
    /**
     * Salva uma única mensagem no histórico do chat (Incremental)
     * @param scId ID do projeto
     * @param message Mensagem a ser salva
     */
    public void saveMessage(String scId, ChatMessage message) {
        if (scId == null || scId.trim().isEmpty() || message == null) return;
        dbHelper.saveMessage(scId, message);
    }

    /**
     * Salva o histórico completo de mensagens do chat (Fallback/Update)
     * @param scId ID do projeto
     * @param messages Lista de mensagens
     */
    public void saveHistory(String scId, List<ChatMessage> messages) {
        if (scId == null || messages == null) return;
        dbHelper.saveMessages(scId, messages);
    }
    
    /**
     * Carrega o histórico de mensagens do chat
     * @param scId ID do projeto
     * @return Lista de mensagens
     */
    public List<ChatMessage> loadHistory(String scId) {
        if (scId == null) return new ArrayList<>();
        try {
            return dbHelper.getHistory(scId);
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
        dbHelper.clearHistory(scId);
    }
}

