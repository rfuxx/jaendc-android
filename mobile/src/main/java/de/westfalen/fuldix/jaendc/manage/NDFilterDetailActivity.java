package de.westfalen.fuldix.jaendc.manage;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;

import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.model.NDFilter;

/**
 * An activity representing a single ND Filter detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link NDFilterListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link NDFilterDetailFragment}.
 */
@TargetApi(11)
public class NDFilterDetailActivity extends Activity implements NDFilterDetailFragment.Callbacks {
    private NDFilterDetailFragment fragment;
    private boolean hasItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ndfilter_detail);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            Intent intent = getIntent();
            hasItemId = intent.hasExtra(NDFilterDetailFragment.ARG_ITEM_ID);
            if(hasItemId) {
                long argItemId = intent.getLongExtra(NDFilterDetailFragment.ARG_ITEM_ID, 0);
                arguments.putLong(NDFilterDetailFragment.ARG_ITEM_ID, argItemId);
            } else {
                getActionBar().setTitle(R.string.title_ndfilter_detail_new);
            }
            fragment = new NDFilterDetailFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .add(R.id.ndfilter_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        if(!hasItemId) {
            menu.removeItem(R.id.action_delete);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.action_delete:
                if(fragment != null) {
                    fragment.actionDelete();
                }
                return true;

            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                backToListActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void backToListActivity() {
        startActivity(new Intent(this, NDFilterListActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Override
    public void onDetailDeleteDone() {
        backToListActivity();
    }

    @Override
    public void onEditSaved(NDFilter filter) {
        // nop (editing inplace and saving on every valid keystroke)
    }
}
