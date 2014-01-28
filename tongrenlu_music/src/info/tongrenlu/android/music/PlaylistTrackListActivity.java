package info.tongrenlu.android.music;

import info.tongrenlu.android.music.adapter.PlaylistTrackListAdapter;
import info.tongrenlu.android.music.provider.DataProvider;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class PlaylistTrackListActivity extends BaseActivity implements OnItemClickListener, LoaderCallbacks<Cursor> {

    private View mProgress = null;
    private View mEmpty = null;
    private ListView mListView = null;
    private PlaylistTrackListAdapter mAdapter = null;

    private ContentObserver contentObserver = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                super.onChange(selfChange);
                PlaylistTrackListActivity.this.getLoaderManager()
                                              .getLoader(0)
                                              .forceLoad();
            }
        };
        this.mAdapter = new PlaylistTrackListAdapter(this, null);

        this.setContentView(R.layout.fragment_list_view);
        this.mProgress = this.findViewById(android.R.id.progress);
        this.mEmpty = this.findViewById(android.R.id.empty);
        this.mListView = (ListView) this.findViewById(android.R.id.list);
        //
        this.mListView.setAdapter(this.mAdapter);
        this.mListView.setOnItemClickListener(this);

        this.registerForContextMenu(this.mListView);
        final Bundle args = new Bundle();
        this.getSupportLoaderManager().initLoader(0, args, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        final CursorLoader loader = new CursorLoader(this);
        loader.setUri(DataProvider.URI_TRACK);
        return loader;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
        this.mAdapter.swapCursor(c);
        this.mProgress.setVisibility(View.GONE);
        if (this.mAdapter.isEmpty()) {
            this.mListView.setVisibility(View.GONE);
            this.mEmpty.setVisibility(View.VISIBLE);
        } else {
            this.mEmpty.setVisibility(View.GONE);
            this.mListView.setVisibility(View.VISIBLE);
            this.mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        this.mAdapter.swapCursor(null);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.getContentResolver()
            .registerContentObserver(DataProvider.URI_TRACK,
                                     true,
                                     this.contentObserver);
        this.getSupportLoaderManager().getLoader(0).forceLoad();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.getContentResolver()
            .unregisterContentObserver(this.contentObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getSupportLoaderManager().destroyLoader(0);
    }

    @Override
    public void onItemClick(final AdapterView<?> listView, final View itemView, final int position, final long itemId) {
        final Cursor c = (Cursor) listView.getItemAtPosition(position);
        final long id = c.getLong(c.getColumnIndex("_id"));
        final String articleId = c.getString(c.getColumnIndex("article_id"));
        final String fileId = c.getString(c.getColumnIndex("file_id"));
        final long size = c.getLong(c.getColumnIndex("size"));
        final String title = c.getString(c.getColumnIndex("title"));
        final String artist = c.getString(c.getColumnIndex("artist"));
        final Context context = this;

        final File source = HttpConstants.getMp3(context, articleId, fileId);
        if (!source.exists()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.dial_info)
                   .setMessage(R.string.dial_message_download_confirm_2);
            builder.setPositiveButton(R.string.action_download,
                                      new OnClickListener() {

                                          @Override
                                          public void onClick(final DialogInterface dialog, final int which) {
                                              final TrackBean trackBean = new TrackBean();
                                              trackBean.setArticleId(articleId);
                                              trackBean.setFileId(fileId);
                                              trackBean.setTitle(title);
                                              trackBean.setArtist(artist);
                                              PlaylistTrackListActivity.this.onDialogPositiveClick(id,
                                                                                                   trackBean);
                                          }
                                      });
            builder.setNegativeButton(R.string.action_cancel,
                                      new OnClickListener() {

                                          @Override
                                          public void onClick(final DialogInterface dialog, final int which) {
                                              final TrackBean trackBean = new TrackBean();
                                              trackBean.setArticleId(articleId);
                                              trackBean.setFileId(fileId);
                                              trackBean.setTitle(title);
                                              trackBean.setArtist(artist);
                                              PlaylistTrackListActivity.this.onDialogNegativeClick(id,
                                                                                                   trackBean);
                                          }
                                      });
            builder.create().show();
        } else if (source.length() != size) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.dial_info)
                   .setMessage(R.string.dial_message_download_confirm_1);
            builder.setPositiveButton(R.string.action_download,
                                      new OnClickListener() {

                                          @Override
                                          public void onClick(final DialogInterface dialog, final int which) {
                                              final TrackBean trackBean = new TrackBean();
                                              trackBean.setArticleId(articleId);
                                              trackBean.setFileId(fileId);
                                              trackBean.setTitle(title);
                                              trackBean.setArtist(artist);
                                              PlaylistTrackListActivity.this.onDialogPositiveClick(id,
                                                                                                   trackBean);
                                          }
                                      });
            builder.setNegativeButton(R.string.action_cancel,
                                      new OnClickListener() {

                                          @Override
                                          public void onClick(final DialogInterface dialog, final int which) {
                                              final TrackBean trackBean = new TrackBean();
                                              trackBean.setArticleId(articleId);
                                              trackBean.setFileId(fileId);
                                              trackBean.setTitle(title);
                                              trackBean.setArtist(artist);
                                              PlaylistTrackListActivity.this.onDialogNegativeClick(id,
                                                                                                   trackBean);
                                          }
                                      });
            builder.create().show();
        } else {
            if (c.moveToFirst()) {
                final ArrayList<TrackBean> trackBeanList = new ArrayList<TrackBean>();
                while (!c.isAfterLast()) {
                    final TrackBean trackBean = new TrackBean();
                    trackBean.setArticleId(c.getString(c.getColumnIndex("article_id")));
                    trackBean.setFileId(c.getString(c.getColumnIndex("file_id")));
                    trackBean.setTitle(c.getString(c.getColumnIndex("title")));
                    trackBean.setArtist(c.getString(c.getColumnIndex("artist")));
                    trackBeanList.add(trackBean);
                    c.moveToNext();
                }

                final Intent serviceIntent = new Intent(context,
                                                        MusicService.class);
                serviceIntent.setAction(MusicService.ACTION_INIT);
                serviceIntent.putParcelableArrayListExtra("trackBeanList",
                                                          trackBeanList);
                serviceIntent.putExtra("position", position);
                context.startService(serviceIntent);

                final Intent activityIntent = new Intent(context,
                                                         MusicPlayerActivity.class);
                context.startActivity(activityIntent);
            }
        }

    }

    public void onDialogPositiveClick(final long id, final TrackBean trackBean) {
        final Context context = this;
        final Uri deleteUri = ContentUris.withAppendedId(DataProvider.URI_TRACK,
                                                         id);
        context.getContentResolver().delete(deleteUri, null, null);
        DownloadService.downloadTrack(context, trackBean);

    }

    public void onDialogNegativeClick(final long id, final TrackBean trackBean) {
        final Context context = this;
        final Uri deleteUri = ContentUris.withAppendedId(DataProvider.URI_TRACK,
                                                         id);
        context.getContentResolver().delete(deleteUri, null, null);
        final File mp3File = HttpConstants.getMp3(context,
                                                  trackBean.getArticleId(),
                                                  trackBean.getFileId());
        FileUtils.deleteQuietly(mp3File);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.fragment_playlist_track, menu);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.action_delete:
            final Cursor c = (Cursor) this.mListView.getItemAtPosition(info.position);
            final String articleId = c.getString(c.getColumnIndex("article_id"));
            final String fileId = c.getString(c.getColumnIndex("file_id"));
            final String title = c.getString(c.getColumnIndex("title"));
            final String artist = c.getString(c.getColumnIndex("artist"));
            final TrackBean trackBean = new TrackBean();
            trackBean.setArticleId(articleId);
            trackBean.setFileId(fileId);
            trackBean.setTitle(title);
            trackBean.setArtist(artist);

            this.onDialogNegativeClick(info.id, trackBean);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
}
