package edu.whut.ruansong.musciplayer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 阮 on 2018/11/17.
 * 前端处理UI变化
 */

public class DisplayActivity extends BaseActivity {
    private HeadsetPlugReceiver headsetReceiver = null;
    private static int song_number = 0;//歌曲总数量
    private int current_music;//当前播放歌曲
    private static List<Song> songsList = new ArrayList<>();
    private int status;//播放状态默认为停止
    private View history_ln_view;//历史播放记录控件
    private int menuFlag = 0;//用来控制历史播放记录控件是否可见
    private ImageButton image_button_play;//底部的图片播放按钮
    private int input_time = 0;
    private Timer sleepTimer = null;
    private AlertDialog dialog;//定时停止播放得对话框
    private int playMode = 0;//播放模式
    private StatusChangedReceiver receiver;//状态接收器，接收来自service的播放器状态信息
    private List<Song> search_list = new ArrayList<>();//用来装查询获取的songs
    private LinearLayout search_LinearLayout;//搜索结果的整个布局
    private ImageView image_music;
    private SearchView searchview = null;
    private int headSet_flag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //解决软键盘弹起时，底部控件被顶上去的问题
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_display);
        startService();//启动后台服务
        initHeadset();//初始化耳机监听
        initSongs(); // 初始化歌曲数据
        dealMusicButton();//初始化播放按钮点击事件
        dealPlayHistoryButton();//历史播放记录
        dealSearch();//搜索本地歌曲的逻辑
        initDealPlayBarBottom();//点击底部一栏的事件
        bindStatusChangedReceiver();//启动广播接收器
    }

    public void startService() {
        Intent intentService = new Intent(DisplayActivity.this, MusicService.class);
        startService(intentService);
    }

    public void initHeadset() {//初始化耳机监听
        //给广播绑定响应的过滤器
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        headsetReceiver = new HeadsetPlugReceiver();
        registerReceiver(headsetReceiver, intentFilter);
    }

    //内部类，接收耳机状态变化
    class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                //耳机相关广播
                if ("android.intent.action.HEADSET_PLUG".equalsIgnoreCase(intent.getAction()))
                    //有状态信息
                    if (intent.hasExtra("state"))
                        //耳机断开
                        if (intent.getIntExtra("state", 0) != 1)
                            //音乐正在播放
                            if (status == MusicService.STATUS_PLAYING)
                                if(headSet_flag == 0){//如果是resume方法被执行过,这个变量是1
                                    //音乐暂停
                                    Log.w("DisplayActivity", "耳机断开，歌曲正在播放，即将暂停");
                                    sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                                }else
                                    headSet_flag = 0;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**************加载歌曲数据************/
    private void initSongs() {
        if (songsList.size() == 0) {
            Log.w("DisplayActivity", "歌曲列表为空，需要加载");
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        //是否是音频
                        int isMusic = cursor.getInt(cursor.getColumnIndex(
                                MediaStore.Audio.Media.IS_MUSIC));
                        //时长
                        long duration = cursor.getLong(
                                cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        //是音乐并且时长大于3分钟
                        if (isMusic != 0 && duration >= 3 * 60 * 1000) {
                            long id = cursor.getLong(
                                    cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                            //歌手信息
                            String artist = cursor.getString(
                                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                            //歌曲名称
                            String musicName = cursor.getString(
                                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                            //文件路径
                            String musicPath = cursor.getString(
                                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                            //专辑id
                            long albumId = cursor.getInt(
                                    cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                            //专辑
                            String album = cursor.getString(
                                    cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                            Song song = new Song(
                                    id, albumId, song_number,
                                    R.drawable.music_2,//song_image_id   图片在drawable里面的id
                                    musicName, artist, musicPath,
                                    duration, isMusic, album
                            );//R.drawable.music_2是歌曲列表前面那个图片
                            songsList.add(song);
                            song_number++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            if (song_number == 0) {
                Toast.makeText(DisplayActivity.this, "没有找到歌曲,请下载！",
                        Toast.LENGTH_SHORT).show();
                return;//不做下面的事情了
            }
        } else {
            Log.w("DisplayActivity", "歌曲列表不为空，直接载入");
        }
        //配置歌曲信息
        SongAdapter adapter = new SongAdapter(DisplayActivity.this,
                R.layout.song_item, songsList);
        ListView listView = findViewById(R.id.list_song);
        listView.setAdapter(adapter);
        //设置歌曲item点击事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                current_music = position;
                if (status == MusicService.STATUS_STOPPED || status == MusicService.STATUS_PLAYING) {
                    //在musicService服务中有逻辑控制到底是播放还是暂停   play_pause()函数
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                } else if (status == MusicService.STATUS_PAUSED) {
                    sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                }
            }
        });
    }

    /**************初始化播放按钮点击事件************/
    public void dealMusicButton() {
        ImageButton b_Paly = findViewById(R.id.button_play);
        b_Paly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//播放按钮
                switch (status) {
                    case MusicService.STATUS_PLAYING:
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED:
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.STATUS_STOPPED:
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        break;
                }
            }
        });
    }

    /**************历史播放记录按钮点击************/
    public void dealPlayHistoryButton() {
        ImageButton history_menu = findViewById(R.id.history_menu);
        history_ln_view = findViewById(R.id.history_ln_view);
        history_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchview.clearFocus();
                if (menuFlag == 0) {
                    SongAdapter adapter_his = new SongAdapter(DisplayActivity.this, R.layout.song_item, PlayHistory.songs);
                    ListView list_playHistory = findViewById(R.id.list_playhistory);
                    list_playHistory.setAdapter(adapter_his);
                    history_ln_view.setVisibility(View.VISIBLE);
                    menuFlag = 1;
                } else {
                    history_ln_view.setVisibility(View.GONE);
                    menuFlag = 0;
                }

            }
        });
    }

    public void dealSearch() {
        searchview = findViewById(R.id.searchview);
        searchview.setSubmitButtonEnabled(true);//提交按钮  显示
        searchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query_text) {
                searchview.clearFocus();//开始搜索后清除搜索框焦点，软键盘消失，光标不再闪烁
//                Log.w("DisplayActivity", "开始处理搜素");
//                Log.w("DisplayActivity", "query是" + query_text);
                //使用  暴力匹配算法（Brute Force Algorithm）
                double matching_degree = 0;//匹配值
                double percent_matching_degree;//百分比匹配度
                //将输入的字符串转换为字符数组
                char[] input_mes = query_text.toCharArray();
                int in_mes_l = input_mes.length;//获得这个字符数组的长度

                for (int i = 0; i < song_number; i++) {//遍历整个列表  匹配歌曲名
//                    Log.w("DisplayActivity", "第" + i + "次遍历");
                    //获取列表中当前歌曲名
                    String current_song_name = songsList.get(i).getSong_name();
                    //当前歌曲名转换为字符数组
                    char[] char_current_song_name = current_song_name.toCharArray();
                    //当前歌曲名转换为的字符数组的长度
                    int cur_name_l = char_current_song_name.length;
//                    Log.w("DisplayActivity", "n的值为"+in_mes_l+"    m的值为"+cur_name_l);
                    if (in_mes_l <= cur_name_l) {//如果目标歌名比当前歌名短或者长度相等
                        int flag_current_mes = 0;
                        int flag_input_mes = 0;
                        //循环比较每一个字符
                        while (true) {
                            //有一个字符相同
                            if (char_current_song_name[flag_current_mes] == input_mes[flag_input_mes]) {
                                flag_current_mes++;//两个数组同时向后移动
                                flag_input_mes++;

                                matching_degree++;//匹配值加一
                            } else {//如果不匹配就只是当前歌名字符向后移动一个
                                flag_current_mes++;
                            }
                            //两个数组任意一个的标志到达末尾后结束本次比较
                            if (flag_input_mes == in_mes_l || flag_current_mes == cur_name_l) {
                                break;
                            }
                        }
                        //匹配百分比计算规则
                        percent_matching_degree = (matching_degree / in_mes_l +
                                matching_degree / cur_name_l) / 2;//相同的字符占整个字符串的平均比例
//                        Log.w("DisplayActivity", "匹配度是"+matching_degree);
//                        Log.w("DisplayActivity", "匹配百分比是"+percent_matching_degree);
                        //找到百分之百匹配的了，直接退出，不找了
                        if (percent_matching_degree == 1) {
                            search_list.add(songsList.get(i));
//                            Log.w("DisplayActivity", "找到百分之百匹配的了，不找了");
                            break;
                        } else if (percent_matching_degree > 0.2) {//阈值为0.2
                            Log.w("DisplayActivity", "百分比匹配度大于阈值");
                            search_list.add(songsList.get(i));
                        }
//                        matching_degree=0;
//                        percent_matching_degree=0;
                    }
                }//遍历匹配循环到此结束
                if (search_list.isEmpty()) {//如果搜索结果为空，提示
                    Toast.makeText(DisplayActivity.this, "搜索结果为空",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DisplayActivity.this, "搜索完毕，显示结果",
                            Toast.LENGTH_SHORT).show();
                    //用search_list配置歌曲信息，加载进入控件
                    SongAdapter adapter_search = new SongAdapter(DisplayActivity.this,
                            R.layout.song_item, search_list);
                    ListView listView_search = findViewById(R.id.list_search);
                    listView_search.setAdapter(adapter_search);
                    //设置search_list歌曲item点击事件   以便可以点击搜素结果 播放歌曲
                    listView_search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view,
                                                int position, long id) {
                            current_music = search_list.get(position).getSong_list_id();
                            if (status == MusicService.STATUS_STOPPED || status == MusicService.STATUS_PLAYING) {
                                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                            } else if (status == MusicService.STATUS_PAUSED) {
                                sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                            }

                        }
                    });
                    search_LinearLayout = findViewById(R.id.search_LinearLayout);
                    search_LinearLayout.setVisibility(View.VISIBLE);//显示搜素结果
                    ImageView close_search = findViewById(R.id.image_close_search);//x 按钮 关闭搜索结果
                    close_search.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            search_LinearLayout.setVisibility(View.GONE);
                            search_list.clear();//清除搜索结果
                        }
                    });
                }
                return true;
            }

            //搜索框内部改变回调，newText就是搜索框里的内容 不知道用处，反正不写就报错
            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
    }

    public void initDealPlayBarBottom() {//底部一整栏的点击事件  待实现歌曲详情页
        View v = findViewById(R.id.play_bar_bottom);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clear_searchView_focus();
                Toast.makeText(DisplayActivity.this, "你点击了底部栏！",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    //绑定广播接收器
    private void bindStatusChangedReceiver() {
        receiver = new StatusChangedReceiver();
        IntentFilter intentFilter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver, intentFilter);
    }

    /*发送命令，控制音乐播放，参数定义在MusicService中*/
    private void sendBroadcastOnCommand(int command) {
        clear_searchView_focus();//清除搜索框的焦点
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", command);
        //根据不同的命令封装不同的数据
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number", current_music);//封装歌曲在list中的位置
                break;
            case MusicService.COMMAND_PREVIOUS:
                //待实现
                break;
            case MusicService.COMMAND_NEXT:
                //待实现
                break;
            case MusicService.COMMAND_PAUSE:
                //待实现
                break;
            case MusicService.COMMAND_STOP:
                //待实现
                break;
            case MusicService.COMMAND_RESUME:
                intent.putExtra("number", current_music);
                break;
            default:
                break;
        }
        sendBroadcast(intent);
//        Log.w("DisplatActivity", "发送了命令广播" + command);
    }

    /*内部类，接受广播命令并执行操作*/
    class StatusChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            clear_searchView_focus();
            //获取播放器状态
            status = intent.getIntExtra("status", -1);
            switch (status) {
                case MusicService.STATUS_PLAYING:
                    Log.w("DisplayActivity", "播放状态，改变播放图标.");
                    image_button_play = findViewById(R.id.button_play);
                    image_button_play.setBackground(getDrawable(R.drawable.pause));//把底部播放按钮的图标改变
                    current_music = MusicService.getCurrent_number();
                    initBottomMes(current_music);
                    image_music = findViewById(R.id.image_music);
                    image_music.setImageDrawable(getImage(songsList.get(
                            current_music).getAlbum_id()));
//                    initImagePlay(1,MusicService.getCurrent_number());
                    break;
                case MusicService.STATUS_PAUSED:
                    Log.w("DisplayActivity", "暂停状态，将改变播放图标.");
                    image_button_play = findViewById(R.id.button_play);
                    image_button_play.setBackground(getDrawable(R.drawable.play_2));//把底部播放按钮的图标改变
                    break;
                case MusicService.STATUS_STOPPED:
                    Log.w("DisplayActivity", "停止状态");
                    break;
                case MusicService.STATUS_COMPLETED:
                    Log.w("DisplayActivity", "已经播放完毕，播放下一首！");
                    break;
                case MusicService.PLAY_MODE_LOOP://单曲循环模式
                    playMode = 1;
                    break;
                case MusicService.PLAY_MODE_RANDOM://单曲循环模式
                    playMode = 2;
                    break;
                default:
                    break;
            }
        }
    }

    /**************一些工具方法类****************/
    public void clear_searchView_focus() {//清除搜索框的焦点
        if (searchview != null) {
            searchview.clearFocus();
        }
    }

    public void initBottomMes(int position) {//设置底部的一栏左侧的歌曲名和歌手
        Song song = songsList.get(position);//获取点击位置的song对象
        TextView songName = findViewById(R.id.buttom_textview_songname);
        songName.setText(song.getSong_name());
        TextView songAuthor = findViewById(R.id.buttom_textview_songauthor);
        songAuthor.setText(song.getSong_author());
    }
    //为完全实现，有错误
//    public void softKeyboardRaise(){//解决软键盘弹起时，底部控件被顶上去的问题
//        View rootView = findViewById(R.id.root_view_display);
//        rootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View v,int left, int top, int right, int bottom,
//                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                if (bottom - oldBottom < -1) {
//                    Log.w("DisplayActivity","软键盘弹上去了,动态设置高度为1");
//                    Log.w("DisplayActivity","bottom"+bottom);
//                    Log.w("DisplayActivity","oldBottom"+oldBottom);
//                    View play_bar_bottom = findViewById(R.id.play_bar_bottom);
//                    //取控件当前的布局参数
//                    RelativeLayout.LayoutParams params =
//                            (RelativeLayout.LayoutParams) play_bar_bottom.getLayoutParams();
//                    //设置高度值
//                    params.height = 1;
//                } else if (bottom - oldBottom > 1) {
//                    Log.w("DisplayActivity","bottom"+bottom);
//                    Log.w("DisplayActivity","oldBottom"+oldBottom);
//                    Log.w("DisplayActivity","软键盘弹下去了，动态设置高度，恢复原先控件高度");
//                    View play_bar_bottom = findViewById(R.id.play_bar_bottom);
//                    //取控件当前的布局参数
//                    RelativeLayout.LayoutParams params =
//                            (RelativeLayout.LayoutParams) play_bar_bottom.getLayoutParams();
//                    //设置高度值
//                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//                     } } });
//    }

    /**********获取  设置歌曲专辑图片*************/
    @SuppressWarnings("deprecation")
    private BitmapDrawable getImage(long albumId) {
        BitmapDrawable bmpDraw;
        String albumArt = getAlbumArt(albumId);
        Bitmap bm;
        if (albumArt != null) {
//            Log.w("DisplayActivity","albumArt不为空");
            bm = BitmapFactory.decodeFile(albumArt);
            bmpDraw = new BitmapDrawable(bm);
            int width = bmpDraw.getIntrinsicWidth();
            int height = bmpDraw.getIntrinsicHeight();
            // drawable转换成bitmap
            Bitmap oldbmp = bmpDraw.getBitmap();
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) 400 / width);
            float sy = ((float) 400 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, width, height,
                    matrix, false);
            return new BitmapDrawable(newbmp);
        } else {
//            Log.w("DisplayActivity","albumArt为空");
            Resources res = getResources();
            Bitmap default_d = BitmapFactory.decodeResource(res,
                    R.drawable.music1);
            int width = default_d.getWidth();
            int height = default_d.getHeight();
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) 400 / width);
            float sy = ((float) 400 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            Bitmap newbmp = Bitmap.createBitmap(default_d, 0, 0, width, height,
                    matrix, false);
            return new BitmapDrawable(newbmp);
        }
    }

    private String getAlbumArt(long album_id) {
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[]{"album_art"};
        Cursor cur = this.getContentResolver().query(Uri.parse(mUriAlbums + "/" +
                        Long.toString(album_id)), projection, null, null,
                null);
        String album_art = null;
        if (cur != null) {
            if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
                cur.moveToNext();
                album_art = cur.getString(0);
            }
            cur.close();
        }
        return album_art;
    }
    //获取  设置歌曲专辑图片           到此结束

    /***********添加顶部右侧 menu***********/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //menu的事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pauseplay_item://延时停止播放
                timePausePlay();
                break;
            case R.id.mode_play://播放模式
                selectMode();
                break;
            case R.id.exit://退出播放器
                ActivityCollector.finishAll();
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
                break;
            default:
                break;
        }
        return true;
    }

    public void timePausePlay() {//定时停止播放
        final AlertDialog.Builder customizeDialog =
                new AlertDialog.Builder(DisplayActivity.this);
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(DisplayActivity.this)
                .inflate(R.layout.dialog, null);
        customizeDialog.setView(dialogView);
        dialog = customizeDialog.show();
        Button b_ok = dialogView.findViewById(R.id.b_time_ok);
        Button b_cancel = dialogView.findViewById(R.id.b_time_cancel);
        b_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sleepTimer = new Timer();
                TextView tv = dialogView.findViewById(R.id.input_time);
                input_time = Integer.parseInt(tv.getText().toString());//获取输入的时间
                //启动任务
                Toast.makeText(DisplayActivity.this, "歌曲将在" + input_time + "分钟后停止播放",
                        Toast.LENGTH_SHORT).show();
                sleepTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                    }
                }, input_time * 60 * 1000);//delay参数的单位是毫秒
                //关闭对话框
                dialog.dismiss();
            }
        });
        b_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sleepTimer != null) {
                    sleepTimer.cancel();
                }
                dialog.dismiss();
                Toast.makeText(DisplayActivity.this, "任务已经取消",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void selectMode() {//选择播放模式
        final AlertDialog.Builder builder = new AlertDialog.Builder(DisplayActivity.this);
        builder.setIcon(R.drawable.play_1);
        builder.setTitle("播放模式");
        final String[] mode = {"顺序播放(默认)", "单曲循环", "随机播放"};
        /*设置一个单项选择框
         * 第一个参数指定要显示的一组下拉单选框的数据集合
         * 第二个参数代表索引，指定默认哪一个单选框被勾选上，0表示默认'顺序播放' 会被勾选上
         * 第三个参数给每一个单选项绑定一个监听器
         */
        final Intent intent_mode = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        builder.setSingleChoiceItems(mode, playMode, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(DisplayActivity.this,
                        "播放模式为：" + mode[which], Toast.LENGTH_SHORT).show();
                if (which == 0) {
                    intent_mode.putExtra("command", MusicService.PLAY_MODE_ORDER);
                } else if (which == 1) {
                    intent_mode.putExtra("command", MusicService.PLAY_MODE_LOOP);
                } else if (which == 2) {
                    intent_mode.putExtra("command", MusicService.PLAY_MODE_RANDOM);
                }
                sendBroadcast(intent_mode);
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    /****************一些重写的系统方法************/
    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
        headSet_flag = 1;
    }

    /*在onDestroy()方法中通过调用unregisterReceiver()方法来取消耳机广播接收器的注册*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (headsetReceiver != null)
            unregisterReceiver(headsetReceiver);
        unregisterReceiver(receiver);
        if (status == MusicService.STATUS_STOPPED) {
            stopService(new Intent(this, MusicService.class));
            if (sleepTimer != null) {
                sleepTimer.cancel();//撤销定时器防止崩溃
            }
        }
    }

    @Override
    public void onBackPressed() {
        ActivityCollector.finishAll();
    }//回退键   不返回登录界面

    public static List<Song> getSongsList() {
        return songsList;
    }

    public static int getSong_number() {
        return song_number;
    }
}