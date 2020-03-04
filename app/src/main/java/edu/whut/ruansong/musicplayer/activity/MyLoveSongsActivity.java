package edu.whut.ruansong.musicplayer.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import java.util.List;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.BaseActivity;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.service.MusicService;
import edu.whut.ruansong.musicplayer.tool.MyDbFunctions;
import edu.whut.ruansong.musicplayer.tool.MyDbHelper;
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
        for(Song s:myLoveSongs){//设置专辑图片
            s.setAlbum_picture(getAlbumPicture(s.getDataPath(),120,120));
        }
        SongAdapter adapter = new SongAdapter(this,R.layout.song_list_item,myLoveSongs);
        ListView listView = findViewById(R.id.listView_activity_myLoveSongs);
        listView.setAdapter(adapter);
        current_number = MusicService.getCurrent_number();
        current_status = MusicService.getCurrent_status();
        /***设置search_list歌曲item点击事件   以便可以点击搜素结果 播放歌曲*/
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //播放控制逻辑
                actual_number = myLoveSongs.get(position).getList_id_display();
                if(current_status == MusicService.STATUS_PLAYING){//播放状态
                    if(current_number == actual_number){//点击的正在播放的歌曲
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);//暂停
                    }else{//点击的别的歌曲
                        current_number = actual_number;
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
    }
    /***发送命令，控制音乐播放，参数定义在MusicService中*/
    private void sendBroadcastOnCommand(int command) {
        //1.创建intent
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
}
