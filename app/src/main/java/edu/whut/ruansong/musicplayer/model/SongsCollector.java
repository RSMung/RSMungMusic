package edu.whut.ruansong.musicplayer.model;

import java.util.ArrayList;
import java.util.Iterator;

public class SongsCollector {
    public static int song_total_number = 0;
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
        song_total_number++;
    }
    /**
     * 从songs中移除Song对象*/
    public static void removeSong(int index){
        mySongs.remove(index);
        song_total_number--;
    }
    //重载
    public static void removeSong(String dataPath){
        for (int i=0; i < mySongs.size(); i++) {
            Song s = mySongs.get(i);
            if (s.getDataPath().equals(dataPath)) {
                mySongs.remove(i);
                song_total_number--;
                break;
            }
        }
    }
    /**
     * 获取song数量*/
    public static int size(){
        return song_total_number;
    }
    /**
     * 返回songs_list对象*/
    public static ArrayList<Song> getSongsList(){
        return mySongs;
    }
    /**
     * 设置songs_list对象*/
    public static void setSongsList(ArrayList<Song> songs){
        mySongs = songs;
        song_total_number = songs.size();
    }

    /**
     * 判断是否已经有这首歌曲*/
    public static boolean isContainSong(String dataPath){
        for (Song mySong : mySongs) {
            if (mySong.getDataPath().equals(dataPath)) {
                return true;
            }
        }
        return false;
    }

    public static int getSongIndex(Song song){
       int result = 0;
       for(int i=0; i < mySongs.size(); i++){
           if(mySongs.get(i).getDataPath().equals(song.getDataPath())){
               result = i;
               break;
           }
       }
       return  result;
    }
}
