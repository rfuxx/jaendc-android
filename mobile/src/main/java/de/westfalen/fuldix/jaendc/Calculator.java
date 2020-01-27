package de.westfalen.fuldix.jaendc;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

import de.westfalen.fuldix.jaendc.db.NDFilterDAO;
import de.westfalen.fuldix.jaendc.model.NDFilter;
import de.westfalen.fuldix.jaendc.model.Time;
import de.westfalen.fuldix.jaendc.text.ClearTextTimeFormat;
import de.westfalen.fuldix.jaendc.text.CountdownTextTimeFormat;
import de.westfalen.fuldix.jaendc.text.OutputTimeFormat;
import de.westfalen.fuldix.jaendc.widget.AppWidget;

public class Calculator implements ListView.OnItemClickListener, CompoundButton.OnCheckedChangeListener, Runnable, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PERS_TIMER_ENDING = "timer_ending";
    private static final String PERS_ALARM_INTENT = "alarm_intent";
    private static final String PERS_MULTISELECT = "multiselect";
    private static final String PERS_TIMELIST_CHECKED_INDEX = "timeList.checkedIndex";
    private static final String PERS_FILTERLIST_CHECKED_IDS = "filterList.checkedIds";

    private final NDFilterAdapter filterAdapter;
    private final ListView timeList;
    private final ListView filterList;
    private final TextView largeTime;
    private final TextViewDynamicSqueezer largeTimeSqueezer;
    private final TextView smallTime;
    private final TextViewDynamicSqueezer smallTimeSqueezer;
    private final ToggleButton startStopButton;
    private final ProgressBar progressBar;
    private final View screen;
    private MenuItem multiselectItem;
    private final ThemedActivityWithActionBarSqueezer themedActivity;
    private final Runnable scrollListsToSelection = new Runnable() {
        @Override
        public void run() {
            scrollListToSelection(timeList);
            scrollListToSelection(filterList);
        }
    };

    private ClearTextTimeFormat clearTextTimeFormat;
    private OutputTimeFormat outputTimeFormat;
    private CountdownTextTimeFormat countdownTextFormat;
    private int timeStyle;
    private int showTimerMinSeconds;
    private boolean showCountdown;

    private boolean multiselect;
    private double calculatedTime;
    private long timerEnding;

    private boolean isShowing = true;
    private boolean uiIsUpdating = false;

    private PendingIntent alarmIntent;

    public Calculator(final ThemedActivityWithActionBarSqueezer themedActivity)
    {
        this.themedActivity = themedActivity;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.themedActivity);
        prefs.registerOnSharedPreferenceChangeListener(this);
        timeStyle = prefs.getInt(ConfigActivity.TIME_STYLE, 0);
        showTimerMinSeconds = prefs.getInt(ConfigActivity.SHOW_TIMER, 4);
        showCountdown = prefs.getBoolean(ConfigActivity.SHOW_COUNTDOWN, false);
        clearTextTimeFormat = new ClearTextTimeFormat(this.themedActivity, timeStyle);
        outputTimeFormat = new OutputTimeFormat(this.themedActivity, timeStyle);
        countdownTextFormat = new CountdownTextTimeFormat(this.themedActivity);

        filterList = (ListView) themedActivity.findViewById(R.id.filterList);
        filterAdapter = new NDFilterAdapter(themedActivity);
        final ArrayAdapter<String> timeAdapter;
        if (Build.VERSION.SDK_INT >= 11) {
            timeAdapter = new ArrayAdapter<>(themedActivity, R.layout.list_item_single, Time.getTimeTexts(timeStyle));
        } else {
            timeAdapter = new HighlightSelectionArrayAdapter<>(themedActivity, Time.getTimeTexts(timeStyle));
        }
        timeList = (ListView) themedActivity.findViewById(R.id.timeList);
        timeList.setAdapter(timeAdapter);
        timeList.setOnItemClickListener(this);
        filterList.setAdapter(filterAdapter);
        filterList.setOnItemClickListener(this);

        largeTime = (TextView) themedActivity.findViewById(R.id.largeTime);
        smallTime = (TextView) themedActivity.findViewById(R.id.smallTime);
        largeTimeSqueezer = new TextViewDynamicSqueezer(themedActivity);
        largeTimeSqueezer.onViewCreate(largeTime);
        smallTimeSqueezer = new TextViewDynamicSqueezer(themedActivity);
        smallTimeSqueezer.onViewCreate(smallTime);
        startStopButton = (ToggleButton) themedActivity.findViewById(R.id.startStopButton);
        themedActivity.applyStartStopButtonStyle(startStopButton);
        startStopButton.setOnCheckedChangeListener(this);
        progressBar = (ProgressBar) themedActivity.findViewById(R.id.progressBar);
        setTimerVisibility();
        screen = (View) themedActivity.findViewById(R.id.screen);

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
                    // v11 the icon is always visible -> show what we *have*
                    multiselectItem.setIcon(themedActivity.getTintedDrawableForActionBar(R.drawable.ic_multiselect_tinted));
                } else {
                    // older the icon is only visible when the menu pops up
                    // -> show what it *will* switch *to*
                    multiselectItem.setIcon(themedActivity.getTintedDrawableForActionBar(R.drawable.ic_singleselect_tinted));
                    multiselectItem.setTitle(R.string.action_multi_single);
                }
            }
            filterList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        } else {
            if(multiselectItem != null) {
                multiselectItem.setChecked(false);
                if (Build.VERSION.SDK_INT >= 11) {
                    multiselectItem.setIcon(themedActivity.getTintedDrawableForActionBar(R.drawable.ic_singleselect_tinted));
                } else {
                    multiselectItem.setIcon(themedActivity.getTintedDrawableForActionBar(R.drawable.ic_multiselect_tinted));
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
            final boolean oldUiIsUpdating = uiIsUpdating;
            try {
                uiIsUpdating = true;
                if (isChecked) {
                    startTimer();
                } else {
                    stopTimer();
                }
            } finally {
                uiIsUpdating = oldUiIsUpdating;
            }
        }
        themedActivity.applyStartStopButtonStyle(buttonView);
    }

    public boolean setUiIsUpdating(final boolean uiIsUpdating) {
        final boolean uiWasUpdating = this.uiIsUpdating;
        this.uiIsUpdating = uiIsUpdating;
        return uiWasUpdating;
    }

    private void calculate() {
        final int timePos = timeList.getCheckedItemPosition();
        if(timePos == ListView.INVALID_POSITION || timePos >= Time.times[timeStyle].length) {
            calculatedTime = 0;
            largeTime.setText(R.string.text_na);
            if (Build.VERSION.SDK_INT >= 23) {
                largeTime.setTextAppearance(R.style.ResultTextNormal);
            } else {
                largeTime.setTextAppearance(themedActivity, R.style.ResultTextNormal);
            }
            smallTime.setText(R.string.text_na);
        } else {
            calculatedTime = Time.times[timeStyle][timePos];
            SparseBooleanArray states = filterList.getCheckedItemPositions();
            for(int f=0; f<filterAdapter.getCount(); f++) {
                NDFilter filter = filterAdapter.getItem(f);
                if(states.get(f)) {
                    calculatedTime *= filter.getFactor();
                }
            }
            if(calculatedTime < Integer.MAX_VALUE / 1000) {
                if(showTimerMinSeconds > 0 && calculatedTime >= showTimerMinSeconds) {
                    calculatedTime = Time.roundTimeToCameraTime(calculatedTime, Time.times[timeStyle][Time.times[timeStyle].length-1], timeStyle);
                }
                final String formattedOutputText = outputTimeFormat.format(calculatedTime);
                final int textAppearanceResource;
                if(calculatedTime > Time.times[timeStyle][Time.times[timeStyle].length-1]) {
                    final int pos = formattedOutputText.length();
                    final SpannableString spanString = new SpannableString(formattedOutputText + "BULB");   // don't translate? Because "BULB" is used by cameras regardless of their language settings?
                    spanString.setSpan(new RelativeSizeSpan(0.333f), pos, pos+4, 0);
                    largeTime.setText(spanString);
                    textAppearanceResource = R.style.ResultTextBulbLength;
                } else {
                    largeTime.setText(formattedOutputText);
                    textAppearanceResource = R.style.ResultTextNormal;
                }
                if (Build.VERSION.SDK_INT >= 23) {
                    largeTime.setTextAppearance(textAppearanceResource);
                } else {
                    largeTime.setTextAppearance(themedActivity, textAppearanceResource);
                }
                smallTime.setText(clearTextTimeFormat.format(calculatedTime));
                if (showTimerMinSeconds > 0 && calculatedTime >= showTimerMinSeconds) {
                    progressBar.setMax((int) (calculatedTime * 1000));
                }
            }  else {
                largeTime.setText(String.format(themedActivity.getString(R.string.text_longer_than_symbol), outputTimeFormat.format(Integer.MAX_VALUE / 1000)));
                final int styledResourceId = StyleHelper.getStyledResourceId(themedActivity.getTheme(), R.attr.resultTextMaxExceeded, R.style.ResultTextMaxExceeded);
                if (Build.VERSION.SDK_INT >= 23) {
                    largeTime.setTextAppearance(styledResourceId);
                } else {
                    largeTime.setTextAppearance(themedActivity, styledResourceId);
                }
                smallTime.setText(String.format(themedActivity.getString(R.string.text_longer_than), clearTextTimeFormat.format(Integer.MAX_VALUE / 1000)));
            }
            setTimerVisibility();
        }
    }

    private void startTimer() {
        timeList.setEnabled(false);
        filterList.setEnabled(false);
        if(multiselectItem != null) {
            multiselectItem.setEnabled(false);
        }
        timerEnding = System.currentTimeMillis() + (long) (calculatedTime * 1000);
        setTimerVisibility();
        alarmIntent = CalculatorAlarm.schedule(themedActivity, timerEnding);
        run();
    }

    private void stopTimer() {
        CalculatorAlarm.cancel(themedActivity);
        alarmIntent = null;
        timerEnding = 0;
        final boolean oldUiIsUpdating = uiIsUpdating;
        try {
            uiIsUpdating = true;
            smallTime.setText(clearTextTimeFormat.format(calculatedTime));
            screen.removeCallbacks(this);
            setTimerVisibility();
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
        if(!isShowing) {
            System.err.println("run() despite not showing");
            return;
        }
        final long current = System.currentTimeMillis();
        final int remaining = (int) (timerEnding - current);
        if(showCountdown) {
            smallTime.setText(countdownTextFormat.format(remaining));
        }
        if(remaining > 0) {
            progressBar.setProgress(remaining);
            final int width = progressBar.getWidth();
            int delayToNext;
            if (width > 0) {
                final int milliseconds = (int) (calculatedTime * 1000);
                final int delayToNextProgressBar = milliseconds / width; // milliseconds/width is the time until next progress bar pixel might hit (avoid unneccesary updates)
                if(showCountdown) {
                    final int delayToNextCountdownText = countdownTextFormat.delayToNext(remaining);
                    delayToNext = Math.min(delayToNextCountdownText, delayToNextProgressBar);
                } else {
                    delayToNext = delayToNextProgressBar;
                }
            } else {
                delayToNext = remaining;
            }
            if (delayToNext < 20) {
                delayToNext = 20;
            }
            screen.postDelayed(this, delayToNext);
        } else {
            // let the alarm go off (to do it right here from UI scope is more timely than relying on the schedulings in the AlarmManager due to weidness that even setExactAndAllowWhileIdle is not necessarily exact)
            try {
                if(alarmIntent != null) {
                    alarmIntent.send();
                }
            } catch (final PendingIntent.CanceledException e) {
                System.err.println("alarm-go-off not sent because canceled");
            }
            // and stop the timer visually
            stopTimer();
        }
    }

    private void restoreRunningTimer() {
        final boolean oldUiIsUpdating = uiIsUpdating;
        try {
            uiIsUpdating = true;
            screen.removeCallbacks(this);
            if (!(showTimerMinSeconds > 0 && calculatedTime >= showTimerMinSeconds)) {
                stopTimer();
            }
            setTimerVisibility();
            if (timerEnding > System.currentTimeMillis()) {
                timeList.setEnabled(false);
                filterList.setEnabled(false);
                if (multiselectItem != null) {
                    multiselectItem.setEnabled(false);
                }
                screen.post(this);
            } else {
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

    private void setTimerVisibility() {
        final boolean oldUiIsUpdating = uiIsUpdating;
        try {
            uiIsUpdating = true;
            boolean isTimerVisible = showTimerMinSeconds > 0 && calculatedTime >= showTimerMinSeconds && calculatedTime < Integer.MAX_VALUE / 1000;
            boolean isTimerRunning = timerEnding > System.currentTimeMillis();
            startStopButton.setVisibility(isTimerVisible ? View.VISIBLE : View.GONE);
            startStopButton.setChecked(isTimerRunning);
            progressBar.setVisibility(isTimerRunning ? View.VISIBLE : View.INVISIBLE);
            smallTime.setVisibility(calculatedTime > 0 ? isTimerVisible ? isTimerRunning && showCountdown ? View.VISIBLE : View.GONE : View.VISIBLE : View.GONE);
            final int largePixels = themedActivity.getResources().getDimensionPixelSize(R.dimen.result_large_fontsize);
            final int smallPixels = themedActivity.getResources().getDimensionPixelSize(R.dimen.result_small_fontsize);
            smallTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, isTimerRunning && showCountdown ? largePixels : smallPixels);
            largeTime.setTextSize(TypedValue.COMPLEX_UNIT_PX, isTimerRunning && showCountdown ? smallPixels : largePixels);
        } finally {
            uiIsUpdating = oldUiIsUpdating;
        }
    }

    void restoreInstanceState(Bundle savedState) {
        timerEnding = savedState.getLong(PERS_TIMER_ENDING);
        alarmIntent = (PendingIntent) savedState.get(PERS_ALARM_INTENT);
        multiselect = savedState.getBoolean(PERS_MULTISELECT);
        applyMultiselect();
        filterAdapter.refreshFilters();
        calculate();
        restoreRunningTimer();
    }

    void saveInstanceState(Bundle savedState) {
        savedState.putLong(PERS_TIMER_ENDING, timerEnding);
        savedState.putParcelable(PERS_ALARM_INTENT, alarmIntent);
        savedState.putBoolean(PERS_MULTISELECT, multiselect);
    }

    void loadPersistentState() {
        isShowing=true;
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(themedActivity);
        timerEnding = sharedPreferences.getLong(PERS_TIMER_ENDING, 0);
        alarmIntent = timerEnding > System.currentTimeMillis() ? CalculatorAlarm.schedule(themedActivity, timerEnding) : null;
        multiselect = sharedPreferences.getBoolean(PERS_MULTISELECT, false);
        applyMultiselect();
        final int timeIndex = sharedPreferences.getInt(PERS_TIMELIST_CHECKED_INDEX, 18);
        timeList.setItemChecked(timeIndex, true);
        filterAdapter.refreshFilters();
        checkIfFiltersExist();
        final String checkedIds = sharedPreferences.getString(PERS_FILTERLIST_CHECKED_IDS, "");
        setCheckedFilterIdsFromString(checkedIds);
        filterList.post(scrollListsToSelection);
        calculate();
        restoreRunningTimer();
    }

    private void setCheckedFilterIdsFromString(final String checkedIds) {
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
        for(int pos=0; pos<filterAdapter.getCount(); pos++) {
            final long id = filterAdapter.getItemId(pos);
            filterList.setItemChecked(pos, idList.contains(id));
        }
    }

    void savePersistentState() {
        isShowing=false;
        screen.removeCallbacks(this);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(themedActivity);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        if(timerEnding > 0) {
            editor.putLong(PERS_TIMER_ENDING, timerEnding);
        }
        editor.putBoolean(PERS_MULTISELECT, multiselect);
        editor.putInt(PERS_TIMELIST_CHECKED_INDEX, timeList.getCheckedItemPosition());
        editor.putString(PERS_FILTERLIST_CHECKED_IDS, getCheckedFilterIdsAsString());
        editor.commit();
    }

    private String getCheckedFilterIdsAsString() {
        final StringBuilder ids = new StringBuilder();
        final SparseBooleanArray states = filterList.getCheckedItemPositions();
        for(int f=0; f<filterAdapter.getCount(); f++) {
            if(states.get(f)) {
                ids.append(filterAdapter.getItemId(f));
                ids.append(';');
            }
        }
        return ids.toString();
    }

    private void checkIfFiltersExist() {
        if(filterAdapter.getCount() == 0) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(themedActivity);
            builder.setTitle(R.string.dialog_create_filters_title);
            builder.setIcon(R.drawable.ic_dialog_alert_tinted);
            builder.setMessage(R.string.dialog_create_filters_text);
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    NDFilterDAO dao = new NDFilterDAO(themedActivity);
                    dao.insertDefaultFilters();
                    filterAdapter.refreshFilters();
                    AppWidget.notifyAppWidgetDataChange(themedActivity);
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
            if(calculatedTime < Integer.MAX_VALUE / 1000) {
                showTimerMinSeconds = sharedPreferences.getInt(ConfigActivity.SHOW_TIMER, 4);
                if (showTimerMinSeconds > 0 && calculatedTime >= showTimerMinSeconds) {
                    progressBar.setMax((int) (calculatedTime * 1000));
                } else {
                    stopTimer();
                }
                setTimerVisibility();
            }
        } else if(ConfigActivity.TIME_STYLE.equals(key)) {
            timeStyle = sharedPreferences.getInt(ConfigActivity.TIME_STYLE, 0);
            clearTextTimeFormat = new ClearTextTimeFormat(themedActivity, timeStyle);
            outputTimeFormat = new OutputTimeFormat(themedActivity, timeStyle);
            final ArrayAdapter<String> timeAdapter;
            if (Build.VERSION.SDK_INT >= 11) {
                timeAdapter = new ArrayAdapter<>(themedActivity, R.layout.list_item_single, Time.getTimeTexts(timeStyle));
            } else {
                timeAdapter = new HighlightSelectionArrayAdapter<>(themedActivity, Time.getTimeTexts(timeStyle));
            }
            timeList.setAdapter(timeAdapter);
            calculate();
        } else if(ConfigActivity.SHOW_COUNTDOWN.equals(key)) {
            showCountdown = sharedPreferences.getBoolean(ConfigActivity.SHOW_COUNTDOWN, false);
        }

        setTimerVisibility();
        if (timerEnding > System.currentTimeMillis()) {
            if(showCountdown) {
                screen.removeCallbacks(this);
                run();
            }
        }
    }

    public void onDestroy() {
        largeTimeSqueezer.onViewDestroy();
        smallTimeSqueezer.onViewDestroy();
    }
}
