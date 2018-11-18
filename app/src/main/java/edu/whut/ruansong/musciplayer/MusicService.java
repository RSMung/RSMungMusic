package edu.whut.ruansong.musciplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Created by 阮阮 on 2018/11/18.
 */

public class MusicService extends Service {
    //播放控制命令
    public static final int COMMAND_UNKNOWN = -1;
    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PAUSE = 1;
    public static final int COMMAND_STOP = 2;
    public static final int COMMAND_RESUME = 3;
    public static final int COMMAND_PREVIOUS = 4;
    public static final int COMMAND_NEXT = 5;
    public static final int COMMAND_CHECK_IS_PLAYING = 6;
    public static final int COMMAND_SEEKTO = 7;
    //播放器状态
    public static final int STATUS_PLAYING = 0;
    public static final int COMMAND_PAUSED = 1;
    public static final int COMMAND_STOPPED = 2;
    public static final int COMMAND_COMPLETED = 3;
    //广播标识
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTTION_UPDATE";

    //歌曲序号，从0开始
    private int number = 0;
    private int status;
    //媒体播放器
    private MediaPlayer player = new MediaPlayer();

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /*内部类，接受广播命令并执行操作*/
    class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取命令
            int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
            //执行命令
            switch (command) {
                case COMMAND_PLAY:
                    number = intent.getIntExtra("number", 0);
                    play(number);
                    break;
                case COMMAND_PREVIOUS:
                    moveNumberToPrevious();
                    break;
                case COMMAND_NEXT:
                    moveNumberToNext();
                    break;
                case COMMAND_PAUSE:
                    pause();
                    break;
                case COMMAND_STOP:
                    stop();
                    break;
                case COMMAND_CHECK_IS_PLAYING:
                    break;
                case COMMAND_UNKNOWN:
                default:
                        break;
            }
        }
    }

    private void load(int number) {
        try {
            player.reset();
            player.setDataSource(DisplayActivity.getSongsList().get(number).getSong_addr());
            player.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void play(int number) {
        if (player != null && player.isPlaying()) {
            player.stop();
        }
        load(number);
        player.start();
        status = MusicService.STATUS_PLAYING;
    }
    private void moveNumberToPrevious(){
        //判断是否到达顶端
        if(number==0){
            Toast.makeText(MusicService.this,"已经达到了列表顶部!",Toast.LENGTH_SHORT).show();
        }else{
            --number;
            play(number);
        }
    }
    private void moveNumberToNext(){
        //判断是否到达了底端
        if(number==(DisplayActivity.getSongsList().size()-1)){
            Toast.makeText(MusicService.this,"已经达到了列表底部!",Toast.LENGTH_SHORT).show();
        }else{
            ++number;
            play(number);
        }
    }
    private void pause(){
        if(player.isPlaying()){
            player.pause();
            status = MusicService.STATUS_PLAYING;
        }
    }
    private void stop(){
        if(player.isPlaying()){
            player.stop();
        }
    }
}
