package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.MusicService;
import info.tongrenlu.android.music.adapter.PlayerTrackAdapter;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class PlayerTrackFragment extends ListFragment implements OnItemClickListener {

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver mMusicUpdateReceiver = null;

    private PlayerTrackAdapter mAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mAdapter = new PlayerTrackAdapter();
        this.setListAdapter(this.mAdapter);
        this.getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.initReceiver();
    }

    private void initReceiver() {
        Context context = this.getActivity();
        this.mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
        this.mMusicUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final String action = intent.getAction();
                if (StringUtils.equals(action, MusicService.EVENT_UPDATE)) {
                    PlayerTrackFragment.this.onMusicPlayerUpdate(intent);
                }
            }
        };
        final IntentFilter filter = new IntentFilter(MusicService.EVENT_UPDATE);
        this.mLocalBroadcastManager.registerReceiver(this.mMusicUpdateReceiver,
                                                     filter);
    }

    protected void onMusicPlayerUpdate(final Intent intent) {
        final ArrayList<TrackBean> playlist = intent.getParcelableArrayListExtra("playlist");
        final int position = intent.getIntExtra("position", 0);
        if (CollectionUtils.isNotEmpty(playlist)) {
            this.mAdapter.setPlaylist(playlist);
            this.mAdapter.setActivePosition(position);
            this.mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
    }

}
