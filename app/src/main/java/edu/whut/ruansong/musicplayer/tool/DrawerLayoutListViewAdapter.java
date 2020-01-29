package edu.whut.ruansong.musicplayer.tool;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import edu.whut.ruansong.musicplayer.R;
import edu.whut.ruansong.musicplayer.model.DrawerLayoutListViewItem;

public class DrawerLayoutListViewAdapter extends ArrayAdapter<DrawerLayoutListViewItem> {
    private int resourceId;//用来放置布局文件的id
    //适配器的构造函数
    public DrawerLayoutListViewAdapter(Context context, int textViewResourceId, List<DrawerLayoutListViewItem> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }
    class ViewHolder {

        ImageView itemImage;

        TextView itemTitle;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        DrawerLayoutListViewItem item = getItem(position); // 获取当前项的item实例
        View view;//子项布局对象
        ViewHolder viewHolder;//内部类对象
        if(convertView == null){//第一次加载
            view = LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.itemImage = view.findViewById(R.id.drawer_layout_list_item_image);
            viewHolder.itemTitle = view.findViewById(R.id.drawer_layout_list_item_title);
        }else{//不是第一次,即布局文件已经加载，可以利用
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        if(item != null && viewHolder!= null){
            viewHolder.itemImage.setImageResource(item.getItem_picture());
            viewHolder.itemTitle.setText(item.getItem_title());
        }
        return view;
    }
}
