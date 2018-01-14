package de.westfalen.fuldix.jaendc;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class CalculatorAlarm extends BroadcastReceiver {
    private static final long[] vibratePattern = { 3000, 500, 3000 };

    public static void schedule(Context context, long timeout) {
        NotificationCanceler.cancelNotification(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = mkPendingIntent(context);
        if (Build.VERSION.SDK_INT >= 19) {
            setExactAlarm_19(am, timeout, pi);
        } else {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeout, pi);
        }
    }

    public static void cancel(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(mkPendingIntent(context));
    }

    private static PendingIntent mkPendingIntent(Context context) {
        Intent intent = new Intent(context, CalculatorAlarm.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @TargetApi(19)
    private static void setExactAlarm_19(AlarmManager am, long triggerAtMillis, PendingIntent operation) {
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, operation);
    }

    private int ringerMode;

    @Override
    public void onReceive(Context context, Intent intent) {
        final AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        ringerMode = audio.getRingerMode();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final Uri sound = ConfigActivity.getConfiguredAlarmTone(context, prefs.getString(ConfigActivity.ALARM_TONE, ConfigActivity.ALARM_TONE_USE_SYSTEM_SOUND));
        if(NDCalculatorActivity.isShowing) {
            if(ringerMode == AudioManager.RINGER_MODE_VIBRATE && sound != null) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                try {
                    vibrator.vibrate(vibratePattern, -1);
                } catch (SecurityException e) {   // if no VIBRATE permission
                    System.err.println("Warning: " + e);
                } finally {
                    NotificationCanceler.schedule(context);
                }
            }
            if(ringerMode == AudioManager.RINGER_MODE_NORMAL && sound != null) {
                Ringtone r = RingtoneManager.getRingtone(context, sound);
                if(r != null) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        ringtoneSetAudioAttributes(r);
                    } else {
                        r.setStreamType(AudioManager.STREAM_ALARM);
                    }
                    r.play();
                    RingtoneStopper.schedule(context, r);
                }
            }
        } else {
            Intent cIntent = new Intent(context, NDCalculatorActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 1, cIntent, 0);
            Notification notification;
            if (Build.VERSION.SDK_INT >= 11) {
                notification = makeNotification_11(context, contentIntent, sound);
            } else {
                notification = new Notification();
                notification.defaults = Notification.DEFAULT_LIGHTS;
                notification.tickerText = context.getString(R.string.notification_exposure_time);
                notification.icon = R.mipmap.ic_launcher;
                notification.contentView = new RemoteViews(context.getPackageName(), R.layout.notification_exposure_time);
                notification.contentIntent = contentIntent;
                notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
                if(ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    notification.sound = sound;
                }
                if(ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    notification.vibrate = vibratePattern;
                }
            }

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            try {
                manager.notify(1, notification);
            } catch (SecurityException e) {   // if no VIBRATE permission
                System.err.println("Warning: " + e);
            } finally {
                NotificationCanceler.schedule(context);
            }
        }
    }

    @TargetApi(11)
    private Notification makeNotification_11(Context context, PendingIntent contentIntent, Uri sound) {
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notification_exposure_time))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setOngoing(false)
                .setOnlyAlertOnce(false)
                .setTicker(context.getString(R.string.notification_exposure_time));
        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            builder.setVibrate(vibratePattern);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            return buildNotification_21(builder, sound);
        } else {
            if(ringerMode == AudioManager.RINGER_MODE_NORMAL && sound != null) {
                builder.setSound(sound, AudioManager.STREAM_ALARM);
            }
            if (Build.VERSION.SDK_INT >= 16) {
                return buildNotification_16(builder);
            } else {
                return builder.getNotification();
            }
        }
    }

    @TargetApi(16)
    private static Notification buildNotification_16(Notification.Builder builder) {
        return builder
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
    }

    @TargetApi(21)
    private Notification buildNotification_21(Notification.Builder builder, Uri sound) {
        if(ringerMode == AudioManager.RINGER_MODE_NORMAL && sound != null) {
            builder.setSound(sound, mkAudioAttributes());
        }
        return builder
                .setCategory(Notification.CATEGORY_ALARM)
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
    }

    @TargetApi(21)
    private static AudioAttributes mkAudioAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build();
    }

    @TargetApi(21)
    private static void ringtoneSetAudioAttributes(final Ringtone r) {
        r.setAudioAttributes(mkAudioAttributes());
    }
}
