package com.findmyteacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChooseActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is already logged in
        if (mAuth.getCurrentUser() != null) {
            checkUserTypeAndNavigate(mAuth.getCurrentUser().getUid());
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button loginButton = findViewById(R.id.btnLogin);
        Button registerButton = findViewById(R.id.btnRegister);

        View.OnClickListener authListener = v -> {
            Intent intent = new Intent(ChooseActivity.this, AuthActivity.class);
            startActivity(intent);
        };

        loginButton.setOnClickListener(authListener);
        registerButton.setOnClickListener(authListener);
    }

    private void checkUserTypeAndNavigate(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        Intent intent;
                        if ("student".equals(userType)) {
                            intent = new Intent(ChooseActivity.this, StudentMainActivity.class);
                        } else {
                            intent = new Intent(ChooseActivity.this, TeacherMainActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        // Document doesn't exist, sign out and stay on choose screen
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChooseActivity", "Error fetching user data", e);
                    mAuth.signOut();
                });
    }
}
