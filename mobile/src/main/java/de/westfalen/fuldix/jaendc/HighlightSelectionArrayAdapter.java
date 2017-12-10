package de.westfalen.fuldix.jaendc;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class HighlightSelectionArrayAdapter<T> extends ArrayAdapter<T> implements ListAdapter {
    private final int backgroundColorSelection;

    public HighlightSelectionArrayAdapter(final Context context, final T[] data) {
        super(context, R.layout.list_item_single, data);

        final Resources.Theme theme = context.getTheme();
        final TypedArray styled = theme.obtainStyledAttributes(new int[]{R.attr.colorListItemActivated});
        backgroundColorSelection = styled.getColor(0, 0x808080);
        styled.recycle();
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
