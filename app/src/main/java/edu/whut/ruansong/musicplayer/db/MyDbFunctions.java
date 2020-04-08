package edu.whut.ruansong.musicplayer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.tool.PictureDealHelper;

public class MyDbFunctions {
    //数据库名
    public static final String DB_NAME = "Mung_Music";
    //数据库版本
    public static final int VERSION = 1;
    private volatile static MyDbFunctions myDbFunctions;
    private SQLiteDatabase db;
    private Context context;
    //私有化构造方法,单例模式
    private MyDbFunctions(Context context){
        db = new MyDbHelper(context,DB_NAME,null,VERSION).getWritableDatabase();
        this.context = context;
    }
    /*双重锁模式*/
    public static MyDbFunctions getInstance(Context context){
        if(myDbFunctions == null){//为了避免不必要的同步
            synchronized (MyDbFunctions.class){
                if(myDbFunctions ==null){//为了在实例为空时才创建实例
                    myDbFunctions = new MyDbFunctions(context);
                }
            }
        }
        return myDbFunctions;
    }
    /**
     * 将Song实例存储到数据库的SONGS表中*/
    public void saveSong(Song song){
        if(song != null && db != null){
            ContentValues values = new ContentValues();
            values.put("title",song.getTitle());
            values.put("artist",song.getArtist());
            values.put("duration",song.getDuration());
            values.put("dataPath",song.getDataPath());
            values.put("listId",song.getList_id_display());
            if(song.isLove())
                values.put("isLove","true");
            else
                values.put("isLove","false");
            db.insert("SONGS",null,values);
        }
    }
    /**
     * 将Song实例从数据库的MyLoveSongs表中删除*/
    public void removeSong(Song song){
        if(song != null && db != null){
            //db.execSQL("delete from lxrData where name=?", new String[] { name });
            db.delete("SONGS","dataPath=?",new String[]{song.getDataPath()});
        }
    }
    /**
     * 给SONGS表中的某个歌曲修改isLove标志*/
    public void setLove(Song song,String flag){
        ContentValues values = new ContentValues();
        values.put("isLove",flag);
        db.update("SONGS",values,"dataPath=?",new String[]{song.getDataPath()});
    }
    /**
     * 从数据库读取SONGS表中所有的我喜爱的歌曲*/
    public ArrayList<Song> loadMyLoveSongs(){
        ArrayList<Song> list = new ArrayList<>();
        if(db != null){
            Cursor cursor = db.query("SONGS",null,"isLove =?",new String[]{"true"},null,null,null);
            if(cursor.moveToFirst()){
                do{
                    Song song = new Song();
                    song.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                    song.setArtist(cursor.getString(cursor.getColumnIndex("artist")));
                    song.setDuration(cursor.getLong(cursor.getColumnIndex("duration")));
                    song.setDataPath(cursor.getString(cursor.getColumnIndex("dataPath")));
                    song.setList_id_display(cursor.getInt(cursor.getColumnIndex("listId")));
                    song.setLove(true);
                    song.setAlbum_icon(PictureDealHelper.getAlbumPicture(context,song.getDataPath(),96,96));
                    list.add(song);
                }while(cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }
    /**
     * 读取数据库中的所有歌曲*/
    public ArrayList<Song> loadAllSongs(){
        ArrayList<Song> list = new ArrayList<>();
        if(db != null){
            Cursor cursor = db.query("SONGS",null,null,null,null,null,null);
            if(cursor.moveToFirst()){
                do{
                    Song song = new Song();
                    song.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                    song.setArtist(cursor.getString(cursor.getColumnIndex("artist")));
                    song.setDuration(cursor.getLong(cursor.getColumnIndex("duration")));
                    song.setDataPath(cursor.getString(cursor.getColumnIndex("dataPath")));
                    song.setList_id_display(cursor.getInt(cursor.getColumnIndex("listId")));
                    String flag = cursor.getString(cursor.getColumnIndex("isLove"));
                    if(flag.equals("true"))
                        song.setLove(true);
                    else
                        song.setLove(false);
                    song.setAlbum_icon(PictureDealHelper.getAlbumPicture(context,song.getDataPath(),96,96));
                    list.add(song);
                }while(cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    /**
     * 判断当前SONGS表中是否有数据*/
    public boolean isSONGS_Null(){
        if(db != null){
            Cursor cursor = db.query("SONGS",null,null,null,null,null,null);
            if(cursor.moveToFirst()){
                return false;//不为空
            }
            cursor.close();
        }
        return true;//空
    }
}
