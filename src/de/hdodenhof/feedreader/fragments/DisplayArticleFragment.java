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

    private ArticleController articleController;
    private long articleId;
    private ParameterProvider mParameterProvider;

    public interface ParameterProvider {
        public long getArticleId();
    }        
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        mParameterProvider = (ParameterProvider) activity;
    }       
    
    public static DisplayArticleFragment newInstance(Long articleId) {
        DisplayArticleFragment instance = new DisplayArticleFragment();
        instance.articleId = articleId;

        return instance;
    }

    public DisplayArticleFragment() {
        this.articleId = -1;
    }

    public Long getShownIndex() {
        return this.articleId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        articleController = new ArticleController(getActivity());
        
        if(this.articleId == -1){
            this.articleId = mParameterProvider.getArticleId();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_singlearticle, container, false);

        if (articleId != -1) {
            Article article = articleController.getArticle(articleId);

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
    }
}
