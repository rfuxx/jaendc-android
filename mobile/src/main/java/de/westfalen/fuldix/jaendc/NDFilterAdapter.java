package de.westfalen.fuldix.jaendc;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.os.Build;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.westfalen.fuldix.jaendc.db.NDFilterDAO;
import de.westfalen.fuldix.jaendc.model.NDFilter;

public class NDFilterAdapter extends BaseAdapter implements ListAdapter {
    @TargetApi(11)
    private class OnTouchDragListener implements View.OnTouchListener {
        private final int pos;
        protected OnTouchDragListener(final int pos) {
            this.pos = pos;
        }
        @Override
        public boolean onTouch(final View view, final MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                final View listItem = (View) view.getParent();
                final ClipData data = ClipData.newPlainText("NDFilter", "");
                dragPos = pos;
                final View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(listItem);
                view.startDrag(data, shadowBuilder, view, 0);
                return true;
            } else {
                return false;
            }
        }
    }
    private final Context context;
    private final int backgroundColorSelection;
    private List<NDFilter> filters;
    private boolean indicateDragable;
    private int dragPos;

    public NDFilterAdapter(final Context context, final boolean indicateDragable) {
        this.context = context;
        this.filters = new ArrayList<>();
        this.indicateDragable = indicateDragable;

        backgroundColorSelection = StyleHelper.getStyledColorSafe(context.getTheme(), R.attr.colorListItemActivated, 0x808080);
    }

    public NDFilterAdapter(final Context context) {
        this(context, false);
    }

    public void refreshFilters() {
        final NDFilterDAO dao = new NDFilterDAO(context);
        this.filters = dao.getAllNDFilters();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filters.size();
    }

    @Override
    public synchronized NDFilter getItem(final int position) {
        final int last = filters.size();
        return filters.get(position < 0 ? 0 : position > last ? last : position);
    }

    @Override
    public synchronized long getItemId(final int position) {
        final int last = filters.size();
        return filters.get(position < 0 ? 0 : position > last ? last : position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final View twoLineListItem;

        if (convertView == null) {
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            twoLineListItem = inflater.inflate(R.layout.list_item_filter, null);
        } else {
            twoLineListItem = convertView;
        }

        final TextView nameView = (TextView) twoLineListItem.findViewById(R.id.list_item_filter_name);
        final TextView dataView = (TextView) twoLineListItem.findViewById(R.id.list_item_filter_data);

        final NDFilter filter = filters.get(position);
        nameView.setText(filter.getName());
        dataView.setText(filter.getDescription(context));
        if (parent instanceof ListView) {
            final ListView listView = (ListView) parent;
            final SparseBooleanArray states = listView.getCheckedItemPositions();
            final boolean selected = states != null && states.get(position);
            nameView.setSelected(selected);
            dataView.setSelected(selected);
            if (Build.VERSION.SDK_INT < 11) {
                if(selected) {
                    twoLineListItem.setBackgroundColor(backgroundColorSelection);
                } else {
                    twoLineListItem.setBackgroundResource(android.R.color.transparent);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 11) {
            final View dragHandle = twoLineListItem.findViewById(R.id.list_item_drag_handle);
            if(dragHandle != null) {
                dragHandle.setVisibility(indicateDragable ? View.VISIBLE : View.GONE);
                dragHandle.setOnTouchListener(new OnTouchDragListener(position));
            }
        }

        return twoLineListItem;
    }

    @Override
    public boolean isEmpty() {
        return filters.isEmpty();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(final int position) {
        return true;
    }

    public synchronized int putChange(final NDFilter filter) {
        final long id = filter.getId();
        boolean isContained = false;
        int pos = filters.size();
        for (int f = 0; f < filters.size(); f++) {
            if (filters.get(f).getId() == id) {
                isContained = true;
                pos = f;
                filters.set(f, filter);
            }
        }
        if (!isContained) {
            filters.add(filter);
        }
        notifyDataSetChanged();
        return pos;
    }

    public synchronized void removeAtPos(final int pos) {
        filters.remove(pos);
        for(int i=pos; i<filters.size(); i++) {
            filters.get(i).setOrderpos(i);
        }
        notifyDataSetChanged();
    }

    public synchronized void removeId(final long id) {
        for(int pos=0; pos < filters.size(); pos++) {
            if(id == filters.get(pos).getId()) {
                removeAtPos(pos);
                break;
            }
        }
    }

    public int getDragPos() {
        return dragPos;
    }

    public synchronized void commitDrop(final int targetPos) {
        final NDFilter dragFilter = filters.get(dragPos);
        filters.remove(dragPos);
        filters.add(targetPos, dragFilter);
        // renumber the whole list, wtf
        for(int i=0; i<filters.size(); i++) {
            filters.get(i).setOrderpos(i);
        }
        notifyDataSetChanged();
    }

    public void setIndicateDragable(final boolean indicateDragable) {
        this.indicateDragable = indicateDragable;
        this.notifyDataSetInvalidated();
    }
}
