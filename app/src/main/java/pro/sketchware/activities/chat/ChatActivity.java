package pro.sketchware.activities.chat;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a.a.a.lC;
import a.a.a.yB;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;

public class ChatActivity extends AppCompatActivity {
    private String sc_id;
    private RecyclerView recyclerViewMessages;
    private TextInputEditText editTextMessage;
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;

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

        setupToolbar();
        setupViews();
        loadProjectInfo();
        loadMockMessages();
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
        TextInputLayout inputLayout = findViewById(R.id.input_layout_message);

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

    private void loadMockMessages() {
        // Adicionar algumas mensagens mockadas
        messages.add(new ChatMessage("Olá! Como posso ajudar você com este projeto?", false, System.currentTimeMillis() - 3600000));
        messages.add(new ChatMessage("Gostaria de adicionar um botão na tela principal", true, System.currentTimeMillis() - 1800000));
        messages.add(new ChatMessage("Claro! Vou ajudar você a adicionar um botão. Qual tipo de botão você precisa?", false, System.currentTimeMillis() - 900000));
        messages.add(new ChatMessage("Um botão simples que abre uma nova tela", true, System.currentTimeMillis() - 300000));
        messages.add(new ChatMessage("Perfeito! Vou criar um botão MaterialButton que navega para uma nova Activity.", false, System.currentTimeMillis() - 60000));
        
        messageAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void sendMessage(String message) {
        messages.add(new ChatMessage(message, true, System.currentTimeMillis()));
        messageAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();

        // Simular resposta automática após 1 segundo
        recyclerViewMessages.postDelayed(() -> {
            messages.add(new ChatMessage("Mensagem recebida: " + message, false, System.currentTimeMillis()));
            messageAdapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
        }, 1000);
    }

    private void scrollToBottom() {
        if (messages.size() > 0) {
            recyclerViewMessages.scrollToPosition(messages.size() - 1);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

