package edu.whut.ruansong.musciplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import edu.whut.ruansong.musciplayer.dynamicBackGround.VideoBackground;

public class LoginActivity extends BaseActivity {

    private VideoBackground videoBackground;
    private Button b_exit;
    private Button b_ok;
    private CheckBox ck;
    private TextView password;
    private TextView user;
    private String pas;//用来存放密码
    private String userStr;//存放用户名
    private SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initButtonDeal();//处理两个按钮的事件
        initBackground();//初始化动态背景
        dealCheckBox();//处理复选框相关事件，有部分在匹配密码后处理
    }

    public void initBackground() {
        //找VideoView控件
        videoBackground = (VideoBackground) findViewById(R.id.videoview); //加载视频文件
        videoBackground.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.three_3));
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
    public void initButtonDeal(){
        b_exit = findViewById(R.id.button_exit);//处理退出按钮的事件
        b_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCollector.finishAll();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });
        b_ok = findViewById(R.id.button_ok);//处理确定按钮的事件
        b_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                password = findViewById(R.id.edtext_pas);//密码文本框
                pas = password.getText().toString();//获得输入的密码（文本）

                if (pas.equals("123456")) {//密码正确
                    //获取用户名放入userStr
                    userStr = user.getText().toString();
                    //检测是否想要记住用户名
                    editor = getSharedPreferences("username_data",
                            MODE_PRIVATE).edit();
                    if (ck.isChecked()) {
                        editor.putString("name", userStr);//存入数据
                    } else {
                        editor.clear();//清除数据
                    }
                    editor.apply();
                    //跳入display活动
                    Intent intent = new Intent(LoginActivity.this, DisplayActivity.class);
                    intent.putExtra("userName", userStr);
                    startActivity(intent);
                } else {//密码不正确，弹窗
                    Toast.makeText(LoginActivity.this, R.string.login_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void dealCheckBox(){
        ck = findViewById(R.id.ckBox);
        user = findViewById(R.id.edtext_user);
        //加载默认用户名
        SharedPreferences pref = getSharedPreferences("username_data", MODE_PRIVATE);
        String default_name = pref.getString("name", "");
        if (!default_name.isEmpty()) {
            user.setText(default_name);
        }
    }
    //返回重启加载
    @Override
    protected void onRestart() {
        super.onRestart();
        initBackground();
    }
    //防止锁屏或者切出的时候，背景音乐在播放
    @Override
    protected void onStop() {
        super.onStop();
        videoBackground.stopPlayback();
    }
}