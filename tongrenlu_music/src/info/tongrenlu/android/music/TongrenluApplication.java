package info.tongrenlu.android.music;

import info.tongrenlu.android.downloadmanager.DownloadManager;
import info.tongrenlu.android.downloadmanager.DownloadManagerImpl;
import info.tongrenlu.android.provider.HttpHelper;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class TongrenluApplication extends Application {

    public static int VERSION_CODE = 0;
    public static String VERSION_NAME = "unknown";

    private BitmapLruCache mBitmapCache = null;
    private DownloadManager mDownloadManager = null;
    private HttpHelper httpHelper = null;

    @Override
    public void onCreate() {
        this.clearNotification();
        this.initPackageInfo();
        this.initBitmapCache();
        this.initHttpHelper();
        this.initDownloadManager();
    }

    private void clearNotification() {
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private void initPackageInfo() {
        try {
            final String packageName = this.getPackageName();
            final PackageManager pm = this.getPackageManager();
            final PackageInfo pInfo = pm.getPackageInfo(packageName,
                                                        PackageManager.GET_META_DATA);
            TongrenluApplication.VERSION_CODE = pInfo.versionCode;
            TongrenluApplication.VERSION_NAME = pInfo.versionName;
        } catch (final NameNotFoundException e) {
            e.printStackTrace();
        }
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

    private void initDownloadManager() {
        this.mDownloadManager = new DownloadManagerImpl();
    }

    public DownloadManager getDownloadManager() {
        return this.mDownloadManager;
    }

    public HttpHelper getHttpHelper() {
        return this.httpHelper;
    }
}
