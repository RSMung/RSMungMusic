package edu.whut.ruansong.musicplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.PlayHistory;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.activity.DisplayActivity;
import edu.whut.ruansong.musicplayer.activity.LoginActivity;

import static android.app.PendingIntent.getActivity;

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

    //progressBar相关
    public static final int PROGRESS_UPDATE = 4;
    public static final int PROGRESS_DURATION = 5;

    //播放控制命令
    public static final int COMMAND_UNKNOWN = -1;
    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PAUSE = 1;
    public static final int COMMAND_STOP = 2;
    public static final int COMMAND_RESUME = 3;
    public static final int COMMAND_PREVIOUS = 4;
    public static final int COMMAND_NEXT = 5;
    //播放顺序命令
    public static final int PLAY_MODE_ORDER = 8;//顺序播放(默认是它)
    public static final int PLAY_MODE_LOOP = 9;//单曲循环
    public static final int PLAY_MODE_RANDOM = 10;//随机播放

    //广播接收器内部类
    private CommandReceiver commandReceiver;
    //媒体播放器
    private MediaPlayer player = null;
    //当前播放模式
    private int current_PlayMode = PLAY_MODE_ORDER;
    //当前歌曲序号，从0开始
    private static int current_number = 0;
    private int duration = 0;//歌曲时长
    private int current_progress = 0;//当前歌曲播放进度
    //下一首歌的序号
    private int next_number = 0;
    //歌曲文件路径
    private String path;
    //播放器状态
    private static int current_status = MusicService.STATUS_STOPPED;
    private Timer timer = null;

    public static int getCurrent_number() {
        return current_number;
    }

    public static int getCurrent_status() {
        return current_status;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * 服务创建的时候调用
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.w("MusicService", "进入onCreate");
        //绑定广播接收器
        commandReceiver = new CommandReceiver();
        IntentFilter intentFilter = new IntentFilter(BROADCAST_MUSICSERVICE_CONTROL);
        registerReceiver(commandReceiver, intentFilter);
        //player实例化
        player = new MediaPlayer();
        //监听播放是否完成
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.w("MusicService", "监听到播放完成");
                //更新播放器状态
                sendBroadcast(MusicService.STATUS_COMPLETED);
                //下一首歌
                nextMusic_update_number();
            }
        });//监听播放是否完成
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("MusicService", "进入onStartCommand");
        //创建为前台服务,免得被系统杀掉进程
        String channel_id = "musicService_channel_id";
        CharSequence name = "musicService_channel_name";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//O(欧)->API26  android 8
            NotificationChannel mChannel = new NotificationChannel(channel_id, name, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(this)
                    .setChannelId(channel_id)
                    .setContentTitle("MusicService")
                    .setContentText("MusicService is running!")
                    .setSmallIcon(R.mipmap.logo).build();
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle("MusicService")
                    .setContentText("MusicService is running!")
                    .setSmallIcon(R.mipmap.logo)
                    .setOngoing(true);
            notification = notificationBuilder.build();
        }
        //notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        // notificationManager.notify(1, notification);把通知显示出来
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        startForeground(1, notification);//前台通知(会一直显示在通知栏)
        //id用0就不会显示通知
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.w("MusicService", "进入onDestroy");
        super.onDestroy();
        if (player != null) {
            player.release();
        }
        unregisterReceiver(commandReceiver);
        stopForeground(true);
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
                    play_pause();
                    break;
                case COMMAND_RESUME:
                    Log.w("MusicService", "COMMAND_RESUME");
                    next_number = intent.getIntExtra("number", 0);//获取点击位置
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
                case COMMAND_STOP:
                    Log.w("MusicService", "COMMAND_STOP");
                    stop();
                    break;
                case PLAY_MODE_ORDER://顺序播放
                    Log.w("MusicService", "PLAY_MODE_ORDER");
                    current_PlayMode = PLAY_MODE_ORDER;
                    break;
                case PLAY_MODE_LOOP://单曲循环
                    Log.w("MusicService", "PLAY_MODE_LOOP");
                    current_PlayMode = PLAY_MODE_LOOP;
                    break;
                case PLAY_MODE_RANDOM://随机播放
                    Log.w("MusicService", "PLAY_MODE_RANDOM");
                    current_PlayMode = PLAY_MODE_RANDOM;
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
    private void sendBroadcast(int content) {
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
        }
        //发送
        sendBroadcast(intent);
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
                sendBroadcast(PLAY_MODE_LOOP);
                break;
            case PLAY_MODE_RANDOM://随机播放
                //用当前歌曲序号做随机数种子
                Random random = new Random(System.currentTimeMillis());
                //产生0-Song_total_number的随机数
                next_number = random.nextInt(DisplayActivity.getSong_total_number());
                break;
            default:
                break;
        }
        Log.w("MusicService", "nextMusic_update_number : " + next_number);
        //顺序播放时可能序号越界
        if (next_number >= (DisplayActivity.getSong_total_number())) {
            Toast.makeText(MusicService.this, "已经达到了列表底部!", Toast.LENGTH_SHORT).show();
            next_number = 0;//恢复到初始位置
            play();
        } else
            play();//不越界则直接播放
    }

    /**
     * 收到COMMAND_PLAY后的动作
     * next_number是主界面的current_music_list_number
     */
    private void play_pause() {
        if (player != null) {
            if (player.isPlaying()) {
                if (next_number == current_number) {//点击的正在播放的歌曲
                    pause();
                    Log.w("MusicService", "点击正在播放的歌曲，暂停播放");
                }
                //点击的别的歌曲,重新装载播放
                play();
            }
            //停止状态,装载播放
            if (current_status == MusicService.STATUS_STOPPED) {
                play();
            }
        }
    }

    /**
     * 应用next_number参数播放歌曲
     */
    public void play() {
        Song song = DisplayActivity.getSongsList().get(next_number);//通过序号拿Song对象
        path = song.getDataPath();//获取歌曲文件地址
        player_start();//path装载到player后才可以获取时长
        PlayHistory.addSong(song);//加入历史记录列表
        //更新播放器状态
        current_status = MusicService.STATUS_PLAYING;
        //更新当前歌曲序号
        current_number = next_number;
        //通知主界面播放器状态更改
        sendBroadcast(MusicService.STATUS_PLAYING);
    }

    /**
     * 进入游离态到开始播放
     */
    public void player_start() {
        try {
            if (player != null) {
                player.reset();//进入游离态
                player.setDataSource(path);//进入初始态
                player.prepareAsync();//异步准备
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start(); // 准备好了就播放
                        duration = player.getDuration();
                        //通知activity这首歌曲的duration
                        sendBroadcast(MusicService.PROGRESS_DURATION);
                        //定时发送广播更新歌曲进度
                        timer = new Timer();
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                if (player != null)
                                    current_progress = player.getCurrentPosition();
                                sendBroadcast(MusicService.PROGRESS_UPDATE);
                            }
                        };
                        timer.schedule(task, 0, 1000);
                    }
                });
            }
        } catch (Exception e) {
            Log.w("MusicService", "In player_start() :" + e);
        }
    }

    private void moveNumberToPrevious() {
        next_number = next_number - 1;
        //判断是否到达顶端
        if (next_number == 0) {
            Toast.makeText(MusicService.this, "已经达到了列表顶部!", Toast.LENGTH_SHORT).show();
        } else {
            next_number = next_number + 1;
            play_pause();
        }
    }

    /**
     * 暂停播放
     */
    private void pause() {
        //播放器暂停
        player.pause();
        //更新播放器状态
        current_status = MusicService.STATUS_PAUSED;
        sendBroadcast(MusicService.STATUS_PAUSED);
        //取消定时发送歌曲进度的任务
        if (timer != null)
            timer.cancel();
    }

    private void stop() {
        if (player.isPlaying()) {
            player.stop();
            current_status = MusicService.STATUS_STOPPED;
            sendBroadcast(MusicService.STATUS_STOPPED);
        }
    }

    private void resume() {//暂停后的恢复播放
        player.start();
        current_status = MusicService.STATUS_PLAYING;
        sendBroadcast(MusicService.STATUS_PLAYING);
        //定时发送歌曲进度
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                current_progress = player.getCurrentPosition();
                sendBroadcast(MusicService.PROGRESS_UPDATE);
            }
        };
        timer.schedule(task, 0, 1000);
    }

}