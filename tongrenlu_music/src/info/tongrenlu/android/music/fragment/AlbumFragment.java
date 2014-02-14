package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.loader.BaseLoader;
import info.tongrenlu.android.music.AlbumPageActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.AlbumGridAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.support.RESTClient;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

public class AlbumFragment extends TitleFragment implements OnItemClickListener, View.OnClickListener {

    public static final int ALBUM_CURSOR_LOADER = 1;
    public static final int ALBUM_JSON_LOADER = 2;

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
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
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
        loaderManager.destroyLoader(AlbumFragment.ALBUM_JSON_LOADER);
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
        loaderManager.initLoader(AlbumFragment.ALBUM_JSON_LOADER,
                                 null,
                                 new AlbumJsonLoaderCallback());
        this.mProgress.setVisibility(View.VISIBLE);
        this.mListView.setVisibility(View.VISIBLE);
        this.mEmpty.setVisibility(View.GONE);
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
            this.refreshAlbumList();
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

    private class AlbumJsonLoaderCallback implements LoaderCallbacks<Boolean> {

        @Override
        public Loader<Boolean> onCreateLoader(final int loaderId, final Bundle args) {
            final Context context = AlbumFragment.this.getActivity();
            return new AlbumDataLoader(context);
        }

        @Override
        public void onLoadFinished(final Loader<Boolean> loader, final Boolean data) {
            AlbumFragment.this.getActivity()
                              .getSupportLoaderManager()
                              .getLoader(AlbumFragment.ALBUM_CURSOR_LOADER)
                              .onContentChanged();
        }

        @Override
        public void onLoaderReset(final Loader<Boolean> loader) {
        }
    }

    private static class AlbumDataLoader extends BaseLoader<Boolean> {

        static final int NO_ERROR = 0;
        static final int NETWORK_ERROR = -100;
        static final int PARSE_ERROR = -200;

        private int mErrorCode = NO_ERROR;

        public AlbumDataLoader(Context ctx) {
            super(ctx);
        }

        @Override
        public Boolean loadInBackground() {
            this.refreshAlbumData();
            return this.isNoError();
        }

        protected boolean isNoError() {
            return this.mErrorCode == NO_ERROR;
        }

        private void refreshAlbumData() {
            Uri hostUri = HttpConstants.getHostUri(this.getContext());
            Uri albumUri = Uri.withAppendedPath(hostUri, "fm/music");
            Bundle param = new Bundle();
            param.putInt("s", Integer.MAX_VALUE);
            String json = this.processHttpGet(albumUri, param);
            if (this.isNoError() && StringUtils.isNotBlank(json)) {
                try {
                    JSONObject albumJson = new JSONObject(json);
                    this.parseAlbumJSON(albumJson);
                } catch (JSONException e) {
                    this.mErrorCode = PARSE_ERROR;
                }
            }
        }

        private String processHttpGet(Uri uri, Bundle param) {
            RESTClient.RESTResponse response = new RESTClient(RESTClient.HTTPVerb.GET,
                                                              uri,
                                                              param).load();
            final int code = response.getCode();
            final String json = response.getData();
            if (code != 200) {
                this.mErrorCode = NETWORK_ERROR;
            }
            return json;
        }

        protected void parseAlbumJSON(final JSONObject responseJSON) throws JSONException {
            if (responseJSON.getBoolean("result")) {
                final JSONObject pageJSON = responseJSON.optJSONObject("page");
                final JSONArray items = pageJSON.optJSONArray("items");
                List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                final ContentResolver contentResolver = this.getContext()
                                                            .getContentResolver();
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject albumObject = items.optJSONObject(i);
                    final String articleId = albumObject.optString("articleId");
                    final String title = albumObject.optString("title");
                    Cursor cursor = null;
                    try {
                        cursor = contentResolver.query(TongrenluContentProvider.ALBUM_URI,
                                                       null,
                                                       "articleId = ?",
                                                       new String[] { articleId },
                                                       null);
                        if (cursor.getCount() == 0) {
                            final ContentValues contentValues = new ContentValues();
                            contentValues.put("articleId", articleId);
                            contentValues.put("title", title);
                            contentValues.put("collectFlg", 0);
                            contentValuesList.add(contentValues);
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(contentValuesList)) {
                    contentResolver.bulkInsert(TongrenluContentProvider.ALBUM_URI,
                                               contentValuesList.toArray(new ContentValues[] {}));
                    contentResolver.notifyChange(TongrenluContentProvider.ALBUM_URI,
                                                 null);
                }
            }
        }
    }
}
