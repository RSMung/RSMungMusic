package edu.whut.ruansong.musicplayer.tool;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDbHelper extends SQLiteOpenHelper {
    private static final String CREATE_SONG = "create table MyLoveSongs ("
            + "id integer primary key autoincrement, "
            + "title text, "//歌名
            + "artist text, "//歌手
            + "dataPath text, "//文件路径
            + "listId integer)";//在DisplayActivity 的listView中的位置
    private Context mContext;
    public MyDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SONG);//建表
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }


}
