package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.loader.JSONLoader;
import info.tongrenlu.android.music.MusicInfoActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.MusicGridAdapter;
import info.tongrenlu.android.music.provider.AlbumContentProvider;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class MusicGridFragment extends TitleFragment implements OnItemClickListener, OnClickListener {

    public static final int ALBUM_CURSOR_LOADER = 1;
    public static final int ALBUM_LIST_LOADER = 2;

    private View mProgress = null;
    private View mEmpty = null;
    private GridView mListView = null;
    private MusicGridAdapter mAdapter = null;

    public MusicGridFragment() {
        this.setTitle("所有专辑");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        this.mAdapter = new MusicGridAdapter(this.getActivity(), null);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_grid_view,
                                           null,
                                           false);
        this.mProgress = view.findViewById(android.R.id.progress);
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mEmpty.setOnClickListener(this);

        this.mListView = (GridView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = this.getActivity();
        activity.getSupportLoaderManager()
                .initLoader(ALBUM_CURSOR_LOADER,
                            null,
                            new AlbumCursorLoaderCallback());

        this.mProgress.setVisibility(View.VISIBLE);
        this.mListView.setVisibility(View.GONE);
        this.mEmpty.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FragmentActivity activity = this.getActivity();
        LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.destroyLoader(ALBUM_CURSOR_LOADER);
        loaderManager.destroyLoader(ALBUM_LIST_LOADER);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case android.R.id.empty:
            FragmentActivity activity = this.getActivity();
            LoaderManager loaderManager = activity.getSupportLoaderManager();
            loaderManager.initLoader(ALBUM_LIST_LOADER,
                                     null,
                                     new MusicListLoaderCallback());
            this.mProgress.setVisibility(View.VISIBLE);
            this.mListView.setVisibility(View.GONE);
            this.mEmpty.setVisibility(View.GONE);
            break;

        default:
            break;
        }

    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
        final Cursor c = (Cursor) listView.getItemAtPosition(position);
        final String articleId = c.getString(c.getColumnIndex("article_id"));
        final String title = c.getString(c.getColumnIndex("title"));

        final Intent intent = new Intent();
        intent.setClass(this.getActivity(), MusicInfoActivity.class);
        intent.putExtra("articleId", articleId);
        intent.putExtra("title", title);

        this.startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_music_grid, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            FragmentActivity activity = this.getActivity();
            LoaderManager loaderManager = activity.getSupportLoaderManager();
            loaderManager.initLoader(ALBUM_LIST_LOADER,
                                     null,
                                     new MusicListLoaderCallback());
            this.mProgress.setVisibility(View.VISIBLE);
            break;
        default:
            break;
        }
        return true;
    }

    private class AlbumCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
            Context context = MusicGridFragment.this.getActivity();
            return new CursorLoader(context,
                                    AlbumContentProvider.URI,
                                    null,
                                    null,
                                    null,
                                    null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            MusicGridFragment.this.mAdapter.swapCursor(c);
            if (c.getCount() == 0) {
                MusicGridFragment.this.mProgress.setVisibility(View.GONE);
                MusicGridFragment.this.mEmpty.setVisibility(View.VISIBLE);
                MusicGridFragment.this.mListView.setVisibility(View.GONE);
            } else {
                MusicGridFragment.this.mProgress.setVisibility(View.GONE);
                MusicGridFragment.this.mEmpty.setVisibility(View.GONE);
                MusicGridFragment.this.mListView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            MusicGridFragment.this.mAdapter.swapCursor(null);
        }

    }

    private class MusicListLoaderCallback implements LoaderCallbacks<PaginateSupport> {

        @Override
        public Loader<PaginateSupport> onCreateLoader(int loaderId, Bundle args) {
            Context context = MusicGridFragment.this.getActivity();
            final Uri uri = HttpConstants.getMusicListUri(context);
            Bundle bundle = new Bundle();
            bundle.putInt("s", Integer.MAX_VALUE);
            return new MusicListLoader(context, uri, bundle);
        }

        @Override
        public void onLoadFinished(Loader<PaginateSupport> loader, PaginateSupport data) {
            MusicGridFragment.this.getActivity()
                                  .getSupportLoaderManager()
                                  .getLoader(ALBUM_CURSOR_LOADER)
                                  .onContentChanged();
        }

        @Override
        public void onLoaderReset(Loader<PaginateSupport> loader) {
        }
    }

    private static class MusicListLoader extends JSONLoader<PaginateSupport> {

        public MusicListLoader(Context ctx, Uri uri, Bundle parameters) {
            super(ctx, uri, parameters);
        }

        @Override
        protected PaginateSupport parseJSON(final JSONObject responseJSON) throws JSONException {
            PaginateSupport paginate = new PaginateSupport();
            if (responseJSON.getBoolean("result")) {
                final JSONObject pageJSON = responseJSON.getJSONObject("page");
                final int itemCount = pageJSON.getInt("itemCount");
                final int page = pageJSON.getInt("page");
                final int size = pageJSON.getInt("size");
                final JSONArray items = pageJSON.getJSONArray("items");
                ContentResolver contentResolver = this.getContext()
                                                      .getContentResolver();
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject musicJsonObject = items.getJSONObject(i);
                    String articleId = musicJsonObject.getString("articleId");
                    String title = musicJsonObject.getString("title");
                    Cursor c = contentResolver.query(AlbumContentProvider.URI,
                                                     null,
                                                     "article_id = ?",
                                                     new String[] { articleId },
                                                     null);
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("article_id", articleId);
                    contentValues.put("title", title);
                    if (c.getCount() > 0) {
                        contentResolver.update(AlbumContentProvider.URI,
                                               contentValues,
                                               "article_id = ?",
                                               new String[] { articleId });
                    } else {
                        contentResolver.insert(AlbumContentProvider.URI,
                                               contentValues);
                    }
                    c.close();
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
            String text = context.getString(R.string.err_network);
            this.showErrorToast(text);
        }

        @Override
        protected void onNetworkError(final int code) {
            final Context context = this.getContext();
            String text = context.getString(R.string.err_network);
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
