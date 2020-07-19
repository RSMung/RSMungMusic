package edu.whut.ruansong.musicplayer.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.ActivityCollector;
import edu.whut.ruansong.musicplayer.model.BaseActivity;
import edu.whut.ruansong.musicplayer.model.PlayHistory;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.model.SongsCollector;
import edu.whut.ruansong.musicplayer.myView.GramophoneView;
import edu.whut.ruansong.musicplayer.service.MusicService;
import edu.whut.ruansong.musicplayer.db.MyDbFunctions;
import edu.whut.ruansong.musicplayer.tool.PictureDealHelper;
import edu.whut.ruansong.musicplayer.tool.SongAdapter;

public class SongDetailActivity extends BaseActivity implements View.OnClickListener {
    private String TAG = "SongDetailActivity";
    //当前播放的歌曲,播放状态,播放进度,当前的歌曲的总时长,当前播放模式
    private int current_number,current_status,current_progress,duration,current_PlayMode;
    private Song current_song;
    private ArrayList<Song> myLoveSongs;//歌曲列表
    private TextView song_name,song_artist,duration_text,current_progress_text;
    private ImageView play_pause_action,pre_action,next_action,playMode;
    private SeekBar seekBar;
    private ProgressBarReceiver progressBarReceiver ;
    private StatusChangedReceiver statusChangedReceiver;
    private MyDbFunctions myDbFunctions;
    private Toolbar toolbar;
    private GramophoneView gramophoneView;
    private int windowWidth,windowHeight;
    private Bitmap album_icon = null;
    private int default_lightColor;
    private int default_darkColor;
    private ImageView btn_history_view,love_song_icon;
    private LinearLayout lv_history ;
    private ListView list_history ;
    private SongAdapter adapter_history ;
    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_song_detail);
        default_lightColor = getResources().getColor(R.color.shallow_violet_color);
        default_darkColor = getResources().getColor(R.color.shallow_green_color);
        toolbar = findViewById(R.id.toolbar_detail_activity);//toolbar栏
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
        current_song = SongsCollector.getSong(current_number);
        //更新专辑图片
        gramophoneView = findViewById(R.id.gramophone_view);
        windowWidth = getWindowManager().getDefaultDisplay().getWidth();
        windowHeight = getWindowManager().getDefaultDisplay().getHeight();
        gramophoneView.setPictureRadius(windowWidth/4);
        album_icon = PictureDealHelper.getAlbumPicture(this,current_song.getDataPath(),windowWidth/4,windowWidth/4);
        updateBackground();
        gramophoneView.setPictureRes(album_icon);
        if(current_status == MusicService.STATUS_PLAYING){
            gramophoneView.setPlaying(true);
        }else {
            gramophoneView.setPlaying(false);
        }
        //更改歌曲名字,歌手
        String title = current_song.getTitle();
        toolbar.setTitle(title);
        String artist = current_song.getArtist();
        toolbar.setSubtitle(artist);
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
        duration = (int)current_song.getDuration();//总时长值
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
        love_song_icon = findViewById(R.id.love_song_icon);
        love_song_icon.setOnClickListener(this);//监听事件
        myDbFunctions = MyDbFunctions.getInstance(this);
        if(myLoveSongs == null)
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
        //历史播放记录控件相关初始化
        loadHistoryView();
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
        myLoveSongs = null;
        album_icon = null;
    }

    @Override
    public void onClick(View v) {
        Log.w("SongDetailActivity","onClick");
        switch (v.getId()) {
            case R.id.love_song_icon:
                if(current_song.isLove()){
                    //已经是喜爱的歌曲了
                    love_song_icon.setImageDrawable(getResources().getDrawable(R.drawable.love));
                    current_song.setLove(false);//修改本地标志
                    myDbFunctions.setLove(current_song.getDataPath(),"false");//更新数据库
                    myLoveSongs.remove(current_song);//更新我喜爱的歌曲list
                }else{//添加并更改图标
                    love_song_icon.setImageDrawable(getResources().getDrawable(R.drawable.full_love_32));
                    current_song.setLove(true);
                    myDbFunctions.setLove(current_song.getDataPath(),"true");
                    myLoveSongs.add(current_song);
                }
                break;
            case R.id.play_pause_action:
                switch (current_status) {
                    case MusicService.STATUS_PLAYING:
                        Log.w("SearchDetailActivity","发送暂停命令");
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED:
                        Log.w("SearchDetailActivity","发送恢复命令");
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED:
                        Log.w("SearchDetailActivity","发送停止命令");
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                    default:
                        Log.w("SearchDetailActivity","什么命令也不发送");
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
            case R.id.btn_history_view_song_detail_activity:
                if(lv_history.getVisibility() == View.GONE){
                    lv_history.setVisibility(View.VISIBLE);
                }else {
                    lv_history.setVisibility(View.GONE);
                }
                break;
            default:
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
     * 把毫秒时长转换为类似03:50的String*/
    public String durationToString(int duration){
        int duration_second = duration / 1000;//单位转换为秒
        int minute = duration_second / 60;//求得分钟数
        int second = duration_second % 60;//求得不满一分钟的秒数
        StringBuilder sb = new StringBuilder();
        if(minute < 10)
            sb.append(0);
        sb.append(minute);
        sb.append(':');
        if(second < 10)
            sb.append(0);
        sb.append(second);
        return sb.toString();
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
                    current_song = SongsCollector.getSong(current_number);
                    duration = (int)current_song.getDuration();
                    //更新UI
                    play_pause_action.setImageDrawable(getDrawable(R.drawable.pause_black_64));//改变图标
                    seekBar.setMax(duration);
                    duration_text.setText(durationToString(duration));
                    toolbar.setTitle(current_song.getTitle());
                    toolbar.setSubtitle(current_song.getArtist());
                    //更新状态
                    current_status = MusicService.STATUS_PLAYING;
                    //更新喜爱标志
                    if(current_song.isLove()){
                        love_song_icon.setImageDrawable(getResources().getDrawable(R.drawable.full_love_32));
                    }else{
                        love_song_icon.setImageDrawable(getResources().getDrawable(R.drawable.love));
                    }
                    //更新留声机
                    album_icon = PictureDealHelper.getAlbumPicture(context, current_song.getDataPath(),windowWidth/4,windowWidth/4);
                    gramophoneView.setPictureRes(album_icon);
                    gramophoneView.setPlaying(true);
                    //更新activity背景
                    updateBackground();
                    //历史播放记录adapter通知数据变化
                    adapter_history.notifyDataSetChanged();
                    break;
                //播放器状态更改为暂停
                case MusicService.STATUS_PAUSED:
                    play_pause_action.setImageDrawable(getDrawable(R.drawable.play_black_64));
                    current_status = MusicService.STATUS_PAUSED;
                    gramophoneView.setPlaying(false);
                    break;
                //音乐播放服务已停止
                case MusicService.STATUS_STOPPED:
                    ActivityCollector.finishAll();
                    current_status = MusicService.STATUS_STOPPED;
                    gramophoneView.setPlaying(false);
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

    /*toolbar的menu加载*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_song_detail_activity,menu);
        return true;
    }
    /*menu的点击事件*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.song_detail_toolbar_menu_share:
                Intent intent = new Intent(SongDetailActivity.this,ShareActivity.class);
                intent.putExtra("dataPath",current_song.getDataPath());
                intent.putExtra("title",current_song.getTitle());
                intent.putExtra("artist",current_song.getArtist());
                startActivity(intent);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 根据专辑图片提取颜色更新activity的背景*/
    public void updateBackground(){
        RelativeLayout root = findViewById(R.id.rootRv_songDetailActivity);
        if(current_song.isDefaultAlbumIcon()){//默认专辑图片提取出来的颜色不好看
//            Log.w(TAG,"默认专辑图片提取出来的颜色不好看");
            root.setBackgroundColor(getResources().getColor(R.color.white_color));
            return;
        }
        Palette.from(album_icon).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                //暗、活跃
                int lightVibrantColor = palette.getLightVibrantColor(default_darkColor);//如果分析不出来，则返回默认颜色
                //亮、柔和
                int lightMutedColor = palette.getLightMutedColor(default_lightColor);
                int[] colors = {lightMutedColor,lightVibrantColor};
                GradientDrawable.Orientation orientation = null;
                int orientation_flag = (int) (Math.random()*8);
                switch (orientation_flag){
                    case 0:
                        orientation = GradientDrawable.Orientation.TOP_BOTTOM;
                        break;
                    case 1:
                        orientation = GradientDrawable.Orientation.TR_BL;
                        break;
                    case 2:
                        orientation = GradientDrawable.Orientation.RIGHT_LEFT;
                        break;
                    case 3:
                        orientation = GradientDrawable.Orientation.BR_TL;
                        break;
                    case 4:
                        orientation = GradientDrawable.Orientation.BOTTOM_TOP;
                        break;
                    case 5:
                        orientation = GradientDrawable.Orientation.BL_TR;
                        break;
                    case 6:
                        orientation = GradientDrawable.Orientation.LEFT_RIGHT;
                        break;
                    default:
                        orientation = GradientDrawable.Orientation.TL_BR;
                        break;
                }
                GradientDrawable gradientBackground = new GradientDrawable(orientation,colors);
                RelativeLayout root = findViewById(R.id.rootRv_songDetailActivity);
                root.setBackground(gradientBackground);
            }
        });
    }

    /**
     * 加载历史播放记录控件*/
    public void loadHistoryView(){
        btn_history_view = findViewById(R.id.btn_history_view_song_detail_activity);
        lv_history = findViewById(R.id.history_view);
        lv_history.setVisibility(View.GONE);
        list_history = findViewById(R.id.history_list_view);
        adapter_history = new SongAdapter(SongDetailActivity.this, R.layout.song_list_item, PlayHistory.getSongs());
        list_history.setAdapter(adapter_history);
        btn_history_view.setOnClickListener(this);
    }
}
