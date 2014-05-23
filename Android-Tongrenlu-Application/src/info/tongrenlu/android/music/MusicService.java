package info.tongrenlu.android.music;

import info.tongrenlu.android.image.LoadImageTask;
import info.tongrenlu.android.player.AudioBecomingNoisyReceiver;
import info.tongrenlu.android.player.AudioFocusHelper;
import info.tongrenlu.android.player.IncomingPhoneReceiver;
import info.tongrenlu.android.player.MusicFocusable;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;
import info.tongrenlu.support.ApplicationSupport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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

    public static final String ACTION_QUERY = "info.tongrenlu.android.MusicService.action.QUERY";
    public static final String ACTION_TOGGLE_PLAYBACK = "info.tongrenlu.android.MusicService.action.TOGGLE_PLAYBACK";
    public static final String ACTION_TOGGLE_LOOP = "info.tongrenlu.android.MusicService.action.TOGGLE_LOOP";
    public static final String ACTION_TOGGLE_SHUFFLE = "info.tongrenlu.android.MusicService.action.TOGGLE_SHUFFLE";
    public static final String ACTION_PLAY = "info.tongrenlu.android.MusicService.action.PLAY";
    public static final String ACTION_PAUSE = "info.tongrenlu.android.MusicService.action.PAUSE";
    public static final String ACTION_STOP = "info.tongrenlu.android.MusicService.action.STOP";
    public static final String ACTION_SKIP = "info.tongrenlu.android.MusicService.action.SKIP";
    public static final String ACTION_REWIND = "info.tongrenlu.android.MusicService.action.REWIND";
    public static final String ACTION_SEEK = "info.tongrenlu.android.MusicService.action.SEEK";
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

    public static final int QUERY_STATE = 1;
    public static final int QUERY_PLAYLIST = 2;

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
    private final IncomingPhoneReceiver mIncomingPhoneReceiver = new IncomingPhoneReceiver();
    private final AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();

    private final ArrayList<TrackBean> mTrackList = new ArrayList<TrackBean>();
    private final ArrayList<TrackBean> mPlayList = new ArrayList<TrackBean>();
    private int mPosition = 0;

    private int mDuration = 0;
    private int mProgress = 0;
    private int mPercent = 0;
    // private TrackBean mNowDisplay = null;
    private TrackBean mNowPlaying = null;
    private int mState = MusicService.STATE_STOPPED;
    private MediaPlayer mMediaPlayer = null;
    private Toast mToast = null;

    private AudioFocusHelper mAudioFocusHelper = null;
    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;
    private boolean mIsStreaming = false;
    private WifiLock mWifiLock;

    @Override
    public void onCreate() {
        super.onCreate();

        this.createMediaPlayerIfNeeded();
        this.createWifiLock();
        this.createAudioFocusHelper();
        this.registerReceiver();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void createMediaPlayerIfNeeded() {
        if (this.mMediaPlayer == null) {

            this.mMediaPlayer = new MediaPlayer();
            this.mMediaPlayer.setWakeMode(this.getApplicationContext(),
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

    private void createWifiLock() {
        final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        this.mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,
                                                    "mylock");
    }

    private void createAudioFocusHelper() {
        final Context context = this.getApplicationContext();
        if (ApplicationSupport.canUseAudioFocusHelper()) {
            this.mAudioFocusHelper = new AudioFocusHelper(context, this);
        } else {
            this.mAudioFocus = AudioFocus.Focused;
        }
    }

    private void registerReceiver() {
        final Context context = this.getApplicationContext();
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
        final IntentFilter incomingPhoneFilter = new IntentFilter();
        incomingPhoneFilter.addAction(Intent.ACTION_ANSWER);
        incomingPhoneFilter.addAction(Intent.ACTION_CALL);
        this.mLocalBroadcastManager.registerReceiver(this.mIncomingPhoneReceiver,
                                                     incomingPhoneFilter);

        IntentFilter audioBecomingNoisyfilter = new IntentFilter();
        audioBecomingNoisyfilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        this.mLocalBroadcastManager.registerReceiver(this.mAudioBecomingNoisyReceiver,
                                                     audioBecomingNoisyfilter);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        final String action = intent.getAction();
        if (MusicService.ACTION_TOGGLE_PLAYBACK.equals(action)) {
            this.processTogglePlaybackRequest();
            this.progressQueryStateRequest(false);
        } else if (MusicService.ACTION_PLAY.equals(action)) {
            this.processPlayRequest();
            this.progressQueryStateRequest(false);
        } else if (MusicService.ACTION_PAUSE.equals(action)) {
            this.processPauseRequest();
            this.progressQueryStateRequest(false);
        } else if (MusicService.ACTION_SKIP.equals(action)) {
            this.processSkipRequest();
            this.progressQueryStateRequest(false);
        } else if (MusicService.ACTION_REWIND.equals(action)) {
            this.processRewindRequest();
            this.progressQueryStateRequest(false);
        } else if (MusicService.ACTION_ADD.equals(action)) {
            this.processAddRequest(intent);
        } else if (MusicService.ACTION_STOP.equals(action)) {
            this.processStopRequest();
        } else if (MusicService.ACTION_SEEK.equals(action)) {
            this.actionSeekTo(intent);
        } else if (MusicService.ACTION_QUERY.equals(action)) {
            boolean includePlaylist = intent.getBooleanExtra("includePlaylist",
                                                             false);
            this.progressQueryStateRequest(includePlaylist);
        }
        return Service.START_NOT_STICKY;
    }

    private void progressQueryStateRequest(boolean includePlaylist) {
        final Intent intent = new Intent(MusicService.EVENT_UPDATE);
        intent.putExtra("state", this.mState);
        this.mDuration = 0;
        this.mProgress = 0;
        if (this.mState == MusicService.STATE_PLAYING || this.mState == MusicService.STATE_PAUSED) {
            this.mDuration = this.mMediaPlayer.getDuration();
            this.mProgress = this.mMediaPlayer.getCurrentPosition();
        }
        intent.putExtra("trackBean", this.mNowPlaying);
        intent.putExtra("position", this.mPosition);
        if (includePlaylist) {
            intent.putExtra("playlist", this.mPlayList);
        }
        intent.putExtra("duration", this.mDuration);
        intent.putExtra("progress", this.mProgress);
        if (this.mIsStreaming) {
            intent.putExtra("percent", this.mPercent);
        }
        this.mLocalBroadcastManager.sendBroadcast(intent);
    }

    private void processTogglePlaybackRequest() {
        switch (this.mState) {
        case STATE_PLAYING:
            this.processPauseRequest();
            break;
        case STATE_PAUSED:
            this.processPlayRequest();
        default:
            break;
        }
    }

    private void processPlayRequest() {
        this.tryToGetAudioFocus();
        if (this.mState == MusicService.STATE_PAUSED) {
            this.mState = MusicService.STATE_PLAYING;
            this.configAndStartMediaPlayer();
        }
    }

    private void processPauseRequest() {
        if (this.mState == MusicService.STATE_PLAYING) {
            this.mState = MusicService.STATE_PAUSED;
            this.mMediaPlayer.pause();
            this.relaxResources(false);
        }
    }

    private void processStopRequest() {
        this.actionStop();
        this.stopSelf();
    }

    private void actionStop() {
        this.mState = MusicService.STATE_STOPPED;
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

    private void processAddRequest(final Intent intent) {
        this.tryToGetAudioFocus();
        if (intent.hasExtra("trackBeanList")) {
            final List<TrackBean> trackBeanList = intent.getParcelableArrayListExtra("trackBeanList");
            this.actionInitTracklist(trackBeanList);
        }
        final int position = intent.getIntExtra("position", 0);
        this.mNowPlaying = this.mPlayList.get(position);
        this.actionInitPlaylist();
        this.actionReset();
    }

    void configAndStartMediaPlayer() {
        if (this.mAudioFocus == AudioFocus.NoFocusNoDuck) {
            if (this.mMediaPlayer.isPlaying()) {
                this.mMediaPlayer.pause();
            }
            return;
        } else if (this.mAudioFocus == AudioFocus.NoFocusCanDuck) {
            this.mMediaPlayer.setVolume(MusicService.DUCK_VOLUME,
                                        MusicService.DUCK_VOLUME);
        } else {
            this.mMediaPlayer.setVolume(1.0f, 1.0f); // we can be loud
        }

        if (!this.mMediaPlayer.isPlaying()) {
            this.mMediaPlayer.start();
            this.sendNotification(this.mNowPlaying);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.relaxResources(true);
        this.giveUpAudioFocus();
        //
        this.mLocalBroadcastManager.unregisterReceiver(this.mIncomingPhoneReceiver);
        this.mLocalBroadcastManager.sendBroadcast(new Intent(MusicService.EVENT_STOP));

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onPrepared(final MediaPlayer mp) {
        this.mState = MusicService.STATE_PLAYING;
        this.configAndStartMediaPlayer();
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        final int flag = this.getLoopFlag();
        if (flag == MusicService.FLAG_LOOP_ONE) {
            this.actionReset();
        } else {
            this.actionPlayNext();
        }
    }

    @Override
    public void onBufferingUpdate(final MediaPlayer mp, final int percent) {
        this.mPercent = percent;
    }

    @Override
    public void onSeekComplete(final MediaPlayer mp) {
        this.progressQueryStateRequest(false);
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
        if (CollectionUtils.isNotEmpty(trackBeanList)) {
            this.mTrackList.clear();
            this.mTrackList.addAll(trackBeanList);

            this.mPlayList.clear();
            this.mPlayList.addAll(trackBeanList);
        }
    }

    protected void actionInitPlaylist() {
        this.mPlayList.clear();
        this.mPlayList.addAll(this.mTrackList);
        if (this.getShuffleFlag() == MusicService.FLAG_RANDOM) {
            Collections.shuffle(this.mPlayList);
        }
        this.mPosition = this.mPlayList.indexOf(this.mNowPlaying);
    }

    protected void actionReset() {
        this.relaxResources(false);
        this.mDuration = 0;
        this.mPercent = 0;
        this.sendNotification(this.mNowPlaying);
        this.progressQueryStateRequest(true);

        Uri data = null;
        final String articleId = this.mNowPlaying.getArticleId();
        final String fileId = this.mNowPlaying.getFileId();
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

    private void playMusic(final Uri data) {
        System.out.println(data.toString());
        this.createMediaPlayerIfNeeded();
        final Context context = this.getApplicationContext();
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
            this.mPosition--;
            if (this.mPosition < 0) {
                final int flag = this.getLoopFlag();
                if (flag == MusicService.FLAG_LOOP_ALL) {
                    this.mPosition += this.mPlayList.size();
                } else {
                    this.processStopRequest();
                    return;
                }
            }
            this.mNowPlaying = this.mPlayList.get(this.mPosition);
            this.actionReset();
        } else {
            this.mMediaPlayer.seekTo(0);
        }
    }

    public void actionPlayNext() {
        this.mPosition++;
        if (this.mPosition == this.mPlayList.size()) {
            final int flag = this.getLoopFlag();
            if (flag == MusicService.FLAG_LOOP_ALL) {
                this.mPosition = 0;
            } else {
                this.processStopRequest();
                return;
            }
        }
        this.mNowPlaying = this.mPlayList.get(this.mPosition);
        this.actionReset();
    }

    private void actionSeekTo(final Intent intent) {
        final int progress = intent.getIntExtra("progress", 0);
        this.mMediaPlayer.seekTo(progress);
    }

    private void showToast(final String text, final int duration) {
        if (this.mToast == null) {
            final Context context = this.getApplicationContext();
            this.mToast = Toast.makeText(context, text, duration);
        } else {
            this.mToast.setText(text);
            this.mToast.setDuration(duration);
        }
        this.mToast.show();
    }

    private void sendNotification(final TrackBean trackBean) {
        final String articleId = trackBean.getArticleId();
        final TongrenluApplication application = (TongrenluApplication) this.getApplication();
        final BitmapLruCache bitmapCache = application.getBitmapCache();
        final String url = HttpConstants.getCoverUrl(this,
                                                     articleId,
                                                     HttpConstants.XS_COVER);
        final HttpHelper http = application.getHttpHelper();
        new LoadImageTask() {

            @Override
            protected void onPostExecute(final Drawable result) {
                super.onPostExecute(result);
                if (!this.isCancelled() && result != null) {
                    Bitmap largeIcon = ((CacheableBitmapDrawable) result).getBitmap();

                    final Intent intent = new Intent(application,
                                                     MusicPlayerActivity.class);
                    final PendingIntent contentIntent = PendingIntent.getActivity(application,
                                                                                  0,
                                                                                  intent,
                                                                                  PendingIntent.FLAG_CANCEL_CURRENT);
                    final NotificationCompat.Builder builder = new NotificationCompat.Builder(application);
                    builder.setContentIntent(contentIntent);
                    builder.setSmallIcon(R.drawable.ic_launcher);
                    builder.setOngoing(true);
                    builder.setAutoCancel(false);
                    final String contentTitle = "正在播放:" + trackBean.getSongTitle();
                    builder.setTicker(contentTitle);
                    builder.setContentTitle(contentTitle);
                    builder.setContentText(trackBean.getLeadArtist());
                    builder.setNumber(MusicService.this.mPlayList.size());
                    builder.setLargeIcon(largeIcon);
                    final Intent skipAction = new Intent(application,
                                                         MusicService.class);
                    skipAction.setAction(MusicService.ACTION_SKIP);
                    final PendingIntent skipActionIntent = PendingIntent.getService(application,
                                                                                    0,
                                                                                    skipAction,
                                                                                    PendingIntent.FLAG_CANCEL_CURRENT);
                    builder.addAction(R.drawable.av_next,
                                      application.getString(R.string.player_next),
                                      skipActionIntent);

                    final Intent stopAction = new Intent(application,
                                                         MusicService.class);
                    stopAction.setAction(MusicService.ACTION_STOP);
                    final PendingIntent stopActionIntent = PendingIntent.getService(application,
                                                                                    0,
                                                                                    stopAction,
                                                                                    PendingIntent.FLAG_CANCEL_CURRENT);
                    builder.addAction(R.drawable.av_stop,
                                      application.getString(R.string.player_stop),
                                      stopActionIntent);
                    builder.setWhen(System.currentTimeMillis());
                    MusicService.this.startForeground(MusicService.NOTIFICATION_ID,
                                                      builder.build());
                }
            }

        }.execute(bitmapCache, url, http);
    }

    private void onShufflePlayChanged(final SharedPreferences sharedPreferences) {
        final int flag = this.getShuffleFlag();
        final Resources res = this.getResources();
        final String[] entries = res.getStringArray(R.array.pref_entries_shuffle_play);
        this.showToast(entries[flag], Toast.LENGTH_LONG);
        this.actionInitPlaylist();
        this.progressQueryStateRequest(true);
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

    private void relaxResources(final boolean releaseMediaPlayer) {
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
        if (this.mState == MusicService.STATE_PLAYING) {
            this.configAndStartMediaPlayer();
        }
    }

    @Override
    public void onLostAudioFocus(final boolean canDuck) {
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
