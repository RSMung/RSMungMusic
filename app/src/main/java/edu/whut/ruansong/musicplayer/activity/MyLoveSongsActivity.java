package edu.whut.ruansong.musicplayer.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import java.util.List;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.BaseActivity;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.model.SongsCollector;
import edu.whut.ruansong.musicplayer.service.MusicService;
import edu.whut.ruansong.musicplayer.db.MyDbFunctions;
import edu.whut.ruansong.musicplayer.tool.PictureDealHelper;
import edu.whut.ruansong.musicplayer.tool.SongAdapter;

public class MyLoveSongsActivity extends BaseActivity {
    private static List<Song> myLoveSongs = new ArrayList<>();//有序可重复
    private static MyDbFunctions myDbFunctions;
    private int current_number,current_status,actual_number;
    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_my_love_songs);
        //toolbar相关
        Toolbar toolbar = findViewById(R.id.toolbar_activity_myLoveSongs);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {//toolbar回退键
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyLoveSongsActivity.this, DisplayActivity.class);
                startActivity(intent);
                finish();
            }
        });
        //数据库相关
        myDbFunctions = MyDbFunctions.getInstance(this);
        //list相关
        myLoveSongs = myDbFunctions.loadMyLoveSongs();//从数据库加载,注意加载出来的这些Song对象没有设置专辑图片
        final SongAdapter adapter = new SongAdapter(this,R.layout.song_list_item,myLoveSongs);
        final ListView listView = findViewById(R.id.listView_activity_myLoveSongs);
        listView.setAdapter(adapter);
        current_number = MusicService.getCurrent_number();
        current_status = MusicService.getCurrent_status();

        //待修改,有BUG
        /***设置search_list歌曲item点击事件   以便可以点击搜素结果 播放歌曲*/
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //播放控制逻辑
                actual_number = SongsCollector.getSongIndex(myLoveSongs.get(position));
                if(current_status == MusicService.STATUS_PLAYING){//播放状态
                    if(current_number == actual_number){//点击的正在播放的歌曲
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);//暂停
                    }else{//点击的别的歌曲
                        current_number = actual_number;
//                        Log.w("MyLoveSongsActivity","current_number: "+current_number);
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                }else if(current_status == MusicService.STATUS_PAUSED){//暂停状态
                    if(current_number == actual_number){
                        //应恢复播放
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                    }else{
                        //点击的别的歌曲
                        current_number = actual_number;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                }else {//停止状态
                    current_number = actual_number;
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                }
            }
        });
        //设置more_options按钮
        adapter.setOnItemMoreOptionsClickListener(new SongAdapter.onItemMoreOptionsListener() {
            @Override
            public void onMoreOptionsClick(final int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MyLoveSongsActivity.this)
                        .setTitle("请确认!")
                        .setIcon(R.drawable.danger)
                        .setMessage("确认要从喜爱的歌曲列表中删除此歌曲吗?")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                /*which
                                 *int BUTTON_POSITIVE = -1; int BUTTON_NEGATIVE = -2;   int BUTTON_NEUTRAL = -3;*/
//                                Toast.makeText(DisplayActivity.this,"你点击了确定"+position,Toast.LENGTH_SHORT).show();
                                //判断是否该歌曲正在播放
                                //从数据库中删除该歌曲的喜爱标志
                                if(myDbFunctions != null){
                                    myDbFunctions.setLove(myLoveSongs.get(position).getDataPath(),"false");
                                }
                                //修改主列表中的喜爱标志
                                SongsCollector.getSong(SongsCollector.getSongIndex(myLoveSongs.get(position))).setLove(false);
                                //从内存喜爱的歌曲列表中删除该歌曲
                                myLoveSongs.remove(position);
                                //通知列表数据变化了
                                if(adapter != null){
                                    adapter.notifyDataSetChanged();
                                }
                                if(listView != null){
                                    listView.invalidate();
                                }
                                Toast.makeText(MyLoveSongsActivity.this,"已删除",Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MyLoveSongsActivity.this,"下次不要点错了哦",Toast.LENGTH_SHORT).show();
                            }
                        })
                        ;
                builder.create().show();
            }
        });
    }
    /***发送命令，控制音乐播放，参数定义在MusicService中*/
    private void sendBroadcastOnCommand(int command) {
        //1.创建intent
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        //2.封装数据
        intent.putExtra("command", command);
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number" , current_number);//封装歌曲在list中的位置
                break;
            case MusicService.COMMAND_RESUME:
            case MusicService.COMMAND_PAUSE:
            default:
                break;
        }
        //3.发送广播
        sendBroadcast(intent);
    }
}
