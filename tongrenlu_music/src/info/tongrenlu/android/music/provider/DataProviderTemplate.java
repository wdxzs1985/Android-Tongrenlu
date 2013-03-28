package info.tongrenlu.android.music.provider;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class DataProviderTemplate {

    public static final String SINGLE_SELECTION = "_ID = ?";

    private final DBHelper mDBHelper;
    private final String mTable;

    public DataProviderTemplate(final DBHelper dBHelper, final String table) {
        this.mDBHelper = dBHelper;
        this.mTable = table;
    }

    public Cursor query(final String[] projection,
                        final String selection,
                        final String[] selectionArgs,
                        final String sortOrder) {
        return this.mDBHelper.query(this.mTable,
                                    projection,
                                    selection,
                                    selectionArgs,
                                    sortOrder);
    }

    public Cursor querySingle(final String[] projection, final String _id) {
        final String[] selectionArgs = new String[] { _id };
        return this.mDBHelper.query(this.mTable,
                                    projection,
                                    DataProviderTemplate.SINGLE_SELECTION,
                                    selectionArgs,
                                    null);
    }

    public Uri insert(final Uri contentUri, final ContentValues values) {
        final long id = this.mDBHelper.insert(this.mTable, values);
        if (id != -1) {
            return ContentUris.withAppendedId(contentUri, id);
        } else {
            return null;
        }
    }

    public int update(final ContentValues values,
                      final String selection,
                      final String[] selectionArgs) {
        return this.mDBHelper.update(this.mTable,
                                     values,
                                     selection,
                                     selectionArgs);
    }

    public int updateSingle(final ContentValues values, final String _id) {
        final String[] selectionArgs = new String[] { _id };
        return this.mDBHelper.update(this.mTable,
                                     values,
                                     DataProviderTemplate.SINGLE_SELECTION,
                                     selectionArgs);
    }

    public int delete(final String selection, final String[] selectionArgs) {
        return this.mDBHelper.delete(this.mTable, selection, selectionArgs);
    }

    public int deleteSingle(final String _id) {
        final String[] selectionArgs = new String[] { _id };
        return this.mDBHelper.delete(this.mTable,
                                     DataProviderTemplate.SINGLE_SELECTION,
                                     selectionArgs);
    }

}
