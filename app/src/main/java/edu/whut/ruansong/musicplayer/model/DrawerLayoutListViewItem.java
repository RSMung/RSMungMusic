package edu.whut.ruansong.musicplayer.model;

/**侧滑栏ListView的item*/
public class DrawerLayoutListViewItem {
    private int item_picture;//item图片
    private String item_title;//item标题

    public DrawerLayoutListViewItem(int item_picture, String item_title) {
        this.item_picture = item_picture;
        this.item_title = item_title;
    }

    public int getItem_picture() {
        return item_picture;
    }

    public void setItem_picture(int item_picture) {
        this.item_picture = item_picture;
    }

    public String getItem_title() {
        return item_title;
    }

    public void setItem_title(String item_title) {
        this.item_title = item_title;
    }
}
