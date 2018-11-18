package edu.whut.ruansong.musciplayer;

import android.media.MediaPlayer;
import android.util.Log;
import android.widget.SeekBar;

import java.net.URL;

/**
 * Created by 阮阮 on 2018/11/17.
 */

public class MusicPlay {
    private static MediaPlayer player = new MediaPlayer();//媒体播放器
    private int num;//当前播放歌曲
    private String path;//歌曲path
    private int time;//歌曲时长

    public MusicPlay(int position,String path) {//构造函数
        this.path = path;
        if(player.isPlaying()){
            if(position==num)//点击的正在播放的歌曲
            {
                player.pause();
                Log.w("MusicPlay","点击的正在播放的歌曲"+position+",暂停播放！");
            }
            else {//点击了新歌曲
                player.stop();
                initMediaPlayer();
                play();
                Log.w("MusicPlay","点击了新歌曲,重新播放！");
            }
        }else
            {
            initMediaPlayer();
            play();
        }
        this.num=position;
    }

    public void initMediaPlayer() {
        try {
            player.reset();
            player.setDataSource(path);
            player.prepare();
            time = player.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play(){
        if(!player.isPlaying()){
            player.start();
        }else{
            player.pause();
        }
    }
    public void pause() {//暂停
        if (player.isPlaying())
            player.pause();
    }

    public void stop() {
        player.stop();
    }

    public void resume() {//暂停后恢复播放
        player.start();
    }

    public void replay() {//播放完成后重新播放
        player.start();
    }

    public static void release(){
        if(player!=null){
            player.stop();
            player.release();
        }
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }
    public int getTime(){
        return time;
    }
}
