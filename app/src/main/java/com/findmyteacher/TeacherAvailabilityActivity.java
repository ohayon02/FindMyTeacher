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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherAvailabilityActivity extends AppCompatActivity {

    private static final String TAG = "TeacherAvailability";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private CalendarView calendarView;
    private TextView tvSelectedDates;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private final List<String> selectedDates = new ArrayList<>();
    private final List<String> initialDates = new ArrayList<>();
    private final List<String> bookedSlotIds = new ArrayList<>();
    private final List<String> teacherConfiguredSlots = new ArrayList<>();

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
        loadTeacherConfiguredSlots();
    }

    private void loadTeacherConfiguredSlots() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String slotsStr = documentSnapshot.getString("availableSlots");
                        teacherConfiguredSlots.clear();
                        if (slotsStr != null && !slotsStr.isEmpty()) {
                            String[] slots = slotsStr.split(",");
                            for (String slot : slots) {
                                String trimmed = slot.trim();
                                if (trimmed.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                                    teacherConfiguredSlots.add(trimmed);
                                }
                            }
                        }
                        // Default slots if none configured
                        if (teacherConfiguredSlots.isEmpty()) {
                            teacherConfiguredSlots.add("14:00");
                            teacherConfiguredSlots.add("15:00");
                            teacherConfiguredSlots.add("16:00");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading teacher slots", e));
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String teacherId = currentUser.getUid();

        // Compute diff
        List<String> addedDates = new ArrayList<>(selectedDates);
        addedDates.removeAll(initialDates);

        List<String> removedDates = new ArrayList<>(initialDates);
        removedDates.removeAll(selectedDates);

        if (addedDates.isEmpty() && removedDates.isEmpty()) {
            Toast.makeText(this, "אין שינויים לשמירה", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();
        boolean skippedBooked = false;

        if (teacherConfiguredSlots.isEmpty()) {
            Toast.makeText(this, "לא הוגדרו שעות פעילות בפרופיל", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add new dates
        for (String date : addedDates) {
            for (String time : teacherConfiguredSlots) {
                String timeForId = time.replace(":", "");
                String docId = teacherId + "_" + date + "_" + timeForId;
                DocumentReference slotRef = db.collection("Availability").document(docId);

                Map<String, Object> slotData = new HashMap<>();
                slotData.put("teacherId", teacherId);
                slotData.put("date", date);
                slotData.put("time", time);
                slotData.put("slotId", docId);
                slotData.put("booked", false);
                slotData.put("bookedBy", null);

                batch.set(slotRef, slotData);
            }
        }

        // Remove dates
        for (String date : removedDates) {
            for (String time : teacherConfiguredSlots) {
                String timeForId = time.replace(":", "");
                String docId = teacherId + "_" + date + "_" + timeForId;

                if (bookedSlotIds.contains(docId)) {
                    skippedBooked = true;
                    continue;
                }

                DocumentReference slotRef = db.collection("Availability").document(docId);
                batch.delete(slotRef);
            }
        }

        final boolean finalSkipped = skippedBooked;
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Availability updated successfully.");
                    String message = "השינויים נשמרו בהצלחה!";
                    if (finalSkipped) {
                        message += "\nחלק מהשעות לא הוסרו כי הן כבר הוזמנו.";
                    }
                    Toast.makeText(TeacherAvailabilityActivity.this, message, Toast.LENGTH_LONG).show();
                    loadAvailability(); // Reload to sync state
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating availability", e);
                    Toast.makeText(TeacherAvailabilityActivity.this, "שגיאה בשמירת השינויים.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAvailability() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String teacherId = currentUser.getUid();

        db.collection("Availability")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    selectedDates.clear();
                    initialDates.clear();
                    bookedSlotIds.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String date = doc.getString("date");
                            String docId = doc.getId();
                            Boolean isBooked = doc.getBoolean("booked");

                            if (Boolean.TRUE.equals(isBooked)) {
                                bookedSlotIds.add(docId);
                            }

                            if (date != null && !selectedDates.contains(date)) {
                                selectedDates.add(date);
                                initialDates.add(date);
                            }
                        }
                    }
                    updateSelectedDatesUI();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading availability", e));
    }
}