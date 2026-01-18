package com.findmyteacher;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TeacherAvailabilityActivity extends AppCompatActivity {

    private RecyclerView rvAvailability;
    private AvailabilityAdapter adapter;
    private List<AvailabilityDate> availabilityDates;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_availability);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        rvAvailability = findViewById(R.id.rvAvailability);
        rvAvailability.setLayoutManager(new LinearLayoutManager(this));

        availabilityDates = new ArrayList<>();
        adapter = new AvailabilityAdapter(availabilityDates, this::onAvailabilityChanged);
        rvAvailability.setAdapter(adapter);

        loadAvailabilityData();
    }

    private void loadAvailabilityData() {
        // For simplicity, we'll show the next 7 days
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            availabilityDates.add(new AvailabilityDate(calendar.getTime(), false));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        adapter.notifyDataSetChanged();
    }

    private void onAvailabilityChanged(AvailabilityDate date, boolean isAvailable) {
        // Here you would update the teacher's availability in Firestore
        // For now, we'll just log it
        System.out.println("Date: " + date.getDate() + ", Available: " + isAvailable);

        // TODO: Implement Firestore update logic here
    }
}
