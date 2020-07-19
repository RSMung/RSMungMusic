package edu.whut.ruansong.musicplayer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDbHelper extends SQLiteOpenHelper {
    private static final String CREATE_SONGS =
            "create table SONGS ("
            + "id integer primary key autoincrement, "
            + "title text, "//歌名
            + "artist text, "//歌手
            + "duration integer,"//时长
            + "dataPath text, "//文件路径
            + "isLove text,"//是否是喜爱的歌曲
            + "isDefaultAlbumIcon text)";//是否使用的默认专辑图片
    private Context mContext;
    public MyDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SONGS);//建表
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }


}
