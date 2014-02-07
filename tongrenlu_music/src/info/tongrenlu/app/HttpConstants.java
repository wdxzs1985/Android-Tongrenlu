package info.tongrenlu.app;

import info.tongrenlu.android.music.SettingsActivity;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;

public class HttpConstants {

    public static final String MUSIC_LIST_URI = "/fm/music";

    public static final String RESOURCE_URI = "/resource/";

    public static final String VERSION_URI = "/static/tongrenlu/app/version.json";

    private static final String APK_URI = "/static/tongrenlu/app/tongrenlu_android.apk";

    public static final int PAGE_SIZE = 50;

    public static final int XXS_COVER = 60;

    public static final int XS_COVER = 90;

    public static final int S_COVER = 120;

    public static final int M_COVER = 180;

    public static final int L_COVER = 400;

    public static Uri getMusicListUri(final Context context) {
        final String host = HttpConstants.getHost(context);
        return Uri.parse(host + HttpConstants.MUSIC_LIST_URI);
    }

    public static Uri getMusicInfoUri(final Context context, final String articleId) {
        final Uri baseUri = HttpConstants.getMusicListUri(context);
        return Uri.withAppendedPath(baseUri, articleId);
    }

    public static String getCoverUrl(final Context context, final String articleId, int size) {
        final String host = HttpConstants.getHost(context);
        final String filename = HttpConstants.getCoverFilename(articleId, size);
        final String url = host + HttpConstants.RESOURCE_URI + filename;
        return url;
    }

    public static File getCover(final Context context, final String articleId, int size) {
        File[] dirs = ContextCompat.getExternalCacheDirs(context);
        final String filename = HttpConstants.getCoverFilename(articleId, size);
        for (File dir : dirs) {
            File file = new File(dir, filename);
            if (file.exists()) {
                return file;
            }
        }
        return new File(dirs[0], filename);
    }

    public static void setImage(final ImageView view, final File data) {
        final Bitmap bm = BitmapFactory.decodeFile(data.getAbsolutePath());
        view.setImageBitmap(bm);
    }

    public static String getMp3Url(final Context context, final String articleId, final String fileId) {
        final String host = HttpConstants.getHost(context);
        final String url = host + HttpConstants.RESOURCE_URI
                + articleId
                + "/"
                + fileId
                + ".mp3";
        return url;
    }

    public static File getMp3(final Context context, final String articleId, final String fileId) {
        File[] dirs = ContextCompat.getExternalFilesDirs(context, articleId);
        for (File dir : dirs) {
            File mp3file = new File(dir, fileId + ".mp3");
            if (mp3file.exists()) {
                return mp3file;
            }
        }
        return new File(dirs[0], fileId + ".mp3");
    }

    private static String getHost(final Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String host = sharedPreferences.getString(SettingsActivity.PREF_KEY_SERVER,
                                                        SettingsActivity.PREF_DEFAULT_SERVER);
        return host;
    }

    private static String getCoverFilename(final String articleId, final int size) {
        return articleId + "/cover_" + size + ".jpg";
    }

    public static void clearCover(final Context context) {
        File[] dirs = ContextCompat.getExternalFilesDirs(context, null);
        for (File dir : dirs) {
            try {
                FileUtils.cleanDirectory(dir);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearMp3File(final Context context) {
        File[] dirs = ContextCompat.getExternalFilesDirs(context, null);
        for (File dir : dirs) {
            try {
                FileUtils.cleanDirectory(dir);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Uri getVersionInfoUri(final Context context) {
        final String host = HttpConstants.getHost(context);
        final String url = host + VERSION_URI;
        return Uri.parse(url);
    }

    public static String getApkUrl(final Context context) {
        final String host = HttpConstants.getHost(context);
        return host + APK_URI;
    }

    public static File getApkFile(final Context context) {
        final File dir = context.getExternalCacheDir();
        return new File(dir, "tongrenlu_android.apk");
    }

    public static String getAvaliableFilename(final String name) {
        return name.replaceAll("[:\\\\/*?|<>]", "_");
    }
}
