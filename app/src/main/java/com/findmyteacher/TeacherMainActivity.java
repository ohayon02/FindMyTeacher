package com.findmyteacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TeacherMainActivity extends AppCompatActivity {
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        tvWelcome = findViewById(R.id.tvWelcomeTeacher);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnTeacherChats = findViewById(R.id.btnTeacherChats);
        FloatingActionButton fabAddProfileInfo = findViewById(R.id.fabAddSlot);

        loadUserData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(TeacherMainActivity.this, ChooseActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnTeacherChats.setOnClickListener(v -> {
            Toast.makeText(this, "מסך הצאטים בבנייה", Toast.LENGTH_SHORT).show();
        });

        fabAddProfileInfo.setOnClickListener(v -> {
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
