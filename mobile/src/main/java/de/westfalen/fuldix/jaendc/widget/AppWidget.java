package de.westfalen.fuldix.jaendc.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.westfalen.fuldix.jaendc.ConfigActivity;
import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.db.NDFilterDAO;
import de.westfalen.fuldix.jaendc.model.NDFilter;
import de.westfalen.fuldix.jaendc.model.Time;
import de.westfalen.fuldix.jaendc.text.ClearTextTimeFormat;
import de.westfalen.fuldix.jaendc.text.CountdownTextTimeFormat;
import de.westfalen.fuldix.jaendc.text.OutputTimeFormat;

@TargetApi(11)
public class AppWidget extends AppWidgetProvider{
    private static final String WIDGET_TIME_LIST = "de.westfalen.fuldix.jaendc.time_list";
    private static final String WIDGET_FILTER_LIST = "de.westfalen.fuldix.jaendc.filter_list";
    private static final String WIDGET_START_BUTTON = "de.westfalen.fuldix.jaendc.start_button";
    private static final String WIDGET_STOP_BUTTON = "de.westfalen.fuldix.jaendc.stop_button";
    private static final String WIDGET_CONFIG_BUTTON = "de.westfalen.fuldix.jaendc.config_button";
    public static final String WIDGET_CONFIG_WAS_MODIFIED = "de.westfalen.fuldix.jaendc.config_result";
    private static final String TIME_SELECTION = "time";
    private static final String FILTER_SELECTION = "filter";

    private static int[] LAYOUT = { R.layout.app_widget_theme_dark,
                                    R.layout.app_widget_theme_light,
                                    R.layout.app_widget_theme_nightmode };

    private static class NDCalcData {
        double time;
        NDFilter filter;
        int filterOrderpos = -1;
        double calculatedTime;
        long timerEnding;
        int showTimer = 4;
        String alarmToneStr = ConfigActivity.ALARM_TONE_BE_SILENT;
        int alarmDuration = 29;
        int transparency = 33;
        Handler myHandler;
        Runnable timerRunner;
        Ringtone ringtonePlaying;
        Runnable ringtoneStopperRunner;
        int timeStyle;
        int theme;
        boolean showCountdown;
        NumberFormat clearTextTimeFormat;
        NumberFormat outputTimeFormat;
        CountdownTextTimeFormat countdownTextFormat;
    }

    private static class TimerRunner implements Runnable {
        private final Context context;
        private final int appWidgetId;
        TimerRunner(final Context context, final int appWidgetId) {
            this.appWidgetId = appWidgetId;
            this.context = context;
        }
        @Override
        public void run() {
            final NDCalcData data = getWidgetData(context, appWidgetId);
            final long current = SystemClock.elapsedRealtime();
            final int remaining = (int) (data.timerEnding - current);
            if (remaining > 0) {
                final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                final RemoteViews remoteViews = createRemoteViews(context, data);
                final int ndtimeMillis = (int) (data.calculatedTime *1000);
                setTimerVisibility(context, remoteViews, data);
                remoteViews.setBoolean(R.id.timeList, "setEnabled", false);
                remoteViews.setBoolean(R.id.filterList, "setEnabled", false);
                // the above "just in case" to ensure all "partial" updates since the last "full" update get applied again
                // strictly need onthe the below
                if(data.showCountdown) {
                    remoteViews.setTextViewText(R.id.smallTime, data.countdownTextFormat.format(remaining));
                }
                remoteViews.setProgressBar(R.id.progressBar, ndtimeMillis, remaining, false);
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
                final int width = 600; // should actually be something like progressBar.getWidth(); -- if only we could query the remoteview about its size...
                int delayToNext;
                if (width > 0) {
                    delayToNext = ndtimeMillis / width; // mdTimeMillis/width is the time until next progress bar pixel might hit (avoid unneccesary updates)
                    if (delayToNext < 20) {
                        delayToNext = 20;
                    }
                } else {
                    delayToNext = remaining;
                }
                data.myHandler.postDelayed(this, delayToNext);
            } else {
                stopTimer(context, appWidgetId);
                final Uri alarmTone = ConfigActivity.getConfiguredAlarmTone(context, data.alarmToneStr);
                if(alarmTone != null) {
                    final AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if(audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                        data.ringtonePlaying = RingtoneManager.getRingtone(context, alarmTone);
                        if(data.ringtonePlaying != null) {
                            data.ringtonePlaying.play();
                            data.ringtoneStopperRunner = new RingtoneStopperRunner(context, appWidgetId);
                            data.myHandler.postDelayed(data.ringtoneStopperRunner, (data.alarmDuration + 1) * 1000);
                        }
                    }
                }
            }
        }
    }

    private static class RingtoneStopperRunner implements Runnable {
        private final Context context;
        private final int appWidgetId;
        RingtoneStopperRunner(final Context context ,final int appWidgetId) {
            this.context = context;
            this.appWidgetId = appWidgetId;
        }
        @Override
        public void run() {
            NDCalcData data = getWidgetData(context, appWidgetId);
            if (data.ringtonePlaying != null) {
                data.ringtonePlaying.stop();
                data.ringtonePlaying = null;
                data.ringtoneStopperRunner = null;
            }
        }
    }

    private static final Map<Integer, NDCalcData> widgetData = new HashMap<>();

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds){
        // There may be multiple widgets active, so update all of them
        for(final int appWidgetId:appWidgetIds){
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(final Context context, final int[] appWidgetIds) {
        for(final int id : appWidgetIds) {
            final NDCalcData data = getWidgetData(context, id);
            stopRingtoneNow(data);
            widgetData.remove(id);
            final String prefPrefix = getPrefPrefix(id);
            final SharedPreferences.Editor prefsEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            prefsEdit.remove(prefPrefix + TIME_SELECTION);
            prefsEdit.remove(prefPrefix + FILTER_SELECTION);
            prefsEdit.remove(prefPrefix + ConfigActivity.TIME_STYLE);
            prefsEdit.remove(prefPrefix + ConfigActivity.SHOW_TIMER);
            prefsEdit.remove(prefPrefix + ConfigActivity.ALARM_TONE);
            prefsEdit.remove(prefPrefix + ConfigActivity.ALARM_DURATION);
            prefsEdit.remove(prefPrefix + ConfigActivity.TRANSPARENCY);
            prefsEdit.remove(prefPrefix + ConfigActivity.THEME);
            prefsEdit.remove(prefPrefix + ConfigActivity.SHOW_COUNTDOWN);
            prefsEdit.commit();
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(final Context context){
        // Enter relevant functionality for when the last widget is disabled
        widgetData.clear();
        super.onDisabled(context);
    }

    // no need for onAppWidgetOptionsChanged()
    // because we handle widget configuration not via AppWidgetOptions
    // but via SharedPreferences (persistent!!)
    // and with our own custom message between the ConfigActivity and the AppWidget
/*
    @Override
    public void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        // the changed newOptions() data is already handled in updateAppWidget()
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }
*/

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final ComponentName name = new ComponentName(context, AppWidget.class);
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(name);
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if(appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        final NDCalcData data = getWidgetData(context, appWidgetId);
        stopRingtoneNow(data);
        switch(intent.getAction()) {
            case WIDGET_TIME_LIST:
                final double time = intent.getDoubleExtra("SELECTED_TIME", 0);
                setTime(context, appWidgetId, time);
                setCalculation(context, appWidgetId);
                break;
            case WIDGET_FILTER_LIST:
                final Object object = intent.getParcelableExtra("SELECTED_FILTER");
                if(object != null && object instanceof NDFilter) {
                    final NDFilter filter = (NDFilter) object;
                    setFilter(context, appWidgetId, filter);
                    setCalculation(context, appWidgetId);
                }
                break;
            case WIDGET_START_BUTTON:
                startTimer(context, appWidgetId);
                break;
            case WIDGET_STOP_BUTTON:
                stopTimer(context, appWidgetId);
                break;
            case WIDGET_CONFIG_BUTTON:
                final Intent configIntent = new Intent(context, ConfigActivity.class);
                configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                configIntent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                context.startActivity(configIntent);
                break;
            case WIDGET_CONFIG_WAS_MODIFIED: {
                final String prefPrefix = getPrefPrefix(appWidgetId);
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                data.showTimer = prefs.getInt(prefPrefix + ConfigActivity.SHOW_TIMER, data.showTimer);
                data.alarmToneStr = prefs.getString(prefPrefix + ConfigActivity.ALARM_TONE, data.alarmToneStr);
                data.alarmDuration = prefs.getInt(prefPrefix + ConfigActivity.ALARM_DURATION, data.alarmDuration);
                data.transparency = prefs.getInt(prefPrefix + ConfigActivity.TRANSPARENCY, data.transparency);
                data.theme = prefs.getInt(prefPrefix + ConfigActivity.THEME, data.theme);
                data.showCountdown = prefs.getBoolean(prefPrefix + ConfigActivity.SHOW_COUNTDOWN, data.showCountdown);
                updateAppWidget(context, appWidgetManager, appWidgetId);
                break;
            }
        }
    }

    private static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId){
        if (android.os.Build.VERSION.SDK_INT < 11) {
            return;
        }
        final NDCalcData data = getWidgetData(context, appWidgetId);
        final String prefPrefix = getPrefPrefix(appWidgetId);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        data.timeStyle = prefs.getInt(prefPrefix + ConfigActivity.TIME_STYLE, 0);
        data.clearTextTimeFormat = new ClearTextTimeFormat(context, data.timeStyle);
        data.outputTimeFormat = new OutputTimeFormat(context, data.timeStyle);

        final RemoteViews remoteViews = createRemoteViews(context, data);

        final Resources.Theme theme = context.getResources().newTheme();
        theme.applyStyle(ConfigActivity.THEMES[data.theme], true);
        final TypedArray styled = theme.obtainStyledAttributes(new int[]{android.R.attr.colorBackground});
        int bgColor = styled.getColor(0, 0x000000);
        styled.recycle();

        final int transpValue = 255 - getWidgetData(context, appWidgetId).transparency * 255 / 100;
        remoteViews.setInt(R.id.widget, "setBackgroundColor", (transpValue & 0xff) << 24 | (bgColor & 0xffffff));

        setupListAdapterAndIntent(context, appWidgetId, remoteViews, R.id.timeList, TimeListService.class, WIDGET_TIME_LIST);
        setupListAdapterAndIntent(context, appWidgetId, remoteViews, R.id.filterList, NDFilterListService.class, WIDGET_FILTER_LIST);

        setupButtonIntent(context, appWidgetId, remoteViews, R.id.startButton, WIDGET_START_BUTTON);
        setupButtonIntent(context, appWidgetId, remoteViews, R.id.stopButton, WIDGET_STOP_BUTTON);
        setupButtonIntent(context, appWidgetId, remoteViews, R.id.configButton, WIDGET_CONFIG_BUTTON);

        // takes care also of adjusting/sanitizing the timer button/progressbar states
        showCalculation(context, appWidgetId, remoteViews, data);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        data.myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final RemoteViews remoteViews = createRemoteViews(context, data);
                // try to restore list indexes -- can only scroll to them but not set them as checked?!?
                int restoreTimeIndex = Arrays.binarySearch(Time.times[data.timeStyle], data.time);
                if (restoreTimeIndex < 0) {
                    restoreTimeIndex = 16;
                }
                remoteViews.setScrollPosition(R.id.timeList, restoreTimeIndex);
                final int restoreFilterIndex;
                if (data.filter != null) {
                    restoreFilterIndex = data.filter.getOrderpos();
                    remoteViews.setScrollPosition(R.id.filterList, restoreFilterIndex);
                }
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
            }
        }, 1000);
    }

    private static void setupListAdapterAndIntent(final Context context, final int appWidgetId, final RemoteViews remoteViews, final int remoteViewId, final Class remoteViewsService, final String intentAction) {
        final Intent svcIntent = new Intent(context, remoteViewsService);
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
        if (Build.VERSION.SDK_INT >= 14) {
            remoteViews.setRemoteAdapter(remoteViewId, svcIntent);
        } else {
            remoteViews.setRemoteAdapter(appWidgetId, remoteViewId, svcIntent);
        }

        final Intent listItemClickIntent = new Intent(context, AppWidget.class);
        listItemClickIntent.setAction(intentAction);
        listItemClickIntent.setData(Uri.parse(listItemClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 100, listItemClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(remoteViewId, clickPendingIntent);
    }

    private static void setupButtonIntent(final Context context, final int appWidgetId, final RemoteViews remoteViews, final int remoteViewId, final String intentAction) {
        final Intent buttonClickIntent = new Intent(context, AppWidget.class);
        buttonClickIntent.setAction(intentAction);
        buttonClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        buttonClickIntent.setData(Uri.parse(buttonClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 101, buttonClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(remoteViewId, clickPendingIntent);
    }

    private static NDCalcData getWidgetData(final Context context, final int appWidgetId) {
        NDCalcData data = widgetData.get(appWidgetId);
        if(data == null) {
            data = new NDCalcData();
            data.myHandler = new Handler();
            widgetData.put(appWidgetId, data);
            final String prefPrefix = getPrefPrefix(appWidgetId);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int oldTimeIndex = Arrays.binarySearch(Time.times[data.timeStyle], data.time);
            int newTimeIndex = prefs.getInt(prefPrefix + TIME_SELECTION, oldTimeIndex);
            data.time = newTimeIndex >= 0 && newTimeIndex < Time.times[data.timeStyle].length ? Time.times[data.timeStyle][newTimeIndex] : 0.0;
            long filterId = prefs.getLong(prefPrefix + FILTER_SELECTION, data.filter != null ? data.filter.getId() : Long.MIN_VALUE);
            NDFilterDAO dao = new NDFilterDAO(context);
            data.filter = dao.getNDFilter(filterId);
            data.filterOrderpos = data.filter.getOrderpos();
            setCalculationOnly(data);
            data.showTimer = prefs.getInt(prefPrefix + ConfigActivity.SHOW_TIMER, data.showTimer);
            data.alarmToneStr = prefs.getString(prefPrefix + ConfigActivity.ALARM_TONE, data.alarmToneStr);
            data.alarmDuration = prefs.getInt(prefPrefix + ConfigActivity.ALARM_DURATION, data.alarmDuration);
            data.transparency = prefs.getInt(prefPrefix + ConfigActivity.TRANSPARENCY, data.transparency);
            data.timeStyle = prefs.getInt(prefPrefix + ConfigActivity.TIME_STYLE, data.timeStyle);
            data.theme = prefs.getInt(prefPrefix + ConfigActivity.THEME, data.theme);
            data.showCountdown = prefs.getBoolean(prefPrefix + ConfigActivity.SHOW_COUNTDOWN, data.showCountdown);
            data.clearTextTimeFormat = new ClearTextTimeFormat(context, data.timeStyle);
            data.outputTimeFormat = new OutputTimeFormat(context, data.timeStyle);
            data.countdownTextFormat = new CountdownTextTimeFormat(context);
        }
        return data;
    }

    private static void setTime(final Context context, final int appWidgetId, final double time) {
        final NDCalcData data = getWidgetData(context, appWidgetId);
        data.time = time;
        final String prefPrefix = getPrefPrefix(appWidgetId);
        final SharedPreferences.Editor prefsEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        int timeIndex = Arrays.binarySearch(Time.times[data.timeStyle], data.time);
        prefsEdit.putInt(prefPrefix + TIME_SELECTION, timeIndex);
        prefsEdit.commit();
    }

    private static void setFilter(final Context context, final int appWidgetId, final NDFilter filter) {
        final NDCalcData data = getWidgetData(context, appWidgetId);
        data.filter = filter;
        data.filterOrderpos = filter != null ? filter.getOrderpos() : -1;
        final String prefPrefix = getPrefPrefix(appWidgetId);
        final SharedPreferences.Editor prefsEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        prefsEdit.putLong(prefPrefix + FILTER_SELECTION, filter != null ? filter.getId() : Long.MIN_VALUE);
        prefsEdit.commit();
    }

    private static void setCalculationOnly(final NDCalcData data) {
        data.calculatedTime = data.time;
        if (data.filter != null) {
            data.calculatedTime *= data.filter.getFactor();
        }
        if(data.showTimer > 0 && data.calculatedTime >= data.showTimer) {
            data.calculatedTime = Time.roundTimeToCameraTime(data.calculatedTime, Time.times[data.timeStyle][Time.times[data.timeStyle].length-1], data.timeStyle);
        }
    }

    private static void setCalculation(final Context context, final int appWidgetId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final NDCalcData data = getWidgetData(context, appWidgetId);
        final RemoteViews remoteViews = createRemoteViews(context, data);
        setCalculationOnly(data);
        showCalculation(context, appWidgetId, remoteViews, data);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
     }

    private static void showCalculation(final Context context, final int appWidgetId, final RemoteViews remoteViews, final NDCalcData data) {
        final CharSequence largeTimeText;
        final CharSequence smallTimeText;
        if(data.calculatedTime < Integer.MAX_VALUE / 1000) {
            if (data.calculatedTime > 0.0) {
                if(data.calculatedTime > Time.times[data.timeStyle][Time.times[data.timeStyle].length-1]) {
                    final String normalText = data.outputTimeFormat.format(data.calculatedTime);
                    final int pos = normalText.length();
                    final SpannableString spanString = new SpannableString(normalText + "BULB");
                    spanString.setSpan(new RelativeSizeSpan(0.333f), pos, pos+4, 0);
                    largeTimeText = spanString;
                } else {
                    largeTimeText = data.outputTimeFormat.format(data.calculatedTime);
                }
                if (data.showTimer > 0 && data.calculatedTime >= data.showTimer) {
                    // we need to handle here also the rebuilding of start/top/bar if the timer is running or not (if updating from externally)
                    if(data.timerEnding > 0 && data.timerRunner != null) {
                        setTimerVisibility(context, remoteViews, data);
                        if(data.showCountdown) {
                            final long current = SystemClock.elapsedRealtime();
                            final int remaining = (int) (data.timerEnding - current);
                            smallTimeText = data.countdownTextFormat.format(remaining);
                        } else {
                            smallTimeText = data.clearTextTimeFormat.format(data.calculatedTime);
                        }
                    } else {
                        setTimerVisibility(context, remoteViews, data);
                        smallTimeText = data.clearTextTimeFormat.format(data.calculatedTime);
                    }
                    data.myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // try to scroll list items back into view -- due to making the timer visible, they may have disappeared from view
                            final RemoteViews remoteViews = createRemoteViews(context, data);
                            final int restoreTimeIndex = Arrays.binarySearch(Time.times[data.timeStyle], data.time);
                            if (restoreTimeIndex >= 0) {
                                remoteViews.setScrollPosition(R.id.timeList, restoreTimeIndex);
                            }
                            if (data.filter != null) {
                                remoteViews.setScrollPosition(R.id.filterList, data.filter.getOrderpos());
                            }
                            AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(appWidgetId, remoteViews);
                        }
                    }, 100);
                } else {
                    // we need to handle here also the rebuilding of start/top/bar
                    // (if updating from externally and settings may have been changed)
                    setTimerVisibility(context, remoteViews, data);
                    remoteViews.setBoolean(R.id.timeList, "setEnabled", true);
                    remoteViews.setBoolean(R.id.filterList, "setEnabled", true);
                    remoteViews.setProgressBar(R.id.progressBar, (int) (data.calculatedTime * 1000), 0, false);
                    if(data.timerEnding > 0 && data.timerRunner != null) {
                        data.myHandler.removeCallbacks(data.timerRunner);
                        data.timerEnding = 0;
                    }
                    smallTimeText = data.clearTextTimeFormat.format(data.calculatedTime);
                }
            } else {
                largeTimeText = context.getString(R.string.text_na);
                smallTimeText = context.getString(R.string.text_na);
                setTimerVisibility(context, remoteViews, data);
            }
        } else {
            final String normalLargeText = String.format(context.getString(R.string.text_longer_than_symbol), data.outputTimeFormat.format(Integer.MAX_VALUE / 1000));
            final SpannableString spanString = new SpannableString(normalLargeText);
            spanString.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.ResultTextMaxExceededColor)), 0, normalLargeText.length(), 0);
            largeTimeText = spanString;
            smallTimeText = String.format(context.getString(R.string.text_longer_than), data.clearTextTimeFormat.format(Integer.MAX_VALUE / 1000));
            setTimerVisibility(context, remoteViews, data);
        }
        remoteViews.setTextViewText(R.id.largeTime, largeTimeText);
        remoteViews.setTextViewText(R.id.smallTime, smallTimeText);
    }

    private static void startTimer(final Context context, final int appWidgetId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final NDCalcData data = getWidgetData(context, appWidgetId);
        final RemoteViews remoteViews = createRemoteViews(context, data);
        final int ndtimeMillis = (int) (data.calculatedTime *1000);
        data.timerEnding = SystemClock.elapsedRealtime() + ndtimeMillis;
        if(data.timerRunner == null) {
            data.timerRunner = new TimerRunner(context, appWidgetId);
        }
        data.myHandler.post(data.timerRunner);
        setTimerVisibility(context, remoteViews, data);
        remoteViews.setProgressBar(R.id.progressBar, ndtimeMillis, ndtimeMillis, false);
        remoteViews.setBoolean(R.id.timeList, "setEnabled", false);
        remoteViews.setBoolean(R.id.filterList, "setEnabled", false);
        if(data.showCountdown) {
            remoteViews.setTextViewText(R.id.smallTime, data.countdownTextFormat.format(ndtimeMillis));
        }
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
    }

    private static void stopTimer(final Context context, final int appWidgetId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final NDCalcData data = getWidgetData(context, appWidgetId);
        data.timerEnding = 0;
        data.myHandler.removeCallbacks(data.timerRunner);
        final RemoteViews remoteViews = createRemoteViews(context, data);
        setTimerVisibility(context, remoteViews, data);
        remoteViews.setProgressBar(R.id.progressBar, (int) (data.calculatedTime * 1000), 0, false);
        remoteViews.setBoolean(R.id.timeList, "setEnabled", true);
        remoteViews.setBoolean(R.id.filterList, "setEnabled", true);
        remoteViews.setTextViewText(R.id.smallTime, data.clearTextTimeFormat.format(data.calculatedTime));
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
    }

    private static void stopRingtoneNow(final NDCalcData data) {
        if(data.ringtonePlaying != null) {
            data.ringtonePlaying.stop();
            data.ringtonePlaying = null;
            data.myHandler.removeCallbacks(data.ringtoneStopperRunner);
            data.ringtoneStopperRunner = null;
        }
    }

    public static void notifyAppWidgetDataChange(final Context context) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] allWidgets = new int[widgetData.size()];
        final Iterator<Integer> widgetDataIdIterator = widgetData.keySet().iterator();
        for(int i=0; i<allWidgets.length; i++) {
            if(!widgetDataIdIterator.hasNext()) {
                break;
            }
            allWidgets[i] = widgetDataIdIterator.next();
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(allWidgets, R.id.filterList);

        final NDFilterDAO dao = new NDFilterDAO(context);
        for(final int appWidgetId : allWidgets) {
            final NDCalcData data = getWidgetData(context, appWidgetId);
            final NDFilter filterBeforeUpdate = data.filter;
            final int factorBeforeUpdate = filterBeforeUpdate != null ? filterBeforeUpdate.getFactor() : 1;
            final NDFilter filterAfterUpdate = dao.getNDFilterAtOrderpos(data.filterOrderpos);
            final int factorAfterUpdate = filterAfterUpdate != null ? filterAfterUpdate.getFactor() : 1;
            // This not not really great.
            // It is a kludge for not really getting any information about the list checked states
            // Trying to remember and handle if something has been deleted or added to the list
            // while the selection "seems" (from point of view of the widget) unchanged
            // to respect (assume?) then that the selected item under the "unchanged" selection
            // has been changed ...and update the calculation accordingly.
            // (and *still* it remains unclear what are the circumstances under which the
            // Remote ListView retains or forgets its visual checked state... :-( )
            if(factorAfterUpdate == factorBeforeUpdate) {
                break;
            } else {
                data.myHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (data.timerEnding > 0) {
                            stopTimer(context, appWidgetId);
                        }
                        data.filter = filterAfterUpdate;
                        setCalculation(context, appWidgetId);
                    }
                }, 500);
            }
        }
    }

    private static void setTimerVisibility(final Context context, final RemoteViews remoteViews, final NDCalcData data) {
        boolean isTimerVisible = data.showTimer > 0 && data.calculatedTime >= data.showTimer && data.calculatedTime < Integer.MAX_VALUE / 1000;
        boolean isTimerRunning = data.timerEnding > 0 && data.timerRunner != null;
        remoteViews.setViewVisibility(R.id.startButton, isTimerVisible && !isTimerRunning ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.stopButton, isTimerVisible && isTimerRunning ? View.VISIBLE : View.GONE);
        remoteViews.setViewVisibility(R.id.progressBar, isTimerVisible ? View.VISIBLE : View.INVISIBLE);
        remoteViews.setViewVisibility(R.id.smallTime, isTimerVisible ? isTimerRunning && data.showCountdown ? View.VISIBLE : View.GONE : View.VISIBLE);
        final int largePixels = context.getResources().getDimensionPixelSize(R.dimen.result_large_fontsize);
        final int smallPixels = context.getResources().getDimensionPixelSize(R.dimen.result_small_fontsize);
        if (Build.VERSION.SDK_INT >= 16) {
            remoteViews.setTextViewTextSize(R.id.smallTime, TypedValue.COMPLEX_UNIT_PX, isTimerRunning && data.showCountdown ? largePixels : smallPixels);
            remoteViews.setTextViewTextSize(R.id.largeTime, TypedValue.COMPLEX_UNIT_PX, isTimerRunning && data.showCountdown ? smallPixels : largePixels);
        }
    }

    private static RemoteViews createRemoteViews(final Context context, final NDCalcData data) {
        return new RemoteViews(context.getPackageName(), Build.VERSION.SDK_INT >= 23 ? LAYOUT[data.theme] : R.layout.app_widget);
    }

    public static String getPrefPrefix(final int appWidgetId) {
        return "widget0x" + Integer.toHexString(appWidgetId) + ":";
    }
}
