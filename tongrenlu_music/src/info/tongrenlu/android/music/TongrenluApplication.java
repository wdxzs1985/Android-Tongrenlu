package info.tongrenlu.android.music;

import java.io.File;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class TongrenluApplication extends Application {

    public static HttpClient getApplicationClient() {
        final HttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(ClientPNames.COOKIE_POLICY,
                                        CookiePolicy.BROWSER_COMPATIBILITY);
        return client;
    }

    public static int VERSION_CODE = 0;
    public static String VERSION_NAME = "unknown";

    private boolean init = false;
    private BitmapLruCache mBitmapCache = null;

    @Override
    public void onCreate() {
        if (!this.init) {
            this.init = true;
            this.clearNotification();
            this.initClient();
            this.initPackageInfo(this);
            this.initBitmapCache(this);
        }
    }

    private void clearNotification() {
        final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private void initClient() {
        final CookieStore cookieStore = new BasicCookieStore();
        final HttpContext localContext = new BasicHttpContext();
        // Bind custom cookie store to the local context
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
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

}
