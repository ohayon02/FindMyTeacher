package com.findmyteacher;

import java.util.Date;

public class AvailabilityDate {
    private Date date;
    private boolean isAvailable;

    public AvailabilityDate(Date date, boolean isAvailable) {
        this.date = date;
        this.isAvailable = isAvailable;
    }

    public Date getDate() {
        return date;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}
