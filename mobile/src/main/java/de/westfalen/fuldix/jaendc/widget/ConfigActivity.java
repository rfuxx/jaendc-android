package de.westfalen.fuldix.jaendc.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import de.westfalen.fuldix.jaendc.R;

public class ConfigActivity extends Activity {
    static final String SHOW_TIMER = "showTimer";
    static final String ALARM_TONE = "alarmTOne";
    static final String TRANSPARENCY = "transparency";
    private static final int PICK_RINGTONE = 1;
    private Button alarmToneButton;
    private int showTimerValue = 4;
    private int transparencyValue = 33;
    private Uri alarmTone;
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
        final Button applyButton = (Button) findViewById(R.id.applyButton);
        showTimerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        });
        alarmToneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.config_alarm_tone_select));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarmTone);
                startActivityForResult(intent, PICK_RINGTONE);
            }
        });
        transparencyBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        });

        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    final Bundle options = AppWidgetManager.getInstance(this).getAppWidgetOptions(appWidgetId);
                    showTimerValue = options.getInt(SHOW_TIMER, showTimerValue);
                    alarmTone = options.getParcelable(ALARM_TONE);
                    transparencyValue = options.getInt(TRANSPARENCY, transparencyValue);
                }
            }
        }
        showTimerBar.setProgress(showTimerValue);
        if(alarmTone == null && appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            final Uri uriFromSystem = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            final Ringtone ringtone = RingtoneManager.getRingtone(this, uriFromSystem);
            if(ringtone != null) {
                alarmTone = uriFromSystem;
            }
        }
        if(alarmTone != null) {
            final Ringtone ringtone = RingtoneManager.getRingtone(this, alarmTone);
            alarmToneButton.setText(ringtone.getTitle(this));
        } else {
            alarmToneButton.setText(getString(R.string.config_alarm_tone_silent));
        }
        transparencyBar.setProgress(transparencyValue);

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    if (android.os.Build.VERSION.SDK_INT >= 16) {
                        final Bundle bundle = new Bundle();
                        bundle.putInt(SHOW_TIMER, showTimerValue);
                        if (alarmTone != null) {
                            bundle.putString(ALARM_TONE, alarmTone.toString());
                        }
                        bundle.putInt(TRANSPARENCY, transparencyValue);
                        AppWidgetManager.getInstance(ConfigActivity.this).updateAppWidgetOptions(appWidgetId, bundle);
                    }

                    final Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_RINGTONE) {
            if (resultCode == RESULT_OK) {
                Uri uriFromPicker = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                Ringtone ringtone = RingtoneManager.getRingtone(this, uriFromPicker);
                if(ringtone != null) {
                    alarmTone = uriFromPicker;
                    alarmToneButton.setText(ringtone.getTitle(this));
                }
            }
        }
    }
}
