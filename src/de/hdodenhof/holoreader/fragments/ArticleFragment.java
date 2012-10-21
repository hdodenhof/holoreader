package de.hdodenhof.holoreader.fragments;

import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.misc.FragmentCallback;
import de.hdodenhof.holoreader.provider.SQLiteHelper;
import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;

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

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View articleView = inflater.inflate(R.layout.fragment_singlearticle, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            articleView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }

        TextView titleView = (TextView) articleView.findViewById(R.id.article_header);
        TextView pubdateView = (TextView) articleView.findViewById(R.id.article_pubdate);
        TextView feednameView = (TextView) articleView.findViewById(R.id.article_feedname);
        WebView contentView = (WebView) articleView.findViewById(R.id.article_text);

        titleView.setText(mTitle);
        pubdateView.setText(DateFormat.format("E, dd MMM yyyy - kk:mm", mPubdate));
        feednameView.setText(mFeedname);

        Document doc = Jsoup.parse(mContent);
        doc.head().append(customStyleElement());

        contentView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                articleView.postDelayed(new Runnable() {
                    public void run() {
                        articleView.findViewById(R.id.progressbar).setVisibility(View.INVISIBLE);
                    }
                }, 500);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("http://")) {
                    view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }
        });
        contentView.loadDataWithBaseURL(null, doc.html(), "text/html", "utf-8", null);

        return articleView;

    }

    private String customStyleElement() {
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

        return styleStringBuilder.toString();
    }
}
