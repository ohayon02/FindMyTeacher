package com.findmyteacher;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookingActivity extends AppCompatActivity implements BookingAdapter.OnSlotClickListener {

    private RecyclerView rvAvailableSlots;
    private BookingAdapter adapter;
    private List<LessonSlot> availableSlots = new ArrayList<>();
    private TextView tvNoSlots;

    private FirebaseFirestore db;
    private String teacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        teacherId = getIntent().getStringExtra("teacherId");
        String teacherName = getIntent().getStringExtra("teacherName");

        setupToolbar(teacherName);

        tvNoSlots = findViewById(R.id.tvNoSlots);
        rvAvailableSlots = findViewById(R.id.rvAvailableSlots);
        rvAvailableSlots.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingAdapter(availableSlots, this);
        rvAvailableSlots.setAdapter(adapter);

        loadAvailableSlots();
    }

    private void setupToolbar(String teacherName) {
        Toolbar toolbar = findViewById(R.id.bookingToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Book a Lesson with " + teacherName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadAvailableSlots() {
        if (teacherId == null) return;

        DocumentReference teacherRef = db.collection("users").document(teacherId);
        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<Map<String, Object>> slotsFromDb = (List<Map<String, Object>>) documentSnapshot.get("availableSlots");
                availableSlots.clear();
                if (slotsFromDb != null) {
                    for (Map<String, Object> slotMap : slotsFromDb) {
                        if (!(boolean) slotMap.get("booked")) { // Only add slots that are not already booked
                           availableSlots.add(new LessonSlot(
                                (String) slotMap.get("date"),
                                (String) slotMap.get("startTime"),
                                (String) slotMap.get("endTime"),
                                false
                           ));
                        }
                    }
                }

                if (availableSlots.isEmpty()) {
                    tvNoSlots.setVisibility(View.VISIBLE);
                } else {
                    tvNoSlots.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onSlotClick(LessonSlot slot) {
        // Here we will implement the booking logic
        bookLesson(slot);
    }

    private void bookLesson(LessonSlot slotToBook) {
        DocumentReference teacherRef = db.collection("users").document(teacherId);

        db.runTransaction(transaction -> {
            // 1. Read the current state of the teacher document
            List<Map<String, Object>> currentSlots = (List<Map<String, Object>>) transaction.get(teacherRef).get("availableSlots");
            if (currentSlots == null) currentSlots = new ArrayList<>();

            // 2. Find the specific slot and update its "booked" status
            for (Map<String, Object> slotMap : currentSlots) {
                if (slotToBook.getDate().equals(slotMap.get("date")) &&
                    slotToBook.getStartTime().equals(slotMap.get("startTime"))) {
                    
                    if((boolean)slotMap.get("booked")){
                        throw new IllegalStateException("This slot has just been booked by someone else!");
                    }
                    slotMap.put("booked", true);
                    break;
                }
            }

            // 3. Write the new state back to the document
            transaction.update(teacherRef, "availableSlots", currentSlots);
            return null; // Transaction success

        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Lesson booked successfully!", Toast.LENGTH_SHORT).show();
            // Refresh the list to remove the booked slot
            loadAvailableSlots();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
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
