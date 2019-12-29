package edu.whut.ruansong.musicplayer.activity;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import edu.whut.ruansong.musicplayer.R;

public class SearchDetailActivity extends AppCompatActivity {

    Toolbar toolbar = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_detail);
        toolbar = findViewById(R.id.toolbar_activity_search_detail);
        setSupportActionBar(toolbar);
    }

    /***********toolbar的menu***********/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_detail_activity, menu);
        MenuItem search_item = menu.getItem(0);
        SearchView searchView = (SearchView)search_item.getActionView();
        searchView.onActionViewExpanded();//展开模式
        //搜索框提示文字
        searchView.setQueryHint(this.getResources().getString(R.string.search_hint));

        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(R.id.search_src_text);
        searchAutoComplete.setTextColor(this.getResources().getColor(R.color.white_color));
        searchAutoComplete.setHintTextColor(this.getResources().getColor(R.color.white_color));
        searchView.setSubmitButtonEnabled(true);//提交按钮  显示
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //使用  暴力匹配算法（Brute Force Algorithm）
                double matching_degree = 0;//匹配值
                double percent_matching_degree;//百分比匹配度
                return false;
            }
        });
        return true;
    }
    //点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                break;
        }
        return true;
    }

}
