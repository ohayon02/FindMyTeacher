package com.findmyteacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentMainActivity extends AppCompatActivity {

    private static final String TAG = "StudentMainActivity";

    private TeacherAdapter adapter;
    private final List<Teacher> allTeachers = new ArrayList<>();
    private final List<Teacher> filteredTeachers = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_main);

        tvWelcome = findViewById(R.id.tvWelcomeUser);
        Button btnLogout = findViewById(R.id.btnLogout);
        RecyclerView recyclerView = findViewById(R.id.rvTeachers);
        SearchView searchView = findViewById(R.id.searchTeachers);

        loadUserData();
        btnLogout.setOnClickListener(v -> logoutUser());

        setupRecyclerView(recyclerView);
        loadTeachers();
        setupSearchView(searchView);
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeacherAdapter(filteredTeachers, (teacher, isChat) -> {
            Intent intent;
            if (isChat) {
                intent = new Intent(StudentMainActivity.this, ChatActivity.class);
            } else {
                intent = new Intent(StudentMainActivity.this, BookingActivity.class);
            }
            intent.putExtra("teacherId", teacher.getId());
            intent.putExtra("teacherName", teacher.getFullName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchView(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTeachers(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTeachers(newText);
                return false;
            }
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvWelcome.setText("Welcome, " + doc.getString("fullName"));
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    private void loadTeachers() {
        db.collection("users").whereEqualTo("userType", "teacher").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTeachers.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        allTeachers.add(doc.toObject(Teacher.class));
                    }
                    filterTeachers(""); // Initially show all teachers
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading teachers", e));
    }

    private void filterTeachers(String query) {
        filteredTeachers.clear();
        if (query.isEmpty()) {
            filteredTeachers.addAll(allTeachers);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            List<Teacher> result = allTeachers.stream()
                    .filter(t -> t.getFullName().toLowerCase().contains(lowerCaseQuery) ||
                                 t.getSubjectsString().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
            filteredTeachers.addAll(result);
        }
        adapter.notifyDataSetChanged();
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(StudentMainActivity.this, ChooseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
