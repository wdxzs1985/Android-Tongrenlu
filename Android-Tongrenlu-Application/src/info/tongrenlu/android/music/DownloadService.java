package info.tongrenlu.android.music;

import info.tongrenlu.android.downloadmanager.DownloadListener;
import info.tongrenlu.android.downloadmanager.DownloadManager;
import info.tongrenlu.android.downloadmanager.DownloadTask;
import info.tongrenlu.android.downloadmanager.DownloadTaskInfo;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.util.List;

import android.app.NotificationManager;
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

    public static final long BAD_ID = -1l;
    public static final int NOTIFICATION_ID = 2;

    public static final String ACTION_ADD = "info.tongrenlu.android.music.DownloadService.action.add";
    public static final String ACTION_REMOVE = "info.tongrenlu.android.music.DownloadService.action.remove";
    public static final String ACTION_START = "info.tongrenlu.android.music.DownloadService.action.start";
    public static final String ACTION_STOP = "info.tongrenlu.android.music.DownloadService.action.stop";

    private DownloadManager mDownloadManager;
    private NotificationManager mNotificationManager = null;

    @Override
    public void onCreate() {
        final TongrenluApplication app = (TongrenluApplication) this.getApplication();
        this.mDownloadManager = app.getDownloadManager();
        this.mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopForeground(true);
        this.mDownloadManager = null;

        this.mNotificationManager.cancel(DownloadService.NOTIFICATION_ID);
        this.mNotificationManager = null;
    }

    @Override
    public int onStartCommand(final Intent intent,
                              final int flags,
                              final int startId) {
        final String action = intent.getAction();
        if (DownloadService.ACTION_ADD.equals(action)) {
            this.processAddRequest(intent);
        } else if (DownloadService.ACTION_STOP.equals(action)) {
            this.stopSelf();
        }
        return Service.START_NOT_STICKY;
    }

    private void processAddRequest(final Intent intent) {
        final long playlistId = intent.getLongExtra("playlistId",
                                                    DownloadService.BAD_ID);
        if (playlistId == DownloadService.BAD_ID) {
            return;
        }
        if (intent.hasExtra("trackBean")) {
            final TrackBean trackBean = intent.getParcelableExtra("trackBean");
            this.addTask(trackBean, playlistId);
        } else if (intent.hasExtra("trackBeanList")) {
            final List<TrackBean> trackBeanList = intent.getParcelableArrayListExtra("trackBeanList");
            for (final TrackBean trackBean : trackBeanList) {
                this.addTask(trackBean, playlistId);
            }
        }
        this.mDownloadManager.start();
    }

    private void addTask(final TrackBean trackBean, final long playlistId) {
        final Context context = this.getApplicationContext();

        final String articleId = trackBean.getArticleId();
        final String fileId = trackBean.getFileId();
        final String from = HttpConstants.getMp3Url(context, articleId, fileId);
        final String to = HttpConstants.getMp3(context, articleId, fileId)
                                       .getAbsolutePath();

        final MusicDownloadTaskInfo taskinfo = new MusicDownloadTaskInfo();
        taskinfo.setTrackBean(trackBean);
        taskinfo.setFrom(from);
        taskinfo.setTo(to);
        taskinfo.setPlaylistId(playlistId);

        final DownloadTask task = new MusicDownloadTask(taskinfo);
        task.registerListener(this);
        this.mDownloadManager.addTask(task);
    }

    protected void sendNotification(final boolean isFinish) {
        final Context context = this.getApplicationContext();
        final Intent intent = new Intent(context, MusicPlayerActivity.class);
        final PendingIntent contentIntent = PendingIntent.getActivity(context,
                                                                      0,
                                                                      intent,
                                                                      PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(contentIntent);
        builder.setSmallIcon(R.drawable.ic_launcher);

        if (isFinish) {
            final String downloadFinish = this.getString(R.string.download_finish);
            builder.setTicker(downloadFinish);
            builder.setContentTitle(downloadFinish);
            builder.setContentText("");
        } else {
            final List<DownloadTask> tasks = this.mDownloadManager.getTasks();
            final DownloadTask runningTask = this.mDownloadManager.getRunning();
            final String songTitle = ((MusicDownloadTaskInfo) runningTask.getTaskinfo()).getTrackBean()
                                                                                        .getSongTitle();
            final String downloading = this.getString(R.string.downloading,
                                                      songTitle);
            builder.setTicker(downloading);
            builder.setContentTitle(downloading);
            builder.setContentText(this.getString(R.string.download_waiting,
                                                  tasks.size()));
        }
        builder.setWhen(System.currentTimeMillis());

        this.mNotificationManager.notify(DownloadService.NOTIFICATION_ID,
                                         builder.build());
    }

    @Override
    public IBinder onBind(final Intent arg0) {
        return null;
    }

    @Override
    public void onDownloadStart(final DownloadTaskInfo taskinfo) {
        this.sendNotification(false);
    }

    @Override
    public void onDownloadCancel(final DownloadTaskInfo taskinfo) {
        this.mNotificationManager.cancel(DownloadService.NOTIFICATION_ID);
    }

    @Override
    public void onDownloadFinish(final DownloadTaskInfo taskinfo) {
        if (taskinfo != null) {
            this.sendNotification(true);

            final MusicDownloadTaskInfo taskinfo2 = (MusicDownloadTaskInfo) taskinfo;
            final TrackBean trackBean = taskinfo2.getTrackBean();

            this.updateTrackState(trackBean);

            final long playlistId = taskinfo2.getPlaylistId();
            this.insertPlaylistTrack(trackBean, playlistId);

            // play
            final Intent serviceIntent = new Intent(this, MusicService.class);
            serviceIntent.setAction(MusicService.ACTION_APPEND);
            serviceIntent.putExtra("trackBean", trackBean);
            this.startService(serviceIntent);
        }
    }

    private void updateTrackState(final TrackBean trackBean) {
        final ContentValues values = new ContentValues();
        values.put("downloadFlg", "1");

        final ContentResolver contentResolver = this.getContentResolver();
        contentResolver.update(TongrenluContentProvider.TRACK_URI,
                               values,
                               "articleId = ? and fileId = ? and downloadFlg = 0",
                               new String[] { trackBean.getArticleId(),
                                             trackBean.getFileId() });

        final Uri albumTrackContentUri = Uri.withAppendedPath(TongrenluContentProvider.ALBUM_URI,
                                                              trackBean.getArticleId() + "/track");

        contentResolver.notifyChange(albumTrackContentUri, null);
        contentResolver.notifyChange(TongrenluContentProvider.TRACK_URI, null);
    }

    private void insertPlaylistTrack(final TrackBean trackBean,
                                     final long playlistId) {
        final ContentValues values = new ContentValues();
        values.put("articleId", trackBean.getArticleId());
        values.put("fileId", trackBean.getFileId());
        values.put("album", trackBean.getAlbum());
        values.put("songTitle", trackBean.getSongTitle());
        values.put("leadArtist", trackBean.getLeadArtist());

        final Uri contentUri = Uri.withAppendedPath(TongrenluContentProvider.PLAYLIST_URI,
                                                    playlistId + "/track");
        final ContentResolver contentResolver = this.getContentResolver();

        if (trackBean.getTrackNumber() == 0) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(contentUri,
                                               null,
                                               null,
                                               null,
                                               null);
                values.put("trackNumber", cursor.getCount() + 1);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            values.put("trackNumber", trackBean.getTrackNumber());
        }

        contentResolver.insert(contentUri, values);
        contentResolver.notifyChange(contentUri, null);
    }

    @Override
    public void onDownloadProgressUpdate(final DownloadTaskInfo taskinfo) {
        // final MusicDownloadTaskInfo taskinfo2 = (MusicDownloadTaskInfo)
        // taskinfo;
        // final TrackBean trackBean = taskinfo2.getTrackBean();
        // System.out.println(String.format("%s is downloading. %d%% (%d / %d)",
        // trackBean.getSongTitle(),
        // taskinfo.getProgress(),
        // taskinfo.getRead(),
        // taskinfo.getTotal()));
    }

    public class MusicDownloadTask extends DownloadTask {

        public MusicDownloadTask(final MusicDownloadTaskInfo taskinfo) {
            super(taskinfo);
        }

        @Override
        protected DownloadTaskInfo doInBackground(final Object... params) {
            final MusicDownloadTaskInfo taskinfo2 = (MusicDownloadTaskInfo) this.getTaskinfo();
            final TrackBean trackBean = taskinfo2.getTrackBean();
            if (trackBean.getDownloadFlg() == 0) {
                if (super.doInBackground(params) != null) {
                } else {
                    return null;
                }
            }
            return taskinfo2;
        }
    }

    public static class MusicDownloadTaskInfo extends DownloadTaskInfo {
        private TrackBean mTrackBean = null;
        private Uri mUri = null;
        private long playlistId = -1;

        public TrackBean getTrackBean() {
            return this.mTrackBean;
        }

        public void setTrackBean(final TrackBean trackBean) {
            this.mTrackBean = trackBean;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public void setUri(final Uri uri) {
            this.mUri = uri;
        }

        public long getPlaylistId() {
            return this.playlistId;
        }

        public void setPlaylistId(final long playlistId) {
            this.playlistId = playlistId;
        }
    }
}
