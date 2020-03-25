package edu.whut.ruansong.musicplayer.tool;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.VectorDrawable;
import android.media.MediaMetadataRetriever;

import edu.whut.ruansong.musicplayer.R;

public class PictureDealHelper {
    /**********获取歌曲专辑图片*************/
    public static Bitmap getAlbumPicture(Context context,String dataPath, int scale_length, int scale_width) {
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
            VectorDrawable album_svg_32 = (VectorDrawable) context.getResources().getDrawable(R.drawable.album_svg_32);
            Bitmap bitmap = Bitmap.createBitmap(album_svg_32.getIntrinsicWidth(), album_svg_32.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            album_svg_32.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            album_svg_32.draw(canvas);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) scale_length / width);
            float sy = ((float) scale_width / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
            return albumPicture;
        }
    }
}
