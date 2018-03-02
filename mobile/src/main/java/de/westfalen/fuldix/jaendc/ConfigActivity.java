package de.westfalen.fuldix.jaendc;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import de.westfalen.fuldix.jaendc.widget.AppWidget;

public class ConfigActivity extends ThemedActivityWithActionBarSqueezer {
    public static final String TIME_STYLE = "timeStyle";
    public static final String SHOW_TIMER = "showTimer";
    public static final String ALARM_TONE = "alarmTone";
    public static final String ALARM_DURATION = "alarmDuration";
    public static final String SHOW_COUNTDOWN = "showCountdown";
    public static final String THEME = "theme";
    public static final String TRANSPARENCY = "transparency";
    public static final String USE_ALARMCLOCK = "useAlarmClock";
    public static final String ALARM_TONE_USE_SYSTEM_SOUND = "use_system_sound";
    public static final String ALARM_TONE_BE_SILENT = "silent";
    public static final int[] THEMES = {R.style.AppThemeDark, R.style.AppThemeLight, R.style.AppThemeNightMode};
    private static final int PICK_RINGTONE = 201;
    private final TextViewDynamicSqueezer titleSqueezer = new TextViewDynamicSqueezer(this);
    private final TextViewDynamicSqueezer subTitleSqueezer = new TextViewDynamicSqueezer(this);
    private Button themeButton;
    private Button timeStyleButton;
    private Button alarmToneButton;
    private int themeValue = 0;
    private int timeStyleValue = 0;
    private int showTimerValue = 4;
    private int transparencyValue = 33;
    private String alarmToneStr = ALARM_TONE_BE_SILENT;
    private int alarmDurationValue = 29;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public ConfigActivity() {
        super(R.id.screen, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        final String prefPrefix = (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                                    ? AppWidget.getPrefPrefix(appWidgetId) : "";
        setPrefPrefix(prefPrefix);
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        themeValue = prefs.getInt(prefPrefix + ConfigActivity.THEME, themeValue);
        timeStyleValue = prefs.getInt(prefPrefix + ConfigActivity.TIME_STYLE, timeStyleValue);
        showTimerValue = prefs.getInt(prefPrefix + ConfigActivity.SHOW_TIMER, showTimerValue);
        boolean showCountdownValue = prefs.getBoolean(prefPrefix + ConfigActivity.SHOW_COUNTDOWN, false);
        alarmToneStr = prefs.getString(prefPrefix + ConfigActivity.ALARM_TONE, ALARM_TONE_BE_SILENT);
        alarmDurationValue = prefs.getInt(prefPrefix + ConfigActivity.ALARM_DURATION, alarmDurationValue);
        final boolean useAlarmClockValue = prefs.getBoolean(prefPrefix + ConfigActivity.USE_ALARMCLOCK, false);

        setContentView(R.layout.activity_config);
        handleSystemUiVisibility(findViewById(R.id.screen), prefPrefix);
        themeButton = (Button) findViewById(R.id.themeButton);
        timeStyleButton = (Button) findViewById(R.id.timeStyleButton);
        alarmToneButton = (Button) findViewById(R.id.alarmToneButton);
        final SeekBar showTimerBar = (SeekBar) findViewById(R.id.showTimerSeek);
        final TextView showTimerText = (TextView) findViewById(R.id.showTimerValue);
        final CheckBox showCountdownSwitch = (CheckBox) findViewById(R.id.showCountdownSwitch);
        final SeekBar transparencyBar = (SeekBar) findViewById(R.id.transparencySeek);
        final TextView transparencyText = (TextView) findViewById(R.id.transparencyValue);
        final TextView transparencyLabel = (TextView) findViewById(R.id.transparencyLabel);
        final Button applyButton = (Button) findViewById(R.id.applyButton);
        final SeekBar alarmDurationBar = (SeekBar) findViewById(R.id.alarmDurationSeek);
        final TextView alarmDurationText = (TextView) findViewById(R.id.alarmDurationValue);
        final TextView useAlarmClockLabel = (TextView) findViewById(R.id.useAlarmClockLabel);
        final CheckBox useAlarmClockSwitch = (CheckBox) findViewById(R.id.useAlarmClockSwitch);
        final Resources resources = getResources();

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (Build.VERSION.SDK_INT >= 11) {
                transparencyValue = prefs.getInt(prefPrefix + ConfigActivity.TRANSPARENCY, transparencyValue);
                setTitle(R.string.title_activity_config_widget);
                final ActionBar ab = getActionBar();
                if (ab != null) {
                    ab.setSubtitle(R.string.subtitle_activity_config_widget_explain);
                }
                transparencyLabel.setVisibility(View.VISIBLE);
                transparencyBar.setVisibility(View.VISIBLE);
                transparencyText.setVisibility(View.VISIBLE);
                final TextView themeLabel = (TextView) findViewById(R.id.themeLabel);
                themeLabel.setVisibility(Build.VERSION.SDK_INT >= 23 ? View.VISIBLE : View.GONE);
                themeButton.setVisibility(Build.VERSION.SDK_INT >= 23 ? View.VISIBLE : View.GONE);
                useAlarmClockSwitch.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, R.string.widget_not_for_2x, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            titleSqueezer.onViewCreate(resources.getString(R.string.title_activity_config_widget), R.id.screen);
            titleSqueezer.onViewCreate(resources.getString(R.string.subtitle_activity_config_widget_explain), R.id.screen);
        } else {
            setTitle(R.string.title_activity_config);
            if (Build.VERSION.SDK_INT >= 11) {
                final ActionBar ab = getActionBar();
                if (ab != null) {
                    getActionBar().setSubtitle(R.string.subtitle_activity_config_explain);
                }
            }
            transparencyLabel.setVisibility(View.GONE);
            transparencyBar.setVisibility(View.GONE);
            transparencyText.setVisibility(View.GONE);
            useAlarmClockLabel.setVisibility(Build.VERSION.SDK_INT >= 21 ? View.VISIBLE : View.GONE);
            useAlarmClockSwitch.setVisibility(Build.VERSION.SDK_INT >= 21 ? View.VISIBLE : View.GONE);
            titleSqueezer.onViewCreate(resources.getString(R.string.title_activity_config), R.id.screen);
            titleSqueezer.onViewCreate(resources.getString(R.string.subtitle_activity_config_explain), R.id.screen);
        }

        final SeekBar.OnSeekBarChangeListener showTimerSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                final String text;
                if (progress > 0) {
                    text = String.format(getString(R.string.config_show_timer_value), progress);
                } else {
                    text = getString(R.string.config_show_timer_value_never);
                }
                showTimerText.setText(text);
                showTimerValue = progress;
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }
        };
        themeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String[] timeStylesArr = getApplicationContext().getResources().getStringArray(R.array.config_theme_options);

                AlertDialog.Builder builder = new AlertDialog.Builder(ConfigActivity.this);

                builder.setTitle(getString(R.string.config_theme));
                builder.setItems(timeStylesArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        themeValue = which;
                        setThemeButtonText();
                    }
                });
                builder.create().show();
            }
        });
        timeStyleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String[] timeStylesArr = getApplicationContext().getResources().getStringArray(R.array.config_time_style_options);

                AlertDialog.Builder builder = new AlertDialog.Builder(ConfigActivity.this);

                builder.setTitle(getString(R.string.config_time_style));
                builder.setItems(timeStylesArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        timeStyleValue = which;
                        setTimeStyleButtonText();
                    }
                });
                builder.create().show();
            }
        });
        alarmToneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.config_alarm_tone_select));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, getConfiguredAlarmTone(ConfigActivity.this, ALARM_TONE_USE_SYSTEM_SOUND));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM | RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getConfiguredAlarmTone(ConfigActivity.this, alarmToneStr));
                startActivityForResult(intent, PICK_RINGTONE);
            }
        });
        final SeekBar.OnSeekBarChangeListener alarmDurationSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                alarmDurationText.setText(String.format(getString(R.string.config_alarm_duration_value), progress + 1));
                alarmDurationValue = progress;
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }
        };
        final SeekBar.OnSeekBarChangeListener transparencySeekBarListner = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                transparencyText.setText(String.format(getString(R.string.config_transparency_value), progress));
                transparencyValue = progress;
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }
        };

        setThemeButtonText();
        setTimeStyleButtonText();
        showTimerBar.setProgress(showTimerValue);
        showTimerSeekBarListener.onProgressChanged(showTimerBar, showTimerValue, false);
        showTimerBar.setOnSeekBarChangeListener(showTimerSeekBarListener);
        showCountdownSwitch.setChecked(showCountdownValue);
        useAlarmClockSwitch.setChecked(useAlarmClockValue);
        switch (alarmToneStr) {
            case ALARM_TONE_BE_SILENT: {
                alarmToneButton.setText(getString(R.string.config_alarm_tone_silent));
                break;
            }
            case ALARM_TONE_USE_SYSTEM_SOUND: {
                alarmToneButton.setText(getString(R.string.config_alarm_tone_system));
                break;
            }
            default: {
                Uri alarmTone = getConfiguredAlarmTone(this, alarmToneStr);
                final Ringtone ringtone = RingtoneManager.getRingtone(this, alarmTone);
                if (ringtone != null) {
                    alarmToneButton.setText(ringtone.getTitle(this));
                } else {
                    alarmToneButton.setText(getString(R.string.config_alarm_tone_system));
                }
                break;
            }
        }
        alarmDurationBar.setProgress(alarmDurationValue);
        alarmDurationSeekBarListener.onProgressChanged(alarmDurationBar, alarmDurationValue, false);
        alarmDurationBar.setOnSeekBarChangeListener(alarmDurationSeekBarListener);
        transparencyBar.setProgress(transparencyValue);
        transparencySeekBarListner.onProgressChanged(transparencyBar, transparencyValue, false);
        transparencyBar.setOnSeekBarChangeListener(transparencySeekBarListner);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    if (alarmToneStr == null) {
                        alarmToneStr = ALARM_TONE_BE_SILENT;
                    }
                    final String prefPrefix = AppWidget.getPrefPrefix(appWidgetId);
                    final SharedPreferences.Editor prefsEdit = PreferenceManager.getDefaultSharedPreferences(ConfigActivity.this).edit();
                    if(Build.VERSION.SDK_INT >= 23) {
                        prefsEdit.putInt(prefPrefix + THEME, themeValue);
                    }
                    prefsEdit.putInt(prefPrefix + TIME_STYLE, timeStyleValue);
                    prefsEdit.putInt(prefPrefix + SHOW_TIMER, showTimerValue);
                    prefsEdit.putBoolean(prefPrefix + SHOW_COUNTDOWN, showCountdownSwitch.isChecked());
                    prefsEdit.putString(prefPrefix + ALARM_TONE, alarmToneStr);
                    prefsEdit.putInt(prefPrefix + ALARM_DURATION, alarmDurationValue);
                    prefsEdit.putInt(prefPrefix + TRANSPARENCY, transparencyValue);
                    prefsEdit.commit();
                    final Intent widgetNotify = new Intent(ConfigActivity.this, AppWidget.class);
                    widgetNotify.setAction(AppWidget.WIDGET_CONFIG_WAS_MODIFIED);
                    widgetNotify.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    sendBroadcast(widgetNotify);
                    // following intent still needed to ensure that initial widget creation succeeds
                    final Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    setResult(RESULT_OK, resultValue);
                    doFinish();
                } else {
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ConfigActivity.this);
                    final SharedPreferences.Editor edit = prefs.edit();
                    edit.putInt(THEME, themeValue);
                    edit.putInt(TIME_STYLE, timeStyleValue);
                    edit.putInt(SHOW_TIMER, showTimerValue);
                    edit.putBoolean(SHOW_COUNTDOWN, showCountdownSwitch.isChecked());
                    if (alarmToneStr != null) {
                        edit.putString(ALARM_TONE, alarmToneStr);
                    } else {
                        edit.putString(ALARM_TONE, ALARM_TONE_USE_SYSTEM_SOUND);
                    }
                    edit.putInt(ALARM_DURATION, alarmDurationValue);
                    if (Build.VERSION.SDK_INT >= 21) {
                        edit.putBoolean(USE_ALARMCLOCK, useAlarmClockSwitch.isChecked());
                    }
                    edit.commit();

                    final Intent resultValue = new Intent();
                    setResult(RESULT_OK, resultValue);
                    doFinish();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if(titleSqueezer != null) {
            titleSqueezer.onViewDestroy();
        }
        if(subTitleSqueezer != null) {
            subTitleSqueezer.onViewDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        doFinish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_RINGTONE) {
            if (resultCode == RESULT_OK) {
                Uri uriFromPicker = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uriFromPicker != null) {
                    Ringtone ringtone = RingtoneManager.getRingtone(this, uriFromPicker);
                    if (ringtone != null) {
                        alarmToneStr = uriFromPicker.toString();
                        alarmToneButton.setText(ringtone.getTitle(this));
                    } else {
                        alarmToneStr = ALARM_TONE_USE_SYSTEM_SOUND;
                        alarmToneButton.setText(R.string.config_alarm_tone_system);
                    }
                } else {
                    alarmToneStr = ALARM_TONE_BE_SILENT;
                    alarmToneButton.setText(R.string.config_alarm_tone_silent);
                }
            }
        }
    }

    private void doFinish() {
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    public static Uri getConfiguredAlarmTone(final Context context, final String alarmToneStr) {
        if (alarmToneStr != null) {
            switch (alarmToneStr) {
                case ALARM_TONE_USE_SYSTEM_SOUND: {
                    Uri alarmTone = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
                    if (alarmTone != null) {
                        return alarmTone;
                    } else {
                        return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                    }
                }
                case ALARM_TONE_BE_SILENT: {
                    return null;
                }
                default: {
                    return Uri.parse(alarmToneStr);
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void setThemeButtonText() {
        String[] themesArr = getApplicationContext().getResources().getStringArray(R.array.config_theme_options);
        if(themeValue < 0) {
            themeValue = 0;
        }
        if(themeValue >= themesArr.length) {
            themeValue = themesArr.length -1;
        }
        themeButton.setText(themesArr[themeValue]);
    }

    private void setTimeStyleButtonText() {
        String[] timeStylesArr = getApplicationContext().getResources().getStringArray(R.array.config_time_style_options);
        if(timeStyleValue < 0) {
            timeStyleValue = 0;
        }
        if(timeStyleValue >= timeStylesArr.length) {
            timeStyleValue = timeStylesArr.length -1;
        }
        timeStyleButton.setText(timeStylesArr[timeStyleValue]);
    }
}
