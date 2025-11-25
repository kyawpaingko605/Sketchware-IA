package pro.sketchware.activities.chat;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.noties.markwon.Markwon;
import pro.sketchware.R;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {
    private final List<ChatMessage> messages;
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private Markwon markwon;

    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public void setMarkwon(Markwon markwon) {
        this.markwon = markwon;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == VIEW_TYPE_USER) {
            view = inflater.inflate(R.layout.item_message_user, parent, false);
        } else {
            view = inflater.inflate(R.layout.item_message_bot, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        String messageText = message.getMessage();
        
        // Aplicar formatação markdown
        if (holder.textMessage != null) {
            // Garantir que o TextView está configurado para renderizar markdown
            holder.textMessage.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            
            if (markwon != null) {
                // Usar Markwon configurado
                markwon.setMarkdown(holder.textMessage, messageText);
            } else {
                // Criar Markwon localmente se não foi configurado
                Markwon localMarkwon = Markwon.builder(holder.itemView.getContext())
                        .build();
                localMarkwon.setMarkdown(holder.textMessage, messageText);
            }
            
            // Aplicar cores para BEFORE (vermelho) e AFTER (verde) após o Markwon renderizar
            holder.textMessage.post(() -> {
                CharSequence text = holder.textMessage.getText();
                if (text instanceof Spannable) {
                    Spannable spannable = (Spannable) text;
                    String textStr = text.toString();
                    
                    // Procurar por "BEFORE:" e aplicar cor vermelha
                    int beforeIndex = textStr.indexOf("BEFORE:");
                    if (beforeIndex >= 0) {
                        int endIndex = Math.min(beforeIndex + 7, textStr.length());
                        spannable.setSpan(new ForegroundColorSpan(Color.RED), beforeIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    
                    // Procurar por "AFTER:" e aplicar cor verde
                    int afterIndex = textStr.indexOf("AFTER:");
                    if (afterIndex >= 0) {
                        int endIndex = Math.min(afterIndex + 6, textStr.length());
                        spannable.setSpan(new ForegroundColorSpan(Color.GREEN), afterIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            });
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.textTime.setText(sdf.format(new Date(message.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
        }
    }
}

