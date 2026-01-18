package com.findmyteacher;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Teacher {
    private String id;
    private String fullName;
    private String email;
    private List<Map<String, String>> subjects;
    private String hourlyPrice;
    private String location;
    private String bio;
    private String extraInfo;
    private List<LessonSlot> availableSlots;

    public Teacher() {}

    public Teacher(String id, String fullName, String email, List<Map<String, String>> subjects,
                   String hourlyPrice, String location, String bio, String extraInfo, List<LessonSlot> availableSlots) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.subjects = subjects;
        this.hourlyPrice = hourlyPrice;
        this.location = location;
        this.bio = bio;
        this.extraInfo = extraInfo;
        this.availableSlots = availableSlots;
    }

    // Getters
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public List<Map<String, String>> getSubjects() { return subjects; }
    public String getHourlyPrice() { return hourlyPrice; }
    public String getLocation() { return location; }
    public String getBio() { return bio; }
    public String getExtraInfo() { return extraInfo; }
    public List<LessonSlot> getAvailableSlots() { return availableSlots; }

    public String getSubjectsString() {
        if (subjects == null || subjects.isEmpty()) return "אין מקצועות רשומים";
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> sub : subjects) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(sub.get("subject")).append(" (").append(sub.get("level")).append(")");
        }
        return sb.toString();
    }

    public String getAvailabilityString() {
        if (availableSlots == null || availableSlots.isEmpty()) {
            return "לא זמין כרגע";
        }

        Collections.sort(availableSlots, (s1, s2) -> s1.getDate().compareTo(s2.getDate()));

        StringBuilder sb = new StringBuilder();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", new Locale("iw"));
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (LessonSlot slot : availableSlots) {
            if (!slot.isBooked()) {
                try {
                    Date date = parseFormat.parse(slot.getDate());
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(dayFormat.format(date));
                } catch (ParseException e) {
                    // Ignore date if format is incorrect
                }
            }
        }

        if (sb.length() == 0) {
            return "כל השיעורים תפוסים";
        }

        return "פנוי ב: " + sb.toString();
    }
}
