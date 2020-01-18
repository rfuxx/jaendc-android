package de.westfalen.fuldix.jaendc;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;

public abstract class ThemedActivityWithActionBarSqueezer extends Activity {
    public static final int[] THEMES  = Build.VERSION.SDK_INT >= 29 ? new int[] {R.style.AppThemeDark, R.style.AppThemeLight, R.style.AppThemeNightMode, R.style.AppThemeDayNight} : new int[] {R.style.AppThemeDark, R.style.AppThemeLight, R.style.AppThemeNightMode};

    private class PrefsListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(ConfigActivity.THEME.equals(key)) {
                if (Build.VERSION.SDK_INT >= 11) {
                    recreate();
                } else {
                    final Intent intent = getIntent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        }
    }

    private final TextViewDynamicSqueezer titleSqueezer = new TextViewDynamicSqueezer(this);
    private final TextViewDynamicSqueezer subTitleSqueezer = new TextViewDynamicSqueezer(this);
    private final PrefsListener prefsListener;
    private final boolean themeApplyRestart;
    private final int viewId;
    private String prefPrefix;

    public ThemedActivityWithActionBarSqueezer(final int viewId) {
        this(viewId, true);
    }

    public ThemedActivityWithActionBarSqueezer(final int viewId, final boolean themeApplyRestart) {
        this.prefsListener = themeApplyRestart ? new PrefsListener() : null;
        this.themeApplyRestart = themeApplyRestart;
        this.viewId = viewId;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final int theme = sanitizeThemeValue(prefs.getInt((prefPrefix != null ? prefPrefix : "") + ConfigActivity.THEME, getDefaultDefaultTheme()));
        setTheme(THEMES[theme]);
        if(themeApplyRestart) {
            prefs.registerOnSharedPreferenceChangeListener(prefsListener);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        addSquezers();
    }

    private void addSquezers() {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            final ActionBar ab = getActionBar();
            if (ab != null) {
                final CharSequence title = ab.getTitle();
                if (title != null) {
                    titleSqueezer.onViewCreate(title, viewId);
                }
                final CharSequence subTitle = ab.getSubtitle();
                if (subTitle != null) {
                    subTitleSqueezer.onViewCreate(subTitle, viewId);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            final TypedArray styled = getTheme().obtainStyledAttributes(new int[]{R.attr.actionBarIconTint});
            final int colorToUse = styled.getColor(0, 0x808080);
            styled.recycle();
            for (int i = 0; i < menu.size(); i++) {
                final Drawable drawable = menu.getItem(i).getIcon();
                if (drawable != null) {
                    drawable.setColorFilter(colorToUse, PorterDuff.Mode.SRC_ATOP);
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDetachedFromWindow() {
        removeSqueezers();
        super.onDetachedFromWindow();
    }

    private void removeSqueezers() {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            titleSqueezer.onViewDestroy();
            subTitleSqueezer.onViewDestroy();
        }
    }

    public void handleSystemUiVisibility(final View view) {
        handleSystemUiVisibility(view, "");
    }

    public void handleSystemUiVisibility(final View view, final String prefPrefix) {
        if (Build.VERSION.SDK_INT >= 11) {
            if (view != null) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                final int theme = sanitizeThemeValue(prefs.getInt(prefPrefix + ConfigActivity.THEME, getDefaultDefaultTheme()));
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

    public Drawable getTintedDrawableForActionBar(final int id) {
        final Drawable drawable;
        if (android.os.Build.VERSION.SDK_INT < 21) {
            drawable = getResources().getDrawable(id);
            if (drawable != null) {
                final TypedArray styled = getTheme().obtainStyledAttributes(new int[]{R.attr.actionBarIconTint});
                drawable.setColorFilter(styled.getColor(0, 0x808080), PorterDuff.Mode.SRC_ATOP);
                styled.recycle();
            }
        } else {
            drawable = getResources().getDrawable(id, getTheme());
        }
        return drawable;
    }

    public void applyStartStopButtonStyle(final CompoundButton button) {
        if (android.os.Build.VERSION.SDK_INT < 21) {
            final TypedArray styled = getTheme().obtainStyledAttributes(new int[]{button.isChecked() ? R.attr.timerButtonStop : R.attr.timerButtonStart});
            final Drawable drawable = button.getBackground();
            drawable.setColorFilter(styled.getColor(0, 0x808080), PorterDuff.Mode.SRC_ATOP);
            styled.recycle();
        }
    }

    public void setPrefPrefix(final String prefPrefix) {
        this.prefPrefix = prefPrefix;
    }

    public static int getDefaultDefaultTheme() {
        return Build.VERSION.SDK_INT >= 29 ? 3 : 0;
    }

    public static int sanitizeThemeValue(final int themeValue) {
        if(themeValue >= 0 && themeValue < THEMES.length) {
            return themeValue;
        } else {
            return 0;
        }
    }
}
