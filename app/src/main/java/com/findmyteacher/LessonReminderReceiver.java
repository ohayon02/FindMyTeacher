package com.findmyteacher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LessonReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String partnerName = intent.getStringExtra("partnerName");
        String lessonTime = intent.getStringExtra("lessonTime");

        String title = "תזכורת לשיעור";
        String message = "יש לך שיעור עם " + partnerName + " בשעה " + lessonTime;

        NotificationHelper.createNotificationChannel(context);
        NotificationHelper.showNotification(context, title, message);
    }
}
