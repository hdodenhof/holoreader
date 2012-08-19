package de.hdodenhof.feedreader.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.provider.RSSContentProvider;
import de.hdodenhof.feedreader.provider.SQLiteHelper.FeedDAO;

public class EditFeedDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private long mFeedID;
    private String mFeedName;
    private View mView;

    public EditFeedDialog() {
    }

    public EditFeedDialog(long id, String name, String url) {
        super();
        mFeedID = id;
        mFeedName = name;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater mLayoutInflater = LayoutInflater.from(getActivity());
        mView = mLayoutInflater.inflate(R.layout.fragment_dialog_edit, null);

        ((TextView) mView.findViewById(R.id.txt_feedname)).setText(mFeedName);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

        dialogBuilder.setView(mView);
        dialogBuilder.setCancelable(true);
        dialogBuilder.setPositiveButton(getResources().getString(R.string.PositiveButton), this);
        dialogBuilder.setNegativeButton(getResources().getString(R.string.NegativeButton), this);

        return dialogBuilder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            ContentResolver contentResolver = getActivity().getContentResolver();
            ContentValues contentValues = new ContentValues();

            contentValues.put(FeedDAO.NAME, ((TextView) mView.findViewById(R.id.txt_feedname)).getText().toString());

            contentResolver.update(RSSContentProvider.URI_FEEDS, contentValues, FeedDAO._ID + " = ?", new String[] { String.valueOf(mFeedID) });
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            dialog.dismiss();
            break;
        default:
            break;
        }
    }

}