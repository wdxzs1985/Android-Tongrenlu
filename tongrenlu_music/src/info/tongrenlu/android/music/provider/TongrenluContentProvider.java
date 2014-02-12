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
    public static final Uri ALBUM_URI = Uri.parse("content://" + AUTHORITY
            + "/album");
    public static final Uri TRACK_URI = Uri.parse("content://" + AUTHORITY
            + "/track");
    public static final Uri PLAYLIST_URI = Uri.parse("content://" + AUTHORITY
            + "/playlist");

    private SimpleDbHelper mDbHelper = null;
    private UriMatcher mUriMatcher = null;

    @Override
    public boolean onCreate() {
        this.mDbHelper = new SimpleDbHelper(this.getContext(),
                                            new TongrenluDBV1());
        this.mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        this.mUriMatcher.addURI(AUTHORITY, "/album", ALBUM);
        this.mUriMatcher.addURI(AUTHORITY, "/track", TRACK);
        this.mUriMatcher.addURI(AUTHORITY, "/playlist", PLAYLIST);
        this.mUriMatcher.addURI(AUTHORITY, "/playlist/#", PLAYLIST_SINGLE);
        this.mUriMatcher.addURI(AUTHORITY, "/playlist/#/track", PLAYLIST_TRACK);
        this.mUriMatcher.addURI(AUTHORITY,
                                "/playlist/#/track/#",
                                PLAYLIST_TRACK_SINGLE);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (this.mUriMatcher.match(uri)) {
        case ALBUM:
            return this.queryAlbums(selection, selectionArgs);
        case TRACK:
            return this.queryTracks(selection, selectionArgs);
        case PLAYLIST:
            return this.queryPlaylists(selection, selectionArgs);
        case PLAYLIST_TRACK:
            return this.queryPlaylistTracks(uri);
        default:
            break;
        }
        return null;
    }

    private Cursor queryAlbums(String selection, String[] selectionArgs) {
        return this.mDbHelper.query("tb_album",
                                    null,
                                    selection,
                                    selectionArgs,
                                    null);
    }

    private Cursor queryTracks(String selection, String[] selectionArgs) {
        return this.mDbHelper.query("tb_track",
                                    null,
                                    selection,
                                    selectionArgs,
                                    null);
    }

    private Cursor queryPlaylists(String selection, String[] selectionArgs) {
        return this.mDbHelper.query("tb_playlist",
                                    null,
                                    selection,
                                    selectionArgs,
                                    null);
    }

    private Cursor queryPlaylistTracks(Uri uri) {
        List<String> segments = uri.getPathSegments();
        String playlistId = segments.get(1);
        return this.mDbHelper.query("tb_playlist_track",
                                    null,
                                    "playlist_id = ?",
                                    new String[] { playlistId },
                                    null);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
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

    private Uri insertAlbum(Uri uri, ContentValues values) {
        return this.insert(uri, "tb_album", values);
    }

    private Uri insertTrack(Uri uri, ContentValues values) {
        return this.insert(uri, "tb_track", values);
    }

    private Uri insertPlaylist(Uri uri, ContentValues values) {
        return this.insert(uri, "tb_playlist", values);
    }

    private Uri insertPlaylistTrack(Uri uri, ContentValues values) {
        List<String> segments = uri.getPathSegments();
        String playlistId = segments.get(1);
        values.put("playlist_id", playlistId);
        return this.insert(uri, "tb_playlist_track", values);
    }

    private Uri insert(Uri uri, String table, ContentValues values) {
        final long id = this.mDbHelper.insert(table, values);
        if (id != -1) {
            return ContentUris.withAppendedId(uri, id);
        } else {
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (this.mUriMatcher.match(uri)) {
        case PLAYLIST_SINGLE:
            return this.updatePlaylist(uri, values);
        }
        return 0;
    }

    private int updatePlaylist(Uri uri, ContentValues values) {
        String _id = uri.getLastPathSegment();
        int rows = this.mDbHelper.update("tb_playlist",
                                         values,
                                         "_id = ?",
                                         new String[] { _id });
        return rows;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
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

    private int deleteTrack(String selection, String[] selectionArgs) {
        int rows = this.mDbHelper.delete("tb_track", selection, selectionArgs);
        if (rows > 0) {
            this.deletePlaylistTrack(selection, selectionArgs);
        }
        return rows;
    }

    private int deletePlaylist(Uri uri) {
        String _id = uri.getLastPathSegment();
        int rows = this.mDbHelper.delete("tb_playlist",
                                         "_id",
                                         new String[] { _id });
        if (rows > 0) {
            this.deletePlaylistTrack("playlist_id = ?", new String[] { _id });
        }
        return rows;
    }

    private int deletePlaylistTrack(Uri uri) {
        String _id = uri.getLastPathSegment();
        return this.deletePlaylistTrack("_id = ?", new String[] { _id });
    }

    private int deletePlaylistTrack(String selection, String[] selectionArgs) {
        return this.mDbHelper.delete("tb_playlist_track",
                                     selection,
                                     selectionArgs);
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

}
