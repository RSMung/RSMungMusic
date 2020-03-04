package edu.whut.ruansong.musicplayer.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
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

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.BaseActivity;

public class ShareActivity extends BaseActivity implements View.OnClickListener {
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
        album_icon.setImageBitmap(getAlbumPicture(dataPath,450,450));
        TextView title_view = findViewById(R.id.share_title),artist_view = findViewById(R.id.share_artist);//歌曲名字和歌手
        title_view.setText(title);
        artist_view.setText(artist);
        //保存按钮
        LinearLayout save_view = findViewById(R.id.save_share_activity);
        save_view.setOnClickListener(this);
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
                saveSharePicture();
                break;
        }
    }
    /**********获取歌曲专辑图片*************/
    public Bitmap getAlbumPicture(String dataPath, int scale_length, int scale_width) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(dataPath);
        byte[] data = mmr.getEmbeddedPicture();
        Bitmap albumPicture;
        if (data != null) {
            //获取bitmap对象
            albumPicture = BitmapFactory.decodeByteArray(data, 0, data.length);
            //获取宽高
            int width = albumPicture.getWidth();
            int height = albumPicture.getHeight();
            //Log.w("DisplayActivity","width = "+width+" height = "+height);
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) scale_length / width);
            float sy = ((float) scale_width / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
            return albumPicture;
        } else {
            albumPicture = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_icon);
            int width = albumPicture.getWidth();
            int height = albumPicture.getHeight();
            //Log.w("DisplayActivity", "width = " + width + " height = " + height);
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) scale_length / width);
            float sy = ((float) scale_width / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
            return albumPicture;
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
        String picture_name = getIntent().getStringExtra("title") + "-"+getIntent().getStringExtra("artist")+".png";
        File share_picture_path = new File(Environment.getExternalStorageDirectory().getPath()+"/MungMusic/share_picture");
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
            Toast.makeText(ShareActivity.this,"海报已经保存到本地,快去分享吧",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
