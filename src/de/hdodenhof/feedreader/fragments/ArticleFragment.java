package de.hdodenhof.feedreader.fragments;

import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.misc.FragmentCallback;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class ArticleFragment extends SherlockFragment {

    @SuppressWarnings("unused")
    private static final String TAG = ArticleFragment.class.getSimpleName();

    private String mTitle;
    private String mContent;
    private String mFeedname;
    private Date mPubdate;

    public static ArticleFragment newInstance() {
        ArticleFragment mArticleFragmentInstance = new ArticleFragment();
        return mArticleFragmentInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mTitle = args.getString(ArticleDAO.TITLE);
        mContent = args.getString(ArticleDAO.CONTENT);
        mFeedname = args.getString(ArticleDAO.FEEDNAME);
        mPubdate = SQLiteHelper.toDate(args.getString(ArticleDAO.PUBDATE));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View mArticleView = inflater.inflate(R.layout.fragment_singlearticle, container, false);

        if (mTitle != null && mContent != null && mPubdate != null) {
            int mViewWidth;

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            if (((FragmentCallback) getActivity()).isDualPane()) {
                // TODO Remove fixed value
                mViewWidth = (int) Math.round(displayMetrics.widthPixels * 0.7);
            } else {
                mViewWidth = displayMetrics.widthPixels;
            }

            // TODO Remove fixed value
            int mContentWidth = Math.round(mViewWidth / displayMetrics.density) - 16;

            Document doc = Jsoup.parse(mContent);
            doc.head().append("<style type=\"text/css\">img { max-width: " + String.valueOf(mContentWidth) + "; height: auto}</style>");

            TextView mTitleView = (TextView) mArticleView.findViewById(R.id.article_header);
            mTitleView.setText(mTitle);

            TextView mPubdateView = (TextView) mArticleView.findViewById(R.id.article_pubdate);
            CharSequence mFormattedPubdate = DateFormat.format("E, dd MMM yyyy - kk:mm", mPubdate);
            mPubdateView.setText(mFormattedPubdate);

            TextView mFeednameView = (TextView) mArticleView.findViewById(R.id.article_feedname);
            mFeednameView.setText(mFeedname);

            WebView mContentView = (WebView) mArticleView.findViewById(R.id.article_text);

            mContentView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    mArticleView.postDelayed(new Runnable() {
                        public void run() {
                            mArticleView.findViewById(R.id.progressbar).setVisibility(View.INVISIBLE);
                            mArticleView.scrollTo(0, 0);
                        }
                    }, 500);
                }
            });
            mContentView.loadDataWithBaseURL(null, doc.html(), "text/html", "utf-8", null);
        }
        return mArticleView;

    }

    private boolean isConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        return mNetworkInfo == null ? false : mNetworkInfo.isAvailable();
    }

}
