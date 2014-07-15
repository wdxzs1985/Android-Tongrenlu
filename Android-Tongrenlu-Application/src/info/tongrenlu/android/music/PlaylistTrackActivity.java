package info.tongrenlu.android.music;

import info.tongrenlu.android.music.fragment.PlaylistAddTrackFragment;
import info.tongrenlu.android.music.fragment.PlaylistAddTrackFragment.PlaylistAddTrackFragmentListener;
import info.tongrenlu.android.music.fragment.PlaylistTrackFragment;
import info.tongrenlu.android.music.fragment.PlaylistTrackFragment.PlaylistTrackFragmentListener;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

public class PlaylistTrackActivity extends ActionBarActivity implements PlaylistTrackFragmentListener, PlaylistAddTrackFragmentListener {

    public static final int PLAYLIST_LOADER_ID = 0;
    public static final long BAD_ID = -1;

    public static final String PLAYLIST_ID = "playlistId";

    private long mPlaylistId = BAD_ID;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_fragment_container);

        this.mPlaylistId = this.getIntent().getLongExtra("playlistId", BAD_ID);
        if (this.mPlaylistId == BAD_ID) {
            this.finish();
            return;
        }

        final Bundle args = new Bundle();
        args.putLong(PLAYLIST_ID, this.mPlaylistId);

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
    public void onPlay(final ArrayList<TrackBean> trackBeanList,
                       final int position) {
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
    public void onSwapTrack(final Uri trackUri,
                            final ArrayList<ContentValues> values) {
        final ContentResolver contentResolver = this.getContentResolver();
        for (final ContentValues contentValues : values) {
            final Long id = contentValues.getAsLong("_id");
            final Uri uri = ContentUris.withAppendedId(TongrenluContentProvider.PLAYLIST_TRACK_URI,
                                                       id);
            contentResolver.update(uri, contentValues, null, null);
            System.out.println("update");
        }
        contentResolver.notifyChange(trackUri, null);
    }

    @Override
    public void onDeleteTrack(final Uri trackUri, final long id) {
        final Uri uri = ContentUris.withAppendedId(TongrenluContentProvider.PLAYLIST_TRACK_URI,
                                                   id);
        final ContentResolver contentResolver = this.getContentResolver();
        contentResolver.delete(uri, null, null);
        // update track number
        contentResolver.update(trackUri, null, null, null);
        contentResolver.notifyChange(trackUri, null);
    }

    @Override
    public void onDeletePlaylist(final Uri uri) {
        final Uri trackUri = Uri.withAppendedPath(uri, "track");

        final ContentResolver contentResolver = this.getContentResolver();
        contentResolver.delete(uri, null, null);
        contentResolver.delete(trackUri, null, null);
        contentResolver.notifyChange(TongrenluContentProvider.PLAYLIST_URI,
                                     null);
        this.finish();
    }

    @Override
    public void onStartAddTrack() {
        final Fragment fragment = new PlaylistAddTrackFragment();
        this.getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void onAddTrack(final TrackBean trackBean) {
        final ContentResolver contentResolver = this.getContentResolver();
        final ContentValues values = new ContentValues();
        values.put("articleId", trackBean.getArticleId());
        values.put("fileId", trackBean.getFileId());
        values.put("album", trackBean.getAlbum());
        values.put("name", trackBean.getName());
        values.put("artist", trackBean.getArtist());

        final Uri contentUri = Uri.withAppendedPath(TongrenluContentProvider.PLAYLIST_URI,
                                                    this.mPlaylistId + "/track");

        if (trackBean.getTrackNumber() == 0) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(contentUri,
                                               null,
                                               null,
                                               null,
                                               null);
                values.put("trackNumber", cursor.getCount() + 1);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            values.put("trackNumber", trackBean.getTrackNumber());
        }

        contentResolver.insert(contentUri, values);
        contentResolver.notifyChange(contentUri, null);

        final String msg = this.getString(R.string.message_add_track_to_playlist,
                                          trackBean.getName());
        Toast.makeText(this.getApplicationContext(), msg, Toast.LENGTH_SHORT)
             .show();

    }

    @Override
    public void onAddTrackFinish() {
        this.getSupportFragmentManager().popBackStack();
    }

}
