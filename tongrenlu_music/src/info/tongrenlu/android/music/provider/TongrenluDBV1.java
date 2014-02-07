package info.tongrenlu.android.music.provider;

import info.tongrenlu.android.provider.DatabaseBuilder;
import android.database.sqlite.SQLiteDatabase;

public class TongrenluDBV1 implements DatabaseBuilder {

    public static final String DB_NAME = "tongrenlu.db";
    public static final int DB_VERSION = 1; //

    public static final String CREATE_TB_TRACK = "create table tb_track (" + "_id integer primary key autoincrement,"
            + "article_id text,"
            + "file_id text,"
            + "title text,"
            + "artist text,"
            + "size integer,"
            + "loaded integer)";
    public static final String CREATE_TB_PLAYLIST = "create table tb_playlist (" + "_id integer primary key autoincrement,"
            + "title text)";
    public static final String CREATE_TB_PLAYLIST_TRACK = "create table tb_playlist_track (" + "_id integer primary key autoincrement,"
            + "playlist_id integer"
            + "track_id integer)";
    public static final String DROP_TB_DOWNLOAD = "drop table tb_download";
    public static final String DROP_TB_PLAYLIST = "drop table tb_playlist";
    public static final String DROP_TB_TRACK = "drop table tb_track";

    @Override
    public String getName() {
        return DB_NAME;
    }

    @Override
    public int getVersion() {
        return DB_VERSION;
    }

    /*** 构造一个数据库，如果没有就创建一个数据库 ***/
    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(TongrenluDBV1.CREATE_TB_TRACK);
        db.execSQL(TongrenluDBV1.CREATE_TB_PLAYLIST);
        db.execSQL(TongrenluDBV1.CREATE_TB_PLAYLIST_TRACK);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
    }

}
