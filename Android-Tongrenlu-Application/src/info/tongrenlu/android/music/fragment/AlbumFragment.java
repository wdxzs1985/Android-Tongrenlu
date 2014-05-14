package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.music.AlbumInfoActivity;
import info.tongrenlu.android.music.MainActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.AlbumGridAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Build;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SearchView;

public class AlbumFragment extends TitleFragment implements OnItemClickListener {

    private View mProgressContainer = null;
    private GridView mListView = null;
    private View mEmpty = null;

    private CursorAdapter mAdapter = null;
    private ContentObserver contentObserver = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);

        final FragmentActivity activity = this.getActivity();
        String title = activity.getString(R.string.label_album);
        this.setTitle(title);

        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                activity.getSupportLoaderManager()
                        .getLoader(MainActivity.ALBUM_LOADER)
                        .onContentChanged();
            }
        };
        activity.getContentResolver()
                .registerContentObserver(TongrenluContentProvider.ALBUM_URI,
                                         true,
                                         this.contentObserver);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_album, null, false);
        this.mAdapter = new AlbumGridAdapter(this.getActivity());
        this.mListView = (GridView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        this.mProgressContainer = view.findViewById(R.id.progressContainer);
        this.mProgressContainer.setVisibility(View.VISIBLE);

        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mEmpty.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity activity = this.getActivity();
        activity.getSupportLoaderManager()
                .initLoader(MainActivity.ALBUM_LOADER,
                            this.getArguments(),
                            new AlbumCursorLoaderCallback());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final FragmentActivity activity = this.getActivity();
        activity.getContentResolver()
                .unregisterContentObserver(this.contentObserver);
        activity.getSupportLoaderManager()
                .destroyLoader(MainActivity.ALBUM_LOADER);
    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
        final Cursor c = (Cursor) listView.getItemAtPosition(position);
        final String articleId = c.getString(c.getColumnIndex("articleId"));
        final String title = c.getString(c.getColumnIndex("title"));

        final Intent intent = new Intent();
        intent.setClass(this.getActivity(), AlbumInfoActivity.class);
        intent.putExtra("articleId", articleId);
        intent.putExtra("title", title);

        this.startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_music_grid, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Activity activity = this.getActivity();
            SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search)
                                                     .getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
            searchView.setIconifiedByDefault(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            ((MainActivity) this.getActivity()).dispatchUpdateAlbum();
            return true;
        case R.id.action_search:
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                this.getActivity().onSearchRequested();
            }
            return true;
        default:
            break;
        }
        return false;
    }

    private class AlbumCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(final int loaderId, final Bundle args) {
            final Context context = AlbumFragment.this.getActivity();
            String selection = null;
            String[] selectionArgs = null;

            if (args != null && args.containsKey("query")) {
                String query = args.getString("query");
                selection = "title LIKE ?";
                selectionArgs = new String[] { "%" + query + "%" };
            }

            return new CursorLoader(context,
                                    TongrenluContentProvider.ALBUM_URI,
                                    null,
                                    selection,
                                    selectionArgs,
                                    "articleId desc");
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
            AlbumFragment.this.mAdapter.swapCursor(c);
            AlbumFragment.this.mProgressContainer.setVisibility(View.GONE);
            if (c.getCount() == 0) {
                AlbumFragment.this.mEmpty.setVisibility(View.VISIBLE);
                AlbumFragment.this.mListView.setVisibility(View.GONE);

                Activity activity = AlbumFragment.this.getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) AlbumFragment.this.getActivity()).dispatchUpdateAlbum();
                } else {
                }
            } else {
                AlbumFragment.this.mEmpty.setVisibility(View.GONE);
                AlbumFragment.this.mListView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
            AlbumFragment.this.mAdapter.swapCursor(null);
        }

    }

}
