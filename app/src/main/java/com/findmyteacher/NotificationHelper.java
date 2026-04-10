package com.findmyteacher;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;

public class NotificationHelper {
    public static final String CHANNEL_ID = "lesson_reminders";
    public static final String CHANNEL_NAME = "תזכורות לשיעורים";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("התראות עבור שיעורים קרובים");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    public static void scheduleLessonReminder(Context context, String dateStr, String timeStr, String partnerName) {
        try {
            String[] dateParts = dateStr.split("-");
            String[] timeParts = timeStr.split(":");
            
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
            calendar.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            calendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            calendar.set(Calendar.SECOND, 0);

            long triggerAtMillis = calendar.getTimeInMillis() - (60 * 60 * 1000); // שעה לפני

            if (triggerAtMillis <= System.currentTimeMillis()) return;

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    context.startActivity(intent);
                    return;
                }
            }

            Intent intent = new Intent(context, LessonReminderReceiver.class);
            intent.putExtra("partnerName", partnerName);
            intent.putExtra("lessonTime", timeStr);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (dateStr + timeStr).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Log.d("NotificationHelper", "Scheduled reminder for " + partnerName + " at " + triggerAtMillis);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
