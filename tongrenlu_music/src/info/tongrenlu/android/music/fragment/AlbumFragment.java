package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.music.AlbumPageActivity;
import info.tongrenlu.android.music.MainActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.AlbumGridAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class AlbumFragment extends TitleFragment implements OnItemClickListener {

    public static final int ALBUM_CURSOR_LOADER = 1;

    private View mProgressContainer = null;
    private GridView mListView = null;
    private AlbumGridAdapter mAdapter = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        this.mAdapter = new AlbumGridAdapter(this.getActivity(), null);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_album, null, false);

        this.mListView = (GridView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);

        this.mProgressContainer = view.findViewById(R.id.progressContainer);
        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final FragmentActivity activity = this.getActivity();
        String title = activity.getApplicationContext()
                               .getString(R.string.label_album);
        this.setTitle(title);

        activity.getSupportLoaderManager()
                .initLoader(AlbumFragment.ALBUM_CURSOR_LOADER,
                            null,
                            new AlbumCursorLoaderCallback());
        this.mProgressContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final FragmentActivity activity = this.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.destroyLoader(AlbumFragment.ALBUM_CURSOR_LOADER);
    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
        final Cursor c = (Cursor) listView.getItemAtPosition(position);
        final String articleId = c.getString(c.getColumnIndex("articleId"));
        final String title = c.getString(c.getColumnIndex("title"));

        final Intent intent = new Intent();
        intent.setClass(this.getActivity(), AlbumPageActivity.class);
        intent.putExtra("articleId", articleId);
        intent.putExtra("title", title);
        intent.putExtra("position", position);

        this.startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_music_grid, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            // this.refreshAlbumList();
            ((MainActivity) this.getActivity()).dispatchUpdateAlbum();
            break;
        default:
            break;
        }
        return true;
    }

    private class AlbumCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(final int loaderId, final Bundle args) {
            final Context context = AlbumFragment.this.getActivity();
            return new CursorLoader(context,
                                    TongrenluContentProvider.ALBUM_URI,
                                    null,
                                    null,
                                    null,
                                    "articleId desc");
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
            AlbumFragment.this.mAdapter.swapCursor(c);
            AlbumFragment.this.mProgressContainer.setVisibility(View.GONE);
            if (c.getCount() == 0) {
                // AlbumFragment.this.mEmpty.setVisibility(View.VISIBLE);
                AlbumFragment.this.mListView.setVisibility(View.GONE);
                ((MainActivity) AlbumFragment.this.getActivity()).dispatchUpdateAlbum();
            } else {
                // AlbumFragment.this.mEmpty.setVisibility(View.GONE);
                AlbumFragment.this.mListView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
            AlbumFragment.this.mAdapter.swapCursor(null);
        }

    }

}
