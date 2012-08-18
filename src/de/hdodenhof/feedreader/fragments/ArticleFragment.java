package de.hdodenhof.feedreader.fragments;

import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
        return new ArticleFragment();
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
        final View articleView = inflater.inflate(R.layout.fragment_singlearticle, container, false);

        if (mTitle != null && mContent != null && mPubdate != null) {
            int viewWidth;

            Display display = getActivity().getWindowManager().getDefaultDisplay();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            if (((FragmentCallback) getActivity()).isDualPane()) {
                float factor = ((float) getResources().getInteger(R.integer.dualpane_feedactivity_article_weight)) / 100;
                viewWidth = (int) Math.round(displayMetrics.widthPixels * factor);
            } else {
                viewWidth = displayMetrics.widthPixels;
            }

            // content margin is 8dp left and 8dp right
            int contentWidth = Math.round(viewWidth / displayMetrics.density) - 16;

            StringBuilder styleStringBuilder = new StringBuilder();
            styleStringBuilder.append("<style type=\"text/css\">");
            styleStringBuilder.append("body { padding: 0; margin: 0; }");
            styleStringBuilder.append("img { max-width: " + String.valueOf(contentWidth) + "; height: auto; }");
            styleStringBuilder.append("figure { margin: 0 !important; }");
            styleStringBuilder.append("</style>");

            Document doc = Jsoup.parse(mContent);
            doc.head().append(styleStringBuilder.toString());

            TextView titleView = (TextView) articleView.findViewById(R.id.article_header);
            titleView.setText(mTitle);

            TextView pubdateView = (TextView) articleView.findViewById(R.id.article_pubdate);
            CharSequence formattedPubdate = DateFormat.format("E, dd MMM yyyy - kk:mm", mPubdate);
            pubdateView.setText(formattedPubdate);

            TextView feednameView = (TextView) articleView.findViewById(R.id.article_feedname);
            feednameView.setText(mFeedname);

            WebView contentView = (WebView) articleView.findViewById(R.id.article_text);

            contentView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    articleView.postDelayed(new Runnable() {
                        public void run() {
                            articleView.findViewById(R.id.progressbar).setVisibility(View.INVISIBLE);
                        }
                    }, 500);
                }
            });
            contentView.loadDataWithBaseURL(null, doc.html(), "text/html", "utf-8", null);
        }
        return articleView;

    }
}
