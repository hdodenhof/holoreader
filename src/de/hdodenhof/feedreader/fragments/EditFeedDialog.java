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
    private String mFeedURL;
    private View mView;

    public EditFeedDialog() {
    }

    public EditFeedDialog(long id, String name, String url) {
        super();
        mFeedID = id;
        mFeedName = name;
        mFeedURL = url;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater mLayoutInflater = LayoutInflater.from(getActivity());
        mView = mLayoutInflater.inflate(R.layout.fragment_edit_feed_dialog, null);

        ((TextView) mView.findViewById(R.id.txt_feedname)).setText(mFeedName);
        ((TextView) mView.findViewById(R.id.txt_feedurl)).setText(mFeedURL);

        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(getActivity());
        mDialogBuilder.setTitle("Edit Feed");
        mDialogBuilder.setView(mView);
        mDialogBuilder.setCancelable(true);
        mDialogBuilder.setPositiveButton(getResources().getString(R.string.PositiveButton), this);
        mDialogBuilder.setNegativeButton(getResources().getString(R.string.NegativeButton), this);

        return mDialogBuilder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            ContentResolver mContentResolver = getActivity().getContentResolver();
            ContentValues mContentValues = new ContentValues();

            mContentValues.put(FeedDAO.NAME, ((TextView) mView.findViewById(R.id.txt_feedname)).getText().toString());
            mContentValues.put(FeedDAO.URL, ((TextView) mView.findViewById(R.id.txt_feedurl)).getText().toString());

            mContentResolver.update(RSSContentProvider.URI_FEEDS, mContentValues, FeedDAO._ID + " = ?", new String[] { String.valueOf(mFeedID) });
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            dialog.dismiss();
            break;
        default:
            break;
        }
    }

}