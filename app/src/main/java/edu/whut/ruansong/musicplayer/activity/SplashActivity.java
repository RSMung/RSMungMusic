package edu.whut.ruansong.musicplayer.activity;

import android.content.Intent;
import android.os.Bundle;

import edu.whut.ruansong.musicplayer.model.BaseActivity;

/*用于解决应用启动有白屏的问题*/
public class SplashActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this,DisplayActivity.class));
        finish();
    }
}
