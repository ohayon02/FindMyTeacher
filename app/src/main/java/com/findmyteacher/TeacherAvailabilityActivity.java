package com.findmyteacher;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TeacherAvailabilityActivity extends AppCompatActivity {

    private static final String TAG = "TeacherAvailability";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private CalendarView calendarView;
    private TextView tvSelectedDates;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private DocumentReference teacherRef;

    private final List<String> selectedDates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_availability);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        teacherRef = db.collection("users").document(currentUser.getUid());

        // Initialize Views
        calendarView = findViewById(R.id.calendarView);
        tvSelectedDates = findViewById(R.id.tvSelectedDates);
        Button btnExit = findViewById(R.id.btnExit);
        Button btnSaveAvailability = findViewById(R.id.btnSaveAvailability);

        // Setup Listeners
        btnExit.setOnClickListener(v -> finish());
        btnSaveAvailability.setOnClickListener(v -> saveAvailability());
        setupCalendarListener();

        loadAvailability();
    }

    private void setupCalendarListener() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            String dateString = DATE_FORMAT.format(calendar.getTime());

            if (selectedDates.contains(dateString)) {
                selectedDates.remove(dateString);
            } else {
                selectedDates.add(dateString);
            }
            updateSelectedDatesUI();
        });
    }

    private void updateSelectedDatesUI() {
        tvSelectedDates.setText(String.join("\n", selectedDates));
    }

    private void saveAvailability() {
        teacherRef.update("availableDates", selectedDates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Availability updated successfully.");
                    Toast.makeText(TeacherAvailabilityActivity.this, "Availability saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating availability", e);
                    Toast.makeText(TeacherAvailabilityActivity.this, "Failed to save availability.", Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressWarnings("unchecked") // Suppressing warning for Firestore data cast
    private void loadAvailability() {
        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("availableDates")) {
                List<String> loadedDates = (List<String>) documentSnapshot.get("availableDates");
                if (loadedDates != null) {
                    selectedDates.clear();
                    selectedDates.addAll(loadedDates);
                    updateSelectedDatesUI();
                }
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading availability", e));
    }
}
