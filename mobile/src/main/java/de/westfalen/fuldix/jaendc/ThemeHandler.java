package de.westfalen.fuldix.jaendc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class ThemeHandler implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Activity activity;
    private final boolean applyRestart;

    public ThemeHandler(final Activity activity) {
        this(activity, true);
    }

    public ThemeHandler(final Activity activity, boolean applyRestart) {
        this.activity = activity;
        this.applyRestart = applyRestart;
    }

    public void onActivityCreate() {
        onActivityCreate("");
    }

    public void onActivityCreate(final String prefPrefix) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final int theme = prefs.getInt(prefPrefix + ConfigActivity.THEME, 0);
        activity.setTheme(ConfigActivity.THEMES[theme]);
        if(applyRestart) {
            prefs.registerOnSharedPreferenceChangeListener(this);
        }
    }

    public void handleSystemUiVisibility(final View view) {
        handleSystemUiVisibility(view, "");
    }

    public void handleSystemUiVisibility(final View view, final String prefPrefix) {
        if (Build.VERSION.SDK_INT >= 11) {
            if (view != null) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                final int theme = prefs.getInt(prefPrefix + ConfigActivity.THEME, 0);
                switch(theme) {
                    case 0:
                        break;
                    case 1:
                        view.setSystemUiVisibility(view.getSystemUiVisibility()
                                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        );
                        break;
                    case 2:
                        view.setSystemUiVisibility(view.getSystemUiVisibility()
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        );
                        break;
                }
            }
        }
    }

    public void onCreateOptionsMenu(final Menu menu) {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            final Resources.Theme theme = activity.getTheme();
            final TypedArray styled = theme.obtainStyledAttributes(new int[]{R.attr.actionBarIconTint});
            final int colorToUse = styled.getColor(0, 0x808080);
            styled.recycle();
            for (int i = 0; i < menu.size(); i++) {
                final Drawable drawable = menu.getItem(i).getIcon();
                if (drawable != null) {
                    drawable.setColorFilter(colorToUse, PorterDuff.Mode.SRC_ATOP);
                }
            }
        }
    }

    public Drawable getTintedDrawableForActionBar(final int id) {
        final Resources.Theme theme = activity.getTheme();
        final Drawable drawable;
        if (android.os.Build.VERSION.SDK_INT < 21) {
            drawable = activity.getResources().getDrawable(id);
            if (drawable != null) {
                final TypedArray styled = theme.obtainStyledAttributes(new int[]{R.attr.actionBarIconTint});
                drawable.setColorFilter(styled.getColor(0, 0x808080), PorterDuff.Mode.SRC_ATOP);
                styled.recycle();
            }
        } else {
            drawable = activity.getResources().getDrawable(id, theme);
        }
        return drawable;
    }

    public void setStartStopButtonStyle(final CompoundButton button) {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            final Resources.Theme theme = activity.getTheme();
            final TypedArray styled = theme.obtainStyledAttributes(new int[]{button.isChecked() ? R.attr.timerButtonStop : R.attr.timerButtonStart});
            final Drawable drawable = button.getBackground();
            drawable.setColorFilter(styled.getColor(0, 0x808080), PorterDuff.Mode.SRC_ATOP);
            styled.recycle();
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if(ConfigActivity.THEME.equals(key)) {
            if (Build.VERSION.SDK_INT >= 11) {
                activity.recreate();
            } else {
                final Intent intent = activity.getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                activity.finish();
                activity.startActivity(intent);
            }
        }
    }
}
