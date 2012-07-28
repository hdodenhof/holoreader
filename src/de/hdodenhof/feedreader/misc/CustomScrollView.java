package de.hdodenhof.feedreader.misc;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.widget.ScrollView;

public class CustomScrollView extends ScrollView {

    public CustomScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomScrollView(Context context) {
        super(context);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (focused instanceof WebView) {
            return;
        } else {
            super.requestChildFocus(child, focused);
        }
    }
}
