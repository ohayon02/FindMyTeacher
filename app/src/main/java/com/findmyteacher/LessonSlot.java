package com.findmyteacher;

public class LessonSlot {
    private String date;
    private String time;
    private boolean isAvailable;
    private String bookedBy;
    private String studentName;
    private String teacherId;

    public LessonSlot(String date, String time, boolean isAvailable, String bookedBy) {
        this.date = date;
        this.time = time;
        this.isAvailable = isAvailable;
        this.bookedBy = bookedBy;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }
}
