package info.tongrenlu.android.music;

import info.tongrenlu.android.downloadmanager.DownloadListener;
import info.tongrenlu.android.downloadmanager.DownloadManager;
import info.tongrenlu.android.downloadmanager.DownloadTask;
import info.tongrenlu.android.downloadmanager.DownloadTaskInfo;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.util.List;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

public class DownloadService extends Service implements DownloadListener {

    public static final int NOTIFICATION_ID = 2;

    public static final String ACTION_ADD = "info.tongrenlu.android.music.DownloadService.action.add";
    public static final String ACTION_REMOVE = "info.tongrenlu.android.music.DownloadService.action.remove";
    public static final String ACTION_START = "info.tongrenlu.android.music.DownloadService.action.start";
    public static final String ACTION_STOP = "info.tongrenlu.android.music.DownloadService.action.stop";

    private DownloadManager mDownloadManager;

    @Override
    public void onCreate() {
        TongrenluApplication app = (TongrenluApplication) this.getApplication();
        this.mDownloadManager = app.getDownloadManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopForeground(true);
        this.mDownloadManager = null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        String action = intent.getAction();
        if (ACTION_ADD.equals(action)) {
            this.processAddRequest(intent);
        } else if (ACTION_STOP.equals(action)) {
            this.stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void processAddRequest(Intent intent) {
        long playlistId = intent.getLongExtra("playlistId", 0);
        if (intent.hasExtra("trackBean")) {
            TrackBean trackBean = intent.getParcelableExtra("trackBean");
            this.addTask(trackBean, playlistId);
        } else if (intent.hasExtra("trackBeanList")) {
            List<TrackBean> trackBeanList = intent.getParcelableArrayListExtra("trackBeanList");
            for (TrackBean trackBean : trackBeanList) {
                this.addTask(trackBean, playlistId);
            }
        }
        this.mDownloadManager.start();
    }

    private void addTask(TrackBean trackBean, long playlistId) {
        Context context = this.getApplicationContext();

        String articleId = trackBean.getArticleId();
        String fileId = trackBean.getFileId();
        String from = HttpConstants.getMp3Url(context, articleId, fileId);
        String to = HttpConstants.getMp3(context, articleId, fileId)
                                 .getAbsolutePath();

        MusicDownloadTaskInfo taskinfo = new MusicDownloadTaskInfo();
        taskinfo.setTrackBean(trackBean);
        taskinfo.setFrom(from);
        taskinfo.setTo(to);
        taskinfo.setPlaylistId(playlistId);

        DownloadTask task = new MusicDownloadTask(taskinfo);
        task.registerListener(this);
        this.mDownloadManager.addTask(task);
    }

    protected void sendNotification(DownloadTaskInfo taskinfo) {
        MusicDownloadTaskInfo taskinfo2 = (MusicDownloadTaskInfo) taskinfo;
        TrackBean trackBean = taskinfo2.getTrackBean();
        final Context context = this.getApplicationContext();
        final Intent intent = new Intent(context, MusicPlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                                                                0,
                                                                intent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setOngoing(false);
        builder.setAutoCancel(true);
        String contentTitle = "download ok:" + trackBean.getTitle();
        builder.setTicker(contentTitle);
        builder.setContentTitle(contentTitle);
        builder.setWhen(System.currentTimeMillis());
        this.startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public IBinder onBind(final Intent arg0) {
        return null;
    }

    @Override
    public void onDownloadStart(DownloadTaskInfo taskinfo) {
    }

    @Override
    public void onDownloadCancel(DownloadTaskInfo taskinfo) {
    }

    @Override
    public void onDownloadFinish(DownloadTaskInfo taskinfo) {
        this.sendNotification(taskinfo);

        MusicDownloadTaskInfo taskinfo2 = (MusicDownloadTaskInfo) taskinfo;
        TrackBean trackBean = taskinfo2.getTrackBean();
        final ContentValues values = new ContentValues();
        values.put("playlist_id", taskinfo2.getPlaylistId());
        values.put("article_id", trackBean.getArticleId());
        values.put("file_id", trackBean.getFileId());
        values.put("title", trackBean.getTitle());
        values.put("artist", trackBean.getArtist());

        ContentResolver contentResolver = this.getContentResolver();
        contentResolver.insert(TongrenluContentProvider.TRACK_URI, values);

        // play
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_APPEND);
        serviceIntent.putExtra("trackBean", trackBean);
        this.startService(serviceIntent);
    }

    private boolean isTrackExists(TrackBean trackBean) {
        ContentResolver contentResolver = this.getContentResolver();
        Cursor cursor = contentResolver.query(TongrenluContentProvider.TRACK_URI,
                                              null,
                                              "article_id = ? and file_id = ?",
                                              new String[] { trackBean.getArticleId(),
                                                      trackBean.getFileId() },
                                              null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    private void insertTrack(TrackBean trackBean) {
        final ContentValues values = new ContentValues();
        values.put("article_id", trackBean.getArticleId());
        values.put("file_id", trackBean.getFileId());
        values.put("title", trackBean.getTitle());
        values.put("artist", trackBean.getArtist());

        ContentResolver contentResolver = this.getContentResolver();
        contentResolver.insert(TongrenluContentProvider.TRACK_URI, values);
    }

    @Override
    public void onDownloadProgressUpdate(DownloadTaskInfo taskinfo) {
        MusicDownloadTaskInfo taskinfo2 = (MusicDownloadTaskInfo) taskinfo;
        TrackBean trackBean = taskinfo2.getTrackBean();
        System.out.println(String.format("%s is downloading. %d%% (%d / %d)",
                                         trackBean.getTitle(),
                                         taskinfo.getProgress(),
                                         taskinfo.getRead(),
                                         taskinfo.getTotal()));
    }

    public class MusicDownloadTask extends DownloadTask {

        public MusicDownloadTask(MusicDownloadTaskInfo taskinfo) {
            super(taskinfo);
        }

        @Override
        protected DownloadTaskInfo doInBackground(Object... params) {
            MusicDownloadTaskInfo taskinfo2 = (MusicDownloadTaskInfo) this.getTaskinfo();
            TrackBean trackBean = taskinfo2.getTrackBean();
            if (!DownloadService.this.isTrackExists(trackBean)) {
                super.doInBackground(params);
                DownloadService.this.insertTrack(trackBean);
            }
            return taskinfo2;
        }
    }

    public static class MusicDownloadTaskInfo extends DownloadTaskInfo {
        private TrackBean mTrackBean = null;
        private Uri mUri = null;
        private long playlistId = 0;

        public TrackBean getTrackBean() {
            return this.mTrackBean;
        }

        public void setTrackBean(TrackBean trackBean) {
            this.mTrackBean = trackBean;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public void setUri(Uri uri) {
            this.mUri = uri;
        }

        public long getPlaylistId() {
            return this.playlistId;
        }

        public void setPlaylistId(long playlistId) {
            this.playlistId = playlistId;
        }
    }
}
