package de.westfalen.fuldix.jaendc;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import de.westfalen.fuldix.jaendc.manage.NDFilterListActivity;

public class NDCalculatorActivity extends Activity {
    private final ThemeHandler themeHandler = new ThemeHandler(this);
    private Calculator calculator;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeHandler.onActivityCreate();
        if (Build.VERSION.SDK_INT >= 11) {
            setActionBarSubtitle();
        }
        setContentView(R.layout.activity_ndcalc);
        themeHandler.handleSystemUiVisibility(findViewById(R.id.screen));
        calculator = new Calculator(this, themeHandler);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        boolean uiWasUpdating = calculator.setUiIsUpdating(true);
        super.onRestoreInstanceState(savedInstanceState);
        calculator.setUiIsUpdating(uiWasUpdating);
        calculator.restoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(final Bundle savedInstanceState) {
        calculator.saveState(savedInstanceState);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ndcalc, menu);
        calculator.onCreateOptionsMenu(menu);
        themeHandler.onCreateOptionsMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if(calculator.onOptionsItemSelected(item))
            return true;
        switch(item.getItemId()) {
            case R.id.action_manage: {
                Intent intent = new Intent();
                intent.setClass(this, NDFilterListActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.action_config: {
                Intent intent = new Intent();
                intent.setClass(this, ConfigActivity.class);
                startActivity(intent);
                break;
            }
        }
        return result;
    }

    @Override
    public void onUserInteraction() {
        RingtoneStopper.stopRingtone(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        NotificationCanceler.cancelNotification(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        calculator.savePersistentState();
        isShowing = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        calculator.loadPersistentState();
        isShowing = true;
    }

    @TargetApi(11)
    private void setActionBarSubtitle() {
        final ActionBar ab = getActionBar();
        if(ab != null) {
            ab.setSubtitle(R.string.app_name_long);
        }
    }

    public static boolean isShowing;
}
