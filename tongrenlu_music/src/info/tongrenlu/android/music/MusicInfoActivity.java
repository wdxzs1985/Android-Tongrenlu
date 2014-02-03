package info.tongrenlu.android.music;

import info.tongrenlu.android.music.adapter.MusicTrackListAdapter;
import info.tongrenlu.android.task.JSONLoadTask;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MusicInfoActivity extends BaseActivity implements OnItemClickListener, OnClickListener {

    private String mArticleId = null;
    // private ContentObserver contentObserver = null;

    private View mProgress = null;
    private View mEmpty = null;
    private ListView mListView = null;
    private MusicTrackListAdapter mAdapter = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_music_info);

        final Intent intent = this.getIntent();
        this.mArticleId = intent.getStringExtra("articleId");
        final String title = intent.getStringExtra("title");

        this.initArticleCover(this.mArticleId);
        this.initAritcleTitle(title);

        this.mAdapter = new MusicTrackListAdapter();

        this.mProgress = this.findViewById(android.R.id.progress);
        this.mEmpty = this.findViewById(android.R.id.empty);
        this.mListView = (ListView) this.findViewById(android.R.id.list);
        //
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);

        final Button downloadAllButton = (Button) this.findViewById(R.id.action_download_all);
        downloadAllButton.setOnClickListener(this);

        final Uri uri = HttpConstants.getMusicInfoUri(MusicInfoActivity.this,
                                                      this.mArticleId);
        new MusicInfoLoadTask().execute(uri);
    }

    private void initArticleCover(final String articleId) {
        final ImageView coverView = (ImageView) this.findViewById(R.id.article_cover);
        HttpConstants.displayCover(coverView, articleId, HttpConstants.S_COVER);
    }

    private void initAritcleTitle(final String title) {
        final TextView articleTitle = (TextView) this.findViewById(R.id.article_title);
        articleTitle.setText(title);
    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View view, final int position, final long id) {
        final TrackBean trackBean = (TrackBean) listView.getItemAtPosition(position);
        this.playTrack(trackBean);
        // DownloadService.downloadTrack(this, trackBean);
    }

    @Override
    public void onClick(final View v) {
        if (!this.mAdapter.isEmpty()) {
            // final ArrayList<TrackBean> items = this.mAdapter.getItems();
            // this.playTrack(items);
            // DownloadService.downloadTrack(this, items);
        }
    }

    class MusicInfoLoadTask extends JSONLoadTask {

        @Override
        protected void onStart() {
            MusicInfoActivity.this.mProgress.setVisibility(View.VISIBLE);
            MusicInfoActivity.this.mListView.setVisibility(View.GONE);
            MusicInfoActivity.this.mEmpty.setVisibility(View.GONE);
        }

        @Override
        protected void processResponseJSON(final JSONObject responseJSON) throws JSONException {
            final MusicInfoActivity context = MusicInfoActivity.this;
            if (responseJSON.getBoolean("result")) {
                final JSONArray items = responseJSON.getJSONArray("playlist");
                for (int i = 0; i < items.length(); i++) {
                    final JSONObject trackJSON = items.getJSONObject(i);
                    final TrackBean trackBean = new TrackBean();
                    if (trackJSON.has("articleId")) {
                        trackBean.setArticleId(trackJSON.getString("articleId"));
                    }

                    if (trackJSON.has("fileId")) {
                        trackBean.setFileId(trackJSON.getString("fileId"));
                    }

                    if (trackJSON.has("title")) {
                        trackBean.setTitle(trackJSON.getString("title"));
                    }

                    if (trackJSON.has("artist")) {
                        trackBean.setArtist(trackJSON.getString("artist"));
                    }
                    MusicInfoActivity.this.mAdapter.addData(trackBean);
                }
            } else {
                Toast.makeText(context,
                               responseJSON.getString("error"),
                               Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onJSONException(final JSONException e) {
            super.onJSONException(e);
            final MusicInfoActivity context = MusicInfoActivity.this;
            Toast.makeText(context, "数据解析失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onNetworkError(final int code) {
            super.onNetworkError(code);
            final MusicInfoActivity context = MusicInfoActivity.this;
            Toast.makeText(context,
                           context.getText(R.string.err_network),
                           Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onFinish() {
            if (MusicInfoActivity.this.mAdapter.isEmpty()) {
                MusicInfoActivity.this.mProgress.setVisibility(View.GONE);
                MusicInfoActivity.this.mListView.setVisibility(View.GONE);
                MusicInfoActivity.this.mEmpty.setVisibility(View.VISIBLE);
            } else {
                MusicInfoActivity.this.mProgress.setVisibility(View.GONE);
                MusicInfoActivity.this.mEmpty.setVisibility(View.GONE);
                MusicInfoActivity.this.mListView.setVisibility(View.VISIBLE);
                MusicInfoActivity.this.mAdapter.notifyDataSetChanged();
            }
        }
    }

    protected void playTrack(final TrackBean trackBean) {
        // TODO
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_ADD);
        serviceIntent.putExtra("trackBean", trackBean);
        this.startService(serviceIntent);
    }

    protected void playTrack(final ArrayList<TrackBean> items) {
        // TODO
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_PLAY);
        serviceIntent.putParcelableArrayListExtra("trackBeanList", items);
        serviceIntent.putExtra("position", 0);
        this.startService(serviceIntent);

        final Intent activityIntent = new Intent(this,
                                                 MusicPlayerActivity.class);
        this.startActivity(activityIntent);
    }
}
