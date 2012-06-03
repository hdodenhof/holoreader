package de.hdodenhof.feedreader.fragments;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.model.Article;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DisplayArticleFragment extends Fragment {

    public static DisplayArticleFragment newInstance(Article article) {
        DisplayArticleFragment f = new DisplayArticleFragment();

        Bundle args = new Bundle();
        args.putParcelable("article", article);
        f.setArguments(args);

        return f;
    }

    public Article getShownIndex() {
        return getArguments().getParcelable("article");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.article_fragment, container, false);

        if (getArguments() != null) {

            Article article = getArguments().getParcelable("article");

            TextView header = (TextView) contentView.findViewById(R.id.article_header);
            header.setText(article.getTitle());

            TextView text = (TextView) contentView.findViewById(R.id.article_text);
            text.setMovementMethod(LinkMovementMethod.getInstance());
            text.setText(article.getFormatedFull());
        }
        
        return contentView;

    }
    
    @Override
    public void onActivityCreated(Bundle savedState){
        super.onActivityCreated(savedState);
    }

}
