package de.hdodenhof.feedreader.fragments;

import java.sql.SQLException;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.dao.FeedsDataSource;
import de.hdodenhof.feedreader.model.Article;
import de.hdodenhof.feedreader.model.Feed;
import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DisplayArticleFragment extends Fragment {

    ArticleController articleController;

    public static DisplayArticleFragment newInstance(Long articleid) {
        DisplayArticleFragment f = new DisplayArticleFragment();

        Bundle args = new Bundle();
        args.putLong("articleid", articleid);
        f.setArguments(args);

        return f;
    }

    public Long getShownIndex() {
        return getArguments().getLong("articleid");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.article_fragment, container, false);

        if (getArguments() != null) {

            Long articleid = getArguments().getLong("articleid");
            articleController = new ArticleController(getActivity());

            Article article = articleController.getArticle(articleid);

            TextView header = (TextView) contentView.findViewById(R.id.article_header);
            header.setText(article.getTitle());

            TextView pubDate = (TextView) contentView.findViewById(R.id.article_pubdate);
            CharSequence formattedPubdate = DateFormat.format("E, dd MMM yyyy - kk:mm", article.getPubDate());
            pubDate.setText(formattedPubdate);            
            
            TextView text = (TextView) contentView.findViewById(R.id.article_text);
            text.setText(article.getFormatedContent());
            text.setMovementMethod(LinkMovementMethod.getInstance());
            
        }

        return contentView;

    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        View feedFragment = getActivity().findViewById(R.id.feed_fragment);
        boolean mDualPane = feedFragment != null;

        if (!mDualPane) {
            long feedId = getActivity().getIntent().getLongExtra("feedid", -1);
            FeedsDataSource fds = new FeedsDataSource(getActivity());
            try {
                fds.open();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            Feed feed = fds.getFeed(feedId);

            fds.close();

            ActionBar actionBar = getActivity().getActionBar();
            actionBar.setTitle(feed.getName());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

}
