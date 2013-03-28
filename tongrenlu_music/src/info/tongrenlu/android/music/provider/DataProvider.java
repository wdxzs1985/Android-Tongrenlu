package info.tongrenlu.android.music.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class DataProvider extends ContentProvider {

    public static final String AUTHORITY = "info.tongrenlu.android.music.provider";
    private static final Uri BASE_URI = Uri.parse("content://"
                                                  + DataProvider.AUTHORITY);
    private static final int URI_ROOT_CODE = 0;
    //
    private static final String URI_TRACK_PATH = "tb_track";
    public static final Uri URI_TRACK = Uri.withAppendedPath(DataProvider.BASE_URI,
                                                             DataProvider.URI_TRACK_PATH);
    private static final int URI_TRACK_CODE = 1;
    //
    private static final String URI_TRACK_SINGLE_PATH = "tb_track/#";
    public static final Uri URI_TRACK_SINGLE = Uri.withAppendedPath(DataProvider.BASE_URI,
                                                                    DataProvider.URI_TRACK_SINGLE_PATH);
    private static final int URI_TRACK_SINGLE_CODE = 2;
    //
    private static final String URI_PLAYLIST_PATH = "tb_playlist";
    public static final Uri URI_PLAYLIST = Uri.withAppendedPath(DataProvider.BASE_URI,
                                                                DataProvider.URI_PLAYLIST_PATH);
    private static final int URI_PLAYLIST_CODE = 3;
    //
    private static final String URI_PLAYLIST_SINGLE_PATH = "tb_playlist/#";
    public static final Uri URI_PLAYLIST_SINGLE = Uri.withAppendedPath(DataProvider.BASE_URI,
                                                                       DataProvider.URI_PLAYLIST_SINGLE_PATH);
    private static final int URI_PLAYLIST_SINGLE_CODE = 4;
    //
    private boolean created = false;
    private UriMatcher mUriMatcher = null;
    private DBHelper mDBHelper = null;
    //
    private DataProviderTemplate playlistProvider = null;
    private DataProviderTemplate trackProvider = null;

    @Override
    public boolean onCreate() {
        this.mUriMatcher = new UriMatcher(DataProvider.URI_ROOT_CODE);
        this.mUriMatcher.addURI(DataProvider.AUTHORITY,
                                DataProvider.URI_PLAYLIST_PATH,
                                DataProvider.URI_PLAYLIST_CODE);
        this.mUriMatcher.addURI(DataProvider.AUTHORITY,
                                DataProvider.URI_PLAYLIST_SINGLE_PATH,
                                DataProvider.URI_PLAYLIST_SINGLE_CODE);
        this.mUriMatcher.addURI(DataProvider.AUTHORITY,
                                DataProvider.URI_TRACK_PATH,
                                DataProvider.URI_TRACK_CODE);
        this.mUriMatcher.addURI(DataProvider.AUTHORITY,
                                DataProvider.URI_TRACK_SINGLE_PATH,
                                DataProvider.URI_TRACK_SINGLE_CODE);

        this.mDBHelper = new DBHelperV1(this.getContext());
        //
        this.trackProvider = new DataProviderTemplate(this.mDBHelper,
                                                      DataProvider.URI_TRACK_PATH);
        this.playlistProvider = new DataProviderTemplate(this.mDBHelper,
                                                         DataProvider.URI_PLAYLIST_PATH);

        this.created = true;
        return this.created;
    }

    @Override
    public Cursor query(final Uri uri,
                        final String[] projection,
                        final String selection,
                        final String[] selectionArgs,
                        final String sortOrder) {
        switch (this.mUriMatcher.match(uri)) {
        case URI_PLAYLIST_CODE:
            return this.playlistProvider.query(projection,
                                               selection,
                                               selectionArgs,
                                               sortOrder);
        case URI_TRACK_CODE:
            return this.trackProvider.query(projection,
                                            selection,
                                            selectionArgs,
                                            sortOrder);
        case URI_TRACK_SINGLE_CODE:
            final String _id = uri.getLastPathSegment();
            return this.trackProvider.querySingle(projection, _id);
        }
        return null;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        Uri newUri = null;
        switch (this.mUriMatcher.match(uri)) {
        case URI_PLAYLIST_CODE:
            newUri = this.playlistProvider.insert(uri, values);
            break;
        case URI_TRACK_CODE:
            newUri = this.trackProvider.insert(uri, values);
            break;
        }
        if (newUri != null) {
            this.getContext().getContentResolver().notifyChange(uri, null);
        }
        return newUri;
    }

    @Override
    public int update(final Uri uri,
                      final ContentValues values,
                      final String selection,
                      final String[] selectionArgs) {
        int rows = 0;
        switch (this.mUriMatcher.match(uri)) {
        case URI_PLAYLIST_SINGLE_CODE:
            rows = this.playlistProvider.updateSingle(values,
                                                      uri.getLastPathSegment());
            break;
        case URI_TRACK_SINGLE_CODE:
            rows = this.trackProvider.updateSingle(values,
                                                   uri.getLastPathSegment());
            break;
        }
        if (rows > 0) {
            this.getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    @Override
    public int delete(final Uri uri,
                      final String selection,
                      final String[] selectionArgs) {
        int rows = 0;
        switch (this.mUriMatcher.match(uri)) {
        case URI_PLAYLIST_SINGLE_CODE:
            rows = this.playlistProvider.deleteSingle(uri.getLastPathSegment());
            break;
        case URI_TRACK_CODE:
            rows = this.trackProvider.delete(selection, selectionArgs);
            break;
        case URI_TRACK_SINGLE_CODE:
            rows = this.trackProvider.deleteSingle(uri.getLastPathSegment());
            break;
        }
        if (rows > 0) {
            this.getContext().getContentResolver().notifyChange(uri, null);
        }
        return rows;
    }

    @Override
    public String getType(final Uri uri) {
        return null;
    }

}
