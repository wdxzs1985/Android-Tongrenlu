package info.tongrenlu.android.music.provider;

import info.tongrenlu.android.provider.DatabaseBuilder;
import android.database.sqlite.SQLiteDatabase;

public class TongrenluDBV2 implements DatabaseBuilder {

    public static final String DB_NAME = "tongrenlu.db";
    public static final int DB_VERSION_2 = 2; //

    public static final String CREATE_TB_ALBUM = "create table " + "tb_album ("
            + "_id integer primary key autoincrement,"
            + "articleId text,"
            + "title text"
            + ")";
    public static final String CREATE_TB_TRACK = "create table " + "tb_track ("
            + "_id integer primary key autoincrement,"
            + "articleId text,"
            + "fileId text,"
            + "album text,"
            + "name text,"
            + "artist text,"
            + "original text,"
            + "trackNumber integer,"
            + "downloadFlg text"
            + ")";
    public static final String CREATE_TB_PLAYLIST = "create table " + "tb_playlist ("
            + "_id integer primary key autoincrement,"
            + "title text"
            + ")";
    public static final String CREATE_TB_PLAYLIST_TRACK = "create table " + "tb_playlist_track ("
            + "_id integer primary key autoincrement,"
            + "playlistId integer,"
            + "articleId text,"
            + "fileId text,"
            + "name text,"
            + "artist text,"
            + "trackNumber integer"
            + ")";

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
        db.execSQL(TongrenluDBV2.CREATE_TB_ALBUM);
        db.execSQL(TongrenluDBV2.CREATE_TB_TRACK);
        db.execSQL(TongrenluDBV2.CREATE_TB_PLAYLIST);
        db.execSQL(TongrenluDBV2.CREATE_TB_PLAYLIST_TRACK);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db,
                          final int oldVersion,
                          final int newVersion) {
        if (oldVersion == TongrenluDBV1.DB_VERSION_1) {
            db.execSQL("drop table tb_album");
            db.execSQL("drop table tb_track");
            db.execSQL("drop table tb_playlist");
            db.execSQL("drop table tb_playlist_track");
        }
        this.onCreate(db);
    }

}
