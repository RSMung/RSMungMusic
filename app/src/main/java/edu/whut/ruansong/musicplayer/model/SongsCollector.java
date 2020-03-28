package edu.whut.ruansong.musicplayer.model;

import java.util.ArrayList;
import java.util.List;

public class SongsCollector {
    private static ArrayList<Song> mySongs = new ArrayList<>();//歌曲数据
    /**
     * 获取给定index的song*/
    public static Song getSong(int index){
        return mySongs.get(index);
    }
    /**
     * 向songs中添加Song对象*/
    public static void addSong(Song song){
        mySongs.add(song);
    }
    /**
     * 从songs中移除Song对象*/
    public static void removeSong(int index){
        mySongs.remove(index);
    }
    /**
     * 获取song数量*/
    public static int size(){
        return mySongs.size();
    }
    /**
     * 返回songs_list对象*/
    public static ArrayList<Song> getSongsList(){
        return mySongs;
    }
    /**
     * 设置songs_list对象*/
    public static void getSongsList(ArrayList<Song> songs){
        mySongs = songs;
    }
}
