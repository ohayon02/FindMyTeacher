package com.findmyteacher;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TeacherMainActivity extends AppCompatActivity {

    private static final String TAG = "TeacherMainActivity";

    private StudentAdapter studentAdapter;
    private final List<Student> allStudents = new ArrayList<>();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration teacherListener;
    private ListenerRegistration availabilityListener;

    private MaterialCalendarView calendarView;
    private RecyclerView rvTodayLessons;
    private LessonSlotAdapter lessonAdapter;
    private List<LessonSlot> allLessons = new ArrayList<>();
    private List<LessonSlot> filteredLessons = new ArrayList<>();
    private Map<String, String> studentNamesCache = new HashMap<>();
    private String selectedDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);
        Log.d(TAG, "onCreate: Teacher Main Activity initialized");

        initializeViews();
        setupStudentRecyclerView();
        setupLessonRecyclerView();
        initCalendar();
        

        
        NotificationHelper.createNotificationChannel(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reloading data");
        loadUserData();
        loadStudents();
        startAvailabilityListener();

    }

    private void initializeViews() {
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());

        MaterialCardView btnManageAvailability = findViewById(R.id.btnManageAvailability);
        btnManageAvailability.setOnClickListener(v -> startActivity(new Intent(this, TeacherAvailabilityActivity.class)));

        MaterialCardView btnInfo = findViewById(R.id.btnInfo);
        btnInfo.setOnClickListener(v -> startActivity(new Intent(this, TeacherProfileEditActivity.class)));
        
        calendarView = findViewById(R.id.calendarView);
        rvTodayLessons = findViewById(R.id.rvTodayLessons);
        
        SearchView searchView = findViewById(R.id.searchStudents);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { filterStudents(q); return false; }
            @Override public boolean onQueryTextChange(String q) { filterStudents(q); return false; }
        });
    }

    private void setupStudentRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvStudents);
        rv.setLayoutManager(new LinearLayoutManager(this));
        studentAdapter = new StudentAdapter(new StudentAdapter.OnStudentClickListener() {
            @Override
            public void onStudentClick(Student student) {
                showStudentActionDialog(student);
            }
        });
        rv.setAdapter(studentAdapter);
        rv.setNestedScrollingEnabled(false);
    }

    private void showStudentActionDialog(Student student) {
        Log.d(TAG, "showStudentActionDialog: for student " + student.getFullName());
        String[] options = {"צ'אט עם התלמיד", "דוח התקדמות AI"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(student.getFullName());
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("studentId", student.getId());
                intent.putExtra("studentName", student.getFullName());
                startActivity(intent);
            } else {
                generateStudentAiReport(student);
            }
        });
        builder.show();
    }

    private void generateStudentAiReport(Student student) {
        Log.d(TAG, "generateStudentAiReport: Generating progress report for " + student.getFullName());
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מנתח התקדמות עם AI...");
        pd.setCancelable(false);
        pd.show();

        db.collection("users").document(student.getId()).get().addOnSuccessListener(doc -> {
            String feedback = doc.getString("lastFeedback");
            
            List<String> lessonDates = allLessons.stream()
                    .filter(l -> student.getId().equals(l.getBookedBy()))
                    .map(LessonSlot::getDate)
                    .distinct()
                    .collect(Collectors.toList());

            GeminiAIHelper.generateStudentProgressReport(this, student.getFullName(), feedback, lessonDates, new GeminiAIHelper.AICallback() {
                @Override
                public void onResponse(String response) {
                    pd.dismiss();
                    Log.d(TAG, "generateStudentAiReport: Report received from AI");
                    showAiReportDialog(student.getFullName(), response);
                }

                @Override
                public void onError(Exception e) {
                    pd.dismiss();
                    Log.e(TAG, "Error generating report", e);
                    Toast.makeText(TeacherMainActivity.this, "שגיאה ביצירת דוח: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(this, "שגיאה בגישה לנתוני תלמיד", Toast.LENGTH_SHORT).show();
        });
    }

    private void showAiReportDialog(String name, String report) {
        new AlertDialog.Builder(this)
                .setTitle("🤖 דוח AI עבור " + name)
                .setMessage(report)
                .setPositiveButton("סגור", null)
                .show();
    }

    private void setupLessonRecyclerView() {
        rvTodayLessons.setLayoutManager(new LinearLayoutManager(this));
        lessonAdapter = new LessonSlotAdapter(filteredLessons, position -> {
            if (position >= 0 && position < filteredLessons.size()) {
                generateLessonAiReport(filteredLessons.get(position));
            }
        });
        rvTodayLessons.setAdapter(lessonAdapter);
        rvTodayLessons.setNestedScrollingEnabled(false);
    }

    private void initCalendar() {
        Calendar today = Calendar.getInstance();
        selectedDate = dateFormat.format(today.getTime());
        calendarView.setSelectedDate(CalendarDay.today());
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                    date.getYear(), date.getMonth(), date.getDay());
            filterLessonsByDate();
        });
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { logoutUser(); return; }

        teacherListener = db.collection("users").document(user.getUid())
            .addSnapshotListener((doc, e) -> {
                if (e != null) return;
                if (doc != null && doc.exists()) {
                    TextView tvWelcome = findViewById(R.id.tvWelcomeTeacher);
                    tvWelcome.setText("שלום, " + doc.getString("fullName"));
                }
            });
    }

    private void startAvailabilityListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "startAvailabilityListener: User is null");
            return;
        }

        availabilityListener = db.collection("Availability")
                .whereEqualTo("teacherId", user.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Availability listener failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        Log.d(TAG, "startAvailabilityListener: Found " + snapshots.size() + " availability slots");
                        allLessons.clear();
                        HashSet<CalendarDay> datesWithLessons = new HashSet<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Log.d(TAG, "startAvailabilityListener: Processing doc: " + doc.getData());
                            String date = doc.getString("date");
                            String time = doc.getString("time");
                            Boolean booked = doc.getBoolean("booked");
                            String bookedBy = doc.getString("bookedBy");
                            
                            boolean isBooked = booked != null && booked;
                            LessonSlot slot = new LessonSlot(date, time, !isBooked, bookedBy);
                            allLessons.add(slot);
                            
                            if (isBooked) {
                                fetchStudentName(slot);
                                try {
                                    String[] p = date.split("-");
                                    datesWithLessons.add(CalendarDay.from(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                                } catch (Exception ignored) {}
                            }
                        }

                        Log.d(TAG, "startAvailabilityListener: about to add " + datesWithLessons.size() + " decorators");
                        calendarView.removeDecorators();
                        calendarView.addDecorator(new EventDecorator(Color.GREEN, datesWithLessons));
                        calendarView.invalidateDecorators();
                        filterLessonsByDate();
                    }
                });
    }

    private void fetchStudentName(LessonSlot slot) {
        String sid = slot.getBookedBy();
        if (sid == null) return;
        if (studentNamesCache.containsKey(sid)) {
            slot.setStudentName(studentNamesCache.get(sid));
            lessonAdapter.notifyDataSetChanged();
            return;
        }
        db.collection("users").document(sid).get().addOnSuccessListener(d -> {
            if (d.exists()) {
                String name = d.getString("fullName");
                studentNamesCache.put(sid, name);
                slot.setStudentName(name);
                lessonAdapter.notifyDataSetChanged();
            }
        });
    }

    private void filterLessonsByDate() {
        filteredLessons.clear();
        for (LessonSlot s : allLessons) {
            if (s.getDate().equals(selectedDate)) filteredLessons.add(s);
        }
        lessonAdapter.notifyDataSetChanged();
    }

    private void loadStudents() {
        Log.d(TAG, "loadStudents: Loading all students");
        db.collection("users").whereEqualTo("userType", "student").get()
            .addOnSuccessListener(snaps -> {
                Log.d(TAG, "loadStudents: Successfully loaded " + snaps.size() + " students");
                allStudents.clear();
                for (QueryDocumentSnapshot d : snaps) {
                    Student s = d.toObject(Student.class);
                    s.setId(d.getId());
                    allStudents.add(s);
                }
                studentAdapter.submitList(new ArrayList<>(allStudents));
            });
    }

    private void filterStudents(String query) {
        String q = query.toLowerCase();
        List<Student> filtered = allStudents.stream()
            .filter(s -> s.getFullName() != null && s.getFullName().toLowerCase().contains(q))
            .collect(Collectors.toList());
        studentAdapter.submitList(filtered);
    }

    private void generateLessonAiReport(LessonSlot slot) {
        if (slot.isAvailable()) return;
        Log.d(TAG, "generateLessonAiReport: Requesting AI lesson summary for student: " + slot.getStudentName());
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מייצר דוח שיעור...");
        pd.show();
        GeminiAIHelper.generateReport(this, slot.getStudentName(), slot.getTime(), new GeminiAIHelper.AICallback() {
            @Override public void onResponse(String r) {
                pd.dismiss();
                Log.d(TAG, "generateLessonAiReport: Success");
                new AlertDialog.Builder(TeacherMainActivity.this)
                        .setTitle("סיכום שיעור AI")
                        .setMessage(r)
                        .setPositiveButton("יציאה", null)
                        .show();
            }
            @Override public void onError(Exception e) {
                pd.dismiss();
                Toast.makeText(TeacherMainActivity.this, "שגיאה ב-AI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logoutUser() {
        Log.d(TAG, "logoutUser: Signing out");
        if (teacherListener != null) teacherListener.remove();
        if (availabilityListener != null) availabilityListener.remove();
        mAuth.signOut();
        startActivity(new Intent(this, ChooseActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (teacherListener != null) teacherListener.remove();
        if (availabilityListener != null) availabilityListener.remove();
    }

    public static class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;
        public EventDecorator(int color, Collection<CalendarDay> dates) { this.color = color; this.dates = new HashSet<>(dates); }
        @Override public boolean shouldDecorate(CalendarDay d) { return dates.contains(dayFormatCheck(d)); }
        
        // Helper to ensure CalendarDay comparison works regardless of internal implementation
        private CalendarDay dayFormatCheck(CalendarDay d) {
            return CalendarDay.from(d.getYear(), d.getMonth(), d.getDay());
        }

        @Override public void decorate(DayViewFacade v) { v.addSpan(new DotSpan(10, color)); }
    }
}
