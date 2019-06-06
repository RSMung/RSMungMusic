package edu.whut.ruansong.musciplayer;

/**
 * Created by 阮 on 2018/11/17.
 */

public class Song {
    private long id;
    private int song_list_id, song_imageid;//在listview里面的位置      左侧图片在drawable里面的id
    private long album_id;//专辑id
    private String song_name, song_author, song_addr;//歌曲名称  歌手   文件路径
    private long duration;//时长
    private int isMusic;
    private String album;

    public Song(long id,long album_id,
                int song_list_id,//在listview里面的位置
                int song_image_id,//左侧图片在drawable里面的id
                String song_name,String song_author,String song_addr,
                long duration,int isMusic,String album) {
        this.id = id;
        this.album_id = album_id;
        this.song_list_id = song_list_id;
        this.song_imageid = song_image_id;
        this.song_name = song_name;
        this.song_author = song_author;
        this.song_addr = song_addr;
        this.duration = duration;
        this.isMusic = isMusic;
        this.album = album;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setAlbum_id(long album_id) {
        this.album_id = album_id;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getIsMusic() {
        return isMusic;
    }

    public void setIsMusic(int isMusic) {
        this.isMusic = isMusic;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getAlbum_id() {return album_id;}

    public void setAlbum_id(int album_id) {this.album_id = album_id;}

    public int getSong_image_id() { return song_imageid;}

    public void setSong_image_id(int song_imageid) {
        this.song_imageid = song_imageid;
    }

    public int getSong_list_id() {
        return this.song_list_id;
    }

    public String getSong_name() {
        return this.song_name;
    }

    public String getSong_author() {
        return this.song_author;
    }

    public void setSong_list_id(int i) {
        this.song_list_id = i;
    }

    public void setSong_name(String str) {
        this.song_name = str;
    }

    public void setSong_author(String str) {
        this.song_author = str;
    }

    public String getSong_addr() {
        return song_addr;
    }

    public void setSong_addr(String song_addr) {
        this.song_addr = song_addr;
    }
}
