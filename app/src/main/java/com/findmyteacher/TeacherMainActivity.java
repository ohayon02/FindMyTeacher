package com.findmyteacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TeacherMainActivity extends AppCompatActivity {

    private static final String TAG = "TeacherMainActivity";

    private StudentAdapter adapter;
    private final List<Student> allStudents = new ArrayList<>();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        tvWelcome = findViewById(R.id.tvWelcomeTeacher);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnManageAvailability = findViewById(R.id.btnManageAvailability);
        Button btnMyLessons = findViewById(R.id.btnMyLessons);
        Button btnInfo = findViewById(R.id.btnInfo);
        RecyclerView recyclerView = findViewById(R.id.rvStudents);
        SearchView searchView = findViewById(R.id.searchStudents);

        loadUserData();

        btnLogout.setOnClickListener(v -> logoutUser());
        btnManageAvailability.setOnClickListener(v -> startActivity(new Intent(this, TeacherAvailabilityActivity.class)));
        btnMyLessons.setOnClickListener(v -> {
            // כאן נפתח את הפעילות החדשה של הקלנדר/רשימת שיעורים
            Toast.makeText(this, "פתיחת לוח שיעורים...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, TeacherLessonsActivity.class));
        });
        btnInfo.setOnClickListener(v -> startActivity(new Intent(this, TeacherProfileEditActivity.class)));

        setupRecyclerView(recyclerView);
        loadStudents();
        setupSearchView(searchView);
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(student -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                logoutUser(); // Redirect to login
                return;
            }
            if (student == null || student.getId() == null || student.getId().isEmpty()) {
                Toast.makeText(this, "Cannot open chat. Student data is missing.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(TeacherMainActivity.this, ChatActivity.class);
            intent.putExtra("studentId", student.getId());
            intent.putExtra("studentName", student.getFullName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterStudents(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterStudents(newText);
                return false;
            }
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            logoutUser();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvWelcome.setText("Welcome, Teacher " + doc.getString("fullName"));
                        
                        // בדיקה אם המורה הגדיר מחיר ומיקום
                        String price = doc.getString("hourlyPrice");
                        String location = doc.getString("location");
                        if (price == null || price.isEmpty() || location == null || location.isEmpty()) {
                            Toast.makeText(this, "אנא הגדר מחיר ומיקום בפרופיל שלך!", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, TeacherProfileEditActivity.class));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    private void loadStudents() {
        db.collection("users").whereEqualTo("userType", "student").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allStudents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Student student = doc.toObject(Student.class);
                        student.setId(doc.getId());
                        allStudents.add(student);
                    }
                    adapter.submitList(new ArrayList<>(allStudents));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading students", e));
    }

    private void filterStudents(String query) {
        List<Student> filteredList;
        if (query == null || query.isEmpty()) {
            filteredList = new ArrayList<>(allStudents);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            filteredList = allStudents.stream()
                    .filter(s -> s.getFullName() != null && s.getFullName().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
        }
        adapter.submitList(filteredList);
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(this, ChooseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
