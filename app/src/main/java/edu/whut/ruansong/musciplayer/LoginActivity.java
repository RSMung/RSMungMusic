package edu.whut.ruansong.musciplayer;


import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import edu.whut.ruansong.musciplayer.dynamicBackGround.VideoBackground;

public class LoginActivity extends BaseActivity {

    private VideoBackground login_videoview = null;
    private int curVolume;
    private static int statusMusicPlayer = 0;//登录状态
    private Button b_login;
    private CheckBox remember_user, remember_password;
    private TextView login_pas;
    private TextView login_user;
    private String passwordStr;//用来存放密码
    private String userStr;//存放用户名
    private SharedPreferences.Editor sPreEditor_usr, sPreEditor_password;
    private ProgressDialog progDia1;
    private ImageButton login_ban_music;
    private int flag_ban_music = 0;//控制禁止音乐图片用的

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("LoginActivity", "onCreate被执行了");
        Log.w("LoginActivity", "statusMusicPlayer是" + statusMusicPlayer);
        if (statusMusicPlayer == 0) {//未登录情况
            setContentView(R.layout.activity_login);
            b_login = findViewById(R.id.button_login);//登录按钮实例化
            initButtonDeal();//处理登录按钮的事件
            initBackground();//初始化动态背景
            loadDefaultMsg();//加载默认用户名和密码
            ban_music_button();
        } else {//已经登陆过  这样做是为了登录后退出app再进入不需要再次登录
            jumpToNextPage();
        }
    }

    public void jumpToNextPage() {
        Intent intent = new Intent(LoginActivity.this, DisplayActivity.class);
        intent.putExtra("userName", userStr);
        startActivity(intent);
    }

    public void initButtonDeal() {
        //处理确定按钮的事件
        b_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login_pas = findViewById(R.id.login_pas);//密码文本框对象
                login_user = findViewById(R.id.login_user);//用户名文本框对象
                userStr = login_user.getText().toString();//获得输入的用户名（文本）
                passwordStr = login_pas.getText().toString();//获得输入的密码（文本）
                if (!userStr.equals("") && !passwordStr.equals("")) {//如果都不为空
                    /******保存用户名和密码*****/
                    //检测是否想要记住用户名
                    sPreEditor_usr = getSharedPreferences("username_data",
                            MODE_PRIVATE).edit();
                    if (remember_user.isChecked()) {
                        sPreEditor_usr.putString("name", userStr);//存入数据
                    } else {
                        sPreEditor_usr.clear();//清除数据
                    }
                    sPreEditor_usr.apply();//生效

                    //检测是否想要记住密码
                    sPreEditor_password = getSharedPreferences("password_data",
                            MODE_PRIVATE).edit();
                    if (remember_password.isChecked()) {
                        sPreEditor_password.putString("password", passwordStr);//保存
                    } else {
                        sPreEditor_password.clear();//清除
                    }
                    sPreEditor_password.apply();//生效

                    /******使用socket与主机通信验证密码*****/
                    final socketLogin s1 = new socketLogin(userStr, passwordStr);
                    s1.start();

                    new Handler().postDelayed(new Runnable() {//延时1000ms后获取主机返回的验证状态
                        @Override
                        public void run() {
                            int login_state = s1.getYes();//获取主机验证后返回的信息
                            Log.w("LoginActivity", "state is " + login_state);
                            if (login_state == 1) {//验证成功
                                jumpToNextPage();
                                statusMusicPlayer = 1;
                            } else {//验证失败
                                Toast.makeText(LoginActivity.this, "账号验证失败，请重试！",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, 1000);

                } else {
                    Toast.makeText(LoginActivity.this,
                            "账号、密码不能为空！", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void initBackground() {
        //找VideoView控件
        login_videoview = findViewById(R.id.login_videoview); //加载视频布局
        login_videoview.setVideoURI(Uri.parse("android.resource://" + getPackageName() +
                "/" + R.raw.five));
        // 播放
        login_videoview.start();
        //循环播放  设置一个在媒体文件播放完毕，到达终点时调用的回调
        login_videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                login_videoview.start();
            }
        });
    }

    public void loadDefaultMsg() {//加载默认用户名和密码

        remember_user = findViewById(R.id.remember_user);
        login_user = findViewById(R.id.login_user);
        //加载默认用户名
        SharedPreferences pref = getSharedPreferences("username_data", MODE_PRIVATE);
        String default_name = pref.getString("name", "");
        if (!default_name.isEmpty()) {
            login_user.setText(default_name);
        }

        remember_password = findViewById(R.id.remember_password);
        login_pas = findViewById(R.id.login_pas);
        //加载默认密码
        SharedPreferences pref_pas = getSharedPreferences("password_data", MODE_PRIVATE);
        String default_password = pref_pas.getString("password", "");
        if (!default_password.isEmpty()) {
            login_pas.setText(default_password);
        }
    }

    public void ban_music_button() {//禁止背景音乐播放
        login_ban_music = findViewById(R.id.login_ban_music);
        login_ban_music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag_ban_music == 0) {//正在播放
                    login_ban_music.setImageDrawable(getResources().getDrawable(R.drawable.music_false));
                    flag_ban_music = 1;
                    //禁止声音之前返回当前音量   自写VideoBackground类中
                    curVolume = login_videoview.setVolume(0);
                } else {
                    login_ban_music.setImageDrawable(getResources().getDrawable(R.drawable.music_true));
                    flag_ban_music = 0;
                    login_videoview.setVolume(curVolume);
                }
            }
        });
    }

    public static void setStatusMusicPlayer(int statusMusicPlayer) {//后台服务MusicService中有用到
        LoginActivity.statusMusicPlayer = statusMusicPlayer;
    }

    //返回重启加载
    @Override
    protected void onRestart() {
        super.onRestart();
        Log.w("LoginActivity", "onRestart被执行了");
        initBackground();
    }

    //防止锁屏或者切出的时候，背景音乐在播放
    @Override
    protected void onStop() {
        super.onStop();
        if (login_videoview != null)
            login_videoview.stopPlayback();
    }
}