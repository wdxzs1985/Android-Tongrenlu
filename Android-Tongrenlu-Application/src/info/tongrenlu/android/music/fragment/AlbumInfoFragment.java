package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.image.LoadBlurImageTask;
import info.tongrenlu.android.image.LoadImageTask;
import info.tongrenlu.android.loader.BaseLoader;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.TongrenluApplication;
import info.tongrenlu.android.music.adapter.AlbumTrackListAdapter;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.android.provider.HttpHelper;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;
import info.tongrenlu.domain.TrackBean;
import info.tongrenlu.support.ApplicationSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.bitmapcache.BitmapLruCache;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
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
    private final ArticleBean mArticleBean;

    private AlbumInfoFragmentListener mListener = null;

    private final int albumTrackCursorLoaderId;
    private final int albumTrackJsonLoaderId;

    public AlbumInfoFragment(final ArticleBean articleBean) {
        this.mArticleBean = articleBean;
        this.mUri = Uri.withAppendedPath(TongrenluContentProvider.ALBUM_URI,
                                         this.mArticleBean.getArticleId());
        this.mTrackUri = Uri.withAppendedPath(this.mUri, "track");

        final long articleId = Long.valueOf(this.mArticleBean.getArticleId());
        this.albumTrackCursorLoaderId = (int) (articleId * 10 + AlbumInfoFragment.ALBUM_TRACK_CURSOR_LOADER);
        this.albumTrackJsonLoaderId = (int) (articleId * 10 + AlbumInfoFragment.ALBUM_TRACK_JSON_LOADER);
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (activity instanceof AlbumInfoFragmentListener) {
            this.mListener = (AlbumInfoFragmentListener) activity;
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
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
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_album_info,
                                           container,
                                           false);

        final TextView articleTitle = (TextView) view.findViewById(R.id.article_title);
        articleTitle.setText(this.mArticleBean.getTitle());

        this.mAdapter = new AlbumTrackListAdapter(this.getActivity());

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
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final TongrenluApplication application = (TongrenluApplication) this.getActivity()
                                                                            .getApplication();
        final BitmapLruCache bitmapCache = application.getBitmapCache();
        final HttpHelper http = application.getHttpHelper();

        final String articleId = this.mArticleBean.getArticleId();
        String url;
        switch (application.getResources().getDisplayMetrics().densityDpi) {
        case DisplayMetrics.DENSITY_XXXHIGH:
        case DisplayMetrics.DENSITY_XXHIGH:
        case DisplayMetrics.DENSITY_XHIGH:
        case DisplayMetrics.DENSITY_HIGH:
        case DisplayMetrics.DENSITY_TV:
            url = HttpConstants.getCoverUrl(application,
                                            articleId,
                                            HttpConstants.L_COVER);
            break;
        default:
            url = HttpConstants.getCoverUrl(application,
                                            articleId,
                                            HttpConstants.S_COVER);
            break;
        }

        final ImageView coverView = (ImageView) view.findViewById(R.id.article_cover);
        coverView.setImageDrawable(null);
        new LoadImageTask() {

            @Override
            protected Drawable doInBackground(final Object... params) {
                Drawable result = super.doInBackground(params);
                if (result == null) {
                    result = AlbumInfoFragment.this.getResources()
                                                   .getDrawable(R.drawable.default_cover);
                }
                return result;
            }

            @Override
            protected void onPostExecute(final Drawable result) {
                super.onPostExecute(result);
                if (!this.isCancelled() && result != null) {
                    if (ApplicationSupport.canUseLargeHeap()) {
                        final Drawable emptyDrawable = new ShapeDrawable();
                        final TransitionDrawable fadeInDrawable = new TransitionDrawable(new Drawable[] { emptyDrawable,
                                result });
                        coverView.setImageDrawable(fadeInDrawable);
                        fadeInDrawable.startTransition(LoadImageTask.TIME_SHORT);
                    } else {
                        coverView.setImageDrawable(result);
                    }
                }
            }

        }.execute(bitmapCache, url, http);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        if (sharedPreferences.getBoolean(SettingFragment.PREF_KEY_BACKGROUND_RENDER,
                                         ApplicationSupport.canUseRenderScript())) {
            String backgroundUrl = null;
            if (ApplicationSupport.canUseLargeHeap()) {
                backgroundUrl = HttpConstants.getCoverUrl(application,
                                                          articleId,
                                                          HttpConstants.L_COVER);
            } else {
                backgroundUrl = HttpConstants.getCoverUrl(application,
                                                          articleId,
                                                          HttpConstants.M_COVER);
            }
            new LoadBlurImageTask() {

                @SuppressWarnings("deprecation")
                @Override
                protected void onPostExecute(final Drawable result) {
                    super.onPostExecute(result);
                    if (!this.isCancelled() && result != null) {
                        if (ApplicationSupport.canUseViewBackground()) {
                            view.setBackground(result);
                        } else {
                            view.setBackgroundDrawable(result);
                        }
                    }
                }

            }.execute(bitmapCache, backgroundUrl, http, application);
        }
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
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
        final FragmentActivity activity = this.getActivity();
        activity.getContentResolver()
                .unregisterContentObserver(this.contentObserver);
        activity.getSupportLoaderManager()
                .destroyLoader(this.albumTrackCursorLoaderId);
        activity.getSupportLoaderManager()
                .destroyLoader(this.albumTrackJsonLoaderId);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
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
                AlbumInfoFragment.this.refreshAlbumTracks();
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

    private void refreshAlbumTracks() {
        final LoaderManager loaderManager = this.getActivity()
                                                .getSupportLoaderManager();
        loaderManager.initLoader(this.albumTrackJsonLoaderId,
                                 null,
                                 new AlbumTrackJsonLoaderCallback());
        this.mProgressContainer.setVisibility(View.VISIBLE);
    }

    private class AlbumTrackJsonLoaderCallback implements LoaderCallbacks<Boolean> {

        @Override
        public Loader<Boolean> onCreateLoader(final int loaderId,
                                              final Bundle args) {
            final TongrenluApplication application = (TongrenluApplication) AlbumInfoFragment.this.getActivity()
                                                                                                  .getApplication();

            final HttpHelper http = application.getHttpHelper();

            final String host = HttpConstants.getHostServer(application);
            final String part = "/fm/music/" + AlbumInfoFragment.this.mArticleBean.getArticleId();
            final String url = host + part;

            return new AlbumTrackDataLoader(application, http, url);
        }

        @Override
        public void onLoadFinished(final Loader<Boolean> loader,
                                   final Boolean noError) {

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

        private int mErrorCode = AlbumTrackDataLoader.NO_ERROR;

        private final HttpHelper http;
        private final String url;

        public AlbumTrackDataLoader(final Context ctx, final HttpHelper http, final String url) {
            super(ctx);
            this.http = http;
            this.url = url;
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
            try {
                final JSONObject responseJSON = this.http.getAsJson(this.url);
                this.parseTrackJSON(responseJSON);
            } catch (final JSONException e) {
                this.mErrorCode = AlbumTrackDataLoader.PARSE_ERROR;
                e.printStackTrace();
            } catch (final IOException e) {
                this.mErrorCode = AlbumTrackDataLoader.NETWORK_ERROR;
                e.printStackTrace();
            }
        }

        protected void parseTrackJSON(final JSONObject responseJSON) throws JSONException {
            final JSONObject articleObject = responseJSON.optJSONObject("music");
            final String articleId = articleObject.optString("id");
            final String album = articleObject.optString("title");

            final JSONArray playlist = responseJSON.optJSONArray("trackList");
            final List<ContentValues> contentValuesList = new ArrayList<ContentValues>();
            final ContentResolver contentResolver = this.getContext()
                                                        .getContentResolver();
            contentResolver.delete(TongrenluContentProvider.TRACK_URI,
                                   "articleId = ?",
                                   new String[] { articleId });
            for (int i = 0; i < playlist.length(); i++) {
                final JSONObject trackObject = playlist.optJSONObject(i);
                final ContentValues contentValues = new ContentValues();
                contentValues.put("articleId", articleId);
                contentValues.put("album", album);
                contentValues.put("fileId", trackObject.optString("id"));
                contentValues.put("name", trackObject.optString("name"));
                contentValues.put("artist", trackObject.optString("artist"));
                contentValues.put("original", trackObject.optString("original"));
                contentValues.put("trackNumber", i + 1);
                contentValuesList.add(contentValues);
            }
            if (CollectionUtils.isNotEmpty(contentValuesList)) {
                contentResolver.bulkInsert(TongrenluContentProvider.TRACK_URI,
                                           contentValuesList.toArray(new ContentValues[] {}));
                contentResolver.notifyChange(TongrenluContentProvider.TRACK_URI,
                                             null);
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
            this.mListener.onPlay(this.getTrackBeans(), position);
            break;
        case R.id.action_download:
            this.mListener.onDownload(this.mArticleBean.getTitle(),
                                      this.getTrackBean(position));
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
                this.mListener.onPlay(this.getTrackBeans(), 0);
                break;
            case R.id.action_download_all:
                this.mListener.onDownloadAll(this.mArticleBean.getTitle(),
                                             this.getTrackBeans());
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
        trackBean.setName(c.getString(c.getColumnIndex("name")));
        trackBean.setArtist(c.getString(c.getColumnIndex("artist")));
        trackBean.setTrackNumber(0);
        // trackBean.setDownloadFlg(c.getColumnIndex("downloadFlg"));
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
                trackBean.setName(c.getString(c.getColumnIndex("name")));
                trackBean.setArtist(c.getString(c.getColumnIndex("artist")));
                trackBean.setTrackNumber(c.getInt(c.getColumnIndex("trackNumber")));
                // trackBean.setDownloadFlg(c.getInt(c.getColumnIndex("downloadFlg")));
                trackBeanList.add(trackBean);
            } while (c.moveToNext());
        }
        return trackBeanList;
    }

    public interface AlbumInfoFragmentListener {

        void onPlay(ArrayList<TrackBean> trackBeanList, int position);

        void onDownload(String title, TrackBean trackBean);

        void onDownloadAll(String title, ArrayList<TrackBean> trackBeanList);

    }

}
