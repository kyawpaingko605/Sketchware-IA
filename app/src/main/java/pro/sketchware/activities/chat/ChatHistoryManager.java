package pro.sketchware.activities.chat;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {
    private final VoidStyleChatStorage voidStorage;
    
    public ChatHistoryManager(Context context) {
        this.voidStorage = new VoidStyleChatStorage(context);
    }
    
    /**
     * Salva uma única mensagem no histórico do chat (Incremental)
     * @param scId ID do projeto
     * @param message Mensagem a ser salva
     */
    public void saveMessage(String scId, ChatMessage message) {
        if (scId == null || scId.trim().isEmpty() || message == null) return;
        voidStorage.saveMessage(scId, ensureDefaultThread(scId), message);
    }

    public void saveMessage(String scId, String threadId, ChatMessage message) {
        if (scId == null || scId.trim().isEmpty() || message == null) return;
        voidStorage.saveMessage(scId, threadId, message);
    }

    /**
     * Salva o histórico completo de mensagens do chat (Fallback/Update)
     * @param scId ID do projeto
     * @param messages Lista de mensagens
     */
    public void saveHistory(String scId, List<ChatMessage> messages) {
        if (scId == null || messages == null) return;
        voidStorage.saveHistory(scId, ensureDefaultThread(scId), messages);
    }

    public void saveHistory(String scId, String threadId, List<ChatMessage> messages) {
        if (scId == null || messages == null) return;
        voidStorage.saveHistory(scId, threadId, messages);
    }
    
    /**
     * Carrega o histórico de mensagens do chat
     * @param scId ID do projeto
     * @return Lista de mensagens
     */
    public List<ChatMessage> loadHistory(String scId) {
        if (scId == null) return new ArrayList<>();
        return loadHistory(scId, ensureDefaultThread(scId));
    }

    public List<ChatMessage> loadHistory(String scId, String threadId) {
        if (scId == null) return new ArrayList<>();
        try {
            return voidStorage.loadHistory(scId, threadId);
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
        voidStorage.clearHistory(scId, ensureDefaultThread(scId));
    }

    public void clearHistory(String scId, String threadId) {
        if (scId == null) return;
        voidStorage.clearHistory(scId, threadId);
    }

    public void deleteProjectHistory(String scId) {
        if (scId == null) return;
        voidStorage.deleteProjectHistory(scId);
    }

    public String ensureDefaultThread(String scId) {
        return voidStorage.ensureDefaultThread(scId);
    }

    public String createThread(String scId) {
        return voidStorage.createThread(scId);
    }

    public List<ChatThread> getThreads(String scId) {
        return voidStorage.getThreads(scId);
    }

    public void updateThreadSummary(String scId, String threadId, String title, String summary, String activeModel) {
        voidStorage.updateThreadSummary(scId, threadId, title, summary, activeModel);
    }
}

