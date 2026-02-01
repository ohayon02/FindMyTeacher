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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class BookingActivity extends AppCompatActivity implements BookingAdapter.OnDateClickListener {

    private static final String TAG = "BookingActivity";

    private final List<String> availableDates = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private BookingAdapter adapter;
    private TextView tvNoSlots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        String teacherId = getIntent().getStringExtra("teacherId");
        String teacherName = getIntent().getStringExtra("teacherName");

        setupToolbar(teacherName);

        tvNoSlots = findViewById(R.id.tvNoSlots);
        RecyclerView rvAvailableDates = findViewById(R.id.rvAvailableDates);
        rvAvailableDates.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingAdapter(availableDates, this);
        rvAvailableDates.setAdapter(adapter);

        loadAvailableDates(teacherId);
    }

    private void setupToolbar(String teacherName) {
        Toolbar toolbar = findViewById(R.id.bookingToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Book with " + teacherName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @SuppressWarnings("unchecked") // Suppressing warning for Firestore data cast
    private void loadAvailableDates(String teacherId) {
        if (teacherId == null) {
            tvNoSlots.setVisibility(View.VISIBLE);
            return;
        }

        DocumentReference teacherRef = db.collection("users").document(teacherId);
        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("availableDates")) {
                List<String> datesFromDb = (List<String>) documentSnapshot.get("availableDates");
                if (datesFromDb != null && !datesFromDb.isEmpty()) {
                    availableDates.clear();
                    availableDates.addAll(datesFromDb);
                    tvNoSlots.setVisibility(View.GONE);
                } else {
                    tvNoSlots.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            } else {
                tvNoSlots.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading available dates", e);
            tvNoSlots.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onDateClick(String date) {
        // For now, just show a toast. We will implement the booking logic later.
        Toast.makeText(this, "You selected " + date, Toast.LENGTH_SHORT).show();
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
