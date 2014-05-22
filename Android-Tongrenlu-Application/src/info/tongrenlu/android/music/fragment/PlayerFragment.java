package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.image.LoadBlurImageTask;
import info.tongrenlu.android.image.LoadImageTask;
import info.tongrenlu.android.music.MusicService;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.SettingsActivity;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;
import info.tongrenlu.support.ApplicationSupport;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class PlayerFragment extends Fragment implements OnClickListener, OnSeekBarChangeListener {

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver mMusicUpdateReceiver = null;

    private TrackBean mTrackBean = null;
    private int mState = MusicService.STATE_STOPPED;

    private TextView mTitleView = null;
    private TextView mArtistView = null;
    private ImageView mCoverView = null;

    private ImageButton mPlayButton = null;
    private ImageButton mLoopButton = null;
    private ImageButton mRandomButton = null;

    private TextView mCurrentPositionView = null;
    private TextView mDurationView = null;
    private SeekBar mProgress = null;

    private boolean mLockSeekbar = false;

    private SharedPreferences mSharedPreferences = null;

    public final static int UPDATE_UI = 0;
    protected final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case UPDATE_UI:
                PlayerFragment.this.performUpdateUI();
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.initController(view);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        this.initLoopButtonImage();
        this.initShuffleButtonImage();
        this.initReceiver();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mHandler.sendEmptyMessage(UPDATE_UI);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mHandler.removeMessages(UPDATE_UI);
        this.mLocalBroadcastManager.unregisterReceiver(this.mMusicUpdateReceiver);
    }

    private void initController(View view) {
        this.mTitleView = (TextView) view.findViewById(R.id.track_title);
        this.mArtistView = (TextView) view.findViewById(R.id.track_artist);
        this.mCoverView = (ImageView) view.findViewById(R.id.article_cover);

        this.mPlayButton = (ImageButton) view.findViewById(R.id.player_play);
        this.mPlayButton.setOnClickListener(this);
        final ImageButton prevButton = (ImageButton) view.findViewById(R.id.player_prev);
        prevButton.setOnClickListener(this);
        final ImageButton nextButton = (ImageButton) view.findViewById(R.id.player_next);
        nextButton.setOnClickListener(this);
        this.mLoopButton = (ImageButton) view.findViewById(R.id.player_loop);
        this.mLoopButton.setOnClickListener(this);
        this.mRandomButton = (ImageButton) view.findViewById(R.id.player_shuffle);
        this.mRandomButton.setOnClickListener(this);

        this.mProgress = (SeekBar) view.findViewById(R.id.player_progress);
        this.mProgress.setOnSeekBarChangeListener(this);
        this.mCurrentPositionView = (TextView) view.findViewById(R.id.player_current_time);
        this.mDurationView = (TextView) view.findViewById(R.id.player_duration);
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

    private void initReceiver() {
        Context context = this.getActivity().getApplicationContext();
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
        this.mMusicUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String action = intent.getAction();
                if (StringUtils.equals(action, MusicService.EVENT_UPDATE)) {
                    PlayerFragment.this.onMusicPlayerUpdate(intent);
                }
            }
        };
        final IntentFilter filter = new IntentFilter(MusicService.EVENT_UPDATE);
        this.mLocalBroadcastManager.registerReceiver(this.mMusicUpdateReceiver,
                                                     filter);
    }

    @Override
    public void onClick(final View v) {
        Context context = this.getActivity().getApplicationContext();
        switch (v.getId()) {
        case R.id.player_play:
            final Intent tooglePlaybackAction = new Intent(context,
                                                           MusicService.class);
            tooglePlaybackAction.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
            context.startService(tooglePlaybackAction);
            break;
        case R.id.player_prev:
            final Intent rewindAction = new Intent(context, MusicService.class);
            rewindAction.setAction(MusicService.ACTION_REWIND);
            context.startService(rewindAction);
            break;
        case R.id.player_next:
            final Intent skipAction = new Intent(context, MusicService.class);
            skipAction.setAction(MusicService.ACTION_SKIP);
            context.startService(skipAction);
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
        Context context = this.getActivity().getApplicationContext();
        final Intent seekAction = new Intent(context, MusicService.class);
        seekAction.setAction(MusicService.ACTION_SEEK);
        seekAction.putExtra("progress", seekBar.getProgress());
        context.startService(seekAction);
    }

    private void updateTitle() {
        final String title = this.mTrackBean.getSongTitle();
        this.mTitleView.setText(title);
    }

    private void updateArtist() {
        final String artist = this.mTrackBean.getLeadArtist();
        this.mArtistView.setText(artist);
    }

    private void updateCover() {
        final String articleId = this.mTrackBean.getArticleId();
        final TongrenluApplication application = (TongrenluApplication) this.getActivity()
                                                                            .getApplication();
        final BitmapLruCache bitmapCache = application.getBitmapCache();
        final String url = HttpConstants.getCoverUrl(application,
                                                     articleId,
                                                     HttpConstants.L_COVER);
        final HttpHelper http = application.getHttpHelper();

        new LoadImageTask() {

            @Override
            protected void onPostExecute(final Drawable result) {
                super.onPostExecute(result);
                if (!this.isCancelled() && result != null) {
                    final Drawable emptyDrawable = new ShapeDrawable();
                    final TransitionDrawable fadeInDrawable = new TransitionDrawable(new Drawable[] { emptyDrawable,
                            result });
                    PlayerFragment.this.mCoverView.setImageDrawable(result);
                    fadeInDrawable.startTransition(LoadImageTask.TIME_SHORT);
                }
            }

        }.execute(bitmapCache, url, http);

        if (this.mSharedPreferences.getBoolean(SettingsActivity.PREF_KEY_BACKGROUND_RENDER,
                                               ApplicationSupport.canUseRenderScript())) {
            String backgroundUrl = null;
            if (ApplicationSupport.canUseLargeHeap()) {
                backgroundUrl = HttpConstants.getCoverUrl(application,
                                                          articleId,
                                                          HttpConstants.L_COVER);
            } else {
                backgroundUrl = HttpConstants.getCoverUrl(application,
                                                          articleId,
                                                          HttpConstants.M_COVER);
            }
            new LoadBlurImageTask() {

                @Override
                protected void onPostExecute(final Drawable result) {
                    super.onPostExecute(result);
                    if (!this.isCancelled() && result != null) {
                        PlayerFragment.this.getActivity()
                                           .getWindow()
                                           .setBackgroundDrawable(result);
                    }
                }

            }.execute(bitmapCache, backgroundUrl, http, application);
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
        this.mDurationView.setText(MusicService.toTime(duration));
        this.mProgress.setMax(duration);// 设置进度条
    }

    private void updateCurrentTime(final int progress) {
        this.mCurrentPositionView.setText(MusicService.toTime(progress));
    }

    private void updateProgress(final int progress) {
        this.mProgress.setProgress(progress);
    }

    private void updateSecondaryProgress(final int progress) {
        this.mProgress.setSecondaryProgress(progress);
    }

    protected void onMusicPlayerUpdate(final Intent intent) {
        this.mState = intent.getIntExtra("state", MusicService.STATE_STOPPED);
        this.updatePlayButton(this.mState);
        if (this.mState != MusicService.STATE_STOPPED) {
            final TrackBean trackBean = intent.getParcelableExtra("trackBean");
            if (trackBean != null && !trackBean.equals(this.mTrackBean)) {
                this.mTrackBean = trackBean;
                this.updateCover();
                this.updateTitle();
                this.updateArtist();
            }

            final int duration = intent.getIntExtra("duration", 0);
            this.updateDuration(duration);

            if (!this.mLockSeekbar) {
                final int progress = intent.getIntExtra("progress", 0);
                this.updateCurrentTime(progress);
                this.updateProgress(progress);

                final int delay = 1000 - progress % 1000;
                this.mHandler.sendEmptyMessageDelayed(UPDATE_UI, delay);
            }

            if (intent.hasExtra("percent")) {
                final int percent = intent.getIntExtra("percent", 0);
                this.updateSecondaryProgress(percent * duration / 100);
            }
        }
    }

    protected void performUpdateUI() {
        Context context = this.getActivity().getApplicationContext();
        final Intent updateUIAction = new Intent(context, MusicService.class);
        updateUIAction.setAction(MusicService.ACTION_STATE);
        context.startService(updateUIAction);
    }
}
