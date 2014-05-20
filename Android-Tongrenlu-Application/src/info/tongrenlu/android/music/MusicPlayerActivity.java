package info.tongrenlu.android.music;

import info.tongrenlu.android.image.LoadBlurImageTask;
import info.tongrenlu.android.image.LoadImageTask;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MusicPlayerActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {

    public static final String LAST_ALBUM = "LAST_ALBUM";
    public static final String LAST_FILE_ID = "LAST_FILE_ID";
    public static final String LAST_TRACK_TITLE = "LAST_TRACK_TITLE";
    public static final String LAST_TRACK_ARTIST = "LAST_TRACK_ARTIST";

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver mMusicReceiver = null;

    private ImageButton mPlayButton = null;
    private ImageButton mLoopButton = null;
    private ImageButton mRandomButton = null;
    private SeekBar mProgress = null;

    private TrackBean mTrackBean = null;
    private int mState = MusicService.STATE_STOPPED;

    private boolean mLockSeekbar = false;

    private SharedPreferences mSharedPreferences = null;

    public final static int UPDATE_UI = 0;
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case UPDATE_UI:
                MusicPlayerActivity.this.performUpdateUI();
                break;
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_player);
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        this.initController();
        this.initLoopButtonImage();
        this.initShuffleButtonImage();
        this.initReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.mHandler.sendEmptyMessage(MusicPlayerActivity.UPDATE_UI);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.mHandler.removeMessages(MusicPlayerActivity.UPDATE_UI);
        this.mLocalBroadcastManager.unregisterReceiver(this.mMusicReceiver);
    }

    private void initController() {
        this.mPlayButton = (ImageButton) this.findViewById(R.id.player_play);
        this.mPlayButton.setOnClickListener(this);
        final ImageButton prevButton = (ImageButton) this.findViewById(R.id.player_prev);
        prevButton.setOnClickListener(this);
        final ImageButton nextButton = (ImageButton) this.findViewById(R.id.player_next);
        nextButton.setOnClickListener(this);
        this.mLoopButton = (ImageButton) this.findViewById(R.id.player_loop);
        this.mLoopButton.setOnClickListener(this);
        this.mRandomButton = (ImageButton) this.findViewById(R.id.player_shuffle);
        this.mRandomButton.setOnClickListener(this);
        this.mProgress = (SeekBar) this.findViewById(R.id.player_progress);
        this.mProgress.setOnSeekBarChangeListener(this);
    }

    private void initReceiver() {
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        this.mMusicReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String action = intent.getAction();
                if (StringUtils.equals(action, MusicService.EVENT_UPDATE)) {
                    MusicPlayerActivity.this.onMusicPlayerUpdate(intent);
                } else if (StringUtils.equals(action, MusicService.EVENT_STOP)) {
                    MusicPlayerActivity.this.finish();
                }
            }
        };
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.EVENT_UPDATE);
        filter.addAction(MusicService.EVENT_STOP);
        this.mLocalBroadcastManager.registerReceiver(this.mMusicReceiver,
                                                     filter);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
        case R.id.player_play:
            final Intent tooglePlaybackAction = new Intent(this,
                                                           MusicService.class);
            tooglePlaybackAction.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
            this.startService(tooglePlaybackAction);
            break;
        case R.id.player_prev:
            final Intent rewindAction = new Intent(this, MusicService.class);
            rewindAction.setAction(MusicService.ACTION_REWIND);
            this.startService(rewindAction);
            break;
        case R.id.player_next:
            final Intent skipAction = new Intent(this, MusicService.class);
            skipAction.setAction(MusicService.ACTION_SKIP);
            this.startService(skipAction);
            break;
        case R.id.player_loop:
            this.actionLoop();
            this.initLoopButtonImage();
            break;
        case R.id.player_shuffle:
            this.actionShuffle();
            this.initShuffleButtonImage();
            break;
        default:
            break;
        }
    }

    private void initLoopButtonImage() {
        final String value = this.mSharedPreferences.getString(SettingsActivity.PREF_KEY_LOOP_PLAY,
                                                               SettingsActivity.PREF_DEFAULT_LOOP_PLAY);
        final Resources res = this.getResources();
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_loop_play);
        final int index = ArrayUtils.indexOf(entryValues, value);
        switch (index) {
        case MusicService.FLAG_NO_LOOP:
            this.mLoopButton.setImageResource(R.drawable.player_btn_player_mode_repeat);
            break;
        case MusicService.FLAG_LOOP_ALL:
            this.mLoopButton.setImageResource(R.drawable.player_btn_player_mode_repeat_active);
            break;
        case MusicService.FLAG_LOOP_ONE:
            this.mLoopButton.setImageResource(R.drawable.player_btn_player_mode_repeat_one);
            break;
        }
    }

    public void actionLoop() {
        final String value = this.mSharedPreferences.getString(SettingsActivity.PREF_KEY_LOOP_PLAY,
                                                               SettingsActivity.PREF_DEFAULT_LOOP_PLAY);
        final Resources res = this.getResources();
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_loop_play);
        final int index = ArrayUtils.indexOf(entryValues, value);
        final int nextIndex = (index + 1) % entryValues.length;
        this.mSharedPreferences.edit()
                               .putString(SettingsActivity.PREF_KEY_LOOP_PLAY,
                                          entryValues[nextIndex])
                               .commit();
    }

    private void initShuffleButtonImage() {
        final String value = this.mSharedPreferences.getString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
                                                               SettingsActivity.PREF_DEFAULT_SHUFFLE_PLAY);
        final Resources res = this.getResources();
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_shuffle_play);
        final int index = ArrayUtils.indexOf(entryValues, value);
        switch (index) {
        case MusicService.FLAG_SEQUENCE:
            this.mRandomButton.setImageResource(R.drawable.player_btn_player_mode_sequence);
            break;
        case MusicService.FLAG_RANDOM:
            this.mRandomButton.setImageResource(R.drawable.player_btn_player_mode_shuffle);
            break;
        }
    }

    public void actionShuffle() {
        final String value = this.mSharedPreferences.getString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
                                                               SettingsActivity.PREF_DEFAULT_SHUFFLE_PLAY);
        final Resources res = this.getResources();
        final String[] entryValues = res.getStringArray(R.array.pref_entry_values_shuffle_play);
        final int index = ArrayUtils.indexOf(entryValues, value);
        final int nextIndex = (index + 1) % entryValues.length;
        this.mSharedPreferences.edit()
                               .putString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
                                          entryValues[nextIndex])
                               .commit();
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (fromUser) {
            this.updateCurrentTime(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
        this.mLockSeekbar = true;
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        this.mLockSeekbar = false;
        final Intent seekAction = new Intent(this, MusicService.class);
        seekAction.setAction(MusicService.ACTION_SEEK);
        seekAction.putExtra("progress", seekBar.getProgress());
        this.startService(seekAction);
    }

    protected void onMusicPlayerUpdate(final Intent intent) {
        this.mState = intent.getIntExtra("state", MusicService.STATE_STOPPED);
        if (this.mState == MusicService.STATE_STOPPED) {
            this.finish();
        } else {
            this.updatePlayButton(this.mState);

            final TrackBean trackBean = intent.getParcelableExtra("trackBean");
            if (trackBean != null && !trackBean.equals(this.mTrackBean)) {
                this.mTrackBean = trackBean;
                this.updateCover();
                this.updateTitle();
                this.updateArtist();

                final Editor editor = this.mSharedPreferences.edit();
                editor.putString(MusicPlayerActivity.LAST_ALBUM,
                                 this.mTrackBean.getAlbum())
                      .putString(MusicPlayerActivity.LAST_FILE_ID,
                                 this.mTrackBean.getFileId())
                      .putString(MusicPlayerActivity.LAST_TRACK_ARTIST,
                                 this.mTrackBean.getLeadArtist())
                      .putString(MusicPlayerActivity.LAST_TRACK_TITLE,
                                 this.mTrackBean.getSongTitle())
                      .commit();
            }

            final int duration = intent.getIntExtra("duration", 0);
            this.updateDuration(duration);

            if (!this.mLockSeekbar) {
                final int progress = intent.getIntExtra("progress", 0);
                this.updateCurrentTime(progress);
                this.updateProgress(progress);

                final int delay = 1000 - progress % 1000;
                this.mHandler.sendEmptyMessageDelayed(MusicPlayerActivity.UPDATE_UI,
                                                      delay);
            }

            if (intent.hasExtra("percent")) {
                final int percent = intent.getIntExtra("percent", 0);
                this.updateSecondaryProgress(percent * duration / 100);
            }
        }
    }

    private void updateTitle() {
        final String title = this.mTrackBean.getSongTitle();
        final TextView titleView = (TextView) this.findViewById(R.id.track_title);
        titleView.setText(title);
    }

    private void updateArtist() {
        final String artist = this.mTrackBean.getLeadArtist();
        final TextView artistView = (TextView) this.findViewById(R.id.track_artist);
        artistView.setText(artist);
    }

    private void updateCover() {
        final String articleId = this.mTrackBean.getArticleId();
        final TongrenluApplication application = (TongrenluApplication) this.getApplication();
        final BitmapLruCache bitmapCache = application.getBitmapCache();
        final String url = HttpConstants.getCoverUrl(application,
                                                     articleId,
                                                     HttpConstants.L_COVER);
        final HttpHelper http = application.getHttpHelper();

        final ImageView coverView = (ImageView) this.findViewById(R.id.article_cover);
        new LoadImageTask() {

            @Override
            protected void onPostExecute(final Drawable result) {
                super.onPostExecute(result);
                if (!this.isCancelled() && result != null) {
                    final Drawable emptyDrawable = new ShapeDrawable();
                    final TransitionDrawable fadeInDrawable = new TransitionDrawable(new Drawable[] { emptyDrawable,
                            result });
                    coverView.setImageDrawable(result);
                    fadeInDrawable.startTransition(LoadImageTask.TIME_SHORT);
                }
            }

        }.execute(bitmapCache, url, http);

        if (this.mSharedPreferences.getBoolean(SettingsActivity.PREF_KEY_BACKGROUND_RENDER,
                                               Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            new LoadBlurImageTask() {

                @Override
                protected void onPostExecute(final Drawable result) {
                    super.onPostExecute(result);
                    if (!this.isCancelled() && result != null) {
                        MusicPlayerActivity.this.getWindow()
                                                .setBackgroundDrawable(result);
                    }
                }

            }.execute(bitmapCache, url, http, application);
        }
    }

    private void updatePlayButton(final int state) {
        switch (state) {
        case MusicService.STATE_PLAYING:
            this.mPlayButton.setImageResource(R.drawable.player_btn_player_pause);
            break;
        default:
            this.mPlayButton.setImageResource(R.drawable.player_btn_player_play);
            break;
        }
    }

    private void updateDuration(final int duration) {
        final TextView durationView = (TextView) this.findViewById(R.id.player_duration);
        durationView.setText(MusicService.toTime(duration));
        this.mProgress.setMax(duration);// 设置进度条
    }

    private void updateCurrentTime(final int progress) {
        final TextView currentPositionView = (TextView) this.findViewById(R.id.player_current_time);
        currentPositionView.setText(MusicService.toTime(progress));
    }

    private void updateProgress(final int progress) {
        this.mProgress.setProgress(progress);
    }

    private void updateSecondaryProgress(final int progress) {
        this.mProgress.setSecondaryProgress(progress);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.activity_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_player_stop:
            final Intent stopAction = new Intent(this, MusicService.class);
            stopAction.setAction(MusicService.ACTION_STOP);
            this.startService(stopAction);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    protected void performUpdateUI() {
        final Intent updateUIAction = new Intent(this, MusicService.class);
        updateUIAction.setAction(MusicService.ACTION_STATE);
        this.startService(updateUIAction);
    }

}
