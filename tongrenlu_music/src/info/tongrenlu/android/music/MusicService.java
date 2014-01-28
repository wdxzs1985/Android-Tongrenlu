package info.tongrenlu.android.music;

import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ServiceCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class MusicService extends Service implements OnCompletionListener, OnPreparedListener, OnBufferingUpdateListener, OnInfoListener, OnSeekCompleteListener, OnErrorListener, OnSharedPreferenceChangeListener {

    public static final String ACTION_INIT = "info.tongrenlu.android.MusicService.ACTION_INIT";
    public static final String ACTION_STOP = "info.tongrenlu.android.MusicService.ACTION_STOP";
    public static final String ACTION_ONLINE = "info.tongrenlu.android.MusicService.ACTION_ONLINE";

    public static final String EVENT_START = "info.tongrenlu.android.MusicService.EVENT_START";
    public static final String EVENT_UPDATE = "info.tongrenlu.android.MusicService.EVENT_UPDATE";
    public static final String EVENT_STOP = "info.tongrenlu.android.MusicService.EVENT_STOP";
    public static final String EVENT_BUFFERING_START = "info.tongrenlu.android.MusicService.EVENT_BUFFERING_START";
    public static final String EVENT_BUFFERING_END = "info.tongrenlu.android.MusicService.EVENT_BUFFERING_END";

    public static final int FLAG_STOP = 0;
    public static final int FLAG_PLAY = 1;
    public static final int FLAG_PAUSE = 2;

    public static final int FLAG_NO_LOOP = 0;
    public static final int FLAG_LOOP_ALL = 1;
    public static final int FLAG_LOOP_ONE = 2;

    public static final int FLAG_SEQUENCE = 0;
    public static final int FLAG_RANDOM = 1;

    public static final int MSG_PLAYING = 1;

    public static String toTime(final int time) {
        final int time2 = time / 1000;
        final int minute = time2 / 60;
        final int second = time2 % 60;
        return String.format("%02d:%02d", minute, second);
    }

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mIncomingPhoneReceiver = null;

    private List<TrackBean> mTrackList = null;
    private LinkedList<TrackBean> mPlayList = null;
    private LinkedList<TrackBean> mHistoryList = null;

    private TrackBean mNowDisplay = null;
    private TrackBean mNowPlaying = null;
    private int mPercent = 0;
    private int mPlayflag = MusicService.FLAG_STOP;// 标记
    private MediaPlayer mMediaPlayer = null;
    // Binder given to clients
    private IBinder mBinder = null;
    private Toast mToast = null;
    private Handler mPlayHandler = null;

    private NotificationManager mNotificationManager = null;
    private NotificationCompat.Builder mBuilder = null;
    private Bitmap mLargeIcon = null;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        this.mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        this.mBinder = new LocalBinder();
        this.mIncomingPhoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_ANSWER)) {
                    final TelephonyManager telephonymanager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    switch (telephonymanager.getCallState()) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        MusicService.this.actionPause();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        MusicService.this.actionResume();
                        break;
                    default:
                        break;
                    }
                }
            }
        };

        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ANSWER");
        this.mLocalBroadcastManager.registerReceiver(this.mIncomingPhoneReceiver,
                                                     filter);

        this.mMediaPlayer = new MediaPlayer();
        this.mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        this.mMediaPlayer.setOnCompletionListener(this);
        this.mMediaPlayer.setOnPreparedListener(this);
        this.mMediaPlayer.setOnBufferingUpdateListener(this);
        this.mMediaPlayer.setOnErrorListener(this);
        this.mMediaPlayer.setOnInfoListener(this);
        this.mMediaPlayer.setOnSeekCompleteListener(this);

        this.mPlayHandler = new Handler() {

            @Override
            public void handleMessage(final Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                case FLAG_PLAY:
                    int duration = 0;
                    int progress = 0;
                    if (MusicService.this.mMediaPlayer.isPlaying()) {
                        duration = MusicService.this.mMediaPlayer.getDuration();
                        progress = MusicService.this.mMediaPlayer.getCurrentPosition();
                    }
                    final Intent intent = new Intent(MusicService.EVENT_UPDATE);
                    intent.putExtra("duration", duration);
                    intent.putExtra("progress", progress);
                    intent.putExtra("percent", MusicService.this.mPercent);
                    MusicService.this.mLocalBroadcastManager.sendBroadcast(intent);

                    // if (Build.VERSION.SDK_INT >=
                    // Build.VERSION_CODES.HONEYCOMB) {
                    // MusicService.this.mBuilder.setProgress(duration,
                    // progress,
                    // false);
                    // MusicService.this.sendNotification();
                    // }

                    final Message newMessage = this.obtainMessage(MusicService.FLAG_PLAY);
                    this.sendMessageDelayed(newMessage, 1000);
                    break;

                default:
                    break;
                }
            }
        };

        this.createNotification();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        // this.initShufflePlay(sharedPreferences);
        // this.initLoopPlay(sharedPreferences);
        // this.mPosition = 0;
        this.mTrackList = new LinkedList<TrackBean>();
        this.mPlayList = new LinkedList<TrackBean>();
        this.mHistoryList = new LinkedList<TrackBean>();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        final String action = intent.getAction();
        if (StringUtils.equals(MusicService.ACTION_INIT, action)) {
            final ArrayList<TrackBean> trackBeanList = intent.getParcelableArrayListExtra("trackBeanList");
            final int position = intent.getIntExtra("position", 0);
            // this.mPosition = intent.getIntExtra("position", this.mPosition);
            this.actionInitTracklist(trackBeanList);
            final int flag = this.getShuffleFlag();
            this.actionInitPlaylist(position, flag);
            this.actionReset(this.mPlayList.pollFirst());
        } else if (StringUtils.equals(MusicService.ACTION_STOP, action)) {
            this.actionStop();
        } else if (StringUtils.equals(MusicService.ACTION_ONLINE, action)) {
            final TrackBean trackBean = intent.getParcelableExtra("trackBean");
            this.playMusicOnline(trackBean);
        }
        return ServiceCompat.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //
        this.mLocalBroadcastManager.unregisterReceiver(this.mIncomingPhoneReceiver);
        this.mLocalBroadcastManager = null;
        //
        this.mMediaPlayer.release();
        this.mMediaPlayer = null;
        //
        this.mNotificationManager.cancel(this.hashCode());// 清除掉通知栏的信息
        this.mNotificationManager = null;

        if (this.mLargeIcon != null) {
            this.mLargeIcon.recycle();
        }

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return this.mBinder;
    }

    @Override
    public void onPrepared(final MediaPlayer mp) {
        this.actionResume();
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        final int flag = this.getLoopFlag();
        if (flag == MusicService.FLAG_LOOP_ONE) {
            this.actionReset(this.mNowPlaying);
        } else {
            this.actionPlayNext();
        }
    }

    @Override
    public void onBufferingUpdate(final MediaPlayer mp, final int percent) {
        System.out.println("percent " + percent);
        this.mPercent = percent;
    }

    @Override
    public void onSeekComplete(final MediaPlayer mp) {
        System.out.println("onSeekComplete");
    }

    @Override
    public boolean onInfo(final MediaPlayer mp, final int what, final int extra) {
        switch (what) {
        case MediaPlayer.MEDIA_INFO_UNKNOWN:
            System.out.println("MEDIA_INFO_UNKNOWN");
            break;
        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            System.out.println("MEDIA_INFO_BUFFERING_START");
            final Intent bufferingStartIntent = new Intent(MusicService.EVENT_BUFFERING_START);
            this.mLocalBroadcastManager.sendBroadcast(bufferingStartIntent);
            break;
        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            System.out.println("MEDIA_INFO_BUFFERING_END");
            final Intent bufferingEndIntent = new Intent(MusicService.EVENT_BUFFERING_END);
            this.mLocalBroadcastManager.sendBroadcast(bufferingEndIntent);
            break;
        case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
            System.out.println("MEDIA_INFO_BAD_INTERLEAVING");
            break;
        case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
            System.out.println("MEDIA_INFO_NOT_SEEKABLE");
            break;
        case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
            System.out.println("MEDIA_INFO_METADATA_UPDATE");
            break;
        default:
            break;
        }
        return false;
    }

    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        switch (what) {
        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
            System.out.println("MEDIA_ERROR_UNKNOWN");
            break;
        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
            System.out.println("MEDIA_ERROR_SERVER_DIED");
            break;
        }

        switch (extra) {
        case MediaPlayer.MEDIA_ERROR_IO:
            System.out.println("MEDIA_ERROR_IO");
            break;
        case MediaPlayer.MEDIA_ERROR_MALFORMED:
            System.out.println("MEDIA_ERROR_MALFORMED");
            break;
        case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
            System.out.println("MEDIA_ERROR_UNSUPPORTED");
            break;
        case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
            System.out.println("MEDIA_ERROR_TIMED_OUT");
            break;
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (StringUtils.equals(key, SettingsActivity.PREF_KEY_SHUFFLE_PLAY)) {
            this.onShufflePlayChanged(sharedPreferences);
        } else if (StringUtils.equals(key, SettingsActivity.PREF_KEY_LOOP_PLAY)) {
            this.onLoopPlayChanged(sharedPreferences);
        }
    }

    protected void actionInitTracklist(final ArrayList<TrackBean> trackBeanList) {
        if (!trackBeanList.isEmpty()) {
            if (this.mTrackList != null) {
                this.mTrackList.clear();
            }
            this.mTrackList.addAll(trackBeanList);
        }
    }

    protected void actionInitPlaylist(final int position, final int flag) {
        this.mPlayList.clear();
        this.mHistoryList.clear();
        this.mPlayList.addAll(this.mTrackList);
        int index = position;
        if (flag == MusicService.FLAG_RANDOM) {
            Collections.shuffle(this.mPlayList);
            index = this.mPlayList.indexOf(this.mTrackList.get(index));
        }
        for (int i = 0; i < index; i++) {
            this.mHistoryList.addLast(this.mPlayList.pollFirst());
        }
    }

    protected void actionReset(final TrackBean trackBean) {
        this.mNowPlaying = trackBean;
        this.dispatchStartEvent(trackBean);
        final String articleId = trackBean.getArticleId();
        final String fileId = trackBean.getFileId();
        Uri data = null;
        final File source = HttpConstants.getMp3(this, articleId, fileId);
        if (source.exists()) {
            data = Uri.fromFile(source);
        } else {
            final String url = HttpConstants.getMp3Url(this, articleId, fileId);
            data = Uri.parse(url);
        }
        this.playMusic(data);
    }

    private void playMusicOnline(final TrackBean trackBean) {
        this.dispatchStartEvent(trackBean);
        final String articleId = trackBean.getArticleId();
        final String fileId = trackBean.getFileId();
        Uri data = null;
        final File source = HttpConstants.getMp3(this, articleId, fileId);
        if (source.exists()) {
            data = Uri.fromFile(source);
            this.playMusic(data);
        } else {
            final String url = HttpConstants.getMp3Url(this, articleId, fileId);
            data = Uri.parse(url);
        }
        this.playMusic(data);
    }

    private void dispatchStartEvent(final TrackBean trackBean) {
        this.mNowDisplay = trackBean;
        final String articleId = trackBean.getArticleId();
        final String title = trackBean.getTitle();
        final String artist = trackBean.getArtist();

        this.decodeLargeIcon(articleId);
        this.mBuilder.setTicker("正在播放:" + title)
                     .setContentTitle(title)
                     .setContentText(artist)
                     .setLargeIcon(this.mLargeIcon);
        this.sendNotification();

        // this.showNotifcation(articleId, title, artist);
        final Intent startIntent = new Intent(MusicService.EVENT_START);
        this.mLocalBroadcastManager.sendBroadcast(startIntent);
    }

    private void decodeLargeIcon(final String articleId) {
        if (this.mLargeIcon != null) {
            this.mLargeIcon.recycle();
            this.mLargeIcon = null;
        }

        final File coverFile = HttpConstants.getCover(this,
                                                      articleId,
                                                      HttpConstants.L_COVER);
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        if (coverFile.isFile()) {
            this.mLargeIcon = BitmapFactory.decodeFile(coverFile.getAbsolutePath(),
                                                       options);
        } else {
            this.mLargeIcon = BitmapFactory.decodeResource(this.getResources(),
                                                           R.drawable.default_180,
                                                           options);
        }
    }

    private void playMusic(final Uri data) {
        System.out.println(data.toString());

        this.mPercent = 0;
        this.mMediaPlayer.reset();
        try {
            this.mMediaPlayer.setDataSource(this, data);
            this.mMediaPlayer.prepareAsync();
            this.mPlayflag = MusicService.FLAG_PAUSE;
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
            this.mPlayflag = MusicService.FLAG_STOP;
        } catch (final SecurityException e) {
            e.printStackTrace();
            this.mPlayflag = MusicService.FLAG_STOP;
        } catch (final IllegalStateException e) {
            e.printStackTrace();
            this.mPlayflag = MusicService.FLAG_STOP;
        } catch (final IOException e) {
            e.printStackTrace();
            this.mPlayflag = MusicService.FLAG_STOP;
        }
    }

    public void actionPlay() {
        switch (this.mPlayflag) {
        case MusicService.FLAG_PLAY:
            this.actionPause();
            break;
        case MusicService.FLAG_PAUSE:
            this.actionResume();
            break;
        case MusicService.FLAG_STOP:
            this.actionReset(this.mNowPlaying);
            break;
        }
    }

    protected void actionPause() {
        this.mPlayflag = MusicService.FLAG_PAUSE;
        this.mMediaPlayer.pause();
        this.mPlayHandler.removeMessages(MusicService.FLAG_PLAY);
    }

    protected void actionResume() {
        this.mPlayflag = MusicService.FLAG_PLAY;
        this.mMediaPlayer.start();
        final Message msg = this.mPlayHandler.obtainMessage(MusicService.FLAG_PLAY);
        this.mPlayHandler.sendMessage(msg);
    }

    public void actionStop() {
        this.mPlayflag = MusicService.FLAG_STOP;
        this.mPlayHandler.removeMessages(MusicService.FLAG_PLAY);
        final Intent stopIntent = new Intent(MusicService.EVENT_STOP);
        this.mLocalBroadcastManager.sendBroadcast(stopIntent);
        this.stopSelf();
    }

    public void actionPlayPrev() {
        if (this.mNowPlaying == null) {
            this.actionStop();
            return;
        }
        if (this.mMediaPlayer.getCurrentPosition() < 3000) {
            this.mPlayList.addFirst(this.mNowPlaying);
            if (this.mHistoryList.isEmpty()) {
                final int flag = this.getLoopFlag();
                if (flag != MusicService.FLAG_LOOP_ALL) {
                    this.actionStop();
                    return;
                } else {
                    this.mHistoryList.addAll(this.mPlayList);
                    this.mPlayList.clear();
                }
            }
            this.actionReset(this.mHistoryList.pollLast());
        } else {
            this.mMediaPlayer.seekTo(0);
        }
    }

    public void actionPlayNext() {
        if (this.mNowPlaying == null) {
            this.actionStop();
            return;
        }
        this.mHistoryList.addLast(this.mNowPlaying);
        if (this.mPlayList.isEmpty()) {
            final int flag = this.getLoopFlag();
            if (flag != MusicService.FLAG_LOOP_ALL) {
                this.actionStop();
                return;
            } else {
                this.mPlayList.addAll(this.mHistoryList);
                this.mHistoryList.clear();
            }
        }
        this.actionReset(this.mPlayList.pollFirst());
    }

    public void actionLoop() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_LOOP_PLAY,
                                                         SettingsActivity.PREF_DEFAULT_LOOP_PLAY);
        final Resources res = this.getResources();
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_loop_play);
        final int index = ArrayUtils.indexOf(entryValues, value);
        final int nextIndex = (index + 1) % entryValues.length;
        sharedPreferences.edit()
                         .putString(SettingsActivity.PREF_KEY_LOOP_PLAY,
                                    entryValues[nextIndex])
                         .commit();
    }

    public void actionRandom() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
                                                         SettingsActivity.PREF_DEFAULT_SHUFFLE_PLAY);
        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_shuffle_play);
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_shuffle_play);
        final int index = ArrayUtils.indexOf(entryValues, value);
        final int nextIndex = (index + 1) % entryValues.length;
        sharedPreferences.edit()
                         .putString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
                                    entryValues[nextIndex])
                         .commit();
        this.showToast(entries[nextIndex], Toast.LENGTH_LONG);
    }

    private void showToast(final String text, final int duration) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this, text, duration);
        } else {
            this.mToast.setText(text);
            this.mToast.setDuration(duration);
        }
        this.mToast.show();
    }

    protected void createNotification() {
        final Context context = this.getApplicationContext();
        this.mBuilder = new NotificationCompat.Builder(context);
        final Intent intent = new Intent(context, MusicPlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent contentIntent = PendingIntent.getActivity(context,
                                                                      0,
                                                                      intent,
                                                                      PendingIntent.FLAG_UPDATE_CURRENT);
        this.mBuilder.setContentIntent(contentIntent)
                     .setSmallIcon(R.drawable.ic_launcher)
                     .setAutoCancel(false)
                     .setOngoing(true);

        // final Intent stopAction = new Intent(context, MusicService.class);
        // stopAction.setAction(MusicService.ACTION_STOP);
        // final PendingIntent stopActionIntent =
        // PendingIntent.getService(context,
        // 0,
        // stopAction,
        // PendingIntent.FLAG_UPDATE_CURRENT);
        // this.mBuilder.addAction(R.drawable.av_stop,
        // context.getString(R.string.player_stop),
        // stopActionIntent);

    }

    protected void sendNotification() {
        this.mBuilder.setWhen(System.currentTimeMillis());
        // 获取通知栏系统服务对象
        this.mNotificationManager.notify(this.hashCode(),
                                         this.mBuilder.getNotification());
    }

    private void onShufflePlayChanged(final SharedPreferences sharedPreferences) {
        final int flag = this.getShuffleFlag();
        //
        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_shuffle_play);
        this.showToast(entries[flag], Toast.LENGTH_LONG);

        this.mPlayList.clear();
        this.mHistoryList.clear();
        this.mPlayList = new LinkedList<TrackBean>(this.mTrackList);
        if (flag == MusicService.FLAG_RANDOM) {
            Collections.shuffle(this.mPlayList);
        } else {
            while (!this.mPlayList.isEmpty()) {
                final TrackBean next = this.mPlayList.removeFirst();
                this.mHistoryList.addLast(next);
                if (StringUtils.equals(this.mNowPlaying.getFileId(),
                                       next.getFileId())) {
                    break;
                }
            }
        }
    }

    public int getShuffleFlag() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
                                                         SettingsActivity.PREF_DEFAULT_SHUFFLE_PLAY);
        final Resources res = this.getResources();
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_shuffle_play);
        return ArrayUtils.indexOf(entryValues, value);
    }

    private void onLoopPlayChanged(final SharedPreferences sharedPreferences) {
        final int index = this.getLoopFlag();
        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_loop_play);
        this.showToast(entries[index], Toast.LENGTH_LONG);
    }

    public int getLoopFlag() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_LOOP_PLAY,
                                                         SettingsActivity.PREF_DEFAULT_LOOP_PLAY);
        final Resources res = this.getResources();
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_loop_play);
        return ArrayUtils.indexOf(entryValues, value);
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        MusicService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return MusicService.this;
        }
    }

    public int getPlayflag() {
        return this.mPlayflag;
    }

    public MediaPlayer getMediaPlayer() {
        return this.mMediaPlayer;
    }

    public TrackBean getNowDisplay() {
        return this.mNowDisplay;
    }

}
