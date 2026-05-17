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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class BookingActivity extends AppCompatActivity implements LessonSlotAdapter.OnSlotClickListener {

    private static final String TAG = "BookingActivity";

    private final List<LessonSlot> lessonSlots = new ArrayList<>();
    private final List<String> slotDocIds = new ArrayList<>();
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
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        setupToolbar(teacherName);

        tvNoSlots = findViewById(R.id.tvNoSlots);
        RecyclerView rvAvailableSlots = findViewById(R.id.rvAvailableDates);
        rvAvailableSlots.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LessonSlotAdapter(lessonSlots, this);
        adapter.setStudentView(true);
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

    private void setupAvailabilityListener() {
        if (teacherId == null || currentUserId == null) {
            tvNoSlots.setVisibility(View.VISIBLE);
            return;
        }

        // Updated to query the Availability collection
        Query availabilityQuery = db.collection("Availability")
                .whereEqualTo("teacherId", teacherId);

        availabilityListener = availabilityQuery.addSnapshotListener(this, (snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                tvNoSlots.setVisibility(View.VISIBLE);
                return;
            }

            if (snapshots != null) {
                lessonSlots.clear();
                slotDocIds.clear();

                for (QueryDocumentSnapshot doc : snapshots) {
                    AvailabilitySlot slotData = doc.toObject(AvailabilitySlot.class);
                    
                    if (!slotData.isBooked()) {
                        // Unbooked slot
                        LessonSlot slot = new LessonSlot(slotData.getDate(), slotData.getTime(), true, null);
                        slot.setStudentName(teacherName);
                        lessonSlots.add(slot);
                        slotDocIds.add(doc.getId());
                    } else if (currentUserId.equals(slotData.getBookedBy())) {
                        // Slot booked by current user
                        LessonSlot slot = new LessonSlot(slotData.getDate(), slotData.getTime(), false, currentUserId);
                        slot.setStudentName(teacherName);
                        lessonSlots.add(slot);
                        slotDocIds.add(doc.getId());
                    }
                }

                if (lessonSlots.isEmpty()) {
                    tvNoSlots.setVisibility(View.VISIBLE);
                    tvNoSlots.setText("אין שיעורים פנויים כרגע");
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
        if (position < 0 || position >= lessonSlots.size()) return;
        
        LessonSlot selectedSlot = lessonSlots.get(position);
        String docId = slotDocIds.get(position);

        if (selectedSlot.isAvailable()) {
            // Book slot: update Availability collection
            db.collection("Availability").document(docId)
                    .update("booked", true, "bookedBy", currentUserId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(BookingActivity.this, "השיעור נקבע בהצלחה", Toast.LENGTH_SHORT).show();
                        NotificationHelper.scheduleLessonReminder(this, selectedSlot.getDate(), selectedSlot.getTime(), teacherName);
                    })
                    .addOnFailureListener(err -> Toast.makeText(BookingActivity.this, "הזמנה נכשלה", Toast.LENGTH_SHORT).show());
        } else if (currentUserId.equals(selectedSlot.getBookedBy())) {
            // Cancel slot
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("ביטול שיעור")
                    .setMessage("האם ברצונך לבטל את השיעור שקבעת?")
                    .setPositiveButton("כן, בטל", (dialog, which) -> {
                        db.collection("Availability").document(docId)
                                .update("booked", false, "bookedBy", null)
                                .addOnSuccessListener(aVoid -> Toast.makeText(BookingActivity.this, "השיעור בוטל", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("לא", null)
                    .show();
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
