package de.hdodenhof.feedreader.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

import de.hdodenhof.feedreader.R;

public class CustomListFragment extends SherlockListFragment {

    private View mRootView;
    private ListView mListView;
    private TextView mEmptyView;
    private TextView mLoadingView;

    @Override
    public void setEmptyText(CharSequence text) {
        mEmptyView.setText(text);
        mListView.setEmptyView(mEmptyView);
    }

    public void setLoadingText(CharSequence text) {
        mLoadingView.setText(text);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_list, container, false);

        mListView = (ListView) mRootView.findViewById(android.R.id.list);
        mEmptyView = (TextView) mRootView.findViewById(R.id.empty);
        mLoadingView = (TextView) mRootView.findViewById(R.id.loading);

        mRootView.findViewById(R.id.listContainer).setVisibility(View.GONE);
        mRootView.findViewById(R.id.loadingContainer).setVisibility(View.VISIBLE);
        return mRootView;
    }

    public void setLoadingFinished() {
        mRootView.findViewById(R.id.loadingContainer).setVisibility(View.GONE);
        mRootView.findViewById(R.id.listContainer).setVisibility(View.VISIBLE);
    }
}
