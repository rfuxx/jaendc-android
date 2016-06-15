package de.westfalen.fuldix.jaendc.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.NumberFormat;

import de.westfalen.fuldix.jaendc.ConfigActivity;
import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.model.Time;
import de.westfalen.fuldix.jaendc.text.CameraTimeFormat;

@TargetApi(11)
public class TimeListService extends RemoteViewsService {

    public static class ListProvider implements RemoteViewsFactory {
        private final Context context;
        private final int appWidgetId;
        private final int timeStyle;
        private final NumberFormat cameraTimeFormat;

        public ListProvider(final Context context, final Intent intent) {
            this.context = context;
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                            AppWidgetManager.INVALID_APPWIDGET_ID);
            final String prefPrefix = AppWidget.getPrefPrefix(appWidgetId);
            timeStyle = prefs.getInt(prefPrefix + ConfigActivity.TIME_STYLE, 0);
            cameraTimeFormat = new CameraTimeFormat(timeStyle);
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public int getCount() {
            return Time.times[timeStyle].length;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public RemoteViews getViewAt(final int position) {
            final RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.widget_list_item_single);
            remoteView.setTextViewText(R.id.single_text, cameraTimeFormat.format(Time.times[timeStyle][position]));

            Bundle extras = new Bundle();
            extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            extras.putDouble("SELECTED_TIME", Time.times[timeStyle][position]);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            remoteView.setOnClickFillInIntent(R.id.single_text, fillInIntent);

            return remoteView;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return Time.times[timeStyle].length;
        }
    }

    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return (new ListProvider(getApplicationContext(), intent));
    }
}
