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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

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

        // האזנה בזמן אמת לקולקשיין Availability עבור מורה ספציפי
        availabilityListener = db.collection("Availability")
                .whereEqualTo("teacherId", teacherId)
                .addSnapshotListener(this, (queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e);
                        tvNoSlots.setVisibility(View.VISIBLE);
                        return;
                    }

                    lessonSlots.clear();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String date = doc.getString("date");
                            String time = doc.getString("time");
                            Boolean isBooked = doc.getBoolean("booked");
                            String bookedBy = doc.getString("bookedBy");

                            if (date != null && time != null && isBooked != null) {
                                if (!isBooked) {
                                    // שיעור פנוי
                                    LessonSlot slot = new LessonSlot(date, time, true, null);
                                    slot.setStudentName(teacherName);
                                    lessonSlots.add(slot);
                                } else if (currentUserId.equals(bookedBy)) {
                                    // שיעור שהתלמיד הנוכחי כבר הזמין (כדי שיוכל לבטל)
                                    LessonSlot slot = new LessonSlot(date, time, false, bookedBy);
                                    slot.setStudentName(teacherName);
                                    lessonSlots.add(slot);
                                }
                            }
                        }
                    }

                    if (lessonSlots.isEmpty()) {
                        tvNoSlots.setVisibility(View.VISIBLE);
                        tvNoSlots.setText("אין שיעורים פנויים כרגע");
                    } else {
                        tvNoSlots.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onSlotClick(int position) {
        if (position < 0 || position >= lessonSlots.size()) return;

        LessonSlot selectedSlot = lessonSlots.get(position);

        if (teacherId == null) {
            Toast.makeText(this, "שגיאה: מורה לא נמצא", Toast.LENGTH_SHORT).show();
            return;
        }

        final String date = selectedSlot.getDate();
        final String time = selectedSlot.getTime();

        // יצירת מזהה מסמך ייחודי התואם למבנה ב-Firebase
        String timeForId = time.replace(":", "");
        String docId = teacherId + "_" + date + "_" + timeForId;
        final DocumentReference slotRef = db.collection("Availability").document(docId);

        if (selectedSlot.isAvailable()) {
            // הזמנת שיעור: עדכון הסטטוס ל-true ומילוי מזהה התלמיד המזמין
            slotRef.update("booked", true, "bookedBy", currentUserId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(BookingActivity.this, "השיעור נקבע בהצלחה", Toast.LENGTH_SHORT).show();
                        NotificationHelper.scheduleLessonReminder(this, date, time, teacherName);
                    })
                    .addOnFailureListener(err -> Toast.makeText(BookingActivity.this, "הזמנה נכשלה", Toast.LENGTH_SHORT).show());
        } else if (currentUserId.equals(selectedSlot.getBookedBy())) {
            // ביטול שיעור שהוזמן בעבר על ידי אותו תלמיד
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("ביטול שיעור")
                    .setMessage("האם ברצונך לבטל את השיעור שקבעת?")
                    .setPositiveButton("כן, בטל", (dialog, which) -> {
                        slotRef.update("booked", false, "bookedBy", null)
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