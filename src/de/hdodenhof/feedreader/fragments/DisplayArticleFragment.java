package de.hdodenhof.feedreader.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.controller.ArticleController;
import de.hdodenhof.feedreader.model.Article;

public class DisplayArticleFragment extends Fragment {

        private ArticleController mArticleController;
        private ActivityConnector mActivityConnector;
        private long mArticleID;

        public interface ActivityConnector {
                public long getArticleId();
        }

        @Override
        public void onAttach(Activity activity) {
                super.onAttach(activity);

                mActivityConnector = (ActivityConnector) activity;
        }

        public static DisplayArticleFragment newInstance(Long articleId) {
                DisplayArticleFragment mArticleFragmentInstance = new DisplayArticleFragment();
                mArticleFragmentInstance.mArticleID = articleId;

                return mArticleFragmentInstance;
        }

        public DisplayArticleFragment() {
                this.mArticleID = -1;
        }

        public Long getShownIndex() {
                return this.mArticleID;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                mArticleController = new ArticleController(getActivity());

                if (this.mArticleID == -1) {
                        this.mArticleID = mActivityConnector.getArticleId();
                }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View mContentView = inflater.inflate(R.layout.fragment_singlearticle, container, false);

                if (mArticleID != -1) {
                        Article mArticle = mArticleController.getArticle(mArticleID);

                        TextView mHeader = (TextView) mContentView.findViewById(R.id.article_header);
                        mHeader.setText(mArticle.getTitle());

                        TextView mPubDate = (TextView) mContentView.findViewById(R.id.article_pubdate);
                        CharSequence mFormattedPubdate = DateFormat.format("E, dd MMM yyyy - kk:mm", mArticle.getPubDate());
                        mPubDate.setText(mFormattedPubdate);

                        TextView mText = (TextView) mContentView.findViewById(R.id.article_text);
                        mText.setText(mArticle.getFormatedContent());
                        mText.setMovementMethod(LinkMovementMethod.getInstance());
                }

                return mContentView;

        }

}
