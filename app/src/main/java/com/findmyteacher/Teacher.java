package com.findmyteacher;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Teacher {
    private String id;
    private String fullName;
    private String email;
    private List<Map<String, String>> subjects;
    private String hourlyPrice;
    private String location;
    private String bio;
    private String extraInfo;

    // Default constructor is required for Firebase
    public Teacher() {}

    public Teacher(String id, String fullName, String email, List<Map<String, String>> subjects, String hourlyPrice, String location, String bio, String extraInfo) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.subjects = subjects;
        this.hourlyPrice = hourlyPrice;
        this.location = location;
        this.bio = bio;
        this.extraInfo = extraInfo;
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public List<Map<String, String>> getSubjects() {
        return subjects;
    }

    public String getHourlyPrice() {
        return hourlyPrice;
    }

    public String getLocation() {
        return location;
    }

    public String getBio() {
        return bio;
    }

    public String getExtraInfo() {
        return extraInfo;
    }
    
    // --- Helper methods for display ---
    public String getSubjectsString() {
        if (subjects == null || subjects.isEmpty()) {
            return "";
        }
        return subjects.stream()
                .map(subjectMap -> subjectMap.get("name"))
                .collect(Collectors.joining(", "));
    }
}
