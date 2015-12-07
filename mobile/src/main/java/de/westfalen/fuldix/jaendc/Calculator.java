package de.westfalen.fuldix.jaendc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

import de.westfalen.fuldix.jaendc.db.NDFilterDAO;
import de.westfalen.fuldix.jaendc.model.NDFilter;
import de.westfalen.fuldix.jaendc.model.Time;
import de.westfalen.fuldix.jaendc.text.ClearTextTimeFormat;
import de.westfalen.fuldix.jaendc.text.OutputTimeFormat;

public class Calculator implements ListView.OnItemClickListener, CompoundButton.OnCheckedChangeListener, Runnable, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PERS_TIMER_ENDING = "timer_ending";
    private static final String PERS_MULTISELECT = "multiselect";
    private static final String PERS_TIMELIST_CHECKED_INDEX = "timeList.checkedIndex";
    private static final String PERS_FILTERLIST_CHECKED_IDS = "filterList.checkedIds";

    private final NumberFormat clearTextTimeFormat = new ClearTextTimeFormat();
    private final NumberFormat outputTimeFormat = new OutputTimeFormat();

    private final NDFilterAdapter filterAdapter;
    private final ListView timeList;
    private final ListView filterList;
    private final TextView largeTime;
    private final TextView smallTime;
    private final ToggleButton startStopButton;
    private final ProgressBar progressBar;
    private MenuItem multiselectItem;
    private final Context context;
    private final Runnable scrollListsToSelection = new Runnable() {
        @Override
        public void run() {
            scrollListToSelection(timeList);
            scrollListToSelection(filterList);
        }
    };

    private boolean multiselect;
    private double ndtime;
    private long timerEnding;

    private boolean uiIsUpdating = false;

    public Calculator(final Activity activity)
    {
        context = activity;
        filterList = (ListView) activity.findViewById(R.id.filterList);
        filterAdapter = new NDFilterAdapter(activity);
        final ArrayAdapter<String> timeAdapter;
        if (Build.VERSION.SDK_INT >= 11) {
            timeAdapter = new ArrayAdapter<>(activity, R.layout.list_item_single, Time.getTimeTexts());
        } else {
            timeAdapter = new HighlightSelectionArrayAdapter<>(activity, R.layout.list_item_single, Time.getTimeTexts());
        }
        timeList = (ListView) activity.findViewById(R.id.timeList);
        timeList.setAdapter(timeAdapter);
        timeList.setOnItemClickListener(this);
        filterList.setAdapter(filterAdapter);
        filterList.setOnItemClickListener(this);

        largeTime = (TextView) activity.findViewById(R.id.largeTime);
        smallTime = (TextView) activity.findViewById(R.id.smallTime);
        startStopButton = (ToggleButton) activity.findViewById(R.id.startStopButton);
        startStopButton.setVisibility(View.INVISIBLE);
        startStopButton.setOnCheckedChangeListener(this);
        progressBar = (ProgressBar) activity.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        timeList.setSelection(16);
        timeList.setItemChecked(18, true);
        onItemClick(timeList, null, 18, 18);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        calculate();
    }

    private void applyMultiselect() {
        if (multiselect) {
            if(multiselectItem != null) {
                multiselectItem.setChecked(true);
                if (Build.VERSION.SDK_INT >= 11) {
                    multiselectItem.setIcon(R.drawable.ic_multiselect);
                } else {
                    multiselectItem.setIcon(R.drawable.ic_singleselect_old);
                    multiselectItem.setTitle(R.string.action_multi_single);
                }
            }
            filterList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        } else {
            if(multiselectItem != null) {
                multiselectItem.setChecked(false);
                if (Build.VERSION.SDK_INT >= 11) {
                    multiselectItem.setIcon(R.drawable.ic_singleselect);
                } else {
                    multiselectItem.setIcon(R.drawable.ic_multiselect_old);
                    multiselectItem.setTitle(R.string.action_multi);
                }
            }
            filterList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            // bit quirky here -- but otherwise the list is in single mode but still has multiple selections
            SparseBooleanArray states = filterList.getCheckedItemPositions();
            for(int i=filterAdapter.getCount()-1; i>=0; i--) {
                if(states.get(i)) {
                    filterList.setItemChecked(i, true);
                    break;
                }
            }
            filterList.post(scrollListsToSelection);
            calculate();
        }
    }

    public void onCreateOptionsMenu(final Menu menu) {
        multiselectItem = menu.findItem(R.id.action_multiselect);
        applyMultiselect();
    }

    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        switch (id) {
            case R.id.action_multiselect: {
                multiselect = !item.isChecked();
                applyMultiselect();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        if(!uiIsUpdating) {
            try {
                uiIsUpdating = true;
                if (isChecked) {
                    startTimer();
                } else {
                    stopTimer(false);
                }
            } finally {
                uiIsUpdating = false;
            }
        }
    }

    public boolean setUiIsUpdating(final boolean uiIsUpdating) {
        final boolean uiWasUpdating = this.uiIsUpdating;
        this.uiIsUpdating = uiIsUpdating;
        return uiWasUpdating;
    }

    private void calculate() {
        final int timePos = timeList.getCheckedItemPosition();
        if(timePos == ListView.INVALID_POSITION) {
            largeTime.setText(R.string.text_na);
            smallTime.setText(R.string.text_na);
        } else {
            ndtime = Time.times[timePos];
            SparseBooleanArray states = filterList.getCheckedItemPositions();
            for(int f=0; f<filterAdapter.getCount(); f++) {
                NDFilter filter = filterAdapter.getItem(f);
                if(states.get(f)) {
                    ndtime *= filter.getFactor();
                }
            }
            if(ndtime < Integer.MAX_VALUE / 1000) {
                largeTime.setText(outputTimeFormat.format(ndtime));
                smallTime.setText(clearTextTimeFormat.format(ndtime));
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                final double startTimer = prefs.getInt(ConfigActivity.SHOW_TIMER, 4);
                if (startTimer > 0 && ndtime >= startTimer) {
                    startStopButton.setVisibility(View.VISIBLE);
                    progressBar.setMax((int) (ndtime * 1000));
                } else {
                    startStopButton.setVisibility(View.INVISIBLE);
                }
            }  else {
                largeTime.setText(String.format(context.getString(R.string.text_longer_than_symbol), outputTimeFormat.format(Integer.MAX_VALUE / 1000)));
                smallTime.setText(String.format(context.getString(R.string.text_longer_than), clearTextTimeFormat.format(Integer.MAX_VALUE / 1000)));
                startStopButton.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void startTimer() {
        timeList.setEnabled(false);
        filterList.setEnabled(false);
        if(multiselectItem != null) {
            multiselectItem.setEnabled(false);
        }
        progressBar.setVisibility(View.VISIBLE);
        timerEnding = SystemClock.elapsedRealtime() + progressBar.getMax();
        CalculatorAlarm.schedule(context, timerEnding);
        run();
    }

    private void stopTimer(final boolean hasBeenTriggered) {
        if(!hasBeenTriggered) {
            CalculatorAlarm.cancel(context);
        }
        timerEnding = 0;
        final boolean oldUiIsUpdating = uiIsUpdating;
        try {
            uiIsUpdating = true;
            startStopButton.setChecked(false);
            progressBar.removeCallbacks(this);
            progressBar.setVisibility(View.INVISIBLE);
            timeList.setEnabled(true);
            filterList.setEnabled(true);
            if (multiselectItem != null) {
                multiselectItem.setEnabled(true);
            }
        } finally {
            uiIsUpdating = oldUiIsUpdating;
        }
    }

    @Override
    public void run() {
        final int milliseconds = progressBar.getMax();
        final long current = SystemClock.elapsedRealtime();
        final int remaining = (int) (timerEnding - current);
        if(remaining > 0) {
            progressBar.setProgress(remaining);
            final int width = progressBar.getWidth();
            int delayToNext;
            if(width > 0) {
                delayToNext = milliseconds / width; // milliseconds/width is the time until next progress bar pixel might hit (avoid unneccesary updates)
                if (delayToNext < 20) {
                    delayToNext = 20;
                }
            } else {
                delayToNext = remaining;
            }
            progressBar.postDelayed(this, delayToNext);
        } else {
            // alarm is played through scheduled alarm
            stopTimer(true);
        }
    }

    private void restoreRunningTimer() {
        final boolean oldUiIsUpdating = uiIsUpdating;
        try {
            uiIsUpdating = true;
            progressBar.removeCallbacks(this);
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final double startTimer = prefs.getInt(ConfigActivity.SHOW_TIMER, 4);
            if (!(startTimer > 0 && ndtime >= startTimer)) {
                stopTimer(false);
            }
            if (timerEnding > SystemClock.elapsedRealtime()) {
                startStopButton.setChecked(true);
                progressBar.setVisibility(View.VISIBLE);
                timeList.setEnabled(false);
                filterList.setEnabled(false);
                if (multiselectItem != null) {
                    multiselectItem.setEnabled(false);
                }
                progressBar.post(this);
            } else {
                startStopButton.setChecked(false);
                progressBar.setVisibility(View.INVISIBLE);
                timeList.setEnabled(true);
                filterList.setEnabled(true);
                if (multiselectItem != null) {
                    multiselectItem.setEnabled(true);
                }
            }
        } finally {
            uiIsUpdating = oldUiIsUpdating;
        }
    }

    void restoreState(Bundle savedState) {
        timerEnding = savedState.getLong(PERS_TIMER_ENDING);
        multiselect = savedState.getBoolean(PERS_MULTISELECT);
        applyMultiselect();
        calculate();
        restoreRunningTimer();
    }

    void saveState(Bundle savedState) {
        savedState.putLong(PERS_TIMER_ENDING, timerEnding);
        savedState.putBoolean(PERS_MULTISELECT, multiselect);
    }

    void loadPersistentState() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        timerEnding = sharedPreferences.getLong(PERS_TIMER_ENDING, 0);
        multiselect = sharedPreferences.getBoolean(PERS_MULTISELECT, false);
        applyMultiselect();
        final int timeIndex = sharedPreferences.getInt(PERS_TIMELIST_CHECKED_INDEX, 18);
        timeList.setItemChecked(timeIndex, true);
        final String checkedIds = sharedPreferences.getString(PERS_FILTERLIST_CHECKED_IDS, "");
        final StringTokenizer tok = new StringTokenizer(checkedIds, ";");
        final Collection<Long> idList = new HashSet<>();
        while(tok.hasMoreTokens()) {
            final String str = tok.nextToken();
            try {
                final Long id = Long.valueOf(str);
                idList.add(id);
            } catch (final NumberFormatException e) {
                System.err.println(String.format("Warning: internal prefstate %s contains unparsable number %s", PERS_FILTERLIST_CHECKED_IDS, str));
            }
        }
        filterAdapter.refreshFilters();
        checkIfFiltersExist();
        for(int pos=0; pos<filterAdapter.getCount(); pos++) {
            final long id = filterAdapter.getItemId(pos);
            filterList.setItemChecked(pos, idList.contains(id));
        }
        filterList.post(scrollListsToSelection);
        calculate();
        restoreRunningTimer();
    }

    void savePersistentState() {
        progressBar.removeCallbacks(this);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PERS_TIMER_ENDING, timerEnding);
        editor.putBoolean(PERS_MULTISELECT, multiselect);
        editor.putInt(PERS_TIMELIST_CHECKED_INDEX, timeList.getCheckedItemPosition());
        final StringBuilder ids = new StringBuilder();
        final SparseBooleanArray states = filterList.getCheckedItemPositions();
        for(int f=0; f<filterAdapter.getCount(); f++) {
            if(states.get(f)) {
                ids.append(filterAdapter.getItemId(f));
                ids.append(';');
            }
        }
        editor.putString(PERS_FILTERLIST_CHECKED_IDS, ids.toString());
        editor.commit();
    }

    private void checkIfFiltersExist() {
        if(filterAdapter.getCount() == 0) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.dialog_create_filters_title);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(R.string.dialog_create_filters_text);
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    NDFilterDAO dao = new NDFilterDAO(context);
                    dao.insertDefaultFilters();
                    filterAdapter.refreshFilters();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        }
    }

    private static void scrollListToSelection(final ListView listView) {
        final int pos = listView.getCheckedItemPosition();
        if(pos < 0 || pos >= listView.getCount())
        {
            return;
        }
        final int first = listView.getFirstVisiblePosition();
        final int last = listView.getLastVisiblePosition();
        if (pos < first)
        {
            listView.setSelection(pos);
        } else if (pos >= last)
        {
            listView.setSelection(1 + pos - (last - first));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(ConfigActivity.SHOW_TIMER.equals(key)) {
            if(ndtime < Integer.MAX_VALUE / 1000) {
                final double startTimer = sharedPreferences.getInt(ConfigActivity.SHOW_TIMER, 4);
                if (startTimer > 0 && ndtime >= startTimer) {
                    startStopButton.setVisibility(View.VISIBLE);
                    progressBar.setMax((int) (ndtime * 1000));
                } else {
                    stopTimer(false);
                    startStopButton.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
