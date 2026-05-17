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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

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

        calendarView = findViewById(R.id.calendarView);
        tvSelectedDates = findViewById(R.id.tvSelectedDates);
        Button btnExit = findViewById(R.id.btnExit);
        Button btnSaveAvailability = findViewById(R.id.btnSaveAvailability);

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
        String uid = mAuth.getUid();
        if (uid == null) return;

        WriteBatch batch = db.batch();
        String[] times = {"14:00", "15:00", "16:00"};

        for (String date : selectedDates) {
            for (String time : times) {
                String slotId = uid + "_" + date + "_" + time.replace(":", "");
                AvailabilitySlot slot = new AvailabilitySlot(slotId, uid, date, time, false);
                batch.set(db.collection("Availability").document(slotId), slot);
            }
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "הזמינות נשמרה בהצלחה!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error saving availability", e);
            Toast.makeText(this, "שגיאה בשמירת הזמינות", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAvailability() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("Availability")
                .whereEqualTo("teacherId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    selectedDates.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        if (date != null && !selectedDates.contains(date)) {
                            selectedDates.add(date);
                        }
                    }
                    updateSelectedDatesUI();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading availability", e));
    }
}
