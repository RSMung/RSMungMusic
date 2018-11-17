package edu.whut.ruansong.musciplayer;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.VideoView;

import edu.whut.ruansong.musciplayer.dynamicBackGround.VideoBackground;

public class LoginActivity extends AppCompatActivity {

    VideoBackground videoBackground;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initBackground();
    }

    public void initBackground() {
        //找VideoView控件
        videoBackground = (VideoBackground) findViewById(R.id.videoview); //加载视频文件
        videoBackground.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.two));
        // 播放
        videoBackground.start();
        //循环播放
        videoBackground.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                videoBackground.start();
            }
        });
    }
    //返回重启加载
    @Override
    protected void onRestart() {
        super.onRestart();
        initBackground();
        }
    //防止锁屏或者切出的时候，音乐在播放
    @Override
    protected void onStop() {
        super.onStop();
        videoBackground.stopPlayback();
    }
}
