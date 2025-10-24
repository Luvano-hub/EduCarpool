package com.educarpool;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AgreementReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "AgreementReminder";
    private static final String CHANNEL_ID = "agreement_reminders";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        String agreementId = intent.getStringExtra("agreement_id");
        String matchId = intent.getStringExtra("match_id");
        String reminderType = intent.getStringExtra("reminder_type");
        String pickupTime = intent.getStringExtra("pickup_time");

        Log.d(TAG, "Agreement reminder triggered: " + reminderType + " for agreement: " + agreementId);

        // Create and show notification
        showReminderNotification(context, reminderType, pickupTime);

        // Also send a chat message reminder
        sendChatReminder(context, matchId, reminderType);
    }

    private void showReminderNotification(Context context, String reminderType, String pickupTime) {
        createNotificationChannel(context);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String title = "Carpool Reminder";
        String message = "You have a carpool coming up!";

        switch (reminderType) {
            case "pickup_reminder":
                title = "🚗 Carpool Pickup Soon!";
                message = "Your carpool pickup is in 1 hour at " + (pickupTime != null ? pickupTime : "scheduled time");
                break;
            case "agreement_pending":
                title = "⏳ Pending Agreement";
                message = "You have a trip agreement waiting for your response";
                break;
            case "weekly_reminder":
                title = "📅 Weekly Carpool";
                message = "Don't forget your scheduled trips this week!";
                break;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Agreement Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for carpool agreements and reminders");

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendChatReminder(Context context, String matchId, String reminderType) {
        // This would send a reminder message to the chat
        // For now, we'll just log it
        Log.d(TAG, "Would send chat reminder for match: " + matchId + ", type: " + reminderType);
    }

    public static void scheduleAgreementReminder(Context context, Agreement agreement) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (agreement.getPickupTime() != null && !agreement.getPickupTime().isEmpty()) {
            schedulePickupReminder(context, alarmManager, agreement);
        }
    }

    private static void schedulePickupReminder(Context context, AlarmManager alarmManager, Agreement agreement) {
        try {
            String[] timeParts = agreement.getPickupTime().split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Create calendar instance for the pickup time
            java.util.Calendar reminderTime = java.util.Calendar.getInstance();
            reminderTime.set(java.util.Calendar.HOUR_OF_DAY, hour);
            reminderTime.set(java.util.Calendar.MINUTE, minute);
            reminderTime.set(java.util.Calendar.SECOND, 0);

            // Set reminder for 1 hour before pickup
            reminderTime.add(java.util.Calendar.HOUR_OF_DAY, -1);

            // If the time has already passed today, schedule for tomorrow
            if (reminderTime.before(java.util.Calendar.getInstance())) {
                reminderTime.add(java.util.Calendar.DAY_OF_MONTH, 1);
            }

            Intent intent = new Intent(context, AgreementReminderReceiver.class);
            intent.putExtra("agreement_id", agreement.getAgreementId());
            intent.putExtra("match_id", agreement.getMatchId());
            intent.putExtra("reminder_type", "pickup_reminder");
            intent.putExtra("pickup_time", agreement.getPickupTime());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    agreement.getAgreementId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), pendingIntent);
            Log.d(TAG, "Scheduled pickup reminder for agreement: " + agreement.getAgreementId() + " at " + reminderTime.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling pickup reminder: " + e.getMessage());
        }
    }

    public static void cancelAgreementReminders(Context context, String agreementId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AgreementReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                agreementId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Cancelled reminders for agreement: " + agreementId);
    }
}