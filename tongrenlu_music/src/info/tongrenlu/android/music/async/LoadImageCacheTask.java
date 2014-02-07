package info.tongrenlu.android.music.async;

import info.tongrenlu.support.HttpHelper;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

public class LoadImageCacheTask extends AsyncTask<Object, Object, Drawable> {

    @Override
    protected Drawable doInBackground(Object... params) {
        BitmapLruCache bitmapCache = (BitmapLruCache) params[0];
        final String url = (String) params[1];
        CacheableBitmapDrawable wrapper = bitmapCache.get(url);
        if (wrapper == null) {
            wrapper = bitmapCache.put(url, HttpHelper.loadImage(url));
        }
        return wrapper;
    }
}
