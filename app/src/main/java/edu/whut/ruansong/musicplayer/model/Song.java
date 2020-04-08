package edu.whut.ruansong.musicplayer.model;

import android.graphics.Bitmap;

/**
 * Created by 阮 on 2018/11/17.
 */

public class Song {
    private String title;//歌名
    private String artist;//  歌手
    private long duration;//时长
    private String dataPath;//歌曲文件路径
    private int list_id_display;
    private boolean isLove;//是否是喜爱的歌曲
    private Bitmap album_icon;

    public Song(){}
    //歌名，歌手，时长，专辑，图标资源id,歌曲文件路径,在DisplayActivity中的listview的位置
    public Song(
            String title,
            String artist,
            long duration,
            String dataPath,
            int list_id_display,
            boolean isLove,
            Bitmap album_icon
            )
    {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.dataPath = dataPath;
        this.list_id_display = list_id_display;
        this.isLove = isLove;
        this.album_icon = album_icon;
    }

    public Bitmap getAlbum_icon() {
        return album_icon;
    }

    public void setAlbum_icon(Bitmap album_icon) {
        this.album_icon = album_icon;
    }

    public void setLove(boolean love) {
        isLove = love;
    }

    public void setTitle(String str) { this.title = str; }

    public void setArtist(String str) { this.artist = str; }

    public void setDuration(long duration) { this.duration = duration; }

    public void setDataPath(String dataPath) { this.dataPath = dataPath; }

    public void setList_id_display(int list_id_display) { this.list_id_display = list_id_display; }


    public String getTitle() { return this.title; }

    public String getArtist() { return this.artist; }

    public long getDuration() { return duration; }

    public String getDataPath() { return dataPath; }

    public int getList_id_display() { return list_id_display; }

    public boolean isLove() {
        return isLove;
    }

}
