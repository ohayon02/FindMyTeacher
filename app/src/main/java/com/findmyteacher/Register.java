package com.findmyteacher;

import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.List;

public class Register extends AppCompatActivity {

    private LinearLayout subjectsContainer;
    private List<View> subjectViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Views
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

        // Setup Grade Dropdown
        String[] grades = getResources().getStringArray(R.array.grades_array);
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, grades);
        autoGrade.setAdapter(gradeAdapter);

        // Add first subject field by default
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
                Toast.makeText(Register.this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSubjectField() {
        // Inflate the row from the XML file
        View rowView = LayoutInflater.from(this).inflate(R.layout.row_subject, subjectsContainer, false);
        
        AutoCompleteTextView autoSub = rowView.findViewById(R.id.autoSubjectRow);
        AutoCompleteTextView autoLev = rowView.findViewById(R.id.autoLevelRow);
        ImageButton btnRemove = rowView.findViewById(R.id.btnRemoveRow);

        // Setup Adapters for this row
        autoSub.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.subjects_array)));
        autoLev.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.levels_array)));

        // Setup Remove Logic
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

        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilGrade.setError(null);

        if (fullName.isEmpty()) {
            tilFullName.setError("נא להזין שם מלא");
            isValid = false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("נא להזין אימייל תקין");
            isValid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("סיסמה חייבת להיות לפחות 6 תווים");
            isValid = false;
        }

        if (rgUserType.getCheckedRadioButtonId() == R.id.rbStudent) {
            if (autoGrade.getText().toString().isEmpty()) {
                tilGrade.setError("נא לבחור כיתה");
                isValid = false;
            }
        } else {
            if (subjectViews.isEmpty()) {
                Toast.makeText(this, "נא להוסיף לפחות מקצוע אחד", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }

        return isValid;
    }
}
