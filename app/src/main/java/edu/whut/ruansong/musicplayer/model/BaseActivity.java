package edu.whut.ruansong.musicplayer.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by 阮 on 2018/11/17.
 */

public class BaseActivity extends AppCompatActivity {
    private static Context context;//内存泄露风险，待解决

    /*活动首次创建的时候调用*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("BaseActivity",getClass().getSimpleName()+"进入了onCreate");
        ActivityCollector.addActivity(this);//add oneself to the activity manager
    }
    /*活动由不可见变为可见的时候调用*/
    @Override
    protected void onStart(){
        super.onStart();
        Log.w("BaseActivity",getClass().getSimpleName()+"进入了onStart");
    }
    /*活动准备好和用户进行交互的时候调用*/
    @Override
    protected void onResume(){
        super.onResume();
        Log.w("BaseActivity",getClass().getSimpleName()+"进入了onResume");
    }
    /*系统准备去启动或者恢复另外一个活动的时候调用/*/
    @Override
    protected void onPause(){
        super.onPause();
        Log.w("BaseActivity",getClass().getSimpleName()+"进入了onPause");
    }
    /*活动完全不可见的时候调用*/
    @Override
    protected void onStop(){
        super.onStop();
        Log.w("BaseActivity",getClass().getSimpleName()+"进入了onStop");
    }
    /*活动被销毁之前调用*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w("BaseActivity",getClass().getSimpleName()+"进入了onDestroy");
        ActivityCollector.removeActivity(this);//remove oneself from the activity manager
    }
    /*停止状态变为运行状态前调用*/
    @Override
    protected void onRestart(){
        Log.w("BaseActivity",getClass().getSimpleName()+"进入了onRestart");
        super.onRestart();
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        Log.w("BaseActivity", getClass().getSimpleName()+"进入onBackPressed");
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.w("BaseActivity", getClass().getSimpleName()+"进入onSaveInstanceState");
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        Log.w("BaseActivity", getClass().getSimpleName()+"进入onRestoreInstanceState");
    }
    public static Context getContext() {
        return context;
    }
}
