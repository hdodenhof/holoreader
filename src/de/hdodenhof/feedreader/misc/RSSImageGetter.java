package de.hdodenhof.feedreader.misc;

import java.io.InputStream;
import java.net.URL;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.view.View;

public class RSSImageGetter implements ImageGetter {

        Context mContext;
        View mView;

        public RSSImageGetter(View view, Context context) {
                this.mContext = context;
                this.mView = view;
        }

        public Drawable getDrawable(String source) {
                try {
                        URL mURL = new URL(source);
                        InputStream mInputStream = mContext.openFileInput(mURL.toString().substring(mURL.toString().lastIndexOf("/")+1));
                        Drawable mDrawable = Drawable.createFromStream(mInputStream, "src");
                        mDrawable.setBounds(0, 0, 0 + mDrawable.getIntrinsicWidth(), 0 + mDrawable.getIntrinsicHeight());
                        return mDrawable;
                } catch (Exception e) {
                        return null;
                }
         
        }

}
