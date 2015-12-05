package de.westfalen.fuldix.jaendc.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import de.westfalen.fuldix.jaendc.NDFilterAdapter;
import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.model.NDFilter;

@TargetApi(11)
public class NDFilterListService extends RemoteViewsService {

    public static class ListProvider implements RemoteViewsFactory {
        private final Context context;
        private final int appWidgetId;
        private final NDFilterAdapter adapter;

        public ListProvider(final Context context, final Intent intent) {
            this.context = context;
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                            AppWidgetManager.INVALID_APPWIDGET_ID);
            adapter = new NDFilterAdapter(context);
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            adapter.refreshFilters();
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public int getCount() {
            return adapter.getCount();
        }

        @Override
        public long getItemId(int position) {
            return adapter.getItemId(position);
        }

        @Override
        public boolean hasStableIds() {
            return adapter.hasStableIds();
        }

        @Override
        public RemoteViews getViewAt(final int position) {
            final RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.widget_list_item_filter);
            NDFilter filter = adapter.getItem(position);
            remoteView.setTextViewText(R.id.list_item_filter_name, filter.getName());
            remoteView.setTextViewText(R.id.list_item_filter_data, filter.getDescription(context));

            Bundle extras = new Bundle();
            extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            extras.putParcelable("SELECTED_FILTER", filter);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            remoteView.setOnClickFillInIntent(R.id.whole_item, fillInIntent);

            return remoteView;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return (new ListProvider(getApplicationContext(), intent));
    }
}
