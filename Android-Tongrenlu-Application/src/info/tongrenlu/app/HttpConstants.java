package info.tongrenlu.app;

import info.tongrenlu.android.music.fragment.SettingFragment;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public class HttpConstants {

    public static final int PAGE_SIZE = 50;

    public static final int XS_COVER = 60;

    public static final int S_COVER = 120;

    public static final int M_COVER = 180;

    public static final int L_COVER = 400;

    public static String getCoverUrl(final Context context,
                                     final String articleId,
                                     final int size) {
        final String fileServer = HttpConstants.getFileServer(context);
        final String filename = HttpConstants.getCoverFilename(articleId, size);
        final String url = fileServer + filename;
        return url;
    }

    public static File getCover(final Context context,
                                final String articleId,
                                final int size) {
        final File[] dirs = ContextCompat.getExternalCacheDirs(context);
        final String filename = HttpConstants.getCoverFilename(articleId, size);
        for (final File dir : dirs) {
            final File file = new File(dir, filename);
            if (file.exists()) {
                return file;
            }
        }
        return new File(dirs[0], filename);
    }

    public static String getMp3Url(final Context context,
                                   final String articleId,
                                   final String fileId) {
        final String fileServer = HttpConstants.getFileServer(context);
        return String.format("%s/m%s/f%s.mp3", fileServer, articleId, fileId);
    }

    public static File getMp3(final Context context,
                              final String articleId,
                              final String fileId) {
        final File[] dirs = ContextCompat.getExternalFilesDirs(context,
                                                               "m" + articleId);
        for (final File dir : dirs) {
            final File mp3file = new File(dir, fileId + ".mp3");
            if (mp3file.exists()) {
                return mp3file;
            }
        }
        return new File(dirs[0], fileId + ".mp3");
    }

    public static String getHostServer(final Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String host = sharedPreferences.getString(SettingFragment.PREF_KEY_HOST_SERVER,
                                                        SettingFragment.PREF_DEFAULT_HOST_SERVER);
        return host;
    }

    public static String getFileServer(final Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String host = sharedPreferences.getString(SettingFragment.PREF_KEY_FILE_SERVER,
                                                        SettingFragment.PREF_DEFAULT_FILE_SERVER);
        return host;
    }

    private static String getCoverFilename(final String articleId,
                                           final int size) {
        return String.format("/m%s/cover_%d.jpg", articleId, size);
    }

    public static void clearCover(final Context context) {
        final File[] dirs = ContextCompat.getExternalFilesDirs(context, null);
        for (final File dir : dirs) {
            try {
                FileUtils.cleanDirectory(dir);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearMp3File(final Context context) {
        final File[] dirs = ContextCompat.getExternalFilesDirs(context, null);
        for (final File dir : dirs) {
            try {
                FileUtils.cleanDirectory(dir);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getAvaliableFilename(final String name) {
        return name.replaceAll("[:\\\\/*?|<>]", "_");
    }

    public static boolean networkEnable(final Context context) {
        final Context applicationContext = context.getApplicationContext();
        final ConnectivityManager connectivity = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo network = connectivity.getActiveNetworkInfo();
        if (network == null) {
            Toast.makeText(applicationContext,
                           "connection disactive.",
                           Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }
}
