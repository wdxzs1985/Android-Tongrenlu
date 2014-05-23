package info.tongrenlu.android.music;

import info.tongrenlu.android.music.fragment.PlaylistTrackFragment;
import info.tongrenlu.android.music.fragment.PlaylistTrackFragment.PlaylistTrackFragmentListener;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;

public class PlaylistTrackActivity extends ActionBarActivity implements PlaylistTrackFragmentListener {

    public static final int PLAYLIST_LOADER_ID = 0;
    public static final long BAD_ID = -1;

    public static final String PLAYLIST_ID = "playlistId";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_fragment_container);

        final long playlistId = this.getIntent().getLongExtra("playlistId",
                                                              BAD_ID);
        if (playlistId == BAD_ID) {
            this.finish();
            return;
        }

        final Bundle args = new Bundle();
        args.putLong(PLAYLIST_ID, playlistId);

        final Fragment fragment = new PlaylistTrackFragment();
        fragment.setArguments(args);

        this.getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPlay(ArrayList<TrackBean> trackBeanList, int position) {
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_ADD);
        serviceIntent.putParcelableArrayListExtra("trackBeanList",
                                                  trackBeanList);
        serviceIntent.putExtra("position", position);
        this.startService(serviceIntent);

        final Intent activityIntent = new Intent(this,
                                                 MusicPlayerActivity.class);
        this.startActivity(activityIntent);
    }

    @Override
    public void onDeleteTrack(Uri trackUri, long id) {
        final Uri uri = ContentUris.withAppendedId(TongrenluContentProvider.PLAYLIST_TRACK_URI,
                                                   id);
        final ContentResolver contentResolver = this.getContentResolver();
        contentResolver.delete(uri, null, null);
        contentResolver.update(trackUri, null, null, null);
        contentResolver.notifyChange(trackUri, null);
    }

    @Override
    public void onDeletePlaylist(Uri uri) {
        Uri trackUri = Uri.withAppendedPath(uri, "track");

        final ContentResolver contentResolver = this.getContentResolver();
        contentResolver.delete(uri, null, null);
        contentResolver.delete(trackUri, null, null);
        contentResolver.notifyChange(TongrenluContentProvider.PLAYLIST_URI,
                                     null);
        this.finish();
    }
}
