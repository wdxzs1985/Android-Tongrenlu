package info.tongrenlu.android.music;

import info.tongrenlu.android.downloadmanager.DownloadManager;
import info.tongrenlu.android.downloadmanager.DownloadManagerImpl;
import info.tongrenlu.android.provider.HttpHelper;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;

public class TongrenluApplication extends Application {

    private BitmapLruCache mBitmapCache = null;
    private HttpHelper httpHelper = null;

    private DownloadManager mDownloadManager = null;

    @Override
    public void onCreate() {
        this.clearNotification();
        this.initBitmapCache();
        this.initHttpHelper();
        this.initDownloadManager();
    }

    private void clearNotification() {
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private void initBitmapCache() {
        final File cacheDir = this.getCacheDir();
        final BitmapLruCache.Builder builder = new BitmapLruCache.Builder(this);
        builder.setMemoryCacheEnabled(true)
               .setMemoryCacheMaxSizeUsingHeapSize()
               .setDiskCacheEnabled(true)
               .setDiskCacheLocation(cacheDir);

        this.mBitmapCache = builder.build();
    }

    public BitmapLruCache getBitmapCache() {
        return this.mBitmapCache;
    }

    private void initHttpHelper() {
        this.httpHelper = new HttpHelper();
    }

    public HttpHelper getHttpHelper() {
        return this.httpHelper;
    }

    private void initDownloadManager() {
        this.mDownloadManager = new DownloadManagerImpl();
    }

    public DownloadManager getDownloadManager() {
        return this.mDownloadManager;
    }
}
