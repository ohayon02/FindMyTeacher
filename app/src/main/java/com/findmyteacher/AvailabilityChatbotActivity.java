package com.findmyteacher;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class AvailabilityChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messageList;
    private EditText etMessage;

    private String currentUserId;
    private DocumentReference teacherRef;
    private static final String CHATBOT_ID = "chatbot_id";

    private final List<JSONObject> chatHistory = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Ensure user is not null before getting UID
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Handle error: user not logged in
            finish();
            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        teacherRef = FirebaseFirestore.getInstance().collection("users").document(currentUserId);

        setupToolbar();
        setupRecyclerView();
        setupSendButton();

        addSystemInstruction();
        startConversation();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AI Assistant");
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

    private void setupSendButton() {
        etMessage = findViewById(R.id.etMessage);
        FloatingActionButton btnSend = findViewById(R.id.btnSendMessage);
        btnSend.setOnClickListener(v -> handleUserMessage());
    }

    private void addSystemInstruction() {
        try {
            JSONObject systemInstruction = new JSONObject();
            systemInstruction.put("role", "system");
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", "You are a helpful assistant for a teacher. Your goal is to answer any questions they have, and also help them manage their teaching schedule. Use the tools provided to you to update their availability. When you confirm a change, be explicit (e.g., \"I have updated your availability for Wednesday\"). Always respond in the user's language."));
            systemInstruction.put("parts", parts);
            chatHistory.add(systemInstruction);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating system instruction", e);
        }
    }

    private void startConversation() {
        addMessage(CHATBOT_ID, "Hello! I'm your personal AI assistant. Ask me anything, or let me know how to update your schedule.", 0);
    }

    private void handleUserMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        try {
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", text));
            userMessage.put("parts", parts);
            chatHistory.add(userMessage);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating user message", e);
        }

        addMessage(currentUserId, text, 0);
        etMessage.setText("");

        addMessage(CHATBOT_ID, "...", 300); // Thinking indicator

        generateResponse();
    }

    private void generateResponse() {
        String apiKey = BuildConfig.API_KEY;
        if (apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY")) {
            handler.post(() -> {
                removeLastMessage();
                addMessage(CHATBOT_ID, "AI features are disabled. Please set your API key in local.properties.", 0);
            });
            return;
        }

        executor.execute(() -> {
            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject requestBody = buildRequestBody();

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }
                    JSONObject responseJson = new JSONObject(response.toString());
                    processResponse(responseJson);
                } else {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                        String responseLine;
                        StringBuilder errorResponse = new StringBuilder();
                        while ((responseLine = br.readLine()) != null) {
                            errorResponse.append(responseLine.trim());
                        }
                        Log.e(TAG, "API Error Response: " + errorResponse.toString());
                    }
                    throw new Exception("API call failed with response code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during API call", e);
                handler.post(() -> {
                    removeLastMessage();
                    addMessage(CHATBOT_ID, "Sorry, an error occurred while contacting the AI.", 0);
                });
            }
        });
    }

    private JSONObject buildRequestBody() throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", new JSONArray(chatHistory));

        JSONObject functionDeclaration = new JSONObject();
        functionDeclaration.put("name", "updateAvailability");
        functionDeclaration.put("description", "Updates the teacher's availability for a specific date.");

        JSONObject parameters = new JSONObject();
        parameters.put("type", "OBJECT");

        JSONObject dateProp = new JSONObject().put("type", "STRING").put("description", "The date in yyyy-MM-dd format. Today is " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        JSONObject availableProp = new JSONObject().put("type", "BOOLEAN");

        parameters.put("properties", new JSONObject().put("date", dateProp).put("isAvailable", availableProp));
        parameters.put("required", new JSONArray().put("date").put("isAvailable"));
        functionDeclaration.put("parameters", parameters);

        JSONArray functionDeclarations = new JSONArray().put(functionDeclaration);
        JSONObject tool = new JSONObject().put("functionDeclarations", functionDeclarations);
        requestBody.put("tools", new JSONArray().put(tool));
        return requestBody;
    }

    private void processResponse(JSONObject responseJson) {
        handler.post(this::removeLastMessage);

        try {
            JSONObject candidate = responseJson.optJSONArray("candidates").optJSONObject(0);
            if (candidate == null) {
                throw new JSONException("No candidates found in response");
            }

            JSONObject content = candidate.optJSONObject("content");
            if (content == null) {
                // Potentially a safety block or finish reason
                String finishReason = candidate.optString("finishReason");
                Log.w(TAG, "No content in candidate, finish reason: " + finishReason);
                addMessage(CHATBOT_ID, "I am unable to provide a response for that.", 0);
                return;
            }

            chatHistory.add(content);

            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.length() == 0) {
                throw new JSONException("No parts in content");
            }

            JSONObject firstPart = parts.getJSONObject(0);
            JSONObject functionCall = firstPart.optJSONObject("functionCall");

            if (functionCall != null) {
                handleFunctionCall(functionCall);
            } else {
                String textResponse = firstPart.optString("text", "I'm not sure how to respond to that.");
                handler.post(() -> addMessage(CHATBOT_ID, textResponse, 0));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error processing JSON response", e);
            handler.post(() -> addMessage(CHATBOT_ID, "Sorry, I received an invalid response from the AI.", 0));
        }
    }

    private void handleFunctionCall(JSONObject functionCall) throws JSONException {
        String functionName = functionCall.optString("name");
        if (functionName.equals("updateAvailability")) {
            JSONObject args = functionCall.optJSONObject("args");
            if (args == null) {
                throw new JSONException("Function call arguments are missing");
            }

            String date = args.optString("date", null);
            if (date == null || !args.has("isAvailable")) {
                handler.post(() -> addMessage(CHATBOT_ID, "I'm sorry, I couldn't figure out the details for the schedule update. Please be more specific.", 0));
                return;
            }
            boolean isAvailable = args.getBoolean("isAvailable");

            updateAvailability(date, isAvailable, success -> {
                try {
                    JSONObject funcResponse = new JSONObject();
                    funcResponse.put("role", "function");

                    JSONObject responsePart = new JSONObject();
                    responsePart.put("name", "updateAvailability");
                    responsePart.put("response", new JSONObject().put("success", success).put("date", date));

                    funcResponse.put("parts", new JSONArray().put(new JSONObject().put("functionResponse", responsePart)));

                    chatHistory.add(funcResponse);
                    generateResponse(); // Send result back to AI
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating function response", e);
                }
            });
        }
    }

    private void updateAvailability(String dateString, boolean isAvailable, java.util.function.Consumer<Boolean> callback) {
        if (isAvailable) {
            LessonSlot newSlot = new LessonSlot(dateString, "16:00", "20:00", false);
            teacherRef.update("availableSlots", FieldValue.arrayUnion(newSlot.toMap()))
                    .addOnSuccessListener(aVoid -> callback.accept(true))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding availability", e);
                        callback.accept(false);
                    });
        } else {
            teacherRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    callback.accept(false);
                    return;
                }

                Object slotsObject = documentSnapshot.get("availableSlots");
                if (!(slotsObject instanceof List)) {
                    callback.accept(true); // Nothing to remove, so it's a success in a way.
                    return;
                }
                List<Map<String, Object>> slots = (List<Map<String, Object>>) slotsObject;

                slots.removeIf(slot -> dateString.equals(slot.get("date")));

                teacherRef.set(Map.of("availableSlots", slots), SetOptions.merge())
                        .addOnSuccessListener(aVoid -> callback.accept(true))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error removing availability", e);
                            callback.accept(false);
                        });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting document for removal", e);
                callback.accept(false);
            });
        }
    }

    private void addMessage(String senderId, String text, long delay) {
        handler.postDelayed(() -> {
            Message message = new Message(senderId, text, System.currentTimeMillis());
            messageList.add(message);
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
        }, delay);
    }

    private void removeLastMessage() {
        if (messageList.isEmpty()) return;
        int lastIndex = messageList.size() - 1;
        messageList.remove(lastIndex);
        adapter.notifyItemRemoved(lastIndex);
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