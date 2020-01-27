package edu.whut.ruansong.musicplayer.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import edu.whut.ruansong.musicplayer.tool.ActivityCollector;
import edu.whut.ruansong.musicplayer.tool.BaseActivity;
import edu.whut.ruansong.musicplayer.service.MusicService;
import edu.whut.ruansong.musicplayer.tool.PlayHistory;
import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.tool.Song;
import edu.whut.ruansong.musicplayer.tool.SongAdapter;

/**
 * Created by 阮 on 2018/11/17.
 * 前端处理UI变化
 */

public class DisplayActivity extends BaseActivity {
    private Toolbar toolbar = null;//toolbar

    private HeadsetPlugReceiver headsetReceiver = null;//耳机监听

    private static List<Song> songsList = new ArrayList<>();//歌曲数据
    private static int song_total_number = 0;//歌曲总数

    private StatusChangedReceiver statusChangedReceiver;//状态接收器，接收来自service的播放器状态信息

    public static final int PLAY_MODE_ORDER = 8;//顺序播放(默认是它)
    public static final int PLAY_MODE_LOOP = 9;//单曲循环
    public static final int PLAY_MODE_RANDOM = 10;//随机播放
    private int playMode = PLAY_MODE_ORDER;//播放模式,默认顺序播放

    private int current_music_list_number;//当前正在播放的歌曲
    private int status;//播放状态默认为停止
    private View view_history;//历史播放记录控件
    private int view_history_Flag = 0;//用来控制历史播放记录控件是否可见
    private ImageButton image_btn_play;//底部的图片播放按钮
    private int input_time = 0;
    private Timer sleepTimer = null;
    private AlertDialog dialog;//定时停止播放得对话框

    private ImageView image_music;
    private int headSet_flag = 0;
    private final int REQ_READ_EXTERNAL_STORAGE = 1;//权限请求码,1代表外部存储权限
    private int login_state = 0;//0是未登录,1是已登陆

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //解决软键盘弹起时，底部控件被顶上去的问题
//        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        /***设定布局*/
        setContentView(R.layout.activity_display);

        /***toolbar控件*/
        toolbar = findViewById(R.id.toolbar_activity_search_detail);
        setSupportActionBar(toolbar);

        /***启动后台服务*/
        startService();

        /***初始化耳机监听*/
        initHeadset();

        /**请求权限(歌曲总数为0则加载歌曲数据)*/
        requestPermissionByHand();
        if(song_total_number == 0)
            load_Songs_data();//加载歌曲数据

        /***初始化播放按钮点击事件*/
        dealMusicButton();

        /***初始化下一首按钮点击事件*/
        dealNextMusicButton();

        /***历史播放记录*/
        dealPlayHistoryButton();

        /***点击底部一栏的事件*/
        initDealPlayBarBottom();

        /***启动广播接收器*/
        bindStatusChangedReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
        headSet_flag = 1;
    }

    /***********toolbar的menu***********/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_display, menu);
        return true;
    }

    //点击事件
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

    /***启动音乐播放服务*/
    public void startService() {
        Intent intentService = new Intent(DisplayActivity.this, MusicService.class);
        startService(intentService);
    }

    /**
     * 耳机监听广播注册
     **/
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
                if ("android.intent.action.HEADSET_PLUG".equalsIgnoreCase(intent.getAction()))
                    //有状态信息
                    if (intent.hasExtra("state"))
                        //耳机断开
                        if (intent.getIntExtra("state", 0) != 1)
                            //音乐正在播放
                            if (status == MusicService.STATUS_PLAYING)
                                if (headSet_flag == 0) {//在onResume方法中此标志被置1
                                    //音乐暂停
                                    Log.w("DisplayActivity", "耳机断开，歌曲正在播放，即将暂停");
                                    sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                                } else
                                    //打开App后首次插入耳机会置0
                                    headSet_flag = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**************加载歌曲数据************/
    private void load_Songs_data() {
        if (songsList.size() == 0) {
            Log.w("DisplayActivity", "歌曲列表为空，需要加载");
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null, null, null, null);
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
                            long albumId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
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
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            if (song_total_number == 0) {
                Toast.makeText(DisplayActivity.this, "本机无歌曲,请下载！", Toast.LENGTH_SHORT).show();
                return;//不做下面的事情了(即不用把歌曲信息载入列表,因为根本没有)
            }
        } else {
            Log.w("DisplayActivity", "歌曲列表不为空，直接载入");
        }
        //配置歌曲信息
        SongAdapter adapter_view_list_song = new SongAdapter(DisplayActivity.this,
                R.layout.song_list_item, songsList);
        ListView view_list_all_song = findViewById(R.id.view_list_all_song);
        view_list_all_song.setAdapter(adapter_view_list_song);

        /**设置歌曲item点击事件*/
        view_list_all_song.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                current_music_list_number = position;
                if (status == MusicService.STATUS_STOPPED || status == MusicService.STATUS_PLAYING) {
                    //在musicService服务中有逻辑控制到底是播放还是暂停   play_pause()函数
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                } else if (status == MusicService.STATUS_PAUSED) {
                    sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                }
            }
        });
    }

    /**************初始化播放按钮点击事件************/
    public void dealMusicButton() {
        ImageButton b_Paly = findViewById(R.id.btn_play);
        b_Paly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//播放按钮
                switch (status) {
                    case MusicService.STATUS_PLAYING:
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED:
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED:
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                }
            }
        });
    }

    /**************历史播放记录按钮点击************/
    public void dealPlayHistoryButton() {
        ImageButton btn_history_menu = findViewById(R.id.btn_history_menu);
        view_history = findViewById(R.id.view_list_history);
        btn_history_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view_history_Flag == 0) {
                    SongAdapter adapter_his = new SongAdapter(DisplayActivity.this, R.layout.song_list_item, PlayHistory.songs);
                    ListView list_playHistory = findViewById(R.id.list_playhistory);
                    list_playHistory.setAdapter(adapter_his);
                    view_history.setVisibility(View.VISIBLE);
                    view_history_Flag = 1;
                } else {
                    view_history.setVisibility(View.GONE);
                    view_history_Flag = 0;
                }

            }
        });
    }

    /**
     * 下一首歌按钮点击事件
     */
    public void dealNextMusicButton() {
        ImageButton btn_next_music = findViewById(R.id.btn_next);
        btn_next_music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });
    }

    /**
     * 底部一整栏的点击事件  待实现歌曲详情页
     **/
    public void initDealPlayBarBottom() {
        View v = findViewById(R.id.play_bar_bottom);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DisplayActivity.this, "你点击了底部栏！",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*******绑定广播接收器,接收来自服务的播放器状态更新消息*/
    private void bindStatusChangedReceiver() {
        statusChangedReceiver = new StatusChangedReceiver();
        IntentFilter intentFilter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(statusChangedReceiver, intentFilter);
    }

    /***发送命令，控制音乐播放，参数定义在MusicService中*/
    private void sendBroadcastOnCommand(int command) {
        //1.创建intent
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        //2.封装数据
        intent.putExtra("command", command);
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number", current_music_list_number);//封装歌曲在list中的位置
                break;
            case MusicService.COMMAND_RESUME:
                intent.putExtra("number", current_music_list_number);
                break;
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
            status = intent.getIntExtra("status", -1);
            switch (status) {
                //播放器状态更改为正在播放
                case MusicService.STATUS_PLAYING:
                    //把底部播放按钮的图标改变
                    Log.w("DisplayActivity", "播放状态，改变播放图标.");
                    image_btn_play = findViewById(R.id.btn_play);
                    image_btn_play.setBackground(getDrawable(R.drawable.pause));
                    current_music_list_number = MusicService.getCurrent_number();
                    //加载歌名和歌手
                    initBottomMes(current_music_list_number);
                    //设置专辑图片
                    image_music = findViewById(R.id.image_music);
                    image_music.setImageDrawable(getImage(songsList.get(current_music_list_number).getAlbum_id()));
                    break;

                //播放器状态更改为暂停
                case MusicService.STATUS_PAUSED:
                    Log.w("DisplayActivity", "暂停状态，将改变播放图标.");
                    image_btn_play = findViewById(R.id.btn_play);
                    image_btn_play.setBackground(getDrawable(R.drawable.play_2));//把底部播放按钮的图标改变
                    break;

                //播放器状态更改为停止
                case MusicService.STATUS_STOPPED:
                    Log.w("DisplayActivity", "停止状态");
                    break;

                //播放器状态更改为播放完成
                case MusicService.STATUS_COMPLETED:
                    Log.w("DisplayActivity", "已经播放完毕，播放下一首!");
                    break;

                case MusicService.PLAY_MODE_LOOP://单曲循环模式
                    playMode = PLAY_MODE_LOOP;//更新本活动内播放模式变量
                    break;

                case MusicService.PLAY_MODE_RANDOM://单曲循环模式
                    playMode = PLAY_MODE_RANDOM;//更新本活动内播放模式变量
                    break;

                default:
                    break;
            }
        }
    }

    /**************一些工具方法类****************/

    /**
     * 设置底部的一栏左侧的歌曲名和歌手
     */
    public void initBottomMes(int position) {//
        Song song = songsList.get(position);//获取点击位置的song对象
        TextView songName = findViewById(R.id.buttom_textview_songname);
        songName.setText(song.getTitle());
        TextView songAuthor = findViewById(R.id.buttom_textview_songauthor);
        songAuthor.setText(song.getArtist());
    }

    /**********获取  设置歌曲专辑图片*************/
    @SuppressWarnings("deprecation")
    private BitmapDrawable getImage(long albumId) {
        BitmapDrawable bmpDraw;
        String albumArt = getAlbumArt(albumId);
        Bitmap bm;
        if (albumArt != null) {
//            Log.w("DisplayActivity","albumArt不为空");
            bm = BitmapFactory.decodeFile(albumArt);
            bmpDraw = new BitmapDrawable(bm);
            int width = bmpDraw.getIntrinsicWidth();
            int height = bmpDraw.getIntrinsicHeight();
            // drawable转换成bitmap
            Bitmap oldbmp = bmpDraw.getBitmap();
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) 400 / width);
            float sy = ((float) 400 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
                    matrix, false);
            return new BitmapDrawable(newbmp);
        } else {
//            Log.w("DisplayActivity","albumArt为空");
            Resources res = getResources();
            Bitmap default_d = BitmapFactory.decodeResource(res,
                    R.drawable.music1);
            int width = default_d.getWidth();
            int height = default_d.getHeight();
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) 400 / width);
            float sy = ((float) 400 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            Bitmap newbmp = Bitmap.createBitmap(default_d, 0, 0, width, height,
                    matrix, false);
            return new BitmapDrawable(newbmp);
        }
    }

    private String getAlbumArt(long album_id) {
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[]{"album_art"};
        Cursor cur = this.getContentResolver().query(Uri.parse(mUriAlbums + "/" +
                        Long.toString(album_id)), projection, null, null,
                null);
        String album_art = null;
        if (cur != null) {
            if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
                cur.moveToNext();
                album_art = cur.getString(0);
            }
            cur.close();
        }
        return album_art;
    }
    /***获取  设置歌曲专辑图片           到此结束*/

    /****旧的menu的点击事件         已淘汰!!!!*/
    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pauseplay_item://延时停止播放
                timePausePlay();
                break;
            case R.id.mode_play://播放模式
                selectMode();
                break;
            case R.id.exit://退出播放器
                ActivityCollector.finishAll();
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
                break;
            default:
                break;
        }
        return true;
    }

     */

    /**
     * 定时停止播放
     */
    public void timePausePlay() {
        final AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(DisplayActivity.this);
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(DisplayActivity.this)
                .inflate(R.layout.dialog, null);
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
                Toast.makeText(DisplayActivity.this, "任务已经取消",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 选择播放模式
     */
    public void selectMode() {//
        final AlertDialog.Builder builder = new AlertDialog.Builder(DisplayActivity.this);
        builder.setIcon(R.drawable.play_1);
        builder.setTitle("播放模式");
        final String[] mode = {"顺序播放(默认)", "单曲循环", "随机播放"};
        /*设置一个单项选择框
         * 第一个参数指定要显示的一组下拉单选框的数据集合
         * 第二个参数代表索引，指定默认哪一个单选框被勾选上，0表示默认'顺序播放' 会被勾选上
         * 第三个参数给每一个单选项绑定一个监听器
         */
        final Intent intent_mode = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        builder.setSingleChoiceItems(mode, playMode, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(DisplayActivity.this,
                        "播放模式为：" + mode[which], Toast.LENGTH_SHORT).show();
                if (which == 0) {
                    intent_mode.putExtra("command", MusicService.PLAY_MODE_ORDER);
                } else if (which == 1) {
                    intent_mode.putExtra("command", MusicService.PLAY_MODE_LOOP);
                } else if (which == 2) {
                    intent_mode.putExtra("command", MusicService.PLAY_MODE_RANDOM);
                }
                sendBroadcast(intent_mode);
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, final String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQ_READ_EXTERNAL_STORAGE:
                // 如果请求被取消了，那么结果数组就是空的
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限被授予了
                    if(song_total_number == 0)
                        load_Songs_data();//加载歌曲数据
                } else {
                    Toast.makeText(DisplayActivity.this, "申请权限失败", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /*在onDestroy()方法中通过调用unregisterReceiver()方法来取消耳机广播接收器的注册*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (headsetReceiver != null)
            unregisterReceiver(headsetReceiver);
        unregisterReceiver(statusChangedReceiver);
        if (status == MusicService.STATUS_STOPPED) {
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
        ActivityCollector.finishAll();
    }

    public static List<Song> getSongsList() {
        return songsList;
    }

    public static int getSong_total_number() {
        return song_total_number;
    }
}