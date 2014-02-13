package info.tongrenlu.android.music;

import info.tongrenlu.android.loader.JSONLoader;
import info.tongrenlu.android.music.adapter.MusicTrackListAdapter;
import info.tongrenlu.android.music.async.LoadImageCacheTask;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;
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
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class AlbumInfoActivity extends BaseActivity implements ActionSlideExpandableListView.OnActionClickListener, OnClickListener {

    public static final int ALBUM_INFO_LOADER = 1;

    private View mProgress = null;
    private View mEmpty = null;
    private ActionSlideExpandableListView mListView = null;
    private MusicTrackListAdapter mAdapter = null;

    private String mArticleId = null;
    private String mTitle = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_music_info);

        final Intent intent = this.getIntent();
        this.mArticleId = intent.getStringExtra("articleId");
        this.mTitle = intent.getStringExtra("title");

        this.initArticleCover(this.mArticleId);
        this.initAritcleTitle(this.mTitle);

        this.mAdapter = new MusicTrackListAdapter();

        this.mProgress = this.findViewById(android.R.id.progress);
        this.mEmpty = this.findViewById(android.R.id.empty);
        this.mListView = (ActionSlideExpandableListView) this.findViewById(android.R.id.list);
        //
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setItemActionListener(this,
                                             R.id.item,
                                             R.id.action_play,
                                             R.id.action_download);

        final Button playAllButton = (Button) this.findViewById(R.id.action_play_all);
        playAllButton.setOnClickListener(this);
        final Button downloadAllButton = (Button) this.findViewById(R.id.action_download_all);
        downloadAllButton.setOnClickListener(this);

        final Bundle bundle = new Bundle();
        bundle.putString("articleId", this.mArticleId);

        this.mProgress.setVisibility(View.VISIBLE);
        this.mListView.setVisibility(View.GONE);
        this.mEmpty.setVisibility(View.GONE);
        this.getSupportLoaderManager()
            .initLoader(AlbumInfoActivity.ALBUM_INFO_LOADER,
                        bundle,
                        new TrackListLoaderCallback());
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
            .destroyLoader(AlbumInfoActivity.ALBUM_INFO_LOADER);
    }

    private void initAritcleTitle(final String title) {
        final TextView articleTitle = (TextView) this.findViewById(R.id.article_title);
        articleTitle.setText(title);
    }

    @Override
    public void onClick(final View itemView, final View clickedView, final int position) {
        final TrackBean trackBean = (TrackBean) this.mListView.getItemAtPosition(position);
        switch (clickedView.getId()) {
        case R.id.item:
        case R.id.action_play:
            this.appendTrack(trackBean);
            break;
        case R.id.action_download:
            this.downloadTrack(trackBean);
            break;
        default:
            break;
        }
    }

    @Override
    public void onClick(final View v) {
        if (!this.mAdapter.isEmpty()) {
            final ArrayList<TrackBean> items = this.mAdapter.getItems();
            switch (v.getId()) {
            case R.id.action_play_all:
                this.playTrack(items, 0);
                break;
            case R.id.action_download_all:
                this.downloadTrack(items);
                break;
            default:
                break;
            }
        }
    }

    protected void appendTrack(final TrackBean trackBean) {
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_APPEND);
        serviceIntent.putExtra("trackBean", trackBean);
        this.startService(serviceIntent);
    }

    protected void playTrack(final ArrayList<TrackBean> items, final int position) {
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_ADD);
        serviceIntent.putParcelableArrayListExtra("trackBeanList", items);
        serviceIntent.putExtra("position", position);
        this.startService(serviceIntent);

        final Intent activityIntent = new Intent(this,
                                                 MusicPlayerActivity.class);
        this.startActivity(activityIntent);
    }

    protected void downloadTrack(final TrackBean item) {
        final Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.setAction(DownloadService.ACTION_ADD);
        serviceIntent.putExtra("trackBean", item);
        Cursor cursor = null;
        try {
            cursor = this.getContentResolver()
                         .query(TongrenluContentProvider.PLAYLIST_URI,
                                null,
                                null,
                                null,
                                null);

            if (cursor.getCount() > 0) {
                final DialogFragment dialog = new SelectPlaylistDialogFragment(serviceIntent);
                dialog.show(this.getSupportFragmentManager(),
                            "PlaylistDialogFragment");
            } else {
                this.showCreatePlaylistDialog(serviceIntent);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected void downloadTrack(final ArrayList<TrackBean> items) {
        final Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.setAction(DownloadService.ACTION_ADD);
        serviceIntent.putParcelableArrayListExtra("trackBeanList", items);

        this.showCreatePlaylistDialog(serviceIntent);
    }

    private class TrackListLoaderCallback implements LoaderCallbacks<ArrayList<TrackBean>> {
        @Override
        public Loader<ArrayList<TrackBean>> onCreateLoader(final int loaderId, final Bundle args) {
            switch (loaderId) {
            case ALBUM_INFO_LOADER:
                final String articleId = args.getString("articleId");
                final Uri uri = HttpConstants.getMusicInfoUri(AlbumInfoActivity.this,
                                                              articleId);
                return new TrackListLoader(AlbumInfoActivity.this, uri);
            default:
                break;
            }
            return null;
        }

        @Override
        public void onLoadFinished(final Loader<ArrayList<TrackBean>> loader, final ArrayList<TrackBean> data) {
            if (CollectionUtils.isEmpty(data)) {
                AlbumInfoActivity.this.mProgress.setVisibility(View.GONE);
                AlbumInfoActivity.this.mListView.setVisibility(View.GONE);
                AlbumInfoActivity.this.mEmpty.setVisibility(View.VISIBLE);
            } else {
                AlbumInfoActivity.this.mProgress.setVisibility(View.GONE);
                AlbumInfoActivity.this.mEmpty.setVisibility(View.GONE);
                AlbumInfoActivity.this.mListView.setVisibility(View.VISIBLE);
                AlbumInfoActivity.this.mAdapter.setItems(data);
                AlbumInfoActivity.this.mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoaderReset(final Loader<ArrayList<TrackBean>> loader) {
            AlbumInfoActivity.this.mAdapter.setItems(new ArrayList<TrackBean>());
        }
    }

    private static class TrackListLoader extends JSONLoader<ArrayList<TrackBean>> {

        public TrackListLoader(final Context ctx, final Uri uri) {
            super(ctx, uri, null);
        }

        @Override
        protected ArrayList<TrackBean> parseJSON(final JSONObject jsonData) throws JSONException {
            final ArrayList<TrackBean> data = new ArrayList<TrackBean>();
            if (jsonData.getBoolean("result")) {
                final JSONArray items = jsonData.getJSONArray("playlist");
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject trackJSON = items.getJSONObject(i);
                    final TrackBean trackBean = new TrackBean();
                    trackBean.setArticleId(trackJSON.getString("articleId"));
                    trackBean.setFileId(trackJSON.getString("fileId"));
                    trackBean.setTitle(trackJSON.getString("title"));
                    if (trackJSON.has("artist")) {
                        trackBean.setArtist(trackJSON.getString("artist"));
                    }
                    data.add(trackBean);
                }
            }
            return data;
        }
    }

    public void showSelectPlaylistDialog() {

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

    public class SelectPlaylistDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

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
                   .setNeutralButton(R.string.action_create, this)
                   .setNegativeButton(R.string.action_cancel, this);
            return builder.create();
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            System.out.println(which);
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                AlbumInfoActivity.this.showCreatePlaylistDialog(this.mIntent);
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

    public class CreatePlaylistDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

        private String mTitle = null;
        private Intent mIntent = null;
        private EditText mTitleView = null;

        public CreatePlaylistDialogFragment(final String title, final Intent serviceIntent) {
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
                final ContentResolver cResolver = this.getActivity()
                                                      .getContentResolver();
                final Uri uri = cResolver.insert(TongrenluContentProvider.PLAYLIST_URI,
                                                 values);
                cResolver.notifyChange(TongrenluContentProvider.PLAYLIST_URI,
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
