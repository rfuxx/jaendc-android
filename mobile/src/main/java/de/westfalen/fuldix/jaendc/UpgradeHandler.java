package de.westfalen.fuldix.jaendc;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class UpgradeHandler extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if(!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            removeId15Id16NotificationChannels(context);
        }
    }

    @TargetApi(26)
    private static void removeId15Id16NotificationChannels(final Context context) {
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager != null) {
            // clean up channels from previous version
            if (manager.getNotificationChannel("timerRunningChannel") != null) {
                manager.deleteNotificationChannel("timerRunningChannel");
            }
            if (manager.getNotificationChannel("timerExpiryChannel") != null) {
                manager.deleteNotificationChannel("timerExpiryChannel");
            }
            if (manager.getNotificationChannel("timerExpiryChannelWithBubble") != null) {
                manager.deleteNotificationChannel("timerExpiryChannelWithBubble");
            }
        }
    }
}
