package info.tongrenlu.android.music.async;

import info.tongrenlu.support.Blur;
import info.tongrenlu.support.HttpHelper;

import java.io.InputStream;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;

public class LoadBlurImageTask extends AsyncTask<Object, Object, Drawable> {

    public static final int BLUR_RADIUS = 10;

    @Override
    protected Drawable doInBackground(Object... params) {
        BitmapLruCache bitmapCache = (BitmapLruCache) params[0];
        final String url = (String) params[1];
        Context context = (Context) params[2];

        CacheableBitmapDrawable wrapper = bitmapCache.get(url);
        if (wrapper == null) {
            InputStream input = HttpHelper.loadImage(url);
            if (input != null) {
                wrapper = bitmapCache.put(url, input);
            }
        }

        if (wrapper == null) {
            return null;
        }

        Bitmap blurBitmap = wrapper.getBitmap();
        blurBitmap = Blur.fastblur(context, blurBitmap, BLUR_RADIUS);
        Resources resource = context.getResources();
        DisplayMetrics metrics = resource.getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float screenHeight = metrics.heightPixels;
        float screenRatio = screenWidth / screenHeight;

        int targetWidth = (int) (blurBitmap.getWidth() * screenRatio);
        int targetHeight = blurBitmap.getHeight();

        int x = (blurBitmap.getWidth() - targetWidth) / 2;

        blurBitmap = Bitmap.createBitmap(blurBitmap,
                                         x,
                                         0,
                                         targetWidth,
                                         targetHeight);

        return new BitmapDrawable(resource, blurBitmap);
    }
}
