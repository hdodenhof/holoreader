package de.hdodenhof.feedreader.tasks;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class ImageDownloaderTask extends AsyncTask<String, Void, Void> {

        @SuppressWarnings("unused")
        private static final String TAG = AddFeedTask.class.getSimpleName();

        private Context mContext;

        public ImageDownloaderTask(Context context) {
                this.mContext = context;
        }

        @Override
        protected Void doInBackground(String... params) {
                try {
                        URL mURL = new URL(params[0]);

                        String mFilename = mURL.toString().substring(mURL.toString().lastIndexOf("/") + 1);

                        mContext.deleteFile(mFilename);
                        FileOutputStream mOutputStream = mContext.openFileOutput(mFilename, Context.MODE_PRIVATE);

                        DefaultHttpClient mHTTPClient = new DefaultHttpClient();
                        HttpGet mRequest = new HttpGet(mURL.toString());
                        HttpResponse mResponse = mHTTPClient.execute(mRequest);
                        InputStream mInputStream = mResponse.getEntity().getContent();

                        Bitmap mBitmap = BitmapFactory.decodeStream(mInputStream);

                        mBitmap.compress(Bitmap.CompressFormat.JPEG, 75, mOutputStream);
                        mOutputStream.close();

                } catch (Exception e) {
                        e.printStackTrace();
                }

                return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
}