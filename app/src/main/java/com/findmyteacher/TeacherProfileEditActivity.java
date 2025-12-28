package com.findmyteacher;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class TeacherProfileEditActivity extends AppCompatActivity {

    private EditText etPrice, etLocation, etBio, etExtraInfo;
    private Button btnSave, btnCancel;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_profile_edit);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etPrice = findViewById(R.id.etPrice);
        etLocation = findViewById(R.id.etLocation);
        etBio = findViewById(R.id.etBio);
        etExtraInfo = findViewById(R.id.etExtraInfo);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancel);

        loadCurrentProfile();

        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadCurrentProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etPrice.setText(doc.getString("hourlyPrice"));
                etLocation.setText(doc.getString("location"));
                etBio.setText(doc.getString("bio"));
                etExtraInfo.setText(doc.getString("extraInfo"));
            }
        });
    }

    private void saveProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> profileData = new HashMap<>();
        profileData.put("hourlyPrice", etPrice.getText().toString().trim());
        profileData.put("location", etLocation.getText().toString().trim());
        profileData.put("bio", etBio.getText().toString().trim());
        profileData.put("extraInfo", etExtraInfo.getText().toString().trim());

        db.collection("users").document(uid).update(profileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בעדכון הפרופיל", Toast.LENGTH_SHORT).show();
                });
    }
}
