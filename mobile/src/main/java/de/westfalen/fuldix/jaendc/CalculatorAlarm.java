package de.westfalen.fuldix.jaendc;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
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
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import de.westfalen.fuldix.jaendc.text.CountdownTextTimeFormat;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.os.VibrationEffect.DEFAULT_AMPLITUDE;

public class CalculatorAlarm extends BroadcastReceiver {
    private static class ScheduledAlarmNotification implements Runnable {
        final long timeout;
        final Context context;
        final PendingIntent pendingIntent;
        final CountdownTextTimeFormat countdownTextTimeFormat;
        private Handler handler;

        public ScheduledAlarmNotification(final long timeout, final Context context, final PendingIntent pendingIntent) {
            this.timeout = timeout;
            this.context = context;
            this.pendingIntent = pendingIntent;
            this.countdownTextTimeFormat = new CountdownTextTimeFormat(context);
        }

        public Handler getHandler() {
            if(handler == null) {
                handler = new Handler();
            }
            return handler;
        }

        @Override
        public void run() {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final long prefsTimeout = prefs.getLong(Calculator.PERS_TIMER_ENDING, 0);
            if(prefsTimeout != 0 && timeout == prefsTimeout) {
                final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (timeout > System.currentTimeMillis()) {
                    final Intent cIntent = new Intent(context, NDCalculatorActivity.class);
                    final PendingIntent contentIntent = PendingIntent.getActivity(context, 1, cIntent, 0);
                    final Notification ongoingNotification = makeNotification(context, contentIntent, null, AudioManager.RINGER_MODE_SILENT, TIMER_COUNTING, this);
                    manager.notify(TIMER_COUNTING, ongoingNotification);
                } else {
                    cancel(context);
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        System.err.println("PendingIntent of timer notification was already cancelled.");
                    }
                }
            }
        }
    }

    private static final long vibrateLength = 500;
    private static final long[] vibratePattern = { 0, vibrateLength };

    public static final int TIMER_COMPLETED = 1;
    public static final int TIMER_COUNTING = 2;

    public static PendingIntent schedule(final Context context, final long timeout) {
        cancel(context);
        NotificationCanceler.cancelNotification(context);
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pi = mkPendingIntent(context);
        if (Build.VERSION.SDK_INT >= 23) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean useAlarmClockValue = prefs.getBoolean(ConfigActivity.USE_ALARMCLOCK, false);
            if (useAlarmClockValue) {
                final PendingIntent showIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, NDCalculatorActivity.class), 0);
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(timeout, showIntent), pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeout, pi);  // Despite the name, this is not at all exact, especially not when idle
            }
        } else if (Build.VERSION.SDK_INT >= 21) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean useAlarmClockValue = prefs.getBoolean(ConfigActivity.USE_ALARMCLOCK, false);
            if(useAlarmClockValue) {
                final PendingIntent showIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, NDCalculatorActivity.class), 0);
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(timeout, showIntent), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, timeout, pi);
            }
        } else if (Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, timeout, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, timeout, pi);
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(Calculator.PERS_TIMER_ENDING, timeout).commit();
        final ScheduledAlarmNotification scheduledAlarmNotification = new ScheduledAlarmNotification(timeout, context, pi);
        scheduledAlarmNotification.run();
        return pi;
    }

    public static void cancel(final Context context) {
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(mkPendingIntent(context));
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(Calculator.PERS_TIMER_ENDING).commit();
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(TIMER_COUNTING);
    }

    private static PendingIntent mkPendingIntent(final Context context) {
        final Intent intent = new Intent(context, CalculatorAlarm.class);
        return PendingIntent.getBroadcast(context, 1000, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        System.out.println("Exposure timer alarm received");
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(TIMER_COUNTING);
        final AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int ringerMode = audio.getRingerMode();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final Uri sound = ConfigActivity.getConfiguredAlarmTone(context, prefs.getString(ConfigActivity.ALARM_TONE, ConfigActivity.ALARM_TONE_USE_SYSTEM_SOUND));
        if(NDCalculatorActivity.isShowing) {
            if(ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createOneShot(vibrateLength, DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(vibrateLength);
                    }
                } catch (SecurityException e) {   // if no VIBRATE permission
                    System.err.println("Warning: " + e);
                } finally {
                    NotificationCanceler.schedule(context);
                }
            }
            if(ringerMode == AudioManager.RINGER_MODE_NORMAL && sound != null) {
                final Ringtone r = RingtoneManager.getRingtone(context, sound);
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
            final Intent cIntent = new Intent(context, NDCalculatorActivity.class);
            final PendingIntent contentIntent = PendingIntent.getActivity(context, 1, cIntent, 0);
            final Notification notification = makeNotification(context, contentIntent, sound, ringerMode, TIMER_COMPLETED, null);

            try {
                manager.notify(TIMER_COMPLETED, notification);
            } catch (SecurityException e) {   // if no VIBRATE permission
                System.err.println("Warning: " + e);
            } finally {
                NotificationCanceler.schedule(context);
            }
        }
    }


    private static Notification makeNotification(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        if (Build.VERSION.SDK_INT >= 26) {
            return makeNotification_26(context, contentIntent, sound, ringerMode, whichNotification, scheduledAlarmNotification);
        } else if (Build.VERSION.SDK_INT >= 11) {
            return makeNotification_11(context, contentIntent, sound, ringerMode, whichNotification, scheduledAlarmNotification);
        } else {
            final Notification notification = new Notification();
            notification.defaults = Notification.DEFAULT_LIGHTS;
            notification.contentView = new RemoteViews(context.getPackageName(), R.layout.notification_exposure_time);
            switch (whichNotification) {
                case TIMER_COMPLETED: {
                    final String msgText = context.getString(R.string.notification_exposure_time);
                    notification.contentView.setTextViewText(R.id.notification_text, msgText);
                    notification.tickerText = msgText;
                    notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
                    break;
                }
                case TIMER_COUNTING: {
                    final String msgText;
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    final boolean showCountdown = prefs.getBoolean(ConfigActivity.SHOW_COUNTDOWN, false);
                    notification.flags = Notification.FLAG_ONGOING_EVENT;
                    if (showCountdown) {
                        final int remainingTime = (int) (scheduledAlarmNotification.timeout - System.currentTimeMillis());
                        msgText = context.getResources().getString(R.string.notification_timer_counting, scheduledAlarmNotification.countdownTextTimeFormat.format(remainingTime));
                        int delayToNext = scheduledAlarmNotification.countdownTextTimeFormat.delayToNext(remainingTime);
                        if (delayToNext >= 0) {
                            scheduledAlarmNotification.getHandler().postDelayed(scheduledAlarmNotification, delayToNext);
                        }
                    } else {
                        msgText = context.getString(R.string.notification_timer_running);
                    }
                    notification.contentView.setTextViewText(R.id.notification_text, msgText);
                    break;
                }
            }
            notification.icon = R.mipmap.ic_launcher;
            notification.contentIntent = contentIntent;
            if(sound != null && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                notification.sound = sound;
            }
            if(sound != null && ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                notification.vibrate = vibratePattern;
            }
            return notification;
        }
    }

    @TargetApi(11)
    private static Notification makeNotification_11(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        final Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS);
        buildNotificationByType(builder, context, whichNotification, scheduledAlarmNotification);
        if (sound != null && ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            builder.setVibrate(vibratePattern);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            return buildNotification_21(builder, sound, ringerMode, whichNotification);
        } else {
            if(sound != null && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                builder.setSound(sound, AudioManager.STREAM_ALARM);
            }
            if (Build.VERSION.SDK_INT >= 16) {
                return buildNotification_16(builder, whichNotification);
            } else {
                return builder.getNotification();
            }
        }
    }

    @TargetApi(16)
    private static Notification buildNotification_16(final Notification.Builder builder, final int whichNotification) {
        switch (whichNotification) {
            case TIMER_COMPLETED:
                builder.setPriority(Notification.PRIORITY_HIGH);
                break;
            case TIMER_COUNTING:
                builder.setPriority(Notification.PRIORITY_LOW);
                break;
        }
        return builder.build();
    }

    @TargetApi(21)
    private static Notification buildNotification_21(final Notification.Builder builder, final Uri sound, final int ringerMode, final int whichNotification) {
        if(ringerMode == AudioManager.RINGER_MODE_NORMAL && sound != null) {
            builder.setSound(sound, mkAudioAttributes());
        }
        return builder
                .setCategory(getCategoryForType(whichNotification))
                .setLocalOnly(true)
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

    @TargetApi(26)
    private static Notification makeNotification_26(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationChannel channel;
        final String channelId;
        switch (whichNotification) {
            case TIMER_COUNTING:
                channelId = "timerRunningChannel";
                channel = new NotificationChannel(channelId, context.getString(R.string.notification_channel_name_timer), NotificationManager.IMPORTANCE_LOW);
                break;
            default:
                channelId = "timerExpiryChannel";
                channel = new NotificationChannel(channelId, context.getString(R.string.notification_channel_name_expiry), IMPORTANCE_HIGH);
                if (sound != null && ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    channel.setVibrationPattern(vibratePattern);
                }
                if(sound != null && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    channel.setSound(sound, mkAudioAttributes());
                }
                break;
        }
        manager.createNotificationChannel(channel);
        final Notification.Builder builder = new Notification.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setCategory(getCategoryForType(whichNotification))
                .setLocalOnly(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        buildNotificationByType(builder, context, whichNotification, scheduledAlarmNotification);
        return builder.build();
    }

    @TargetApi(11)
    private static void buildNotificationByType(final Notification.Builder builder, final Context context, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        switch (whichNotification) {
            case TIMER_COMPLETED:
                builder.setOngoing(false)
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(false)
                        .setContentText(context.getString(R.string.notification_exposure_time))
                        .setTicker(context.getString(R.string.notification_exposure_time));
                break;
            case TIMER_COUNTING:
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                final boolean showCountdown = prefs.getBoolean(ConfigActivity.SHOW_COUNTDOWN, false);
                builder.setOngoing(true);
                if(showCountdown) {
                    final int remainingTime = (int) (scheduledAlarmNotification.timeout - System.currentTimeMillis());
                    builder.setContentText(context.getResources().getString(R.string.notification_timer_counting, scheduledAlarmNotification.countdownTextTimeFormat.format(remainingTime)));
                    int delayToNext = scheduledAlarmNotification.countdownTextTimeFormat.delayToNext(remainingTime);
                    if(delayToNext >= 0) {
                        scheduledAlarmNotification.getHandler().postDelayed(scheduledAlarmNotification, delayToNext);
                    }
                } else {
                    builder.setContentText(context.getString(R.string.notification_timer_running));
                }
                break;
        }
    }

    @TargetApi(21)
    private static String getCategoryForType(final int whichNotification) {
        switch (whichNotification) {
            case TIMER_COMPLETED:
                return Notification.CATEGORY_ALARM;
            case TIMER_COUNTING:
                return Notification.CATEGORY_PROGRESS;
            default:
                return null;
        }
    }
}
