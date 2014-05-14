package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.music.MainActivity;
import info.tongrenlu.android.music.MusicPlayerActivity;
import info.tongrenlu.android.music.MusicService;
import info.tongrenlu.android.music.R;
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
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class TrackFragment extends TitleFragment implements ActionSlideExpandableListView.OnActionClickListener, LoaderCallbacks<Cursor> {

    private View mProgressContainer = null;
    private View mEmpty = null;
    private ActionSlideExpandableListView mListView = null;
    private CursorAdapter mAdapter = null;

    private ContentObserver contentObserver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        final FragmentActivity activity = this.getActivity();
        String title = activity.getString(R.string.label_track);
        this.setTitle(title);

        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                activity.getSupportLoaderManager()
                        .getLoader(MainActivity.TRACK_LOADER)
                        .onContentChanged();
            }
        };
        activity.getContentResolver()
                .registerContentObserver(TongrenluContentProvider.TRACK_URI,
                                         true,
                                         this.contentObserver);

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_expandable_list_view,
                                           null,
                                           false);
        this.mAdapter = new PlaylistTrackListAdapter(this.getActivity());
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mListView = (ActionSlideExpandableListView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setItemActionListener(this,
                                             R.id.item,
                                             R.id.action_play,
                                             R.id.action_delete);
        this.mProgressContainer = view.findViewById(R.id.progressContainer);
        this.mProgressContainer.setVisibility(View.VISIBLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = this.getActivity();
        activity.getSupportLoaderManager()
                .initLoader(MainActivity.TRACK_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final FragmentActivity activity = this.getActivity();
        final CursorLoader loader = new CursorLoader(activity);
        loader.setUri(TongrenluContentProvider.TRACK_URI);
        loader.setSelection("downloadFlg = ?");
        loader.setSelectionArgs(new String[] { "1" });
        loader.setSortOrder("_id asc");
        return loader;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
        this.mAdapter.swapCursor(c);
        this.mProgressContainer.setVisibility(View.GONE);
        if (this.mAdapter.isEmpty()) {
            this.mListView.setVisibility(View.GONE);
            this.mEmpty.setVisibility(View.VISIBLE);
        } else {
            this.mEmpty.setVisibility(View.GONE);
            this.mListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        this.mAdapter.swapCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final FragmentActivity activity = this.getActivity();
        activity.getContentResolver()
                .unregisterContentObserver(this.contentObserver);
        activity.getSupportLoaderManager()
                .destroyLoader(MainActivity.TRACK_LOADER);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_track, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_play_all:
            // this.refreshAlbumList();
            this.playTrack(0);
            break;
        default:
            break;
        }
        return true;
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

            final FragmentActivity activity = this.getActivity();
            final Intent serviceIntent = new Intent(activity,
                                                    MusicService.class);
            serviceIntent.setAction(MusicService.ACTION_ADD);
            serviceIntent.putParcelableArrayListExtra("trackBeanList",
                                                      trackBeanList);
            serviceIntent.putExtra("position", position);
            activity.startService(serviceIntent);

            final Intent activityIntent = new Intent(activity,
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

        final FragmentActivity activity = this.getActivity();
        File file = HttpConstants.getMp3(activity, articleId, fileId);
        FileUtils.deleteQuietly(file);
    }

    private void deleteFromTrack(Cursor c) {
        String articleId = c.getString(c.getColumnIndex("articleId"));
        String fileId = c.getString(c.getColumnIndex("fileId"));
        final FragmentActivity activity = this.getActivity();
        ContentResolver contentResolver = activity.getContentResolver();
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
        final FragmentActivity activity = this.getActivity();
        ContentResolver contentResolver = activity.getContentResolver();
        contentResolver.delete(TongrenluContentProvider.PLAYLIST_TRACK_URI,
                               "articleId = ? and fileId = ?",
                               new String[] { articleId, fileId });
    }
}
