package info.tongrenlu.android.music;

import info.tongrenlu.android.music.MusicService.LocalBinder;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
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

public class MusicPlayerActivity extends BaseActivity implements
        OnClickListener, OnSeekBarChangeListener {

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver mMusicReceiver = null;

    private ImageButton mPlayButton = null;
    private ImageButton mLoopButton = null;
    private ImageButton mRandomButton = null;
    private SeekBar mProgress = null;
    private MusicService mService = null;
    private ServiceConnection mConnection = null;
    private boolean mBound = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_player);
        this.initController();
        this.initReceiver();
        this.initServiceBinder();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        final Intent intent = new Intent(this, MusicService.class);
        this.bindService(intent, this.mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (this.mBound) {
            this.unbindService(this.mConnection);
            this.mBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                if (StringUtils.equals(action, MusicService.EVENT_START)) {
                    MusicPlayerActivity.this.onMusicPlayerStart();
                } else if (StringUtils.equals(action, MusicService.EVENT_UPDATE)) {
                    MusicPlayerActivity.this.onMusicPlayerUpdate(intent);
                } else if (StringUtils.equals(action, MusicService.EVENT_STOP)) {
                    MusicPlayerActivity.this.finish();
                } else if (StringUtils.equals(action,
                                              MusicService.EVENT_BUFFERING_START)) {
                    MusicPlayerActivity.this.onMusicPlayerBuffering(true);
                } else if (StringUtils.equals(action,
                                              MusicService.EVENT_BUFFERING_END)) {
                    MusicPlayerActivity.this.onMusicPlayerBuffering(false);
                }
            }
        };
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.EVENT_START);
        filter.addAction(MusicService.EVENT_UPDATE);
        filter.addAction(MusicService.EVENT_STOP);
        this.mLocalBroadcastManager.registerReceiver(this.mMusicReceiver,
                                                     filter);
    }

    private void initServiceBinder() {

        this.mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(final ComponentName className,
                                           final IBinder service) {
                // We've bound to LocalService, cast the IBinder and get
                // LocalService instance
                final LocalBinder binder = (LocalBinder) service;
                MusicPlayerActivity.this.mService = binder.getService();
                MusicPlayerActivity.this.mBound = true;
                //
                MusicPlayerActivity.this.onMusicPlayerStart();
                MusicPlayerActivity.this.initLoopButtonImage();
                MusicPlayerActivity.this.initRandomButtonImage();
            }

            @Override
            public void onServiceDisconnected(final ComponentName className) {
                MusicPlayerActivity.this.mBound = false;
                MusicPlayerActivity.this.mService = null;
            }

        };
    }

    @Override
    public void onClick(final View v) {
        if (this.mBound) {
            switch (v.getId()) {
            case R.id.player_play:
                this.mService.actionPlay();
                this.initPlayButtonImage();
                break;
            case R.id.player_prev:
                this.mService.actionPlayPrev();
                this.initPlayButtonImage();
                break;
            case R.id.player_next:
                this.mService.actionPlayNext();
                this.initPlayButtonImage();
                break;
            case R.id.player_loop:
                this.mService.actionLoop();
                this.initLoopButtonImage();
                break;
            case R.id.player_shuffle:
                this.mService.actionRandom();
                this.initRandomButtonImage();
                break;
            default:
                break;
            }
        }
    }

    private void initPlayButtonImage() {
        switch (this.mService.getPlayflag()) {
        case MusicService.FLAG_PLAY:
            this.mPlayButton.setImageResource(R.drawable.player_btn_player_pause);
            break;
        case MusicService.FLAG_PAUSE:
            this.mPlayButton.setImageResource(R.drawable.player_btn_player_play);
            break;
        }
    }

    private void initLoopButtonImage() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_LOOP_PLAY,
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

    private void initRandomButtonImage() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String value = sharedPreferences.getString(SettingsActivity.PREF_KEY_SHUFFLE_PLAY,
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

    @Override
    public void onProgressChanged(final SeekBar seekBar,
                                  final int progress,
                                  final boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        this.mService.getMediaPlayer().seekTo(seekBar.getProgress());
    }

    protected void onMusicPlayerStart() {
        final TrackBean trackBean = this.mService.getNowDisplay();
        final String articleId = trackBean.getArticleId();
        final ImageView coverView = (ImageView) this.findViewById(R.id.article_cover);
        HttpConstants.displayLargeCover(coverView, articleId);

        final String title = trackBean.getTitle();
        final TextView titleView = (TextView) this.findViewById(R.id.track_title);
        titleView.setText(title);

        final String artist = trackBean.getArtist();
        final TextView artistView = (TextView) this.findViewById(R.id.track_artist);
        artistView.setText(artist);

        this.mProgress.setMax(0);// 设置进度条
        this.mProgress.setProgress(0);// 设置进度条
        this.mProgress.setSecondaryProgress(0);
    }

    protected void onMusicPlayerUpdate(final Intent intent) {
        if (this.mBound) {
            this.initPlayButtonImage();
        }

        final int duration = intent.getIntExtra("duration", 0);
        final TextView durationView = (TextView) this.findViewById(R.id.player_duration);
        durationView.setText(MusicService.toTime(duration));
        this.mProgress.setMax(duration);// 设置进度条

        final int progress = intent.getIntExtra("progress", 0);
        final TextView currentPositionView = (TextView) this.findViewById(R.id.player_current_time);
        currentPositionView.setText(MusicService.toTime(progress));
        this.mProgress.setProgress(progress);// 设置进度条

        final int percent = intent.getIntExtra("percent", 0);
        this.mProgress.setSecondaryProgress(percent * duration / 100);
    }

    protected void onMusicPlayerBuffering(final boolean start) {
        if (start) {
            // this.mBuffering.setVisibility(View.VISIBLE);
        } else {
            // this.mBuffering.setVisibility(View.GONE);
        }
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

}
