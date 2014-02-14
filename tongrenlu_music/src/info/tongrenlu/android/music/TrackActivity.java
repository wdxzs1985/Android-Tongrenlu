package info.tongrenlu.android.music;

import info.tongrenlu.android.music.adapter.PlaylistTrackListAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;

import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class TrackActivity extends FragmentActivity implements ActionSlideExpandableListView.OnActionClickListener, LoaderCallbacks<Cursor> {

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
        loader.setSelection("downloadFlg = ?");
        loader.setSelectionArgs(new String[] { "1" });
        loader.setSortOrder("_id asc");
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
    public void onClick(View itemView, View clickedView, int position) {

        switch (clickedView.getId()) {
        case R.id.item:
        case R.id.action_play:
            this.playTrack(position);
            break;
        case R.id.action_delete:
            this.deleteTrack(position);
            break;
        default:
            break;
        }
    }

    protected void playTrack(int position) {
        final Cursor c = (Cursor) this.mListView.getItemAtPosition(position);
        if (c.moveToFirst()) {
            final ArrayList<TrackBean> trackBeanList = new ArrayList<TrackBean>();
            while (!c.isAfterLast()) {
                final TrackBean trackBean = new TrackBean();
                trackBean.setArticleId(c.getString(c.getColumnIndex("articleId")));
                trackBean.setFileId(c.getString(c.getColumnIndex("fileId")));
                trackBean.setSongTitle(c.getString(c.getColumnIndex("songTitle")));
                trackBean.setLeadArtist(c.getString(c.getColumnIndex("leadArtist")));
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

    private void deleteTrack(int position) {
        final Cursor c = (Cursor) this.mListView.getItemAtPosition(position);
        this.deleteFromTrack(c);
        this.deleteFromPlaylistTrack(c);
        this.deleteMp3File(c);
    }

    private void deleteMp3File(Cursor c) {
        String articleId = c.getString(c.getColumnIndex("articleId"));
        String fileId = c.getString(c.getColumnIndex("fileId"));

        File file = HttpConstants.getMp3(this, articleId, fileId);
        FileUtils.deleteQuietly(file);
    }

    private void deleteFromTrack(Cursor c) {
        String articleId = c.getString(c.getColumnIndex("articleId"));
        String fileId = c.getString(c.getColumnIndex("fileId"));
        ContentResolver contentResolver = this.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("downloadFlg", 0);
        contentResolver.update(TongrenluContentProvider.TRACK_URI,
                               values,
                               "articleId = ? and fileId = ? and downloadFlg = 1",
                               new String[] { articleId, fileId });
        contentResolver.notifyChange(TongrenluContentProvider.TRACK_URI, null);
    }

    private void deleteFromPlaylistTrack(Cursor c) {
        String articleId = c.getString(c.getColumnIndex("articleId"));
        String fileId = c.getString(c.getColumnIndex("fileId"));
        ContentResolver contentResolver = this.getContentResolver();
        contentResolver.delete(TongrenluContentProvider.PLAYLIST_TRACK_URI,
                               "articleId = ? and fileId = ?",
                               new String[] { articleId, fileId });
    }
}
