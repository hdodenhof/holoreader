package de.hdodenhof.feedreader.listadapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class RSSArticleAdapter extends SimpleCursorAdapter implements RSSAdapter {

    @SuppressWarnings("unused")
    private static final String TAG = RSSArticleAdapter.class.getSimpleName();

    private int mLayout;

    public RSSArticleAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mLayout = layout;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        prepareView(view, context, cursor);
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        return super.swapCursor(c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        final LayoutInflater inflater = LayoutInflater.from(context);
        View mView = inflater.inflate(mLayout, parent, false);

        return mView;
    }

    private View prepareView(View view, Context context, Cursor cursor) {
        String mTitle = cursor.getString(cursor.getColumnIndex(ArticleDAO.TITLE));
        String mSummary = cursor.getString(cursor.getColumnIndex(ArticleDAO.SUMMARY));
        int mRead = cursor.getInt(cursor.getColumnIndex(ArticleDAO.READ));

        final TextView mArticleTitle = (TextView) view.findViewById(R.id.list_item_entry_title);
        final TextView mArticleSummary = (TextView) view.findViewById(R.id.list_item_entry_summary);
        final TextView mArticleRead = (TextView) view.findViewById(R.id.list_item_entry_read);

        if (mArticleTitle != null) {
            mArticleTitle.setText(mTitle);
        }
        if (mArticleSummary != null) {
            mArticleSummary.setText(mSummary);
        }
        if (mArticleRead != null) {
            mArticleRead.setText(SQLiteHelper.toBoolean(mRead) ? "read" : "unread");
        }

        return view;
    }

    public int getType() {
        return RSSAdapter.TYPE_ARTICLE;
    }

}
