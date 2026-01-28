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
import java.util.ArrayList;
import java.util.List;

public class BookingActivity extends AppCompatActivity {

    private RecyclerView rvAvailableDates;
    private BookingAdapter adapter;
    private List<String> availableDates = new ArrayList<>();
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
        rvAvailableDates = findViewById(R.id.rvAvailableDates);
        rvAvailableDates.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookingAdapter(availableDates, this::onDateClick);
        rvAvailableDates.setAdapter(adapter);

        loadAvailableDates();
    }

    private void setupToolbar(String teacherName) {
        Toolbar toolbar = findViewById(R.id.bookingToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Book with " + teacherName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadAvailableDates() {
        if (teacherId == null) return;

        DocumentReference teacherRef = db.collection("users").document(teacherId);
        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("availableDates")) {
                List<String> datesFromDb = (List<String>) documentSnapshot.get("availableDates");
                availableDates.clear();
                if (datesFromDb != null) {
                    availableDates.addAll(datesFromDb);
                }

                if (availableDates.isEmpty()) {
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

    private void onDateClick(String date) {
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
