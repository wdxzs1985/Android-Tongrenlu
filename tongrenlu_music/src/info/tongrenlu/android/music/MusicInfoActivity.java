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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class MusicInfoActivity extends BaseActivity implements ActionSlideExpandableListView.OnActionClickListener, OnClickListener {

    private String mArticleId = null;

    private View mProgress = null;
    private View mEmpty = null;
    private ActionSlideExpandableListView mListView = null;
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

        final Uri uri = HttpConstants.getMusicInfoUri(MusicInfoActivity.this,
                                                      this.mArticleId);
        new MusicInfoLoadTask().execute(uri);
    }

    private void initArticleCover(final String articleId) {
        final ImageView coverView = (ImageView) this.findViewById(R.id.article_cover);
        HttpConstants.displayCover(coverView, articleId, HttpConstants.L_COVER);
    }

    private void initAritcleTitle(final String title) {
        final TextView articleTitle = (TextView) this.findViewById(R.id.article_title);
        articleTitle.setText(title);
    }

    @Override
    public void onClick(View itemView, View clickedView, int position) {
        switch (clickedView.getId()) {
        case R.id.item:
        case R.id.action_play:
            final TrackBean trackBean = (TrackBean) this.mListView.getItemAtPosition(position);
            this.playTrack(trackBean);
            break;
        case R.id.action_download:
            // TODO download
            System.out.println("TODO download");
            break;
        default:
            break;
        }
    }

    @Override
    public void onClick(final View v) {
        if (!this.mAdapter.isEmpty()) {
            ArrayList<TrackBean> items = this.mAdapter.getItems();
            switch (v.getId()) {
            case R.id.action_play_all:
                this.playTrack(items);
                break;
            case R.id.action_download_all:
                // TODO download
                System.out.println("TODO download");
                break;
            default:
                break;
            }
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
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_ADD);
        serviceIntent.putExtra("trackBean", trackBean);
        this.startService(serviceIntent);
    }

    protected void playTrack(final ArrayList<TrackBean> items) {
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_ADD);
        serviceIntent.putParcelableArrayListExtra("trackBeanList", items);
        serviceIntent.putExtra("position", 0);
        this.startService(serviceIntent);

        final Intent activityIntent = new Intent(this,
                                                 MusicPlayerActivity.class);
        this.startActivity(activityIntent);
    }

}
