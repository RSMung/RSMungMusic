package edu.whut.ruansong.musciplayer;

/**
 * Created by 阮阮 on 2018/11/17.
 */

public class Song {
    private int song_id, song_imageid;
    private String song_name, song_author, song_addr;

    public Song(int song_id, int song_imageidid, String song_name, String song_author, String song_addr) {
        this.song_id = song_id;
        this.song_imageid = song_imageidid;
        this.song_name = song_name;
        this.song_author = song_author;
        this.song_addr = song_addr;
    }

    public int getSong_imageid() {
        return song_imageid;
    }

    public void setSong_imageid(int song_imageid) {
        this.song_imageid = song_imageid;
    }

    public int getSong_id() {
        return this.song_id;
    }

    public String getSong_name() {
        return this.song_name;
    }

    public String getSong_author() {
        return this.song_author;
    }

    public void setSong_id(int i) {
        this.song_id = i;
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
