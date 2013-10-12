package de.hdodenhof.holoreader.listadapters;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.hdodenhof.holoreader.R;
import de.hdodenhof.holoreader.misc.DiskLruImageCache;
import de.hdodenhof.holoreader.misc.Helpers;
import de.hdodenhof.holoreader.provider.SQLiteHelper;
import de.hdodenhof.holoreader.provider.SQLiteHelper.ArticleDAO;

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

    private LruCache<String, Bitmap> mImageCache;
    private DiskLruImageCache mDiskImageCache;
    private Context mContext;
    private boolean mIsModeExtendedPossible;
    private int mMode;
    private int mLayout;

    public RSSArticleAdapter(Context context, Cursor c, String[] from, int[] to, int flags, int currentMode, boolean isModeExtendedPossible) {
        super(context, (currentMode == MODE_COMPACT) ? LAYOUT_MODE_COMPACT : LAYOUT_MODE_EXTENDED, c, from, to, flags);
        mLayout = (currentMode == MODE_COMPACT) ? LAYOUT_MODE_COMPACT : LAYOUT_MODE_EXTENDED;
        mContext = context;
        mMode = currentMode;
        mIsModeExtendedPossible = isModeExtendedPossible;

        final int memoryClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        final int cacheSize = 1024 * 1024 * memoryClass / 8;

        mImageCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };

        mDiskImageCache = new DiskLruImageCache(context, "images", cacheSize * 2);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(ArticleDAO._ID));
        String title = cursor.getString(cursor.getColumnIndex(ArticleDAO.TITLE));
        String summary = cursor.getString(cursor.getColumnIndex(ArticleDAO.SUMMARY));
        String pubdate = Helpers.formatToYesterdayOrToday(SQLiteHelper.toDate(cursor.getString(cursor.getColumnIndex(ArticleDAO.PUBDATE))));
        String imageURL = cursor.getString(cursor.getColumnIndex(ArticleDAO.IMAGE));
        String read = cursor.getString(cursor.getColumnIndex(ArticleDAO.READ));

        final TextView articleTitle = (TextView) view.findViewById(R.id.list_item_entry_title);
        final TextView articlePubdate = (TextView) view.findViewById(R.id.list_item_entry_date);
        final ImageView articleImage = (ImageView) view.findViewById(R.id.list_item_entry_image);
        final TextView articleSummary = (TextView) view.findViewById(R.id.list_item_entry_summary);

        if (articleTitle != null) {
            articleTitle.setText(title);
        }
        if (articleSummary != null) {
            articleSummary.setText(summary);
        }
        if (articlePubdate != null) {
            articlePubdate.setText(pubdate);
        }

        if (read != null) {
            articleImage.setAlpha(ALPHA_STATE_DIMMED);
            dim(articleTitle);
            dim(articleSummary);
            dim(articlePubdate);
        } else {
            articleImage.setAlpha(ALPHA_STATE_FULL);
            lightup(articleTitle);
            lightup(articleSummary);
            lightup(articlePubdate);
        }

        if (articleImage != null) {
            setInvisible(articleImage);

            if (imageURL != null && imageURL != "") {
                prepareImage(id, imageURL, articleImage);
            }
        }
    }

    @SuppressLint("NewApi")
    private void prepareImage(long id, String imageURL, final ImageView articleImage) {
        try {
            URL url = new URL(imageURL);

            String key = getUniqueKey(id, url);
            Bitmap image = mImageCache.get(key);
            if (image == null) {
                image = mDiskImageCache.getBitmap(key);
            }
            if (image == null) {
                if (cancelPotentialDownload(imageURL, articleImage)) {
                    int imageDimension = (mIsModeExtendedPossible) ? mContext.getResources().getDimensionPixelSize(R.dimen.image_dimension_extended) : mContext
                            .getResources().getDimensionPixelSize(R.dimen.image_dimension_compact);

                    ImageDownloaderTask task = new ImageDownloaderTask(id, articleImage, imageDimension);
                    DownloadDrawable downloadDrawable = new DownloadDrawable(task);
                    articleImage.setImageDrawable(downloadDrawable);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageURL);
                    } else {
                        task.execute(imageURL);
                    }
                }
            } else {
                cancelPotentialDownload(imageURL, articleImage);
                articleImage.setImageBitmap(image);
                setVisible(articleImage);
            }
        } catch (MalformedURLException e) {
            // nothing to do here
        }
    }

    private void dim(TextView view) {
        ColorStateList colors = view.getTextColors();
        view.setTextColor(colors.withAlpha(ALPHA_STATE_DIMMED));
    }

    private void lightup(TextView view) {
        ColorStateList colors = view.getTextColors();
        view.setTextColor(colors.withAlpha(ALPHA_STATE_FULL));
    }

    @Override
    public Cursor swapCursor(Cursor c) {
        return super.swapCursor(c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(mLayout, parent, false);

        return Helpers.addBackgroundIndicator(context, view, R.attr.customActivatedBackgroundIndicator);
    }

    public int getType() {
        return RSSAdapter.TYPE_ARTICLE;
    }

    public class ImageDownloaderTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> mImageViewReference;

        private long mID;
        private URL mURL;
        private int mImageDimension;

        public ImageDownloaderTask(long id, ImageView imageView, int imageDimension) {
            mImageViewReference = new WeakReference<ImageView>(imageView);
            mImageDimension = imageDimension;
            mID = id;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                mURL = new URL(params[0]);
                Bitmap bitmap = BitmapFactory.decodeStream(mURL.openConnection().getInputStream());
                float originalWidth = bitmap.getWidth();
                float originalHeight = bitmap.getHeight();

                if (originalHeight < mImageDimension || originalWidth < mImageDimension) {
                    return null;
                } else {
                    if (originalHeight > mImageDimension) {
                        int newHeight = mImageDimension;
                        int newWidth = (int) ((newHeight / originalHeight) * originalWidth);
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    }

                    if (!isCancelled()) {
                        String key = getUniqueKey(mID, mURL);
                        synchronized (mDiskImageCache) {
                            if (mDiskImageCache.getBitmap(key) == null) {
                                mDiskImageCache.put(key, bitmap);
                            }
                        }
                        synchronized (mImageCache) {
                            if (mImageCache.get(key) == null) {
                                mImageCache.put(key, bitmap);
                            }
                        }
                        return bitmap;
                    } else {
                        return null;
                    }
                }

            } catch (IOException e) {
                return null;
            } catch (NullPointerException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (mImageViewReference != null && bitmap != null) {
                ImageView imageView = mImageViewReference.get();
                ImageDownloaderTask imageDownloaderTask = getImageDownloaderTask(imageView);
                if (this == imageDownloaderTask) {
                    imageView.setImageBitmap(bitmap);
                    setVisible(imageView);
                }
            }
        }
    }

    private void setVisible(View view) {
        view.setVisibility(View.VISIBLE);
        if (mMode == MODE_COMPACT) {
            View articleSummary = ((View) view.getParent()).findViewById(R.id.list_item_entry_summary);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) articleSummary.getLayoutParams();
            layoutParams.addRule(RelativeLayout.BELOW, R.id.list_item_entry_image);
            articleSummary.setLayoutParams(layoutParams);
        }
    }

    private void setInvisible(View view) {
        if (mMode == MODE_COMPACT) {
            View articleSummary = ((View) view.getParent()).findViewById(R.id.list_item_entry_summary);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) articleSummary.getLayoutParams();
            layoutParams.addRule(RelativeLayout.BELOW, R.id.list_item_entry_title);
            articleSummary.setLayoutParams(layoutParams);
        }
        view.setVisibility(View.GONE);
    }

    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        ImageDownloaderTask imageDownloaderTask = getImageDownloaderTask(imageView);

        if (imageDownloaderTask != null) {
            URL bitmapUrl = imageDownloaderTask.mURL;
            if ((bitmapUrl == null) || (!bitmapUrl.toString().equals(url))) {
                imageDownloaderTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    private static ImageDownloaderTask getImageDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadDrawable) {
                DownloadDrawable downloadDrawable = (DownloadDrawable) drawable;
                return downloadDrawable.getImageDownloaderTask();
            }
        }
        return null;
    }

    private static class DownloadDrawable extends ColorDrawable {
        private final WeakReference<ImageDownloaderTask> mImageDownloaderTaskReference;

        public DownloadDrawable(ImageDownloaderTask imageDownloaderTask) {
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
    private static String getUniqueKey(long id, URL extUrl) {
        String filename = "";
        String path = extUrl.getPath();

        String[] pathContents = path.split("[\\\\/]");
        if (pathContents != null) {
            String lastPart = pathContents[pathContents.length - 1];
            String[] lastPartContents = lastPart.split("\\.");
            if (lastPartContents != null && lastPartContents.length > 1) {
                int lastPartContentLength = lastPartContents.length;
                String name = "";
                for (int i = 0; i < lastPartContentLength; i++) {
                    if (i < (lastPartContents.length - 1)) {
                        name += lastPartContents[i];
                        if (i < (lastPartContentLength - 2)) {
                            name += ".";
                        }
                    }
                }
                String extension = lastPartContents[lastPartContentLength - 1];

                // remove linebreaks and spaces, fixes #55
                name = name.replaceAll("\\s", "");

                // make sure keys match [a-z0-9_-]{1,64} (DiskLruCache restriction since 1.3.0)
                name = name.toLowerCase().replaceAll("[^a-z0-9_-]", "");
                int nameMaxLength = 64 - (extension.length() + 1) - (String.valueOf(id).length() + 1);
                if (name.length() > nameMaxLength){
                    name = name.substring(0, nameMaxLength);
                }

                filename = name + "-" + extension;
            }
        }
        return id + "_" + filename;
    }
}
