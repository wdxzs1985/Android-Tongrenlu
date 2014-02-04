package info.tongrenlu.android.music;

import info.tongrenlu.android.music.musicplayer.AudioFocusHelper;
import info.tongrenlu.android.music.musicplayer.MusicFocusable;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class MusicService extends Service implements OnCompletionListener, OnPreparedListener, OnBufferingUpdateListener, OnInfoListener, OnSeekCompleteListener, OnErrorListener, OnSharedPreferenceChangeListener, MusicFocusable {

    public static final String ACTION_TOGGLE_PLAYBACK = "info.tongrenlu.android.MusicService.action.TOGGLE_PLAYBACK";
    public static final String ACTION_TOGGLE_LOOP = "info.tongrenlu.android.MusicService.action.TOGGLE_LOOP";
    public static final String ACTION_TOGGLE_SHUFFLE = "info.tongrenlu.android.MusicService.action.TOGGLE_SHUFFLE";
    public static final String ACTION_PLAY = "info.tongrenlu.android.MusicService.action.PLAY";
    public static final String ACTION_PAUSE = "info.tongrenlu.android.MusicService.action.PAUSE";
    public static final String ACTION_STOP = "info.tongrenlu.android.MusicService.action.STOP";
    public static final String ACTION_SKIP = "info.tongrenlu.android.MusicService.action.SKIP";
    public static final String ACTION_REWIND = "info.tongrenlu.android.MusicService.action.REWIND";
    public static final String ACTION_SEEK = "info.tongrenlu.android.MusicService.action.SEEK";;
    public static final String ACTION_ADD = "info.tongrenlu.android.MusicService.action.ADD";

    public static final String EVENT_START = "info.tongrenlu.android.MusicService.EVENT_START";
    public static final String EVENT_UPDATE = "info.tongrenlu.android.MusicService.EVENT_UPDATE";
    public static final String EVENT_STOP = "info.tongrenlu.android.MusicService.EVENT_STOP";

    public static final float DUCK_VOLUME = 0.1f;
    public static final int NOTIFICATION_ID = 1;

    public static final int STATE_STOPPED = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_PAUSED = 3;

    public static final int FLAG_NO_LOOP = 0;
    public static final int FLAG_LOOP_ALL = 1;
    public static final int FLAG_LOOP_ONE = 2;

    public static final int FLAG_SEQUENCE = 0;
    public static final int FLAG_RANDOM = 1;

    enum AudioFocus {
        NoFocusNoDuck, NoFocusCanDuck, Focused
    }

    public static String toTime(final int time) {
        final int time2 = time / 1000;
        final int minute = time2 / 60;
        final int second = time2 % 60;
        return String.format("%02d:%02d", minute, second);
    }

    private LocalBroadcastManager mLocalBroadcastManager;

    private List<TrackBean> mTrackList = null;
    private LinkedList<TrackBean> mPlayList = null;
    private LinkedList<TrackBean> mHistoryList = null;

    // private TrackBean mNowDisplay = null;
    private TrackBean mNowPlaying = null;
    private int mState = STATE_STOPPED;
    private MediaPlayer mMediaPlayer = null;
    private Toast mToast = null;

    private AudioFocusHelper mAudioFocusHelper = null;
    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    private boolean mIsStreaming = false;
    private WifiLock mWifiLock;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder = null;
    private Bitmap mLargeIcon = null;

    void createMediaPlayerIfNeeded() {
        if (this.mMediaPlayer == null) {
            Context context = this.getApplicationContext();
            this.mMediaPlayer = new MediaPlayer();
            this.mMediaPlayer.setWakeMode(context,
                                          PowerManager.PARTIAL_WAKE_LOCK);
            this.mMediaPlayer.setOnPreparedListener(this);
            this.mMediaPlayer.setOnCompletionListener(this);
            this.mMediaPlayer.setOnErrorListener(this);
            this.mMediaPlayer.setOnInfoListener(this);
            this.mMediaPlayer.setOnSeekCompleteListener(this);
            this.mMediaPlayer.setOnBufferingUpdateListener(this);
        } else {
            this.mMediaPlayer.reset();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = this.getApplicationContext();
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        this.mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        this.mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,
                                                    "mylock");
        if (android.os.Build.VERSION.SDK_INT >= 8) {
            this.mAudioFocusHelper = new AudioFocusHelper(context, this);
        } else {
            this.mAudioFocus = AudioFocus.Focused;
        }
        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ANSWER");
        this.createMediaPlayerIfNeeded();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.mTrackList = new LinkedList<TrackBean>();
        this.mPlayList = new LinkedList<TrackBean>();
        this.mHistoryList = new LinkedList<TrackBean>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_TOGGLE_PLAYBACK)) {
            this.processTogglePlaybackRequest();
        } else if (action.equals(ACTION_PLAY)) {
            this.processPlayRequest();
        } else if (action.equals(ACTION_PAUSE)) {
            this.processPauseRequest();
        } else if (action.equals(ACTION_SKIP)) {
            this.processSkipRequest();
        } else if (action.equals(ACTION_STOP)) {
            this.processStopRequest();
        } else if (action.equals(ACTION_REWIND)) {
            this.processRewindRequest();
        } else if (action.equals(ACTION_ADD)) {
            this.processAddRequest(intent);
        } else if (action.equals(ACTION_SEEK)) {
            this.actionSeekTo(intent);
        }
        return START_NOT_STICKY;
    }

    private void processTogglePlaybackRequest() {
        switch (this.mState) {
        case STATE_PAUSED:
        case STATE_STOPPED:
            this.processPlayRequest();
            break;
        default:
            this.processPauseRequest();
            break;
        }
    }

    private void processPlayRequest() {
        this.tryToGetAudioFocus();
        switch (this.mState) {
        case STATE_PAUSED:
            this.mState = STATE_PLAYING;
            this.configAndStartMediaPlayer();
            break;
        case STATE_STOPPED:
            this.actionReset(this.mNowPlaying);
            break;
        default:
            break;
        }
    }

    private void processPauseRequest() {
        if (this.mState == STATE_PLAYING) {
            this.mState = STATE_PAUSED;
            this.mMediaPlayer.pause();
            this.relaxResources(false);
        }
    }

    private void processStopRequest() {
        this.actionStop();
        this.stopSelf();
    }

    private void actionStop() {
        this.mState = STATE_STOPPED;
        this.mMediaPlayer.stop();
    }

    private void processSkipRequest() {
        switch (this.mState) {
        case STATE_PLAYING:
        case STATE_PAUSED:
            this.tryToGetAudioFocus();
            this.actionPlayNext();
            break;
        default:
            break;
        }
    }

    private void processRewindRequest() {
        switch (this.mState) {
        case STATE_PLAYING:
        case STATE_PAUSED:
            this.tryToGetAudioFocus();
            this.actionPlayPrev();
            break;
        default:
            break;
        }
    }

    private void processAddRequest(Intent intent) {
        this.tryToGetAudioFocus();
        if (intent.hasExtra("trackBean")) {
            TrackBean trackBean = intent.getParcelableExtra("trackBean");
            this.actionReset(trackBean);
        } else if (intent.hasExtra("trackBeanList")) {
            List<TrackBean> trackBeanList = intent.getParcelableArrayListExtra("trackBeanList");
            this.actionInitTracklist(trackBeanList);
            final int position = intent.getIntExtra("position", 0);
            final int flag = this.getShuffleFlag();
            this.actionInitPlaylist(position, flag);
            this.actionReset(this.mPlayList.pollFirst());
        }
    }

    void configAndStartMediaPlayer() {
        if (this.mAudioFocus == AudioFocus.NoFocusNoDuck) {
            if (this.mMediaPlayer.isPlaying()) {
                this.mMediaPlayer.pause();
            }
            return;
        } else if (this.mAudioFocus == AudioFocus.NoFocusCanDuck) {
            this.mMediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
        } else {
            this.mMediaPlayer.setVolume(1.0f, 1.0f); // we can be loud
        }

        if (!this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.relaxResources(true);
        this.giveUpAudioFocus();
        //
        this.mLocalBroadcastManager.sendBroadcast(new Intent(MusicService.EVENT_STOP));
        this.mLocalBroadcastManager = null;
        //
        this.mNotificationManager.cancel(NOTIFICATION_ID);// 清除掉通知栏的信息
        this.mNotificationManager = null;

        if (this.mLargeIcon != null) {
            this.mLargeIcon.recycle();
        }

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(final MediaPlayer mp) {
        this.mState = STATE_PLAYING;
        this.configAndStartMediaPlayer();
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

        Intent intent = new Intent(MusicService.EVENT_UPDATE);
        intent.putExtra("trackBean", this.mNowPlaying);
        intent.putExtra("state", this.mState);
        if (this.mState == STATE_PLAYING || this.mState == STATE_PAUSED) {
            intent.putExtra("duration", this.mMediaPlayer.getDuration());
            intent.putExtra("progress", this.mMediaPlayer.getCurrentPosition());
        } else {
            intent.putExtra("duration", 0);
            intent.putExtra("progress", 0);
        }
        if (this.mIsStreaming) {
            intent.putExtra("percent", percent);
        }
        this.mLocalBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onSeekComplete(final MediaPlayer mp) {
        Intent intent = new Intent(MusicService.EVENT_UPDATE);
        intent.putExtra("trackBean", this.mNowPlaying);
        intent.putExtra("state", this.mState);
        if (this.mState == STATE_PLAYING || this.mState == STATE_PAUSED) {
            intent.putExtra("duration", this.mMediaPlayer.getDuration());
            intent.putExtra("progress", this.mMediaPlayer.getCurrentPosition());
        } else {
            intent.putExtra("duration", 0);
            intent.putExtra("progress", 0);
        }
        this.mLocalBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public boolean onInfo(final MediaPlayer mp, final int what, final int extra) {
        switch (what) {
        case MediaPlayer.MEDIA_INFO_UNKNOWN:
            System.out.println("MEDIA_INFO_UNKNOWN");
            break;
        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
            System.out.println("MEDIA_INFO_BUFFERING_START");
            break;
        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
            System.out.println("MEDIA_INFO_BUFFERING_END");
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

        this.processStopRequest();
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (StringUtils.equals(key, SettingsActivity.PREF_KEY_SHUFFLE_PLAY)) {
            this.onShufflePlayChanged(sharedPreferences);
        } else if (StringUtils.equals(key, SettingsActivity.PREF_KEY_LOOP_PLAY)) {
            this.onLoopPlayChanged(sharedPreferences);
        }
    }

    protected void actionInitTracklist(final List<TrackBean> trackBeanList) {
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
        this.relaxResources(false);
        this.mNowPlaying = trackBean;
        this.dispatchStartEvent(trackBean);
        final String articleId = trackBean.getArticleId();
        final String fileId = trackBean.getFileId();
        Uri data = null;
        final File source = HttpConstants.getMp3(this, articleId, fileId);
        if (source.exists()) {
            data = Uri.fromFile(source);
            this.mIsStreaming = false;
        } else {
            final String url = HttpConstants.getMp3Url(this, articleId, fileId);
            data = Uri.parse(url);
            this.mIsStreaming = true;
        }
        this.playMusic(data);
    }

    private void dispatchStartEvent(final TrackBean trackBean) {
        // this.mNowDisplay = trackBean;
        final String articleId = trackBean.getArticleId();
        this.decodeLargeIcon(articleId);
        this.sendNotification();

        Intent intent = new Intent(MusicService.EVENT_UPDATE);
        intent.putExtra("duration", 0);
        intent.putExtra("progress", 0);
        intent.putExtra("percent", 0);
        intent.putExtra("trackBean", trackBean);
        intent.putExtra("state", this.mState);
        this.mLocalBroadcastManager.sendBroadcast(intent);
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
                                                           R.drawable.default_400,
                                                           options);
        }
    }

    private void playMusic(final Uri data) {
        System.out.println(data.toString());
        this.createMediaPlayerIfNeeded();
        Context context = this.getApplicationContext();
        try {
            this.mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            this.mMediaPlayer.setDataSource(context, data);
            this.mMediaPlayer.prepareAsync();
            this.mState = MusicService.STATE_PREPARING;

            if (this.mIsStreaming) {
                this.mWifiLock.acquire();
            } else if (this.mWifiLock.isHeld()) {
                this.mWifiLock.release();
            }
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
            this.mState = MusicService.STATE_STOPPED;
        } catch (final SecurityException e) {
            e.printStackTrace();
            this.mState = MusicService.STATE_STOPPED;
        } catch (final IllegalStateException e) {
            e.printStackTrace();
            this.mState = MusicService.STATE_STOPPED;
        } catch (final IOException e) {
            e.printStackTrace();
            this.mState = MusicService.STATE_STOPPED;
        }
    }

    public void actionPlayPrev() {
        if (this.mMediaPlayer.getCurrentPosition() < 3000) {
            if (this.mNowPlaying != null) {
                this.mPlayList.addFirst(this.mNowPlaying);
            }
            if (this.mHistoryList.isEmpty()) {
                final int flag = this.getLoopFlag();
                if (flag == MusicService.FLAG_LOOP_ALL) {
                    this.mHistoryList.addAll(this.mPlayList);
                    this.mPlayList.clear();
                } else {
                    this.processStopRequest();
                    return;
                }
            }
            this.actionReset(this.mHistoryList.pollLast());
        } else {
            this.mMediaPlayer.seekTo(0);
        }
    }

    public void actionPlayNext() {
        if (this.mNowPlaying != null) {
            this.mHistoryList.addLast(this.mNowPlaying);
        }
        if (this.mPlayList.isEmpty()) {
            final int flag = this.getLoopFlag();
            if (flag != MusicService.FLAG_LOOP_ALL) {
                this.mPlayList.addAll(this.mHistoryList);
                this.mHistoryList.clear();
            } else {
                this.processStopRequest();
                return;
            }
        }
        this.actionReset(this.mPlayList.pollFirst());
    }

    private void actionSeekTo(Intent intent) {
        int progress = intent.getIntExtra("progress", 0);
        this.mMediaPlayer.seekTo(progress);
    }

    private void showToast(final String text, final int duration) {
        if (this.mToast == null) {
            Context context = this.getApplicationContext();
            this.mToast = Toast.makeText(context, text, duration);
        } else {
            this.mToast.setText(text);
            this.mToast.setDuration(duration);
        }
        this.mToast.show();
    }

    protected void sendNotification() {
        final Context context = this.getApplicationContext();
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                                                                0,
                                                                new Intent(context,
                                                                           MusicPlayerActivity.class),
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
        this.mBuilder = new NotificationCompat.Builder(context);
        this.mBuilder.setContentIntent(contentIntent);
        this.mBuilder.setTicker("正在播放:" + this.mNowPlaying.getTitle());
        this.mBuilder.setContentTitle(this.mNowPlaying.getTitle());
        this.mBuilder.setContentText(this.mNowPlaying.getArtist());
        this.mBuilder.setSmallIcon(R.drawable.ic_launcher);
        if (this.mLargeIcon != null) {
            this.mBuilder.setLargeIcon(this.mLargeIcon);
        }
        this.mBuilder.setOngoing(true);
        this.mBuilder.setAutoCancel(false);
        this.startForeground(NOTIFICATION_ID, this.mBuilder.build());
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

    private void tryToGetAudioFocus() {
        if (this.mAudioFocus != AudioFocus.Focused && this.mAudioFocusHelper != null
                && this.mAudioFocusHelper.requestFocus()) {
            this.mAudioFocus = AudioFocus.Focused;
        }
    }

    private void giveUpAudioFocus() {
        if (this.mAudioFocus == AudioFocus.Focused && this.mAudioFocusHelper != null
                && this.mAudioFocusHelper.abandonFocus()) {
            this.mAudioFocus = AudioFocus.NoFocusNoDuck;
        }
    }

    private void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        this.stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && this.mMediaPlayer != null) {
            this.mMediaPlayer.reset();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (this.mWifiLock.isHeld()) {
            this.mWifiLock.release();
        }
    }

    @Override
    public void onGainedAudioFocus() {
        Toast.makeText(this.getApplicationContext(),
                       "gained audio focus.",
                       Toast.LENGTH_SHORT).show();
        this.mAudioFocus = AudioFocus.Focused;

        // restart media player with new focus settings
        if (this.mState == STATE_PLAYING) {
            this.configAndStartMediaPlayer();
        }
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {
        Toast.makeText(this.getApplicationContext(),
                       "lost audio focus." + (canDuck ? "can duck" : "no duck"),
                       Toast.LENGTH_SHORT)
             .show();
        this.mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck
                : AudioFocus.NoFocusNoDuck;

        // start/restart/pause media player with new focus settings
        if (this.mMediaPlayer != null && this.mMediaPlayer.isPlaying()) {
            this.configAndStartMediaPlayer();
        }
    }

}
