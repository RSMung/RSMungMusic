package edu.whut.ruansong.musicplayer.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.List;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.ActivityCollector;
import edu.whut.ruansong.musicplayer.model.BaseActivity;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.service.MusicService;
import edu.whut.ruansong.musicplayer.tool.MyDbFunctions;

public class SongDetailActivity extends BaseActivity implements View.OnClickListener {
    //当前播放的歌曲,播放状态,播放进度,当前的歌曲的总时长,当前播放模式
    private int current_number,current_status,current_progress,duration,current_PlayMode;
    private Song current_song;
    private List<Song> songsList ,myLoveSongs;//歌曲列表
    private ImageView album_view;
    private TextView song_name,song_artist,duration_text,current_progress_text;
    private ImageView play_pause_action,pre_action,next_action,playMode;
    private SeekBar seekBar;
    private ProgressBarReceiver progressBarReceiver ;
    private StatusChangedReceiver statusChangedReceiver;
    private MyDbFunctions myDbFunctions;
    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_song_detail);
        Toolbar toolbar = findViewById(R.id.toolbar_detail_activity);//toolbar栏
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {//toolbar回退键
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SongDetailActivity.this, DisplayActivity.class);
                startActivity(intent);
                finish();
            }
        });
        //获取intent里面的信息
        Intent my_intent = getIntent();
        current_number = my_intent.getIntExtra("current_number",0);
        current_status = my_intent.getIntExtra("current_status", MusicService.STATUS_STOPPED);
        Log.w("SongDetailActivity","current_status ="+current_status);
        current_progress = my_intent.getIntExtra("current_progress",0);
        //准备更新UI
        songsList = DisplayActivity.getSongsList();
        current_song = songsList.get(current_number);
        //更新专辑图片
        Bitmap album_icon = getAlbumPicture(current_song.getDataPath(),450,450);
        album_view = findViewById(R.id.album_icon_detail_activity);
        album_view.setImageBitmap(album_icon);
        //更改歌曲名字,歌手
        String title = current_song.getTitle();
        song_name = findViewById(R.id.detail_activity_name);
        song_name.setText(title);
        String artist = current_song.getArtist();
        song_artist = findViewById(R.id.detail_activity_author);
        song_artist.setText(artist);
        //播放_暂停按钮
        play_pause_action = findViewById(R.id.play_pause_action);
        if(current_status == MusicService.STATUS_PLAYING){
            play_pause_action.setImageDrawable(getResources().getDrawable(R.drawable.pause_black_64));
        }else{
            play_pause_action.setImageDrawable(getResources().getDrawable(R.drawable.play_black_64));
        }
        play_pause_action.setOnClickListener(this);
        //上一首
        pre_action = findViewById(R.id.pre_action);
        pre_action.setOnClickListener(this);
        //下一首
        next_action = findViewById(R.id.next_action);
        next_action.setOnClickListener(this);
        //更改歌曲进度
        seekBar = findViewById(R.id.seekBar);
        duration_text = findViewById(R.id.duration_text);
        current_progress_text = findViewById(R.id.current_progress_text);
        current_progress = my_intent.getIntExtra("current_progress",0);//当前进度值
        current_progress_text.setText(durationToString(current_progress));//当前进度文本
        duration = my_intent.getIntExtra("duration",0);//总时长值
        duration_text.setText(durationToString(duration));//总时长文本
        seekBar.setMax(duration);
        seekBar.setProgress(current_progress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent intent_seekTo = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
                intent_seekTo.putExtra("command",MusicService.COMMAND_SEEK_TO);
                intent_seekTo.putExtra("seekBar_progress",seekBar.getProgress());
                sendBroadcast(intent_seekTo);
            }
        });
        //进度条相关广播接收器注册
        progressBarReceiver = new ProgressBarReceiver();
        IntentFilter intentFilter1 = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_PROGRESS);
        registerReceiver(progressBarReceiver, intentFilter1);
        //播放器状态广播接收
        statusChangedReceiver = new StatusChangedReceiver();
        IntentFilter intentFilter2 = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(statusChangedReceiver,intentFilter2);
        //爱心图标
        ImageView love_song_icon = findViewById(R.id.love_song_icon);
        love_song_icon.setOnClickListener(this);//监听事件
        myDbFunctions = MyDbFunctions.getInstance(this);
        myLoveSongs = myDbFunctions.loadMyLoveSongs();//从数据库加载我喜爱的歌曲
        for(Song s:myLoveSongs){
            if(s.getTitle().equals(current_song.getTitle())){
                love_song_icon.setImageDrawable(getResources().getDrawable(R.drawable.full_love_32));
                current_song.setLove(true);
            }
        }
        //播放模式
        playMode = findViewById(R.id.playMode_detail_activity);
        playMode.setOnClickListener(this);
        current_PlayMode = my_intent.getIntExtra("current_PlayMode",0) + MusicService.PLAY_MODE_ORDER;
        switch (current_PlayMode){
            case MusicService.PLAY_MODE_ORDER:
                playMode.setImageDrawable(getDrawable(R.drawable.order_32));
                break;
            case MusicService.PLAY_MODE_LOOP:
                playMode.setImageDrawable(getDrawable(R.drawable.cycle_32));
                break;
            case MusicService.PLAY_MODE_RANDOM:
                playMode.setImageDrawable(getDrawable(R.drawable.random_32));
                break;
        }
        //分享
        ImageView share_action = findViewById(R.id.share_detail_activity);
        share_action.setOnClickListener(this);
    }
    /**
     * Activity即将销毁,做一些最终的资源回收
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w("SongDetailActivity", "进入onDestroy");
        if (progressBarReceiver != null)
            unregisterReceiver(progressBarReceiver);
        if (statusChangedReceiver != null)
            unregisterReceiver(statusChangedReceiver);
        songsList = null;
        myLoveSongs = null;
    }

    @Override
    public void onClick(View v) {
        Log.w("SongDetailActivity","onClick");
        switch (v.getId()) {
            case R.id.love_song_icon:
                ImageView love_song_icon = findViewById(R.id.love_song_icon);
                if(current_song.isLove()){
                    //已经是喜爱的歌曲了
                    love_song_icon.setImageDrawable(getResources().getDrawable(R.drawable.love));
                    current_song.setLove(false);
                    myDbFunctions.removeSong(current_song);
                    myLoveSongs.remove(current_song);
                }else{//添加并更改图标
                    love_song_icon.setImageDrawable(getResources().getDrawable(R.drawable.full_love_32));
                    current_song.setLove(true);
                    myDbFunctions.saveSong(current_song);
                    myLoveSongs.add(current_song);
                }
                break;
            case R.id.play_pause_action:
                switch (current_status) {
                    case MusicService.STATUS_PLAYING:
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED:
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED:
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                    default:
                        break;
                }
                break;
            case R.id.next_action:
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                break;
            case R.id.pre_action:
                sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
                break;
            case R.id.playMode_detail_activity:
                showPopupMenu(playMode);
                break;
            case R.id.share_detail_activity:
                Intent intent = new Intent(SongDetailActivity.this,ShareActivity.class);
                intent.putExtra("dataPath",current_song.getDataPath());
                intent.putExtra("title",current_song.getTitle());
                intent.putExtra("artist",current_song.getArtist());
                startActivity(intent);
                break;
        }
    }
    /**
     * 内部类，接受service广播动态更新progressBar
     */
    class ProgressBarReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int progress_broadcast_content = intent.getIntExtra("content", 0);
            switch (progress_broadcast_content) {
                case MusicService.PROGRESS_DURATION:
                    duration = intent.getIntExtra("duration", 0);
                    seekBar.setMax(duration);
                    duration_text.setText(durationToString(duration));
                    break;
                case MusicService.PROGRESS_UPDATE:
                    current_progress = intent.getIntExtra("current_progress", 0);
                    seekBar.setProgress(current_progress);
                    current_progress_text.setText(durationToString(current_progress));
                    break;
                default:
                    break;
            }
        }
    }
    /**
     * 把毫秒时长转换为类似3:50的String*/
    public String durationToString(int duration){
        int duration_second = duration / 1000;//单位转换为秒
        int minute = duration_second / 60;//求得分钟数
        int second = duration_second % 60;//求得不满一分钟的秒数
        return minute+":"+second;
    }
    /**********获取歌曲专辑图片*************/
    public Bitmap getAlbumPicture(String dataPath, int scale_length, int scale_width) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(dataPath);
        byte[] data = mmr.getEmbeddedPicture();
        Bitmap albumPicture;
        if (data != null) {
            //获取bitmap对象
            albumPicture = BitmapFactory.decodeByteArray(data, 0, data.length);
            //获取宽高
            int width = albumPicture.getWidth();
            int height = albumPicture.getHeight();
            //Log.w("DisplayActivity","width = "+width+" height = "+height);
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) scale_length / width);
            float sy = ((float) scale_width / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
            return albumPicture;
        } else {
            albumPicture = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_icon);
            int width = albumPicture.getWidth();
            int height = albumPicture.getHeight();
            //Log.w("DisplayActivity", "width = " + width + " height = " + height);
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) scale_length / width);
            float sy = ((float) scale_width / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
            return albumPicture;
        }
    }
    /***发送命令，控制音乐播放，参数定义在MusicService中*/
    private void sendBroadcastOnCommand(int command) {
        //1.创建intent,控制命令
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        //2.封装数据
        intent.putExtra("command", command);
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number", current_number);//封装歌曲在list中的位置
                break;
            case MusicService.COMMAND_RESUME:
            case MusicService.COMMAND_PAUSE:
            case MusicService.COMMAND_REQUEST_DURATION:
            default:
                break;
        }
        //3.发送广播
        sendBroadcast(intent);
    }
    /*****内部类，接受播放器状态更改广播命令并执行操作*/
    class StatusChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取播放器状态
            current_status = intent.getIntExtra("status", -1);
            switch (current_status) {
                //播放器状态更改为正在播放
                case MusicService.STATUS_PLAYING:
                    current_number = MusicService.getCurrent_number();//更改存储的当前播放歌曲序号
                    current_song = songsList.get(current_number);
                    duration = (int)current_song.getDuration();
                    //更新UI
                    play_pause_action.setImageDrawable(getDrawable(R.drawable.pause_black_64));//改变图标
                    seekBar.setMax(duration);
                    duration_text.setText(durationToString(duration));
                    album_view.setImageBitmap(getAlbumPicture(current_song.getDataPath(),450,450));
                    song_name.setText(current_song.getTitle());
                    song_artist.setText(current_song.getArtist());
                    //更新状态
                    current_status = MusicService.STATUS_PLAYING;
                    break;
                //播放器状态更改为暂停
                case MusicService.STATUS_PAUSED:
                    play_pause_action.setImageDrawable(getDrawable(R.drawable.play_black_64));
                    current_status = MusicService.STATUS_PAUSED;
                    break;
                //音乐播放服务已停止
                case MusicService.STATUS_STOPPED:
                    ActivityCollector.finishAll();
                    current_status = MusicService.STATUS_STOPPED;
                    break;
                //播放器状态更改为播放完成
                case MusicService.STATUS_COMPLETED:
                    Log.w("SongDetailActivity", "STATUS_COMPLETED");
                    current_status = MusicService.STATUS_COMPLETED;
                    break;
                case MusicService.PLAY_MODE_UPDATE:
                    //顺序,单曲,随机 --->  8,9,10
                    //在弹窗中位置分别是0,1,2
                    current_PlayMode = intent.getIntExtra("playMode",MusicService.PLAY_MODE_ORDER);
                    break;
                default:
                    break;
            }
        }
    }
    /**
     * 播放模式弹出窗口*/
    @SuppressLint("RestrictedApi")
    private void showPopupMenu(View view) {
        // 这里的view代表popupMenu需要依附的view
        PopupMenu popupMenu = new PopupMenu(SongDetailActivity.this, view);
        // 获取布局文件
        popupMenu.getMenuInflater().inflate(R.menu.menu_play_mode, popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // 控件每一个item的点击事件
                Intent intent_mode = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
                switch (item.getItemId()){
                    case R.id.order_mode:
                        intent_mode.putExtra("command", MusicService.PLAY_MODE_ORDER);
                        playMode.setImageDrawable(getDrawable(R.drawable.order_32));
                        break;
                    case R.id.cycle_mode:
                        intent_mode.putExtra("command", MusicService.PLAY_MODE_LOOP);
                        playMode.setImageDrawable(getDrawable(R.drawable.cycle_32));
                        break;
                    case R.id.random_mode:
                        intent_mode.putExtra("command", MusicService.PLAY_MODE_RANDOM);
                        playMode.setImageDrawable(getDrawable(R.drawable.random_32));
                        break;
                }
                sendBroadcast(intent_mode);
                return true;
            }
        });
        //为了显示icon
        try {
            Field field = popupMenu.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            MenuPopupHelper helper = (MenuPopupHelper) field.get(popupMenu);
            helper.setForceShowIcon(true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
