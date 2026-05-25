package com.findmyteacher;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiChatActivity extends AppCompatActivity {

    private static final String TAG = "AiChatActivity";
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private int questionCount = 0;
    private final StringBuilder history = new StringBuilder();
    private String studentName = "תלמיד";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUid;
    private CollectionReference chatMessagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        currentUid = FirebaseAuth.getInstance().getUid();

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

        if (currentUid != null) {
            // מיקום שמירת השיחה: תחת ישות ה-AI המרכזית בתוך קולקשיין users
            chatMessagesRef = db.collection("users")
                    .document("system_ai_bot")
                    .collection("chats")
                    .document(currentUid)
                    .collection("messages");
        }

        loadStudentName();
        btnSend.setOnClickListener(v -> sendMessage());

        // טעינת שיחה קיימת או התחלת שיחה חדשה במידה ואין היסטוריה
        loadChatHistory();
    }

    private void loadStudentName() {
        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getString("fullName") != null) {
                            studentName = doc.getString("fullName");
                        }
                    });
        }
    }

    private void loadChatHistory() {
        if (chatMessagesRef == null) return;

        chatMessagesRef.orderBy("timestamp", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        messages.clear();
                        history.setLength(0);

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String sender = doc.getString("sender");
                            String text = doc.getString("text");
                            Long count = doc.getLong("questionCount");

                            if (sender != null && text != null) {
                                messages.add(new ChatMessage(sender, text));
                                String displaySender = "AI".equals(sender) ? "AI" : "תלמיד";
                                history.append(displaySender).append(": ").append(text).append("\n");
                            }
                            if (count != null) {
                                questionCount = count.intValue();
                            }
                        }
                        adapter.notifyDataSetChanged();
                        rvChat.scrollToPosition(messages.size() - 1);
                    } else {
                        // אם אין היסטוריית הודעות ב-Firestore, מתחילים שיחה חדשה מהתחלה
                        startAiConversation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading chat history, starting new conversation", e);
                    startAiConversation();
                });
    }

    private void startAiConversation() {
        addMessageToUiAndFirestore("AI", "מתחבר לבינה המלאכותית...");

        GeminiAIHelper.continueAiChat(this, studentName, "", "", 0, new GeminiAIHelper.AICallback() {
            @Override
            public void onResponse(String response) {
                removeLastMessage();
                addMessageToUiAndFirestore("AI", response);
                history.append("AI: ").append(response).append("\n");
            }

            @Override
            public void onError(Exception e) {
                removeLastMessage();
                addMessageToUiAndFirestore("AI", "שגיאה בחיבור ל-AI: " + e.getMessage());
                Log.e(TAG, "AI Error", e);
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessageToUiAndFirestore("Me", text);
        etMessage.setText("");
        history.append("תלמיד: ").append(text).append("\n");
        questionCount++;

        addMessageToUiAndFirestore("AI", "מעבד נתונים...");

        GeminiAIHelper.continueAiChat(this, studentName, history.toString(), text, questionCount, new GeminiAIHelper.AICallback() {
            @Override
            public void onResponse(String response) {
                removeLastMessage();

                if (response.contains("SUMMARY:")) {
                    String finalSummary = response.substring(response.indexOf("SUMMARY:") + 8).trim();
                    saveSummaryToFirestore(finalSummary);
                } else {
                    addMessageToUiAndFirestore("AI", response);
                    history.append("AI: ").append(response).append("\n");
                }
            }

            @Override
            public void onError(Exception e) {
                removeLastMessage();
                addMessageToUiAndFirestore("AI", "שגיאה ב-AI: " + e.getMessage());
            }
        });
    }

    private void addMessageToUiAndFirestore(String sender, String text) {
        // 1. עדכון ה-UI במסך
        messages.add(new ChatMessage(sender, text));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);

        // 2. שמירה אוטומטית ב-Firestore (למעט הודעות מערכת זמניות)
        if (chatMessagesRef != null && !"מתחבר לבינה המלאכותית...".equals(text) && !"מעבד נתונים...".equals(text)) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("sender", sender);
            msgMap.put("text", text);
            msgMap.put("timestamp", System.currentTimeMillis());
            msgMap.put("questionCount", questionCount);

            chatMessagesRef.add(msgMap)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save message to Firestore", e));
        }
    }

    private void removeLastMessage() {
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            String text = messages.get(lastIndex).text;
            messages.remove(lastIndex);
            adapter.notifyItemRemoved(lastIndex);

            // אם זו הודעת מערכת זמנית היא לא נשמרה ב-Firestore מלכתחילה, אך אם זו הודעה רגילה שמסירים:
            if (chatMessagesRef != null && ("מתחבר לבינה המלאכותית...".equals(text) || "מעבד נתונים...".equals(text))) {
                return;
            }

            // מחיקת ההודעה האחרונה מה-Firestore במידה ונדרש סנכרון מלא בעקבות הסרת טעינה
            chatMessagesRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            queryDocumentSnapshots.getDocuments().get(0).getReference().delete();
                        }
                    });
        }
    }

    private void saveSummaryToFirestore(String summary) {
        if (currentUid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("lastFeedback", summary);
        data.put("feedbackTimestamp", System.currentTimeMillis());

        // שמירת הסטטוס והסיכום תחת כרטיס המשתמש של התלמיד לצפייה של המורים
        db.collection("users").document(currentUid).update(data)
                .addOnSuccessListener(aVoid -> addMessageToUiAndFirestore("AI", "הניתוח הסתיים בהצלחה! הדיווח נשלח למורים שלך. אתה יכול לסגור את הצ'אט."));
    }

    private static class ChatMessage {
        String sender, text;
        ChatMessage(String s, String t) { sender = s; text = t; }
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private final List<ChatMessage> list;
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