package info.tongrenlu.android.music;

import info.tongrenlu.android.music.adapter.PlaylistTrackListAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.View;

import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class TrackActivity extends BaseActivity implements ActionSlideExpandableListView.OnActionClickListener, LoaderCallbacks<Cursor> {

    public static final int TRACK_LOADER_ID = 0;

    private View mProgress = null;
    private View mEmpty = null;
    private ActionSlideExpandableListView mListView = null;
    private PlaylistTrackListAdapter mAdapter = null;

    private ContentObserver contentObserver = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAdapter = new PlaylistTrackListAdapter(this, null);

        this.setContentView(R.layout.activity_playlist_info);
        this.mProgress = this.findViewById(android.R.id.progress);
        this.mEmpty = this.findViewById(android.R.id.empty);
        this.mListView = (ActionSlideExpandableListView) this.findViewById(android.R.id.list);
        //
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setItemActionListener(this,
                                             R.id.item,
                                             R.id.action_play,
                                             R.id.action_delete);

        this.registerForContextMenu(this.mListView);

        this.getSupportLoaderManager().initLoader(TRACK_LOADER_ID, null, this);

        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                TrackActivity.this.getSupportLoaderManager()
                                  .getLoader(TRACK_LOADER_ID)
                                  .onContentChanged();
            }
        };
        this.getContentResolver()
            .registerContentObserver(TongrenluContentProvider.TRACK_URI,
                                     true,
                                     this.contentObserver);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final CursorLoader loader = new CursorLoader(this);
        loader.setUri(TongrenluContentProvider.TRACK_URI);
        return loader;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
        this.mAdapter.swapCursor(c);
        this.mProgress.setVisibility(View.GONE);
        if (this.mAdapter.isEmpty()) {
            this.mListView.setVisibility(View.GONE);
            this.mEmpty.setVisibility(View.VISIBLE);
        } else {
            this.mEmpty.setVisibility(View.GONE);
            this.mListView.setVisibility(View.VISIBLE);
            this.mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        this.mAdapter.swapCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getContentResolver()
            .unregisterContentObserver(this.contentObserver);
        this.getSupportLoaderManager().destroyLoader(TRACK_LOADER_ID);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.fragment_playlist_track, menu);
    }

    @Override
    public void onClick(View itemView, View clickedView, int position) {
        final Cursor c = (Cursor) this.mListView.getItemAtPosition(position);
        switch (clickedView.getId()) {
        case R.id.item:
        case R.id.action_play:
            this.playTrack(c);
            break;
        case R.id.action_delete:
            this.deleteTrack(c);
            break;
        default:
            break;
        }
    }

    protected void playTrack(final Cursor c) {
        int position = c.getPosition();
        if (c.moveToFirst()) {
            final ArrayList<TrackBean> trackBeanList = new ArrayList<TrackBean>();
            while (!c.isAfterLast()) {
                final TrackBean trackBean = new TrackBean();
                trackBean.setArticleId(c.getString(c.getColumnIndex("article_id")));
                trackBean.setFileId(c.getString(c.getColumnIndex("file_id")));
                trackBean.setTitle(c.getString(c.getColumnIndex("title")));
                trackBean.setArtist(c.getString(c.getColumnIndex("artist")));
                trackBeanList.add(trackBean);
                c.moveToNext();
            }

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
    }

    private void deleteTrack(Cursor c) {
        long id = c.getLong(c.getColumnIndex("_id"));
        Uri uri = ContentUris.withAppendedId(TongrenluContentProvider.TRACK_URI,
                                             id);
        ContentResolver contentResolver = this.getContentResolver();
        contentResolver.delete(uri, null, null);
        contentResolver.notifyChange(TongrenluContentProvider.TRACK_URI, null);
    }
}
