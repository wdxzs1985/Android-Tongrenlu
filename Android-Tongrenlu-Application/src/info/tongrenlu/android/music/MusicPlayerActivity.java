package info.tongrenlu.android.music;

import org.apache.commons.lang3.StringUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;

public class MusicPlayerActivity extends FragmentActivity {

    public static final String LAST_ALBUM = "LAST_ALBUM";
    public static final String LAST_FILE_ID = "LAST_FILE_ID";
    public static final String LAST_TRACK_TITLE = "LAST_TRACK_TITLE";
    public static final String LAST_TRACK_ARTIST = "LAST_TRACK_ARTIST";

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver mMusicStopReceiver = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_player);
        this.initReceiver();
    }

    private void initReceiver() {
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        this.mMusicStopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String action = intent.getAction();
                if (StringUtils.equals(action, MusicService.EVENT_STOP)) {
                    MusicPlayerActivity.this.finish();
                }
            }
        };
        final IntentFilter filter = new IntentFilter(MusicService.EVENT_STOP);
        this.mLocalBroadcastManager.registerReceiver(this.mMusicStopReceiver,
                                                     filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mLocalBroadcastManager.unregisterReceiver(this.mMusicStopReceiver);
    }

}
