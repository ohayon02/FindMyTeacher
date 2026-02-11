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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingActivity extends AppCompatActivity implements BookingAdapter.OnDateClickListener {

    private static final String TAG = "BookingActivity";

    private final List<BookingAdapter.BookingDate> dateList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private BookingAdapter adapter;
    private TextView tvNoSlots;
    private String teacherId;
    private ListenerRegistration dateListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        teacherId = getIntent().getStringExtra("teacherId");
        String teacherName = getIntent().getStringExtra("teacherName");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupToolbar(teacherName);

        tvNoSlots = findViewById(R.id.tvNoSlots);
        RecyclerView rvAvailableDates = findViewById(R.id.rvAvailableDates);
        rvAvailableDates.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingAdapter(dateList, this);
        rvAvailableDates.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupDateListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dateListener != null) {
            dateListener.remove();
        }
    }

    private void setupToolbar(String teacherName) {
        Toolbar toolbar = findViewById(R.id.bookingToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Book with " + teacherName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void setupDateListener() {
        if (teacherId == null) {
            tvNoSlots.setVisibility(View.VISIBLE);
            return;
        }

        DocumentReference teacherRef = db.collection("users").document(teacherId);
        dateListener = teacherRef.addSnapshotListener(this, (documentSnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                tvNoSlots.setVisibility(View.VISIBLE);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                List<String> availableDatesFromDb = (List<String>) documentSnapshot.get("availableDates");
                Map<String, String> bookedDatesMap = new HashMap<>();

                if (documentSnapshot.contains("bookedDates")) {
                    Object bookedDatesObject = documentSnapshot.get("bookedDates");

                    if (bookedDatesObject instanceof Map) {
                        // New format: Map<String, String>
                        Map<String, String> fromDb = (Map<String, String>) bookedDatesObject;
                        bookedDatesMap.putAll(fromDb);
                    } else if (bookedDatesObject instanceof List) {
                        // Old format: List<String>
                        List<String> fromDb = (List<String>) bookedDatesObject;
                        for (String date : fromDb) {
                            // Mark as booked by an unknown user to make it red and un-bookable by others
                            bookedDatesMap.put(date, "unknown_user_placeholder");
                        }
                    }
                }

                dateList.clear();
                if (availableDatesFromDb != null && !availableDatesFromDb.isEmpty()) {
                    for (String date : availableDatesFromDb) {
                        boolean isBooked = bookedDatesMap.containsKey(date);
                        String bookedBy = isBooked ? bookedDatesMap.get(date) : null;
                        dateList.add(new BookingAdapter.BookingDate(date, isBooked, bookedBy));
                    }
                    tvNoSlots.setVisibility(View.GONE);
                } else {
                    tvNoSlots.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            } else {
                Log.d(TAG, "Current data: null");
                tvNoSlots.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDateClick(int position) {
        BookingAdapter.BookingDate selectedDate = dateList.get(position);

        if (teacherId == null) {
            Toast.makeText(this, "Error: Teacher not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        final DocumentReference teacherRef = db.collection("users").document(teacherId);
        final String date = selectedDate.getDate();

        if (!selectedDate.isBooked()) {
            // Book the date - needs careful handling for data migration from List to Map
            teacherRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Toast.makeText(BookingActivity.this, "Teacher data not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Object bookedDatesObject = documentSnapshot.get("bookedDates");

                // Case 1: Data is in the old List format. Convert to Map and update.
                if (bookedDatesObject instanceof List) {
                    Map<String, String> newBookedDatesMap = new HashMap<>();
                    for (String d : (List<String>) bookedDatesObject) {
                        newBookedDatesMap.put(d, "unknown_user_placeholder"); // Preserve old bookings
                    }
                    newBookedDatesMap.put(date, currentUserId); // Add the new booking
                    teacherRef.update("bookedDates", newBookedDatesMap)
                            .addOnSuccessListener(aVoid -> Toast.makeText(BookingActivity.this, "You have booked " + date, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(err -> Toast.makeText(BookingActivity.this, "Booking failed. Please try again.", Toast.LENGTH_SHORT).show());
                } else {
                    // Case 2: Data is already a Map or doesn't exist. A simple dot-notation update is safe.
                    teacherRef.update("bookedDates." + date, currentUserId)
                            .addOnSuccessListener(aVoid -> Toast.makeText(BookingActivity.this, "You have booked " + date, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(err -> Toast.makeText(BookingActivity.this, "Booking failed. Please try again.", Toast.LENGTH_SHORT).show());
                }
            }).addOnFailureListener(e -> Toast.makeText(BookingActivity.this, "Could not read teacher data to book.", Toast.LENGTH_SHORT).show());

        } else if (currentUserId.equals(selectedDate.getBookedBy())) {
            // Unbook the date - this is safe as it operates on a map field
            teacherRef.update("bookedDates." + date, FieldValue.delete())
                    .addOnSuccessListener(aVoid -> Toast.makeText(BookingActivity.this, "Booking for " + date + " canceled", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(BookingActivity.this, "Cancellation failed. Please try again.", Toast.LENGTH_SHORT).show());
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
