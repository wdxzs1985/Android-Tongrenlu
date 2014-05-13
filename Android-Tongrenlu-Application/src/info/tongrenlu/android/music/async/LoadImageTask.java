package info.tongrenlu.android.music.async;

import info.tongrenlu.android.provider.HttpHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

public class LoadImageTask extends AsyncTask<Object, Object, Drawable> {

    @Override
    protected Drawable doInBackground(Object... params) {
        final BitmapLruCache bitmapCache = (BitmapLruCache) params[0];
        final String url = (String) params[1];
        final HttpHelper http = (HttpHelper) params[2];
        CacheableBitmapDrawable wrapper = bitmapCache.get(url);
        if (wrapper == null) {
            try {
                byte[] data = http.getByteArray(url);
                wrapper = bitmapCache.put(url, new ByteArrayInputStream(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wrapper;
    }
}
