package edu.whut.ruansong.musicplayer.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.BaseActivity;
import edu.whut.ruansong.musicplayer.tool.PictureDealHelper;

public class ShareActivity extends BaseActivity implements View.OnClickListener {
    private final int REQ_WRITE_EXTERNAL_STORAGE = 2;//权限请求码,1代表读取外部存储权限,2代表写存储
    private boolean flag_write_storage = false;
    @Override
    protected void onCreate(Bundle savedInstanceStated){
        super.onCreate(savedInstanceStated);
        setContentView(R.layout.activity_share);
        //关闭分享
        ImageView close = findViewById(R.id.close_share);
        close.setOnClickListener(this);
        //初始化UI
        Intent my_intent = getIntent();
        String dataPath = my_intent.getStringExtra("dataPath");
        String title = my_intent.getStringExtra("title");
        String artist = my_intent.getStringExtra("artist");
        ImageView album_icon = findViewById(R.id.share_album_icon);//专辑图片
        album_icon.setImageBitmap(PictureDealHelper.getAlbumPicture(this,dataPath,450,450));
        TextView title_view = findViewById(R.id.share_title),artist_view = findViewById(R.id.share_artist);//歌曲名字和歌手
        title_view.setText(title);
        artist_view.setText(artist);
        //保存按钮
        LinearLayout save_view = findViewById(R.id.save_share_activity);
        save_view.setOnClickListener(this);
        //请求写存储权限
        requestPermissionByHand();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.close_share:
                Intent intent = new Intent(ShareActivity.this,SongDetailActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.save_share_activity:
                if(flag_write_storage){
                    saveSharePicture();
                }else{
                    Toast.makeText(ShareActivity.this,"请手动打开写存储权限",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    /**
     * 生成海报保存到本地用于分享*/
    public void saveSharePicture(){
        //获取对应bitmap
        LinearLayout main_share_picture = findViewById(R.id.main_share_picture);
        Bitmap bitmap = Bitmap.createBitmap(main_share_picture.getWidth(), main_share_picture.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        main_share_picture.draw(canvas);
        //保存到本地
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        String picture_name = format.format(date)+".png";
        File share_picture_path = new File(Environment.getExternalStorageDirectory().getPath()+"/MungMusic/share_picture/");
        if (!share_picture_path.exists()) {//这个目录不存在则创建
            boolean flag = share_picture_path.mkdirs();
            if(!flag){//创建失败
                Toast.makeText(ShareActivity.this,"请授予写入本地权限",Toast.LENGTH_SHORT).show();
                return;
            }
        }
        File picture_path = new File(Environment.getExternalStorageDirectory().getPath()+"/MungMusic/share_picture",picture_name);
        if(picture_path.exists()){
            Toast.makeText(ShareActivity.this,"本地已有海报,快去分享吧",Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            FileOutputStream out = new FileOutputStream(picture_path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            Toast.makeText(ShareActivity.this,"海报已经保存到"+picture_path+",快去分享吧",Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 通知图库更新
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + picture_path)));
    }
    /**
     * 向用户请求权限
     */
    public void requestPermissionByHand() {
        //判断当前系统的版本
        if (Build.VERSION.SDK_INT >= 23) {
            Log.w("ShareActivity","检查写存储权限");
            int checkWriteStoragePermission = ContextCompat.checkSelfPermission(
                    ShareActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            //如果写入没有被授予
            if (checkWriteStoragePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        ShareActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQ_WRITE_EXTERNAL_STORAGE);
                Log.w("ShareActivity","写存储权限,正在请求");
            }else{//已有权限
                flag_write_storage = true;
            }
        }
    }
    /**
     * 向用户请求权限后的回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, final String[] permissions, int[] grantResults) {
        if (requestCode == REQ_WRITE_EXTERNAL_STORAGE) {
            // 如果请求被取消了，那么结果数组就是空的
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                flag_write_storage = true;
                Toast.makeText(ShareActivity.this, "写存储权限申请成功", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(ShareActivity.this, "写存储权限申请失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
