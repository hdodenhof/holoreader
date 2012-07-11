package de.hdodenhof.feedreader.fragments;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.models.Article;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class ArticleFragment extends Fragment {

        @SuppressWarnings("unused")
        private static final String TAG = ArticleFragment.class.getSimpleName();

        private Article mArticle;

        public static ArticleFragment newInstance(){
                ArticleFragment mArticleFragmentInstance = new ArticleFragment();
                return mArticleFragmentInstance;
        }
        
        public static ArticleFragment newInstance(Article article) {
                ArticleFragment mArticleFragmentInstance = new ArticleFragment();
                mArticleFragmentInstance.mArticle = article;

                return mArticleFragmentInstance;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                
                Bundle args = getArguments();
                String title = args.getString(ArticleDAO.TITLE);                
                String content = args.getString(ArticleDAO.CONTENT);
                String pubdate= args.getString(ArticleDAO.PUBDATE);
                
                mArticle = new Article();
                mArticle.setTitle(title);
                mArticle.setContent(content);
                mArticle.setPubDate(SQLiteHelper.toDate(pubdate));
                
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View mContentView = inflater.inflate(R.layout.fragment_singlearticle, container, false);
                
                if (mArticle != null) {
                        int mViewWidth;

                        Display display = getActivity().getWindowManager().getDefaultDisplay();
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        display.getMetrics(displayMetrics);

                        if (((FragmentCallback) getActivity()).isDualPane()) {
                                // TODO Remove fixed value
                                mViewWidth = (int) Math.round(displayMetrics.widthPixels * 0.75);
                        } else {
                                mViewWidth = displayMetrics.widthPixels;
                        }

                        // TODO Remove fixed value
                        int mContentWidth = Math.round((mViewWidth - 40) / displayMetrics.density);

                        Document doc = Jsoup.parse(mArticle.getContent());
                        doc.head().append("<style type=\"text/css\">img { max-width: " + String.valueOf(mContentWidth) + "; height: auto}</style>");

                        TextView mHeader = (TextView) mContentView.findViewById(R.id.article_header);
                        mHeader.setText(mArticle.getTitle());

                        TextView mPubDate = (TextView) mContentView.findViewById(R.id.article_pubdate);
                        CharSequence mFormattedPubdate = DateFormat.format("E, dd MMM yyyy - kk:mm", mArticle.getPubDate());
                        mPubDate.setText(mFormattedPubdate);

                        WebView mText = (WebView) mContentView.findViewById(R.id.article_text);
                        mText.loadDataWithBaseURL(null, doc.html(), "text/html", "utf-8", null);

                        mContentView.scrollTo(0, 0);
                }

                return mContentView;

        }

}
