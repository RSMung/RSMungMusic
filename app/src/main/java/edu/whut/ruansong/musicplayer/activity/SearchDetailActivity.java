package edu.whut.ruansong.musicplayer.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.Song;
import edu.whut.ruansong.musicplayer.model.SongsCollector;
import edu.whut.ruansong.musicplayer.tool.SongAdapter;
import edu.whut.ruansong.musicplayer.service.MusicService;

public class SearchDetailActivity extends AppCompatActivity {

    private List<Song> songsList = null;
    private List<Song> search_list = new ArrayList<>();//用来装查询结果
    private int current_number = 0;//当前正在播放的歌曲
    private int current_status;//播放状态默认为停止
    private LinearLayout search_LinearLayout;//搜索结果的整个布局
    private int actual_number = 0;//在search_view里面点击的歌曲在display_activity的list里面的位置
    private SongAdapter adapter_search;
    private ListView listView_search ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_detail);
        Log.w("SearchDetailActivity", "进入onCreate");
        Toolbar toolbar = findViewById(R.id.toolbar_activity_display);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent_back = new Intent(SearchDetailActivity.this, DisplayActivity.class);
                startActivity(intent_back);
            }
        });
        current_number = MusicService.getCurrent_number();
        current_status = MusicService.getCurrent_status();
        search_LinearLayout = findViewById(R.id.search_LinearLayout);
        listView_search = findViewById(R.id.list_search);
        /***设置search_list歌曲item点击事件   以便可以点击搜素结果 播放歌曲*/
        listView_search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //把更新过的list传回display_activity,然后在onStart方法中通知了适配器数据变化
//                    DisplayActivity.setSongsList(songsList);
                //播放控制逻辑
                actual_number = SongsCollector.getSongIndex(search_list.get(position));
                if(current_status == MusicService.STATUS_PLAYING){//播放状态
                    if(current_number == actual_number){//点击的正在播放的歌曲
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);//暂停
                    }else{//点击的别的歌曲
                        current_number = actual_number;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                }else if(current_status == MusicService.STATUS_PAUSED){//暂停状态
                    if(current_number == actual_number){
                        //应恢复播放
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                    }else{
                        //点击的别的歌曲
                        current_number = actual_number;
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    }
                }else {//停止状态
                    current_number = actual_number;
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                }
            }
        });
        ImageView close_search = findViewById(R.id.image_close_search);//x 按钮 关闭搜索结果
        close_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                search_LinearLayout.setVisibility(View.GONE);
                search_list.clear();//清除搜索结果
            }
        });
        //用search_list配置歌曲信息，加载进入控件
        adapter_search = new SongAdapter(SearchDetailActivity.this, R.layout.song_list_item, search_list);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w("SearchDetailActivity", "进入onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w("SearchDetailActivity", "进入onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w("SearchDetailActivity", "进入onPause");
    }

    /**
     * 活动不可见时调用
     */
    @Override
    protected void onStop() {
        //this.finish();//用toolbar的返回键回退后,再用系统的回退键,不会再退回到搜索界面
        //或者在manifest文件中定义DisplayActivity启动模式为singleTask
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w("SearchDetailActivity", "进入onDestroy");
        search_list = null;//防止内存泄露
        songsList = null;
    }

    /***********toolbar的menu***********/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_detail_activity, menu);
        MenuItem search_item = menu.getItem(0);
        SearchView searchView = (SearchView) search_item.getActionView();
        searchView.onActionViewExpanded();//展开模式
        //搜索框提示文字
        searchView.setQueryHint(this.getResources().getString(R.string.search_hint));

        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(R.id.search_src_text);
        searchAutoComplete.setTextColor(this.getResources().getColor(R.color.white_color));
        searchAutoComplete.setHintTextColor(this.getResources().getColor(R.color.white_color));
//        searchView.setSubmitButtonEnabled(true);//提交按钮  显示
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if(!TextUtils.isEmpty(query)){//注意判空
                    search_list.clear();
                    adapter_search.notifyDataSetChanged();
                    listView_search.deferNotifyDataSetChanged();
                    dealSearchAction(query);
                }
                return false;
            }
        });
        return true;
    }

    //menu点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                break;
        }
        return true;
    }

    private void dealSearchAction(String mes) {
        //使用  暴力匹配算法（Brute Force Algorithm）

        //将输入的字符串转换为字符数组
        char[] charArray_mes = mes.toCharArray();
        int mes_len = charArray_mes.length;//获得这个字符数组的长度
        //获取本地歌曲列表
        songsList = SongsCollector.getSongsList();
        int num_songs = songsList.size();
        if (songsList.isEmpty() || !(SongsCollector.size()>0)  ) {
            Toast.makeText(SearchDetailActivity.this, "本机无歌曲,请下载", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.w("SearchDetailActivity","搜索正在执行");
        String current_song_name;
        char[] charArray_current_song_name;
        int cur_name_len;
        float matching_degree = 0;//匹配值
        float percent_matching_degree;//百分比匹配度
        for (int i = 0; i < num_songs; i++) {//遍历整个列表  匹配歌曲名
//            Log.w("SearchDetailActivity","第" + i + "次遍历");
            //获取列表中当前歌曲名
            current_song_name = songsList.get(i).getTitle();
            //当前歌曲名转换为字符数组
            charArray_current_song_name = current_song_name.toCharArray();
            //当前歌曲名转换为的字符数组的长度
            cur_name_len = charArray_current_song_name.length;
//            Log.w("SearchDetailActivity", "目标长度"+mes_len+"    当前歌名长度"+cur_name_len);
            /**目标歌名比当前歌名长则一定不匹配*/
            if (mes_len <= cur_name_len) {//如果目标歌名比当前歌名短或者长度相等
                short flag_current_mes = 0;//两个循环标志
                short flag_input_mes = 0;
                /***计算匹配值*/
                //循环比较每一个字符
                while (true) {
                    //有一个字符相同
                    if (charArray_current_song_name[flag_current_mes] == charArray_mes[flag_input_mes]) {
                        flag_current_mes++;//两个数组同时向后移动
                        flag_input_mes++;
                        matching_degree++;//匹配值加一
                    } else {//如果不匹配就只是当前歌名字符向后移动一个
                        flag_current_mes++;
                    }
                    //两个数组任意一个的循环标志到达末尾后结束本次比较
                    if (flag_input_mes == mes_len || flag_current_mes == cur_name_len) {
                        break;
                    }
                }
                /***计算百分比匹配度*/
                //相同的字符占目标歌名和当前歌名的平均比例
                percent_matching_degree = (matching_degree / mes_len + matching_degree / cur_name_len) / 2;
//                Log.w("SearchDetailActivity", "匹配度是"+matching_degree);
//                Log.w("SearchDetailActivity", "匹配百分比是"+percent_matching_degree);
                //百分之百匹配，结束查找
                if (percent_matching_degree == 1) {
                    search_list.add(songsList.get(i));
//                    Log.w("SearchDetailActivity", "百分之百匹配,直接退出");
                    break;
                } else if (percent_matching_degree > 0.2) {//阈值为0.2
//                    Log.w("SearchDetailActivity", "百分比匹配度大于阈值");
                    search_list.add(songsList.get(i));
                }
            }
            //匹配度清零
            matching_degree = 0;
            percent_matching_degree = 0;
        }//for (int i = 0; i < num_songs; i++)到此结束

        if (search_list.isEmpty()) {//如果搜索结果为空，提示
            Toast.makeText(SearchDetailActivity.this, "搜索结果为空", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(SearchDetailActivity.this, "搜索完毕，显示结果", Toast.LENGTH_SHORT).show();
            try{
                adapter_search.notifyDataSetChanged();
                listView_search.setAdapter(adapter_search);
            }catch (Exception e){
                Log.w("SearchDetailActivity",e);
            }

            if(!search_list.isEmpty()){//搜索结果不为空
                search_LinearLayout.setVisibility(View.VISIBLE);//显示搜素结果
            }
        }
    }

    /***发送命令，控制音乐播放，参数定义在MusicService中*/
    private void sendBroadcastOnCommand(int command) {
        //1.创建intent
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        //2.封装数据
        intent.putExtra("command", command);
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number", current_number);//封装歌曲在list中的位置
                break;
            case MusicService.COMMAND_RESUME:
            case MusicService.COMMAND_PAUSE:
            default:
                break;
        }
        //3.发送广播
        sendBroadcast(intent);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int orientation = newConfig.orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏操作
            Log.w("SearchDetailActivity","竖屏");
        }
        else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏操作
            Log.w("SearchDetailActivity","横屏");
        }
    }
}
