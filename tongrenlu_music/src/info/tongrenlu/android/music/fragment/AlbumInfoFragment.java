package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.loader.BaseLoader;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.music.adapter.AlbumTrackListAdapter;
import info.tongrenlu.android.music.async.LoadBlurImageTask;
import info.tongrenlu.android.music.async.LoadImageTask;
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
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class AlbumInfoFragment extends Fragment implements ActionSlideExpandableListView.OnActionClickListener, OnClickListener {

    public static final int ALBUM_TRACK_CURSOR_LOADER = 1;
    public static final int ALBUM_TRACK_JSON_LOADER = 2;

    private View mProgressContainer = null;
    private View mEmpty = null;
    private ActionSlideExpandableListView mListView = null;
    private AlbumTrackListAdapter mAdapter = null;

    private ContentObserver contentObserver = null;

    private final Uri mUri;
    private final Uri mTrackUri;
    private final String mArticleId;
    private final String mTitle;

    private AlbumInfoFragmentListener mListener = null;

    private final int albumTrackCursorLoaderId;
    private final int albumTrackJsonLoaderId;

    public AlbumInfoFragment(String articleId, String title, long id) {
        this.mArticleId = articleId;
        this.mTitle = title;
        this.mUri = Uri.withAppendedPath(TongrenluContentProvider.ALBUM_URI,
                                         this.mArticleId);
        this.mTrackUri = Uri.withAppendedPath(this.mUri, "track");
        this.albumTrackCursorLoaderId = (int) (id * 10 + ALBUM_TRACK_CURSOR_LOADER);
        this.albumTrackJsonLoaderId = (int) (id * 10 + ALBUM_TRACK_JSON_LOADER);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof AlbumInfoFragmentListener) {
            this.mListener = (AlbumInfoFragmentListener) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentActivity activity = this.getActivity();
        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                activity.getSupportLoaderManager()
                        .getLoader(AlbumInfoFragment.this.albumTrackCursorLoaderId)
                        .onContentChanged();
            }
        };

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_album_info,
                                           null,
                                           false);

        final TongrenluApplication application = (TongrenluApplication) this.getActivity()
                                                                            .getApplication();
        final BitmapLruCache bitmapCache = application.getBitmapCache();
        final String url = HttpConstants.getCoverUrl(application,
                                                     this.mArticleId,
                                                     HttpConstants.L_COVER);
        final ImageView coverView = (ImageView) view.findViewById(R.id.article_cover);
        coverView.setImageDrawable(null);
        new LoadImageTask() {

            @Override
            protected Drawable doInBackground(Object... params) {
                Drawable result = super.doInBackground(params);
                if (result == null) {
                    result = AlbumInfoFragment.this.getResources()
                                                   .getDrawable(R.drawable.default_120);
                }
                return result;
            }

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

        new LoadBlurImageTask() {

            @Override
            protected void onPostExecute(final Drawable result) {
                super.onPostExecute(result);
                if (!this.isCancelled() && result != null) {
                    view.setBackground(result);
                }
            }

        }.execute(bitmapCache, url, application);

        final TextView articleTitle = (TextView) view.findViewById(R.id.article_title);
        articleTitle.setText(this.mTitle);

        this.mAdapter = new AlbumTrackListAdapter(this.getActivity(), null);

        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mEmpty.setVisibility(View.GONE);

        this.mListView = (ActionSlideExpandableListView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setItemActionListener(this,
                                             R.id.item,
                                             R.id.action_play,
                                             R.id.action_download);
        this.mListView.setVisibility(View.GONE);

        this.mProgressContainer = view.findViewById(R.id.progressContainer);

        final Button playAllButton = (Button) view.findViewById(R.id.action_play_all);
        playAllButton.setOnClickListener(this);
        final Button downloadAllButton = (Button) view.findViewById(R.id.action_download_all);
        downloadAllButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final FragmentActivity activity = this.getActivity();

        this.mProgressContainer.setVisibility(View.VISIBLE);

        activity.getSupportLoaderManager()
                .initLoader(this.albumTrackCursorLoaderId,
                            null,
                            new AlbumTrackCursorLoaderCallback());

        activity.getContentResolver()
                .registerContentObserver(this.mTrackUri,
                                         true,
                                         this.contentObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.mListener = null;

        final FragmentActivity activity = this.getActivity();
        activity.getContentResolver()
                .unregisterContentObserver(this.contentObserver);
        activity.getSupportLoaderManager()
                .destroyLoader(this.albumTrackCursorLoaderId);
        activity.getSupportLoaderManager()
                .destroyLoader(this.albumTrackJsonLoaderId);
    }

    class AlbumTrackCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
            final CursorLoader loader = new CursorLoader(AlbumInfoFragment.this.getActivity());
            loader.setUri(AlbumInfoFragment.this.mTrackUri);
            return loader;
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
            AlbumInfoFragment.this.mAdapter.swapCursor(c);
            AlbumInfoFragment.this.mProgressContainer.setVisibility(View.GONE);
            if (AlbumInfoFragment.this.mAdapter.isEmpty()) {
                AlbumInfoFragment.this.refreshAlbumList();
            } else {
                AlbumInfoFragment.this.mEmpty.setVisibility(View.GONE);
                AlbumInfoFragment.this.mListView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
            AlbumInfoFragment.this.mAdapter.swapCursor(null);
        }
    }

    private void refreshAlbumList() {
        final LoaderManager loaderManager = this.getActivity()
                                                .getSupportLoaderManager();
        loaderManager.initLoader(this.albumTrackJsonLoaderId,
                                 null,
                                 new AlbumTrackJsonLoaderCallback());
        this.mProgressContainer.setVisibility(View.VISIBLE);
    }

    private class AlbumTrackJsonLoaderCallback implements LoaderCallbacks<Boolean> {

        @Override
        public Loader<Boolean> onCreateLoader(final int loaderId, final Bundle args) {
            Context context = AlbumInfoFragment.this.getActivity();

            AlbumTrackDataLoader loader = new AlbumTrackDataLoader(context);

            Uri hostUri = HttpConstants.getHostUri(context);
            String part = "fm/music/" + AlbumInfoFragment.this.mArticleId;
            loader.mUri = Uri.withAppendedPath(hostUri, part);

            return loader;
        }

        @Override
        public void onLoadFinished(final Loader<Boolean> loader, final Boolean noError) {

            if (noError) {
                AlbumInfoFragment.this.getActivity()
                                      .getSupportLoaderManager()
                                      .getLoader(AlbumInfoFragment.this.albumTrackCursorLoaderId)
                                      .onContentChanged();
            } else {
                AlbumInfoFragment.this.mProgressContainer.setVisibility(View.GONE);
                Toast.makeText(AlbumInfoFragment.this.getActivity(),
                               R.string.err_network,
                               Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onLoaderReset(final Loader<Boolean> loader) {
        }
    }

    private static class AlbumTrackDataLoader extends BaseLoader<Boolean> {

        static final int NO_ERROR = 0;
        static final int NETWORK_ERROR = -100;
        static final int PARSE_ERROR = -200;

        private int mErrorCode = NO_ERROR;

        private Uri mUri = null;

        public AlbumTrackDataLoader(Context ctx) {
            super(ctx);
        }

        @Override
        public Boolean loadInBackground() {
            this.refreshTrackData();
            return this.isNoError();
        }

        protected boolean isNoError() {
            return this.mErrorCode == NO_ERROR;
        }

        private void refreshTrackData() {
            Bundle param = new Bundle();
            param.putInt("s", Integer.MAX_VALUE);
            String json = this.processHttpGet(this.mUri, param);
            if (this.isNoError() && StringUtils.isNotBlank(json)) {
                try {
                    JSONObject trackJson = new JSONObject(json);
                    this.parseTrackJSON(trackJson);
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

        protected void parseTrackJSON(final JSONObject responseJSON) throws JSONException {
            if (responseJSON.getBoolean("result")) {
                final JSONObject articleObject = responseJSON.optJSONObject("articleBean");
                String album = articleObject.optString("title");

                final JSONArray playlist = responseJSON.optJSONArray("playlist");
                List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
                final ContentResolver contentResolver = this.getContext()
                                                            .getContentResolver();
                for (int i = 0; i < playlist.length(); i++) {
                    final JSONObject trackObject = playlist.optJSONObject(i);
                    final String articleId = trackObject.optString("articleId");
                    final String fileId = trackObject.optString("fileId");
                    final String songTitle = trackObject.optString("title");
                    final String leadArtist = trackObject.optString("artist");
                    final String original = trackObject.optString("original");
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
                            contentValues.put("original", original);
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
    public void onClick(final View itemView, final View clickedView, final int position) {
        switch (clickedView.getId()) {
        case R.id.item:
        case R.id.action_play:
            this.mListener.onPlay(this.getTrackBean(position));
            break;
        case R.id.action_download:
            this.mListener.onDownload(this.mTitle, this.getTrackBean(position));
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
                this.mListener.onPlayAll(this.getTrackBeans());
                break;
            case R.id.action_download_all:
                this.mListener.onDownloadAll(this.mTitle, this.getTrackBeans());
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
        trackBean.setTrackNumber(0);
        trackBean.setDownloadFlg(c.getInt(c.getColumnIndex("downloadFlg")));
        return trackBean;
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
                trackBean.setTrackNumber(c.getInt(c.getColumnIndex("trackNumber")));
                trackBean.setDownloadFlg(c.getInt(c.getColumnIndex("downloadFlg")));
                trackBeanList.add(trackBean);
            } while (c.moveToNext());
        }
        return trackBeanList;
    }

    public interface AlbumInfoFragmentListener {

        void onPlay(TrackBean trackBean);

        void onDownload(String title, TrackBean trackBean);

        void onPlayAll(ArrayList<TrackBean> trackBeanList);

        void onDownloadAll(String title, ArrayList<TrackBean> trackBeanList);

    }

}
