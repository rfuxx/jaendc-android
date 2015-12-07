package de.westfalen.fuldix.jaendc;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class HighlightSelectionArrayAdapter<T> extends ArrayAdapter<T> implements ListAdapter {
    public HighlightSelectionArrayAdapter(final Context context, final T[] data) {
        super(context, R.layout.list_item_single, data);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View viewItem = super.getView(position, convertView, parent);

        if (parent instanceof ListView) {
            ListView listView = (ListView) parent;
            boolean selected = position == listView.getCheckedItemPosition();
            viewItem.setBackgroundResource(selected ? R.drawable.background_selection : R.drawable.background_normal);
        }

        return viewItem;
    }
}
