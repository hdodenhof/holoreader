package de.hdodenhof.feedreader.listadapters;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.hdodenhof.feedreader.R;
import de.hdodenhof.feedreader.misc.DiskLruImageCache;
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

    private static final int LAYOUT_MODE_COMPACT = R.layout.listitem_article;
    private static final int LAYOUT_MODE_EXTENDED = R.layout.listitem_article_extended;

    private static final int ALPHA_STATE_DIMMED = 128;
    private static final int ALPHA_STATE_FULL = 255;

    private int mLayout;
    private int mMode;
    private boolean mIsModeExtendedPossible;
    private LruCache<String, Bitmap> mImageCache;
    private DiskLruImageCache mDiskImageCache;
    private Context mContext;

    public RSSArticleAdapter(Context context, Cursor c, String[] from, int[] to, int flags, int currentMode, boolean isModeExtendedPossible) {
        super(context, (currentMode == MODE_COMPACT) ? LAYOUT_MODE_COMPACT : LAYOUT_MODE_EXTENDED, c, from, to, flags);
        mLayout = (currentMode == MODE_COMPACT) ? LAYOUT_MODE_COMPACT : LAYOUT_MODE_EXTENDED;
        mContext = context;
        mMode = currentMode;
        mIsModeExtendedPossible = isModeExtendedPossible;

        final int mMemoryClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        final int mCacheSize = 1024 * 1024 * mMemoryClass / 8;

        mImageCache = new LruCache<String, Bitmap>(mCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };

        mDiskImageCache = new DiskLruImageCache(context, "images", mCacheSize * 2);
    }

    @SuppressLint("NewApi")
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String mTitle = cursor.getString(cursor.getColumnIndex(ArticleDAO.TITLE));
        String mSummary = cursor.getString(cursor.getColumnIndex(ArticleDAO.SUMMARY));
        String mPubdate = formatToYesterdayOrToday(SQLiteHelper.toDate(cursor.getString(cursor.getColumnIndex(ArticleDAO.PUBDATE))));
        String mImageURL = cursor.getString(cursor.getColumnIndex(ArticleDAO.IMAGE));
        int mRead = cursor.getInt(cursor.getColumnIndex(ArticleDAO.READ));

        final TextView mArticleTitle = (TextView) view.findViewById(R.id.list_item_entry_title);
        final TextView mArticlePubdate = (TextView) view.findViewById(R.id.list_item_entry_date);
        final ImageView mArticleImage = (ImageView) view.findViewById(R.id.list_item_entry_image);
        final TextView mArticleSummary = (TextView) view.findViewById(R.id.list_item_entry_summary);

        if (mArticleTitle != null) {
            mArticleTitle.setText(mTitle);
        }
        if (mArticleSummary != null) {
            mArticleSummary.setText(mSummary);
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

        if (mArticleImage != null) {
            setInvisible(mArticleImage);

            if (mImageURL != null && mImageURL != "") {
                try {
                    URL mURL = new URL(mImageURL);

                    String mKey = getFileName(mURL);
                    Bitmap mImage = mImageCache.get(mKey);
                    if (mImage == null) {
                        mImage = mDiskImageCache.getBitmap(mKey);
                    }
                    if (mImage == null) {
                        if (cancelPotentialDownload(mImageURL, mArticleImage)) {
                            int mImageDimension = (mIsModeExtendedPossible) ? mContext.getResources().getDimensionPixelSize(R.dimen.image_dimension_extended)
                                    : mContext.getResources().getDimensionPixelSize(R.dimen.image_dimension_compact);

                            ImageDownloaderTask mTask = new ImageDownloaderTask(mArticleImage, mImageDimension);
                            DownloadedDrawable mDownloadedDrawable = new DownloadedDrawable(mTask);
                            mArticleImage.setImageDrawable(mDownloadedDrawable);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mImageURL);
                            } else {
                                mTask.execute(mImageURL);
                            }
                        }
                    } else {
                        cancelPotentialDownload(mImageURL, mArticleImage);
                        mArticleImage.setImageBitmap(mImage);
                        setVisible(mArticleImage);
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
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
        private URL mURL;
        private int mImageDimension;

        public ImageDownloaderTask(ImageView imageView, int imageDimension) {
            mImageViewReference = new WeakReference<ImageView>(imageView);
            mImageDimension = imageDimension;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                mURL = new URL(params[0]);
                Bitmap mBitmap = BitmapFactory.decodeStream(mURL.openConnection().getInputStream());
                float originalWidth = mBitmap.getWidth();
                float originalHeight = mBitmap.getHeight();

                if (originalHeight < mImageDimension || originalWidth < mImageDimension) {
                    return null;
                } else {
                    if (originalHeight > mImageDimension) {
                        int newHeight = mImageDimension;
                        int newWidth = (int) ((newHeight / originalHeight) * originalWidth);
                        mBitmap = Bitmap.createScaledBitmap(mBitmap, newWidth, newHeight, true);
                    }

                    if (!isCancelled()) {
                        String mKey = getFileName(mURL);
                        synchronized (mDiskImageCache) {
                            if (mDiskImageCache.getBitmap(mKey) == null) {
                                mDiskImageCache.put(mKey, mBitmap);
                            }
                        }
                        synchronized (mImageCache) {
                            if (mImageCache.get(mKey) == null) {
                                mImageCache.put(mKey, mBitmap);
                            }
                        }
                        return mBitmap;
                    } else {
                        return null;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mImageViewReference != null && bitmap != null) {
                ImageView mImageView = mImageViewReference.get();
                ImageDownloaderTask mImageDownloaderTask = getImageDownloaderTask(mImageView);
                if (this == mImageDownloaderTask) {
                    mImageView.setImageBitmap(bitmap);
                    setVisible(mImageView);
                }
            }
        }
    }

    private void setVisible(View view) {
        view.setVisibility(View.VISIBLE);
        if (mMode == MODE_COMPACT) {
            View mArticleSummary = ((View) view.getParent()).findViewById(R.id.list_item_entry_summary);
            RelativeLayout.LayoutParams mLayoutParams = (RelativeLayout.LayoutParams) mArticleSummary.getLayoutParams();
            mLayoutParams.addRule(RelativeLayout.BELOW, R.id.list_item_entry_image);
            mArticleSummary.setLayoutParams(mLayoutParams);
        }
    }

    private void setInvisible(View view) {
        if (mMode == MODE_COMPACT) {
            View mArticleSummary = ((View) view.getParent()).findViewById(R.id.list_item_entry_summary);
            RelativeLayout.LayoutParams mLayoutParams = (RelativeLayout.LayoutParams) mArticleSummary.getLayoutParams();
            mLayoutParams.addRule(RelativeLayout.BELOW, R.id.list_item_entry_title);
            mArticleSummary.setLayoutParams(mLayoutParams);
        }
        view.setVisibility(View.GONE);
    }

    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        ImageDownloaderTask mImageDownloaderTask = getImageDownloaderTask(imageView);

        if (mImageDownloaderTask != null) {
            URL mBitmapUrl = mImageDownloaderTask.mURL;
            if ((mBitmapUrl == null) || (!mBitmapUrl.toString().equals(url))) {
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

    /*
     * From http://stackoverflow.com/a/1856542
     */
    private static String getFileName(URL extUrl) {
        String mFilename = "";
        String mPath = extUrl.getPath();
        String[] mPathContents = mPath.split("[\\\\/]");
        if (mPathContents != null) {
            String mLastPart = mPathContents[mPathContents.length - 1];
            String[] mLastPartContents = mLastPart.split("\\.");
            if (mLastPartContents != null && mLastPartContents.length > 1) {
                int mLastPartContentLength = mLastPartContents.length;
                String mName = "";
                for (int i = 0; i < mLastPartContentLength; i++) {
                    if (i < (mLastPartContents.length - 1)) {
                        mName += mLastPartContents[i];
                        if (i < (mLastPartContentLength - 2)) {
                            mName += ".";
                        }
                    }
                }
                String mExtension = mLastPartContents[mLastPartContentLength - 1];
                mFilename = mName + "." + mExtension;
            }
        }
        return mFilename;
    }

}
