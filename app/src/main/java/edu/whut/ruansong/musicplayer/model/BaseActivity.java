package edu.whut.ruansong.musicplayer.model;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by 阮 on 2018/11/17.
 */

public class BaseActivity extends AppCompatActivity {
    private static Context context;//内存泄露风险，待解决

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);//add oneself to the activity manager
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);//remove oneself from the activity manager
    }
    public static Context getContext() {
        return context;
    }
}
