package de.westfalen.fuldix.jaendc;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class NotificationCanceler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        cancelNotification(context);
    }

    public static void cancelNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(1);
        cancelSchedule(context);
    }

    public static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long timeout = SystemClock.elapsedRealtime() + 5 * 60 * 1000;
        am.set(AlarmManager.ELAPSED_REALTIME, timeout, mkPendingIntent(context));
    }

    private static void cancelSchedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(mkPendingIntent(context));
    }

    private static PendingIntent mkPendingIntent(Context context) {
        Intent intent = new Intent(context, NotificationCanceler.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
