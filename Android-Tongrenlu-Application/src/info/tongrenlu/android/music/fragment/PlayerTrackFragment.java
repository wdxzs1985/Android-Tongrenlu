package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.MusicService;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.PlayerTrackAdapter;
import info.tongrenlu.domain.TrackBean;
import info.tongrenlu.support.ApplicationSupport;

import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

public class PlayerTrackFragment extends Fragment implements
        OnItemClickListener, OnClickListener {

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver mMusicUpdateReceiver = null;

    private ListView mListView = null;

    private PlayerTrackAdapter mAdapter = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_player_track,
                                           null,
                                           false);
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mAdapter = new PlayerTrackAdapter();
        this.mListView = (ListView) view.findViewById(android.R.id.list);
        this.mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        this.mListView.setOnItemClickListener(this);
        this.mListView.setVisibility(View.VISIBLE);
        this.mListView.setAdapter(this.mAdapter);

        final ImageButton closeButton = (ImageButton) view.findViewById(R.id.action_close);
        closeButton.setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        this.getFragmentManager().popBackStack();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.initReceiver();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.performUpdateUI();
    }

    private void initReceiver() {
        final Context context = this.getActivity();
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
            if (ApplicationSupport.canUseSmoothScroll()) {
                this.mListView.smoothScrollToPosition(position);
            } else {
                this.mListView.setSelection(position);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mLocalBroadcastManager.unregisterReceiver(this.mMusicUpdateReceiver);
    }

    @Override
    public void onItemClick(final AdapterView<?> listView,
                            final View itemView,
                            final int position,
                            final long itemId) {
        if (position == AdapterView.INVALID_POSITION) {
            return;
        }
        final Context context = this.getActivity().getApplicationContext();
        final Intent playAction = new Intent(context, MusicService.class);
        playAction.setAction(MusicService.ACTION_ADD);
        playAction.putExtra("position", position);
        context.startService(playAction);
    }

    protected void performUpdateUI() {
        final Context context = this.getActivity().getApplicationContext();
        final Intent updateAction = new Intent(context, MusicService.class);
        updateAction.setAction(MusicService.ACTION_QUERY);
        updateAction.putExtra("includePlaylist", true);
        context.startService(updateAction);
    }
}
