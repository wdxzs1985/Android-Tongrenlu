package info.tongrenlu.android.music;

import info.tongrenlu.android.fragment.TitleFragmentAdapter;
import info.tongrenlu.android.fragment.ZoomOutPageTransformer;
import info.tongrenlu.android.music.fragment.AlbumFragment;
import info.tongrenlu.android.music.fragment.AlbumUpdateFragment;
import info.tongrenlu.android.music.fragment.CreatePlaylistDialogFragment;
import info.tongrenlu.android.music.fragment.CreatePlaylistDialogFragment.CreatePlaylistDialogFragmentListener;
import info.tongrenlu.android.music.fragment.PlaylistFragment;
import info.tongrenlu.android.music.fragment.SelectPlaylistDialogFragment;
import info.tongrenlu.android.music.fragment.SelectPlaylistDialogFragment.SelectPlaylistDialogFragmentListener;
import info.tongrenlu.android.music.fragment.TrackFragment;
import info.tongrenlu.android.music.fragment.TrackFragment.TrackFragmentListener;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.app.CommonConstants;
import info.tongrenlu.app.HttpConstants;
import info.tongrenlu.domain.TrackBean;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.viewpagerindicator.PageIndicator;

public class MainActivity extends ActionBarActivity implements TrackFragmentListener, SelectPlaylistDialogFragmentListener, CreatePlaylistDialogFragmentListener {

    public static final int ALBUM_LOADER = 0;
    public static final int PLAYLIST_LOADER = 1;
    public static final int TRACK_LOADER = 2;

    private long mExitTime = 0;
    protected FragmentPagerAdapter mAdapter;
    protected ViewPager mPager;
    protected PageIndicator mIndicator;

    private Toast mToast = null;

    public final static int UPDATE_ALBUM = 1;

    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case UPDATE_ALBUM:
                MainActivity.this.onUpdateAlbum();
                break;
            }
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        // ActionBar actionBar = this.getSupportActionBar();
        // actionBar.setDisplayShowTitleEnabled(false);

        final FragmentManager fm = this.getSupportFragmentManager();
        final TitleFragmentAdapter adapter = new TitleFragmentAdapter(fm);
        adapter.addItem(new AlbumFragment());
        adapter.addItem(new PlaylistFragment());
        adapter.addItem(new TrackFragment());
        this.mAdapter = adapter;

        this.mPager = (ViewPager) this.findViewById(R.id.pager);
        this.mPager.setAdapter(this.mAdapter);
        this.mPager.setPageTransformer(true, new ZoomOutPageTransformer());

        this.mIndicator = (PageIndicator) this.findViewById(R.id.indicator);
        this.mIndicator.setViewPager(this.mPager);
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

    public void dispatchUpdateAlbum() {
        this.mHandler.sendEmptyMessage(MainActivity.UPDATE_ALBUM);
    }

    public void onUpdateAlbum() {
        final AlbumUpdateFragment fragment = new AlbumUpdateFragment();
        fragment.show(this.getSupportFragmentManager(), "update");
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            this.showSetting();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showSetting() {
        final Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    @Override
    public void onDeleteTrack(TrackBean trackBean) {
        this.deleteTrack(trackBean);
        this.deletePlaylistTrack(trackBean);
        this.deleteMp3File(trackBean);
    }

    private void deleteMp3File(TrackBean trackBean) {
        String articleId = trackBean.getArticleId();
        String fileId = trackBean.getFileId();

        File file = HttpConstants.getMp3(this, articleId, fileId);
        FileUtils.deleteQuietly(file);
    }

    private void deleteTrack(TrackBean trackBean) {
        String articleId = trackBean.getArticleId();
        String fileId = trackBean.getFileId();
        ContentResolver contentResolver = this.getContentResolver();
        ContentValues values = new ContentValues();
        values.put("downloadFlg", 0);
        contentResolver.update(TongrenluContentProvider.TRACK_URI,
                               values,
                               "articleId = ? and fileId = ? and downloadFlg = 1",
                               new String[] { articleId, fileId });
        contentResolver.notifyChange(TongrenluContentProvider.TRACK_URI, null);
    }

    private void deletePlaylistTrack(TrackBean trackBean) {
        String articleId = trackBean.getArticleId();
        String fileId = trackBean.getFileId();
        ContentResolver contentResolver = this.getContentResolver();
        contentResolver.delete(TongrenluContentProvider.PLAYLIST_TRACK_URI,
                               "articleId = ? and fileId = ?",
                               new String[] { articleId, fileId });
    }

    @Override
    public void onAddToPlaylist(TrackBean trackBean) {
        Bundle args = new Bundle();
        args.putString("title", "");
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
    public void onShowCreatePlaylistDialogFragment(Bundle args) {
        DialogFragment dialog = new CreatePlaylistDialogFragment();
        dialog.setArguments(new Bundle(args));
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
        long playlistId = extras.getLong("playlistId");
        TrackBean trackBean = extras.getParcelable("trackBean");

        final ContentResolver contentResolver = this.getContentResolver();
        final ContentValues values = new ContentValues();
        values.put("articleId", trackBean.getArticleId());
        values.put("fileId", trackBean.getFileId());
        values.put("album", trackBean.getAlbum());
        values.put("songTitle", trackBean.getSongTitle());
        values.put("leadArtist", trackBean.getLeadArtist());

        final Uri contentUri = Uri.withAppendedPath(TongrenluContentProvider.PLAYLIST_URI,
                                                    playlistId + "/track");

        if (trackBean.getTrackNumber() == 0) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(contentUri,
                                               null,
                                               null,
                                               null,
                                               null);
                values.put("trackNumber", cursor.getCount() + 1);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            values.put("trackNumber", trackBean.getTrackNumber());
        }

        contentResolver.insert(contentUri, values);
        contentResolver.notifyChange(contentUri, null);

        String msg = this.getString(R.string.message_add_track_to_playlist,
                                    trackBean.getSongTitle());
        Toast.makeText(this.getApplicationContext(), msg, Toast.LENGTH_SHORT)
             .show();
    }

    @Override
    public void onPlay(ArrayList<TrackBean> trackBeanList, int position) {
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
