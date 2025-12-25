package com.findmyteacher;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Register extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private LinearLayout subjectsContainer;
    private List<View> subjectViews = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextInputLayout tilFullName = findViewById(R.id.tilFullName);
        TextInputLayout tilEmail = findViewById(R.id.tilEmail);
        TextInputLayout tilPassword = findViewById(R.id.tilPassword);
        TextInputLayout tilGrade = findViewById(R.id.tilGrade);
        LinearLayout llTeacherExpertise = findViewById(R.id.llTeacherExpertise);
        subjectsContainer = findViewById(R.id.subjectsContainer);
        Button btnAddSubject = findViewById(R.id.btnAddSubject);

        EditText etFullName = findViewById(R.id.etFullName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        AutoCompleteTextView autoGrade = findViewById(R.id.autoGrade);

        RadioGroup rgUserType = findViewById(R.id.rgUserType);
        Button btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);

        String[] grades = getResources().getStringArray(R.array.grades_array);
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, grades);
        autoGrade.setAdapter(gradeAdapter);

        addSubjectField();
        btnAddSubject.setOnClickListener(v -> addSubjectField());

        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStudent) {
                tilGrade.setVisibility(View.VISIBLE);
                llTeacherExpertise.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbTeacher) {
                tilGrade.setVisibility(View.GONE);
                llTeacherExpertise.setVisibility(View.VISIBLE);
            }
        });

        btnRegisterSubmit.setOnClickListener(v -> {
            if (validateRegister(tilFullName, etFullName, tilEmail, etEmail, tilPassword, etPassword, rgUserType, tilGrade, autoGrade)) {
                registerUser(etFullName.getText().toString().trim(),
                             etEmail.getText().toString().trim(),
                             etPassword.getText().toString().trim(),
                             rgUserType.getCheckedRadioButtonId() == R.id.rbStudent ? "student" : "teacher",
                             autoGrade.getText().toString());
            }
        });
    }

    private void registerUser(String fullName, String email, String password, String userType, String grade) {
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(userId, fullName, email, userType, grade);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String userId, String fullName, String email, String userType, String grade) {
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("userType", userType);

        if (userType.equals("student")) {
            user.put("grade", grade);
        } else {
            List<Map<String, String>> subjects = new ArrayList<>();
            for (View view : subjectViews) {
                AutoCompleteTextView autoSub = view.findViewById(R.id.autoSubjectRow);
                AutoCompleteTextView autoLev = view.findViewById(R.id.autoLevelRow);
                Map<String, String> subjectMap = new HashMap<>();
                subjectMap.put("subject", autoSub.getText().toString());
                subjectMap.put("level", autoLev.getText().toString());
                subjects.add(subjectMap);
            }
            user.put("subjects", subjects);
        }

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(Register.this, "Registered Successfully! Please Login.", Toast.LENGTH_LONG).show();
                    mAuth.signOut(); // Sign out after registration
                    startActivity(new Intent(Register.this, Login.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(Register.this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addSubjectField() {
        View rowView = LayoutInflater.from(this).inflate(R.layout.row_subject, subjectsContainer, false);
        AutoCompleteTextView autoSub = rowView.findViewById(R.id.autoSubjectRow);
        AutoCompleteTextView autoLev = rowView.findViewById(R.id.autoLevelRow);
        ImageButton btnRemove = rowView.findViewById(R.id.btnRemoveRow);

        autoSub.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.subjects_array)));
        autoLev.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.levels_array)));

        btnRemove.setOnClickListener(v -> {
            subjectsContainer.removeView(rowView);
            subjectViews.remove(rowView);
        });

        subjectsContainer.addView(rowView);
        subjectViews.add(rowView);
    }

    private boolean validateRegister(TextInputLayout tilFullName, EditText etFullName,
                                   TextInputLayout tilEmail, EditText etEmail,
                                   TextInputLayout tilPassword, EditText etPassword,
                                   RadioGroup rgUserType,
                                   TextInputLayout tilGrade, AutoCompleteTextView autoGrade) {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isValid = true;

        if (fullName.isEmpty()) {
            tilFullName.setError("Full name required");
            isValid = false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Valid email required");
            isValid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 chars");
            isValid = false;
        }
        if (rgUserType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
    }
}
