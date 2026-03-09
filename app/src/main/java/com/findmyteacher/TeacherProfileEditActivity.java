package com.findmyteacher;

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
import java.util.HashMap;
import java.util.Map;

public class TeacherProfileEditActivity extends AppCompatActivity {

    private static final String TAG = "TeacherProfileEdit";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private DocumentReference teacherRef;

    private EditText etPrice, etLocation, etBio, etExtraInfo;
    private TextView tvPriceRecommendation;

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
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading profile", e));
    }

    private void getAiPriceRecommendation() {
        // סימולציה של AI - המלצה לפי מיקום וביוגרפיה (במערכת אמיתית נשתמש ב-Gemini API)
        String location = etLocation.getText().toString().trim();
        int basePrice = 120;
        
        if (location.toLowerCase().contains("תל אביב") || location.toLowerCase().contains("מרכז")) {
            basePrice += 40;
        }
        
        String recommendation = "המלצת AI: לפי המיקום והתחום שלך, מחיר מומלץ הוא " + basePrice + " - " + (basePrice + 30) + " ש\"ח.";
        tvPriceRecommendation.setText(recommendation);
        tvPriceRecommendation.setVisibility(View.VISIBLE);
        etPrice.setText(String.valueOf(basePrice));
    }

    private void saveProfile() {
        String price = etPrice.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        // חובה להגדיר מחיר וכתובת
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
