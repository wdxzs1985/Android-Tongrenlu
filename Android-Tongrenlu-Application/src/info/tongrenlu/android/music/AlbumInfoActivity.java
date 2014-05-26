package info.tongrenlu.android.music;

import info.tongrenlu.android.fragment.CursorFragmentAdapter;
import info.tongrenlu.android.fragment.DepthPageTransformer;
import info.tongrenlu.android.music.fragment.AlbumInfoFragment;
import info.tongrenlu.android.music.fragment.AlbumInfoFragment.AlbumInfoFragmentListener;
import info.tongrenlu.android.music.fragment.CreatePlaylistDialogFragment;
import info.tongrenlu.android.music.fragment.CreatePlaylistDialogFragment.CreatePlaylistDialogFragmentListener;
import info.tongrenlu.android.music.fragment.SelectPlaylistDialogFragment;
import info.tongrenlu.android.music.fragment.SelectPlaylistDialogFragment.SelectPlaylistDialogFragmentListener;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.ArticleBean;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

public class AlbumInfoActivity extends ActionBarActivity implements AlbumInfoFragmentListener, SelectPlaylistDialogFragmentListener, CreatePlaylistDialogFragmentListener {

    public static final int ALBUM_CURSOR_LOADER = 1;

    protected CursorFragmentAdapter mAdapter;
    protected ViewPager mPager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_album);

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final FragmentManager fm = this.getSupportFragmentManager();
        this.mAdapter = new CursorFragmentAdapter(fm, null) {

            @Override
            protected Fragment newFragment(final Cursor c) {
                final String articleId = c.getString(c.getColumnIndex("articleId"));
                final String title = c.getString(c.getColumnIndex("title"));

                ArticleBean articleBean = new ArticleBean();
                articleBean.setArticleId(articleId);
                articleBean.setTitle(title);

                return new AlbumInfoFragment(articleBean);
            }

        };
        this.mPager = (ViewPager) this.findViewById(R.id.pager);
        this.mPager.setAdapter(this.mAdapter);
        this.mPager.setPageTransformer(true, new DepthPageTransformer());

        final String articleId = this.getIntent().getStringExtra("articleId");
        this.getSupportLoaderManager()
            .initLoader(AlbumInfoActivity.ALBUM_CURSOR_LOADER,
                        null,
                        new AlbumCursorLoaderCallback(articleId));
    }

    private class AlbumCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        private final String mArticleId;

        public AlbumCursorLoaderCallback(final String articleId) {
            this.mArticleId = articleId;
        }

        @Override
        public Loader<Cursor> onCreateLoader(final int loaderId, final Bundle args) {
            final Context context = AlbumInfoActivity.this;
            return new CursorLoader(context,
                                    TongrenluContentProvider.ALBUM_URI,
                                    null,
                                    null,
                                    null,
                                    "articleId desc");
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
            AlbumInfoActivity.this.mAdapter.swapCursor(c);
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    final String articleId = c.getString(c.getColumnIndex("articleId"));
                    if (StringUtils.equals(articleId, this.mArticleId)) {
                        AlbumInfoActivity.this.mPager.setCurrentItem(c.getPosition());
                        break;
                    } else {
                        c.moveToNext();
                    }
                }
            } else {
                AlbumInfoActivity.this.finish();
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
            AlbumInfoActivity.this.mAdapter.swapCursor(null);
        }

    }

    @Override
    public void onPlay(final ArrayList<TrackBean> trackBeanList, final int position) {
        if (CollectionUtils.isNotEmpty(trackBeanList)) {
            final Intent serviceIntent = new Intent(this, MusicService.class);
            serviceIntent.setAction(MusicService.ACTION_ADD);
            serviceIntent.putParcelableArrayListExtra("trackBeanList",
                                                      trackBeanList);
            serviceIntent.putExtra("position", position);
            this.startService(serviceIntent);

            final Intent activityIntent = new Intent(this,
                                                     MusicPlayerActivity.class);
            this.startActivity(activityIntent);
        }
    }

    @Override
    public void onDownload(final String title, final TrackBean trackBean) {
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putParcelable("trackBean", trackBean);

        Cursor cursor = null;
        try {
            cursor = this.getContentResolver()
                         .query(TongrenluContentProvider.PLAYLIST_URI,
                                null,
                                null,
                                null,
                                null);
            DialogFragment dialog;
            if (cursor.getCount() > 0) {
                dialog = new SelectPlaylistDialogFragment();
            } else {
                dialog = new CreatePlaylistDialogFragment();
            }
            dialog.setArguments(args);
            dialog.show(this.getSupportFragmentManager(), "dialog");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    @Override
    public void onDownloadAll(final String title, final ArrayList<TrackBean> trackBeanList) {
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putParcelableArrayList("trackBeanList", trackBeanList);

        DialogFragment dialog = new CreatePlaylistDialogFragment();
        dialog.setArguments(args);
        dialog.show(this.getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onShowCreatePlaylistDialogFragment(Bundle extras) {
        DialogFragment dialog = new CreatePlaylistDialogFragment();
        dialog.setArguments(new Bundle(extras));
        dialog.show(this.getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onCreatePlaylist(Bundle extras) {
        String title = extras.getString("title",
                                        this.getString(R.string.title_new_playlist));
        final ContentResolver contentResolver = this.getContentResolver();
        final ContentValues values = new ContentValues();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(TongrenluContentProvider.PLAYLIST_URI,
                                           null,
                                           "title like ?",
                                           new String[] { title + "%" },
                                           null);
            if (cursor.getCount() == 0) {
                values.put("title", title);
            } else {
                values.put("title",
                           String.format("%s (%d)", title, cursor.getCount()));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        final Uri uri = contentResolver.insert(TongrenluContentProvider.PLAYLIST_URI,
                                               values);
        contentResolver.notifyChange(TongrenluContentProvider.PLAYLIST_URI,
                                     null);

        long playlistId = ContentUris.parseId(uri);
        extras.putLong("playlistId", playlistId);

        this.onSelectPlaylist(extras);
    }

    @Override
    public void onSelectPlaylist(Bundle extras) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.setAction(DownloadService.ACTION_ADD);
        intent.putExtras(extras);
        this.startService(intent);
    }

}
