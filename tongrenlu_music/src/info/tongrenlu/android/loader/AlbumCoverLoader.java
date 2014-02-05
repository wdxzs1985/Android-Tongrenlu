package info.tongrenlu.android.loader;

import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.support.HttpHelper;

import org.apache.commons.lang3.StringUtils;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.content.Context;
import android.graphics.drawable.Drawable;

public class AlbumCoverLoader extends BaseLoader<Drawable> {

    private final String mArticleId;
    private final int mSize;

    public AlbumCoverLoader(Context ctx, String articleId, int size) {
        super(ctx);
        this.mArticleId = articleId;
        this.mSize = size;
    }

    @Override
    public Drawable loadInBackground() {
        CacheableBitmapDrawable wrapper = null;
        if (StringUtils.isNotBlank(this.mArticleId)) {
            final TongrenluApplication application = (TongrenluApplication) this.getContext();
            final BitmapLruCache cache = application.getBitmapCache();
            final String url = HttpConstants.getCoverUrl(application,
                                                         this.mArticleId,
                                                         this.mSize);
            wrapper = cache.get(url);
            if (wrapper == null) {
                wrapper = cache.put(url, HttpHelper.loadImage(url));
            }
        }
        return wrapper;
    }

}
