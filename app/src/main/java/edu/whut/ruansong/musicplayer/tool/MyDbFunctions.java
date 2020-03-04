package edu.whut.ruansong.musicplayer.tool;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import edu.whut.ruansong.musicplayer.model.Song;

public class MyDbFunctions {
    //数据库名
    public static final String DB_NAME = "Mung_music";
    //数据库版本
    public static final int VERSION = 1;
    private static MyDbFunctions myDbFunctions;
    private SQLiteDatabase db;
    //私有化构造方法,单例模式
    private MyDbFunctions(Context context){
        db = new MyDbHelper(context,DB_NAME,null,VERSION).getWritableDatabase();
    }
    /**
     * 获取GeneralDbFunctions的实例*/
    public synchronized static MyDbFunctions getInstance(Context context){
        if(myDbFunctions == null){
            myDbFunctions = new MyDbFunctions(context);
        }
        return myDbFunctions;
    }
    /**
     * 将Song实例存储到数据库的MyLoveSongs表中*/
    public void saveSong(Song song){
        if(song != null && db != null){
            ContentValues values = new ContentValues();
            values.put("title",song.getTitle());
            values.put("artist",song.getArtist());
            values.put("dataPath",song.getDataPath());
            values.put("listId",song.getList_id_display());
            db.insert("MyLoveSongs",null,values);
        }
    }
    /**
     * 将Song实例从数据库的MyLoveSongs表中删除*/
    public void removeSong(Song song){
        if(song != null && db != null){
            //db.execSQL("delete from lxrData where name=?", new String[] { name });
            db.delete("MyLoveSongs","title=?",new String[]{song.getTitle()});
        }
    }
    /**
     * 从数据库读取MyLoveSongs表中所有的我喜爱的歌曲*/
    public List<Song> loadMyLoveSongs(){
        List<Song> list = new ArrayList<>();
        if(db != null){
            Cursor cursor = db.query("MyLoveSongs",null,null,null,null,null,null);
            if(cursor.moveToFirst()){
                do{
                    Song song = new Song();
                    song.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                    song.setArtist(cursor.getString(cursor.getColumnIndex("artist")));
                    song.setDataPath(cursor.getString(cursor.getColumnIndex("dataPath")));
                    song.setList_id_display(cursor.getInt(cursor.getColumnIndex("listId")));
                    list.add(song);
                }while(cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }
}
