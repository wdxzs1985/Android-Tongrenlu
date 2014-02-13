package info.tongrenlu.android.music.provider;

import info.tongrenlu.android.provider.SimpleDbHelper;

import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class TongrenluContentProvider extends ContentProvider {

    private static final int ALBUM = 10;
    private static final int TRACK = 20;
    private static final int PLAYLIST = 30;
    private static final int PLAYLIST_SINGLE = 31;
    private static final int PLAYLIST_TRACK = 32;
    private static final int PLAYLIST_TRACK_SINGLE = 33;

    public static final String AUTHORITY = "info.tongrenlu.android.music";
    public static final Uri ALBUM_URI = Uri.parse("content://" + TongrenluContentProvider.AUTHORITY
            + "/album");
    public static final Uri TRACK_URI = Uri.parse("content://" + TongrenluContentProvider.AUTHORITY
            + "/track");
    public static final Uri PLAYLIST_URI = Uri.parse("content://" + TongrenluContentProvider.AUTHORITY
            + "/playlist");

    private SimpleDbHelper mDbHelper = null;
    private UriMatcher mUriMatcher = null;

    @Override
    public boolean onCreate() {
        this.mDbHelper = new SimpleDbHelper(this.getContext(),
                                            new TongrenluDBV1());
        this.mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "album",
                                TongrenluContentProvider.ALBUM);
        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "track",
                                TongrenluContentProvider.TRACK);
        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "playlist",
                                TongrenluContentProvider.PLAYLIST);
        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "playlist/#",
                                TongrenluContentProvider.PLAYLIST_SINGLE);
        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "playlist/#/track",
                                TongrenluContentProvider.PLAYLIST_TRACK);
        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "playlist/#/track/#",
                                TongrenluContentProvider.PLAYLIST_TRACK_SINGLE);
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {
        switch (this.mUriMatcher.match(uri)) {
        case ALBUM:
            return this.queryAlbums(selection, selectionArgs, sortOrder);
        case TRACK:
            return this.queryTracks(selection, selectionArgs, sortOrder);
        case PLAYLIST:
            return this.queryPlaylists(selection, selectionArgs, sortOrder);
        case PLAYLIST_TRACK:
            return this.queryPlaylistTracks(uri, sortOrder);
        default:
            break;
        }
        return null;
    }

    private Cursor queryAlbums(final String selection, final String[] selectionArgs, String sortOrder) {
        return this.mDbHelper.query("tb_album",
                                    null,
                                    selection,
                                    selectionArgs,
                                    sortOrder);
    }

    private Cursor queryTracks(final String selection, final String[] selectionArgs, String sortOrder) {
        return this.mDbHelper.query("tb_track",
                                    null,
                                    selection,
                                    selectionArgs,
                                    sortOrder);
    }

    private Cursor queryPlaylists(final String selection, final String[] selectionArgs, String sortOrder) {
        return this.mDbHelper.query("tb_playlist",
                                    null,
                                    selection,
                                    selectionArgs,
                                    sortOrder);
    }

    private Cursor queryPlaylistTracks(final Uri uri, String sortOrder) {
        final List<String> segments = uri.getPathSegments();
        final String playlistId = segments.get(1);
        return this.mDbHelper.query("tb_playlist_track",
                                    null,
                                    "playlist_id = ?",
                                    new String[] { playlistId },
                                    sortOrder);
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        switch (this.mUriMatcher.match(uri)) {
        case ALBUM:
            return this.insertAlbum(uri, values);
        case TRACK:
            return this.insertTrack(uri, values);
        case PLAYLIST:
            return this.insertPlaylist(uri, values);
        case PLAYLIST_TRACK:
            return this.insertPlaylistTrack(uri, values);
        }
        return null;
    }

    private Uri insertAlbum(final Uri uri, final ContentValues values) {
        return this.insert(uri, "tb_album", values);
    }

    private Uri insertTrack(final Uri uri, final ContentValues values) {
        return this.insert(uri, "tb_track", values);
    }

    private Uri insertPlaylist(final Uri uri, final ContentValues values) {
        return this.insert(uri, "tb_playlist", values);
    }

    private Uri insertPlaylistTrack(final Uri uri, final ContentValues values) {
        final List<String> segments = uri.getPathSegments();
        final String playlistId = segments.get(1);
        values.put("playlist_id", playlistId);
        return this.insert(uri, "tb_playlist_track", values);
    }

    private Uri insert(final Uri uri, final String table, final ContentValues values) {
        final long id = this.mDbHelper.insert(table, values);
        if (id != -1) {
            return ContentUris.withAppendedId(uri, id);
        } else {
            return null;
        }
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        switch (this.mUriMatcher.match(uri)) {
        case PLAYLIST_SINGLE:
            return this.updatePlaylist(uri, values);
        }
        return 0;
    }

    private int updatePlaylist(final Uri uri, final ContentValues values) {
        final String _id = uri.getLastPathSegment();
        final int rows = this.mDbHelper.update("tb_playlist",
                                               values,
                                               "_id = ?",
                                               new String[] { _id });
        return rows;
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        switch (this.mUriMatcher.match(uri)) {
        case TRACK:
            return this.deleteTrack(selection, selectionArgs);
        case PLAYLIST_SINGLE:
            return this.deletePlaylist(uri);
        case PLAYLIST_TRACK_SINGLE:
            return this.deletePlaylistTrack(uri);
        }
        return 0;
    }

    private int deleteTrack(final String selection, final String[] selectionArgs) {
        final int rows = this.mDbHelper.delete("tb_track",
                                               selection,
                                               selectionArgs);
        if (rows > 0) {
            this.deletePlaylistTrack(selection, selectionArgs);
        }
        return rows;
    }

    private int deletePlaylist(final Uri uri) {
        final String _id = uri.getLastPathSegment();
        final int rows = this.mDbHelper.delete("tb_playlist",
                                               "_id",
                                               new String[] { _id });
        if (rows > 0) {
            this.deletePlaylistTrack("playlist_id = ?", new String[] { _id });
        }
        return rows;
    }

    private int deletePlaylistTrack(final Uri uri) {
        final String _id = uri.getLastPathSegment();
        return this.deletePlaylistTrack("_id = ?", new String[] { _id });
    }

    private int deletePlaylistTrack(final String selection, final String[] selectionArgs) {
        return this.mDbHelper.delete("tb_playlist_track",
                                     selection,
                                     selectionArgs);
    }

    @Override
    public String getType(final Uri uri) {
        return null;
    }

}
