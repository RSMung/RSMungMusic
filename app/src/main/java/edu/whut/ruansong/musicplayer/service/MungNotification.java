package edu.whut.ruansong.musicplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.activity.DisplayActivity;
import edu.whut.ruansong.musicplayer.model.Song;

public class MungNotification {
    private static final int NOTIFY_ID = 0x0277;
    private static NotificationManager mNotificationManager;
    private int current_number = 0;
    private int current_status = MusicService.STATUS_STOPPED;
    private String channelId = "Mung音乐播放器";
    private String channelName = "Mung音乐播放器";

    /**
     * 构造函数
     */
    public MungNotification(MusicService context) {
        Log.w("MungNotification","进入MungNotification构造函数");
        mNotificationManager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
    }

    /**
     * 显示通知
     */
    public void notifyPlay(MusicService context) {
        Log.w("MungNotification","进入notifyPlay");
        Notification myNotification = buildNotification(context);
        if (myNotification != null) {
            context.startForeground(NOTIFY_ID, myNotification);
        }
    }

    /**
     * 停止通知了后又重新构建通知并发送
     */
    public void notifyPause(MusicService context) {
        Log.w("MungNotification","进入notifyPause");
        context.stopForeground(false);
        Notification myNotification = buildNotification(context);
        if (myNotification != null) {
            mNotificationManager.notify(NOTIFY_ID, myNotification);
        }
    }

    /**
     * 取消所有通知
     */
    public void stopNotify(MusicService context) {
        Log.w("MungNotification","进入stopNotify");
        context.stopForeground(true);
        mNotificationManager.cancelAll();
    }

    /**
     * 构建通知
     */
    private Notification buildNotification(MusicService context) {
        Log.w("MungNotification","进入buildNotification");
        Notification myNotification = null;
        current_number = MusicService.getCurrent_number();
        //跳转到display_activity事件
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, new Intent(context, DisplayActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
//        boolean isPlaying = MusicService.getCurrent_status() == MusicService.STATUS_PLAYING;
        Song current_song = context.getSong();
        Bitmap album_icon = getAlbumPicture(current_song.getDataPath(),context);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){//适配Android8及以上
            NotificationChannel myChannel = new NotificationChannel(channelId,channelName,NotificationManager.IMPORTANCE_DEFAULT);
            myChannel.setDescription("Mung音乐播放器");
            myChannel.enableLights(false);//呼吸灯
            myChannel.enableVibration(false);//震动
            myChannel.setVibrationPattern(new long[]{0});//1000,500,2000---->震动1s停止0.5s再震动2s
            myChannel.setSound(null, null);//声音
            mNotificationManager.createNotificationChannel(myChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.logo)
                .setLargeIcon(album_icon)
                .setContentTitle(current_song.getTitle())
                .setContentText(current_song.getArtist())
                .setShowWhen(false)//不显示时间
                .setOngoing(false)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)
                .setVibrate(new long[]{0})
                .setSound(null)
                .setChannelId(channelId);
        myNotification = builder.build();
        return myNotification;
    }

    /**********获取歌曲专辑图片*************/
    public Bitmap getAlbumPicture(String dataPath,MusicService musicService) {
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
            float sx = ((float) 120 / width);
            float sy = ((float) 120 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
            return albumPicture;
        } else {
            albumPicture = BitmapFactory.decodeResource(musicService.getResources(), R.drawable.default_album_icon);
            int width = albumPicture.getWidth();
            int height = albumPicture.getHeight();
            //Log.w("DisplayActivity", "width = " + width + " height = " + height);
            // 创建操作图片用的Matrix对象
            Matrix matrix = new Matrix();
            // 计算缩放比例
            float sx = ((float) 120 / width);
            float sy = ((float) 120 / height);
            // 设置缩放比例
            matrix.postScale(sx, sy);
            // 建立新的bitmap，其内容是对原bitmap的缩放后的图
            albumPicture = Bitmap.createBitmap(albumPicture, 0, 0, width, height, matrix, false);
            return albumPicture;
        }
    }

    /**
     * 构建PendingIntent
     */
//    private PendingIntent createBroadcast(MusicService context, int action) {
//        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
//        intent.putExtra("command", action);
//        current_number = MusicService.getCurrent_number();
//        intent.putExtra("number",current_number);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
//        return pendingIntent;
//    }
}
