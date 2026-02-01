package com.findmyteacher;

import com.google.firebase.firestore.PropertyName;

public class LessonSlot {

    @PropertyName("date")
    private String date;

    @PropertyName("startTime")
    private String startTime;

    @PropertyName("endTime")
    private String endTime;

    @PropertyName("booked")
    private boolean booked;

    public LessonSlot() {
        // Default constructor required for calls to DataSnapshot.getValue(LessonSlot.class)
    }

    public LessonSlot(String date, String startTime, String endTime, boolean booked) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.booked = booked;
    }

    @PropertyName("date")
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @PropertyName("startTime")
    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    @PropertyName("endTime")
    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    @PropertyName("booked")
    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }
}
