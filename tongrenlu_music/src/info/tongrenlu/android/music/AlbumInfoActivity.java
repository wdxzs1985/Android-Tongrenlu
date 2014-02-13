package info.tongrenlu.android.music;

import info.tongrenlu.android.loader.BaseLoader;
import info.tongrenlu.android.music.adapter.AlbumTrackListAdapter;
import info.tongrenlu.android.music.async.LoadImageCacheTask;
import info.tongrenlu.android.music.fragment.AlbumFragment;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;
import info.tongrenlu.support.RESTClient;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class AlbumInfoActivity extends BaseActivity implements
        ActionSlideExpandableListView.OnActionClickListener, OnClickListener {

    public static final int ALBUM_TRACK_CURSOR_LOADER = 1;
    public static final int ALBUM_TRACK_JSON_LOADER = 2;

    private View mProgress = null;
    private View mEmpty = null;
    private ActionSlideExpandableListView mListView = null;
    private AlbumTrackListAdapter mAdapter = null;

    private Uri mUri = null;
    private Uri mTrackUri = null;
    private String mArticleId = null;
    private String mTitle = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_album_info);

        final Intent intent = this.getIntent();
        this.mArticleId = intent.getStringExtra("articleId");
        this.mTitle = intent.getStringExtra("title");

        this.mUri = Uri.withAppendedPath(TongrenluContentProvider.ALBUM_URI,
                                         this.mArticleId);
        this.mTrackUri = Uri.withAppendedPath(this.mUri, "track");

        this.initArticleCover(this.mArticleId);
        this.initAritcleTitle(this.mTitle);

        this.mAdapter = new AlbumTrackListAdapter(this, null);

        this.mProgress = this.findViewById(android.R.id.progress);
        this.mProgress.setVisibility(View.VISIBLE);

        this.mEmpty = this.findViewById(android.R.id.empty);
        this.mEmpty.setVisibility(View.GONE);

        this.mListView = (ActionSlideExpandableListView) this.findViewById(android.R.id.list);
        //
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setItemActionListener(this,
                                             R.id.item,
                                             R.id.action_play,
                                             R.id.action_download);
        this.mListView.setVisibility(View.GONE);

        final Button playAllButton = (Button) this.findViewById(R.id.action_play_all);
        playAllButton.setOnClickListener(this);
        final Button downloadAllButton = (Button) this.findViewById(R.id.action_download_all);
        downloadAllButton.setOnClickListener(this);

        this.getSupportLoaderManager()
            .initLoader(AlbumInfoActivity.ALBUM_TRACK_CURSOR_LOADER,
                        null,
                        new AlbumTrackCursorLoaderCallback());
    }

    private void initAritcleTitle(final String title) {
        final TextView articleTitle = (TextView) this.findViewById(R.id.article_title);
        articleTitle.setText(title);
    }

    private void initArticleCover(final String articleId) {
        final TongrenluApplication application = (TongrenluApplication) this.getApplication();
        final BitmapLruCache bitmapCache = application.getBitmapCache();
        final String url = HttpConstants.getCoverUrl(application,
                                                     articleId,
                                                     HttpConstants.S_COVER);
        final ImageView coverView = (ImageView) this.findViewById(R.id.article_cover);
        new LoadImageCacheTask() {

            @Override
            protected void onPostExecute(final Drawable result) {
                super.onPostExecute(result);
                if (!this.isCancelled() && result != null) {
                    final Drawable emptyDrawable = new ShapeDrawable();
                    final TransitionDrawable fadeInDrawable = new TransitionDrawable(new Drawable[] { emptyDrawable,
                                                                                                     result });
                    coverView.setImageDrawable(result);
                    fadeInDrawable.startTransition(200);
                }
            }

        }.execute(bitmapCache, url);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.getSupportLoaderManager()
            .destroyLoader(AlbumInfoActivity.ALBUM_TRACK_CURSOR_LOADER);
        this.getSupportLoaderManager()
            .destroyLoader(AlbumInfoActivity.ALBUM_TRACK_JSON_LOADER);
    }

    class AlbumTrackCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
            final CursorLoader loader = new CursorLoader(AlbumInfoActivity.this);
            loader.setUri(AlbumInfoActivity.this.mTrackUri);
            loader.setSortOrder("trackNumber asc");
            return loader;
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
            AlbumInfoActivity.this.mAdapter.swapCursor(c);
            if (AlbumInfoActivity.this.mAdapter.isEmpty()) {
                AlbumInfoActivity.this.refreshAlbumList();
            } else {
                AlbumInfoActivity.this.mProgress.setVisibility(View.GONE);
                AlbumInfoActivity.this.mEmpty.setVisibility(View.GONE);
                AlbumInfoActivity.this.mListView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
            AlbumInfoActivity.this.mAdapter.swapCursor(null);
        }
    }

    private void refreshAlbumList() {
        final LoaderManager loaderManager = this.getSupportLoaderManager();
        loaderManager.initLoader(AlbumFragment.ALBUM_JSON_LOADER,
                                 null,
                                 new AlbumTrackJsonLoaderCallback());
        this.mProgress.setVisibility(View.VISIBLE);
        this.mListView.setVisibility(View.VISIBLE);
        this.mEmpty.setVisibility(View.GONE);
    }

    private class AlbumTrackJsonLoaderCallback implements
            LoaderCallbacks<Boolean> {

        @Override
        public Loader<Boolean> onCreateLoader(final int loaderId,
                                              final Bundle args) {
            final Context context = AlbumInfoActivity.this;
            final Uri hostUri = HttpConstants.getHostUri(context);
            final String part = "fm/music/" + AlbumInfoActivity.this.mArticleId;

            final AlbumTrackDataLoader loader = new AlbumTrackDataLoader(context);
            loader.mUri = Uri.withAppendedPath(hostUri, part);

            return loader;
        }

        @Override
        public void onLoadFinished(final Loader<Boolean> loader,
                                   final Boolean data) {
            AlbumInfoActivity.this.getSupportLoaderManager()
                                  .getLoader(AlbumInfoActivity.ALBUM_TRACK_CURSOR_LOADER)
                                  .onContentChanged();
        }

        @Override
        public void onLoaderReset(final Loader<Boolean> loader) {
        }
    }

    private static class AlbumTrackDataLoader extends BaseLoader<Boolean> {

        static final int NO_ERROR = 0;
        static final int NETWORK_ERROR = -100;
        static final int PARSE_ERROR = -200;

        private int mErrorCode = AlbumTrackDataLoader.NO_ERROR;

        private Uri mUri = null;

        public AlbumTrackDataLoader(final Context ctx) {
            super(ctx);
        }

        @Override
        public Boolean loadInBackground() {
            this.refreshTrackData();
            return this.isNoError();
        }

        protected boolean isNoError() {
            return this.mErrorCode == AlbumTrackDataLoader.NO_ERROR;
        }

        private void refreshTrackData() {
            final Bundle param = new Bundle();
            param.putInt("s", Integer.MAX_VALUE);
            final String json = this.processHttpGet(this.mUri, param);
            if (this.isNoError() && StringUtils.isNotBlank(json)) {
                try {
                    final JSONObject trackJson = new JSONObject(json);
                    this.parseTrackJSON(trackJson);
                } catch (final JSONException e) {
                    this.mErrorCode = AlbumTrackDataLoader.PARSE_ERROR;
                }
            }
        }

        private String processHttpGet(final Uri uri, final Bundle param) {
            final RESTClient.RESTResponse response = new RESTClient(RESTClient.HTTPVerb.GET,
                                                                    uri,
                                                                    param).load();
            final int code = response.getCode();
            final String json = response.getData();
            if (code != 200) {
                this.mErrorCode = AlbumTrackDataLoader.NETWORK_ERROR;
            }
            return json;
        }

        protected void parseTrackJSON(final JSONObject responseJSON)
                throws JSONException {
            if (responseJSON.getBoolean("result")) {
                final JSONObject articleObject = responseJSON.optJSONObject("articleBean");
                final String album = articleObject.optString("title");

                final JSONArray playlist = responseJSON.optJSONArray("playlist");
                final List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                final ContentResolver contentResolver = this.getContext()
                                                            .getContentResolver();
                for (int i = 0; i < playlist.length(); i++) {
                    final JSONObject trackObject = playlist.optJSONObject(i);
                    final String articleId = trackObject.optString("articleId");
                    final String fileId = trackObject.optString("fileId");
                    final String songTitle = trackObject.optString("title");
                    final String leadArtist = trackObject.optString("artist");
                    final int trackNumber = i + 1;
                    Cursor cursor = null;
                    try {
                        cursor = contentResolver.query(TongrenluContentProvider.TRACK_URI,
                                                       null,
                                                       "articleId = ? and fileId = ?",
                                                       new String[] { articleId,
                                                                     fileId },
                                                       null);
                        if (cursor.getCount() == 0) {
                            final ContentValues contentValues = new ContentValues();
                            contentValues.put("articleId", articleId);
                            contentValues.put("fileId", fileId);
                            contentValues.put("album", album);
                            contentValues.put("songTitle", songTitle);
                            contentValues.put("leadArtist", leadArtist);
                            contentValues.put("trackNumber", trackNumber);
                            contentValuesList.add(contentValues);
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
                if (CollectionUtils.isNotEmpty(contentValuesList)) {
                    contentResolver.bulkInsert(TongrenluContentProvider.TRACK_URI,
                                               contentValuesList.toArray(new ContentValues[] {}));
                    contentResolver.notifyChange(TongrenluContentProvider.TRACK_URI,
                                                 null);
                }
            }
        }
    }

    @Override
    public void onClick(final View itemView,
                        final View clickedView,
                        final int position) {
        switch (clickedView.getId()) {
        case R.id.item:
        case R.id.action_play:
            this.playTrack(position);
            break;
        case R.id.action_download:
            this.downloadTrack(position);
            break;
        default:
            break;
        }
    }

    @Override
    public void onClick(final View v) {
        if (!this.mAdapter.isEmpty()) {
            switch (v.getId()) {
            case R.id.action_play_all:
                this.playAllTrack();
                break;
            case R.id.action_download_all:
                this.downloadAllTrack();
                break;
            default:
                break;
            }
        }
    }

    private TrackBean getTrackBean(final int position) {
        final Cursor c = (Cursor) this.mAdapter.getItem(position);
        final TrackBean trackBean = new TrackBean();
        trackBean.setArticleId(c.getString(c.getColumnIndex("articleId")));
        trackBean.setFileId(c.getString(c.getColumnIndex("fileId")));
        trackBean.setSongTitle(c.getString(c.getColumnIndex("songTitle")));
        trackBean.setLeadArtist(c.getString(c.getColumnIndex("leadArtist")));
        return trackBean;
    }

    private void playTrack(final int position) {
        final TrackBean trackBean = this.getTrackBean(position);
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_APPEND);
        serviceIntent.putExtra("trackBean", trackBean);
        this.startService(serviceIntent);
    }

    private void downloadTrack(final int position) {
        final TrackBean trackBean = this.getTrackBean(position);
        final ArrayList<TrackBean> trackBeanList = new ArrayList<TrackBean>();
        trackBeanList.add(trackBean);
        this.downloadTrack(trackBeanList);
    }

    private ArrayList<TrackBean> getTrackBeans() {
        final ArrayList<TrackBean> trackBeanList = new ArrayList<TrackBean>();
        final Cursor c = this.mAdapter.getCursor();
        if (c.moveToFirst()) {
            do {
                final TrackBean trackBean = new TrackBean();
                trackBean.setArticleId(c.getString(c.getColumnIndex("articleId")));
                trackBean.setFileId(c.getString(c.getColumnIndex("fileId")));
                trackBean.setSongTitle(c.getString(c.getColumnIndex("songTitle")));
                trackBean.setLeadArtist(c.getString(c.getColumnIndex("leadArtist")));
                trackBeanList.add(trackBean);
            } while (c.moveToNext());
        }
        return trackBeanList;
    }

    protected void playAllTrack() {
        final ArrayList<TrackBean> trackBeanList = this.getTrackBeans();
        if (CollectionUtils.isNotEmpty(trackBeanList)) {
            final Intent serviceIntent = new Intent(this, MusicService.class);
            serviceIntent.setAction(MusicService.ACTION_ADD);
            serviceIntent.putParcelableArrayListExtra("trackBeanList",
                                                      trackBeanList);
            serviceIntent.putExtra("position", 0);
            this.startService(serviceIntent);

            final Intent activityIntent = new Intent(this,
                                                     MusicPlayerActivity.class);
            this.startActivity(activityIntent);
        }
    }

    private void downloadAllTrack() {
        final ArrayList<TrackBean> trackBeanList = this.getTrackBeans();
        if (CollectionUtils.isNotEmpty(trackBeanList)) {
            this.downloadTrack(trackBeanList);
        }
    }

    protected void downloadTrack(final ArrayList<TrackBean> trackBeanList) {
        final Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.setAction(DownloadService.ACTION_ADD);
        serviceIntent.putParcelableArrayListExtra("trackBeanList",
                                                  trackBeanList);
        this.showCreatePlaylistDialog(serviceIntent);
    }

    public void showCreatePlaylistDialog(final Intent serviceIntent) {
        String title = this.mTitle;
        Cursor cursor = null;
        try {
            cursor = this.getContentResolver()
                         .query(TongrenluContentProvider.PLAYLIST_URI,
                                null,
                                "title = ?",
                                new String[] { title },
                                null);

            if (cursor.getCount() > 0) {
                title = String.format("%s (%d)", title, cursor.getCount());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        final DialogFragment dialog = new CreatePlaylistDialogFragment(title,
                                                                       serviceIntent);
        dialog.show(this.getSupportFragmentManager(), "PlaylistDialogFragment");
    }

    public class SelectPlaylistDialogFragment extends DialogFragment implements
            DialogInterface.OnClickListener {

        private Intent mIntent = null;

        public SelectPlaylistDialogFragment(final Intent serviceIntent) {
            this.mIntent = serviceIntent;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Cursor cursor = AlbumInfoActivity.this.getContentResolver()
                                                        .query(TongrenluContentProvider.PLAYLIST_URI,
                                                               null,
                                                               null,
                                                               null,
                                                               null);

            final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setTitle(R.string.dial_select_playlist)
                   .setCursor(cursor, this, "title")
                   .setPositiveButton(R.string.action_create, this)
                   .setNegativeButton(R.string.action_cancel, this);
            return builder.create();
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            System.out.println(which);
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                AlbumInfoActivity.this.showCreatePlaylistDialog(this.mIntent);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
            default:
                final AlertDialog alertDialog = (AlertDialog) dialog;
                final long playlistId = alertDialog.getListView()
                                                   .getItemIdAtPosition(which);
                this.mIntent.putExtra("playlistId", playlistId);
                AlbumInfoActivity.this.startService(this.mIntent);
                break;
            }

        }
    }

    public class CreatePlaylistDialogFragment extends DialogFragment implements
            DialogInterface.OnClickListener {

        private String mTitle = null;
        private Intent mIntent = null;
        private EditText mTitleView = null;

        public CreatePlaylistDialogFragment(final String title,
                final Intent serviceIntent) {
            this.mTitle = title;
            this.mIntent = serviceIntent;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final LayoutInflater inflater = this.getActivity()
                                                .getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_create_playlist,
                                               null);
            this.mTitleView = (EditText) view.findViewById(R.id.playlist_title);
            this.mTitleView.setText(this.mTitle);

            final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setTitle(R.string.dial_create_playlist)
                   .setView(view)
                   .setPositiveButton(R.string.action_create, this)
                   .setNegativeButton(R.string.action_cancel, this);
            return builder.create();
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            System.out.println(which);
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                final ContentValues values = new ContentValues();
                values.put("title", this.mTitleView.getText().toString());
                final ContentResolver contentResolver = this.getActivity()
                                                            .getContentResolver();
                final Uri uri = contentResolver.insert(TongrenluContentProvider.PLAYLIST_URI,
                                                       values);
                contentResolver.notifyChange(TongrenluContentProvider.PLAYLIST_URI,
                                             null);

                final long playlistId = ContentUris.parseId(uri);
                this.mIntent.putExtra("playlistId", playlistId);
                AlbumInfoActivity.this.startService(this.mIntent);
                break;
            default:

                break;
            }

        }
    }

}
