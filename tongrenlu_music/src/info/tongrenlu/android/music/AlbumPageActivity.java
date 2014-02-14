package info.tongrenlu.android.music;

import info.tongrenlu.android.fragment.CursorFragmentAdapter;
import info.tongrenlu.android.music.fragment.AlbumInfoFragment;
import info.tongrenlu.android.music.fragment.AlbumInfoFragment.AlbumInfoFragmentListener;
import info.tongrenlu.android.music.provider.TongrenluContentProvider;
import info.tongrenlu.domain.TrackBean;

import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class AlbumPageActivity extends FragmentActivity implements AlbumInfoFragmentListener {

    public static final int ALBUM_CURSOR_LOADER = 1;

    protected CursorFragmentAdapter mAdapter;
    protected ViewPager mPager;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_album_page);

        final FragmentManager fm = this.getSupportFragmentManager();
        final CursorFragmentAdapter adapter = new CursorFragmentAdapter(fm,
                                                                        null) {

            @Override
            protected Fragment newFragment(Cursor c) {
                long id = c.getLong(c.getColumnIndex("_id"));
                String articleId = c.getString(c.getColumnIndex("articleId"));
                String title = c.getString(c.getColumnIndex("title"));
                AlbumInfoFragment fragment = new AlbumInfoFragment(articleId,
                                                                   title,
                                                                   id);
                return fragment;
            }

            @Override
            public CharSequence getPageTitle(final int position) {
                Cursor c = this.getCursor();
                c.moveToPosition(position);
                return c.getString(c.getColumnIndex("title"));
            }

        };
        this.mAdapter = adapter;

        this.mPager = (ViewPager) this.findViewById(R.id.pager);
        this.mPager.setAdapter(this.mAdapter);

        int position = this.getIntent().getIntExtra("position", 0);
        this.getSupportLoaderManager()
            .initLoader(AlbumPageActivity.ALBUM_CURSOR_LOADER,
                        null,
                        new AlbumCursorLoaderCallback(position));
    }

    private class AlbumCursorLoaderCallback implements LoaderCallbacks<Cursor> {

        private final int mPosition;

        public AlbumCursorLoaderCallback(int position) {
            this.mPosition = position;
        }

        @Override
        public Loader<Cursor> onCreateLoader(final int loaderId, final Bundle args) {
            final Context context = AlbumPageActivity.this;
            return new CursorLoader(context,
                                    TongrenluContentProvider.ALBUM_URI,
                                    null,
                                    null,
                                    null,
                                    "articleId desc");
        }

        @Override
        public void onLoadFinished(final Loader<Cursor> loader, final Cursor c) {
            AlbumPageActivity.this.mAdapter.swapCursor(c);
            if (c.getCount() == 0) {
                AlbumPageActivity.this.finish();
            } else {
                AlbumPageActivity.this.mPager.setCurrentItem(this.mPosition);
            }
        }

        @Override
        public void onLoaderReset(final Loader<Cursor> loader) {
            AlbumPageActivity.this.mAdapter.swapCursor(null);
        }

    }

    @Override
    public void onPlay(TrackBean trackBean) {
        final Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_APPEND);
        serviceIntent.putExtra("trackBean", trackBean);
        this.startService(serviceIntent);
    }

    @Override
    public void onPlayAll(ArrayList<TrackBean> trackBeanList) {
        if (CollectionUtils.isNotEmpty(trackBeanList)) {
            final Intent serviceIntent = new Intent(this, MusicService.class);
            serviceIntent.setAction(MusicService.ACTION_ADD);
            serviceIntent.putParcelableArrayListExtra("trackBeanList",
                                                      trackBeanList);
            serviceIntent.putExtra("position", 0);
            this.startService(serviceIntent);

            final Intent activityIntent = new Intent(this,
                                                     MusicPlayerActivity.class);
            this.startActivity(activityIntent);
        }
    }

    @Override
    public void onDownload(String title, TrackBean trackBean) {
        final Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.setAction(DownloadService.ACTION_ADD);
        serviceIntent.putExtra("trackBean", trackBean);

        Cursor cursor = null;
        try {
            cursor = this.getContentResolver()
                         .query(TongrenluContentProvider.PLAYLIST_URI,
                                null,
                                null,
                                null,
                                null);
            if (cursor.getCount() > 0) {
                new SelectPlaylistDialogFragment(title, serviceIntent).show(this.getSupportFragmentManager(),
                                                                            "dialog");
            } else {
                new CreatePlaylistDialogFragment(title, serviceIntent).show(this.getSupportFragmentManager(),
                                                                            "dialog");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    @Override
    public void onDownloadAll(String title, ArrayList<TrackBean> trackBeanList) {
        final Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.setAction(DownloadService.ACTION_ADD);
        serviceIntent.putParcelableArrayListExtra("trackBeanList",
                                                  trackBeanList);
        new CreatePlaylistDialogFragment(title, serviceIntent).show(this.getSupportFragmentManager(),
                                                                    "dialog");
    }

    public void showCreatePlaylistDialog(String title, final Intent serviceIntent) {
        Cursor cursor = null;
        try {
            cursor = this.getContentResolver()
                         .query(TongrenluContentProvider.PLAYLIST_URI,
                                null,
                                "title = ?",
                                new String[] { title },
                                null);

            if (cursor.getCount() > 0) {
                title = String.format("%s (%d)", title, cursor.getCount());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public class SelectPlaylistDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

        private String mTitle = null;
        private Intent mIntent = null;

        public SelectPlaylistDialogFragment(String title, final Intent serviceIntent) {
            this.mTitle = title;
            this.mIntent = serviceIntent;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final Cursor cursor = this.getActivity()
                                      .getContentResolver()
                                      .query(TongrenluContentProvider.PLAYLIST_URI,
                                             null,
                                             null,
                                             null,
                                             null);

            final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setTitle(R.string.dial_select_playlist)
                   .setCursor(cursor, this, "title")
                   .setPositiveButton(R.string.action_create, this)
                   .setNegativeButton(R.string.action_cancel, this);
            return builder.create();
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            System.out.println(which);
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                new CreatePlaylistDialogFragment(this.mTitle, this.mIntent).show(this.getActivity()
                                                                                     .getSupportFragmentManager(),
                                                                                 "dialog");
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
            default:
                final AlertDialog alertDialog = (AlertDialog) dialog;
                final long playlistId = alertDialog.getListView()
                                                   .getItemIdAtPosition(which);
                this.mIntent.putExtra("playlistId", playlistId);
                this.getActivity().startService(this.mIntent);
                break;
            }

        }
    }

    public class CreatePlaylistDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

        private String mTitle = null;
        private Intent mIntent = null;
        private EditText mTitleView = null;

        public CreatePlaylistDialogFragment(final String title, final Intent serviceIntent) {
            this.mTitle = title;
            this.mIntent = serviceIntent;
        }

        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            final LayoutInflater inflater = this.getActivity()
                                                .getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_create_playlist,
                                               null);
            this.mTitleView = (EditText) view.findViewById(R.id.playlist_title);
            this.mTitleView.setText(this.mTitle);

            final AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
            builder.setTitle(R.string.dial_create_playlist)
                   .setView(view)
                   .setPositiveButton(R.string.action_create, this)
                   .setNegativeButton(R.string.action_cancel, this);
            return builder.create();
        }

        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            System.out.println(which);
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                String title = this.mTitleView.getText().toString();
                final long playlistId = AlbumPageActivity.this.insertPlaylist(title);
                this.mIntent.putExtra("playlistId", playlistId);
                AlbumPageActivity.this.startService(this.mIntent);
                break;
            default:

                break;
            }

        }
    }

    private long insertPlaylist(String title) {
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
        return ContentUris.parseId(uri);
    }
}
