package com.findmyteacher;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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
import com.google.firebase.firestore.DocumentSnapshot;
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

        initializeViews();
        setupStudentRecyclerView();
        setupLessonRecyclerView();
        initCalendar();
        
        loadUserData();
        loadStudents();
        
        NotificationHelper.createNotificationChannel(this);
    }

    private void initializeViews() {
        ImageButton btnLogout = findViewById(R.id.btnLogout);
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
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מנתח התקדמות עם AI...");
        pd.setCancelable(false);
        pd.show();

        // Get student's feedback from Firestore
        db.collection("users").document(student.getId()).get().addOnSuccessListener(doc -> {
            String feedback = doc.getString("lastFeedback");
            
            // Collect lesson dates for this student
            List<String> lessonDates = allLessons.stream()
                    .filter(l -> student.getId().equals(l.getBookedBy()))
                    .map(LessonSlot::getDate)
                    .distinct()
                    .collect(Collectors.toList());

            GeminiAIHelper.generateStudentProgressReport(student.getFullName(), feedback, lessonDates, new GeminiAIHelper.AICallback() {
                @Override
                public void onResponse(String response) {
                    pd.dismiss();
                    showAiReportDialog(student.getFullName(), response);
                }

                @Override
                public void onError(Exception e) {
                    pd.dismiss();
                    Toast.makeText(TeacherMainActivity.this, "שגיאה ביצירת דוח", Toast.LENGTH_SHORT).show();
                }
            });
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
                    updateLessonsFromDoc(doc);
                }
            });
    }

    private void updateLessonsFromDoc(DocumentSnapshot doc) {
        allLessons.clear();
        HashSet<CalendarDay> datesWithLessons = new HashSet<>();
        Map<String, Map<String, Object>> availability = (Map<String, Map<String, Object>>) doc.get("availability");
        if (availability != null) {
            for (Map.Entry<String, Map<String, Object>> dateEntry : availability.entrySet()) {
                String dateStr = dateEntry.getKey();
                for (Map.Entry<String, Object> slotEntry : dateEntry.getValue().entrySet()) {
                    boolean isBooked = slotEntry.getValue() instanceof String;
                    String bookedBy = isBooked ? (String) slotEntry.getValue() : null;
                    LessonSlot slot = new LessonSlot(dateStr, slotEntry.getKey(), !isBooked, bookedBy);
                    allLessons.add(slot);
                    if (isBooked) {
                        fetchStudentName(slot);
                        scheduleNotification(slot);
                        try {
                            String[] p = dateStr.split("-");
                            datesWithLessons.add(CalendarDay.from(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        calendarView.removeDecorators();
        calendarView.addDecorator(new EventDecorator(Color.GREEN, datesWithLessons));
        filterLessonsByDate();
    }

    private void scheduleNotification(LessonSlot slot) {
        // Schedule reminder for the teacher
        String sid = slot.getBookedBy();
        if (sid == null) return;
        
        db.collection("users").document(sid).get().addOnSuccessListener(d -> {
            if (d.exists()) {
                String studentName = d.getString("fullName");
                NotificationHelper.scheduleLessonReminder(this, slot.getDate(), slot.getTime(), studentName);
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
        db.collection("users").whereEqualTo("userType", "student").get()
            .addOnSuccessListener(snaps -> {
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
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מייצר דוח שיעור...");
        pd.show();
        GeminiAIHelper.generateReport(slot.getStudentName(), slot.getTime(), new GeminiAIHelper.AICallback() {
            @Override public void onResponse(String r) {
                pd.dismiss();
                new AlertDialog.Builder(TeacherMainActivity.this).setTitle("סיכום שיעור AI").setMessage(r).show();
            }
            @Override public void onError(Exception e) {
                pd.dismiss();
                Toast.makeText(TeacherMainActivity.this, "שגיאה ב-AI", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logoutUser() {
        if (teacherListener != null) teacherListener.remove();
        mAuth.signOut();
        startActivity(new Intent(this, ChooseActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (teacherListener != null) teacherListener.remove();
    }

    public static class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;
        public EventDecorator(int color, Collection<CalendarDay> dates) { this.color = color; this.dates = new HashSet<>(dates); }
        @Override public boolean shouldDecorate(CalendarDay d) { return dates.contains(d); }
        @Override public void decorate(DayViewFacade v) { v.addSpan(new DotSpan(10, color)); }
    }
}
