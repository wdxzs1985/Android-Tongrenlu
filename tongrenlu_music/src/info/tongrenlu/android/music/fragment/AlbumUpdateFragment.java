package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.loader.BaseLoader;
import info.tongrenlu.android.music.MainActivity;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.widget.Toast;

public class AlbumUpdateFragment extends DialogFragment {

    public static final int ALBUM_JSON_LOADER = 2;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = this.getActivity();
        ProgressDialog dialog = new ProgressDialog(activity);
        // dialog.setTitle(R.string.loading);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage(activity.getText(R.string.loading));
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.initLoader(ALBUM_JSON_LOADER,
                                 null,
                                 new AlbumJsonLoaderCallback());
        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final FragmentActivity activity = this.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        loaderManager.destroyLoader(ALBUM_JSON_LOADER);
    }

    private class AlbumJsonLoaderCallback implements LoaderCallbacks<Boolean> {

        @Override
        public Loader<Boolean> onCreateLoader(final int loaderId, final Bundle args) {
            TongrenluApplication application = (TongrenluApplication) AlbumUpdateFragment.this.getActivity()
                                                                                              .getApplication();

            HttpHelper http = application.getHttpHelper();

            String host = HttpConstants.getHost(application);
            String part = "/fm/music?s=" + Integer.MAX_VALUE;
            String url = host + part;

            return new AlbumDataLoader(application, http, url);
        }

        @Override
        public void onLoadFinished(final Loader<Boolean> loader, final Boolean noError) {
            if (noError) {
                AlbumUpdateFragment.this.getActivity()
                                        .getSupportLoaderManager()
                                        .getLoader(MainActivity.ALBUM_LOADER)
                                        .onContentChanged();
                AlbumUpdateFragment.this.dismissAllowingStateLoss();
            } else {
                Toast.makeText(AlbumUpdateFragment.this.getActivity(),
                               R.string.err_network,
                               Toast.LENGTH_LONG).show();
            }
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

        private final HttpHelper http;
        private final String url;

        public AlbumDataLoader(Context ctx, HttpHelper http, String url) {
            super(ctx);
            this.http = http;
            this.url = url;
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
            try {
                JSONObject responseJSON = this.http.getAsJson(this.url);
                this.parseAlbumJSON(responseJSON);
            } catch (JSONException e) {
                this.mErrorCode = PARSE_ERROR;
                e.printStackTrace();
            } catch (IOException e) {
                this.mErrorCode = NETWORK_ERROR;
                e.printStackTrace();
            }
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
