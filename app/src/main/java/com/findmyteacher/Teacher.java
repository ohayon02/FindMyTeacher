package com.findmyteacher;

import java.util.List;
import java.util.Map;

public class Teacher {
    private String id;
    private String fullName;
    private String email;
    private List<Map<String, String>> subjects;

    public Teacher() {} // Required for Firestore

    public Teacher(String id, String fullName, String email, List<Map<String, String>> subjects) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.subjects = subjects;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public List<Map<String, String>> getSubjects() { return subjects; }
    
    public String getSubjectsString() {
        if (subjects == null || subjects.isEmpty()) return "אין מקצועות רשומים";
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> sub : subjects) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(sub.get("subject")).append(" (").append(sub.get("level")).append(")");
        }
        return sb.toString();
    }
}
