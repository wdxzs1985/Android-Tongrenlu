package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.music.MainActivity;
import info.tongrenlu.android.music.PlaylistTrackActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import android.content.Context;
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
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class PlaylistFragment extends TitleFragment implements OnItemClickListener {

    private View mProgressContainer = null;
    private ListView mListView = null;
    private View mEmpty = null;

    private CursorAdapter mAdapter = null;
    private ContentObserver contentObserver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentActivity activity = this.getActivity();

        String title = activity.getString(R.string.label_playlist);
        this.setTitle(title);

        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                activity.getSupportLoaderManager()
                        .getLoader(MainActivity.PLAYLIST_LOADER)
                        .onContentChanged();
            }
        };
        activity.getContentResolver()
                .registerContentObserver(TongrenluContentProvider.PLAYLIST_URI,
                                         true,
                                         this.contentObserver);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final FragmentActivity activity = this.getActivity();
        activity.getContentResolver()
                .unregisterContentObserver(this.contentObserver);
        activity.getSupportLoaderManager()
                .destroyLoader(MainActivity.PLAYLIST_LOADER);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_playlist,
                                           null,
                                           false);
        this.mAdapter = new SimpleCursorAdapter(this.getActivity(),
                                                R.layout.list_item_playlist,
                                                null,
                                                new String[] { "title" },
                                                new int[] { android.R.id.text1 },
                                                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        this.mListView = (ListView) view.findViewById(android.R.id.list);
        this.mListView.setOnItemClickListener(this);
        this.mListView.setVisibility(View.GONE);
        this.mListView.setAdapter(this.mAdapter);
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mProgressContainer = view.findViewById(R.id.progressContainer);
        this.mProgressContainer.setVisibility(View.VISIBLE);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = this.getActivity();
        activity.getSupportLoaderManager()
                .initLoader(MainActivity.PLAYLIST_LOADER,
                            null,
                            new PlaylistCursorLoaderCallback());
    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
        FragmentActivity activity = this.getActivity();
        final Intent intent = new Intent(activity, PlaylistTrackActivity.class);
        intent.putExtra("playlistId", itemId);
        this.startActivity(intent);
    }

    private class PlaylistCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Context context = PlaylistFragment.this.getActivity();
            return new CursorLoader(context,
                                    TongrenluContentProvider.PLAYLIST_URI,
                                    null,
                                    null,
                                    null,
                                    null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            PlaylistFragment.this.mAdapter.swapCursor(c);
            PlaylistFragment.this.mProgressContainer.setVisibility(View.GONE);
            if (c.getCount() == 0) {
                PlaylistFragment.this.mListView.setVisibility(View.GONE);
                PlaylistFragment.this.mEmpty.setVisibility(View.VISIBLE);
            } else {
                PlaylistFragment.this.mListView.setVisibility(View.VISIBLE);
                PlaylistFragment.this.mEmpty.setVisibility(View.GONE);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            PlaylistFragment.this.mAdapter.swapCursor(null);
        }

    }

}
