package edu.whut.ruansong.musicplayer.myView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.util.AttributeSet;
import android.view.View;

import edu.whut.ruansong.musicplayer.R;

/**
 * Created by Kingfar on 2017/12/18.
 * 仿网易云音乐留声机（唱片机）View
 */

public class GramophoneView extends View {

    /**
     * 尺寸计算设计说明：
     * 1、唱片有两个主要尺寸：中间图片的半径、黑色圆环的宽度。
     *    黑色圆环的宽度 = 图片半径的一半。
     * 2、唱针分为“手臂”和“头”，手臂分两段，一段长的一段短的，头也是一段长的一段短的。
     *    唱针四个部分的尺寸求和 = 唱片中间图片的半径+黑色圆环的宽度
     *    唱针各部分长度 比例——长的手臂：短的手臂：长的头：短的头 = 8:4:2:1
     * 3、唱片黑色圆环顶部到唱针顶端的距离 = 唱针长的手臂的长度。
     */

    private int halfMeasureWidth;
    // 绘制唱片相关变量
    // 中间图片默认半径
    private static final int DEFAULT_PICTURE_RADIUS = 400;
    // 唱片旋转默认速度，其实是通过每次旋转叠加的角度来控制速度
    private static final float DEFAULT_DISK_ROTATE_SPEED = 0.3f;
    private int pictureRadius;    // 中间图片的半径
    private int ringWidth;        // 黑色圆环宽度
    private float diskRotateSpeed;// 唱片旋转速度
    private Paint discPaint;      // 唱片画笔
    private Path clipPath;        // 裁剪图片的路径
    private Bitmap bitmap;        // 图片
    private Rect srcRect;         // 图片被裁减范围
    private Rect dstRect;         // 图片被绘制范围

    // 绘制唱针相关变量
    private static final int PLAY_DEGREE = -15;  // 播放状态时唱针的旋转角度
    private static final int PAUSE_DEGREE = -45; // 暂停状态时唱针的旋转角度
    private int smallCircleRadius = 20;          // 唱针顶部小圆半径
    private int bigCircleRadius;    // 唱针顶部大圆半径
    private int longArmLength;      // 唱针手臂，较长那段的长度
    private int shortArmLength;     // 唱针手臂，较短那段的长度
    private int longHeadLength;     // 唱针的头，较长那段的长度
    private int shortHeadLength;    // 唱针的头，较短那段的长度
    private Paint needlePaint;      // 唱针画笔

    // 状态控制相关变量
    private boolean isPlaying;            // 是否处于播放状态
    private int needleDegreeCounter;      // 唱针旋转角度计数器
    private float diskDegreeCounter;      // 唱片旋转角度计数器

    public GramophoneView(Context context) {
        this(context, null);
    }

    public GramophoneView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // 读取xml文件属性
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GramophoneView);
        pictureRadius = (int)typedArray.getDimension(R.styleable.GramophoneView_picture_radius, DEFAULT_PICTURE_RADIUS);
        diskRotateSpeed = typedArray.getFloat(R.styleable.GramophoneView_disk_rotate_speed, DEFAULT_DISK_ROTATE_SPEED);
        Drawable drawable = typedArray.getDrawable(R.styleable.GramophoneView_src);
        if(drawable == null){
            bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.album_svg_32);
        } else{
            if(drawable instanceof BitmapDrawable)
                bitmap = ((BitmapDrawable)drawable).getBitmap();
            if(drawable instanceof VectorDrawable)
                bitmap = getBitmap((VectorDrawable) drawable);
        }
        typedArray.recycle();

        // 初始化唱片变量
        ringWidth = pictureRadius>>1;
        discPaint = new Paint();
        discPaint.setColor(Color.BLACK);
        discPaint.setStyle(Paint.Style.STROKE);
        discPaint.setStrokeWidth(ringWidth);
        srcRect = new Rect();
        dstRect = new Rect();
        setBitmapRect(srcRect, dstRect);
        clipPath = new Path();
        clipPath.addCircle(0, 0, pictureRadius, Path.Direction.CW);
        diskDegreeCounter = 0;

        // 初始化唱针变量
        bigCircleRadius = smallCircleRadius<<1;
        shortHeadLength = (pictureRadius + ringWidth)/15;
        longHeadLength = shortHeadLength<<1;
        shortArmLength = longHeadLength<<1;
        longArmLength = shortArmLength<<1;
        needlePaint = new Paint();
        needleDegreeCounter = PAUSE_DEGREE;
    }
    /*
    * 根据VectorDrawable对象得到Bitmap对象*/
    private Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    /**
     * 根据加载的图片资源尺寸和设置的唱片中间图片直径，
     * 为canvas.drawBitmap()方法设置源Rect和目标Rect，
     * 以宽度为例，假设图片资源宽度为width，唱片中间图片直径为diameter
     * 如果width <= diameter，则截取宽度为整张图片宽度。
     * 如果width > diameter，则截取宽度为图片资源横向中间长度为diameter的区域。
     * 高度的截取方法与宽度相同。
     * @param src 源矩形
     * @param dst 目标矩形
     */
    private void setBitmapRect(Rect src, Rect dst){
//        这种处理方式意义好像不大，暂时注释
//        int bitmapWidth = bitmap.getWidth();
//        int bitmapHeight = bitmap.getHeight();
//        // 唱片里的图片直径，也就是唱片里的图片的外接正方形边长
//        int diameter = pictureRadius<<1;
//        // 图片宽度小于唱片图片直径
//        if(bitmapWidth <= diameter){
//            src.left = 0;
//            src.right = bitmapWidth;
//        } else {
//            src.left = (bitmap.getWidth()-diameter)/2;
//            src.right = bitmap.getWidth()/2+diameter;
//        }
//        // 图片高度小于唱片图片直径
//        if(bitmapHeight <= diameter){
//            src.top = 0;
//            src.bottom = bitmapHeight;
//        } else {
//            src.top = (bitmap.getHeight()-diameter)/2;
//            src.bottom = bitmap.getHeight()/2+diameter;
//        }
        src.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        dst.set(-pictureRadius, -pictureRadius, pictureRadius, pictureRadius);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        /**
         * wrap_content属性下View的宽高设计：
         * 宽度：等于唱片直径，即图片半径+圆环宽度求和再乘以2。
         * 高度：等于唱片直径+唱针较长的手臂
         */
        int width = (pictureRadius+ringWidth)*2;
        int height = (pictureRadius+ringWidth)*2+longArmLength;
        int measuredWidth = resolveSize(width, widthMeasureSpec);
        int measuredHeight = resolveSize(height, heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        halfMeasureWidth = getMeasuredWidth()>>1;
        drawDisk(canvas);
        drawNeedle(canvas);
        if(needleDegreeCounter > PAUSE_DEGREE){
            invalidate();
        }
    }

    // 绘制唱片（胶片）
    private void drawDisk(Canvas canvas){
        diskDegreeCounter = diskDegreeCounter%360+diskRotateSpeed;
        drawDisk(canvas, diskDegreeCounter);
    }

    // 绘制旋转了制定角度的唱片
    private void drawDisk(Canvas canvas, float degree){
        // 绘制圆环，注意理解平移的圆心距离和圆环半径是怎么计算的
        canvas.save();
        canvas.translate(halfMeasureWidth, pictureRadius+ringWidth+longArmLength);
        canvas.rotate(degree);
        canvas.drawCircle(0, 0, pictureRadius+ringWidth/2, discPaint);
        // 绘制图片
        canvas.clipPath(clipPath);
        canvas.drawBitmap(bitmap, srcRect, dstRect, discPaint);
        canvas.restore();
    }

    // 绘制唱针
    private void drawNeedle(Canvas canvas){
        // 由于PLAY_DEGREE和PAUSE_DEGREE之间的差值是30，所以每次增/减值应当是30的约数即可
        if(isPlaying){
            if(needleDegreeCounter < PLAY_DEGREE){
                needleDegreeCounter+=3;
            }
        } else {
            if(needleDegreeCounter > PAUSE_DEGREE){
                needleDegreeCounter-=3;
            }
        }
        drawNeedle(canvas, needleDegreeCounter);
    }

    // 绘制旋转了指定角度的唱针
    private void drawNeedle(Canvas canvas, int degree){
        // 移动坐标到水平中点
        canvas.save();
        canvas.translate(halfMeasureWidth, 0);
        // 绘制唱针手臂
        needlePaint.setStrokeWidth(20);
        needlePaint.setColor(Color.parseColor("#C0C0C0"));
        // 绘制第一段臂
        canvas.rotate(degree);
        canvas.drawLine(0, 0, 0, longArmLength, needlePaint);
        // 绘制第二段臂
        canvas.translate(0, longArmLength);
        canvas.rotate(-30);
        canvas.drawLine(0, 0, 0, shortArmLength, needlePaint);
        // 绘制唱针头
        // 绘制第一段唱针头
        canvas.translate(0, shortArmLength);
        needlePaint.setStrokeWidth(40);
        canvas.drawLine(0, 0, 0, longHeadLength, needlePaint);
        // 绘制第二段唱针头
        canvas.translate(0, longHeadLength);
        needlePaint.setStrokeWidth(60);
        canvas.drawLine(0, 0, 0, shortHeadLength, needlePaint);
        canvas.restore();

        // 两个重叠的圆形
        canvas.save();
        canvas.translate(halfMeasureWidth, 0);
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setColor(Color.parseColor("#C0C0C0"));
        canvas.drawCircle(0, 0, bigCircleRadius, needlePaint);
        needlePaint.setColor(Color.parseColor("#8A8A8A"));
        canvas.drawCircle(0, 0, smallCircleRadius, needlePaint);
        canvas.restore();
    }

    /**
     * 设置是否处于播放状态
     * @param isPlaying true:播放，false:暂停
     */
    public void setPlaying(boolean isPlaying){
        this.isPlaying = isPlaying;
        invalidate();
    }

    /**
     * 获取播放状态
     * @return true:播放，false:暂停
     */
    public boolean getPlaying(){
        return isPlaying;
    }

    /**
     * 获取图片半径
     * @return 图片半径
     */
    public int getPictureRadius() {
        return pictureRadius;
    }

    /**
     * 设置图片半径
     * @param pictureRadius 图片半径
     */
    public void setPictureRadius(int pictureRadius) {
        this.pictureRadius = pictureRadius;
    }

    /**
     * 获取唱片旋转速度
     * @return 唱片旋转速度
     */
    public float getDiskRotateSpeed() {
        return diskRotateSpeed;
    }

    /**
     * 设置唱片旋转速度
     * @param diskRotateSpeed 旋转速度
     */
    public void setDiskRotateSpeed(float diskRotateSpeed) {
        this.diskRotateSpeed = diskRotateSpeed;
    }

    /**
     * 设置图片资源id
     * @param resId 图片资源id
     */
    public void setPictureRes(int resId){
        bitmap = BitmapFactory.decodeResource(getContext().getResources(), resId);
        setBitmapRect(srcRect, dstRect);
        invalidate();
    }
    public void setPictureRes(Bitmap myBitmap){
        bitmap = myBitmap;
        setBitmapRect(srcRect, dstRect);
        invalidate();
    }
}

