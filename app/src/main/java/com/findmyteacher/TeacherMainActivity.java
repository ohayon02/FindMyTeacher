package com.findmyteacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TeacherMainActivity extends AppCompatActivity {
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvWelcome;
    private CalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        tvWelcome = findViewById(R.id.tvWelcomeTeacher);
        calendarView = findViewById(R.id.calendarView);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnTeacherChats = findViewById(R.id.btnTeacherChats);
        Button btnManageAvailability = findViewById(R.id.btnManageAvailability);
        Button btnInfo = findViewById(R.id.btnInfo);

        loadUserData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(TeacherMainActivity.this, ChooseActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnTeacherChats.setOnClickListener(v -> {
            // Let's create a new clean ChatActivity later
            Toast.makeText(this, "Chat is not implemented yet.", Toast.LENGTH_SHORT).show();
        });

        btnManageAvailability.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherMainActivity.this, TeacherAvailabilityActivity.class);
            startActivity(intent);
        });

        btnInfo.setOnClickListener(v -> {
             Intent intent = new Intent(TeacherMainActivity.this, TeacherProfileEditActivity.class);
             startActivity(intent);
        });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("fullName");
                tvWelcome.setText("Welcome, Teacher " + name);
            }
        });
    }
}
