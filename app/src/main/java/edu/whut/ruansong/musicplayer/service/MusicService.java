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
import java.util.Timer;
import java.util.TimerTask;

import edu.whut.ruansong.musicplayer.model.PlayHistory;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.activity.DisplayActivity;
import edu.whut.ruansong.musicplayer.model.SongsCollector;


/**
 * Created by 阮 on 2018/11/18.
 * 音乐service，后台处理播放逻辑
 */

public class MusicService extends Service {
    //广播标识
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTTION_UPDATE";
    public static final String BROADCAST_MUSICSERVICE_PROGRESS = "MusicService.PROGRESS";
    //播放器状态
    public static final int STATUS_PLAYING = 0;
    public static final int STATUS_PAUSED = 1;
    public static final int STATUS_STOPPED = 2;
    public static final int STATUS_COMPLETED = 3;
//    public static final int STATUS_PREVIOUS = 4;
    //progressBar相关
    public static final int PROGRESS_UPDATE = 4;
    public static final int PROGRESS_DURATION = 5;
    //播放模式已更新
    public static final int PLAY_MODE_UPDATE = 6;
    //播放控制命令
    public static final int COMMAND_UNKNOWN = -1;
    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PAUSE = 1;
    //    public static final int COMMAND_STOP = 2;
    public static final int COMMAND_RESUME = 3;
    public static final int COMMAND_PREVIOUS = 4;
    public static final int COMMAND_NEXT = 5;
    public static final int COMMAND_REQUEST_DURATION = 6;//请求当前歌曲的总时长
    //播放顺序命令
    public static final int PLAY_MODE_ORDER = 8;//顺序播放(默认是它)
    public static final int PLAY_MODE_LOOP = 9;//单曲循环
    public static final int PLAY_MODE_RANDOM = 10;//随机播放
    public static final int COMMAND_SEEK_TO = 11;//seekTo控制播放命令
    //广播接收器
    private CommandReceiver commandReceiver;
    //媒体播放器
    private MediaPlayer player = null;
    /*存储用变量*/
    private int current_PlayMode = PLAY_MODE_ORDER;//当前播放模式
    private static int current_number = 0;//当前歌曲序号，从0开始
    private int duration = 0;//歌曲时长
    private int current_progress = 0;//当前歌曲播放进度
    private int next_number = 0;//下一首歌的序号
    private String song_path;//歌曲文件路径
    private Song song = null;//当前播放的song类
    private static int current_status = MusicService.STATUS_STOPPED;//播放器状态
    //用于定时更新progress进度
    private Timer timer = null;
    //通知栏
    private MungNotification myNotification;
    private Thread update_progress_thread;
    //耳机监听
    private HeadsetPlugReceiver headsetReceiver = null;

    /*方便其他类获取,用于逻辑控制*/
    public static int getCurrent_number() {
        return current_number;
    }

    public static int getCurrent_status() {
        return current_status;
    }

    public Song getSong() {
        return song;
    }
    /*生命周期*/

    /**
     * 当组件调用bindService()想要绑定到service时系统调用此方法
     * 一旦绑定后，下次再调用bindService()不会回调该方法
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * 系统在service第一次创建时执行此方法，来执行只运行一次的初始化工作
     * 如果service已经运行，这个方法不会被调用
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.w("MusicService", "进入onCreate");
        //绑定广播接收器
        commandReceiver = new CommandReceiver();
        IntentFilter intentFilter = new IntentFilter(BROADCAST_MUSICSERVICE_CONTROL);
        registerReceiver(commandReceiver, intentFilter);
        //通知栏
        myNotification = new MungNotification(this);
        //初始化耳机监听
        initHeadset();
    }

    /**
     * 每次调用startService()方法启动该Service都会回调该方法
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("MusicService", "进入onStartCommand");
        //恢复更新进度条
        //可能因为activity被消灭又重新启动
        //update_progress();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 想要解除与service的绑定时系统调用此方法
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("MusicService", "进入onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.w("MusicService", "进入onDestroy");
        super.onDestroy();
        if (player != null) {
            player.release();
        }
        unregisterReceiver(commandReceiver);
        myNotification.stopNotify(this);
        //发送广播,音乐服务已停止
        sendServiceBroadcast(MusicService.STATUS_STOPPED);
        //取消广播接收器的注册
        if (headsetReceiver != null)
            unregisterReceiver(headsetReceiver);
    }

    /**
     * 内部类，接受广播命令并执行操作
     */
    class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取命令
            //COMMAND_UNKNOWN是默认值
            int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
            Log.w("MusicService", "命令: " + command);
            //执行命令
            switch (command) {
                case COMMAND_PLAY:
                    Log.w("MusicService", "COMMAND_PLAY");
                    next_number = intent.getIntExtra("number", 0);//获取点击位置
//                    Log.w("MusicService", "number: "+next_number);
                    play();
                    break;
                case COMMAND_RESUME:
                    Log.w("MusicService", "COMMAND_RESUME");
                    resume();
                    break;
                case COMMAND_PREVIOUS:
                    Log.w("MusicService", "COMMAND_PREVIOUS");
                    moveNumberToPrevious();
                    break;
                case COMMAND_NEXT:
                    Log.w("MusicService", "COMMAND_NEXT");
                    nextMusic_update_number();
                    break;
                case COMMAND_PAUSE:
                    Log.w("MusicService", "COMMAND_PAUSE");
                    pause();
                    break;
//                case COMMAND_STOP:
//                    Log.w("MusicService", "COMMAND_STOP");
//                    stop();
//                    break;
                case PLAY_MODE_ORDER://顺序播放
                case PLAY_MODE_LOOP://单曲循环
                case PLAY_MODE_RANDOM://随机播放
                    current_PlayMode = command;
                    sendServiceBroadcast(PLAY_MODE_UPDATE);
                    break;
                case COMMAND_REQUEST_DURATION://广播当前歌曲时长
                    Log.w("MusicService", "COMMAND_REQUEST_DURATION");
                    sendServiceBroadcast(PROGRESS_DURATION);
                    break;
                case COMMAND_SEEK_TO://seekTo播放命令
                    Log.w("MusicService","COMMAND_SEEK_TO");
                    int seekBar_progress = intent.getIntExtra("seekBar_progress",0);
                    dealSeekTo(seekBar_progress);
                    break;
                case COMMAND_UNKNOWN:
                    Log.w("MusicService", "COMMAND_UNKNOWN");
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 发送广播
     */
    private void sendServiceBroadcast(int content) {
        Intent intent = null;
        switch (content) {
            case PROGRESS_DURATION:
                intent = new Intent(BROADCAST_MUSICSERVICE_PROGRESS);
                intent.putExtra("content", content);
                intent.putExtra("duration", duration);
                break;
            case PROGRESS_UPDATE:
                intent = new Intent(BROADCAST_MUSICSERVICE_PROGRESS);
                intent.putExtra("content", content);
                intent.putExtra("current_progress", current_progress);
                break;
            case STATUS_PLAYING:
            case STATUS_PAUSED:
            case STATUS_STOPPED:
            case STATUS_COMPLETED:
                intent = new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
                intent.putExtra("status", content);
                break;
            case PLAY_MODE_UPDATE:
                intent = new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
                intent.putExtra("status", content);
                intent.putExtra("playMode",current_PlayMode);
                break;
        }
        if(intent != null){
            //发送
            sendBroadcast(intent);
        }else
            Log.w("MusicService","广播intent是null");
    }

    /**
     * 下一首歌
     */
    private void nextMusic_update_number() {
        switch (current_PlayMode) {
            case PLAY_MODE_ORDER://默认顺序播放
                next_number = current_number + 1;
                break;
            case PLAY_MODE_LOOP://单曲循环
                next_number = current_number;
                break;
            case PLAY_MODE_RANDOM://随机播放
                //用当前歌曲序号做随机数种子
                Random random = new Random(System.currentTimeMillis());
                //产生0-Song_total_number的随机数
                next_number = random.nextInt(SongsCollector.size());
                break;
            default:
                break;
        }
        Log.w("MusicService", "nextMusic_update_number : " + next_number);
        //顺序播放时可能序号越界
        if (next_number >= (SongsCollector.size())) {
            //Toast.makeText(MusicService.this, "已经达到了列表底部!", Toast.LENGTH_SHORT).show();
            next_number = 0;//恢复到初始位置
            play();
        } else if(next_number != current_number || current_PlayMode == PLAY_MODE_LOOP){
            //不越界并且不是当前歌曲才播放
            //或者不越界的同时处于循环模式也播放
            play();
            //取消定时发送歌曲进度的任务
            if(update_progress_thread != null && update_progress_thread.isAlive())
                update_progress_thread.destroy();
        }
    }

    /**
     * 应用next_number参数播放歌曲
     */
    public void play() {
        song = SongsCollector.getSong(next_number);//通过序号拿Song对象
        song_path = song.getDataPath();//获取歌曲文件地址
        player_start();//path装载到player后才可以获取时长
        PlayHistory.addSong(song);//加入历史记录列表
        //更新播放器状态
        current_status = MusicService.STATUS_PLAYING;
        //更新当前歌曲序号
        current_number = next_number;
        //通知主界面播放器状态更改
        sendServiceBroadcast(MusicService.STATUS_PLAYING);
        //通知栏
        myNotification.notifyPlay(this);
    }

    /**
     * 重置player对象
     * 重新装载歌曲
     * 定时发送广播更新歌曲进度
     */
    public void player_start() {
        try {
            if(player != null)
                player.reset();
            else
                player = new MediaPlayer();
            player.setDataSource(song_path);//进入初始态
            player.prepareAsync();//异步准备
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start(); // 准备好了再播放
                    duration = player.getDuration();
                    //通知activity这首歌曲的duration
                    sendServiceBroadcast(MusicService.PROGRESS_DURATION);
                    update_progress();//启动子线程更新进度条
                }
            });
            //监听播放是否完成
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    Log.w("MusicService", "监听到播放完成");
                    //更新播放器状态
                    sendServiceBroadcast(MusicService.STATUS_COMPLETED);
                    //下一首歌
                    nextMusic_update_number();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂停播放
     */
    private void pause() {
        if(player != null && current_status==STATUS_PLAYING){
            //播放器暂停
            player.pause();
            //更新播放器状态
            current_status = MusicService.STATUS_PAUSED;
            sendServiceBroadcast(MusicService.STATUS_PAUSED);
            //取消定时发送歌曲进度的任务
            if(update_progress_thread != null && update_progress_thread.isAlive())
                update_progress_thread.destroy();
            //更新通知
            myNotification.notifyPause(this);
        }
    }

    private void resume() {//暂停后的恢复播放
        player.start();
        current_status = MusicService.STATUS_PLAYING;
        sendServiceBroadcast(MusicService.STATUS_PLAYING);
        update_progress();//启动子线程更新进度条
        myNotification.notifyPlay(this);
    }
    /**创建子线程定时发送广播更新进度条*/
    public void update_progress(){
        if(update_progress_thread!=null && update_progress_thread.isAlive())
            update_progress_thread.destroy();
        update_progress_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //定时发送广播更新歌曲进度
                timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        if (player != null && player.isPlaying())
                            current_progress = player.getCurrentPosition();
                        sendServiceBroadcast(MusicService.PROGRESS_UPDATE);
                        //Log.w("MusicService","Timer运行中");
                    }
                };
                timer.schedule(task, 50, 1000);
            }
        });
        update_progress_thread.start();
    }
    /**
     * 处理seekTo播放命令*/
    public void dealSeekTo(int seekBar_progress){
        if(player == null)
            return;
        player.seekTo(seekBar_progress);
        player.start();
    }
    /**
     * 收到COMMAND_PLAY后的动作
     * next_number是主界面的current_music_list_number
     */
//    private void play_pause() {
//        if (player != null) {
//            if (player.isPlaying()) {
//                if (next_number == current_number) {//点击的正在播放的歌曲
//                    pause();
//                    Log.w("MusicService", "点击正在播放的歌曲，暂停播放");
//                }else{
//                    play();//点击的别的歌曲,重新装载播放
//                }
//            }else if(current_status == MusicService.STATUS_PAUSED){
//                Log.w("MusicService", "暂停状态，恢复播放");
//                resume();
//            }else if (current_status == MusicService.STATUS_STOPPED) {
//                Log.w("MusicService", "停止状态,装载播放");
//                play();
//            }
//        }
//    }
    /**停止播放*/
//    private void stop() {
//        if (player.isPlaying()) {
//            player.stop();
//            current_status = MusicService.STATUS_STOPPED;
//            sendServiceBroadcast(MusicService.STATUS_STOPPED);
//        }
//    }
    /**上一首*/
    private void moveNumberToPrevious() {
        if(current_number - 1 >= 0)
            next_number = current_number - 1;
        play();
    }
    /******内部类，接收耳机状态变化*/
    class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                // 耳机插拔相关广播,有状态信息,并且耳机是断开的,并且音乐正在播放
                // equalsIgnoreCase比较时忽略大小写
                if ("android.intent.action.HEADSET_PLUG".equalsIgnoreCase(action) &&
                        intent.hasExtra("state") &&
                        (intent.getIntExtra("state", 0) != 1) &&
                        current_status == MusicService.STATUS_PLAYING) {
                    //音乐暂停
                    Log.w("DisplayActivity","未插耳机");
                    pause();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /*耳机监听广播注册*/
    public void initHeadset() {//初始化耳机监听
        IntentFilter intentFilter = new IntentFilter();//给广播绑定响应的过滤器
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        headsetReceiver = new HeadsetPlugReceiver();
        registerReceiver(headsetReceiver, intentFilter);
    }
}