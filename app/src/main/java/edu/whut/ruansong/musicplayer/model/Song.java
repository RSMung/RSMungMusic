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
    private boolean isLove;//是否是喜爱的歌曲
    private Bitmap album_icon;
    private boolean isDefaultAlbumIcon;

    public Song(){}
    //歌名，歌手，时长，专辑，图标资源id,歌曲文件路径,在DisplayActivity中的listview的位置
    public Song(
            String title,
            String artist,
            long duration,
            String dataPath,
            boolean isLove,
            Bitmap album_icon,
            boolean isDefaultAlbumIcon
            )
    {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.dataPath = dataPath;
        this.isLove = isLove;
        this.album_icon = album_icon;
        this.isDefaultAlbumIcon = isDefaultAlbumIcon;
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

    public String getTitle() { return this.title; }

    public String getArtist() { return this.artist; }

    public long getDuration() { return duration; }

    public String getDataPath() { return dataPath; }

    public boolean isLove() {
        return isLove;
    }

    public boolean isDefaultAlbumIcon() {
        return isDefaultAlbumIcon;
    }

    public void setFlagDefaultAlbumIcon(boolean flagDefaultAlbumIcon) {
        isDefaultAlbumIcon = flagDefaultAlbumIcon;
    }
}
