package edu.whut.ruansong.musicplayer.tool;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Created by Android Studio.
 * User: lvdou-jack
 * Date: 2019/6/6
 * Time: 21:16
 * 使用socket与本机进行通信实现密码验证
 */
public class SocketLogin extends Thread{
    private String user,password;
    private String ip = "112.124.66.85";
    //本地服务器ip  192.168.31.174
    //云服务器ip   112.124.66.85
    private int port = 1998;
    private String return_state_string;//用于保存主机返回的密码验证状态
    private int respond_state_int = -1;
    public SocketLogin(String user, String password){
        this.user = user;
        this.password = password;
    }
    public int getRespond_state_int(){
        return respond_state_int;
    }
    @Override
    public void run(){
        Log.w("SocketLogin", "进入run方法");
        Socket socket = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try{
            //创建客户端socket对象
            socket = new Socket(ip,port);
            //获取输出流
            outputStream = socket.getOutputStream();

            //先发送用户名，再发送密码
            // 首先需要计算得知消息的长度
            byte[] userBytes = user.getBytes(StandardCharsets.UTF_8);
            // 然后将消息的长度优先发送出去
            outputStream.write(userBytes.length >> 8);
            outputStream.write(userBytes.length);
            //然后将消息发送出去
            outputStream.write(userBytes);
            outputStream.flush();

            //发送密码
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            outputStream.write(passwordBytes.length >> 8);
            outputStream.write(passwordBytes.length);

            outputStream.write(passwordBytes);
            outputStream.flush();

            //接收主机返回的密码验证信息
            inputStream = socket.getInputStream();
            //先读长度信息
            int L1 = 0;
            int L2 = 0;
            int length = 0;
            while(true) {
                //获取数据的长度
//                Log.w("SocketLogin", "read上");
                L1 = inputStream.read();//先读第一个字节
//                Log.w("SocketLogin", "read下");
                if (L1 == -1)
                    break;
                L2 = inputStream.read();
                length = (L1 << 8) + L2;
                //接收数据
                //构造一个该长度的数组
                byte[] bytes = new byte[length];
                //读取该长度的信息
                inputStream.read(bytes);
                return_state_string = new String(bytes, StandardCharsets.UTF_8);
                Log.w("SocketLogin","return_state_string is "+ return_state_string);
                if(return_state_string.equals("true"))
                    respond_state_int = 1;
                else if(return_state_string.equals("false"))
                    respond_state_int = 0;
                Log.w("SocketLogin","state is "+ respond_state_int);
            }
        }catch (Exception e){
            Log.w("SocketLogin",e);
        }finally {
            try {//关闭资源
                if(outputStream != null)
                    outputStream.close();
                if(socket != null)
                    socket.close();
            }catch (Exception e){
                Log.w("SocketLogin",e);
            }
        }
    }
}
