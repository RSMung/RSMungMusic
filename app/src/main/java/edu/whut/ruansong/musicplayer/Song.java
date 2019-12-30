package edu.whut.ruansong.musicplayer;

/**
 * Created by 阮 on 2018/11/17.
 */

public class Song {
    private String title;//歌名
    private String artist;//  歌手
    private long duration;//时长
    private long album_id;//专辑id
    private int song_item_picture;//列表每一项的图标，为了好看加上的
    private String dataPath;//歌曲文件路径
    private int list_id_display;

    //歌名，歌手，时长，专辑，图标资源id,歌曲文件路径,在DisplayActivity中的listview的位置
    public Song(
            String title,
            String artist,
            long duration,
            long album_id,
            int song_item_picture,
            String dataPath,
            int list_id_display
            )
    {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.album_id = album_id;
        this.song_item_picture = song_item_picture;
        this.dataPath = dataPath;
        this.list_id_display = list_id_display;
    }

    public void setTitle(String str) { this.title = str; }

    public void setArtist(String str) { this.artist = str; }

    public void setDuration(long duration) { this.duration = duration; }

    public void setAlbum_id(long album_id) { this.album_id = album_id; }

    public void setSong_item_picture(int song_item_picture) { this.song_item_picture = song_item_picture; }

    public void setDataPath(String dataPath) { this.dataPath = dataPath; }

    public void setList_id_display(int list_id_display) { this.list_id_display = list_id_display; }


    public String getTitle() { return this.title; }

    public String getArtist() { return this.artist; }

    public long getDuration() { return duration; }

    public long getAlbum_id() {return album_id;}

    public int getSong_item_picture() { return song_item_picture; }

    public String getDataPath() { return dataPath; }

    public int getList_id_display() { return list_id_display; }

}
