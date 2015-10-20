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
import de.westfalen.fuldix.jaendc.text.CameraTimeFormat;
import de.westfalen.fuldix.jaendc.text.ClearTextTimeFormat;
import de.westfalen.fuldix.jaendc.text.OutputTimeFormat;

public class Calculator implements ListView.OnItemClickListener, CompoundButton.OnCheckedChangeListener, Runnable {
    private final static double[] times = { 1d/8000, 1d/6400, 1d/5000,
            1d/4000, 1d/3200, 1d/2500,
            1d/2000, 1d/1600, 1d/1250,
            1d/1000, 1d/800, 1d/640,
            1d/500, 1d/400, 1d/320,
            1d/250, 1d/200, 1d/160,
            1d/125, 1d/100, 1d/80,
            1d/60, 1d/50, 1d/40,
            1d/30, 1d/25, 1d/20,
            1d/15, 1d/12, 1d/10,
            1d/8, 1d/6, 1d/5,
            1d/4, 0.3d, 0.4d,
            0.5d, 0.6d, 0.8d,
            1, 1.3d, 1.6d,
            2, 2.5d, 3.2d,
            3, 5, 6,
            8, 10, 13,
            15, 20, 25,
            30
    };
    private final String[] timeTexts;

    private static final String PERS_TIMER_ENDING = "timer_ending";
    private static final String PERS_MULTISELECT = "multiselect";
    private static final String PERS_TIMELIST_CHECKED_INDEX = "timeList.checkedIndex";
    private static final String PERS_FILTERLIST_CHECKED_IDS = "filterList.checkedIds";

    private final NumberFormat cameraTimeFormat = new CameraTimeFormat();
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
            doList(timeList);
            doList(filterList);
        }
    };

    private boolean multiselect;
    private long timerEnding;

    private boolean uiIsUpdating = false;

    public Calculator(final Activity activity)
    {
        timeTexts = new String[times.length];
        for(int i=timeTexts.length-1; i>=0; i--) {
            timeTexts[i] = cameraTimeFormat.format(times[i]);
        }
        context = activity;
        filterList = (ListView) activity.findViewById(R.id.filterList);
        filterAdapter = new NDFilterAdapter(activity);
        final ArrayAdapter<String> timeAdapter;
        if (Build.VERSION.SDK_INT >= 11) {
            timeAdapter = new ArrayAdapter<>(activity, R.layout.list_item_single, timeTexts);
        } else {
            timeAdapter = new HighlightSelectionArrayAdapter<>(activity, R.layout.list_item_single, timeTexts);
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
            double time = times[timePos];
            SparseBooleanArray states = filterList.getCheckedItemPositions();
            for(int f=0; f<filterAdapter.getCount(); f++) {
                NDFilter filter = filterAdapter.getItem(f);
                if(states.get(f)) {
                    time *= filter.getFactor();
                }
            }
            if(time < Integer.MAX_VALUE / 1000) {
                largeTime.setText(outputTimeFormat.format(time));
                smallTime.setText(clearTextTimeFormat.format(time));
                if (time >= 4) {
                    startStopButton.setVisibility(View.VISIBLE);
                    progressBar.setMax((int) (time * 1000));
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
                if (delayToNext < 50) {
                    delayToNext = 50;
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

    private static void doList(final ListView listView) {
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
}
