package de.hdodenhof.feedreader.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.models.Article;

/**
 * 
 * @author Henning Dodenhof
 *
 */
public class ArticleFragment extends Fragment {

        @SuppressWarnings("unused")
        private static final String TAG = ArticleFragment.class.getSimpleName();

        private Article mArticle;

        public static ArticleFragment newInstance(Article article) {
                ArticleFragment mArticleFragmentInstance = new ArticleFragment();
                mArticleFragmentInstance.mArticle = article;

                return mArticleFragmentInstance;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View mContentView = inflater.inflate(R.layout.fragment_singlearticle, container, false);

                if (mArticle != null) {
                        TextView mHeader = (TextView) mContentView.findViewById(R.id.article_header);
                        mHeader.setText(mArticle.getTitle());

                        TextView mPubDate = (TextView) mContentView.findViewById(R.id.article_pubdate);
                        CharSequence mFormattedPubdate = DateFormat.format("E, dd MMM yyyy - kk:mm", mArticle.getPubDate());
                        mPubDate.setText(mFormattedPubdate);

                        TextView mText = (TextView) mContentView.findViewById(R.id.article_text);
                        mText.setText(Html.fromHtml(mArticle.getContent()));
                        mText.setMovementMethod(LinkMovementMethod.getInstance());
                }

                return mContentView;

        }

}
