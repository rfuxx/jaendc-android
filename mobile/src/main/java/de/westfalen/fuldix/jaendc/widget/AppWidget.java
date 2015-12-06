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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.model.NDFilter;
import de.westfalen.fuldix.jaendc.text.ClearTextTimeFormat;
import de.westfalen.fuldix.jaendc.text.OutputTimeFormat;

@TargetApi(11)
public class AppWidget extends AppWidgetProvider{
    private static class NDCalcData {
        double time;
        Set<NDFilter> filters = new HashSet<>();
        int ndtimeMillis;
        long timerEnding;
        int showTimer = 4;
        Uri alarmTone;
        int transparency = 33;
        Handler myHandler;
        Runnable timerRunner;
        Ringtone ringtonePlaying;
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
                remoteViews.setViewVisibility(R.id.startButton, View.GONE);
                remoteViews.setViewVisibility(R.id.stopButton, View.VISIBLE);
                remoteViews.setBoolean(R.id.timeList, "setEnabled", false);
                remoteViews.setBoolean(R.id.filterList, "setEnabled", false);
                // the above "just in case" to ensure all "partial" updates since the last "full" update get applied again
                // strictly need onthe the below
                remoteViews.setProgressBar(R.id.progressBar, data.ndtimeMillis, remaining, false);
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
                final int width = 600; // should actually be something like progressBar.getWidth(); -- if only we could query the remoteview about its size...
                int delayToNext;
                if (width > 0) {
                    delayToNext = data.ndtimeMillis / width; // mdTimeMillis/width is the time until next progress bar pixel might hit (avoid unneccesary updates)
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
                    data.myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (data.ringtonePlaying != null) {
                                data.ringtonePlaying.stop();
                                data.ringtonePlaying = null;
                            }
                        }
                    }, 30*1000);
                }
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

        final NDCalcData data = getWidgetData(appWidgetId);
        data.showTimer = newOptions.getInt(ConfigActivity.SHOW_TIMER, data.showTimer);
        data.transparency = newOptions.getInt(ConfigActivity.TRANSPARENCY, data.transparency);
        final String alarmToneStr = newOptions.getString(ConfigActivity.ALARM_TONE);
        if(alarmToneStr != null){
            data.alarmTone = Uri.parse(alarmToneStr);
        }
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
        }
    }

    private static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId){
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.app_widget);

        final int transpValue = 255 - getWidgetData(appWidgetId).transparency * 255 / 100;
        remoteViews.setInt(R.id.widget, "setBackgroundColor", (transpValue & 0xff) << 24);
//        remoteViews.setInt(R.id.filterList, "setChoiceMode", ListView.CHOICE_MODE_MULTIPLE);  // does not accept this - but it's not working well, anyway (checked item states on screen rotation)

        setupListAdapterAndIntent(context, appWidgetId, remoteViews, R.id.timeList, TimeListService.class, "de.westfalen.fuldix.jaendc.time_list");
        setupListAdapterAndIntent(context, appWidgetId, remoteViews, R.id.filterList, NDFilterListService.class, "de.westfalen.fuldix.jaendc.filter_list");

        setupButtonIntent(context, appWidgetId, remoteViews, R.id.startButton, "de.westfalen.fuldix.jaendc.start_button");
        setupButtonIntent(context, appWidgetId, remoteViews, R.id.stopButton, "de.westfalen.fuldix.jaendc.stop_button");

        remoteViews.setScrollPosition(R.id.timeList, 16);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        final Handler myHandler = new Handler();
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);
                remoteViews.setScrollPosition(R.id.timeList, 16);
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
        double ndtime = data.time;
        for(final NDFilter filter : data.filters) {
            ndtime *= filter.getFactor();
        }
        final String largeTimeText;
        final String smallTimeText;
        if(ndtime < Integer.MAX_VALUE / 1000) {
            if (ndtime > 0.0) {
                largeTimeText = outputTimeFormat.format(ndtime);
                smallTimeText = clearTextTimeFormat.format(ndtime);
                if (data.showTimer > 0 && ndtime >= data.showTimer) {
                    remoteViews.setViewVisibility(R.id.startButton, View.VISIBLE);
                    remoteViews.setViewVisibility(R.id.progressBar, View.VISIBLE);
                    data.ndtimeMillis = (int) ndtime * 1000;
                } else {
                    remoteViews.setViewVisibility(R.id.startButton, View.GONE);
                    remoteViews.setViewVisibility(R.id.progressBar, View.GONE);
                }
            } else {
                largeTimeText = context.getString(R.string.text_na);
                smallTimeText = context.getString(R.string.text_na);
            }
        } else {
            largeTimeText = String.format(context.getString(R.string.text_longer_than_symbol), outputTimeFormat.format(Integer.MAX_VALUE / 1000));
            smallTimeText = String.format(context.getString(R.string.text_longer_than), clearTextTimeFormat.format(Integer.MAX_VALUE / 1000));
        }
        remoteViews.setTextViewText(R.id.largeTime, largeTimeText);
        remoteViews.setTextViewText(R.id.smallTime, smallTimeText);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
    }

    public static void startTimer(final Context context, final int appWidgetId) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        final NDCalcData data = getWidgetData(appWidgetId);
        remoteViews.setViewVisibility(R.id.startButton, View.GONE);
        remoteViews.setViewVisibility(R.id.stopButton, View.VISIBLE);
        remoteViews.setProgressBar(R.id.progressBar, data.ndtimeMillis, data.ndtimeMillis, false);
        remoteViews.setBoolean(R.id.timeList, "setEnabled", false);
        remoteViews.setBoolean(R.id.filterList, "setEnabled", false);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
        data.timerEnding = SystemClock.elapsedRealtime() + data.ndtimeMillis;
        if(data.myHandler == null) {
            data.myHandler = new Handler();
        }
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
        remoteViews.setProgressBar(R.id.progressBar, data.ndtimeMillis, 0, false);
        remoteViews.setBoolean(R.id.timeList, "setEnabled", true);
        remoteViews.setBoolean(R.id.filterList, "setEnabled", true);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews);
        data.timerEnding = 0;
        data.myHandler.removeCallbacks(data.timerRunner);
    }
}
