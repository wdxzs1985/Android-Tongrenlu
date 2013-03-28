package info.tongrenlu.android.music;

import info.tongrenlu.android.music.adapter.ArticleListAdapter;
import info.tongrenlu.android.task.JSONLoadTask;
import info.tongrenlu.app.CommonConstants;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.ArticleBean;
import info.tongrenlu.domain.MusicBean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class MusicListActivity extends BaseActivity implements
        OnScrollListener, OnItemClickListener {

    private View mProgress = null;
    private View mEmpty = null;
    private AbsListView mListView = null;
    private ArticleListAdapter mAdapter = null;

    private MusicListLoadTask runningTask = null;
    private int mPage = 0;
    private boolean mLast = false;

    private long mExitTime = 0;
    private Toast mToast = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.fragment_grid_view);
        this.mProgress = this.findViewById(android.R.id.progress);
        this.mEmpty = this.findViewById(android.R.id.empty);
        this.mListView = (AbsListView) this.findViewById(android.R.id.list);
        //
        this.mAdapter = new ArticleListAdapter();
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnScrollListener(this);
        this.mListView.setOnItemClickListener(this);

        this.loadNextPage();
    }

    protected void loadNextPage() {
        if (this.runningTask == null && !this.mLast) {
            final Uri uri = HttpConstants.getMusicListUri(this);
            final Bundle parameters = new Bundle();
            parameters.putString("q", "");
            parameters.putString("p", String.valueOf(this.mPage + 1));
            parameters.putString("s", String.valueOf(HttpConstants.PAGE_SIZE));
            new MusicListLoadTask(this).execute(uri, parameters);
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> listView,
                            final View view,
                            final int position,
                            final long _id) {
        final ArticleBean articleBean = (ArticleBean) listView.getItemAtPosition(position);
        final String articleId = articleBean.getArticleId();
        final String title = articleBean.getTitle();

        final Intent intent = new Intent();
        intent.putExtra("articleId", articleId);
        intent.putExtra("title", title);
        intent.setClass(this, MusicInfoActivity.class);

        this.startActivity(intent);
    }

    @Override
    public void onScroll(final AbsListView arg0,
                         final int arg1,
                         final int arg2,
                         final int arg3) {

    }

    @Override
    public void onScrollStateChanged(final AbsListView view,
                                     final int scrollState) {
        switch (scrollState) {
        case SCROLL_STATE_IDLE:
            this.mAdapter.setScrolling(false);
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
    public void onBackPressed() {
        final long now = System.currentTimeMillis();
        final int duration = (int) (CommonConstants.TWO * CommonConstants.SECOND);
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this,
                                         R.string.press_back_hit_1,
                                         duration);
        }
        if (now - this.mExitTime > duration) {
            this.mExitTime = now;
            this.mToast.setText(R.string.press_back_hit_1);
            this.mToast.show();
        } else {
            this.mToast.setText(R.string.press_back_hit_2);
            this.mToast.show();
            this.finish();
        }
    }

    private class MusicListLoadTask extends JSONLoadTask {

        private final MusicListActivity mActivity;

        public MusicListLoadTask(final MusicListActivity activity) {
            this.mActivity = activity;
        }

        @Override
        protected void onStart() {
            super.onStart();
            this.mActivity.runningTask = this;
            this.mActivity.mProgress.setVisibility(View.VISIBLE);
            this.mActivity.mListView.setVisibility(View.VISIBLE);
            this.mActivity.mEmpty.setVisibility(View.GONE);
        }

        @Override
        protected void processResponseJSON(final JSONObject responseJSON)
                throws JSONException {
            super.processResponseJSON(responseJSON);
            if (responseJSON.getBoolean("result")) {
                final JSONObject pageJSON = responseJSON.getJSONObject("page");
                final int page = pageJSON.getInt("page");
                this.mActivity.mLast = pageJSON.getBoolean("last");
                if (this.mActivity.mPage < page) {
                    this.mActivity.mPage = page;
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
                        this.mActivity.mAdapter.addData(musicBean);
                    }
                }
            } else {
                Toast.makeText(this.mActivity,
                               responseJSON.getString("error"),
                               Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onJSONException(final JSONException e) {
            super.onJSONException(e);
            Toast.makeText(this.mActivity, "数据解析失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onNetworkError(final int code) {
            super.onNetworkError(code);
            Toast.makeText(this.mActivity,
                           R.string.err_network,
                           Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onFinish() {
            super.onFinish();
            if (this.mActivity.mAdapter.isEmpty()) {
                this.mActivity.mProgress.setVisibility(View.GONE);
                this.mActivity.mListView.setVisibility(View.GONE);
                this.mActivity.mEmpty.setVisibility(View.VISIBLE);
            } else {
                this.mActivity.mProgress.setVisibility(View.GONE);
                this.mActivity.mEmpty.setVisibility(View.GONE);
                this.mActivity.mListView.setVisibility(View.VISIBLE);
                this.mActivity.mAdapter.notifyDataSetChanged();
            }
            this.mActivity.runningTask = null;
        }
    }
}
