package edu.whut.ruansong.musciplayer;


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
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import edu.whut.ruansong.musciplayer.dynamicBackGround.VideoBackground;

public class LoginActivity extends BaseActivity {

    private VideoBackground videoBackground;
    private Button b_ok;
    private CheckBox ck,ck_password;
    private TextView password;
    private TextView user;
    private String pas;//用来存放密码
    private String userStr;//存放用户名
    private SharedPreferences.Editor editor,editor_pas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initButtonDeal();//处理登录按钮的事件
        initBackground();//初始化动态背景
        dealCheckBox();//处理复选框相关事件，有部分在匹配密码后处理
    }

    public void initBackground() {
        //找VideoView控件
        videoBackground = findViewById(R.id.videoview); //加载视频布局
        videoBackground.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.stars));
        // 播放
        videoBackground.start();
        //循环播放  设置一个在媒体文件播放完毕，到达终点时调用的回调
        videoBackground.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                videoBackground.start();
            }
        });
    }
    public Handler  handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch (msg.what){
                case 1://获取服务器成功
                    String reponse = (String) msg.obj;
                    //代表身份验证成功
                    if(reponse.contains(":1")){
                        Intent intent = new Intent(LoginActivity.this, DisplayActivity.class);
                        intent.putExtra("userName", userStr);
                        startActivity(intent);
                    }else{
                        Toast.makeText(LoginActivity.this, "账号验证失败，请重试！", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case 0://获取服务器数据失败
                    Toast.makeText(LoginActivity.this, "获取服务器数据失败", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };
    //发送网络请求的方法
    public void sendRequestWithHttpClient(final String username,final  String password){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //用HttpClient发送请求分为五步
                //第一步，创建HttpClient对象
                HttpClient httpClient = new DefaultHttpClient();
                //第二步，创建代表请求的对象，参数是访问的服务器的地址
                /*真机和run服务器的主机连接到同一个局域网，
                URL中IP的值就是run服务器主机的局域网地址*/
                //如果在模拟器上面进行测试，那么 IP的值设置为 10.0.2.2
                String LOGIN_URL = "http://192.168.31.174:8080/Demo_Login/android/" +
                        "loginServlet.jsp?";
                LOGIN_URL = LOGIN_URL+"name="+username+"&password="+password;
                HttpGet httpGet = new HttpGet(LOGIN_URL);

                try {
                    //第三步，执行请求，并获取服务器发还的相应对象
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    //第四步，检查相应的状态是否正常，检查状态码的值是200表示正常
                    if(httpResponse.getStatusLine().getStatusCode() ==200){
                        //第五步，从相应对象中取出数据，放到entity中
                        HttpEntity entity = httpResponse.getEntity();
                        //把数据转换为字符串
                        String responce = EntityUtils.toString(entity,"utf-8");

                        //在子线程中将Message对象发送出去
                        Message message = new Message();
                        message.what = 1;//代表获取数据获取成功
                        message.obj = responce.toString();
                        handler.sendMessage(message);

                        //检测是否想要记住用户名
                        editor = getSharedPreferences("username_data",
                                MODE_PRIVATE).edit();
                        if (ck.isChecked()) {
                            editor.putString("name", userStr);//存入数据
                        } else {
                            editor.clear();//清除数据
                        }
                        editor.apply();

                        //检测是否想要记住密码
                        editor_pas = getSharedPreferences("password_data",
                                MODE_PRIVATE).edit();
                        if(ck_password.isChecked()){
                            editor_pas.putString("password",pas);
                        }else{
                            editor_pas.clear();
                        }
                        editor_pas.apply();
                    }

                }catch (Exception e){
                    //获取服务器数据出错
                    Message message = new Message();
                    message.what = 0;//代表获取数据出错
                    handler.sendMessage(message);

                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void initButtonDeal(){
        b_ok = findViewById(R.id.button_ok);//处理确定按钮的事件
        b_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                password = findViewById(R.id.edtext_pas);//密码文本框
                user = findViewById(R.id.edtext_user);//用户名文本框
                userStr = user.getText().toString();//获得输入的用户名（文本）
                pas = password.getText().toString();//获得输入的密码（文本）
//                Log.w("调试","userStr is"+userStr);
//                Log.w("调试","pas is"+pas);
                if(!userStr.equals("")&&!pas.equals("")){//如果都不为空
                    //开启新线程去请求服务器中的数据
                    sendRequestWithHttpClient(userStr,pas);
                    Log.w("调试","第二个运行了");
                }else{
                    Toast.makeText(LoginActivity.this,
                            "账号或者密码不能为空！",Toast.LENGTH_SHORT).show();
                }

//                if (pas.equals("123456")) {//密码正确
//                    //获取用户名放入userStr
//                    userStr = user.getText().toString();
                    //检测是否想要记住用户名
//                    editor = getSharedPreferences("username_data",
//                            MODE_PRIVATE).edit();
//                    if (ck.isChecked()) {
//                        editor.putString("name", userStr);//存入数据
//                    } else {
//                        editor.clear();//清除数据
//                    }
//                    editor.apply();
                    //检测是否想要记住密码
//                    editor_pas = getSharedPreferences("password_data",
//                            MODE_PRIVATE).edit();
//                    if(ck_password.isChecked()){
//                        editor_pas.putString("password",pas);
//                    }else{
//                        editor_pas.clear();
//                    }
//                    editor_pas.apply();
//                    //跳入display活动
//                    Intent intent = new Intent(LoginActivity.this, DisplayActivity.class);
//                    intent.putExtra("userName", userStr);
//                    startActivity(intent);
//
//                    ActivityCollector.removeActivity(LoginActivity.this);
//                    finish();
//                } else {//密码不正确，弹窗
//                    Toast.makeText(LoginActivity.this, R.string.login_error, Toast.LENGTH_SHORT).show();
//                }
            }
        });
    }
    public void dealCheckBox(){//加载默认用户名和密码
        ck = findViewById(R.id.ckBox);
        user = findViewById(R.id.edtext_user);
        //加载默认用户名
        SharedPreferences pref = getSharedPreferences("username_data", MODE_PRIVATE);
        String default_name = pref.getString("name", "");
        if (!default_name.isEmpty()) {
            user.setText(default_name);
        }

        ck_password = findViewById(R.id.remember_password);
        password = findViewById(R.id.edtext_pas);
        //加载默认密码
        SharedPreferences pref_pas = getSharedPreferences("password_data", MODE_PRIVATE);
        String default_password = pref_pas.getString("password", "");
        if (!default_password.isEmpty()) {
            password.setText(default_password);
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