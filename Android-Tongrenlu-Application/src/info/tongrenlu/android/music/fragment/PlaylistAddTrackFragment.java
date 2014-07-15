package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.MainActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.SimpleTrackListAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.TrackBean;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class PlaylistAddTrackFragment extends Fragment implements
        LoaderCallbacks<Cursor>, OnItemClickListener {

    private View mProgressContainer = null;
    private View mEmpty = null;
    private ListView mListView = null;
    private CursorAdapter mAdapter = null;

    private PlaylistAddTrackFragmentListener mListener = null;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (activity instanceof PlaylistAddTrackFragmentListener) {
            this.mListener = (PlaylistAddTrackFragmentListener) activity;
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simple_list_view,
                                container,
                                false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mEmpty.setVisibility(View.GONE);

        this.mAdapter = new SimpleTrackListAdapter(this.getActivity());
        this.mListView = (ListView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        this.mListView.setVisibility(View.GONE);

        this.mProgressContainer = view.findViewById(R.id.progressContainer);
        this.mProgressContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity activity = this.getActivity();
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
        activity.getSupportLoaderManager()
                .destroyLoader(MainActivity.TRACK_LOADER);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    @Override
    public void onItemClick(final AdapterView<?> parent,
                            final View view,
                            final int position,
                            final long id) {
        final Cursor c = (Cursor) this.mAdapter.getItem(position);
        final TrackBean trackBean = new TrackBean();
        trackBean.setArticleId(c.getString(c.getColumnIndex("articleId")));
        trackBean.setFileId(c.getString(c.getColumnIndex("fileId")));
        trackBean.setName(c.getString(c.getColumnIndex("name")));
        trackBean.setArtist(c.getString(c.getColumnIndex("artist")));
        trackBean.setTrackNumber(0);
        this.mListener.onAddTrack(trackBean);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_playlist_add_track, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_close:
            this.mListener.onAddTrackFinish();
            break;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    public interface PlaylistAddTrackFragmentListener {
        void onAddTrack(TrackBean trackBean);

        void onAddTrackFinish();
    }
}
