package com.findmyteacher;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
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

public class TeacherLessonsActivity extends AppCompatActivity {

    private static final String TAG = "TeacherLessons";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    
    private MaterialCalendarView calendarView;
    private RecyclerView rvLessons;
    private Button btnBack;
    private LessonSlotAdapter adapter;
    private List<LessonSlot> allLessons = new ArrayList<>();
    private List<LessonSlot> filteredLessons = new ArrayList<>();
    private Map<String, String> studentNamesCache = new HashMap<>();
    private String selectedDate;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_lessons);

        calendarView = findViewById(R.id.calendarView);
        rvLessons = findViewById(R.id.rvLessons);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        
        setupRecyclerView();
        
        Calendar today = Calendar.getInstance();
        selectedDate = dateFormat.format(today.getTime());
        calendarView.setSelectedDate(CalendarDay.today());
        
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                    date.getYear(), date.getMonth(), date.getDay());
            filterLessonsByDate();
        });

        loadAllLessons();
    }

    private void setupRecyclerView() {
        rvLessons.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LessonSlotAdapter(filteredLessons, position -> {
            LessonSlot slot = filteredLessons.get(position);
            generateAiReport(slot);
        });
        rvLessons.setAdapter(adapter);
    }

    private void loadAllLessons() {
        String teacherId = mAuth.getUid();
        if (teacherId == null) return;

        // טעינת כל הסלוטים של המורה מהקולקשיין החדש Availability
        db.collection("Availability")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allLessons.clear();
                    HashSet<CalendarDay> datesWithLessons = new HashSet<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String dateStr = doc.getString("date");
                        String time = doc.getString("time");
                        Boolean isBooked = doc.getBoolean("booked");
                        String bookedBy = doc.getString("bookedBy");

                        if (dateStr == null || time == null || isBooked == null) continue;

                        LessonSlot slot = new LessonSlot(dateStr, time, !isBooked, bookedBy);
                        allLessons.add(slot);

                        if (isBooked) {
                            fetchStudentName(slot);
                            try {
                                String[] parts = dateStr.split("-");
                                datesWithLessons.add(CalendarDay.from(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                            } catch (Exception ignored) {}
                        }
                    }
                    calendarView.removeDecorators();
                    calendarView.addDecorator(new EventDecorator(Color.GREEN, datesWithLessons));
                    filterLessonsByDate();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading lessons", e));
    }

    private void fetchStudentName(LessonSlot slot) {
        String studentId = slot.getBookedBy();
        if (studentId == null) return;

        if (studentNamesCache.containsKey(studentId)) {
            slot.setStudentName(studentNamesCache.get(studentId));
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("users").document(studentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        if (name == null) name = "תלמיד ללא שם";
                        studentNamesCache.put(studentId, name);
                        slot.setStudentName(name);
                        for (LessonSlot s : allLessons) {
                            if (studentId.equals(s.getBookedBy())) s.setStudentName(name);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void filterLessonsByDate() {
        filteredLessons.clear();
        for (LessonSlot slot : allLessons) {
            if (slot.getDate().equals(selectedDate)) filteredLessons.add(slot);
        }
        adapter.notifyDataSetChanged();
    }

    private void generateAiReport(LessonSlot slot) {
        if (slot.isAvailable()) return;
        
        String studentName = slot.getStudentName() != null ? slot.getStudentName() : "התלמיד";
        
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מייצר דוח AI עבור " + studentName + "...");
        pd.setCancelable(false);
        pd.show();

        // הוספתי את הפרמטר 'this' כ-Context
        GeminiAIHelper.generateReport(this, studentName, slot.getTime(), new GeminiAIHelper.AICallback() {
            @Override
            public void onResponse(String response) {
                pd.dismiss();
                showReportDialog(response);
            }

            @Override
            public void onError(Exception e) {
                pd.dismiss();
                Toast.makeText(TeacherLessonsActivity.this, "שגיאה ביצירת דוח: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showReportDialog(String report) {
        new MaterialAlertDialogBuilder(this)
               .setTitle("🤖 דוח התקדמות AI")
               .setMessage(report)
               .setPositiveButton("יציאה", (dialog, which) -> dialog.dismiss())
               .setCancelable(true)
               .show();
    }

    public static class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;
        public EventDecorator(int color, Collection<CalendarDay> dates) { this.color = color; this.dates = new HashSet<>(dates); }
        @Override public boolean shouldDecorate(CalendarDay day) { return dates.contains(day); }
        @Override public void decorate(DayViewFacade view) { view.addSpan(new DotSpan(10, color)); }
    }
}
