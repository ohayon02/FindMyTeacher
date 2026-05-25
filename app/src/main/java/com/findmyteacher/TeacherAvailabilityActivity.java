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
        WriteBatch batch = db.batch();

        // השעות הקבועות שיוגדרו לכל יום שנבחר
        String[] times = {"14:00", "15:00", "16:00"};

        for (String date : selectedDates) {
            for (String time : times) {
                // יצירת מזהה ייחודי קבוע לכל סלוט (למשל: uid_2026-05-14_1400)
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

        // ביצוע השמירה במכה אחת בפיירבייס
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Availability saved successfully to Availability collection.");
                    Toast.makeText(TeacherAvailabilityActivity.this, "השעות נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating availability", e);
                    Toast.makeText(TeacherAvailabilityActivity.this, "שגיאה בשמירת השעות.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAvailability() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String teacherId = currentUser.getUid();

        // טעינת השעות הקיימות מהקולקשיין החדש Availability
        db.collection("Availability")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        selectedDates.clear();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String date = doc.getString("date");
                            if (date != null && !selectedDates.contains(date)) {
                                selectedDates.add(date);
                            }
                        }
                        updateSelectedDatesUI();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading availability", e));
    }
}