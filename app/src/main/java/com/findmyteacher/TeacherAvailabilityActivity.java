package com.findmyteacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherAvailabilityActivity extends AppCompatActivity {

    private static final String TAG = "TeacherAvailability";
    private RecyclerView rvAvailability;
    private AvailabilityAdapter adapter;
    private List<AvailabilityDate> availabilityDates;
    private FirebaseFirestore db;
    private String currentUserId;
    private DocumentReference teacherRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_availability);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        teacherRef = db.collection("users").document(currentUserId);

        rvAvailability = findViewById(R.id.rvAvailability);
        rvAvailability.setLayoutManager(new LinearLayoutManager(this));

        availabilityDates = new ArrayList<>();
        adapter = new AvailabilityAdapter(availabilityDates, this::onAvailabilityChanged);
        rvAvailability.setAdapter(adapter);

        FloatingActionButton fabChat = findViewById(R.id.fabChat);
        fabChat.setOnClickListener(view -> 
            startActivity(new Intent(TeacherAvailabilityActivity.this, AvailabilityChatbotActivity.class))
        );

        loadAvailabilityData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvailabilityData(); // Refresh data when returning to the activity
    }

    private void loadAvailabilityData() {
        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;

            List<Map<String, Object>> slots = (List<Map<String, Object>>) documentSnapshot.get("availableSlots");
            if (slots == null) slots = new ArrayList<>();

            availabilityDates.clear();
            Calendar calendar = Calendar.getInstance();

            for (int i = 0; i < 7; i++) {
                Date day = calendar.getTime();
                String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(day);
                boolean isAvailable = false;
                for (Map<String, Object> slot : slots) {
                    if (dateString.equals(slot.get("date"))) {
                        isAvailable = true;
                        break;
                    }
                }
                availabilityDates.add(new AvailabilityDate(day, isAvailable));
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading availability", e));
    }

    private void onAvailabilityChanged(AvailabilityDate date, boolean isAvailable) {
        String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.getDate());

        if (isAvailable) {
            LessonSlot newSlot = new LessonSlot(dateString, "16:00", "20:00", false);
            teacherRef.update("availableSlots", FieldValue.arrayUnion(newSlot.toMap()))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Added availability for " + dateString))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding availability", e));
        } else {
            // To remove, we need to find the exact map to remove.
            teacherRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) return;
                List<Map<String, Object>> slots = (List<Map<String, Object>>) documentSnapshot.get("availableSlots");
                if (slots == null) return;

                slots.removeIf(slot -> dateString.equals(slot.get("date")));

                teacherRef.set(Map.of("availableSlots", slots), SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Removed availability for " + dateString))
                    .addOnFailureListener(e -> Log.e(TAG, "Error removing availability", e));
            });
        }
    }
}
