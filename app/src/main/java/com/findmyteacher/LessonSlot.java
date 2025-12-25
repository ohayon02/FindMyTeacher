package com.findmyteacher;

public class LessonSlot {
    private String id;
    private String teacherId;
    private String date; // Format: yyyy-MM-dd
    private String time; // Format: HH:mm
    private String studentId; // null if available
    private String studentName;
    private String status; // "available" or "booked"

    public LessonSlot() {}

    public LessonSlot(String id, String teacherId, String date, String time, String status) {
        this.id = id;
        this.teacherId = teacherId;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public String getId() { return id; }
    public String getTeacherId() { return teacherId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStatus() { return status; }

    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setStatus(String status) { this.status = status; }
}
