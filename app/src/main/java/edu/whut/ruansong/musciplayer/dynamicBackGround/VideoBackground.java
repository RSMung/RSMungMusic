package edu.whut.ruansong.musciplayer.dynamicBackGround;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.VideoView;

/**
 * Created by 阮阮 on 2018/11/17.
 */

public class VideoBackground extends VideoView {
    public VideoBackground(Context context) {
        super(context);
    }

    public VideoBackground(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoBackground(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    //重写onMeasure方法，为了更好的自适应全屏幕
    //作用是返回一个默认的值，如果MeasureSpec没有强制限制的话则使用提供的大小.
    // 否则在允许范围内可任意指定大小
    //第一个参数size为提供的默认大小，第二个参数为测量的大小

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //我们重新计算高度
        int width = getDefaultSize(0, widthMeasureSpec);
        int height = getDefaultSize(0, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
