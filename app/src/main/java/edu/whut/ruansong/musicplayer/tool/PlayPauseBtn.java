package edu.whut.ruansong.musicplayer.tool;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import edu.whut.ruansong.musicplayer.R;

public class PlayPauseBtn extends LinearLayout {
    //命名空间
    private static String NAMESPACE = "http://schemas.android.com/apk/res/edu.whut.ruansong.musicplayer.tool";
    private int playImage = 0;
    private int pauseImage = 0;
    private boolean isPlay = true;

    /**
     * 构造函数1
     * @param context
     * 组件没有属性没有样式时
     */
    public PlayPauseBtn(Context context) {
        super(context);
        initView();
    }

    /**
     * 构造函数2
     * @param context
     * 该组件在声明时调用了样式
     */
    public PlayPauseBtn(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * 构造函数2
     * @param context
     * 该组件在声明时只有属性
     */
    public PlayPauseBtn(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 获取图片的Int值
        playImage = attrs.getAttributeResourceValue(NAMESPACE, "play", R.drawable.play_5_gray);
        pauseImage = attrs.getAttributeResourceValue(NAMESPACE, "pause", R.drawable.pause_5_gray);
        initView();
    }

    public boolean isPlay(){
        return isPlay;
    }
    /**
     * 初始化函数
     */
    private void initView(){
        View.inflate(getContext(), R.layout.play_pause_btn, this);
        this.setClickable(true);
        if(pauseImage == 0){
            return ;
        }else{
            setIsStart(isPlay);
        }
    }
    /**
     * 通过传进一个boolean值，设置按钮的状态
     * @param isStart 设置开始或者暂停的状态
     */
    public void setIsStart(boolean isStart){
        this.isPlay = isStart;
        if(isStart){
            this.setBackgroundResource(playImage);
        }
        else{
            this.setBackgroundResource(pauseImage);
        }
    }
}
