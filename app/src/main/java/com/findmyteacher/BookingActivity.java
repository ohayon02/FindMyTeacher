package com.findmyteacher;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookingActivity extends AppCompatActivity implements LessonSlotAdapter.OnSlotClickListener {

    private static final String TAG = "BookingActivity";

    private final List<LessonSlot> lessonSlots = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private LessonSlotAdapter adapter;
    private TextView tvNoSlots;
    private String teacherId;
    private String teacherName;
    private ListenerRegistration availabilityListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        teacherId = getIntent().getStringExtra("teacherId");
        teacherName = getIntent().getStringExtra("teacherName");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupToolbar(teacherName);

        tvNoSlots = findViewById(R.id.tvNoSlots);
        RecyclerView rvAvailableSlots = findViewById(R.id.rvAvailableDates);
        rvAvailableSlots.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LessonSlotAdapter(lessonSlots, this);
        rvAvailableSlots.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupAvailabilityListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (availabilityListener != null) {
            availabilityListener.remove();
        }
    }

    private void setupToolbar(String teacherName) {
        Toolbar toolbar = findViewById(R.id.bookingToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("קביעת שיעור עם " + (teacherName != null ? teacherName : "המורה"));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void setupAvailabilityListener() {
        if (teacherId == null) {
            tvNoSlots.setVisibility(View.VISIBLE);
            return;
        }

        DocumentReference teacherRef = db.collection("users").document(teacherId);
        availabilityListener = teacherRef.addSnapshotListener(this, (documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                tvNoSlots.setVisibility(View.VISIBLE);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                Map<String, Map<String, Object>> availability = (Map<String, Map<String, Object>>) documentSnapshot.get("availability");
                lessonSlots.clear();

                if (availability != null && !availability.isEmpty()) {
                    for (Map.Entry<String, Map<String, Object>> dateEntry : availability.entrySet()) {
                        String date = dateEntry.getKey();
                        Map<String, Object> slots = dateEntry.getValue();
                        for (Map.Entry<String, Object> slotEntry : slots.entrySet()) {
                            String time = slotEntry.getKey();
                            Object slotValue = slotEntry.getValue();
                            
                            if (slotValue instanceof Boolean && (Boolean) slotValue) {
                                // Slot is available
                                lessonSlots.add(new LessonSlot(date, time, true, null));
                            } else if (slotValue instanceof String) {
                                String bookedBy = (String) slotValue;
                                // Show only if booked by the current user
                                if (currentUserId.equals(bookedBy)) {
                                    lessonSlots.add(new LessonSlot(date, time, false, bookedBy));
                                }
                            }
                        }
                    }
                }

                if (lessonSlots.isEmpty()) {
                    tvNoSlots.setVisibility(View.VISIBLE);
                } else {
                    tvNoSlots.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            } else {
                tvNoSlots.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onSlotClick(int position) {
        LessonSlot selectedSlot = lessonSlots.get(position);

        if (teacherId == null) {
            Toast.makeText(this, "שגיאה: מורה לא נמצא", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentReference teacherRef = db.collection("users").document(teacherId);
        final String date = selectedSlot.getDate();
        final String time = selectedSlot.getTime();
        final String slotPath = "availability." + date + "." + time;

        if (selectedSlot.isAvailable()) {
            teacherRef.update(slotPath, currentUserId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(BookingActivity.this, "השיעור נקבע בהצלחה ל-" + date + " בשעה " + time, Toast.LENGTH_SHORT).show();
                        NotificationHelper.scheduleLessonReminder(this, date, time, teacherName);
                    })
                    .addOnFailureListener(err -> Toast.makeText(BookingActivity.this, "הזמנה נכשלה", Toast.LENGTH_SHORT).show());
        } else if (currentUserId.equals(selectedSlot.getBookedBy())) {
            teacherRef.update(slotPath, true)
                    .addOnSuccessListener(aVoid -> Toast.makeText(BookingActivity.this, "השיעור בוטל", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(BookingActivity.this, "ביטול נכשל", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
