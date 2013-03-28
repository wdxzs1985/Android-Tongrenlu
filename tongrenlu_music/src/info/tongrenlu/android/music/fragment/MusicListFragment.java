package info.tongrenlu.android.music.fragment;

import info.tongrenlu.android.music.R;
import info.tongrenlu.android.music.adapter.ArticleListAdapter;
import info.tongrenlu.android.task.JSONLoadTask;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;
import info.tongrenlu.domain.MusicBean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

import com.viewpagerindicator.sample.TitleFragment;

public class MusicListFragment extends TitleFragment implements
        OnScrollListener, OnItemClickListener {
    private OnArticleSelectedListener mListener;

    private View mProgress = null;
    private View mEmpty = null;
    private GridView mListView = null;
    private ArticleListAdapter mAdapter = null;

    private String mQuery = "";
    private int mPage = 0;
    private boolean mLast = false;

    private MusicListLoadTask runningTask = null;

    public MusicListFragment() {
        this.setTitle("所有专辑");
        this.mAdapter = new ArticleListAdapter();
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnArticleSelectedListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString()
                                         + " must implement OnArticleSelectedListener");
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_grid_view,
                                           null,
                                           false);
        this.mProgress = view.findViewById(android.R.id.progress);
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mListView = (GridView) view.findViewById(android.R.id.list);
        //
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
        if (this.runningTask == null && !this.mLast) {
            final Uri uri = HttpConstants.getMusicListUri(this.getActivity());
            final Bundle parameters = new Bundle();
            parameters.putString("q", this.mQuery);
            parameters.putString("p",
                                 String.valueOf(MusicListFragment.this.mPage + 1));
            parameters.putString("s", String.valueOf(HttpConstants.PAGE_SIZE));
            new MusicListLoadTask().execute(uri, parameters);
        }
    }

    class MusicListLoadTask extends JSONLoadTask {

        @Override
        protected void onStart() {
            MusicListFragment.this.runningTask = this;
            MusicListFragment.this.mProgress.setVisibility(View.VISIBLE);
            MusicListFragment.this.mListView.setVisibility(View.VISIBLE);
            MusicListFragment.this.mEmpty.setVisibility(View.GONE);
        }

        @Override
        protected void processResponseJSON(final JSONObject responseJSON)
                throws JSONException {
            if (responseJSON.getBoolean("result")) {
                final JSONObject pageJSON = responseJSON.getJSONObject("page");
                final int page = pageJSON.getInt("page");
                MusicListFragment.this.mLast = pageJSON.getBoolean("last");
                if (MusicListFragment.this.mPage < page) {
                    MusicListFragment.this.mPage = page;

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
                        // musicBean.setDescription(musicJsonObject.getString("description"));
                        MusicListFragment.this.mAdapter.addData(musicBean);
                    }
                }
            } else {
                Toast.makeText(MusicListFragment.this.getActivity(),
                               responseJSON.getString("error"),
                               Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onJSONException(final JSONException e) {
            super.onJSONException(e);
            Toast.makeText(MusicListFragment.this.getActivity(),
                           "数据解析失败",
                           Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onNetworkError(final int code) {
            Toast.makeText(MusicListFragment.this.getActivity(),
                           "网络连接错误",
                           Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onFinish() {
            if (MusicListFragment.this.mAdapter.isEmpty()) {
                MusicListFragment.this.mProgress.setVisibility(View.GONE);
                MusicListFragment.this.mListView.setVisibility(View.GONE);
                MusicListFragment.this.mEmpty.setVisibility(View.VISIBLE);
            } else {
                MusicListFragment.this.mProgress.setVisibility(View.GONE);
                MusicListFragment.this.mEmpty.setVisibility(View.GONE);
                MusicListFragment.this.mListView.setVisibility(View.VISIBLE);
                MusicListFragment.this.mAdapter.notifyDataSetChanged();
            }
            MusicListFragment.this.runningTask = null;
        }

    }

    @Override
    public void onScroll(final AbsListView view,
                         final int firstVisibleItem,
                         final int visibleItemCount,
                         final int totalItemCount) {

    }

    @Override
    public void onScrollStateChanged(final AbsListView view,
                                     final int scrollState) {
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
    public void onItemClick(final AdapterView<?> listView,
                            final View itemView,
                            final int position,
                            final long itemId) {
        final ArticleBean articleBean = (ArticleBean) listView.getItemAtPosition(position);
        this.mListener.onArticleSelected(articleBean);

    }

    public interface OnArticleSelectedListener {
        public void onArticleSelected(ArticleBean articleBean);
    }
}
