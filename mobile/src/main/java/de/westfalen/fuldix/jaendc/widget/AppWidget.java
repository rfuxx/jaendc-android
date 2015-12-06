package de.westfalen.fuldix.jaendc.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.model.NDFilter;
import de.westfalen.fuldix.jaendc.model.Time;
import de.westfalen.fuldix.jaendc.text.ClearTextTimeFormat;
import de.westfalen.fuldix.jaendc.text.OutputTimeFormat;

@TargetApi(11)
public class AppWidget extends AppWidgetProvider{
    private static class NDCalcData {
        double time;
        Set<NDFilter> filters = new HashSet<>();
        double ndtime;
        long timerEnding;
        int showTimer = 4;
        Uri alarmTone;
        int alarmDuration = 29;
        int transparency = 33;
        Handler myHandler;
        Runnable timerRunner;
        Ringtone ringtonePlaying;
        Runnable ringtoneStopperRunner;
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
            final NDCalcData data = getWidgetData(appWidgetId);
            final long current = SystemClock.elapsedRealtime();
            final int remaining = (int) (data.timerEnding - current);
            if (remaining > 0) {
                final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);
                final int ndtimeMillis = (int) (data.ndtime*1000);
                remoteViews.setViewVisibility(R.id.startButton, View.GONE);
                remoteViews.setViewVisibility(R.id.stopButton, View.VISIBLE);
                remoteViews.setBoolean(R.id.timeList, "setEnabled", false);
                remoteViews.setBoolean(R.id.filterList, "setEnabled", false);
                // the above "just in case" to ensure all "partial" updates since the last "full" update get applied again
                // strictly need onthe the below
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
                if(data.alarmTone != null) {
                    data.ringtonePlaying = RingtoneManager.getRingtone(context, data.alarmTone);
                    data.ringtonePlaying.play();
                    data.ringtoneStopperRunner = new RingtoneStopperRunner(appWidgetId);
                    data.myHandler.postDelayed(data.ringtoneStopperRunner, (data.alarmDuration+1)*1000);
                }
            }
        }
    }

    private static class RingtoneStopperRunner implements Runnable {
        private final int appWidgetId;
        RingtoneStopperRunner(final int appWidgetId) {
            this.appWidgetId = appWidgetId;
        }
        @Override
        public void run() {
            NDCalcData data = getWidgetData(appWidgetId);
            if (data.ringtonePlaying != null) {
                data.ringtonePlaying.stop();
                data.ringtonePlaying = null;
                data.ringtoneStopperRunner = null;
            }
        }
    }

    private static final boolean multiselect = false; // due to limitations that we have no influence and are not even notified about screen orientation change - and the list views do not even keep their "checked items" states then :-(
    private static final NumberFormat clearTextTimeFormat = new ClearTextTimeFormat();
    private static final NumberFormat outputTimeFormat = new OutputTimeFormat();
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
        for(int id : appWidgetIds) {
            widgetData.remove(id);
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(final Context context){
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(final Context context){
        // Enter relevant functionality for when the last widget is disabled
        widgetData.clear();
        super.onDisabled(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        // the changed newOptions() data is already handled in updateAppWidget()
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

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
        NDCalcData data = getWidgetData(appWidgetId);
        if(data.ringtonePlaying != null) {
            data.ringtonePlaying.stop();
            data.ringtonePlaying = null;
            data.myHandler.removeCallbacks(data.ringtoneStopperRunner);
            data.ringtoneStopperRunner = null;
        }
        switch(intent.getAction()) {
            case "de.westfalen.fuldix.jaendc.time_list":
                final double time = intent.getDoubleExtra("SELECTED_TIME", 0);
                setTime(appWidgetId, time);
                setCalculation(context, appWidgetId);
                break;
            case "de.westfalen.fuldix.jaendc.filter_list":
                final Object object = intent.getParcelableExtra("SELECTED_FILTER");
                if(object != null && object instanceof NDFilter) {
                    final NDFilter filter = (NDFilter) object;
                    setFilter(appWidgetId, filter);
                    setCalculation(context, appWidgetId);
                }
                break;
            case "de.westfalen.fuldix.jaendc.start_button":
                startTimer(context, appWidgetId);
                break;
            case "de.westfalen.fuldix.jaendc.stop_button":
                stopTimer(context, appWidgetId);
                break;
            case "de.westfalen.fuldix.jaendc.config_button":
                final Intent configIntent = new Intent(context, ConfigActivity.class);
                configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                configIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                configIntent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                context.startActivity(configIntent);
                break;
        }
    }

    private static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId){
        final NDCalcData data = getWidgetData(appWidgetId);
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            final Bundle widgetOption = appWidgetManager.getAppWidgetOptions(appWidgetId);
            data.showTimer = widgetOption.getInt(ConfigActivity.SHOW_TIMER, data.showTimer);
            data.alarmDuration = widgetOption.getInt(ConfigActivity.ALARM_DURATION, data.alarmDuration);
            data.transparency = widgetOption.getInt(ConfigActivity.TRANSPARENCY, data.transparency);
            final String alarmToneStr = widgetOption.getString(ConfigActivity.ALARM_TONE);
            if (alarmToneStr != null) {
                data.alarmTone = Uri.parse(alarmToneStr);
            }
        }

        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.app_widget);

        final int transpValue = 255 - getWidgetData(appWidgetId).transparency * 255 / 100;
        remoteViews.setInt(R.id.widget, "setBackgroundColor", (transpValue & 0xff) << 24);
//        remoteViews.setInt(R.id.filterList, "setChoiceMode", ListView.CHOICE_MODE_MULTIPLE);  // does not accept this - but it's not working well, anyway (checked item states on screen rotation)

        setupListAdapterAndIntent(context, appWidgetId, remoteViews, R.id.timeList, TimeListService.class, "de.westfalen.fuldix.jaendc.time_list");
        setupListAdapterAndIntent(context, appWidgetId, remoteViews, R.id.filterList, NDFilterListService.class, "de.westfalen.fuldix.jaendc.filter_list");

        setupButtonIntent(context, appWidgetId, remoteViews, R.id.startButton, "de.westfalen.fuldix.jaendc.start_button");
        setupButtonIntent(context, appWidgetId, remoteViews, R.id.stopButton, "de.westfalen.fuldix.jaendc.stop_button");
        setupButtonIntent(context, appWidgetId, remoteViews, R.id.configButton, "de.westfalen.fuldix.jaendc.config_button");


        // when options about showTime have changed, adjust current visibilities and stop existing timer
        final String largeTimeText;
        final String smallTimeText;
        if(data.ndtime < Integer.MAX_VALUE / 1000) {
            if (data.ndtime > 0.0) {
                largeTimeText = outputTimeFormat.format(data.ndtime);
                smallTimeText = clearTextTimeFormat.format(data.ndtime);
                if (data.showTimer > 0 && data.ndtime >= data.showTimer) {
                    if(data.timerEnding > 0 && data.timerRunner != null) {
                        remoteViews.setViewVisibility(R.id.startButton, View.GONE);
                        remoteViews.setViewVisibility(R.id.stopButton, View.VISIBLE);
                    } else {
                        remoteViews.setViewVisibility(R.id.startButton, View.VISIBLE);
                        remoteViews.setViewVisibility(R.id.stopButton, View.GONE);
                    }
                    remoteViews.setViewVisibility(R.id.progressBar, View.VISIBLE);
                } else {
                    remoteViews.setViewVisibility(R.id.startButton, View.GONE);
                    remoteViews.setViewVisibility(R.id.stopButton, View.GONE);
                    remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
                    remoteViews.setBoolean(R.id.timeList, "setEnabled", true);
                    remoteViews.setBoolean(R.id.filterList, "setEnabled", true);
                    remoteViews.setProgressBar(R.id.progressBar, (int) (data.ndtime * 1000), 0, false);
                    if(data.timerEnding > 0 && data.timerRunner != null) {
                        data.myHandler.removeCallbacks(data.timerRunner);
                        data.timerEnding = 0;
                    }
                }
            } else {
                largeTimeText = context.getString(R.string.text_na);
                smallTimeText = context.getString(R.string.text_na);
                remoteViews.setViewVisibility(R.id.startButton, View.GONE);
                remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
            }
        } else {
            largeTimeText = String.format(context.getString(R.string.text_longer_than_symbol), outputTimeFormat.format(Integer.MAX_VALUE / 1000));
            smallTimeText = String.format(context.getString(R.string.text_longer_than), clearTextTimeFormat.format(Integer.MAX_VALUE / 1000));
            remoteViews.setViewVisibility(R.id.startButton, View.GONE);
            remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
        }
        remoteViews.setTextViewText(R.id.largeTime, largeTimeText);
        remoteViews.setTextViewText(R.id.smallTime, smallTimeText);

        // try to restore list indexes -- can only scroll to them but not set them as checked?!?
        int index = Arrays.binarySearch(Time.times, data.time);
        if(index < 0) {
            index = 16;
        }
        final int restoreTimeIndex = index;
        remoteViews.setScrollPosition(R.id.timeList, restoreTimeIndex);
        final int restoreFilterIndex;
        if(data.filters.isEmpty()) {
            restoreFilterIndex = -1;
        } else {
            restoreFilterIndex = data.filters.iterator().next().getOrderpos();
            remoteViews.setScrollPosition(R.id.filterList, restoreFilterIndex);
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        data.myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);
                remoteViews.setScrollPosition(R.id.timeList, restoreTimeIndex);
                if(restoreFilterIndex >= 0) {
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
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0, listItemClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(remoteViewId, clickPendingIntent);
    }

    private static void setupButtonIntent(final Context context, final int appWidgetId, final RemoteViews remoteViews, final int remoteViewId, final String intentAction) {
        final Intent buttonClickIntent = new Intent(context, AppWidget.class);
        buttonClickIntent.setAction(intentAction);
        buttonClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        buttonClickIntent.setData(Uri.parse(buttonClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0, buttonClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(remoteViewId, clickPendingIntent);
    }

    private static NDCalcData getWidgetData(final int appWidgetId) {
        NDCalcData data = widgetData.get(appWidgetId);
        if(data == null) {
            data = new NDCalcData();
            data.myHandler = new Handler();
            widgetData.put(appWidgetId, data);
        }
        return data;
    }

    private static void setTime(final int appWidgetId, final double time) {
        NDCalcData data = getWidgetData(appWidgetId);
        data.time = time;
    }

    private static void setFilter(final int appWidgetId, final NDFilter filter) {
        NDCalcData data = getWidgetData(appWidgetId);
        if(multiselect) {
            if (data.filters.contains(filter)) {
                data.filters.remove(filter);
            } else {
                data.filters.add(filter);
            }
        } else {
            data.filters.clear();
            data.filters.add(filter);
        }
    }

    private static void setCalculation(final Context context, final int appWidgetId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.app_widget);
        final NDCalcData data = getWidgetData(appWidgetId);
        data.ndtime = data.time;
        for(final NDFilter filter : data.filters) {
            data.ndtime *= filter.getFactor();
        }
        final String largeTimeText;
        final String smallTimeText;
        if(data.ndtime < Integer.MAX_VALUE / 1000) {
            if (data.ndtime > 0.0) {
                largeTimeText = outputTimeFormat.format(data.ndtime);
                smallTimeText = clearTextTimeFormat.format(data.ndtime);
                if (data.showTimer > 0 && data.ndtime >= data.showTimer) {
                    remoteViews.setViewVisibility(R.id.startButton, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.progressBar, View.VISIBLE);
                    data.myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // try to scroll list items back into view -- due to making the timer visible, they may have disappeared from view
                            final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.app_widget);
                            final int restoreTimeIndex = Arrays.binarySearch(Time.times, data.time);
                            if(restoreTimeIndex >= 0) {
                                remoteViews.setScrollPosition(R.id.timeList, restoreTimeIndex);
                            }
                            if(!data.filters.isEmpty()) {
                                remoteViews.setScrollPosition(R.id.filterList, data.filters.iterator().next().getOrderpos());
                            }
                            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
                        }
                    }, 1000);
                } else {
                    remoteViews.setViewVisibility(R.id.startButton, View.GONE);
                    remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
                }
            } else {
                largeTimeText = context.getString(R.string.text_na);
                smallTimeText = context.getString(R.string.text_na);
                remoteViews.setViewVisibility(R.id.startButton, View.GONE);
                remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
            }
        } else {
            largeTimeText = String.format(context.getString(R.string.text_longer_than_symbol), outputTimeFormat.format(Integer.MAX_VALUE / 1000));
            smallTimeText = String.format(context.getString(R.string.text_longer_than), clearTextTimeFormat.format(Integer.MAX_VALUE / 1000));
            remoteViews.setViewVisibility(R.id.startButton, View.GONE);
            remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
        }
        remoteViews.setTextViewText(R.id.largeTime, largeTimeText);
        remoteViews.setTextViewText(R.id.smallTime, smallTimeText);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
    }

    public static void startTimer(final Context context, final int appWidgetId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        final NDCalcData data = getWidgetData(appWidgetId);
        final int ndtimeMillis = (int) (data.ndtime*1000);
        remoteViews.setViewVisibility(R.id.startButton, View.GONE);
        remoteViews.setViewVisibility(R.id.stopButton, View.VISIBLE);
        remoteViews.setProgressBar(R.id.progressBar, ndtimeMillis, ndtimeMillis, false);
        remoteViews.setBoolean(R.id.timeList, "setEnabled", false);
        remoteViews.setBoolean(R.id.filterList, "setEnabled", false);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
        data.timerEnding = SystemClock.elapsedRealtime() + ndtimeMillis;
        if(data.timerRunner == null) {
            data.timerRunner = new TimerRunner(context, appWidgetId);
        }
        data.myHandler.post(data.timerRunner);
    }

    public static void stopTimer(final Context context, final int appWidgetId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        final NDCalcData data = getWidgetData(appWidgetId);
        remoteViews.setViewVisibility(R.id.startButton, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.stopButton, View.GONE);
        remoteViews.setProgressBar(R.id.progressBar, (int) (data.ndtime * 1000), 0, false);
        remoteViews.setBoolean(R.id.timeList, "setEnabled", true);
        remoteViews.setBoolean(R.id.filterList, "setEnabled", true);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
        data.timerEnding = 0;
        data.myHandler.removeCallbacks(data.timerRunner);
    }
}
