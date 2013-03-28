package info.tongrenlu.android.music.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class DBHelper extends SQLiteOpenHelper {
    //
    private static final String DB_NAME = "tongrenlu.db";

    //

    /*** 构造函数 **/
    public DBHelper(final Context context, final int version) {
        super(context, DBHelper.DB_NAME, null, version);
    }

    /**
     * 插入数据
     * 
     * @return
     **/
    public long insert(final String tableName, final ContentValues values) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final long _id = db.insert(tableName, null, values);
        return _id;
    }

    /*** 更新数据 */
    public int update(final String tableName,
                      final ContentValues values,
                      final String selection,
                      final String[] selectionArgs) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final int rows = db.update(tableName, values, selection, selectionArgs);
        return rows;
    }

    /** 删除数据 */
    public int delete(final String tableName,
                      final String selection,
                      final String[] selectionArgs) {
        final SQLiteDatabase db = this.getWritableDatabase();
        final int rows = db.delete(tableName, selection, selectionArgs);
        return rows;
    }

    /***
     * 查找数据
     * 
     * @param sortOrder
     * @param projection
     */
    public Cursor query(final String tableName,
                        final String[] projection,
                        final String selection,
                        final String[] selectionArgs,
                        final String sortOrder) {
        final SQLiteDatabase db = this.getReadableDatabase();
        final Cursor c = db.query(tableName,
                                  projection,
                                  selection,
                                  selectionArgs,
                                  null,
                                  null,
                                  sortOrder);
        return c;
    }

}
