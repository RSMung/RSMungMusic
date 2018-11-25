package edu.whut.ruansong.musciplayer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 阮阮 on 2018/11/17.
 * 前端处理UI变化
 */

public class DisplayActivity extends BaseActivity {
    private static List<Song> songsList = new ArrayList<>();
    private HeadsetPlugReceiver headsetReceiver;
    private int num;//当前播放歌曲
    View history_ln_view;//历史播放记录控件
    private int flag = 0;//用来控制历史播放记录控件是否可见
    private int status = MusicService.STATUS_STOPPED;//播放状态默认为停止
    ImageButton imgb_play;//底部的图片播放按钮
    private int input_time = 0;
    private Timer sleepTimer;
    private AlertDialog dialog;//定时停止播放得对话框
    private int playMode = 0;//播放模式
    StatusChangedReceiver receiver;
    private static int song_number = 0;//歌曲总数量
    List<Song> search_list = new ArrayList<>();//用来装查询获取的songs
    LinearLayout search_LinearLayout;//搜索结果的整个布局
    public static int DisplayActivity_life_on = 1;
    public static int DisplayActivity_life_off = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        //启动服务
        Intent intentService = new Intent(DisplayActivity.this, MusicService.class);
        startService(intentService);
        isEmptyUsername();//判断用户名是否为空
        initHeadset();//初始化耳机监听
        initSongs(); // 初始化歌曲数据
        dealMusicButton();//初始化播放按钮点击事件
        dealPlayHistoryButton();//历史播放记录
        dealSearch();
        initDealPlayBarBottom();//点击底部一栏的事件
        bindStatusChangedReceiver();//启动广播接收器
    }

    public void isEmptyUsername() {//判断用户名是否为空
        Intent intent = getIntent();
        String userName = intent.getStringExtra("userName");
        //判断用户名是否为空
        if (userName.isEmpty()) {
            Toast.makeText(DisplayActivity.this, getResources().getString(R.string.null_username),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void initHeadset() {//初始化耳机监听
        //给广播绑定响应的过滤器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        headsetReceiver = new HeadsetPlugReceiver();
        registerReceiver(headsetReceiver, intentFilter);
    }

    class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.getAction().equalsIgnoreCase("android.intent.action.HEADSET_PLUG")) {
                    if (intent.hasExtra("state")) {
                        if (intent.getIntExtra("state", 0) == 1) {
                            Toast.makeText(context, "耳机已连接", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "耳机已断开", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**************初始化歌曲数据************/
    private void initSongs() {
        if(songsList.size()==0){
            Log.w("DisplayActivity", "歌曲列表为空，需要加载");
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        // 获取歌手信息
                        String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                        //获取歌曲名称
                        String disName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                        //获取文件路径
                        String url = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                        Song song = new Song(song_number, R.drawable.music_2, disName, artist, url);
                        //R.drawable.music_2是歌曲列表前面那个图片
                        songsList.add(song);
                        song_number++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            if(songsList.size()==0){
                Toast.makeText(DisplayActivity.this,"没有找到歌曲，请下载！",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }else{
            Log.w("DisplayActivity", "歌曲列表不为空，直接载入");
        }
        //配置歌曲信息
        SongAdapter adapter = new SongAdapter(DisplayActivity.this, R.layout.song_item, songsList);
        ListView listView = findViewById(R.id.list_song);
        listView.setAdapter(adapter);
        //设置歌曲item点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                num = position;
                if(status==MusicService.STATUS_STOPPED ||status==MusicService.STATUS_PLAYING){
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                }else if(status==MusicService.STATUS_PAUSED){
                    sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                }
            }
        });
    }

    /**************初始化播放按钮点击事件************/
    public void dealMusicButton() {
        ImageButton b_paly = findViewById(R.id.button_play);
        b_paly.setOnClickListener(new View.OnClickListener() {//播放
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

    /*发送命令，控制音乐播放，参数定义在MusicService中*/
    private void sendBroadcastOnCommand(int command) {
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", command);
        //根据不同的命令封装不同的数据
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number", num);//封装歌曲id
                break;
            case MusicService.COMMAND_PREVIOUS:
                break;
            case MusicService.COMMAND_NEXT:
                break;
            case MusicService.COMMAND_PAUSE:
                break;
            case MusicService.COMMAND_STOP:
                break;
            case MusicService.COMMAND_RESUME:
                int add = 0;
                //把历史播放列表的最后一首歌曲在主播放列表的位置传出去
                for (int i = 0; i < songsList.size(); i++) {
                    String name = PlayHistory.songs.get(PlayHistory.songs.size() - 1).getSong_name();
                    if (songsList.get(i).getSong_name().equals(name)) {
                        add = i;
                        break;
                    }
                }
                intent.putExtra("number", add);
                break;
            default:
                break;
        }
        sendBroadcast(intent);
        Log.w("DisplatActivity", "发送了命令广播" + command);
    }

    /*内部类，接受广播命令并执行操作*/
    class StatusChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取播放器状态
            status = intent.getIntExtra("status", -1);
            switch (status) {
                case MusicService.STATUS_PLAYING:
                    Log.w("DisplayActivity", "播放状态，改变播放图标.");
                    imgb_play = findViewById(R.id.button_play);
                    imgb_play.setBackground(getDrawable(R.drawable.pause));//把底部播放按钮的图标改变
                    initButtomMes(MusicService.getCurrent_number());
//                    initImagePlay(1,MusicService.getCurrent_number());
                    break;
                case MusicService.STATUS_PAUSED:
                    Log.w("DisplayActivity", "暂停状态，将改变播放图标.");
                    imgb_play = findViewById(R.id.button_play);
                    imgb_play.setBackground(getDrawable(R.drawable.play_2));//把底部播放按钮的图标改变
                    break;
                case MusicService.STATUS_STOPPED:
                    Log.w("DisplayActivity", "停止状态");
                    break;
                case MusicService.STATUS_COMPLETED:
                    Log.w("DisplayActivity", "已经播放完毕，播放下一首！");
                    break;
                case MusicService.PLAYMODE_LOOP://单曲循环模式
                    playMode = 1;
                    break;
                case MusicService.PLAYMODE_RANDOM://单曲循环模式
                    playMode = 2;
                    break;
                default:
                    break;
            }
        }
    }

    public void initButtomMes(int position) {//设置底部的一栏左侧的歌曲名和歌手
        Song song = songsList.get(position);//获取点击位置的song对象
        TextView songname = findViewById(R.id.buttom_textview_songname);
        songname.setText(song.getSong_name());
        TextView songauthor = findViewById(R.id.buttom_textview_songauthor);
        songauthor.setText(song.getSong_author());
    }

    public void dealPlayHistoryButton() {//历史播放记录按钮点击
        ImageButton history_menu = findViewById(R.id.history_menu);
        history_ln_view = findViewById(R.id.history_ln_view);
        history_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag == 0) {
                    SongAdapter adapter_his = new SongAdapter(DisplayActivity.this, R.layout.song_item, PlayHistory.songs);
                    ListView list_playhistory = findViewById(R.id.list_playhistory);
                    list_playhistory.setAdapter(adapter_his);
                    history_ln_view.setVisibility(View.VISIBLE);
                    flag = 1;
                } else {
                    history_ln_view.setVisibility(View.GONE);
                    flag = 0;
                }

            }
        });
    }

    public void initDealPlayBarBottom() {//底部一整栏的点击事件
        View v = findViewById(R.id.play_bar_bottom);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
    }

    //添加顶部右侧 menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //menu的事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pauseplay_item:
                timePausePlay();
                break;
            case R.id.mode_play:
                selectMode();
                break;
            case R.id.exit:
                ActivityCollector.finishAll();
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
                break;
            default:
                break;
        }
        return true;
    }

    public void dealSearch() {
        SearchView searchview = findViewById(R.id.searchview);
        searchview.setSubmitButtonEnabled(true);
        searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.w("DisplayActivity", "开始处理搜素");
                Log.w("DisplayActivity", "query是" + query);
                for (int flag = 0; flag < song_number; flag++) {//遍历整个列表
                    Log.w("DisplayActivity", "第"+flag+"次遍历");
                    String current_song_name = songsList.get(flag).getSong_name();
                    if (current_song_name.equals(query)) {
                        search_list.add(songsList.get(flag));
                        break;
                    }
                }
                if (search_list.isEmpty()) {
                    Toast.makeText(DisplayActivity.this, "搜索结果为空",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DisplayActivity.this, "搜索完毕，准备显示结果",
                            Toast.LENGTH_SHORT).show();
                    //用search_list配置歌曲信息
                    SongAdapter adapter_search = new SongAdapter(DisplayActivity.this,
                            R.layout.song_item, search_list);
                    ListView listView_search = findViewById(R.id.list_search);
                    listView_search.setAdapter(adapter_search);
                    //设置search_list歌曲item点击事件
                    listView_search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view,
                                                int position, long id) {
                            num = search_list.get(position).getSong_id();
                            if(status==MusicService.STATUS_STOPPED ||status==MusicService.STATUS_PLAYING){
                                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                            }else if(status==MusicService.STATUS_PAUSED){
                                sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                            }

                        }
                    });
                    search_LinearLayout = findViewById(R.id.search_LinearLayout);
                    Log.w("DisplayActivity", "结果列表应该可见了才对");
                    search_LinearLayout.setVisibility(View.VISIBLE);
                    ImageView close = findViewById(R.id.image_close_search);
                    close.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Log.w("DisplayActivity", "结果列表应该bu可见了才对");
                            search_LinearLayout.setVisibility(View.GONE);
                            search_list.clear();
                        }
                    });
                }
                return false;
            }

            //搜索框内部改变回调，newText就是搜索框里的内容
            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
    }

    public void timePausePlay() {//定时停止播放
        final AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(DisplayActivity.this);
        final View dialogView = LayoutInflater.from(DisplayActivity.this)
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

//    //定义定时器任务暂停播放
//    TimerTask timerTask_PAUSEPLAY = new TimerTask() {
//        @Override
//        public void run() {
//            sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
//        }
//    };

    public void selectMode() {//选择播放模式
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
                    intent_mode.putExtra("command", MusicService.PLAYMODE_ORDER);
                } else if (which == 1) {
                    intent_mode.putExtra("command", MusicService.PLAYMODE_LOOP);
                } else if (which == 2) {
                    intent_mode.putExtra("command", MusicService.PLAYMODE_RANDOM);
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


    //绑定广播接收器
    private void bindStatusChangedReceiver() {
        receiver = new StatusChangedReceiver();
        IntentFilter intentFilter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
    }

    /*在onDestroy()方法中通过调用unregisterReceiver()方法来取消耳机广播接收器的注册*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(headsetReceiver);
        unregisterReceiver(receiver);
        if (status == MusicService.STATUS_STOPPED) {
            stopService(new Intent(this, MusicService.class));
            sleepTimer.cancel();//撤销定时器防止崩溃
        }
    }

    public static List<Song> getSongsList() {
        return songsList;
    }

    public static int getSong_number() {
        return song_number;
    }

//    public void initImagePlay(int status, int position) {
//        if (status == 1) {
//            ListView list_songs = findViewById(R.id.list_song);//再让某个特定位置的image可见
//            View position_item = list_songs.getChildAt(position);
//            LinearLayout l = position_item.findViewById(R.id.song_item);
//            ImageView song_image = l.findViewById(R.id.song_image);
//            song_image.setVisibility(View.VISIBLE);
//        } else {
//
//        }
//    }
}