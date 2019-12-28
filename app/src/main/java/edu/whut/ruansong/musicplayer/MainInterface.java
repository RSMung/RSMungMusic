package edu.whut.ruansong.musicplayer;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

public class MainInterface extends BaseActivity{
    private Toolbar toolbar = null;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_interface);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar_maininterface,menu);
        return true;
    }
}
