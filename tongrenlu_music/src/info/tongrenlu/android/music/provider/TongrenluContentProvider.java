package info.tongrenlu.android.music.provider;

import info.tongrenlu.android.provider.SimpleDbHelper;

import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;

public class TongrenluContentProvider extends ContentProvider {

    /**
     * <h1>content://info.tongrenlu.android.music/album</h1>
     * <ul>
     * <li>query</li>
     * <li>insert</li>
     * </ul>
     */
    private static final int QI_ALBUM = 11001;
    /**
     * <h1>content://info.tongrenlu.android.music/album/#/track</h1>
     * <ul>
     * <li>query</li>
     * </ul>
     */
    private static final int Q_ALBUM_TRACK = 10002;
    /**
     * <h1>content://info.tongrenlu.android.music/track</h1>
     * <ul>
     * <li>query</li>
     * <li>insert</li>
     * <li>update</li>
     * </ul>
     */
    private static final int QIU_TRACK = 11102;
    /**
     * <h1>content://info.tongrenlu.android.music/track/#</h1>
     * <ul>
     * <li>update</li>
     * </ul>
     */
    private static final int U_TRACK_SINGLE = 00102;
    /**
     * <h1>content://info.tongrenlu.android.music/playlist</h1>
     * <ul>
     * <li>query</li>
     * <li>insert</li>
     * </ul>
     */
    private static final int QI_PLAYLIST = 11003;
    /**
     * <h1>content://info.tongrenlu.android.music/playlist/#</h1>
     * <ul>
     * <li>update</li>
     * <li>delete</li>
     * </ul>
     */
    private static final int UD_PLAYLIST_SINGLE = 00113;
    /**
     * <h1>content://info.tongrenlu.android.music/playlist/#/track</h1>
     * <ul>
     * <li>query</li>
     * <li>insert</li>
     * <li>delete</li>
     * </ul>
     */
    private static final int QID_PLAYLIST_TRACK = 11014;
    /**
     * <h1>content://info.tongrenlu.android.music/playlist/#/track/#</h1>
     * <ul>
     * <li>update</li>
     * <li>delete</li>
     * </ul>
     */
    private static final int UD_PLAYLIST_TRACK_SINGLE = 00014;

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
                                TongrenluContentProvider.QI_ALBUM);

        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "album/#/track",
                                TongrenluContentProvider.Q_ALBUM_TRACK);

        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "track",
                                TongrenluContentProvider.QIU_TRACK);

        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "track/#",
                                TongrenluContentProvider.U_TRACK_SINGLE);

        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "playlist",
                                TongrenluContentProvider.QI_PLAYLIST);

        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "playlist/#",
                                TongrenluContentProvider.UD_PLAYLIST_SINGLE);

        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "playlist/#/track",
                                TongrenluContentProvider.QID_PLAYLIST_TRACK);

        this.mUriMatcher.addURI(TongrenluContentProvider.AUTHORITY,
                                "playlist/#/track/#",
                                TongrenluContentProvider.UD_PLAYLIST_TRACK_SINGLE);
        return true;
    }

    @Override
    public Cursor query(final Uri uri,
                        final String[] projection,
                        final String selection,
                        final String[] selectionArgs,
                        final String sortOrder) {
        switch (this.mUriMatcher.match(uri)) {
        case QI_ALBUM:
            return this.queryAlbums(selection, selectionArgs, sortOrder);
        case Q_ALBUM_TRACK:
            return this.queryAlbumTrack(uri, sortOrder);
        case QIU_TRACK:
            return this.queryTracks(selection, selectionArgs, sortOrder);
        case QI_PLAYLIST:
            return this.queryPlaylists(selection, selectionArgs, sortOrder);
        case QID_PLAYLIST_TRACK:
            return this.queryPlaylistTracks(uri, sortOrder);
        default:
            break;
        }
        return null;
    }

    private Cursor queryAlbums(final String selection,
                               final String[] selectionArgs,
                               final String sortOrder) {
        return this.mDbHelper.query("tb_album",
                                    null,
                                    selection,
                                    selectionArgs,
                                    sortOrder);
    }

    private Cursor queryAlbumTrack(final Uri uri, final String sortOrder) {
        final List<String> segments = uri.getPathSegments();
        final String articleId = segments.get(1);
        return this.mDbHelper.query("tb_track",
                                    null,
                                    "articleId = ?",
                                    new String[] { articleId },
                                    sortOrder);
    }

    private Cursor queryTracks(final String selection,
                               final String[] selectionArgs,
                               final String sortOrder) {
        return this.mDbHelper.query("tb_track",
                                    null,
                                    selection,
                                    selectionArgs,
                                    sortOrder);
    }

    private Cursor queryPlaylists(final String selection,
                                  final String[] selectionArgs,
                                  final String sortOrder) {
        return this.mDbHelper.query("tb_playlist",
                                    null,
                                    selection,
                                    selectionArgs,
                                    sortOrder);
    }

    private Cursor queryPlaylistTracks(final Uri uri, final String sortOrder) {
        final List<String> segments = uri.getPathSegments();
        final String playlistId = segments.get(1);
        return this.mDbHelper.query("tb_playlist_track",
                                    null,
                                    "playlistId = ?",
                                    new String[] { playlistId },
                                    sortOrder);
    }

    @Override
    public int bulkInsert(final Uri uri, final ContentValues[] values) {
        switch (this.mUriMatcher.match(uri)) {
        case QI_ALBUM:
            return this.bulkInsertAlbum(uri, values);
        case QIU_TRACK:
            return this.bulkInsertTrack(uri, values);
        }
        return 0;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        switch (this.mUriMatcher.match(uri)) {
        case QI_PLAYLIST:
            return this.insertPlaylist(uri, values);
        case QID_PLAYLIST_TRACK:
            return this.insertPlaylistTrack(uri, values);
        }
        return null;
    }

    private int bulkInsertAlbum(final Uri uri, final ContentValues[] values) {
        final SQLiteDatabase sdb = this.mDbHelper.getWritableDatabase();

        sdb.beginTransaction();
        final SQLiteStatement stmt = sdb.compileStatement("INSERT INTO tb_album (" + "articleId, "
                                                          + "title, "
                                                          + "collectFlg"
                                                          + ") VALUES ("
                                                          + "?, "
                                                          + "?, "
                                                          + "0"
                                                          + ");");
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            stmt.bindString(1, values[i].getAsString("articleId"));
            stmt.bindString(2, values[i].getAsString("title"));
            stmt.executeInsert();
        }
        sdb.setTransactionSuccessful();
        sdb.endTransaction();
        return length;
    }

    private int bulkInsertTrack(final Uri uri, final ContentValues[] values) {
        final SQLiteDatabase sdb = this.mDbHelper.getWritableDatabase();

        sdb.beginTransaction();
        final SQLiteStatement stmt = sdb.compileStatement("INSERT INTO tb_track (" + "articleId, "
                                                          + "fileId, "
                                                          + "album, "
                                                          + "songTitle, "
                                                          + "leadArtist, "
                                                          + "trackNumber, "
                                                          + "downloadFlg"
                                                          + ") VALUES ("
                                                          + "?, "
                                                          + "?, "
                                                          + "?, "
                                                          + "?, "
                                                          + "?, "
                                                          + "?, "
                                                          + "0"
                                                          + ");");
        final int length = values.length;
        for (int i = 0; i < length; i++) {
            stmt.bindString(1, values[i].getAsString("articleId"));
            stmt.bindString(2, values[i].getAsString("fileId"));
            stmt.bindString(3, values[i].getAsString("album"));
            stmt.bindString(4, values[i].getAsString("songTitle"));
            stmt.bindString(5, values[i].getAsString("leadArtist"));
            stmt.bindLong(6, values[i].getAsLong("trackNumber"));
            stmt.executeInsert();
        }
        sdb.setTransactionSuccessful();
        sdb.endTransaction();
        return length;
    }

    private Uri insertPlaylist(final Uri uri, final ContentValues values) {
        return this.insert(uri, "tb_playlist", values);
    }

    private Uri insertPlaylistTrack(final Uri uri, final ContentValues values) {
        final List<String> segments = uri.getPathSegments();
        final String playlistId = segments.get(1);
        values.put("playlistId", playlistId);
        return this.insert(uri, "tb_playlist_track", values);
    }

    private Uri insert(final Uri uri,
                       final String table,
                       final ContentValues values) {
        final long id = this.mDbHelper.insert(table, values);
        if (id != -1) {
            return ContentUris.withAppendedId(uri, id);
        } else {
            return null;
        }
    }

    @Override
    public int update(final Uri uri,
                      final ContentValues values,
                      final String selection,
                      final String[] selectionArgs) {
        switch (this.mUriMatcher.match(uri)) {
        case QIU_TRACK:
            return this.updateTrack(uri, values, selection, selectionArgs);
        case UD_PLAYLIST_SINGLE:
            return this.updatePlaylist(uri, values);
        case UD_PLAYLIST_TRACK_SINGLE:
            return this.updatePlaylistTrack(uri, values);
        }
        return 0;
    }

    private int updateTrack(final Uri uri,
                            final ContentValues values,
                            final String selection,
                            final String[] selectionArgs) {
        return this.mDbHelper.update("tb_track",
                                     values,
                                     selection,
                                     selectionArgs);
    }

    private int updatePlaylist(final Uri uri, final ContentValues values) {
        final String _id = uri.getLastPathSegment();
        return this.mDbHelper.update("tb_playlist",
                                     values,
                                     "_id = ?",
                                     new String[] { _id });
    }

    private int updatePlaylistTrack(final Uri uri, final ContentValues values) {
        return 0;
    }

    @Override
    public int delete(final Uri uri,
                      final String selection,
                      final String[] selectionArgs) {
        switch (this.mUriMatcher.match(uri)) {
        case UD_PLAYLIST_SINGLE:
            return this.deletePlaylist(uri);
        case UD_PLAYLIST_TRACK_SINGLE:
            return this.deletePlaylistTrack(uri);
        }
        return 0;
    }

    private int deletePlaylist(final Uri uri) {
        final String _id = uri.getLastPathSegment();
        final String table = "tb_playlist";
        final String selection = "_id = ?";
        final String[] selectionArgs = new String[] { _id };
        return this.mDbHelper.delete(table, selection, selectionArgs);
    }

    private int deletePlaylistTrack(final Uri uri) {
        final String _id = uri.getLastPathSegment();
        final String selection = "_id = ?";
        final String[] selectionArgs = new String[] { _id };
        return this.mDbHelper.delete("tb_playlist_track",
                                     selection,
                                     selectionArgs);
    }

    @Override
    public String getType(final Uri uri) {
        return null;
    }

}
