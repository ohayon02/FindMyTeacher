package com.findmyteacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class TeacherMainActivity extends AppCompatActivity {

    private static final String TAG = "TeacherMainActivity";

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        tvWelcome = findViewById(R.id.tvWelcomeTeacher);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnManageAvailability = findViewById(R.id.btnManageAvailability);
        Button btnInfo = findViewById(R.id.btnInfo);
        Button btnTeacherChats = findViewById(R.id.btnTeacherChats);

        loadUserData();

        btnLogout.setOnClickListener(v -> logoutUser());
        btnManageAvailability.setOnClickListener(v -> startActivity(new Intent(this, TeacherAvailabilityActivity.class)));
        btnInfo.setOnClickListener(v -> startActivity(new Intent(this, TeacherProfileEditActivity.class)));
        btnTeacherChats.setOnClickListener(v ->  Toast.makeText(this, "Chat is not implemented yet.", Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvWelcome.setText("Welcome, Teacher " + doc.getString("fullName"));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(this, ChooseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
