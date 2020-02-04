package de.westfalen.fuldix.jaendc.manage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.StyleHelper;
import de.westfalen.fuldix.jaendc.db.NDFilterDAO;
import de.westfalen.fuldix.jaendc.model.NDFilter;

/**
 * A fragment representing a single ND Filter detail screen.
 * This fragment is either contained in a {@link NDFilterListActivity}
 * in two-pane mode (on tablets) or a {@link NDFilterDetailActivity}
 * on handsets.
 */
@TargetApi(11)
public class NDFilterDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "filter_id";
    public static final String RESULT_ITEM_DELETED = "item_deleted_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private NDFilterDAO dao;
    private NDFilter mItem;
    private Callbacks callbacks;
    private NDFilterEditController controller;

    public interface Callbacks {
        void onDetailDeleteDone(final NDFilter filter);
        void onEditSaved(final NDFilter filter);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public NDFilterDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        callbacks = (Callbacks) activity;
        dao = new NDFilterDAO(activity);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItem = dao.getNDFilter(getArguments().getLong(ARG_ITEM_ID));
        } else {
            mItem = new NDFilter();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ndfilter_detail, container, false);
        controller = new NDFilterEditController(rootView, mItem, dao, callbacks);
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(controller != null) {
            controller.saveIfPossible();
        }
    }

    public void actionDelete() {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.confirm_delete_title);
        StyleHelper.setDialogIcon(builder, activity, R.drawable.ic_dialog_alert_tinted, R.attr.dialogIconTint);

        String filterName = mItem.getName().trim();
        if(filterName.equals("")) {
            filterName = activity.getString(R.string.confirm_delete_this_filter);
        }
        String msg = String.format(activity.getString(R.string.confirm_delete), filterName);
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                controller.willDelete();
                dao.deleteNDFilter(mItem);
                callbacks.onDetailDeleteDone(mItem);
                dialog.dismiss();
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
