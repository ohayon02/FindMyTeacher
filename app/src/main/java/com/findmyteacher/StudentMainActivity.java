package com.findmyteacher;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentMainActivity extends AppCompatActivity {

    private static final String TAG = "StudentMainActivity";

    private TeacherAdapter teacherAdapter;
    private LessonSlotAdapter myLessonsAdapter;
    private final List<Teacher> allTeachers = new ArrayList<>();
    private final List<Teacher> filteredTeachers = new ArrayList<>();
    private final List<LessonSlot> myBookedLessons = new ArrayList<>();

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private TextView tvWelcome;
    private String studentFullName = "תלמיד";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_main);

        tvWelcome = findViewById(R.id.tvWelcomeUser);
        Button btnLogout = findViewById(R.id.btnLogout);
        RecyclerView rvTeachers = findViewById(R.id.rvTeachers);
        RecyclerView rvMyLessons = findViewById(R.id.rvMyLessons);
        SearchView searchView = findViewById(R.id.searchTeachers);
        Button btnReportProgress = findViewById(R.id.btnReportProgress);
        Button btnChatAI = findViewById(R.id.btnChatAI);

        loadUserData();
        btnLogout.setOnClickListener(v -> logoutUser());
        btnReportProgress.setOnClickListener(v -> showProgressReportDialog());
        btnChatAI.setOnClickListener(v -> startAIChat());

        setupTeachersRecyclerView(rvTeachers);
        setupMyLessonsRecyclerView(rvMyLessons);

        loadTeachersAndMyLessons();
        setupSearchView(searchView);

        NotificationHelper.createNotificationChannel(this);
    }

    private void startAIChat() {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("studentId", "AI");
        intent.putExtra("studentName", "מורה AI חכם");
        startActivity(intent);
    }

    private void showProgressReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("איך הולך לך בלימודים?");

        final EditText input = new EditText(this);
        input.setHint("כתוב כאן איך אתה מרגיש עם החומר, קשיים או הצלחות...");
        builder.setView(input);

        builder.setPositiveButton("שלח ונתח עם AI", (dialog, which) -> {
            String feedback = input.getText().toString();
            if (!feedback.isEmpty()) {
                saveAndAnalyzeFeedback(feedback);
            }
        });
        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveAndAnalyzeFeedback(String feedback) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("lastFeedback", feedback);
        data.put("feedbackTimestamp", System.currentTimeMillis());

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("שומר ומנתח עם AI...");
        pd.show();

        db.collection("users").document(uid).update(data)
                .addOnSuccessListener(aVoid -> {
                    List<String> lessonDates = myBookedLessons.stream()
                            .map(LessonSlot::getDate)
                            .collect(Collectors.toList());

                    GeminiAIHelper.generateStudentProgressReport(this, studentFullName, feedback, lessonDates, new GeminiAIHelper.AICallback() {
                        @Override
                        public void onResponse(String response) {
                            pd.dismiss();
                            showAiResponseDialog(response);
                        }

                        @Override
                        public void onError(Exception e) {
                            pd.dismiss();
                            Toast.makeText(StudentMainActivity.this, "הניתוח נכשל: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "שגיאה בשמירת המשוב", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAiResponseDialog(String report) {
        new AlertDialog.Builder(this)
                .setTitle("🤖 ניתוח התקדמות AI")
                .setMessage(report)
                .setPositiveButton("יציאה", null)
                .show();
    }

    private void setupTeachersRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        teacherAdapter = new TeacherAdapter(filteredTeachers, (teacher, isChat) -> {
            // ייעול כפילות ה-Intent: החלפת מחלקת היעד בצורה דינמית
            Class<?> targetClass = isChat ? ChatActivity.class : BookingActivity.class;
            Intent intent = new Intent(StudentMainActivity.this, targetClass);
            intent.putExtra("teacherId", teacher.getId());
            intent.putExtra("teacherName", teacher.getFullName());
            startActivity(intent);
        });
        recyclerView.setAdapter(teacherAdapter);
    }

    private void setupMyLessonsRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // הוסר ה-Listener הריק של הלחיצה הכללית
        myLessonsAdapter = new LessonSlotAdapter(myBookedLessons, null);

        myLessonsAdapter.setStudentView(true);
        myLessonsAdapter.setOnCancelClickListener(this::showCancelConfirmation);
        recyclerView.setAdapter(myLessonsAdapter);
    }

    private void showCancelConfirmation(int position) {
        LessonSlot slot = myBookedLessons.get(position);
        // תיקון המלל מ"הסטודנט" ל"המורה" כדי שיתאים למסך התלמיד
        new AlertDialog.Builder(this)
                .setTitle("ביטול שיעור")
                .setMessage("האם אתה בטוח שברצונך לבטל את השיעור עם המורה " + slot.getStudentName() + " בתאריך " + slot.getDate() + " בשעה " + slot.getTime() + "?")
                .setPositiveButton("כן, בטל", (dialog, which) -> cancelLesson(slot, position))
                .setNegativeButton("לא", null)
                .show();
    }

    private void cancelLesson(LessonSlot slot, int position) {
        String teacherId = slot.getTeacherId();
        if (teacherId == null) return;

        String timeForId = slot.getTime().replace(":", "");
        String docId = teacherId + "_" + slot.getDate() + "_" + timeForId;

        db.collection("Availability").document(docId)
                .update("booked", false, "bookedBy", null)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "השיעור בוטל בהצלחה", Toast.LENGTH_SHORT).show();
                    // ייעול: עדכון מקומי במקום קריאה חוזרת ויקרה לכל ה-Database מהשרת
                    if (position >= 0 && position < myBookedLessons.size()) {
                        myBookedLessons.remove(position);
                        myLessonsAdapter.notifyItemRemoved(position);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cancelling lesson", e);
                    Toast.makeText(this, "שגיאה בביטול השיעור", Toast.LENGTH_SHORT).show();
                });
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
                studentFullName = doc.getString("fullName");
                tvWelcome.setText("שלום, " + studentFullName);
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    private void loadTeachersAndMyLessons() {
        String currentUserId = mAuth.getUid();
        if (currentUserId == null) return;

        db.collection("users").whereEqualTo("userType", "teacher").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTeachers.clear();
                    myBookedLessons.clear();

                    Map<String, String> teacherNames = new HashMap<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Teacher t = doc.toObject(Teacher.class);
                        t.setId(doc.getId());
                        allTeachers.add(t);
                        teacherNames.put(t.getId(), t.getFullName());
                    }
                    filterTeachers("");

                    db.collection("Availability")
                            .whereEqualTo("bookedBy", currentUserId)
                            .get()
                            .addOnSuccessListener(slotsDocs -> {
                                for (QueryDocumentSnapshot slotDoc : slotsDocs) {
                                    String date = slotDoc.getString("date");
                                    String time = slotDoc.getString("time");
                                    String tId = slotDoc.getString("teacherId");

                                    LessonSlot slot = new LessonSlot(date, time, false, currentUserId);
                                    slot.setTeacherId(tId);
                                    slot.setStudentName(teacherNames.getOrDefault(tId, "מורה"));
                                    myBookedLessons.add(slot);
                                }
                                myLessonsAdapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error loading booked lessons", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading teachers", e));
    }

    private void filterTeachers(String query) {
        filteredTeachers.clear();
        if (query.isEmpty()) {
            filteredTeachers.addAll(allTeachers);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            // ייעול ה-Stream: הזרמה ישירה לתוך הרשימה ללא יצירת רשימה זמנית בזיכרון
            allTeachers.stream()
                    .filter(t -> (t.getFullName() != null && t.getFullName().toLowerCase().contains(lowerCaseQuery)) ||
                            (t.getSubjectsString() != null && t.getSubjectsString().toLowerCase().contains(lowerCaseQuery)))
                    .forEach(filteredTeachers::add);
        }
        teacherAdapter.notifyDataSetChanged();
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(StudentMainActivity.this, ChooseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}