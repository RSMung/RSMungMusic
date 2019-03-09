package edu.whut.ruansong.musciplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by 阮 on 2018/11/17.
 */

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }
}
