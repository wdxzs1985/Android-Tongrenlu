package info.tongrenlu.android.music;

import info.tongrenlu.android.downloadmanager.DownloadManager;
import info.tongrenlu.android.downloadmanager.DownloadManagerImpl;

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

    @Override
    public void onCreate() {
        this.clearNotification();
        this.initPackageInfo(this);
        this.initBitmapCache(this);
        this.initDownloadManager(this);
    }

    private void clearNotification() {
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private void initPackageInfo(final Context context) {
        try {
            final String packageName = context.getPackageName();
            final PackageManager pm = context.getPackageManager();
            final PackageInfo pInfo = pm.getPackageInfo(packageName,
                                                        PackageManager.GET_META_DATA);
            VERSION_CODE = pInfo.versionCode;
            VERSION_NAME = pInfo.versionName;
        } catch (final NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initBitmapCache(Context context) {
        File cacheDir = context.getCacheDir();
        BitmapLruCache.Builder builder = new BitmapLruCache.Builder(context);
        builder.setMemoryCacheEnabled(true)
               .setMemoryCacheMaxSizeUsingHeapSize()
               .setDiskCacheEnabled(true)
               .setDiskCacheLocation(cacheDir);

        this.mBitmapCache = builder.build();
    }

    public BitmapLruCache getBitmapCache() {
        return this.mBitmapCache;
    }

    private void initDownloadManager(Context context) {
        this.mDownloadManager = new DownloadManagerImpl(1);
    }

    public DownloadManager getDownloadManager() {
        return this.mDownloadManager;
    }
}
