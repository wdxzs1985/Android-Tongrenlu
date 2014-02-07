package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.fragment.TitleFragment;
import info.tongrenlu.android.loader.JSONLoader;
import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.MusicGridAdapter;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;
import info.tongrenlu.domain.MusicBean;
import info.tongrenlu.support.PaginateSupport;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class MusicGridFragment extends TitleFragment implements OnScrollListener, OnItemClickListener {

    public static final int ALBUM_LIST_LOADER = 2;

    private OnArticleSelectedListener mListener;

    private View mProgress = null;
    private View mEmpty = null;
    private GridView mListView = null;
    private MusicGridAdapter mAdapter = null;

    private final String mQuery = "";
    private int mPage = 0;
    private boolean mLast = false;

    public MusicGridFragment() {
        this.setTitle("所有专辑");
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnArticleSelectedListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAdapter = new MusicGridAdapter();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_grid_view,
                                           null,
                                           false);
        this.mProgress = view.findViewById(android.R.id.progress);
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mListView = (GridView) view.findViewById(android.R.id.list);
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnScrollListener(this);
        this.mListView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.loadNextPage();

    }

    protected void loadNextPage() {
        if (!this.mLast) {
            this.mProgress.setVisibility(View.VISIBLE);
            this.mListView.setVisibility(View.VISIBLE);
            this.mEmpty.setVisibility(View.GONE);

            final Bundle parameters = new Bundle();
            parameters.putString("q", this.mQuery);
            parameters.putString("p",
                                 String.valueOf(MusicGridFragment.this.mPage + 1));
            parameters.putString("s", String.valueOf(HttpConstants.PAGE_SIZE));
            this.getActivity()
                .getSupportLoaderManager()
                .restartLoader(ALBUM_LIST_LOADER,
                               parameters,
                               new MusicListLoaderCallback());
        }
    }

    @Override
    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {

    }

    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        switch (scrollState) {
        case SCROLL_STATE_IDLE:
            this.mAdapter.setScrolling(false);
            // ListViewの表示するべきデータ位置を取得
            final int first = view.getFirstVisiblePosition();
            final int count = view.getChildCount();
            final int totalItemCount = view.getCount();

            if (first + count == totalItemCount && !this.mLast) {
                this.loadNextPage();
            } else {
                this.mAdapter.notifyDataSetChanged();
            }
            break;
        case SCROLL_STATE_FLING:
            this.mAdapter.setScrolling(true);
            break;
        case SCROLL_STATE_TOUCH_SCROLL:
            this.mAdapter.setScrolling(true);
            break;
        default:

            break;
        }

    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
        final ArticleBean articleBean = (ArticleBean) listView.getItemAtPosition(position);
        this.mListener.onArticleSelected(articleBean);

    }

    public interface OnArticleSelectedListener {
        public void onArticleSelected(ArticleBean articleBean);
    }

    private class MusicListLoaderCallback implements LoaderCallbacks<PaginateSupport> {

        @Override
        public Loader<PaginateSupport> onCreateLoader(int loaderId, Bundle args) {
            Context context = MusicGridFragment.this.getActivity();
            final Uri uri = HttpConstants.getMusicListUri(context);
            return new MusicListLoader(context, uri, args);
        }

        @Override
        public void onLoadFinished(Loader<PaginateSupport> loader, PaginateSupport data) {
            if (data == null || data.getItemCount() == 0) {
                MusicGridFragment.this.mProgress.setVisibility(View.GONE);
                MusicGridFragment.this.mListView.setVisibility(View.GONE);
                MusicGridFragment.this.mEmpty.setVisibility(View.VISIBLE);
            } else {
                MusicGridFragment.this.mProgress.setVisibility(View.GONE);
                MusicGridFragment.this.mEmpty.setVisibility(View.GONE);
                MusicGridFragment.this.mListView.setVisibility(View.VISIBLE);

                List<ArticleBean> items = MusicGridFragment.this.mAdapter.getItems();
                for (Object articleBean : data.getItems()) {
                    items.add((ArticleBean) articleBean);
                }

                MusicGridFragment.this.mAdapter.notifyDataSetChanged();

                MusicGridFragment.this.mPage = data.getPage();
                MusicGridFragment.this.mLast = data.isLast();
            }
        }

        @Override
        public void onLoaderReset(Loader<PaginateSupport> loader) {
            MusicGridFragment.this.mAdapter.setItems(new ArrayList<ArticleBean>());
        }
    }

    private static class MusicListLoader extends JSONLoader<PaginateSupport> {

        public MusicListLoader(Context ctx, Uri uri, Bundle parameters) {
            super(ctx, uri, parameters);
        }

        @Override
        protected PaginateSupport parseJSON(final JSONObject responseJSON) throws JSONException {
            PaginateSupport paginate = new PaginateSupport();
            List<MusicBean> itemList = new ArrayList<MusicBean>();
            if (responseJSON.getBoolean("result")) {
                final JSONObject pageJSON = responseJSON.getJSONObject("page");
                final int itemCount = pageJSON.getInt("itemCount");
                final int page = pageJSON.getInt("page");
                final int size = pageJSON.getInt("size");
                final JSONArray items = pageJSON.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject musicJsonObject = items.getJSONObject(i);
                    final MusicBean musicBean = new MusicBean();
                    if (musicJsonObject.has("articleId")) {
                        musicBean.setArticleId(musicJsonObject.getString("articleId"));
                    }
                    if (musicJsonObject.has("title")) {
                        musicBean.setTitle(musicJsonObject.getString("title"));
                    }
                    itemList.add(musicBean);
                }

                paginate.setItemCount(itemCount);
                paginate.setPage(page);
                paginate.setSize(size);
                paginate.setItems(itemList);
            }
            return paginate;
        }

    }
}
