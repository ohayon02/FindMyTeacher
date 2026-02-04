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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class BookingActivity extends AppCompatActivity implements BookingAdapter.OnDateClickListener {

    private static final String TAG = "BookingActivity";

    private final List<BookingAdapter.BookingDate> dateList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private BookingAdapter adapter;
    private TextView tvNoSlots;
    private String teacherId;
    private ListenerRegistration dateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        teacherId = getIntent().getStringExtra("teacherId");
        String teacherName = getIntent().getStringExtra("teacherName");

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

                List<String> bookedDatesFromDb = new ArrayList<>();
                if (documentSnapshot.contains("bookedDates")) {
                    List<String> fromDb = (List<String>) documentSnapshot.get("bookedDates");
                    if (fromDb != null) {
                        bookedDatesFromDb.addAll(fromDb);
                    }
                }

                dateList.clear();
                if (availableDatesFromDb != null && !availableDatesFromDb.isEmpty()) {
                    for (String date : availableDatesFromDb) {
                        boolean isBooked = bookedDatesFromDb.contains(date);
                        dateList.add(new BookingAdapter.BookingDate(date, isBooked));
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
        DocumentReference teacherRef = db.collection("users").document(teacherId);

        if (!selectedDate.isBooked()) {
            // Book the date
            teacherRef.update("bookedDates", FieldValue.arrayUnion(selectedDate.getDate()))
                .addOnSuccessListener(aVoid -> Toast.makeText(BookingActivity.this, "You have booked " + selectedDate.getDate(), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(BookingActivity.this, "Booking failed. Please try again.", Toast.LENGTH_SHORT).show());
        } else {
            // Unbook the date
            teacherRef.update("bookedDates", FieldValue.arrayRemove(selectedDate.getDate()))
                .addOnSuccessListener(aVoid -> Toast.makeText(BookingActivity.this, "Booking for " + selectedDate.getDate() + " canceled", Toast.LENGTH_SHORT).show())
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
