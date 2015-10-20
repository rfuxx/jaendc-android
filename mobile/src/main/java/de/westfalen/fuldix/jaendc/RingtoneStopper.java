package de.westfalen.fuldix.jaendc;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.os.SystemClock;

public class RingtoneStopper extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        stopRingtone(context);
    }

    private static Ringtone ringtone;

    public static void stopRingtone(Context context) {
        if(ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }
        cancelSchedule(context);
    }

    public static void schedule(Context context, Ringtone r) {
        stopRingtone(context);
        ringtone = r;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long timeout = SystemClock.elapsedRealtime() + 30 * 1000;
        am.set(AlarmManager.ELAPSED_REALTIME, timeout, mkPendingIntent(context));
    }

    public static void cancelSchedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(mkPendingIntent(context));
    }

    private static PendingIntent mkPendingIntent(Context context) {
        Intent intent = new Intent(context, RingtoneStopper.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
