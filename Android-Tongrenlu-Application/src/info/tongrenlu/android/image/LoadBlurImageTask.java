package info.tongrenlu.android.image;

import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.support.ApplicationSupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
    protected Drawable doInBackground(final Object... params) {
        final BitmapLruCache bitmapCache = (BitmapLruCache) params[0];
        final String url = (String) params[1];
        final HttpHelper http = (HttpHelper) params[2];
        final Context context = (Context) params[3];

        CacheableBitmapDrawable wrapper = bitmapCache.get(url);
        if (wrapper == null) {
            try {
                final byte[] data = http.getByteArray(url);
                wrapper = bitmapCache.put(url, new ByteArrayInputStream(data));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        if (wrapper == null) {
            return null;
        }

        Bitmap blurBitmap = wrapper.getBitmap();
        if (ApplicationSupport.canUseRenderScript()) {
            blurBitmap = RenderScriptBlur.fastblur(context,
                                                   blurBitmap,
                                                   LoadBlurImageTask.BLUR_RADIUS);
        } else {
            blurBitmap = StackBlur.fastblur(context,
                                            blurBitmap,
                                            LoadBlurImageTask.BLUR_RADIUS);
        }

        final Resources resource = context.getResources();
        final DisplayMetrics metrics = resource.getDisplayMetrics();
        final float screenWidth = metrics.widthPixels;
        final float screenHeight = metrics.heightPixels;
        final float screenRatio = screenWidth / screenHeight;

        final int targetWidth = (int) (blurBitmap.getWidth() * screenRatio);
        final int targetHeight = blurBitmap.getHeight();

        final int x = (blurBitmap.getWidth() - targetWidth) / 2;

        blurBitmap = Bitmap.createBitmap(blurBitmap,
                                         x,
                                         0,
                                         targetWidth,
                                         targetHeight);

        final Drawable result = new BitmapDrawable(resource, blurBitmap);
        return result;
    }
}
