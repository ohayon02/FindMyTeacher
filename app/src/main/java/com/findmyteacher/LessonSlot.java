package com.findmyteacher;

public class LessonSlot {
    private String date;
    private String time;
    private boolean isAvailable;
    private String bookedBy;

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
}
