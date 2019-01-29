package com.polestar.superclone.reward;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.ArraySet;
import android.text.TextUtils;

import com.polestar.superclone.MApp;
import com.polestar.superclone.constant.AppConstants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

/**
 * Created by guojia on 2019/1/26.
 */

class TaskPreference {

    private static String PREFERENCE_NAME = "reward_task";

    private static SharedPreferences getSharedPreference(){
       return MApp.getApp().getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences getTaskSharedPreference(long taskId){
        return MApp.getApp().getSharedPreferences(PREFERENCE_NAME+"_" + taskId, Context.MODE_PRIVATE);
    }

    public static void updateLastUpdateTime() {
        getSharedPreference().edit().putLong("last_udpate_time", System.currentTimeMillis()).commit();
    }

    public static long getLastUpdateTime() {
        return  getSharedPreference().getLong("last_update_time", 0);
    }


    //Task related reward_task_xxxx

    private static int updateTaskFinishTime(long taskId, boolean update) {
        ArrayList<Long> list = getTaskFinishTime(taskId);
        Set<String> res = new ArraySet<>();
        if (list == null || list.size() == 0) {
            if (update) {
                res.add("" + System.currentTimeMillis());
            }
        } else {
            for (Long time: list) {
                if (isToday(time)){
                    res.add("" + time);
                }
            }
            if (update) {
                res.add("" + System.currentTimeMillis());
            }
        }
        SharedPreferences sp = getTaskSharedPreference(taskId);
        sp.edit().putStringSet("finish_times", res).commit();
        return res.size();
    }

    public static boolean isToday(long time) {
        boolean flag = false;
        //获取当前系统时间
        long longDate = System.currentTimeMillis();
        Date inputJudgeDate = new Date(time);
        Date nowDate = new Date(longDate);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = dateFormat.format(nowDate);
        String subDate = format.substring(0, 10);
        //定义每天的24h时间范围
        String beginTime = subDate + " 00:00:00";
        String endTime = subDate + " 23:59:59";
        Date paseBeginTime = null;
        Date paseEndTime = null;
        try {
            paseBeginTime = dateFormat.parse(beginTime);
            paseEndTime = dateFormat.parse(endTime);

        } catch (ParseException e) {
            //log.error(e.getMessage());
        }
        if(inputJudgeDate.after(paseBeginTime) && inputJudgeDate.before(paseEndTime)) {
            flag = true;
        }
        return flag;
    }

    private static ArrayList<Long> getTaskFinishTime(long taskId) {
        SharedPreferences sp = getTaskSharedPreference(taskId);
        Set<String> s = sp.getStringSet("finish_times", null);
        if (s!= null && s.size() >0) {
            ArrayList<Long> list = new ArrayList<>();
            for (String ss:s){
                try{
                    Long time = Long.valueOf(ss);
                    list.add(time);
                }catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
            return list;
        }
        return null;
    }

    public void incTaskFinishCount(long taskId) {
        SharedPreferences sp = getTaskSharedPreference(taskId);
        int cnt = sp.getInt("task_count" , 0) + 1;
        sp.edit().putInt("task_count", cnt);

        updateTaskFinishTime(taskId, true);
    }

    public int getTaskFinishCount(long taskId) {
        return  getTaskSharedPreference(taskId).getInt("task_count", 0);
    }

    public int getTaskFinishTodayCount(long taskId) {
        return updateTaskFinishTime(taskId, false);
    }
}