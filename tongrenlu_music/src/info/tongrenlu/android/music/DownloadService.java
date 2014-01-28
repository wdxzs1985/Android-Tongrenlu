package info.tongrenlu.android.music;

import info.tongrenlu.android.music.provider.DataProvider;
import info.tongrenlu.android.task.FileDownloadTask;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ServiceCompat;

public class DownloadService extends Service {

    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_ADD = "info.tongrenlu.android.DownloadService.ACTION_ADD";

    public static void downloadTrack(final Context context, final TrackBean... trackBean) {
        if (trackBean != null && trackBean.length > 0) {
            final ArrayList<TrackBean> items = new ArrayList<TrackBean>();
            Collections.addAll(items, trackBean);
            downloadTrack(context, items);
        }
    }

    public static void downloadTrack(final Context context, final ArrayList<TrackBean> items) {
        if (items != null && !items.isEmpty()) {
            final Intent serviceIntent = new Intent(context,
                                                    DownloadService.class);
            serviceIntent.setAction(DownloadService.ACTION_ADD);
            serviceIntent.putParcelableArrayListExtra("trackBeanList", items);
            context.startService(serviceIntent);
        }
    }

    private LinkedList<TrackBean> mDownloadList = null;
    private TrackBean mDownloading = null;
    private Bitmap mLargeIcon = null;
    private Uri newUrl = null;
    private int size = 0;
    private int loaded = 0;

    private NotificationManager mNotifyManager = null;
    private NotificationCompat.Builder mBuilder = null;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mDownloadList = new LinkedList<TrackBean>();
        this.mNotifyManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        this.mBuilder = new NotificationCompat.Builder(this);
        final Intent intent = new Intent(this, MusicPlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent contentIntent = PendingIntent.getActivity(this,
                                                                      0,
                                                                      intent,
                                                                      PendingIntent.FLAG_UPDATE_CURRENT);
        this.mBuilder.setContentIntent(contentIntent);
        this.mBuilder.setSmallIcon(R.drawable.ic_launcher)
                     .setAutoCancel(false)
                     .setOngoing(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mDownloadList.clear();
        this.mDownloadList = null;
        this.mNotifyManager = null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        final String action = intent.getAction();
        if (DownloadService.ACTION_ADD.equals(action)) {
            final ArrayList<TrackBean> trackBeanList = intent.getParcelableArrayListExtra("trackBeanList");
            for (final TrackBean trackBean : trackBeanList) {
                this.addToDownloadList(trackBean);
            }
        }
        this.execute();
        return ServiceCompat.START_STICKY;
    }

    protected void addToDownloadList(final TrackBean trackBean) {
        final String fileId = trackBean.getFileId();
        final Cursor c = this.getContentResolver()
                             .query(DataProvider.URI_TRACK,
                                    null,
                                    "file_id = ?",
                                    new String[] { fileId },
                                    null);
        if (!c.moveToFirst()) {
            this.mDownloadList.addLast(trackBean);
        }
        c.close();
    }

    private void execute() {
        if (this.mDownloading == null) {
            if (!this.mDownloadList.isEmpty()) {
                this.mDownloading = this.mDownloadList.pollFirst();
                this.executeDownload();
            } else {
                this.mNotifyManager.cancel(this.hashCode());
            }
        }
    }

    protected void executeDownload() {
        final String articleId = this.mDownloading.getArticleId();
        final String fileId = this.mDownloading.getFileId();
        final String spec = HttpConstants.getMp3Url(this, articleId, fileId);
        final File file = HttpConstants.getMp3(this, articleId, fileId);

        final TrackDownloadTask task = new TrackDownloadTask();
        task.execute(spec, file);
    }

    class TrackDownloadTask extends FileDownloadTask {

        @Override
        protected void onPreExecute() {
            final String articleId = DownloadService.this.mDownloading.getArticleId();
            DownloadService.this.size = 0;
            DownloadService.this.loaded = 0;

            final ContentValues values = new ContentValues();
            values.put("article_id", articleId);
            values.put("file_id", DownloadService.this.mDownloading.getFileId());
            values.put("title", DownloadService.this.mDownloading.getTitle());
            values.put("artist", DownloadService.this.mDownloading.getArtist());
            values.put("size", DownloadService.this.size);
            values.put("loaded", DownloadService.this.loaded);
            DownloadService.this.newUrl = DownloadService.this.getContentResolver()
                                                              .insert(DataProvider.URI_TRACK,
                                                                      values);

            final String downloading = "正在下载:";
            final String title = DownloadService.this.mDownloading.getTitle();
            DownloadService.this.decodeLargeIcon(articleId);
            DownloadService.this.mBuilder.setTicker(downloading + title)
                                         .setContentTitle(downloading)
                                         .setContentText(title)
                                         .setLargeIcon(DownloadService.this.mLargeIcon);
            DownloadService.this.sendNotification();
        }

        @Override
        protected void onProgressUpdate(final Long... values) {
            DownloadService.this.loaded = values[0].intValue();
            DownloadService.this.size = values[1].intValue();
        }

        @Override
        protected void onPostExecute(final File result) {
            if (DownloadService.this.loaded > 0 && DownloadService.this.loaded == DownloadService.this.size) {
                final ContentValues contentValues = new ContentValues();
                contentValues.put("loaded", DownloadService.this.loaded);
                contentValues.put("size", DownloadService.this.size);
                DownloadService.this.getContentResolver()
                                    .update(DownloadService.this.newUrl,
                                            contentValues,
                                            null,
                                            null);
                DownloadService.this.reset();
                DownloadService.this.execute();
            } else {
                DownloadService.this.getContentResolver()
                                    .delete(DownloadService.this.newUrl,
                                            null,
                                            null);
                DownloadService.this.reset();
                DownloadService.this.execute();
            }
        }
    }

    protected void decodeLargeIcon(final String articleId) {
        if (this.mLargeIcon != null) {
            this.mLargeIcon.recycle();
            this.mLargeIcon = null;
        }

        final File coverFile = HttpConstants.getCover(this,
                                                      articleId,
                                                      HttpConstants.S_COVER);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        if (coverFile.isFile()) {
            this.mLargeIcon = BitmapFactory.decodeFile(coverFile.getAbsolutePath(),
                                                       options);
        } else {
            this.mLargeIcon = BitmapFactory.decodeResource(this.getResources(),
                                                           R.drawable.default_120,
                                                           options);
        }
    }

    protected void reset() {
        if (this.mLargeIcon != null) {
            this.mLargeIcon.recycle();
            this.mLargeIcon = null;
        }
        this.mDownloading = null;
    }

    protected void sendNotification() {
        this.mBuilder.setWhen(System.currentTimeMillis());
        // 获取通知栏系统服务对象
        this.mNotifyManager.notify(this.hashCode(),
                                   this.mBuilder.getNotification());
    }

    @Override
    public IBinder onBind(final Intent arg0) {
        return null;
    }

}
