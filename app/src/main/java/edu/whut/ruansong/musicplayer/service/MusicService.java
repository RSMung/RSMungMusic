package edu.whut.ruansong.musicplayer.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Random;

import edu.whut.ruansong.musicplayer.tool.PlayHistory;
import edu.whut.ruansong.musicplayer.tool.Song;
import edu.whut.ruansong.musicplayer.activity.DisplayActivity;
import edu.whut.ruansong.musicplayer.activity.LoginActivity;

/**
 * Created by 阮 on 2018/11/18.
 * 音乐service，后台处理播放逻辑
 */

public class MusicService extends Service {

    //广播标识
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTTION_UPDATE";

    //播放器状态
    public static final int STATUS_PLAYING = 0;
    public static final int STATUS_PAUSED = 1;
    public static final int STATUS_STOPPED = 2;
    public static final int STATUS_COMPLETED = 3;

    //播放控制命令
    public static final int COMMAND_UNKNOWN = -1;
    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PAUSE = 1;
    public static final int COMMAND_STOP = 2;
    public static final int COMMAND_RESUME = 3;
    public static final int COMMAND_PREVIOUS = 4;
    public static final int COMMAND_NEXT = 5;
    public static final int COMMAND_CHECK_IS_PLAYING = 6;
    //播放顺序命令
    public static final int PLAY_MODE_ORDER = 8;//顺序播放(默认是它)
    public static final int PLAY_MODE_LOOP = 9;//单曲循环
    public static final int PLAY_MODE_RANDOM = 10;//随机播放

    //广播接收器内部类
    private CommandReceiver commandReceiver;
    //媒体播放器
    private MediaPlayer player = new MediaPlayer();
    //当前播放模式
    private int current_PlayMode = PLAY_MODE_ORDER;
    //当前歌曲序号，从0开始
    private static int current_number = -1;
    //下一首歌的序号
    private int next_number = 0;
    //歌曲文件路径
    private String path;
    //播放器状态
    private static int current_status = MusicService.STATUS_STOPPED;



    public static int getCurrent_number() { return current_number; }
    public static int getCurrent_status() { return current_status; }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**服务创建的时候调用*/
    @Override
    public void onCreate() {
        super.onCreate();
        //绑定广播接收器
        commandReceiver = new CommandReceiver();
        IntentFilter intentFilter = new IntentFilter(BROADCAST_MUSICSERVICE_CONTROL);
        registerReceiver(commandReceiver, intentFilter);

        Log.w("MusicService", "服务的onCreate被执行了");

        //监听播放是否完成
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.w("DisplayActivity", "已经播放完毕，播放下一首！");
                //更新播放器状态
                sendBroadcastOnStatusChange(MusicService.STATUS_COMPLETED);
                //下一首歌
                nextMusic_update_number();
            }
        });//监听播放是否完成
    }

    /**内部类，接受广播命令并执行操作*/
    class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取命令
            //COMMAND_UNKNOWN是默认值
            int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
            Log.w("MusicService", "获取到了命令广播" + "+" + command);
            //执行命令
            switch (command) {
                case COMMAND_PLAY:
                    next_number = intent.getIntExtra("number", 0);//获取点击位置
                    play_pause();
                    break;
                case COMMAND_RESUME:
                    next_number = intent.getIntExtra("number", 0);//获取点击位置
                    resume();
                    break;
                case COMMAND_PREVIOUS:
                    Log.w("MusicService", "上一首");
                    moveNumberToPrevious();
                    break;
                case COMMAND_NEXT:
                    Log.w("MusicService", "下一首");
                    nextMusic_update_number();
                    break;
                case COMMAND_PAUSE:
                    Log.w("MusicService", "暂停");
                    pause();
                    break;
                case COMMAND_STOP:
                    Log.w("MusicService", "停止");
                    stop();
                    break;
                case COMMAND_CHECK_IS_PLAYING:
                    Log.w("MusicService", "状态查询中");
                    if (player != null && player.isPlaying())
                        sendBroadcastOnStatusChange(MusicService.STATUS_PLAYING);
                    else if(current_number==-1)
                        sendBroadcastOnStatusChange(MusicService.STATUS_STOPPED);
                    else
                        sendBroadcastOnStatusChange(MusicService.STATUS_PAUSED);
                    break;
                case PLAY_MODE_ORDER://顺序播放
                    current_PlayMode = PLAY_MODE_ORDER;
                    sendBroadcastOnStatusChange(MusicService.PLAY_MODE_ORDER);
                    break;
                case PLAY_MODE_LOOP://单曲循环
                    current_PlayMode = PLAY_MODE_LOOP;
                    sendBroadcastOnStatusChange(MusicService.PLAY_MODE_LOOP);
                    break;
                case PLAY_MODE_RANDOM://随机播放
                    current_PlayMode = PLAY_MODE_RANDOM;
                    sendBroadcastOnStatusChange(MusicService.PLAY_MODE_RANDOM);
                    break;
                case COMMAND_UNKNOWN:
                    break;
                default:
                    break;
            }
        }
    }

    /**发送广播更新播放器状态改变*/
    private void sendBroadcastOnStatusChange(int status) {
        Intent intent = new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        //加入状态信息
        intent.putExtra("status", status);
        Log.w("MusicService", "播放器状态改变 " + status);
        //发送
        sendBroadcast(intent);
    }

    /**下一首歌*/
    private void nextMusic_update_number() {
        switch (current_PlayMode) {
            case PLAY_MODE_ORDER://默认顺序播放
                next_number = current_number + 1;
                break;
            case PLAY_MODE_LOOP://单曲循环
                next_number = current_number;
                sendBroadcastOnStatusChange(PLAY_MODE_LOOP);
                break;
            case PLAY_MODE_RANDOM://随机播放
                //用当前歌曲序号做随机数种子
                Random random = new Random(current_number);
                //产生0-Song_total_number的随机数
                next_number = random.nextInt(DisplayActivity.getSong_total_number());
                break;
            default:
                break;
        }
        //顺序播放时可能序号越界
        if (next_number == (DisplayActivity.getSong_total_number())) {
            Toast.makeText(MusicService.this, "已经达到了列表底部!", Toast.LENGTH_SHORT).show();
            next_number--;
            play();
        }else
            play();//不越界则直接播放
    }

    /**应用next_number参数播放歌曲*/
    public void play() {
        Song song = DisplayActivity.getSongsList().get(next_number);//通过序号拿Song对象
        path = song.getDataPath();//获取歌曲文件地址
        PlayHistory.addSong(song);//加入历史记录列表
        prePlay();
        //更新播放器状态
        current_status = MusicService.STATUS_PLAYING;
        //通知主界面播放器状态更改
        sendBroadcastOnStatusChange(MusicService.STATUS_PLAYING);
        //更新当前歌曲序号
        current_number = next_number;
    }

    /**进入游离态到开始播放*/
    public void prePlay() {
        try {
            player.reset();//进入游离态
            player.setDataSource(path);//进入初始态
            player.prepare();//进入准备好的状态
            player.start();//开始播放
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**收到COMMAND_PLAY后的动作
     * next_number是主界面的current_music_list_number*/
    private void play_pause() {
        if (player != null && player.isPlaying()) {
            if (next_number == current_number) {//点击的正在播放的歌曲
                pause();
                Log.w("MusicService", "点击正在播放的歌曲，暂停播放");
            } else //点击了新歌曲
                play();
        } else {//没有在播放歌曲，直接播放
            Log.w("MusicService", "没有在播放歌曲，直接播放" + next_number);
            play();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MusicService", "onDestroy executed");
        if (player != null) {
            player.release();
        }
        Log.w("MusicService", "命令接收器已经被取消注册，服务被销毁");
        unregisterReceiver(commandReceiver);
    }

    private void moveNumberToPrevious() {
        next_number = next_number -1 ;
        //判断是否到达顶端
        if (next_number == 0) {
            Toast.makeText(MusicService.this, "已经达到了列表顶部!", Toast.LENGTH_SHORT).show();
        } else {
            next_number = next_number +1;
            play_pause();
        }
    }

    /**暂停播放*/
    private void pause() {
        //播放器暂停
        player.pause();
        //更新播放器状态
        current_status = MusicService.STATUS_PAUSED;
        sendBroadcastOnStatusChange(MusicService.STATUS_PAUSED);
    }

    private void stop() {
        if (player.isPlaying()) {
            player.stop();
            current_status = MusicService.STATUS_STOPPED;
            sendBroadcastOnStatusChange(MusicService.STATUS_STOPPED);
        }
        LoginActivity.setLogin_status(0);
    }

    private void resume() {//暂停后的恢复播放
        player.start();
        current_status = MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChange(MusicService.STATUS_PLAYING);
    }
}