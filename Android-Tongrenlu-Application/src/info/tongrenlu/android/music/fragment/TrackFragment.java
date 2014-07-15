package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.music.MainActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.TrackListAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import android.app.Activity;
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
    private TrackFragmentListener mListener = null;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (activity instanceof TrackFragmentListener) {
            this.mListener = (TrackFragmentListener) activity;
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);

        final FragmentActivity activity = this.getActivity();
        final String title = activity.getString(R.string.label_track);
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
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_expandable_list_view,
                                           container,
                                           false);
        this.mAdapter = new TrackListAdapter(this.getActivity());
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mListView = (ActionSlideExpandableListView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setItemActionListener(this,
                                             R.id.item,
                                             R.id.action_add_to_playlist,
                                             R.id.action_delete);
        this.mProgressContainer = view.findViewById(R.id.progressContainer);
        this.mProgressContainer.setVisibility(View.VISIBLE);
        return view;
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
        loader.setSortOrder("articleId desc, trackNumber asc");
        return loader;
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
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
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
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_track, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_play_all:
            this.playTrack(0);
            break;
        default:
            break;
        }
        return true;
    }

    @Override
    public void onClick(final View itemView,
                        final View clickedView,
                        final int position) {

        switch (clickedView.getId()) {
        case R.id.item:
            this.playTrack(position);
            break;
        case R.id.action_add_to_playlist:
            this.addToPlaylist(position);
            break;
        case R.id.action_delete:
            this.deleteTrack(position);
            break;
        default:
            break;
        }
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
                trackBean.setDownloadFlg(c.getString(c.getColumnIndex("downloadFlg")));
                trackBeanList.add(trackBean);
                c.moveToNext();
            }

            this.mListener.onPlay(trackBeanList, position);
        }
    }

    private void addToPlaylist(final int position) {
        final Cursor c = (Cursor) this.mListView.getItemAtPosition(position);
        final TrackBean trackBean = new TrackBean();
        trackBean.setArticleId(c.getString(c.getColumnIndex("articleId")));
        trackBean.setFileId(c.getString(c.getColumnIndex("fileId")));
        trackBean.setName(c.getString(c.getColumnIndex("name")));
        trackBean.setArtist(c.getString(c.getColumnIndex("artist")));
        trackBean.setDownloadFlg(c.getString(c.getColumnIndex("downloadFlg")));
        trackBean.setTrackNumber(0);
        this.mListener.onAddToPlaylist(trackBean);
    }

    private void deleteTrack(final int position) {
        final Cursor c = (Cursor) this.mListView.getItemAtPosition(position);
        final TrackBean trackBean = new TrackBean();
        trackBean.setArticleId(c.getString(c.getColumnIndex("articleId")));
        trackBean.setFileId(c.getString(c.getColumnIndex("fileId")));

        this.mListener.onDeleteTrack(trackBean);
    }

    public interface TrackFragmentListener {

        void onPlay(ArrayList<TrackBean> trackBeanList, int position);

        void onDeleteTrack(TrackBean trackBean);

        void onAddToPlaylist(TrackBean trackBean);
    }
}
