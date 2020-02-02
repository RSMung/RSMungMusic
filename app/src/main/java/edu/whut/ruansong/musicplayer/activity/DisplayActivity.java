package edu.whut.ruansong.musicplayer.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import edu.whut.ruansong.musicplayer.model.ActivityCollector;
import edu.whut.ruansong.musicplayer.model.BaseActivity;
import edu.whut.ruansong.musicplayer.model.DrawerLayoutListViewItem;
import edu.whut.ruansong.musicplayer.service.MusicService;
import edu.whut.ruansong.musicplayer.model.PlayHistory;
import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.tool.DrawerLayoutListViewAdapter;
import edu.whut.ruansong.musicplayer.tool.SongAdapter;

/**
 * Created by 阮 on 2018/11/17.
 * 处理UI变化
 */

public class DisplayActivity extends BaseActivity {
    /*控件*/
    private Toolbar toolbar = null;//toolbar
    private ListView main_song_list_view = null;//歌曲列表
    private SongAdapter adapter_main_song_list_view = null;//歌曲列表的适配器
    private ProgressBar progressBar_activity_display = null;//播放进度条
    private View play_bar_bottom = null;//底部播放控制栏
    private ImageView album_icon = null;//专辑图片
    private TextView play_bar_song_name = null;//歌曲名字
    private TextView play_bar_song_author = null;//歌手
    private ImageButton play_bar_btn_play = null;//底部的图片播放按钮
    private ImageButton play_bar_btn_next = null;//下一首歌曲按钮
    private ImageButton btn_history_menu = null;//历史播放记录按钮
    private View history_view = null;//历史播放记录整体
    private ListView history_list_view = null;//历史播放记录list
    private ListView drawer_layout_list_view = null;//侧滑栏的listView
    private AlertDialog dialog = null;//定时停止播放得对话框
    private DrawerLayout drawerlayout = null;//侧滑栏
    private ActionBarDrawerToggle drawerToggle = null;
    /*用于存储*/
    private int duration = 0;//当前的歌曲的总时长
    private int current_progress = 0;//当前的歌曲播放进度
    private static List<Song> songsList = new ArrayList<>();//歌曲数据
    private static int song_total_number = 0;//歌曲总数
    private int current_number = 0;//当前正在播放的歌曲
    private int current_status = MusicService.STATUS_STOPPED;//播放状态默认为停止
    private int view_history_Flag = 0;//用来控制历史播放记录控件是否可见
    private int input_time = 0;
    private int progress_broadcast_content = MusicService.PROGRESS_DURATION;//进度条的广播命令
    private final int REQ_READ_EXTERNAL_STORAGE = 1;//权限请求码,1代表外部存储权限
    private int default_playMode = 0;//默认播放模式,用于打开单选框时默认选中位置的设置
    /*广播接收器*/
    private HeadsetPlugReceiver headsetReceiver = null;//耳机监听
    private StatusChangedReceiver statusChangedReceiver = null;//状态接收器，接收来自service的播放器状态信息
    private ProgressBarReceiver progressBarReceiver = null;
    /*其它*/
    private Timer sleepTimer = null;//定时停止播放

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("DisplayActivity", "进入onCreate");
        /*解决软键盘弹起时，底部控件被顶上去的问题*/
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        /*设定布局*/
        setContentView(R.layout.activity_display);
        /*加载控件*/
        loadWidget();
        /*设置toolbar*/
        setSupportActionBar(toolbar);
        /*启动后台服务*/
        Intent intentService = new Intent(DisplayActivity.this, MusicService.class);
        startService(intentService);
        /*初始化耳机监听*/
        initHeadset();
        /*请求权限*/
        requestPermissionByHand();
        if (song_total_number == 0)
            load_Songs_data();//加载歌曲数据
        /*配置歌曲列表*/
        config_listViewAdapter();
        /*启动广播接收器*/
        bindBroadcastReceiver();
        /*侧滑菜单界面*/
        config_DrawerLayout();
        /*统一处理点击事件*/
        dealClick();
    }

    /**
     * 由不可见变为可见的时候调用
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.w("DisplayActivity", "进入onStart");
        adapter_main_song_list_view.notifyDataSetChanged();
    }

    /**
     * 准备好和用户进行交互的时候调用
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.w("DisplayActivity", "进入onResume");
    }

    /**
     * Activity正在停止，仍可见
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.w("DisplayActivity", "进入onPause");
    }

    /**
     * Activity即将停止，不可见，位于后台,可以做稍微重量级的回收工作
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.w("DisplayActivity", "进入onStop");
    }

    /**
     * Activity即将销毁,做一些最终的资源回收
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w("DisplayActivity", "进入onDestroy");
        if (headsetReceiver != null)
            unregisterReceiver(headsetReceiver);//取消广播接收器的注册
        if (statusChangedReceiver != null)
            unregisterReceiver(statusChangedReceiver);
        if (progressBarReceiver != null)
            unregisterReceiver(progressBarReceiver);
        if (current_status == MusicService.STATUS_STOPPED) {
            stopService(new Intent(this, MusicService.class));
            if (sleepTimer != null) {
                sleepTimer.cancel();//撤销定时器防止崩溃
            }
        }
    }

    /**
     * 回退键   不返回登录界面
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCollector.finishAll();
    }

    /**加载控件*/
    public void loadWidget(){
        toolbar = findViewById(R.id.toolbar_activity_display);
        main_song_list_view = findViewById(R.id.main_song_list_view);
        progressBar_activity_display = findViewById(R.id.progressBar_activity_display);
        play_bar_bottom =  findViewById(R.id.play_bar_bottom);
        album_icon = findViewById(R.id.album_icon);
        play_bar_song_name = findViewById(R.id.play_bar_song_name);
        play_bar_song_author = findViewById(R.id.play_bar_song_author);
        play_bar_btn_play = findViewById(R.id.play_bar_btn_play);
        play_bar_btn_next = findViewById(R.id.play_bar_btn_next);
        btn_history_menu = findViewById(R.id.btn_history_menu);
        history_view = findViewById(R.id.history_view);
        history_list_view = findViewById(R.id.history_list_view);
        drawer_layout_list_view = findViewById(R.id.drawer_layout_list);
    }

    /**toolbar的menu加载*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_display, menu);
        return true;
    }

    /**toolbar的menu点击事件*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.display_toolbar_menu_search://toolbar上的搜索按钮
                Intent intent_jump_toolbar_search =
                        new Intent(DisplayActivity.this, SearchDetailActivity.class);
                startActivity(intent_jump_toolbar_search);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 耳机监听广播注册
     ************/
    public void initHeadset() {//初始化耳机监听
        IntentFilter intentFilter = new IntentFilter();//给广播绑定响应的过滤器
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        headsetReceiver = new HeadsetPlugReceiver();
        registerReceiver(headsetReceiver, intentFilter);
    }

    /******内部类，接收耳机状态变化*/
    class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                //耳机相关广播
                if ("android.intent.action.HEADSET_PLUG".equalsIgnoreCase(intent.getAction())) {
                    //Log.w("DisplayActivity", "耳机相关广播");
                    //有状态信息
                    if (intent.hasExtra("state")) {
                        //Log.w("DisplayActivity", "有状态信息");
                        //耳机断开
                        if (intent.getIntExtra("state", 0) != 1) {
                            //Log.w("DisplayActivity", "耳机断开");
                            //音乐正在播放
                            if (current_status == MusicService.STATUS_PLAYING) {
                                //Log.w("DisplayActivity", "音乐正在播放");
                                //音乐暂停
                                sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**************加载歌曲数据************/
    private void load_Songs_data() {
        if (songsList.size() == 0) {
            //Log.w("DisplayActivity", "歌曲列表为空，需要加载");
            ContentResolver contentResolver = getContentResolver();
            try (Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        //是否是音频
                        int isMusic = cursor.getInt(cursor.getColumnIndex(
                                MediaStore.Audio.Media.IS_MUSIC));
                        //时长
                        long duration = cursor.getLong(
                                cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        //是音乐并且时长大于3分钟
                        if (isMusic != 0 && duration >= 3 * 60 * 1000) {
                            //歌名
                            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                            //歌手
                            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                            //专辑id
                            long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                            //文件路径
                            String dataPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

                            //歌名，歌手，时长，专辑,图标,文件路径,sequence number of list in display activity
                            Song song = new Song(
                                    title,
                                    artist,
                                    duration,
                                    albumId,
                                    R.drawable.song_item_picture,
                                    dataPath,
                                    song_total_number
                            );//R.drawable.song_item_picture是歌曲列表每一项前面那个图标
                            songsList.add(song);
                            song_total_number++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (song_total_number == 0) {
                Toast.makeText(DisplayActivity.this, "本机无歌曲,请下载！", Toast.LENGTH_SHORT).show();
                //不做下面的事情了(即不用把歌曲信息载入列表,因为根本没有)
            }
            //更新toolbar的标题
            toolbar.setTitle(getResources().getString(R.string.title_toolbar) + "(总数:" + song_total_number + ")");
        }
    }

    /******统一处理点击事件**************/
    public void dealClick() {
        /*初始化播放按钮点击事件*/
        ImageButton btn_Play = findViewById(R.id.play_bar_btn_play);
        btn_Play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.w("DisplayActivity", "btn_Play");
                switch (current_status) {
                    case MusicService.STATUS_PLAYING:
                        //Log.w("DisplayActivity", "STATUS_PLAYING");
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED:
                        //Log.w("DisplayActivity", "STATUS_PAUSED");
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED:
                        //Log.w("DisplayActivity", "STATUS_STOPPED");
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                    default:
                        break;
                }
            }
        });
        /*初始化下一首按钮点击事件*/
        play_bar_btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //旧的歌曲图标修改为默认
                songsList.get(current_number).setSong_item_picture(R.drawable.song_item_picture);
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });
        /*点击底部一栏的事件*/
        play_bar_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DisplayActivity.this, "歌曲详情页待实现！", Toast.LENGTH_SHORT).show();
            }
        });
        /*设置歌曲列表item点击事件*/
        main_song_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //旧的歌曲图标修改为默认
                songsList.get(current_number).setSong_item_picture(R.drawable.song_item_picture);
                if(current_status == MusicService.STATUS_PLAYING){//播放状态
                    if(current_number == position){//点击的正在播放的歌曲
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);//暂停
                    }else{//点击的别的歌曲
                        current_number = position;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                }else if(current_status == MusicService.STATUS_PAUSED){//暂停状态
                    if(current_number == position){
                        //应恢复播放
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                    }else{
                        //点击的别的歌曲
                        current_number = position;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                }else {//停止状态
                    current_number = position;
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                }
            }
        });
        /*历史播放记录按钮*/
        btn_history_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view_history_Flag == 0) {
                    SongAdapter adapter_his = new SongAdapter(DisplayActivity.this, R.layout.song_list_item, PlayHistory.songs);
                    history_list_view.setAdapter(adapter_his);
                    history_view.setVisibility(View.VISIBLE);
                    view_history_Flag = 1;
                } else {
                    history_view.setVisibility(View.GONE);
                    view_history_Flag = 0;
                }

            }
        });
        /*设置侧滑栏listView的item点击事件*/
        drawer_layout_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                switch (position) {
                    case 0:
                        //播放模式选择
                        selectMode();
                        drawerlayout.closeDrawer(GravityCompat.START);
                        break;
                    case 1://定时停止播放
                        timePausePlay();
                        drawerlayout.closeDrawer(GravityCompat.START);
                        break;
                    case 2://反馈与建议
                        feedbackAndSuggesttions();
                        drawerlayout.closeDrawer(GravityCompat.START);
                        break;
                    case 3://退出
                        Intent stop_service_intent = new Intent(DisplayActivity.this, MusicService.class);
                        stopService(stop_service_intent);
                        ActivityCollector.finishAll();
                        System.exit(0);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * 侧滑菜单界面
     */
    public void config_DrawerLayout() {
        drawerlayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerlayout, toolbar, R.string.app_name, R.string.app_name) {
            @Override
            public void onDrawerOpened(View drawerView) {//完全打开时触发
                super.onDrawerOpened(drawerView);
                //Toast.makeText(DisplayActivity.this,"onDrawerOpened",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDrawerClosed(View drawerView) {//完全关闭时触发
                super.onDrawerClosed(drawerView);
                //Toast.makeText(DisplayActivity.this,"onDrawerClosed",Toast.LENGTH_SHORT).show();
            }

            /**
             * 当抽屉被滑动的时候调用此方法
             * slideOffset表示 滑动的幅度（0-1）
             */
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }

            /**
             * 当抽屉滑动状态改变的时候被调用
             * 状态值是STATE_IDLE（闲置--0）, STATE_DRAGGING（拖拽的--1）, STATE_SETTLING（固定--2）中之一。
             *具体状态可以慢慢调试
             */
            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
            }
        };
        drawerlayout.setDrawerListener(drawerToggle);
        //设置toolbar左侧图标点击打开侧滑栏
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawerlayout.isDrawerOpen(GravityCompat.START)) {
                    //Log.w("DisplayActivity", "closeDrawer");
                    drawerlayout.closeDrawer(GravityCompat.START);
                } else {
                    //Log.w("DisplayActivity", "openDrawer");
                    drawerlayout.openDrawer(GravityCompat.START);
                }
            }
        });
        //配置侧滑界面listView
        List<DrawerLayoutListViewItem> drawer_list_view_content = new ArrayList<>();
        DrawerLayoutListViewItem stopWithTime = new DrawerLayoutListViewItem(R.drawable.stop_with_time, "定时停止播放");
        DrawerLayoutListViewItem play_mode_select = new DrawerLayoutListViewItem(R.drawable.setting, "播放模式");
        DrawerLayoutListViewItem feedback_suggestions = new DrawerLayoutListViewItem(R.drawable.feedback_suggestions,"反馈与建议");
        DrawerLayoutListViewItem exit = new DrawerLayoutListViewItem(R.drawable.exit, "退出");
        drawer_list_view_content.add(play_mode_select);
        drawer_list_view_content.add(stopWithTime);
        drawer_list_view_content.add(feedback_suggestions);
        drawer_list_view_content.add(exit);
        DrawerLayoutListViewAdapter drawer_list_view_adapter = new DrawerLayoutListViewAdapter(DisplayActivity.this, R.layout.drawer_layout_list_item, drawer_list_view_content);
        drawer_layout_list_view.setAdapter(drawer_list_view_adapter);
    }

    /**
     * 配置歌曲列表
     */
    public void config_listViewAdapter() {
        adapter_main_song_list_view = new SongAdapter(DisplayActivity.this, R.layout.song_list_item, songsList);
        main_song_list_view.setAdapter(adapter_main_song_list_view);
    }

    /*******绑定广播接收器,接收来自服务的广播*/
    private void bindBroadcastReceiver() {
        //播放器状态接收
        statusChangedReceiver = new StatusChangedReceiver();
        IntentFilter intentFilter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(statusChangedReceiver, intentFilter);
        //进度条相关广播
        progressBarReceiver = new ProgressBarReceiver();
        IntentFilter intentFilter1 = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_PROGRESS);
        registerReceiver(progressBarReceiver, intentFilter1);
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
                    //把底部播放按钮的图标改变,列表中正在播放的歌曲的颜色改变
                    Log.w("DisplayActivity", "STATUS_PLAYING");
                    play_bar_btn_play.setBackground(getDrawable(R.drawable.pause_5));//改变图标
                    current_number = MusicService.getCurrent_number();//更改存储的当前播放歌曲序号
                    //加载歌名和歌手,设置专辑图片
                    initBottomMes(current_number);
                    //新的歌曲图标修改为正在播放
                    songsList.get(current_number).setSong_item_picture(R.drawable.playing_grass_green);
                    //通知适配器数据变化
                    adapter_main_song_list_view.notifyDataSetChanged();
                    break;

                //播放器状态更改为暂停
                case MusicService.STATUS_PAUSED:
                    Log.w("DisplayActivity", "STATUS_PAUSED");
                    play_bar_btn_play.setBackground(getDrawable(R.drawable.play_5));//把底部播放按钮的图标改变
                    //歌曲图标修改为默认图标
                    songsList.get(current_number).setSong_item_picture(R.drawable.song_item_picture);
                    //通知适配器数据变化
                    adapter_main_song_list_view.notifyDataSetChanged();
                    break;

                //播放器状态更改为停止
                case MusicService.STATUS_STOPPED:
                    Log.w("DisplayActivity", "STATUS_STOPPED");
                    break;

                //播放器状态更改为播放完成
                case MusicService.STATUS_COMPLETED:
                    //歌曲图标修改为默认图标
                    songsList.get(current_number).setSong_item_picture(R.drawable.song_item_picture);
                    Log.w("DisplayActivity", "STATUS_COMPLETED");
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * 内部类，接受service广播动态更新progressBar
     */
    class ProgressBarReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            progress_broadcast_content = intent.getIntExtra("content", 0);
            switch (progress_broadcast_content) {
                case MusicService.PROGRESS_DURATION:
                    duration = intent.getIntExtra("duration", 0);
                    progressBar_activity_display.setMax(duration);
                    break;
                case MusicService.PROGRESS_UPDATE:
                    current_progress = intent.getIntExtra("current_progress", 0);
                    progressBar_activity_display.setProgress(current_progress);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 设置底部的一栏左侧的歌曲名和歌手以及专辑图片
     ***/
    public void initBottomMes(int position) {//
        Song song = songsList.get(position);//获取点击位置的song对象
        play_bar_song_name.setText(song.getTitle());
        play_bar_song_author.setText(song.getArtist());
        //设置专辑图片
        //album_icon.setImageDrawable(getImage(songsList.get(current_number).getAlbum_id()));
        album_icon.setImageBitmap(getAlbumPicture(songsList.get(current_number).getDataPath()));
    }

    /**********获取歌曲专辑图片*************/
    public Bitmap getAlbumPicture(String dataPath) {
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
            float sx = ((float) 120 / width);
            float sy = ((float) 120 / height);
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
            float sx = ((float) 120 / width);
            float sy = ((float) 120 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
            return albumPicture;
        }
    }

    /*** 定时停止播放*/
    public void timePausePlay() {
        final AlertDialog.Builder customizeDialog = new AlertDialog.Builder(DisplayActivity.this);
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(DisplayActivity.this)
                .inflate(R.layout.dialog_stop_with_time, null);
        customizeDialog.setView(dialogView);
        dialog = customizeDialog.show();
        Button b_ok = dialogView.findViewById(R.id.b_time_ok);
        Button b_cancel = dialogView.findViewById(R.id.b_time_cancel);
        b_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sleepTimer = new Timer();
                TextView tv = dialogView.findViewById(R.id.input_time);
                input_time = Integer.parseInt(tv.getText().toString());//获取输入的时间
                //启动任务
                Toast.makeText(DisplayActivity.this, "歌曲将在" + input_time + "分钟后停止播放",
                        Toast.LENGTH_SHORT).show();
                sleepTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                    }
                }, input_time * 60 * 1000);//delay参数的单位是毫秒
                //关闭对话框
                dialog.dismiss();
            }
        });
        b_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sleepTimer != null) {
                    sleepTimer.cancel();
                }
                dialog.dismiss();
                Toast.makeText(DisplayActivity.this, "任务已经取消", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 选择播放模式
     */
    public void selectMode() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(DisplayActivity.this);
        builder.setIcon(R.drawable.setting);
        builder.setTitle("播放模式");
        final String[] mode = {"顺序播放", "单曲循环", "随机播放"};
        /*设置一个单项选择框
         * 第一个参数指定要显示的一组下拉单选框的数据集合
         * 第二个参数代表索引，指定默认哪一个单选框被勾选上，0表示默认'顺序播放' 会被勾选上
         * 第三个参数给每一个单选项绑定一个监听器
         */
        final Intent intent_mode = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        builder.setSingleChoiceItems(mode, default_playMode, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(DisplayActivity.this,
                        "播放模式为：" + mode[which], Toast.LENGTH_SHORT).show();
                switch (which) {
                    case 0:
                        intent_mode.putExtra("command", MusicService.PLAY_MODE_ORDER);
                        dialog.cancel();
                        break;
                    case 1:
                        intent_mode.putExtra("command", MusicService.PLAY_MODE_LOOP);
                        default_playMode = 1;
                        dialog.cancel();
                        break;
                    case 2:
                        intent_mode.putExtra("command", MusicService.PLAY_MODE_RANDOM);
                        default_playMode = 2;
                        dialog.cancel();
                        break;
                    default:
                        break;
                }
                sendBroadcast(intent_mode);
            }
        });
        builder.show();
    }

    /**
     * 反馈与建议*/
    public void feedbackAndSuggesttions(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("反馈与建议")
                .setIcon(R.drawable.feedback_suggestions)
                .setView(R.layout.dialog_feedback);
        builder.create().show();
    }

    /**
     * 向用户请求权限
     */
    public void requestPermissionByHand() {
        //判断当前系统的版本
        if (Build.VERSION.SDK_INT >= 23) {
            int checkWriteStoragePermission = ContextCompat.checkSelfPermission(
                    DisplayActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
            //如果没有被授予
            if (checkWriteStoragePermission != PackageManager.PERMISSION_GRANTED) {
                //请求权限,此处可以同时申请多个权限
                ActivityCompat.requestPermissions(
                        DisplayActivity.this, new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, REQ_READ_EXTERNAL_STORAGE);
            }
        }
    }

    /**
     * 向用户请求权限后的回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, final String[] permissions, int[] grantResults) {
        if (requestCode == REQ_READ_EXTERNAL_STORAGE) {
            // 如果请求被取消了，那么结果数组就是空的
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予了
                if (song_total_number == 0)
                    load_Songs_data();//加载歌曲数据
                adapter_main_song_list_view.notifyDataSetChanged();
            } else {
                Toast.makeText(DisplayActivity.this, "申请权限失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static List<Song> getSongsList() {
        return songsList;
    }

    public static void setSongsList(List<Song> songsList) {
        DisplayActivity.songsList = songsList;
    }

    public static int getSong_total_number() {
        return song_total_number;
    }
}