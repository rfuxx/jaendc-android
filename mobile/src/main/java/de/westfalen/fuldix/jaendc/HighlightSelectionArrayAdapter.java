package de.westfalen.fuldix.jaendc;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class HighlightSelectionArrayAdapter<T> extends ArrayAdapter<T> implements ListAdapter {
    private final int backgroundColorSelection;

    public HighlightSelectionArrayAdapter(final Context context, final T[] data) {
        super(context, R.layout.list_item_single, data);

        backgroundColorSelection = StyleHelper.getStyledColorSafe(context.getTheme(), R.attr.colorListItemActivated, 0x808080);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View viewItem = super.getView(position, convertView, parent);

        if (parent instanceof ListView) {
            ListView listView = (ListView) parent;
            boolean selected = position == listView.getCheckedItemPosition();
            if(selected) {
                viewItem.setBackgroundColor(backgroundColorSelection);
            } else {
                viewItem.setBackgroundResource(android.R.color.transparent);
            }
        }

        return viewItem;
    }
}
