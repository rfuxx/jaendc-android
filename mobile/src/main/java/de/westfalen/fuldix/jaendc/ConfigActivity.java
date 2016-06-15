package de.westfalen.fuldix.jaendc;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import de.westfalen.fuldix.jaendc.widget.AppWidget;

public class ConfigActivity extends Activity {
    public static final String TIME_STYLE = "timeStyle";
    public static final String SHOW_TIMER = "showTimer";
    public static final String ALARM_TONE = "alarmTone";
    public static final String ALARM_DURATION = "alarmDuration";
    public static final String TRANSPARENCY = "transparency";
    public static final String ALARM_TONE_USE_SYSTEM_SOUND = "use_system_sound";
    public static final String ALARM_TONE_BE_SILENT = "silent";
    private static final int PICK_RINGTONE = 201;
    private Button timeStyleButton;
    private Button alarmToneButton;
    private int timeStyleValue = 0;
    private int showTimerValue = 4;
    private int transparencyValue = 33;
    private String alarmToneStr = ALARM_TONE_BE_SILENT;
    private int alarmDurationValue = 29;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        setResult(RESULT_CANCELED);

        timeStyleButton = (Button) findViewById(R.id.timeStyleButton);
        alarmToneButton = (Button) findViewById(R.id.alarmToneButton);
        final SeekBar showTimerBar = (SeekBar) findViewById(R.id.showTimerSeek);
        final TextView showTimerText = (TextView) findViewById(R.id.showTimerValue);
        final SeekBar transparencyBar = (SeekBar) findViewById(R.id.transparencySeek);
        final TextView transparencyText = (TextView) findViewById(R.id.transparencyValue);
        final TextView transparencyLabel = (TextView) findViewById(R.id.transparencyLabel);
        final Button applyButton = (Button) findViewById(R.id.applyButton);
        final SeekBar alarmDurationBar = (SeekBar) findViewById(R.id.alarmDurationSeek);
        final TextView alarmDurationText = (TextView) findViewById(R.id.alarmDurationValue);

        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (Build.VERSION.SDK_INT >= 11) {
                final String prefPrefix = AppWidget.getPrefPrefix(appWidgetId);
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ConfigActivity.this);
                timeStyleValue = prefs.getInt(prefPrefix + ConfigActivity.TIME_STYLE, timeStyleValue);
                showTimerValue = prefs.getInt(prefPrefix + ConfigActivity.SHOW_TIMER, showTimerValue);
                alarmToneStr = prefs.getString(prefPrefix + ConfigActivity.ALARM_TONE, ALARM_TONE_BE_SILENT);
                alarmDurationValue = prefs.getInt(prefPrefix + ConfigActivity.ALARM_DURATION, alarmDurationValue);
                transparencyValue = prefs.getInt(prefPrefix + ConfigActivity.TRANSPARENCY, transparencyValue);
                setTitle(R.string.title_activity_config_widget);
                final ActionBar ab = getActionBar();
                if (ab != null) {
                    ab.setSubtitle(R.string.subtitle_activity_config_widget_explain);
                }
                transparencyLabel.setVisibility(View.VISIBLE);
                transparencyBar.setVisibility(View.VISIBLE);
                transparencyText.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, R.string.widget_not_for_2x, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            timeStyleValue = prefs.getInt(TIME_STYLE, timeStyleValue);
            showTimerValue = prefs.getInt(SHOW_TIMER, showTimerValue);
            alarmToneStr = prefs.getString(ALARM_TONE, ALARM_TONE_USE_SYSTEM_SOUND);
            alarmDurationValue = prefs.getInt(ALARM_DURATION, alarmDurationValue);
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

        setTimeStyleButtonText();
        showTimerBar.setProgress(showTimerValue);
        showTimerSeekBarListener.onProgressChanged(showTimerBar, showTimerValue, false);
        showTimerBar.setOnSeekBarChangeListener(showTimerSeekBarListener);
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
                    prefsEdit.putInt(prefPrefix + ConfigActivity.TIME_STYLE, timeStyleValue);
                    prefsEdit.putInt(prefPrefix + ConfigActivity.SHOW_TIMER, showTimerValue);
                    prefsEdit.putString(prefPrefix + ConfigActivity.ALARM_TONE, alarmToneStr);
                    prefsEdit.putInt(prefPrefix + ConfigActivity.ALARM_DURATION, alarmDurationValue);
                    prefsEdit.putInt(prefPrefix + ConfigActivity.TRANSPARENCY, transparencyValue);
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
                    edit.putInt(TIME_STYLE, timeStyleValue);
                    edit.putInt(SHOW_TIMER, showTimerValue);
                    if (alarmToneStr != null) {
                        edit.putString(ALARM_TONE, alarmToneStr);
                    } else {
                        edit.putString(ALARM_TONE, ALARM_TONE_USE_SYSTEM_SOUND);
                    }
                    edit.putInt(ALARM_DURATION, alarmDurationValue);
                    edit.commit();

                    final Intent resultValue = new Intent();
                    setResult(RESULT_OK, resultValue);
                    doFinish();
                }
            }
        });
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
