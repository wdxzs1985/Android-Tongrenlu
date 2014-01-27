package info.tongrenlu.app;

import info.tongrenlu.android.music.SettingsActivity;
import info.tongrenlu.android.task.ArticleCoverDownloadTask;

import java.io.File;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.ImageView;

public class HttpConstants {

    public static final String MUSIC_LIST_URI = "/fm/music";

    public static final String RESOURCE_URI = "/resource/";

    public static final String VERSION_URI = "/static/tongrenlu/app/version.json";

    private static final String APK_URI = "/static/tongrenlu/app/tongrenlu_android.apk";

    public static final String SD_PATH = "/sdcard/tongrenlu";

    public static final int PAGE_SIZE = 50;

    public static final int NORMAL_COVER = 180;

    public static final int LARGE_COVER = 400;

    public static Uri getMusicListUri(final Context context) {
        final String host = HttpConstants.getHost(context);
        return Uri.parse(host + HttpConstants.MUSIC_LIST_URI);
    }

    public static Uri getMusicInfoUri(final Context context, final String articleId) {
        final Uri baseUri = HttpConstants.getMusicListUri(context);
        return Uri.withAppendedPath(baseUri, articleId);
    }

    public static String getCoverUrl(final Context context, final String articleId) {
        final String host = HttpConstants.getHost(context);
        final String filename = HttpConstants.getCoverFilename(articleId,
                                                               HttpConstants.NORMAL_COVER);
        final String url = host + HttpConstants.RESOURCE_URI + filename;
        return url;
    }

    public static File getCover(final Context context, final String articleId) {
        final File dir = getCoverDir(context);
        final String filename = HttpConstants.getCoverFilename(articleId,
                                                               HttpConstants.NORMAL_COVER);
        return new File(dir, filename);
    }

    public static void displayCover(final ImageView view, final String articleId) {
        final Context context = view.getContext();
        final File data = HttpConstants.getCover(context, articleId);
        if (!data.isFile()) {
            final String url = HttpConstants.getCoverUrl(context, articleId);
            new ArticleCoverDownloadTask(view, articleId).execute(url, data);
        } else {
            HttpConstants.setImage(view, data);
        }
    }

    public static void displayLargeCover(final ImageView view, final String articleId) {
        final Context context = view.getContext();
        final File data = HttpConstants.getLargeCover(context, articleId);
        if (!data.isFile()) {
            final String url = HttpConstants.getLargeCoverUrl(context,
                                                              articleId);
            new ArticleCoverDownloadTask(view, articleId).execute(url, data);
        } else {
            HttpConstants.setImage(view, data);
        }
    }

    public static void setImage(final ImageView view, final File data) {
        final Bitmap bm = BitmapFactory.decodeFile(data.getAbsolutePath());
        view.setImageBitmap(bm);
    }

    public static String getLargeCoverUrl(final Context context, final String articleId) {
        final String host = HttpConstants.getHost(context);
        final String filename = HttpConstants.getCoverFilename(articleId,
                                                               HttpConstants.LARGE_COVER);
        final String url = host + HttpConstants.RESOURCE_URI + filename;
        return url;
    }

    public static File getLargeCover(final Context context, final String articleId) {
        final File dir = getCoverDir(context);
        final String filename = HttpConstants.getCoverFilename(articleId,
                                                               HttpConstants.LARGE_COVER);
        return new File(dir, filename);
    }

    public static String getMp3Url(final Context context, final String articleId, final String fileId) {
        final String host = HttpConstants.getHost(context);
        final String filename = HttpConstants.getMp3Filename(articleId, fileId);
        final String url = host + HttpConstants.RESOURCE_URI + filename;
        return url;
    }

    public static File getMp3(final Context context, final String articleId, final String fileId) {
        final File dir = getMp3Dir(context);
        final String filename = HttpConstants.getMp3Filename(articleId, fileId);
        return new File(dir, filename);
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

    private static String getMp3Filename(final String articleId, final String fileId) {
        return articleId + "/" + fileId + ".mp3";
    }

    public static void clearCover(final Context context) {
        final File dir = getCoverDir(context);
        try {
            FileUtils.cleanDirectory(dir);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearMp3File(final Context context) {
        final File dir = getMp3Dir(context);
        try {
            FileUtils.cleanDirectory(dir);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static Uri getVersionInfoUri(final Context context) {
        final String host = HttpConstants.getHost(context);
        final String url = host + VERSION_URI;
        return Uri.parse(url);
    }

    public static File getMp3Dir(final Context context) {
        File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        final File dir = new File(publicDir, "tongrenlu");
        if (!dir.mkdirs()) {
            System.out.println("Directory not created");
        }
        return dir;
    }

    public static File getCoverDir(final Context context) {
        final File dir = context.getCacheDir();
        return dir;
    }

    public static String getApkUrl(final Context context) {
        final String host = HttpConstants.getHost(context);
        return host + APK_URI;
    }

    public static File getApkFile(final Context context) {
        final File dir = context.getExternalCacheDir();
        return new File(dir, "tongrenlu_android.apk");
    }

    public static File getSDCard() {
        return new File(SD_PATH);
    }

    public static String getAvaliableFilename(final String name) {
        return name.replaceAll("[:\\\\/*?|<>]", "_");
    }
}
