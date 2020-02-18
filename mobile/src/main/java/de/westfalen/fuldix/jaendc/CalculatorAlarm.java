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
import android.graphics.BitmapFactory;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import de.westfalen.fuldix.jaendc.text.CountdownTextTimeFormat;

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
                if (timeout > System.currentTimeMillis()) {
                    final Intent cIntent = new Intent(context, NDCalculatorActivity.class);
                    final PendingIntent contentIntent = PendingIntent.getActivity(context, 1, cIntent, 0);
                    final Notification ongoingNotification = makeNotification(context, contentIntent, null, AudioManager.RINGER_MODE_SILENT, TIMER_COUNTING, this);
                    final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if(manager != null) {
                        manager.notify(TIMER_COUNTING, ongoingNotification);
                    } else {
                        System.err.println("Warning: NotificationManager is null");
                    }
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

    private static final String[] channelIds = { ""
                                                , "timerExpiryChannel.V2"
                                                , "timerRunningChannel.V2" };
    private static final Set<String> channelIdsSet = new HashSet<>(Arrays.asList(channelIds));
    public static final int TIMER_COMPLETED = 1;
    public static final int TIMER_COUNTING = 2;

    public static PendingIntent schedule(final Context context, final long timeout) {
        cancel(context);
        NotificationCanceler.cancelNotification(context);
        final PendingIntent pi = mkPendingIntent(context);
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(am != null) {
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
                if (useAlarmClockValue) {
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
        } else {
            System.err.println("Warning: AlarmManager is null");
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putLong(Calculator.PERS_TIMER_ENDING, timeout).commit();
        final ScheduledAlarmNotification scheduledAlarmNotification = new ScheduledAlarmNotification(timeout, context, pi);
        scheduledAlarmNotification.run();
        return pi;
    }

    public static void cancel(final Context context) {
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(am != null) {
            am.cancel(mkPendingIntent(context));
        } else {
            System.err.println("Warning: AlarmManager is null");
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(Calculator.PERS_TIMER_ENDING).commit();
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager != null) {
            manager.cancel(TIMER_COUNTING);
        } else {
            System.err.println("Warning: NotificationManager is null");
        }
    }

    private static PendingIntent mkPendingIntent(final Context context) {
        final Intent intent = new Intent(context, CalculatorAlarm.class);
        return PendingIntent.getBroadcast(context, 1000, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        System.out.println("Exposure timer alarm received");
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager != null) {
            manager.cancel(TIMER_COUNTING);
        } else {
            System.err.println("Warning: NotificationManager is null");
        }
        final int ringerMode;
        final AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if(audio != null) {
            ringerMode = audio.getRingerMode();
        } else {
            System.err.println("Warning: AudioManager is null");
            ringerMode = AudioManager.RINGER_MODE_SILENT;
        }
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final Uri sound = ConfigActivity.getConfiguredAlarmTone(context, prefs.getString(ConfigActivity.ALARM_TONE, ConfigActivity.ALARM_TONE_USE_SYSTEM_SOUND));
        if(NDCalculatorActivity.isShowing) {
            if(ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                try {
                    final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if(vibrator != null) {
                        if (Build.VERSION.SDK_INT >= 26) {
                            vibrator.vibrate(VibrationEffect.createOneShot(vibrateLength, DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(vibrateLength);
                        }
                    } else {
                        System.err.println("Warning: Vibrator is null");
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
                        r.setAudioAttributes(mkAudioAttributes());
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
                if(manager != null) {
                    manager.notify(TIMER_COMPLETED, notification);
                } else {
                    System.err.println("Warning: NotificationManager is null");
                }
            } catch (final SecurityException e) {   // if no VIBRATE permission
                System.err.println("Warning: " + e);
            } finally {
                NotificationCanceler.schedule(context);
            }
        }
    }


    private static Notification makeNotification(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        if (Build.VERSION.SDK_INT >= 26) {
            return makeNotification_26(context, contentIntent, sound, ringerMode, whichNotification, scheduledAlarmNotification);
        } else if (Build.VERSION.SDK_INT >= 24) {
            return makeNotification_24(context, contentIntent, sound, ringerMode, whichNotification, scheduledAlarmNotification);
        } else if (Build.VERSION.SDK_INT >= 21) {
            return makeNotification_21(context, contentIntent, sound, ringerMode, whichNotification, scheduledAlarmNotification);
        } else if (Build.VERSION.SDK_INT >= 16) {
            return makeNotification_16(context, contentIntent, sound, ringerMode, whichNotification, scheduledAlarmNotification);
        } else if (Build.VERSION.SDK_INT >= 11) {
            return makeNotification_11(context, contentIntent, sound, ringerMode, whichNotification, scheduledAlarmNotification);
        } else {
            return makeNotification_legacy(context, contentIntent, sound, ringerMode, whichNotification, scheduledAlarmNotification);
        }
    }

    private static Notification makeNotification_legacy(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
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

    @TargetApi(11)
    private static Notification makeNotification_11(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        final Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS);
        buildNotificationByType(builder, context, whichNotification, scheduledAlarmNotification);
        if (sound != null && ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            builder.setVibrate(vibratePattern);
        }
        if (sound != null && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            builder.setSound(sound, AudioManager.STREAM_ALARM);
        }
        return builder.getNotification();
    }

    @TargetApi(16)
    private static Notification makeNotification_16(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        final Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setPriority(getPriorityForType(whichNotification));
        buildNotificationByType(builder, context, whichNotification, scheduledAlarmNotification);
        if (sound != null && ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            builder.setVibrate(vibratePattern);
        }
        if (sound != null && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            builder.setSound(sound, AudioManager.STREAM_ALARM);
        }
        return builder.build();
    }

    @TargetApi(21)
    private static Notification makeNotification_21(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        final Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setPriority(getPriorityForType(whichNotification))
                .setCategory(getCategoryForType(whichNotification))
                .setLocalOnly(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        buildNotificationByType(builder, context, whichNotification, scheduledAlarmNotification);
        if (sound != null && ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            builder.setVibrate(vibratePattern);
        }
        if(ringerMode == AudioManager.RINGER_MODE_NORMAL && sound != null) {
            builder.setSound(sound, mkAudioAttributes());
        }
        return builder.build();
    }

    @TargetApi(21)
    private static AudioAttributes mkAudioAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build();
    }

    @TargetApi(24)
    private static Notification makeNotification_24(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        final Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)   // no large icon, because large icons should represent the notification, not the app: https://material.io/design/platform-guidance/android-notifications.html#style
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setPriority(getPriorityForType(whichNotification))
                .setCategory(getCategoryForType(whichNotification))
                .setLocalOnly(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        buildNotificationByType(builder, context, whichNotification, scheduledAlarmNotification);
        if (sound != null && ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            builder.setVibrate(vibratePattern);
        }
        if(ringerMode == AudioManager.RINGER_MODE_NORMAL && sound != null) {
            builder.setSound(sound, mkAudioAttributes());
        }
        return builder.build();
    }

    @TargetApi(26)
    private static Notification makeNotification_26(final Context context, final PendingIntent contentIntent, final Uri sound, final int ringerMode, final int whichNotification, final ScheduledAlarmNotification scheduledAlarmNotification) {
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final String channelId;
        if(manager != null) {
            switch (whichNotification) {
                case TIMER_COUNTING: {
                    // no cleanupNotificationChannels_26 because this channel id cannot change due to configuration
                    final NotificationChannel channel = ensureNotificationChannel_26(context, manager, channelIds[TIMER_COUNTING], NotificationManager.IMPORTANCE_LOW, null, null, R.string.notification_channel_name_timer, R.string.notification_channel_description_timer);
                    channelId = channel.getId();
                    break;
                }
                case TIMER_COMPLETED: {
                    cleanupNotificationChannels_26(manager, channelIds[TIMER_COMPLETED], sound, vibratePattern); // do this only on completed timer (otherwise may imact performance too much)
                    final NotificationChannel channel = ensureNotificationChannel_26(context, manager, channelIds[TIMER_COMPLETED], NotificationManager.IMPORTANCE_HIGH, sound, vibratePattern, R.string.notification_channel_name_expiry, R.string.notification_channel_description_expiry);
                    channelId = channel.getId();
                    break;
                }
                default: {
                    channelId = NotificationChannel.DEFAULT_CHANNEL_ID;
                    break;
                }
            }
        } else {
            System.err.println("Warning: NotificationManager is null");
            channelId = NotificationChannel.DEFAULT_CHANNEL_ID;
        }
        final Notification.Builder builder = new Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)   // no large icon, because large icons should represent the notification, not the app: https://material.io/design/platform-guidance/android-notifications.html#style
                .setContentTitle(context.getString(R.string.app_name))
                .setContentIntent(contentIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setCategory(getCategoryForType(whichNotification))
                .setLocalOnly(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        buildNotificationByType(builder, context, whichNotification, scheduledAlarmNotification);
        return builder.build();
    }

    @TargetApi(26)
    private static NotificationChannel ensureNotificationChannel_26(final Context context, final NotificationManager manager, final String baseChannelId, final int importance, final Uri sound, final long[] vibratePattern, final int channelNameResource, final int channelDescriptionResource) {
        final String actualChannelId = getActualChannelId_26(baseChannelId, sound, vibratePattern);
        final NotificationChannel channel;
        final String channelName = context.getString(channelNameResource);
        final NotificationChannel existingChannel = manager.getNotificationChannel(actualChannelId);
        if(existingChannel != null) {
            channel = existingChannel;
            channel.setName(channelName);
        } else {
            channel = new NotificationChannel(actualChannelId, channelName, importance);
            channel.setShowBadge(false);
            if(vibratePattern != null) {
                channel.setVibrationPattern(vibratePattern);
            }
            if(sound != null) {
                channel.setSound(sound, mkAudioAttributes());
            }
            manager.createNotificationChannel(channel);
        }
        channel.setDescription(context.getString(channelDescriptionResource));
        return channel;
    }

    @TargetApi(26)
    private static void cleanupNotificationChannels_26(final NotificationManager manager, final String baseChannelId, final Uri sound, final long[] vibratePattern) {
        final String actualChannelId = getActualChannelId_26(baseChannelId, sound, vibratePattern);
        final List<NotificationChannel> channels = manager.getNotificationChannels();
        for(final NotificationChannel channel : channels) {
            final String id = channel.getId();
            final StringTokenizer tok = new StringTokenizer(id, ":");
            if(tok.hasMoreTokens()) {
                final String base = tok.nextToken();
                if(tok.hasMoreTokens()) {
                    if(channelIdsSet.contains(base)) {
                        if(baseChannelId.equals(base)) {
                            final String vibrateIndicator = tok.nextToken();
                            if(("v".equals(vibrateIndicator) && vibratePattern != null)
                                ||("n".equals(vibrateIndicator) && vibratePattern == null))
                            {
                                if (!actualChannelId.equals(id)) {
                                    // base id ok but actual id does not exist anymore
                                    manager.deleteNotificationChannel(id);
                                }
                            } else {
                                // base id has the wrong vibrate indicator token
                                manager.deleteNotificationChannel(id);
                            }
                        }
                    } else {
                        // base id does not exist (channel ids from a previous app version?)
                        manager.deleteNotificationChannel(id);
                    }
                } else {
                    // base id has not vibrate indicator token (from a previous dev/beta version?)
                    manager.deleteNotificationChannel(id);
                }
            }
        }
    }

    @TargetApi(26)
    private static String getActualChannelId_26(final String baseChannelId, final Uri sound, final long[] vibratePattern) {
        if (sound != null) {
            if (vibratePattern != null) {
                return baseChannelId + ":v:" + sound;
            } else {
                return baseChannelId + ":n:" + sound;
            }
        } else {
            if (vibratePattern != null) {
                return baseChannelId + ":v:";
            } else {
                return baseChannelId + ":n:";
            }
        }
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

    @TargetApi(16)
    private static int getPriorityForType(final int whichNotification) {
        switch (whichNotification) {
            case TIMER_COMPLETED:
                return Notification.PRIORITY_HIGH;
            case TIMER_COUNTING:
                return Notification.PRIORITY_LOW;
            default:
                return Notification.PRIORITY_DEFAULT;
        }
    }
}
