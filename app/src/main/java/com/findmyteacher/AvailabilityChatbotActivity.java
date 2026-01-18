package com.findmyteacher;

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AvailabilityChatbotActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messageList;
    private EditText etMessage;
    private FloatingActionButton btnSend;

    private FirebaseFirestore db;
    private String currentUserId;
    private static final String CHATBOT_ID = "chatbot_id";

    private int conversationDayOffset = 0;
    private boolean conversationActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupToolbar();
        setupRecyclerView();

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSendMessage);
        btnSend.setOnClickListener(v -> handleUserMessage());

        startConversation();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Availability AI Assistant");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        rvMessages = findViewById(R.id.rvMessages);
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);
    }

    private void startConversation() {
        addMessage(CHATBOT_ID, "Hello! I'm your AI assistant. Let's update your availability for the next 7 days.", 500);
        askAboutNextDay();
    }

    private void askAboutNextDay() {
        if (!conversationActive) return;
        if (conversationDayOffset >= 7) {
            conversationActive = false;
            addMessage(CHATBOT_ID, "Great, we've updated the whole week! You can write to me anytime to make further changes.", 500);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, conversationDayOffset);
        String dayName = new SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH).format(calendar.getTime());

        String question = String.format("Are you available to teach on %s (4 PM - 8 PM)?", dayName);
        addMessage(CHATBOT_ID, question, 1000);
    }

    private void handleUserMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        addMessage(currentUserId, text, 0);
        etMessage.setText("");

        processUserResponse(text);
    }

    private void processUserResponse(String text) {
        String lowerCaseText = text.toLowerCase();

        // --- Enhanced AI Response --- //
        if (handleSmallTalk(lowerCaseText)) {
            return; // Small talk was handled, no need to process further
        }

        Date targetDate = parseDateFromText(lowerCaseText);
        boolean isYes = lowerCaseText.contains("yes") || lowerCaseText.contains("available") || lowerCaseText.contains("free") || lowerCaseText.contains("כן") || lowerCaseText.contains("פנוי");
        boolean isNo = lowerCaseText.contains("no") || lowerCaseText.contains("not") || lowerCaseText.contains("unavailable") || lowerCaseText.contains("לא");

        if (targetDate == null && conversationActive) { 
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, conversationDayOffset);
            targetDate = c.getTime();
        }
        
        if (isYes || isNo) {
            if (targetDate != null) {
                updateAvailability(targetDate, isYes);
            } else {
                 addMessage(CHATBOT_ID, "Which day are you referring to?", 500);
            }
        } else {
            addMessage(CHATBOT_ID, "I'm sorry, I'm focused on setting your schedule right now. Please tell me if you are available for a specific day.", 500);
        }
    }
    
    private boolean handleSmallTalk(String text) {
        if (text.contains("hello") || text.contains("שלום")) {
            addMessage(CHATBOT_ID, "Hello there! Ready to set up your week?", 500);
            return true;
        }
        if (text.contains("how are you") || text.contains("מה שלומך")) {
            addMessage(CHATBOT_ID, "I'm a computer program, so I'm always running at optimal performance! How can I help you with your schedule today?", 500);
            return true;
        }
        if (text.contains("thank you") || text.contains("thanks") || text.contains("תודה")){
            addMessage(CHATBOT_ID, "You're welcome! Anything else I can help with?", 500);
            return true;
        }
        return false;
    }

    private Date parseDateFromText(String text) {
        Calendar c = Calendar.getInstance();
        if (text.contains("tomorrow") || text.contains("מחר")) {
            c.add(Calendar.DAY_OF_YEAR, 1);
            return c.getTime();
        }
        if (text.contains("today") || text.contains("היום")) {
            return c.getTime();
        }

        String[] daysEn = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
        String[] daysHe = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};

        for (int i = 0; i < daysEn.length; i++) {
            if (text.contains(daysEn[i]) || text.contains(daysHe[i])) {
                int today = c.get(Calendar.DAY_OF_WEEK) - 1; // sunday=0
                int diff = (i - today + 7) % 7;
                if(diff == 0 && !text.contains("today")) diff = 7;
                c.add(Calendar.DAY_OF_YEAR, diff);
                return c.getTime();
            }
        }
        return null;
    }

    private void updateAvailability(Date date, boolean isAvailable) {
        String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
        DocumentReference teacherRef = db.collection("users").document(currentUserId);

        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<Map<String, Object>> slots = (List<Map<String, Object>>) documentSnapshot.get("availableSlots");
                if (slots == null) slots = new ArrayList<>();
                
                final String finalDateString = dateString;
                slots.removeIf(slot -> finalDateString.equals(slot.get("date")));

                String dayName = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date);
                if (isAvailable) {
                    LessonSlot newSlot = new LessonSlot(dateString, "16:00", "20:00", false);
                    slots.add(newSlot.toMap());
                    addMessage(CHATBOT_ID, "Great, I've set you as available for " + dayName + ".", 500);
                } else {
                    addMessage(CHATBOT_ID, "Got it, you are not available on " + dayName + ".", 500);
                }

                teacherRef.update("availableSlots", slots).addOnSuccessListener(aVoid -> {
                    if (conversationActive && dateString.equals(getTargetDateString(conversationDayOffset))) {
                        conversationDayOffset++;
                        askAboutNextDay();
                    }
                });
            }
        });
    }

    private String getTargetDateString(int offset) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offset);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime());
    }

    private void addMessage(String senderId, String text, long delay) {
        new Handler().postDelayed(() -> {
            Message message = new Message(senderId, text, System.currentTimeMillis());
            messageList.add(message);
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
        }, delay);
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
