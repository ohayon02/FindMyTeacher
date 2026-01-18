package com.findmyteacher;

import java.util.HashMap;
import java.util.Map;

public class LessonSlot {
    private String date;
    private String startTime;
    private String endTime;
    private boolean isBooked;

    public LessonSlot() {}

    public LessonSlot(String date, String startTime, String endTime, boolean isBooked) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBooked = isBooked;
    }

    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public boolean isBooked() { return isBooked; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", date);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("booked", isBooked);
        return map;
    }
}
