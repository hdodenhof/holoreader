package de.hdodenhof.feedreader.fragments;

import java.util.ArrayList;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.helpers.SQLiteHelper.ArticleDAO;
import de.hdodenhof.feedreader.listadapters.RSSArticleAdapter;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.models.Feed;
import de.hdodenhof.feedreader.providers.RSSContentProvider;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class ArticleListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

        @SuppressWarnings("unused")
        private static final String TAG = ArticleListFragment.class.getSimpleName();

        private ListView mArticlesListView;
        private RSSArticleAdapter mArticleAdapter;
        private boolean mInitialized = false;

        private String mCursorFilter;
        private boolean mScrollTop = false;

        public void updateFeedlist(ArrayList<Feed> feeds) {
                getLoaderManager().restartLoader(0, null, this);
        }

        public void selectFeed(int feedID) {
                mCursorFilter = "feed/" + String.valueOf(feedID);
                mScrollTop = true;
                getLoaderManager().restartLoader(0, null, this);
        }

        public void changePosition(int position) {
                if (mInitialized) {
                        if (mArticlesListView.getCheckedItemPosition() != position) {
                                int mPosition = (position - 1 < 0) ? 0 : position - 1;
                                mArticlesListView.smoothScrollToPositionFromTop(mPosition, 0, 500);
                                mArticlesListView.setItemChecked(position, true);
                        }
                }
        }

        public void setChoiceModeSingle() {
                mArticlesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        
        public void refreshList() {
                getLoaderManager().restartLoader(0, null, this);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
                super.onActivityCreated(savedInstanceState);

                if (savedInstanceState != null) {

                }
                
                if(getActivity().getIntent().hasExtra("feedid")){
                        mCursorFilter = "feed/" + getActivity().getIntent().getIntExtra("feedid", 0);
                }

                String[] uiBindFrom = { ArticleDAO.TITLE, ArticleDAO.SUMMARY, ArticleDAO.READ };
                int[] uiBindTo = { R.id.list_item_entry_title, R.id.list_item_entry_summary, R.id.list_item_entry_read };

                getLoaderManager().initLoader(0, null, this);

                mArticleAdapter = new RSSArticleAdapter(getActivity(), R.layout.listitem_article, null, uiBindFrom, uiBindTo,
                                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

                this.setEmptyText("No articles");
                this.setListAdapter(mArticleAdapter);
                mArticlesListView = getListView();

                mArticlesListView.setOnItemClickListener((OnItemClickListener) getActivity());

                mInitialized = true;
                ((FragmentCallback) getActivity()).onFragmentReady(this);

        }

        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                Uri mBaseUri;
                if (mCursorFilter != null) {
                        mBaseUri = Uri.withAppendedPath(RSSContentProvider.URI_ARTICLES, mCursorFilter);
                } else {
                        mBaseUri = RSSContentProvider.URI_ARTICLES;
                }

                String[] mProjection = { ArticleDAO._ID, ArticleDAO.FEEDID, ArticleDAO.TITLE, ArticleDAO.SUMMARY, ArticleDAO.READ };
                CursorLoader mCursorLoader = new CursorLoader(getActivity(), mBaseUri, mProjection, null, null, null);
                return mCursorLoader;
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                mArticleAdapter.swapCursor(data);
                if (mScrollTop) {
                        mArticlesListView.scrollTo(0, 0);
                        mScrollTop = false;
                }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
                mArticleAdapter.swapCursor(null);
        }
}
