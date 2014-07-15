package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.PlaylistTrackActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.SimpleTrackListAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
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

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

public class PlaylistTrackFragment extends Fragment implements LoaderCallbacks<Cursor>, OnItemClickListener {

    private Uri mUri = null;
    private Uri mTrackUri = null;
    private ContentObserver contentObserver = null;

    private View mProgressContainer = null;
    private View mEmpty = null;
    private DragSortListView mListView = null;
    private CursorAdapter mAdapter = null;

    private PlaylistTrackFragmentListener mListener = null;

    private DragSortController mController = null;

    private final DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(final int from, final int to) {
            PlaylistTrackFragment.this.swapTrack(from, to);
        }
    };

    private final DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener() {
        @Override
        public void remove(final int which) {
            PlaylistTrackFragment.this.deleteTrack(which);
        }
    };

    /**
     * Called in onCreateView. Override this to provide a custom
     * DragSortController.
     */
    public DragSortController buildController(final DragSortListView dslv) {
        final DragSortController controller = new DragSortController(dslv);
        controller.setDragHandleId(R.id.article_cover);
        controller.setRemoveEnabled(true);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DOWN);
        controller.setRemoveMode(DragSortController.FLING_REMOVE);
        return controller;
    }

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

        final long playlistId = this.getArguments()
                                    .getLong(PlaylistTrackActivity.PLAYLIST_ID,
                                             PlaylistTrackActivity.BAD_ID);

        this.mUri = ContentUris.withAppendedId(TongrenluContentProvider.PLAYLIST_URI,
                                               playlistId);
        this.mTrackUri = Uri.withAppendedPath(this.mUri, "track");

        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                System.out.println("onChange");
                PlaylistTrackFragment.this.getActivity()
                                          .getSupportLoaderManager()
                                          .getLoader(PlaylistTrackActivity.PLAYLIST_LOADER_ID)
                                          .onContentChanged();
            }
        };

        this.getActivity()
            .getContentResolver()
            .registerContentObserver(this.mTrackUri, true, this.contentObserver);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dragsort_list_view,
                                container,
                                false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Context context = this.getActivity().getApplicationContext();
        this.mAdapter = new SimpleTrackListAdapter(context);

        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mListView = (DragSortListView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);

        this.mController = this.buildController(this.mListView);
        this.mListView.setFloatViewManager(this.mController);
        this.mListView.setOnTouchListener(this.mController);
        this.mListView.setDragEnabled(true);
        this.mListView.setDropListener(this.onDrop);
        this.mListView.setRemoveListener(this.onRemove);

        this.mProgressContainer = view.findViewById(R.id.progressContainer);
        this.mProgressContainer.setVisibility(View.VISIBLE);

    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.getActivity()
            .getSupportLoaderManager()
            .initLoader(PlaylistTrackActivity.PLAYLIST_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {

        this.mProgressContainer.setVisibility(View.VISIBLE);

        final CursorLoader loader = new CursorLoader(this.getActivity());
        loader.setUri(this.mTrackUri);
        loader.setSortOrder("trackNumber asc");
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
        this.getActivity()
            .getSupportLoaderManager()
            .destroyLoader(PlaylistTrackActivity.PLAYLIST_LOADER_ID);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
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
        case R.id.action_add_track:
            this.mListener.onStartAddTrack();
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
                trackBean.setName(c.getString(c.getColumnIndex("name")));
                trackBean.setArtist(c.getString(c.getColumnIndex("artist")));
                trackBeanList.add(trackBean);

                c.moveToNext();
            }
            this.mListener.onPlay(trackBeanList, position);
        }
    }

    protected void swapTrack(final int from, final int to) {
        if (from != to) {
            final ArrayList<ContentValues> values = new ArrayList<ContentValues>();
            values.add(this.getSwapTrackValues(from, to));
            if (from < to) {
                for (int i = from + 1; i <= to; i++) {
                    values.add(this.getSwapTrackValues(i, i - 1));
                }
            } else if (from > to) {
                for (int i = from - 1; i >= to; i--) {
                    values.add(this.getSwapTrackValues(i, i + 1));
                }
            }
            this.mListener.onSwapTrack(this.mTrackUri, values);
        }
    }

    private ContentValues getSwapTrackValues(final int from, final int to) {
        final ContentValues values = new ContentValues();
        final Cursor c = this.mAdapter.getCursor();

        c.moveToPosition(from);
        values.put("_id", c.getLong(c.getColumnIndex("_id")));

        c.moveToPosition(to);
        values.put("trackNumber", c.getInt(c.getColumnIndex("trackNumber")));
        return values;
    }

    private void deleteTrack(final int position) {
        final long id = this.mListView.getItemIdAtPosition(position);
        this.mListener.onDeleteTrack(this.mTrackUri, id);
    }

    private void deletePlaylist() {
        this.mListener.onDeletePlaylist(this.mUri);
    }

    public interface PlaylistTrackFragmentListener {

        void onPlay(ArrayList<TrackBean> trackBeanList, int position);

        void onSwapTrack(Uri trackUri, ArrayList<ContentValues> values);

        void onDeleteTrack(Uri trackUri, long id);

        void onDeletePlaylist(Uri uri);

        void onStartAddTrack();
    }

    @Override
    public void onItemClick(final AdapterView<?> parent,
                            final View view,
                            final int position,
                            final long id) {
        if (position != ListView.INVALID_POSITION) {
            this.playTrack(position);
        }
    }
}
