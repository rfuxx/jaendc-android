package de.westfalen.fuldix.jaendc;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import de.westfalen.fuldix.jaendc.manage.NDFilterListActivity;

public class NDCalculatorActivity extends ThemedActivityWithActionBarSqueezer {
    private Calculator calculator;

    public NDCalculatorActivity() {
        super(R.id.screen);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ndcalc);
        handleSystemUiVisibility(findViewById(R.id.screen));
        calculator = new Calculator(this);
        if (Build.VERSION.SDK_INT >= 11) {
            final ActionBar ab = getActionBar();
            if(ab != null) {
                ab.setSubtitle(R.string.app_name_long);
            }
        }
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

    @Override
    public void onDestroy() {
        calculator.onDestroy();
        super.onDestroy();
    }

    public static boolean isShowing;
}
