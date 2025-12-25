package com.findmyteacher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentMainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TeacherAdapter adapter;
    private List<Teacher> allTeachers = new ArrayList<>();
    private List<Teacher> filteredTeachers = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        tvWelcome = findViewById(R.id.tvWelcomeUser);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        recyclerView = findViewById(R.id.rvTeachers);
        SearchView searchView = findViewById(R.id.searchTeachers);

        loadUserData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(StudentMainActivity.this, ChooseActivity.class));
            finish();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeacherAdapter(filteredTeachers, teacher -> {
            Intent intent = new Intent(StudentMainActivity.this, ChatActivity.class);
            intent.putExtra("teacherId", teacher.getId());
            intent.putExtra("teacherName", teacher.getFullName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadTeachers();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTeachers(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTeachers(newText);
                return true;
            }
        });
    }

    private void loadUserData() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("fullName");
                tvWelcome.setText("Welcome, " + name);
            }
        });
    }

    private void loadTeachers() {
        db.collection("users")
                .whereEqualTo("userType", "teacher")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTeachers.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String name = doc.getString("fullName");
                        String email = doc.getString("email");
                        List<Map<String, String>> subjects = (List<Map<String, String>>) doc.get("subjects");
                        allTeachers.add(new Teacher(id, name, email, subjects));
                    }
                    filteredTeachers.clear();
                    filteredTeachers.addAll(allTeachers);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading teachers", e));
    }

    private void filterTeachers(String query) {
        filteredTeachers.clear();
        if (query.isEmpty()) {
            filteredTeachers.addAll(allTeachers);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Teacher t : allTeachers) {
                if (t.getSubjectsString().toLowerCase().contains(lowerCaseQuery) ||
                    t.getFullName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredTeachers.add(t);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
