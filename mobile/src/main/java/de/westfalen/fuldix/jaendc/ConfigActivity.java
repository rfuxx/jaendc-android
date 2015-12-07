package de.westfalen.fuldix.jaendc;

import android.app.ActionBar;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class ConfigActivity extends Activity {
    public static final String SHOW_TIMER = "showTimer";
    public static final String ALARM_TONE = "alarmTone";
    public static final String ALARM_DURATION = "alarmDuration";
    public static final String TRANSPARENCY = "transparency";
    public static final String ALARM_TONE_USE_SYSTEM_SOUND = "use_system_sound";
    public static final String ALARM_TONE_BE_SILENT = "silent";
    private static final int PICK_RINGTONE = 201;
    private Button alarmToneButton;
    private int showTimerValue = 4;
    private int transparencyValue = 33;
    private String alarmToneStr;
    private int alarmDurationValue = 29;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        setResult(RESULT_CANCELED);

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
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                final Bundle options = AppWidgetManager.getInstance(this).getAppWidgetOptions(appWidgetId);
                showTimerValue = options.getInt(SHOW_TIMER, showTimerValue);
                alarmToneStr = options.getString(ALARM_TONE, ALARM_TONE_BE_SILENT);
                alarmDurationValue = options.getInt(ALARM_DURATION, alarmDurationValue);
                transparencyValue = options.getInt(TRANSPARENCY, transparencyValue);
                setTitle(R.string.title_activity_config_widget);
                final ActionBar ab = getActionBar();
                if (ab != null) {
                    ab.setSubtitle(R.string.subtitle_activity_config_widget_explain);
                }
                transparencyLabel.setVisibility(View.VISIBLE);
                transparencyBar.setVisibility(View.VISIBLE);
                transparencyText.setVisibility(View.VISIBLE);
            }
        } else {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            showTimerValue = prefs.getInt(SHOW_TIMER, showTimerValue);
            alarmToneStr = prefs.getString(ALARM_TONE, ALARM_TONE_USE_SYSTEM_SOUND);
            alarmDurationValue = prefs.getInt(ALARM_DURATION, alarmDurationValue);
            setTitle(R.string.title_activity_config);
            if (android.os.Build.VERSION.SDK_INT >= 11) {
                final ActionBar ab = getActionBar();
                if(ab != null) {
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
                alarmDurationText.setText(String.format(getString(R.string.config_alarm_duration_value), progress+1));
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
                if(ringtone != null) {
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
                    if (android.os.Build.VERSION.SDK_INT >= 16) {
                        final Bundle bundle = new Bundle();
                        bundle.putInt(SHOW_TIMER, showTimerValue);
                        if (alarmToneStr != null) {
                            bundle.putString(ALARM_TONE, alarmToneStr);
                        } else {
                            bundle.putString(ALARM_TONE, ALARM_TONE_BE_SILENT);
                        }
                        bundle.putInt(ALARM_DURATION, alarmDurationValue);
                        bundle.putInt(TRANSPARENCY, transparencyValue);
                        AppWidgetManager.getInstance(ConfigActivity.this).updateAppWidgetOptions(appWidgetId, bundle);
                    }
                    final Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    setResult(RESULT_OK, resultValue);
                    doFinish();
                } else {
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ConfigActivity.this);
                    final SharedPreferences.Editor edit = prefs.edit();
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
                if(uriFromPicker != null) {
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
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    public static Uri getConfiguredAlarmTone(final Context context, final String alarmToneStr) {
        if(alarmToneStr != null) {
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
}
