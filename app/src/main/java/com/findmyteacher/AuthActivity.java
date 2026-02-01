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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    private TextInputLayout tilEmail, tilPassword, tilFullName, tilGrade;
    private EditText etEmail, etPassword, etFullName;
    private AutoCompleteTextView autoGrade;
    private Button btnLoginSubmit, btnRegisterSubmit;
    private TextView tvSwitchAuthText, tvSwitchAuthLink, tvHeader;
    private ImageButton btnBack;
    private RadioGroup rgUserType;
    private LinearLayout llTeacherExpertise, subjectsContainer;
    private Button btnAddSubject;
    private List<View> subjectViews = new ArrayList<>();

    private boolean isLogin = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);

        initializeViews();
        setupClickListeners();
        updateUI();
    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnBack = findViewById(R.id.btnBack);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvSwitchAuthText = findViewById(R.id.tvSwitchAuthText);
        tvSwitchAuthLink = findViewById(R.id.tvSwitchAuthLink);
        tilFullName = findViewById(R.id.tilFullName);
        etFullName = findViewById(R.id.etFullName);
        tilGrade = findViewById(R.id.tilGrade);
        autoGrade = findViewById(R.id.autoGrade);
        rgUserType = findViewById(R.id.rgUserType);
        llTeacherExpertise = findViewById(R.id.llTeacherExpertise);
        subjectsContainer = findViewById(R.id.subjectsContainer);
        btnAddSubject = findViewById(R.id.btnAddSubject);
        tvHeader = findViewById(R.id.tvHeader);

        String[] grades = getResources().getStringArray(R.array.grades_array);
        autoGrade.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, grades));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        tvSwitchAuthLink.setOnClickListener(v -> {
            isLogin = !isLogin;
            updateUI();
        });

        btnLoginSubmit.setOnClickListener(v -> {
            if (validateLogin()) {
                loginUser(etEmail.getText().toString().trim(), etPassword.getText().toString().trim());
            }
        });

        btnRegisterSubmit.setOnClickListener(v -> {
            if (validateRegister()) {
                registerUser(etFullName.getText().toString().trim(),
                        etEmail.getText().toString().trim(),
                        etPassword.getText().toString().trim(),
                        rgUserType.getCheckedRadioButtonId() == R.id.rbStudent ? "student" : "teacher",
                        autoGrade.getText().toString());
            }
        });

        btnAddSubject.setOnClickListener(v -> addSubjectField());

        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            tilGrade.setVisibility(checkedId == R.id.rbStudent ? View.VISIBLE : View.GONE);
            llTeacherExpertise.setVisibility(checkedId == R.id.rbTeacher ? View.VISIBLE : View.GONE);
        });
    }

    private void updateUI() {
        if (isLogin) {
            tvHeader.setText("שלום שוב!");
            tilFullName.setVisibility(View.GONE);
            rgUserType.setVisibility(View.GONE);
            tilGrade.setVisibility(View.GONE);
            llTeacherExpertise.setVisibility(View.GONE);
            btnLoginSubmit.setVisibility(View.VISIBLE);
            btnRegisterSubmit.setVisibility(View.GONE);
            tvSwitchAuthText.setText("אין לך חשבון? ");
            tvSwitchAuthLink.setText("הירשם עכשיו");
        } else {
            tvHeader.setText("יצירת חשבון");
            tilFullName.setVisibility(View.VISIBLE);
            rgUserType.setVisibility(View.VISIBLE);
            // Reset radio group to student by default
            if(rgUserType.getCheckedRadioButtonId() == -1) rgUserType.check(R.id.rbStudent);
            tilGrade.setVisibility(rgUserType.getCheckedRadioButtonId() == R.id.rbStudent ? View.VISIBLE : View.GONE);
            llTeacherExpertise.setVisibility(rgUserType.getCheckedRadioButtonId() == R.id.rbTeacher ? View.VISIBLE : View.GONE);
            btnLoginSubmit.setVisibility(View.GONE);
            btnRegisterSubmit.setVisibility(View.VISIBLE);
            tvSwitchAuthText.setText("יש לך כבר חשבון? ");
            tvSwitchAuthLink.setText("התחבר");
        }
    }

    // --- Login Logic ---
    private void loginUser(String email, String password) {
        progressDialog.setMessage("מתחבר...");
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        checkUserTypeAndNavigate(mAuth.getCurrentUser().getUid());
                    } else {
                        Toast.makeText(AuthActivity.this, "התחברות נכשלה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserTypeAndNavigate(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        if ("student".equals(userType)) {
                            startActivity(new Intent(AuthActivity.this, StudentMainActivity.class));
                        } else if ("teacher".equals(userType)) {
                            startActivity(new Intent(AuthActivity.this, TeacherMainActivity.class));
                        }
                        finish();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user data", e));
    }

    private boolean validateLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean isValid = true;

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("נא להזין אימייל תקין");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("נא להזין סיסמה");
            isValid = false;
        }
        return isValid;
    }

    // --- Register Logic ---
    private void registerUser(String fullName, String email, String password, String userType, String grade) {
        progressDialog.setMessage("יוצר חשבון...");
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(mAuth.getCurrentUser().getUid(), fullName, email, userType, grade);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(AuthActivity.this, "הרשמה נכשלה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
                if(autoSub != null && autoLev != null && !autoSub.getText().toString().isEmpty()) {
                    Map<String, String> subjectMap = new HashMap<>();
                    subjectMap.put("subject", autoSub.getText().toString());
                    subjectMap.put("level", autoLev.getText().toString());
                    subjects.add(subjectMap);
                }
            }
            user.put("subjects", subjects);
        }

        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(AuthActivity.this, "נרשמת בהצלחה!", Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                    isLogin = true;
                    updateUI(); // Switch to login view
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(AuthActivity.this, "שגיאה במסד הנתונים: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private boolean validateRegister() {
        boolean isValid = validateLogin(); // Basic email/pass validation

        if (etFullName.getText().toString().trim().isEmpty()) {
            tilFullName.setError("שם מלא הוא שדה חובה");
            isValid = false;
        } else {
            tilFullName.setError(null);
        }

        if (rgUserType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "יש לבחור סוג משתמש", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }
}
