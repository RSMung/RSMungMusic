package edu.whut.ruansong.musciplayer;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Android Studio.
 * User: lvdou-jack
 * Date: 2019/6/6
 * Time: 21:16
 * 使用socket与本机进行通信实现密码验证
 */
public class socketLogin extends Thread{
    private String user,password;
    private String ip = "192.168.31.174";//主机ip
    private int port = 1234;
    private String r_state;//用于保存主机返回的密码验证状态
    private int yes = 0;
    public socketLogin(String user,String password){
        this.user = user;
        this.password = password;
    }
    public int getYes(){
        return yes;
    }
    @Override
    public void run(){
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
            byte[] sendBytes = user.getBytes("UTF-8");
            // 然后将消息的长度优先发送出去
            outputStream.write(sendBytes.length >> 8);
            outputStream.write(sendBytes.length);
            //然后将消息发送出去
            outputStream.write(sendBytes);
            outputStream.flush();

            //发送密码
            byte[] sendBytes2 = password.getBytes("UTF-8");
            outputStream.write(sendBytes2.length >> 8);
            outputStream.write(sendBytes2.length);

            outputStream.write(sendBytes2);
            outputStream.flush();

            //接收主机返回的密码验证信息
            inputStream = socket.getInputStream();
            //先读长度信息
            int L1 = 0;
            while(true) {
                L1 = inputStream.read();//先读第一个字节
                if (L1 == -1)
                    break;
                int L2 = inputStream.read();
                int length = (L1 << 8) + L2;//获取了数据的长度
                //构造一个该长度的数组
                byte[] bytes = new byte[length];
                //读取该长度的信息
                inputStream.read(bytes);
                r_state = new String(bytes, "UTF-8");
                if(r_state.equals("true"))
                    yes = 1;
                else if(r_state.equals("flase"))
                    yes = 0;
                Log.w("socketLogin","state is "+yes);
            }
        }catch (Exception e){
            Log.w("socketLogin",e);
        }finally {
            try {//关闭资源
                if(outputStream != null)
                    outputStream.close();
                if(socket != null)
                    socket.close();
            }catch (Exception e){
                Log.w("socketLogin",e);
            }
        }
    }
}
