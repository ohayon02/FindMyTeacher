package com.findmyteacher;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Teacher {

    @DocumentId
    private String id;

    @PropertyName("fullName")
    private String fullName;

    @PropertyName("email")
    private String email;

    @PropertyName("subjects")
    private List<Map<String, String>> subjects;

    @PropertyName("hourlyPrice")
    private String hourlyPrice;

    @PropertyName("location")
    private String location;

    @PropertyName("bio")
    private String bio;

    @PropertyName("extraInfo")
    private String extraInfo;

    public Teacher() {
        // Default constructor required for calls to DataSnapshot.getValue(Teacher.class)
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

    // Setters
    public void setId(String id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setSubjects(List<Map<String, String>> subjects) { this.subjects = subjects; }
    public void setHourlyPrice(String hourlyPrice) { this.hourlyPrice = hourlyPrice; }
    public void setLocation(String location) { this.location = location; }
    public void setBio(String bio) { this.bio = bio; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }

    // Helper method
    public String getSubjectsString() {
        if (subjects == null || subjects.isEmpty()) {
            return "Not specified";
        }
        return subjects.stream()
                .map(subjectMap -> subjectMap.get("subject"))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining(", "));
    }
}
