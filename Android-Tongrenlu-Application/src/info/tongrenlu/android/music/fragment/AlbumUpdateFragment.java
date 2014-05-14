package info.tongrenlu.android.music.fragment;

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
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class AlbumUpdateFragment extends DialogFragment {

    public static final int ALBUM_JSON_LOADER = 2;

    private AlbumTask mTask = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = this.getActivity();
        ProgressDialog dialog = new ProgressDialog(activity);
        // dialog.setTitle(R.string.loading);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle(R.string.loading);
        dialog.setMessage(activity.getText(R.string.loading_album));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(true);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mTask = new AlbumTask();
        this.mTask.execute();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (this.mTask != null) {
            this.mTask.cancel(true);
            this.mTask = null;
        }
    }

    private class AlbumTask extends AsyncTask<Object, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            TongrenluApplication application = (TongrenluApplication) AlbumUpdateFragment.this.getActivity()
                                                                                              .getApplication();
            return this.refreshAlbumData(application);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                AlbumUpdateFragment.this.getActivity()
                                        .getSupportLoaderManager()
                                        .getLoader(MainActivity.ALBUM_LOADER)
                                        .onContentChanged();
            } else {
                Toast.makeText(AlbumUpdateFragment.this.getActivity(),
                               R.string.err_network,
                               Toast.LENGTH_LONG).show();
            }
            AlbumUpdateFragment.this.dismissAllowingStateLoss();
        }

        private Boolean refreshAlbumData(TongrenluApplication application) {
            final int s = 10;
            int p = 1;
            boolean isLast = false;

            final HttpHelper http = application.getHttpHelper();
            final String host = HttpConstants.getHost(application);
            final ContentResolver contentResolver = application.getContentResolver();

            ProgressDialog dialog = (ProgressDialog) AlbumUpdateFragment.this.getDialog();

            int progress = 0;
            try {
                while (!isLast && !this.isCancelled()) {
                    String url = String.format("%s/fm/music?p=%d&s=%d",
                                               host,
                                               p,
                                               s);
                    JSONObject responseJSON = http.getAsJson(url);
                    if (responseJSON.getBoolean("result")) {
                        final JSONObject pageJSON = responseJSON.optJSONObject("page");
                        isLast = pageJSON.optBoolean("last");

                        dialog.setIndeterminate(false);
                        dialog.setMax(pageJSON.getInt("itemCount"));

                        final JSONArray items = pageJSON.optJSONArray("items");
                        List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
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
                            dialog.setProgress(++progress);
                        }
                        if (CollectionUtils.isNotEmpty(contentValuesList)) {
                            contentResolver.bulkInsert(TongrenluContentProvider.ALBUM_URI,
                                                       contentValuesList.toArray(new ContentValues[] {}));
                            contentResolver.notifyChange(TongrenluContentProvider.ALBUM_URI,
                                                         null);
                        }
                        p++;
                    } else {
                        isLast = true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

}
