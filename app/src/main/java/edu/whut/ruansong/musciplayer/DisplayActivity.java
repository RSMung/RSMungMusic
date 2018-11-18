package edu.whut.ruansong.musciplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 阮阮 on 2018/11/17.
 * 显示本地音乐文件目录
 */

public class DisplayActivity extends BaseActivity {
    private static List<Song> songsList = new ArrayList<>();
    private HeadsetPlugReceiver headsetReceiver;
    private Intent intent;
    private String userName;
    private IntentFilter intentFilter;
    private SeekBar seekBar;
    private static MediaPlayer player = new MediaPlayer();//媒体播放器
    private int num;//当前播放歌曲
    private String path;//歌曲path
    private int time;//歌曲时长
    View history_ln_view;//历史播放记录控件
    private int flag = 0;//用来控制历史播放记录控件是否可见

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        initWelcome();//初始化欢迎信息
        initHeadset();//初始化耳机监听
        dealExitButton();
        initSongs(); // 初始化歌曲数据
        dealMusicButton();//初始化四个播放相关的按钮点击事件
        initDealPlayBarBottom();//点击底部一栏的事件
    }

    private void initSongs() {
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
                    Song song = new Song(R.drawable.music, disName, artist, url);
                    songsList.add(song);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
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
                Song song = songsList.get(position);
//                Toast.makeText(DisplayActivity.this, song.getSong_addr(), Toast.LENGTH_SHORT).show();
                TextView songname = findViewById(R.id.buttom_textview_songname);
                songname.setText(song.getSong_name());
                TextView songauthor = findViewById(R.id.buttom_textview_songauthor);
                songauthor.setText(song.getSong_author());
//                MusicPlay musicPlay = new MusicPlay(position,song.getSong_addr());
                path = song.getSong_addr();
//                Log.w("当前位置","当前位置是"+position);

                if (player.isPlaying()) {
                    if (position == num)//点击的正在播放的歌曲
                    {
                        player.pause();
//                        Log.w("MusicPlay","点击的正在播放的歌曲"+position+",暂停播放！");
                    } else {//点击了新歌曲
                        player.stop();
                        initMediaPlayer();
                        play();
                        PlayHistory.addSong(song);//添加进历史记录
//                        Log.w("MusicPlay","点击了新歌曲,重新播放！");
                    }
                } else {
                    initMediaPlayer();
                    play();
                    PlayHistory.addSong(song);//添加进历史记录
                }
                num = position;

//                seekBar = findViewById(R.id.music_progress);
//                seekBar.setProgress(musicPlay.getTime());
            }
        });
    }

    /*在onDestroy()方法中通过调用unregisterReceiver()方法来取消广播接收器的注册*/
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(headsetReceiver);
        MusicPlay.release();
    }

    public void initHeadset() {
        //给广播绑定响应的过滤器
        intentFilter = new IntentFilter();
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

    public void initWelcome() {
        intent = getIntent();
        userName = intent.getStringExtra("userName");
//        Toast.makeText(SecondActivity.this,userName, Toast.LENGTH_LONG).show();//调试用
        //设置欢迎信息
        if (userName.isEmpty()) {
            Toast.makeText(DisplayActivity.this, getResources().getString(R.string.null_username),
                    Toast.LENGTH_SHORT).show();
        } else {
            TextView welcome = findViewById(R.id.text_welcome);
            welcome.setText(getResources().getString(R.string.welcome) + "," + userName + "!");
        }
    }

    public void dealMusicButton() {
        ImageButton b_paly = findViewById(R.id.button_play);
        ImageButton history_menu = findViewById(R.id.history_menu);
        b_paly.setOnClickListener(new View.OnClickListener() {//播放
            @Override
            public void onClick(View view) {
                if(player.isPlaying()){
                    player.pause();
                }else if(player!=null){
                    player.start();
                }
            }
        });
        history_menu.setOnClickListener(new View.OnClickListener() {//历史播放记录
            @Override
            public void onClick(View view) {
                if(flag==0){
                    SongAdapter adapter_his = new SongAdapter(DisplayActivity.this, R.layout.song_item, PlayHistory.songs);
                    ListView list_playhistory = findViewById(R.id.list_playhistory);
                    list_playhistory.setAdapter(adapter_his);
                    history_ln_view = findViewById(R.id.history_ln_view);
                    history_ln_view.setVisibility(View.VISIBLE);
                    flag = 1;
                }else{
                    history_ln_view = findViewById(R.id.history_ln_view);
                    history_ln_view.setVisibility(View.GONE);
                    flag = 0;
                }

            }
        });
    }

    public void dealExitButton() {
        //退出按钮的点击事件
        Button exit = findViewById(R.id.button_exit_welcome);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DisplayActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    public void initDealPlayBarBottom() {
        View v = findViewById(R.id.play_bar_bottom);
        v.setOnClickListener(new View.OnClickListener() {//点击事件
            @Override
            public void onClick(View view) {
            }
        });
    }

    public void initMediaPlayer() {
        try {
            player.reset();
            player.setDataSource(path);
            player.prepare();
            time = player.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (!player.isPlaying()) {
            player.start();
        } else {
            player.pause();
        }
    }
    //添加 menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //menu的点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pauseplay_item:
                timePausePlay();
                break;
            case R.id.off_item:
                timeOff();
                break;
        }
        return true;
    }
    public void timePausePlay(){//定时停止播放

    }
    public void timeOff(){//定时关机

    }
    private void sendBroadcastOnCommand(int command){
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command",command);
        //根据不同的命令封装不同的数据
        switch (command){
        }
    }
    public static List<Song> getSongsList(){
        return songsList;
    }
}