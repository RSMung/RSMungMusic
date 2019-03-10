package edu.whut.ruansong.musciplayer;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by 阮 on 2018/11/17.
 */

public class SongAdapter extends ArrayAdapter<Song> {
    private int resourceId;//用来放置布局文件的id

    //适配器的构造函数
    public SongAdapter(Context context, int textViewResourceId, List<Song> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    //这个方法在每个子项被滚动到屏幕内的时候会被调用
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Song song = getItem(position); // 获取当前项的Song实例
        View view;
        ViewHolder viewHolder;//内部类
        if (convertView == null) {//如果是第一次加载
            view = LayoutInflater.from(getContext()).inflate(
                    resourceId, parent, false);//布局  对象化
            viewHolder = new ViewHolder();

            //把布局文件里面的三个对象加载出来
            viewHolder.songImage = view.findViewById(R.id.song_image);
            viewHolder.songName = view.findViewById (R.id.song_name);
            viewHolder.songAuthor=view.findViewById(R.id.song_author);
            view.setTag(viewHolder); // 将ViewHolder存储在View中
        } else {//不是第一次加载，即布局文件已经加载，可以利用
            view = convertView;
            viewHolder = (ViewHolder) view.getTag(); // 重新获取ViewHolder
        }
        //传入具体信息
        viewHolder.songImage.setImageResource(song.getSong_image_id());
        viewHolder.songName.setText(song.getSong_name());
        viewHolder.songAuthor.setText(song.getSong_author());

        //设置两个文本的字体style
        viewHolder.songName.setTypeface(Typeface.DEFAULT_BOLD);
        viewHolder.songAuthor.setTypeface(Typeface.DEFAULT_BOLD);

        return view;
    }

    class ViewHolder {

        ImageView songImage;

        TextView songName;

        TextView songAuthor;

    }
}
