package com.findmyteacher;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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
        
        // תאריך ברירת מחדל - היום
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

        db.collection("users").document(teacherId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    allLessons.clear();
                    HashSet<CalendarDay> datesWithLessons = new HashSet<>();
                    
                    if (documentSnapshot.exists() && documentSnapshot.contains("availability")) {
                        Map<String, Map<String, Object>> availability = (Map<String, Map<String, Object>>) documentSnapshot.get("availability");
                        
                        if (availability != null) {
                            for (Map.Entry<String, Map<String, Object>> dateEntry : availability.entrySet()) {
                                String dateStr = dateEntry.getKey(); // yyyy-MM-dd
                                Map<String, Object> slots = dateEntry.getValue();
                                
                                for (Map.Entry<String, Object> slotEntry : slots.entrySet()) {
                                    String time = slotEntry.getKey();
                                    Object slotValue = slotEntry.getValue();
                                    
                                    boolean isBooked = false;
                                    String bookedBy = null;
                                    
                                    if (slotValue instanceof String) {
                                        isBooked = true;
                                        bookedBy = (String) slotValue;
                                    }
                                    
                                    LessonSlot slot = new LessonSlot(dateStr, time, !isBooked, bookedBy);
                                    allLessons.add(slot);
                                    
                                    if (isBooked) {
                                        fetchStudentName(slot);
                                        try {
                                            String[] parts = dateStr.split("-");
                                            int year = Integer.parseInt(parts[0]);
                                            int month = Integer.parseInt(parts[1]);
                                            int day = Integer.parseInt(parts[2]);
                                            datesWithLessons.add(CalendarDay.from(year, month, day));
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing date: " + dateStr, e);
                                        }
                                    }
                                }
                            }
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
                        // עדכון כל הסלוטים של אותו תלמיד
                        for (LessonSlot s : allLessons) {
                            if (studentId.equals(s.getBookedBy())) {
                                s.setStudentName(name);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void filterLessonsByDate() {
        filteredLessons.clear();
        for (LessonSlot slot : allLessons) {
            if (slot.getDate().equals(selectedDate)) {
                filteredLessons.add(slot);
            }
        }
        adapter.notifyDataSetChanged();
        
        if (filteredLessons.isEmpty()) {
            Toast.makeText(this, "אין שיעורים בתאריך זה", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateAiReport(LessonSlot slot) {
        if (slot.isAvailable()) return;
        
        String studentName = slot.getStudentName() != null ? slot.getStudentName() : "התלמיד";
        
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מייצר דוח AI עבור " + studentName + "...");
        pd.setCancelable(false);
        pd.show();

        GeminiAIHelper.generateReport(studentName, slot.getTime(), new GeminiAIHelper.AICallback() {
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("🤖 דוח התקדמות AI")
               .setMessage(report)
               .setPositiveButton("הבנתי", null)
               .show();
    }

    public static class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;

        public EventDecorator(int color, Collection<CalendarDay> dates) {
            this.color = color;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(10, color));
        }
    }
}
