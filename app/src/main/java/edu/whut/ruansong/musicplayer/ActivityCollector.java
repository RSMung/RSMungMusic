package edu.whut.ruansong.musicplayer;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 阮 on 2018/11/17.
 * manage activities
 * add
 * remove
 * finishAll
 */

public class ActivityCollector {
    private static List<Activity> activities = new ArrayList<>();

    protected static void addActivity(Activity activity) {
        activities.add(activity);
    }

    protected static void removeActivity(Activity activity) {
        activities.remove(activity);
    }

    public static void finishAll() {
        for (Activity activity : activities) {
            if (!activity.isFinishing()) {
                activity.finish();
            }
        }
    }
}
