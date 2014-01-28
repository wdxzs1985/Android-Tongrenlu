package info.tongrenlu.android.music;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

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

    public static String getApplicationVersionName() {
        return versionName;
    }

    public static int getApplicationVersionCode() {
        return versionCode;
    }

    private static String versionName = null;

    private static int versionCode = 0;

    private static boolean init = false;

    @Override
    public void onCreate() {
        if (!init) {
            init = true;
            this.clearNotification();
            this.initClient();
            this.initPackageInfo(this);
            // this.startUpdateService(this);
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
            versionCode = pInfo.versionCode;
            versionName = pInfo.versionName;
        } catch (final NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    // private void startUpdateService(final Context context) {
    // final Intent intent = new Intent(context, UpdateService.class);
    // context.startService(intent);
    // }

}
