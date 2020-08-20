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
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
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

import com.wang.avi.AVLoadingIndicatorView;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import edu.whut.ruansong.musicplayer.db.MyDbFunctions;
import edu.whut.ruansong.musicplayer.model.ActivityCollector;
import edu.whut.ruansong.musicplayer.model.BaseActivity;
import edu.whut.ruansong.musicplayer.model.DrawerLayoutListViewItem;
import edu.whut.ruansong.musicplayer.model.SongsCollector;
import edu.whut.ruansong.musicplayer.myView.GramophoneView;
import edu.whut.ruansong.musicplayer.service.MusicService;
import edu.whut.ruansong.musicplayer.model.PlayHistory;
import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.tool.DrawerLayoutListViewAdapter;
import edu.whut.ruansong.musicplayer.tool.PictureDealHelper;
import edu.whut.ruansong.musicplayer.tool.SongAdapter;

/**
 * Created by 阮 on 2018/11/17.
 * main activity
 */

public class DisplayActivity extends BaseActivity {
    /*控件*/
    private Toolbar toolbar = null;//toolbar
    private SongAdapter adapter_main_song_list_view;
    private SongAdapter adapter_history;//歌曲列表的适配器
    private ProgressBar progressBar_activity_display = null;//播放进度条
    private ImageView album_icon = null;//专辑图片
    private TextView play_bar_song_name = null;//歌曲名字
    private TextView play_bar_song_author = null;//歌手
    private ImageButton play_bar_btn_play = null;//底部的图片播放按钮
    private View history_view = null;//历史播放记录整体
    private ListView drawer_layout_list_view = null;//侧滑栏的listView
    private AlertDialog dialog = null;//定时停止播放得对话框
    private DrawerLayout drawerlayout = null;//侧滑栏
    private TextView tv_intput = null;//定时停止播放的输入框
    private MyDbFunctions myDbFunctions;
    private int current_progress = 0;//当前的歌曲播放进度
    private int current_number = 0;//当前正在播放的歌曲
    private int current_status = MusicService.STATUS_STOPPED;//播放状态默认为停止
    private int input_time = 0;//用于存储定时停止播放输入的时间
    private boolean pause_task_flag = false;//是否有定时停止任务的标志
    private double rest_of_time = 0;//用于存储定时停止播放任务的剩余时间
    private final int REQ_READ_EXTERNAL_STORAGE = 1;//权限请求码,1代表读取外部存储权限,2代表写存储
    private int default_playMode = 0;//默认播放模式,用于打开单选框时默认选中位置的设置
    /*广播接收器*/
    private StatusChangedReceiver statusChangedReceiver = null;//状态接收器，接收来自service的播放器状态信息
    private ProgressBarReceiver progressBarReceiver = null;
    /*其它*/
    private CountDownTimer countDownTimer = null;//倒计时,用于定时停止播放

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*解决软键盘弹起时，底部控件被顶上去的问题*/
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        /*设定布局*/
        setContentView(R.layout.activity_display);

        /*启动后台服务*/
        Intent intentService = new Intent(DisplayActivity.this, MusicService.class);
        startService(intentService);

        /*toolbar相关*/
        initMyToolbar();
        //侧滑菜单界面
        initMyDrawerLayout();

        /*初始化底部播放控制栏*/
        initControlPlayBar();

        /*获取歌曲数据*/
        load_Songs_data();

        /*启动广播接收器*/
        bindBroadcastReceiver();
    }

    /****************************初始化顶部工具栏*****************/
    private void initMyToolbar(){
        toolbar = findViewById(R.id.toolbar_activity_display);
        drawer_layout_list_view = findViewById(R.id.drawer_layout_list);
        setSupportActionBar(toolbar);
        //更新toolbar的标题
        toolbar.setTitle(getResources().getString(R.string.title_toolbar));
    }
    /** toolbar的menu加载*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_display, menu);
        return true;
    }

    /* toolbar的menu点击事件*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.display_toolbar_menu_search://toolbar上的搜索按钮
                Intent intent_jump_toolbar_search =
                        new Intent(DisplayActivity.this, SearchDetailActivity.class);
                startActivity(intent_jump_toolbar_search);
                break;
            case R.id.refresh:
                new ScanMusicTask(DisplayActivity.this).execute();
//                scanMusic();//扫描歌曲
                break;
            default:
                break;
        }
        return true;
    }

    /*侧滑菜单界面*/
    public void initMyDrawerLayout() {
        //drawer_layout是主界面的最外层布局的id
        drawerlayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerlayout, toolbar, R.string.app_name, R.string.app_name) {
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
        DrawerLayoutListViewItem myLoveSongs = new DrawerLayoutListViewItem(R.drawable.love, "我喜欢的音乐");
        DrawerLayoutListViewItem stopWithTime = new DrawerLayoutListViewItem(R.drawable.stop_with_time, "定时停止播放");
        DrawerLayoutListViewItem play_mode_select = new DrawerLayoutListViewItem(R.drawable.setting, "播放模式");
        DrawerLayoutListViewItem feedback_suggestions = new DrawerLayoutListViewItem(R.drawable.about, "关于");
        DrawerLayoutListViewItem exit = new DrawerLayoutListViewItem(R.drawable.exit, "退出");
        drawer_list_view_content.add(myLoveSongs);
        drawer_list_view_content.add(play_mode_select);
        drawer_list_view_content.add(stopWithTime);
        drawer_list_view_content.add(feedback_suggestions);
        drawer_list_view_content.add(exit);
        DrawerLayoutListViewAdapter drawer_list_view_adapter = new DrawerLayoutListViewAdapter(DisplayActivity.this, R.layout.drawer_layout_list_item, drawer_list_view_content);
        drawer_layout_list_view.setAdapter(drawer_list_view_adapter);
        /*设置侧滑栏listView的item点击事件*/
        drawer_layout_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                switch (position) {
                    case 0:
                        //我喜爱的歌曲
                        drawerlayout.closeDrawer(GravityCompat.START);
                        Intent intent = new Intent(DisplayActivity.this, MyLoveSongsActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        //播放模式选择
                        if (SongsCollector.size() != 0) {
                            selectMode();
                            drawerlayout.closeDrawer(GravityCompat.START);
                        } else {
                            Toast.makeText(DisplayActivity.this, "本机无歌曲,请下载！", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2://定时停止播放
                        if (SongsCollector.size() != 0) {
                            timePausePlay();
                            drawerlayout.closeDrawer(GravityCompat.START);
                        } else {
                            Toast.makeText(DisplayActivity.this, "本机无歌曲,请下载！", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 3://反馈与建议
                        feedbackAndSuggesttions();
                        drawerlayout.closeDrawer(GravityCompat.START);
                        break;
                    case 4://退出
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

    /*定时停止播放*/
    public void timePausePlay() {
        final AlertDialog.Builder customizeDialog = new AlertDialog.Builder(DisplayActivity.this);
        @SuppressLint("InflateParams")
        final View dialogView = LayoutInflater.from(DisplayActivity.this).inflate(R.layout.dialog_stop_with_time, null);
        customizeDialog.setView(dialogView);
        customizeDialog.setIcon(R.drawable.stop_with_time);
        customizeDialog.setTitle("定时停止播放");
        Button b_ok = dialogView.findViewById(R.id.btn_time_ok);
        Button b_cancel = dialogView.findViewById(R.id.btn_time_cancel);
        final TextView tv_rest_of_time = dialogView.findViewById(R.id.rest_of_time);
        tv_intput = dialogView.findViewById(R.id.input_time);
        if (!pause_task_flag) {//无任务
            b_cancel.setVisibility(View.INVISIBLE);//取消按钮不可见
            tv_rest_of_time.setVisibility(View.INVISIBLE);//剩余时间文本框不可见
        } else {//有任务
            b_ok.setVisibility(View.INVISIBLE);//建立任务按钮不可见
            tv_intput.setVisibility(View.INVISIBLE);//输入框不可见
            DecimalFormat df = new DecimalFormat("0.00 ");
            StringBuilder rest = new StringBuilder("剩余: " + df.format(rest_of_time) + "分钟");
            tv_rest_of_time.setText(rest);
        }
        dialog = customizeDialog.show();//实例化dialog
        b_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tv_intput.getText().toString().equals("")) {
                    //Log.w("DisplayActivity","输入时间为空");
                    Toast.makeText(DisplayActivity.this, "请输入时间!", Toast.LENGTH_SHORT).show();
                    return;
                }
                input_time = Integer.parseInt(tv_intput.getText().toString());//获取输入的时间
                Toast.makeText(DisplayActivity.this, input_time + "分钟后若有歌曲在播放则停止", Toast.LENGTH_SHORT).show();
                //倒计时任务
                countDownTimer = new CountDownTimer(input_time * 60 * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
//                        Log.w("DisplayActivity","millisUntilFinished:"+millisUntilFinished);
                        rest_of_time = (double) millisUntilFinished / 1000.0 / 60.0;
                    }

                    @Override
                    public void onFinish() {
                        Log.w("DisplayActivity","倒计时结束");
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                    }
                };
                countDownTimer.start();
                //关闭对话框
                dialog.dismiss();
                pause_task_flag = true;
            }
        });
        b_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                dialog.dismiss();
                pause_task_flag = false;
                Toast.makeText(DisplayActivity.this, "任务已经取消", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*选择播放模式*/
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

    /*反馈与建议*/
    public void feedbackAndSuggesttions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.about))
                .setIcon(R.drawable.about)
                .setView(R.layout.dialog_feedback);
        builder.create().show();
    }
    /*--------------顶部工具栏相关结束----------------------------*/

    /*----------------------------初始化歌曲列表控件-------------*/
    private void initMySongListView(){
//        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
//        Log.w("初始化歌曲列表控件",df.format(new Date()));
        //歌曲列表
        final ListView main_song_list_view = findViewById(R.id.main_song_list_view);
        adapter_main_song_list_view = new SongAdapter(DisplayActivity.this,
                R.layout.song_list_item, SongsCollector.getSongsList());
        main_song_list_view.setAdapter(adapter_main_song_list_view);
        /*设置歌曲列表item点击事件*/
        main_song_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (current_status == MusicService.STATUS_PLAYING) {//播放状态
                    if (current_number == position) {//点击的正在播放的歌曲
                        Log.w("DisplayActivity","点击的正在播放的歌曲");
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);//暂停
                    } else {//点击的别的歌曲
                        current_number = position;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                } else if (current_status == MusicService.STATUS_PAUSED) {//暂停状态
                    if (current_number == position) {
                        //应恢复播放
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                    } else {
                        //点击的别的歌曲
                        current_number = position;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                } else {//停止状态
                    current_number = position;
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                }
            }
        });
        //item内部的more_options按钮点击
        adapter_main_song_list_view.setOnItemMoreOptionsClickListener(new SongAdapter.onItemMoreOptionsListener() {
            @Override
            public void onMoreOptionsClick(final int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisplayActivity.this)
                        .setTitle("请确认!")
                        .setIcon(R.drawable.danger)
                        .setMessage("确认要从本地列表中删除此歌曲吗?")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                /*which
                                *int BUTTON_POSITIVE = -1; int BUTTON_NEGATIVE = -2;   int BUTTON_NEUTRAL = -3;*/
//                                Toast.makeText(DisplayActivity.this,"你点击了确定"+position,Toast.LENGTH_SHORT).show();
                                //判断是否该歌曲正在播放
                                if(position == current_number){
                                    sendBroadcastOnCommand(MusicService.COMMAND_NEXT);//播放下一首歌曲
                                }
                                //从数据库中删除该歌曲
                                if(myDbFunctions != null){
                                    myDbFunctions.removeSong(SongsCollector.getSong(position).getDataPath());
                                }
                                //从内存列表中删除该歌曲
                                SongsCollector.removeSong(position);
                                //通知列表数据变化了
                                if(adapter_main_song_list_view != null){
                                    adapter_main_song_list_view.notifyDataSetChanged();
                                }
                                if(main_song_list_view != null){
                                    main_song_list_view.invalidate();
                                }
                                Toast.makeText(DisplayActivity.this,"已删除,点击刷新按钮可找回",Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                Toast.makeText(DisplayActivity.this,"下次不要点错了哦",Toast.LENGTH_SHORT).show();
                            }
                        })
                        ;
                builder.create().show();
            }
        });
    }
    /*---------------------歌曲列表控件初始化结束---------------------*/

    /*---------------------初始化底部播放控制栏---------------------*/
    private void initControlPlayBar(){
        progressBar_activity_display = findViewById(R.id.progressBar_activity_display);
        //底部播放控制栏
        View play_bar_bottom = findViewById(R.id.play_bar_bottom);
        /*点击底部一栏的事件*/
        play_bar_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//跳转到歌曲详情页
                if (SongsCollector.size() != 0) {
                    Intent intent = new Intent(DisplayActivity.this, SongDetailActivity.class);
                    intent.putExtra("current_number", current_number);
                    intent.putExtra("current_status", current_status);
                    intent.putExtra("current_progress", current_progress);
                    intent.putExtra("current_PlayMode", default_playMode);
                    startActivity(intent);
                } else
                    Toast.makeText(DisplayActivity.this, "别点啦,不会有结果的", Toast.LENGTH_SHORT).show();
            }
        });

        album_icon = findViewById(R.id.album_icon);
        play_bar_song_name = findViewById(R.id.play_bar_song_name);
        play_bar_song_author = findViewById(R.id.play_bar_song_author);

        play_bar_btn_play = findViewById(R.id.play_bar_btn_play);
        /*初始化播放按钮点击事件*/
        play_bar_btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.w("DisplayActivity", "btn_Play");
                if (SongsCollector.size() != 0) {
                    switch (current_status) {
                        case MusicService.STATUS_PLAYING:
                            Log.w("DisplayActivity","歌曲正在播放,点击了play按钮,所以暂停");
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
                } else {
                    Toast.makeText(DisplayActivity.this, "本机无歌曲,请下载！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //下一首歌曲按钮
        ImageButton play_bar_btn_next = findViewById(R.id.play_bar_btn_next);
        /*初始化下一首按钮点击事件*/
        play_bar_btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SongsCollector.size() != 0) {
                    sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                } else {
                    Toast.makeText(DisplayActivity.this, "本机无歌曲,请下载！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //历史播放记录按钮
        ImageButton btn_history_menu = findViewById(R.id.btn_history_menu);
        /*历史播放记录按钮*/
        btn_history_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (history_view.getVisibility() == View.GONE) {
                    history_view.setVisibility(View.VISIBLE);
                } else {
                    history_view.setVisibility(View.GONE);
                }

            }
        });

        history_view = findViewById(R.id.history_view);
        history_view.setVisibility(View.GONE);
        //历史播放记录list
        ListView history_list_view = findViewById(R.id.history_list_view);
        adapter_history = new SongAdapter(DisplayActivity.this, R.layout.song_list_item, PlayHistory.getSongs());
        history_list_view.setAdapter(adapter_history);
    }
    /*设置底部的一栏左侧的歌曲名和歌手以及专辑图片*/
    private void updateBottomMes(int position) {//
        Song song = SongsCollector.getSong(position);//获取点击位置的song对象
        play_bar_song_name.setText(song.getTitle());
        play_bar_song_author.setText(song.getArtist());
        //设置专辑图片
        //album_icon.setImageDrawable(getImage(songsList.get(current_number).getAlbum_id()));
        album_icon.setImageBitmap(PictureDealHelper.getAlbumPicture(this, SongsCollector.getSong(current_number).getDataPath(), 120, 120));
    }
    /*---------------------底部播放控制栏初始化结束---------------------*/

    /*---------------------存储权限以及歌曲数据---------------------*/
    /*加载歌曲数据*/
    private void load_Songs_data() {
        if (SongsCollector.size() == 0) {
            myDbFunctions = MyDbFunctions.getInstance(this);//获取数据库操作类实例
            if (!myDbFunctions.isSONGS_Null()) {
                //数据库里面有数据,直接加载数据库里面的
                new GetDB_DataTask(DisplayActivity.this).execute();
                return;
            }
            /*判断是否需要请求权限,然后获取歌曲数据*/
            requestPermissionByHand();
        }else{
            initMySongListView();//已有歌曲,直接初始化控件
        }
    }
    /*向用户请求权限*/
    private void requestPermissionByHand() {
        //判断当前系统的版本
        if (Build.VERSION.SDK_INT >= 23) {//6.0以上
            int checkReadStoragePermission = ContextCompat.checkSelfPermission(
                    DisplayActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
            //如果读取没有被授予
            if (checkReadStoragePermission != PackageManager.PERMISSION_GRANTED) {
                //请求权限,此处可以同时申请多个权限
                ActivityCompat.requestPermissions(
                        DisplayActivity.this, new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, REQ_READ_EXTERNAL_STORAGE);
            } else {//已有权限,加载歌曲
                new ScanMusicTask(DisplayActivity.this).execute();
//                scanMusic();
            }
        }
        else{
            new ScanMusicTask(DisplayActivity.this).execute();
//            scanMusic();
        }
    }

    /*向用户请求权限后的回调*/
    @Override
    public void onRequestPermissionsResult(int requestCode, final String[] permissions, int[] grantResults) {
        if (requestCode == REQ_READ_EXTERNAL_STORAGE) {
            // 如果请求被取消了，那么结果数组就是空的
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予了
                if (SongsCollector.size() == 0) {
                    new ScanMusicTask(DisplayActivity.this).execute();
//                    scanMusic();//加载歌曲数据
                }
            } else {
                Toast.makeText(DisplayActivity.this, "读存储权限申请失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*扫描歌曲文件*/
    /*
     * Params：开始异步任务执行时传入的参数类型；
     * Progress：异步任务执行过程中，返回下载进度值的类型；
     * Result：异步任务执行完成后，返回的结果类型
     * 静态内部类+弱引用防止内存泄漏
     * */
    private static class GetDB_DataTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<DisplayActivity> displayActivityWeakReference ;
        GetDB_DataTask(DisplayActivity displayActivity){
            displayActivityWeakReference = new WeakReference<>(displayActivity);
        }
        //在后台任务开始执行之间调用，在主线程执行
        @Override
        protected void onPreExecute() {
            DisplayActivity displayActivity = displayActivityWeakReference.get();
//            Toast.makeText(displayActivity,"开始加载歌曲数据",Toast.LENGTH_SHORT).show();
            AVLoadingIndicatorView loading_animation = displayActivity.findViewById(R.id.loading_animation);
            loading_animation.setVisibility(View.VISIBLE);//显示加载动画
        }

        //在子线程中运行，处理耗时任务
        @Override
        protected Boolean doInBackground(Void... params) {
            DisplayActivity displayActivity = displayActivityWeakReference.get();
            if(displayActivity != null){
                if(displayActivity.myDbFunctions != null){
                    if (!displayActivity.myDbFunctions.isSONGS_Null()) {
                        //数据库里面有数据,直接加载数据库里面的
                        SongsCollector.setSongsList(displayActivity.myDbFunctions.loadAllSongs());
                        SongsCollector.song_total_number = SongsCollector.size();
                        return true;
                    }
                }
            }
            return false;
        }

        //在这个方法中可以对UI进行操作，在主线程中进行
//        @Override
//        protected void onProgressUpdate(Void... values) {
//        }

        //返回的数据会作为参数传递到此方法中，可以利用返回的数据来进行一些UI操作，在主线程中进行
        @Override
        protected void onPostExecute(Boolean result) {
            DisplayActivity displayActivity = displayActivityWeakReference.get();
            if (result) {
                //调试用
//                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
//                Log.w("异步加载数据库数据完成",df.format(new Date()));
//                Toast.makeText(displayActivity, "加载歌曲数据成功", Toast.LENGTH_SHORT).show();
                /*初始化歌曲列表控件,必须在获取数据后面*/
                displayActivity.initMySongListView();
            } else {
                Toast.makeText(displayActivity, "加载歌曲数据失败", Toast.LENGTH_SHORT).show();
            }
            AVLoadingIndicatorView loading_animation = displayActivity.findViewById(R.id.loading_animation);
            loading_animation.setVisibility(View.GONE);//加载动画消失
        }
    }

    private static class ScanMusicTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<DisplayActivity> displayActivityWeakReference ;
        ScanMusicTask(DisplayActivity displayActivity){
            displayActivityWeakReference = new WeakReference<>(displayActivity);
        }

        @Override
        protected void onPreExecute() {
            DisplayActivity displayActivity = displayActivityWeakReference.get();
//            Toast.makeText(displayActivity,"开始扫描本地歌曲",Toast.LENGTH_SHORT).show();
            AVLoadingIndicatorView loading_animation = displayActivity.findViewById(R.id.loading_animation);
            loading_animation.setVisibility(View.VISIBLE);//加载动画显示
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            DisplayActivity displayActivity = displayActivityWeakReference.get();
            ContentResolver contentResolver = displayActivity.getContentResolver();
            try (Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        //是否是音频
                        int isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
                        //时长
                        long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        //是音乐并且时长大于2分钟
                        if (isMusic != 0 && duration >= 2 * 60 * 1000) {
                            //文件路径
                            String dataPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                            if (SongsCollector.isContainSong(dataPath)) {//数据库中已经有这首歌曲了,所以跳过
                                continue;
                            }
                            //歌名
                            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                            //歌手
                            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                            //专辑id
//                            long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                            //歌名，歌手，时长,文件路径,是否喜爱,专辑,专辑图片,是否使用默认专辑图片
                            Song song = new Song(
                                    title,
                                    artist,
                                    duration,
                                    dataPath,
                                    false,
                                    PictureDealHelper.getAlbumPicture(dataPath,96,96),
                                    false
                            );//R.drawable.song_item_picture是歌曲列表每一项前面那个图标
                            if(!SongsCollector.isContainSong(song.getDataPath())){//只添加当前列表中没有
                                SongsCollector.addSong(song);
                                if(song.getAlbum_icon() == null){
                                    song.setFlagDefaultAlbumIcon(true);
                                }
                                displayActivityWeakReference.get().myDbFunctions.saveSong(song);
                            }//只添加当前列表中没有
                        }//是音乐并且时长大于2分钟
                    }//游标的移动
                }//游标不为空
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        //返回的数据会作为参数传递到此方法中，可以利用返回的数据来进行一些UI操作，在主线程中进行
        @Override
        protected void onPostExecute(Boolean result) {
            DisplayActivity displayActivity = displayActivityWeakReference.get();
            if(result){
                Toast.makeText(displayActivity,"歌曲扫描完毕",Toast.LENGTH_SHORT).show();
                //给一些歌曲添加默认专辑图片
//                int size = SongsCollector.size();//调试用
//                Log.w("歌曲数量有","size = "+size);
                for(int i = 0; i < SongsCollector.size(); i ++){
                    Song s = SongsCollector.getSong(i);
                    if(s.getAlbum_icon() == null){
                        Bitmap b = PictureDealHelper.getAlbumPicture(displayActivity,
                                s.getDataPath(),96,96);
                        s.setAlbum_icon(b);
                    }
                }
                /*初始化歌曲列表控件,必须在获取数据后面*/
                displayActivity.initMySongListView();
                AVLoadingIndicatorView loading_animation = displayActivity.findViewById(R.id.loading_animation);
                loading_animation.setVisibility(View.GONE);//加载动画消失
                if (SongsCollector.size() == 0) {
                    Toast.makeText(displayActivity, "本机无歌曲,请下载！", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(displayActivity,"默认专辑图片出错",Toast.LENGTH_SHORT).show();
            }
        }
    }
//    private void scanMusic(){
//        Toast.makeText(DisplayActivity.this,"开始扫描歌曲",Toast.LENGTH_SHORT).show();
//        ContentResolver contentResolver = getContentResolver();
//        try (Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                null, null, null, null)) {
//            if (cursor != null) {
//                while (cursor.moveToNext()) {
//                    //是否是音频
//                    int isMusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
//                    //时长
//                    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
//                    //是音乐并且时长大于2分钟
//                    if (isMusic != 0 && duration >= 2 * 60 * 1000) {
//                        //文件路径
//                        String dataPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
//                        if (SongsCollector.isContainSong(dataPath)) {//数据库中已经有这首歌曲了,所以跳过
//                            continue;
//                        }
//                        //歌名
//                        String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
//                        //歌手
//                        String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
//                        //专辑id
//                        long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
//                        //歌名，歌手，时长，专辑,图标,文件路径,sequence number of list in display activity
//                        Song song = new Song(
//                                title,
//                                artist,
//                                duration,
//                                dataPath,
//                                SongsCollector.size(),
//                                false,
//                                PictureDealHelper.getAlbumPicture(this, dataPath, 96, 96)
//                        );//R.drawable.song_item_picture是歌曲列表每一项前面那个图标
//                        if(!SongsCollector.isContainSong(song.getDataPath())){//只添加当前列表中没有
//                            SongsCollector.addSong(song);
//                            myDbFunctions.saveSong(song);
//                            SongsCollector.size()++;
//                        }//只添加当前列表中没有
//                    }//是音乐并且时长大于2分钟
//                }//游标的移动
//            }//游标不为空
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        Toast.makeText(this,"歌曲扫描完毕",Toast.LENGTH_SHORT).show();
//        if(adapter_main_song_list_view != null){//通知数据变化
//            adapter_main_song_list_view.notifyDataSetChanged();
//        }
//        if(main_song_list_view != null){
//            main_song_list_view.invalidate();
//        }
//    }
    /*---------------------存储权限以及歌曲数据---结束---------------------*/

    /**
     * 由不可见变为可见的时候调用
     */
    @Override
    protected void onStart() {
        super.onStart();
        //广播接收器重新注册
        if (progressBarReceiver == null) {
            bindBroadcastReceiver();
        }
        //重新加载底部栏的歌名,歌手,专辑图片,播放按钮UI
        current_number = MusicService.getCurrent_number();
        current_status = MusicService.getCurrent_status();
        if (SongsCollector.size() != 0) {//避免空引用错误
            play_bar_song_name.setText(SongsCollector.getSong(current_number).getTitle());
            play_bar_song_author.setText(SongsCollector.getSong(current_number).getArtist());
            album_icon.setImageBitmap(
                    PictureDealHelper.getAlbumPicture(this, SongsCollector.getSong(current_number).getDataPath(), 120, 120));
            if (current_status == MusicService.STATUS_PLAYING) {
                //正在播放
                play_bar_btn_play.setBackground(getDrawable(R.drawable.pause_32));
            } else {
                play_bar_btn_play.setBackground(getDrawable(R.drawable.play_32));
            }
        }
    }

    /**
     * 准备好和用户进行交互的时候调用
     */
    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_REQUEST_DURATION);
    }

    /**
     * Activity正在停止，仍可见
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Activity即将停止，不可见，位于后台,可以做稍微重量级的回收工作
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Activity即将销毁,做一些最终的资源回收
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消广播接收器的注册
        if (statusChangedReceiver != null)
            unregisterReceiver(statusChangedReceiver);
        if (progressBarReceiver != null)
            unregisterReceiver(progressBarReceiver);
        if (current_status == MusicService.STATUS_STOPPED) {
            stopService(new Intent(this, MusicService.class));
            if (countDownTimer != null) {
                countDownTimer.cancel();//撤销定时器防止崩溃
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
            int status = intent.getIntExtra("status", -1);
            if (status != MusicService.PLAY_MODE_UPDATE)
                current_status = status;
            switch (status) {
                //播放器状态更改为正在播放
                case MusicService.STATUS_PLAYING:
                    //把底部播放按钮的图标改变,列表中正在播放的歌曲的颜色改变
                    Log.w("DisplayActivity", "STATUS_PLAYING");
                    play_bar_btn_play.setBackground(getDrawable(R.drawable.pause_32));//改变图标
                    current_number = MusicService.getCurrent_number();//更改存储的当前播放歌曲序号
                    //加载歌名和歌手,设置专辑图片
                    updateBottomMes(current_number);
                    //通知适配器数据变化
                    adapter_main_song_list_view.notifyDataSetChanged();
                    adapter_history.notifyDataSetChanged();
                    break;

                //播放器状态更改为暂停
                case MusicService.STATUS_PAUSED:
                    Log.w("DisplayActivity", "STATUS_PAUSED");
                    play_bar_btn_play.setBackground(getDrawable(R.drawable.play_32));//把底部播放按钮的图标改变
                    //通知适配器数据变化
                    adapter_main_song_list_view.notifyDataSetChanged();
                    break;

                //音乐播放服务已停止
                case MusicService.STATUS_STOPPED:
                    Log.w("DisplayActivity", "STATUS_STOPPED");
                    ActivityCollector.finishAll();
                    break;

                //播放器状态更改为播放完成
                case MusicService.STATUS_COMPLETED:
                    Log.w("DisplayActivity", "STATUS_COMPLETED");
                    break;
                case MusicService.PLAY_MODE_UPDATE:
                    //顺序,单曲,随机 --->  8,9,10
                    //在弹窗中位置分别是0,1,2
                    default_playMode = intent.getIntExtra("playMode", MusicService.PLAY_MODE_ORDER) - 8;
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
            //进度条的广播命令
            int progress_broadcast_content = intent.getIntExtra("content", 0);
            switch (progress_broadcast_content) {
                case MusicService.PROGRESS_DURATION:
                    //Log.w("DisplayActivity","PROGRESS_DURATION");
                    /*用于存储*/
                    //当前的歌曲的总时长
                    int duration = intent.getIntExtra("duration", 0);
                    progressBar_activity_display.setMax(duration);
                    break;
                case MusicService.PROGRESS_UPDATE:
                    current_progress = intent.getIntExtra("current_progress", 0);
                    //Log.w("DisplayActivity","PROGRESS_UPDATE"+current_progress);
                    progressBar_activity_display.setProgress(current_progress);
                    break;
                default:
                    break;
            }
        }
    }

    /*横竖屏切换,应用回调的函数*/
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int orientation = newConfig.orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏操作
            Log.w("DisplayActivity", "竖屏");
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏操作
            Log.w("DisplayActivity", "横屏");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_HEADSETHOOK == keyCode) { //按下了耳机键
            switch (current_status) {
                case MusicService.STATUS_PLAYING:
                    Log.w("DisplayActivity", "按下了耳机键");
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
//        if (event.getRepeatCount() == 0) {  //如果长按的话，getRepeatCount值会一直变大
//            //短按
//        } else {
//            //长按
//        }
        return super.onKeyDown(keyCode, event);
    }

}