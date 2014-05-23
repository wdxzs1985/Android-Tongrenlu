package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.PlaylistTrackActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.PlaylistTrackListAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class PlaylistTrackFragment extends Fragment implements LoaderCallbacks<Cursor>, OnItemClickListener {

    private Uri mUri = null;
    private Uri mTrackUri = null;
    private ContentObserver contentObserver = null;

    private View mProgressContainer = null;
    private View mEmpty = null;
    private ListView mListView = null;
    private CursorAdapter mAdapter = null;

    private PlaylistTrackFragmentListener mListener = null;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (activity instanceof PlaylistTrackFragmentListener) {
            this.mListener = (PlaylistTrackFragmentListener) activity;
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        long playlistId = this.getArguments()
                              .getLong(PlaylistTrackActivity.PLAYLIST_ID,
                                       PlaylistTrackActivity.BAD_ID);

        this.mUri = ContentUris.withAppendedId(TongrenluContentProvider.PLAYLIST_URI,
                                               playlistId);
        this.mTrackUri = Uri.withAppendedPath(this.mUri, "track");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_track,
                                container,
                                false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = this.getActivity().getApplicationContext();
        this.mAdapter = new PlaylistTrackListAdapter(context);

        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mListView = (ListView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        this.mProgressContainer = view.findViewById(R.id.progressContainer);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.getLoaderManager()
            .initLoader(PlaylistTrackActivity.PLAYLIST_LOADER_ID, null, this);
        this.mProgressContainer.setVisibility(View.VISIBLE);

        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                PlaylistTrackFragment.this.getLoaderManager()
                                          .getLoader(PlaylistTrackActivity.PLAYLIST_LOADER_ID)
                                          .onContentChanged();
            }
        };

        this.getActivity()
            .getContentResolver()
            .registerContentObserver(this.mTrackUri, true, this.contentObserver);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final CursorLoader loader = new CursorLoader(this.getActivity());
        loader.setUri(this.mTrackUri);
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
        this.getActivity()
            .getContentResolver()
            .unregisterContentObserver(this.contentObserver);
        this.getLoaderManager()
            .destroyLoader(PlaylistTrackActivity.PLAYLIST_LOADER_ID);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_playlist_track, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_play_all:
            this.playTrack(0);
            break;
        case R.id.action_delete:
            this.deletePlaylist();
            break;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void playTrack(final int position) {
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
            this.mListener.onPlay(trackBeanList, position);
        }
    }

    private void deleteTrack(int position) {
        final Cursor c = (Cursor) this.mListView.getItemAtPosition(position);
        long id = c.getLong(c.getColumnIndex("_id"));
        this.mListener.onDeleteTrack(this.mTrackUri, id);
    }

    private void deletePlaylist() {
        this.mListener.onDeletePlaylist(this.mUri);
    }

    public interface PlaylistTrackFragmentListener {

        void onPlay(ArrayList<TrackBean> trackBeanList, int position);

        void onDeleteTrack(Uri uri, long id);

        void onDeletePlaylist(Uri uri);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position != ListView.INVALID_POSITION) {
            this.playTrack(position);
        }
    }
}
