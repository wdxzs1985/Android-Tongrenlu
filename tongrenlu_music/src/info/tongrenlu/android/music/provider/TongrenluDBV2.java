package info.tongrenlu.android.music.provider;

import android.database.sqlite.SQLiteDatabase;

public class TongrenluDBV2 extends TongrenluDBV1 {

    public static final int DB_VERSION_2 = 2; //

    public static final String CREATE_TB_ALBUM = "create table tb_album (" + "_id integer primary key autoincrement,"
            + "article_id text,"
            + "title text,"
            + "collect integer)";

    @Override
    public String getName() {
        return DB_NAME;
    }

    @Override
    public int getVersion() {
        return DB_VERSION_2;
    }

    /*** 构造一个数据库，如果没有就创建一个数据库 ***/
    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(CREATE_TB_TRACK);
        db.execSQL(CREATE_TB_ALBUM);
        db.execSQL(CREATE_TB_PLAYLIST);
        db.execSQL(CREATE_TB_PLAYLIST_TRACK);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        if (oldVersion <= DB_VERSION_1) {
            db.execSQL(CREATE_TB_ALBUM);
        }
    }

}
