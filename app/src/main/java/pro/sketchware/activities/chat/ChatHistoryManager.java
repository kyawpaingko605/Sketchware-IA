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
     * Salva o histórico de mensagens do chat
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
        return dbHelper.getHistory(scId);
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

