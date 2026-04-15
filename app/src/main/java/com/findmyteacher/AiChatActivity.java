package com.findmyteacher;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiChatActivity extends AppCompatActivity {

    private static final String TAG = "AiChatActivity";
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private int questionCount = 0;
    private StringBuilder history = new StringBuilder();
    private String studentName = "תלמיד";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        rvChat = findViewById(R.id.rvChatAi);
        etMessage = findViewById(R.id.etAiMessage);
        btnSend = findViewById(R.id.btnAiSend);
        btnBack = findViewById(R.id.btnAiBack);

        adapter = new ChatAdapter(messages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadStudentName();
        btnSend.setOnClickListener(v -> sendMessage());

        startAiConversation();
    }

    private void loadStudentName() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            studentName = doc.getString("fullName");
                        }
                    });
        }
    }

    private void startAiConversation() {
        addMessage("AI", "מתחבר לבינה המלאכותית...");
        GeminiAIHelper.continueAiChat(this, studentName, "", "", 0, new GeminiAIHelper.AICallback() {
            @Override
            public void onResponse(String response) {
                removeLastMessage(); // תיקון: הסרה בטוחה עם עדכון ה-Adapter
                addMessage("AI", response);
                history.append("AI: ").append(response).append("\n");
            }

            @Override
            public void onError(Exception e) {
                removeLastMessage();
                addMessage("AI", "שגיאה בחיבור ל-AI: " + e.getMessage());
                Log.e(TAG, "AI Error", e);
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessage("Me", text);
        etMessage.setText("");
        history.append("תלמיד: ").append(text).append("\n");
        questionCount++;

        addMessage("AI", "מעבד נתונים...");

        GeminiAIHelper.continueAiChat(this, studentName, history.toString(), text, questionCount, new GeminiAIHelper.AICallback() {
            @Override
            public void onResponse(String response) {
                removeLastMessage();
                
                if (response.contains("SUMMARY:")) {
                    String finalSummary = response.substring(response.indexOf("SUMMARY:") + 8).trim();
                    saveSummaryToFirestore(finalSummary);
                } else {
                    addMessage("AI", response);
                    history.append("AI: ").append(response).append("\n");
                }
            }

            @Override
            public void onError(Exception e) {
                removeLastMessage();
                addMessage("AI", "שגיאה ב-AI: " + e.getMessage());
            }
        });
    }

    private void addMessage(String sender, String text) {
        messages.add(new ChatMessage(sender, text));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
    }

    private void removeLastMessage() {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            messages.remove(lastIndex);
            adapter.notifyItemRemoved(lastIndex); // תיקון: עדכון ה-Adapter על ההסרה
        }
    }

    private void saveSummaryToFirestore(String summary) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("lastFeedback", summary);
        data.put("feedbackTimestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("users").document(uid).update(data)
                .addOnSuccessListener(aVoid -> {
                    addMessage("AI", "הניתוח הסתיים בהצלחה! הדיווח נשלח למורים שלך. אתה יכול לסגור את הצ'אט.");
                });
    }

    private static class ChatMessage {
        String sender, text;
        ChatMessage(String s, String t) { sender = s; text = t; }
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private List<ChatMessage> list;
        ChatAdapter(List<ChatMessage> l) { list = l; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_2, p, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            ChatMessage m = list.get(p);
            h.t1.setText(m.sender.equals("AI") ? "🤖 AI" : "👤 אני");
            h.t2.setText(m.text);
            h.t1.setTextColor(m.sender.equals("AI") ? 0xFF2196F3 : 0xFF4CAF50);
        }

        @Override public int getItemCount() { return list.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2;
            ViewHolder(View v) { super(v); t1 = v.findViewById(android.R.id.text1); t2 = v.findViewById(android.R.id.text2); }
        }
    }
}
