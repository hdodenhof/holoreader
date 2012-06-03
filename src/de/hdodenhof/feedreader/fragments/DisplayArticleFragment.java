package de.hdodenhof.feedreader.fragments;

import java.sql.SQLException;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.dao.ArticlesDataSource;
import de.hdodenhof.feedreader.model.Article;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DisplayArticleFragment extends Fragment {

    private ArticlesDataSource articlesdatasource;
    
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

            articlesdatasource = new ArticlesDataSource(getActivity());
            try {
                articlesdatasource.open();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            Article article = articlesdatasource.getArticle(articleid);

            TextView header = (TextView) contentView.findViewById(R.id.article_header);
            header.setText(article.getTitle());

            TextView text = (TextView) contentView.findViewById(R.id.article_text);
            text.setMovementMethod(LinkMovementMethod.getInstance());
            text.setText(article.getFormatedContent());
        }
        
        return contentView;

    }
    
    @Override
    public void onActivityCreated(Bundle savedState){
        super.onActivityCreated(savedState);
    }

}
