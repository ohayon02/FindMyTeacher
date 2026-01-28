package com.findmyteacher;

import java.util.HashMap;
import java.util.Map;

public class LessonSlot {
    private String date;
    private String startTime;
    private String endTime;
    private boolean booked;

    // Default constructor required for calls to DataSnapshot.getValue(LessonSlot.class)
    public LessonSlot() {}

    public LessonSlot(String date, String startTime, String endTime, boolean booked) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.booked = booked;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("booked", booked);
        return result;
    }
}
