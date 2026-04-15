package com.findmyteacher;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherProfileEditActivity extends AppCompatActivity {

    private static final String TAG = "TeacherProfileEdit";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private DocumentReference teacherRef;

    private EditText etPrice, etLocation, etBio, etExtraInfo;
    private TextView tvPriceRecommendation;
    private String currentSubjects = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_profile_edit);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        teacherRef = db.collection("users").document(currentUser.getUid());

        initializeViews();
        loadCurrentProfile();
    }

    private void initializeViews() {
        etPrice = findViewById(R.id.etPrice);
        etLocation = findViewById(R.id.etLocation);
        etBio = findViewById(R.id.etBio);
        etExtraInfo = findViewById(R.id.etExtraInfo);
        tvPriceRecommendation = findViewById(R.id.tvPriceRecommendation);
        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnGetAiPrice = findViewById(R.id.btnGetAiPriceRecommendation);

        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
        btnGetAiPrice.setOnClickListener(v -> getAiPriceRecommendation());
    }

    private void loadCurrentProfile() {
        teacherRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etPrice.setText(doc.getString("hourlyPrice"));
                etLocation.setText(doc.getString("location"));
                etBio.setText(doc.getString("bio"));
                etExtraInfo.setText(doc.getString("extraInfo"));
                
                // שליפת מקצועות לצורך ה-AI
                Object subjectsObj = doc.get("subjects");
                if (subjectsObj instanceof List) {
                    List<?> subjects = (List<?>) subjectsObj;
                    StringBuilder sb = new StringBuilder();
                    for (Object s : subjects) {
                        if (s instanceof Map) {
                            Map<String, String> map = (Map<String, String>) s;
                            sb.append(map.get("subject")).append(", ");
                        }
                    }
                    currentSubjects = sb.toString();
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading profile", e));
    }

    private void getAiPriceRecommendation() {
        String location = etLocation.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (location.isEmpty()) {
            Toast.makeText(this, "אנא הזן מיקום כדי לקבל המלצה מדויקת", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Gemini AI מנתח מחירי שוק ומחשב המלצה...");
        pd.show();

        // שליפת מחירים של מורים אחרים באזור
        db.collection("users")
                .whereEqualTo("userType", "teacher")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Integer> otherPrices = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String priceStr = doc.getString("hourlyPrice");
                        if (priceStr != null && !priceStr.isEmpty()) {
                            try {
                                otherPrices.add(Integer.parseInt(priceStr));
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    GeminiAIHelper.getPriceRecommendation(this, location, bio, currentSubjects, otherPrices, new GeminiAIHelper.AICallback() {
                        @Override
                        public void onResponse(String response) {
                            pd.dismiss();
                            tvPriceRecommendation.setText(response);
                            tvPriceRecommendation.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {
                            pd.dismiss();
                            Toast.makeText(TeacherProfileEditActivity.this, "שגיאה בחיבור ל-AI", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    private void saveProfile() {
        String price = etPrice.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (price.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "חובה להזין מחיר ומיקום!", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("hourlyPrice", price);
        profileData.put("location", location);
        profileData.put("bio", etBio.getText().toString().trim());
        profileData.put("extraInfo", etExtraInfo.getText().toString().trim());

        teacherRef.update(profileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(this, "שגיאה בעדכון הפרופיל", Toast.LENGTH_SHORT).show();
                });
    }
}
