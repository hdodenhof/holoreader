package de.hdodenhof.feedreader.listadapters;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.provider.SQLiteHelper;
import de.hdodenhof.feedreader.provider.SQLiteHelper.ArticleDAO;

/**
 * 
 * @author Henning Dodenhof
 * 
 */
public class RSSArticleAdapter extends SimpleCursorAdapter implements RSSAdapter {

    @SuppressWarnings("unused")
    private static final String TAG = RSSArticleAdapter.class.getSimpleName();

    public static final int MODE_COMPACT = 1;
    public static final int MODE_EXTENDED = 2;

    private static final int ALPHA_STATE_DIMMED = 128;
    private static final int ALPHA_STATE_FULL = 255;

    private int mLayout;
    private int mMode;
    private LruCache<String, Bitmap> mImageCache;

    public RSSArticleAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags, int mode) {
        super(context, layout, c, from, to, flags);
        mLayout = layout;
        mMode = mode;

        final int mMemoryClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        final int mCacheSize = 1024 * 1024 * mMemoryClass / 8;

        mImageCache = new LruCache<String, Bitmap>(mCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String mTitle = cursor.getString(cursor.getColumnIndex(ArticleDAO.TITLE));
        String mSummary = cursor.getString(cursor.getColumnIndex(ArticleDAO.SUMMARY));
        String mPubdate = formatToYesterdayOrToday(SQLiteHelper.toDate(cursor.getString(cursor.getColumnIndex(ArticleDAO.PUBDATE))));
        String mImageURL = cursor.getString(cursor.getColumnIndex(ArticleDAO.IMAGE));
        int mRead = cursor.getInt(cursor.getColumnIndex(ArticleDAO.READ));

        final TextView mArticleTitle = (TextView) view.findViewById(R.id.list_item_entry_title);
        final TextView mArticleSummary = (TextView) view.findViewById(R.id.list_item_entry_summary);
        final TextView mArticlePubdate = (TextView) view.findViewById(R.id.list_item_entry_date);
        final ImageView mArticleImage = (ImageView) view.findViewById(R.id.list_item_entry_image);

        if (mArticleTitle != null) {
            mArticleTitle.setText(mTitle);
        }
        if (mMode == MODE_EXTENDED && mArticleSummary != null) {
            mArticleSummary.setText(mSummary);
            mArticleSummary.setVisibility(View.VISIBLE);
            if (mArticleTitle != null) {
                mArticleTitle.setMaxLines(1);
            }
        }
        if (mArticlePubdate != null) {
            mArticlePubdate.setText(mPubdate);
        }

        if (SQLiteHelper.toBoolean(mRead)) {
            mArticleImage.setAlpha(ALPHA_STATE_DIMMED);
            dim(mArticleTitle);
            dim(mArticleSummary);
            dim(mArticlePubdate);
        } else {
            mArticleImage.setAlpha(ALPHA_STATE_FULL);
            lightup(mArticleTitle);
            lightup(mArticleSummary);
            lightup(mArticlePubdate);
        }

        if (mArticleImage != null && mImageURL != null && mImageURL != "") {
            Bitmap mImage = mImageCache.get(mImageURL);
            if (mImage == null) {
                if (cancelPotentialDownload(mImageURL, mArticleImage)) {
                    ImageDownloaderTask mTask = new ImageDownloaderTask(mArticleImage);
                    DownloadedDrawable mDownloadedDrawable = new DownloadedDrawable(mTask);
                    mArticleImage.setImageDrawable(mDownloadedDrawable);
                    mTask.execute(mImageURL);
                }
            } else {
                cancelPotentialDownload(mImageURL, mArticleImage);
                mArticleImage.setImageBitmap(mImage);
            }
        }
    }

    private void dim(TextView view) {
        ColorStateList mColors = view.getTextColors();
        view.setTextColor(mColors.withAlpha(ALPHA_STATE_DIMMED));
    }

    private void lightup(TextView view) {
        ColorStateList mColors = view.getTextColors();
        view.setTextColor(mColors.withAlpha(ALPHA_STATE_FULL));
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        return super.swapCursor(c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater mInflater = LayoutInflater.from(context);
        View mView = mInflater.inflate(mLayout, parent, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            TypedArray mAttributes = context.obtainStyledAttributes(new int[] { android.R.attr.activatedBackgroundIndicator });
            int mResource = mAttributes.getResourceId(0, 0);
            mAttributes.recycle();

            // setBackgroundResource resets padding
            int mLeftPadding = mView.getPaddingLeft();
            int mTopPadding = mView.getPaddingTop();
            int mRightPadding = mView.getPaddingRight();
            int mBottomPadding = mView.getPaddingBottom();
            mView.setBackgroundResource(mResource);
            mView.setPadding(mLeftPadding, mTopPadding, mRightPadding, mBottomPadding);
        }

        return mView;
    }

    public int getType() {
        return RSSAdapter.TYPE_ARTICLE;
    }

    private String formatToYesterdayOrToday(Date date) {
        Calendar mToday = Calendar.getInstance();
        Calendar mYesterday = Calendar.getInstance();
        mYesterday.add(Calendar.DATE, -1);
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTime(date);

        SimpleDateFormat mTimeFormatter = new SimpleDateFormat("kk:mm");

        if (mCalendar.get(Calendar.YEAR) == mToday.get(Calendar.YEAR) && mCalendar.get(Calendar.DAY_OF_YEAR) == mToday.get(Calendar.DAY_OF_YEAR)) {
            return "Today, " + mTimeFormatter.format(date);
        } else if (mCalendar.get(Calendar.YEAR) == mYesterday.get(Calendar.YEAR) && mCalendar.get(Calendar.DAY_OF_YEAR) == mYesterday.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday, " + mTimeFormatter.format(date);
        } else {
            return DateFormat.format("MMM dd, kk:mm", date).toString();
        }
    }

    public class ImageDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;
        private String mURL;
        private int mImageDimension;

        public ImageDownloaderTask(ImageView imageView) {
            mImageViewReference = new WeakReference<ImageView>(imageView);
            mImageDimension = (int) (120 * mContext.getResources().getDisplayMetrics().density + 0.5f);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            mURL = params[0];
            try {
                Bitmap mBitmap = BitmapFactory.decodeStream(new URL(mURL).openConnection().getInputStream());
                float originalWidth = mBitmap.getWidth();
                float originalHeight = mBitmap.getHeight();

                if (originalHeight > mImageDimension) {
                    int newHeight = mImageDimension;
                    int newWidth = (int) ((newHeight / originalHeight) * originalWidth);
                    mBitmap = Bitmap.createScaledBitmap(mBitmap, newWidth, newHeight, true);
                }

                return mBitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (bitmap != null) {
                synchronized (mImageCache) {
                    if (mImageCache.get(mURL) == null) {
                        mImageCache.put(mURL, bitmap);
                    }
                }
            }

            if (mImageViewReference != null) {
                ImageView mImageView = mImageViewReference.get();
                ImageDownloaderTask mImageDownloaderTask = getImageDownloaderTask(mImageView);
                if (this == mImageDownloaderTask) {
                    mImageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        ImageDownloaderTask mImageDownloaderTask = getImageDownloaderTask(imageView);

        if (mImageDownloaderTask != null) {
            String mBitmapUrl = mImageDownloaderTask.mURL;
            if ((mBitmapUrl == null) || (!mBitmapUrl.equals(url))) {
                mImageDownloaderTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private static ImageDownloaderTask getImageDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable mDrawable = imageView.getDrawable();
            if (mDrawable instanceof DownloadedDrawable) {
                DownloadedDrawable mDownloadedDrawable = (DownloadedDrawable) mDrawable;
                return mDownloadedDrawable.getImageDownloaderTask();
            }
        }
        return null;
    }

    private static class DownloadedDrawable extends ColorDrawable {
        private final WeakReference<ImageDownloaderTask> mImageDownloaderTaskReference;

        public DownloadedDrawable(ImageDownloaderTask imageDownloaderTask) {
            super(Color.TRANSPARENT);
            mImageDownloaderTaskReference = new WeakReference<ImageDownloaderTask>(imageDownloaderTask);
        }

        public ImageDownloaderTask getImageDownloaderTask() {
            return mImageDownloaderTaskReference.get();
        }
    }

}
