package com.findmyteacher;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private MessageAdapter adapter;
    private EditText etMessage;
    private RecyclerView rvMessages;
    private ProgressBar progressBar;

    private String currentUserId;
    private CollectionReference messagesCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        String otherUserId = getIntent().getStringExtra("studentId");
        if (otherUserId == null) {
            otherUserId = getIntent().getStringExtra("teacherId");
        }

        if (otherUserId == null || otherUserId.isEmpty()) {
            Toast.makeText(this, "Error: Chat partner not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String chatId = generateChatId(currentUserId, otherUserId);
        messagesCollection = db.collection("chats").document(chatId).collection("messages");

        String chatPartnerName = getIntent().getStringExtra("studentName");
        if (chatPartnerName == null) {
            chatPartnerName = getIntent().getStringExtra("teacherName");
        }

        setupToolbar(chatPartnerName);
        setupViews();
        listenForMessages();
    }

    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title != null ? title : "Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        ImageButton btnSend = findViewById(R.id.btnSendMessage);
        progressBar = findViewById(R.id.progressBar);

        adapter = new MessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private String generateChatId(String id1, String id2) {
        String[] ids = {id1, id2};
        Arrays.sort(ids);
        return ids[0] + "_" + ids[1];
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Message message = new Message(currentUserId, text, System.currentTimeMillis());

        messagesCollection.add(message)
                .addOnSuccessListener(documentReference -> etMessage.setText(""))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForMessages() {
        progressBar.setVisibility(View.VISIBLE);

        messagesCollection.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Log.w(TAG, "Listen for messages failed.", error);
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<Message> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Message message = doc.toObject(Message.class);
                            message.setId(doc.getId());
                            messages.add(message);
                        }
                        adapter.submitList(messages, () -> {
                            if (adapter.getItemCount() > 0) {
                                rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                            }
                        });
                    } else {
                        adapter.submitList(Collections.emptyList());
                    }
                });
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
