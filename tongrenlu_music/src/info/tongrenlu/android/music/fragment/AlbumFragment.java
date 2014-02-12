package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.loader.JSONLoader;
import info.tongrenlu.android.music.AlbumInfoActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.AlbumGridAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.support.PaginateSupport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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

public class AlbumFragment extends TitleFragment implements
        OnItemClickListener, View.OnClickListener {

    public static final int ALBUM_CURSOR_LOADER = 1;
    public static final int ALBUM_LIST_LOADER = 2;

    private View mProgress = null;
    private View mEmpty = null;
    private GridView mListView = null;
    private AlbumGridAdapter mAdapter = null;

    public AlbumFragment() {
        this.setTitle("所有专辑");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        this.mAdapter = new AlbumGridAdapter(this.getActivity(), null);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_album, null, false);
        this.mProgress = view.findViewById(android.R.id.progress);
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mEmpty.findViewById(R.id.action_refresh).setOnClickListener(this);

        this.mListView = (GridView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity activity = this.getActivity();
        activity.getSupportLoaderManager()
                .initLoader(AlbumFragment.ALBUM_CURSOR_LOADER,
                            null,
                            new AlbumCursorLoaderCallback());

        this.mProgress.setVisibility(View.VISIBLE);
        this.mListView.setVisibility(View.GONE);
        this.mEmpty.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final FragmentActivity activity = this.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.destroyLoader(AlbumFragment.ALBUM_CURSOR_LOADER);
        loaderManager.destroyLoader(AlbumFragment.ALBUM_LIST_LOADER);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
        case R.id.action_refresh:
            this.refreshAlbumList();
            break;
        default:
            break;
        }

    }

    private void refreshAlbumList() {
        final FragmentActivity activity = this.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.initLoader(AlbumFragment.ALBUM_LIST_LOADER,
                                 null,
                                 new MusicListLoaderCallback());
        this.mProgress.setVisibility(View.VISIBLE);
        this.mListView.setVisibility(View.VISIBLE);
        this.mEmpty.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(final AdapterView<?> listView,
                            final View itemView,
                            final int position,
                            final long itemId) {
        final Cursor c = (Cursor) listView.getItemAtPosition(position);
        final String articleId = c.getString(c.getColumnIndex("article_id"));
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
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            this.refreshAlbumList();
            break;
        default:
            break;
        }
        return true;
    }

    private class AlbumCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(final int loaderId,
                                             final Bundle args) {
            final Context context = AlbumFragment.this.getActivity();
            return new CursorLoader(context,
                                    TongrenluContentProvider.ALBUM_URI,
                                    null,
                                    null,
                                    null,
                                    null);
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
            AlbumFragment.this.mAdapter.swapCursor(c);
            if (c.getCount() == 0) {
                AlbumFragment.this.mProgress.setVisibility(View.GONE);
                AlbumFragment.this.mEmpty.setVisibility(View.VISIBLE);
                AlbumFragment.this.mListView.setVisibility(View.GONE);
            } else {
                AlbumFragment.this.mProgress.setVisibility(View.GONE);
                AlbumFragment.this.mEmpty.setVisibility(View.GONE);
                AlbumFragment.this.mListView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
            AlbumFragment.this.mAdapter.swapCursor(null);
        }

    }

    private class MusicListLoaderCallback implements
            LoaderCallbacks<PaginateSupport> {

        @Override
        public Loader<PaginateSupport> onCreateLoader(final int loaderId,
                                                      final Bundle args) {
            final Context context = AlbumFragment.this.getActivity();
            final Uri uri = HttpConstants.getMusicListUri(context);
            final Bundle bundle = new Bundle();
            bundle.putInt("s", Integer.MAX_VALUE);
            return new MusicListLoader(context, uri, bundle);
        }

        @Override
        public void onLoadFinished(final Loader<PaginateSupport> loader,
                                   final PaginateSupport data) {
            AlbumFragment.this.getActivity()
                              .getSupportLoaderManager()
                              .getLoader(AlbumFragment.ALBUM_CURSOR_LOADER)
                              .onContentChanged();
        }

        @Override
        public void onLoaderReset(final Loader<PaginateSupport> loader) {
        }
    }

    private static class MusicListLoader extends JSONLoader<PaginateSupport> {

        public MusicListLoader(final Context ctx, final Uri uri,
                final Bundle parameters) {
            super(ctx, uri, parameters);
        }

        @Override
        protected PaginateSupport parseJSON(final JSONObject responseJSON)
                throws JSONException {
            final PaginateSupport paginate = new PaginateSupport();
            if (responseJSON.getBoolean("result")) {
                final JSONObject pageJSON = responseJSON.getJSONObject("page");
                final int itemCount = pageJSON.getInt("itemCount");
                final int page = pageJSON.getInt("page");
                final int size = pageJSON.getInt("size");
                final JSONArray items = pageJSON.getJSONArray("items");
                final ContentResolver contentResolver = this.getContext()
                                                            .getContentResolver();
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject musicJsonObject = items.getJSONObject(i);
                    final String articleId = musicJsonObject.getString("articleId");
                    final String title = musicJsonObject.getString("title");
                    final Cursor c = contentResolver.query(TongrenluContentProvider.ALBUM_URI,
                                                           null,
                                                           "article_id = ?",
                                                           new String[] { articleId },
                                                           null);
                    final ContentValues contentValues = new ContentValues();
                    contentValues.put("article_id", articleId);
                    contentValues.put("title", title);
                    if (c.getCount() == 0) {
                        contentResolver.insert(TongrenluContentProvider.ALBUM_URI,
                                               contentValues);
                    }
                }

                paginate.setItemCount(itemCount);
                paginate.setPage(page);
                paginate.setSize(size);
            }
            return paginate;
        }

        @Override
        protected void onJSONException(final JSONException e) {
            final Context context = this.getContext();
            final String text = context.getString(R.string.err_network);
            this.showErrorToast(text);
        }

        @Override
        protected void onNetworkError(final int code) {
            final Context context = this.getContext();
            final String text = context.getString(R.string.err_network);
            this.showErrorToast(text);
        }

        private void showErrorToast(final String text) {
            // final Context context = this.getContext();
            // new Handler().post(new Runnable() {
            // @Override
            // public void run() {
            // Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            // }
            // });
        }
    }
}
