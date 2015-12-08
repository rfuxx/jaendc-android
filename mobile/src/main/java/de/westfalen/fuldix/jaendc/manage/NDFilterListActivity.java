package de.westfalen.fuldix.jaendc.manage;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import de.westfalen.fuldix.jaendc.NDCalculatorActivity;
import de.westfalen.fuldix.jaendc.NDFilterAdapter;
import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.db.NDFilterDAO;
import de.westfalen.fuldix.jaendc.model.NDFilter;
import de.westfalen.fuldix.jaendc.widget.AppWidget;


/**
 * An activity representing a list of ND Filters. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link NDFilterDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link NDFilterListFragment} and the item details
 * (if present) is a {@link NDFilterDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link NDFilterListFragment.Callbacks} interface
 * to listen for item selections.
 */
@TargetApi(11)
public class NDFilterListActivity extends Activity
        implements NDFilterListFragment.Callbacks, NDFilterDetailFragment.Callbacks {

    private static final int ACT_EDIT_DETAIL = 101;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private NDFilterDetailFragment currentFragment;
    private ActionMode actionMode;
    private boolean itemHasBeenChanged;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ndfilter_list);
        // Show the Up button in the action bar.
        final ActionBar ab = getActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        if (findViewById(R.id.ndfilter_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((NDFilterListFragment) getFragmentManager()
                    .findFragmentById(R.id.ndfilter_list))
                    .setActivateOnItemClick();
        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_manage, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add:
                final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
                final ListView lv = fr.getListView();
                lv.setItemChecked(lv.getCheckedItemPosition(), false);
                doDetail(null);
                return true;

            case R.id.action_list_delete:
                actionDelete();
                return true;

            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                startActivity(new Intent(this, NDCalculatorActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback method from {@link NDFilterListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(final NDFilter filter) {
        doDetail(filter);
    }

    private void doDetail(final NDFilter filter) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            final Bundle arguments = new Bundle();
            if(filter != null) {
                arguments.putLong(NDFilterDetailFragment.ARG_ITEM_ID, filter.getId());
            }
            final NDFilterDetailFragment fragment = new NDFilterDetailFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .replace(R.id.ndfilter_detail_container, fragment)
                    .commit();
            currentFragment = fragment;

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            if(actionMode != null) {
                updateActionModeSubtitle(actionMode);
            } else {
                final Intent detailIntent = new Intent(this, NDFilterDetailActivity.class);
                if (filter != null) {
                    detailIntent.putExtra(NDFilterDetailFragment.ARG_ITEM_ID, filter.getId());
                }
                startActivityForResult(detailIntent, ACT_EDIT_DETAIL);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(itemHasBeenChanged) {
            AppWidget.notifyAppWidgetDataChange(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        itemHasBeenChanged = false;
    }

    @Override
    protected void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
        if(requestCode == ACT_EDIT_DETAIL) {
            if(data != null && data.hasExtra(NDFilterDetailFragment.RESULT_ITEM_DELETED)) {
                final long itemDeleted = data.getLongExtra(NDFilterDetailFragment.RESULT_ITEM_DELETED, Long.MIN_VALUE);
                // filter was deleted in child activity
                final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
                final NDFilterAdapter la = fr.getListAdapter();
                la.removeId(itemDeleted);
            }
        }
    }

    private void actionDelete() {
        if (mTwoPane) {
            // In two-pane mode, delete the currently selected filter
            // which is displayed in the detail pane
            final NDFilterDetailFragment fr = (NDFilterDetailFragment) getFragmentManager().findFragmentById(R.id.ndfilter_detail_container);
            if(fr != null) {
                fr.actionDelete(); // this will call onDetailDeleteDone when done.
            }
        } else {
            // in single-pane mode... use ActionMode
            // to allow selection of filters for deletion
            actionMode = startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.menu_mode_delete, menu);
                    final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
                    fr.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                    fr.getListAdapter().setIndicateDragable(false);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    mode.setTitle(R.string.action_mode_title);
                    updateActionModeSubtitle(mode);
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch(item.getItemId()) {
                        case R.id.action_select_all: {
                            final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
                            final ListView lv = fr.getListView();
                            int numCheckedItems = getNumCheckedItems();
                            boolean newState = numCheckedItems != lv.getCount();
                            for(int i=lv.getCount()-1; i>=0; i--) {
                                lv.setItemChecked(i, newState);
                            }
                            updateActionModeSubtitle(mode);
                            break;
                        }
                        case R.id.action_mode_delete: {
                            confirmDelete();
                            break;
                        }
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
                    final ListView lv = fr.getListView();
                    final NDFilterAdapter la = fr.getListAdapter();
                    // quirks starting...
                    int p = lv.getFirstVisiblePosition();
                    lv.setAdapter(la);
                    lv.setSelection(p);
                    // quirks above because on changing choicemode the list does not visually clear the choices
                    lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
                    la.setIndicateDragable(true);
                    actionMode = null;
                }

                private void confirmDelete() {
                    final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
                    final ListView lv = fr.getListView();
                    final NDFilterAdapter la = fr.getListAdapter();
                    final SparseBooleanArray states = lv.getCheckedItemPositions();
                    int numFiltersToDelete = getNumCheckedItems();
                    if(numFiltersToDelete > 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(NDFilterListActivity.this);
                        builder.setTitle(R.string.confirm_delete_title);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        final String msg;
                        if(numFiltersToDelete > 1) {
                            msg = getResources().getQuantityString(R.plurals.confirm_delete_multiple_filter, numFiltersToDelete, numFiltersToDelete);
                        } else {
                            int pos=-1;
                            for(int i = la.getCount()-1; i>=0; i--) {
                                if(states.get(i)) {
                                    pos = i;
                                    break;
                                }
                            }
                            String filterName = la.getItem(pos).getName().trim();
                            if (filterName.equals("")) {
                                filterName = getString(R.string.confirm_delete_this_filter);
                            }
                            msg = String.format(getString(R.string.confirm_delete), filterName);
                        }
                        builder.setMessage(msg);
                        builder.setCancelable(true);
                        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                NDFilterDAO dao = new NDFilterDAO(NDFilterListActivity.this);
                                dao.openWritable();
                                SparseBooleanArray states = lv.getCheckedItemPositions();
                                for(int i=la.getCount()-1; i >= 0; i--) {
                                    if(states.get(i)) {
                                        NDFilter filter = la.getItem(i);
                                        dao.deleteNDFilter(filter);
                                        la.removeAtPos(i);
                                    }
                                }
                                dao.close();
                                actionMode.finish();
                                dialog.dismiss();
                                itemHasBeenChanged = true;
                            }
                        });
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();
                    }
                }
            });
        }
    }

    @Override
    public void onDetailDeleteDone(final NDFilter filter) {
        if(currentFragment != null) {
            getFragmentManager().beginTransaction().remove(currentFragment).commit();
            currentFragment = null;
        }
        final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
        final NDFilterAdapter la = fr.getListAdapter();
        final ListView lv = fr.getListView();
        final int pos = lv.getCheckedItemPosition();
        if(pos >= 0) {
            lv.setItemChecked(pos, false);
            la.removeAtPos(pos);
        }
        itemHasBeenChanged = true;
    }

    @Override
    public void onEditSaved(NDFilter filter) {
        final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
        final NDFilterAdapter la = fr.getListAdapter();
        final int pos = la.putChange(filter);
        fr.getListView().setSelection(pos);
        itemHasBeenChanged = true;
    }

    @Override
    public void onItemOrderChanged() {
        itemHasBeenChanged = true;
    }

    private int getNumCheckedItems() {
        final NDFilterListFragment fr = (NDFilterListFragment) getFragmentManager().findFragmentById(R.id.ndfilter_list);
        final ListView lv = fr.getListView();
        final NDFilterAdapter la = fr.getListAdapter();
        final SparseBooleanArray states = lv.getCheckedItemPositions();
        int numCheckedItems = 0;
        for(int i=la.getCount()-1; i>=0; i--) {
            if(states.get(i)) {
                numCheckedItems++;
            }
        }
        return numCheckedItems;
    }

    private void updateActionModeSubtitle(final ActionMode mode) {
        final int numCheckedItems = getNumCheckedItems();
        mode.setSubtitle(getResources().getQuantityString(R.plurals.action_mode_subtitle, numCheckedItems, numCheckedItems));
        MenuItem deleteAction = mode.getMenu().findItem(R.id.action_mode_delete);
        deleteAction.setEnabled(numCheckedItems > 0);
    }
}
