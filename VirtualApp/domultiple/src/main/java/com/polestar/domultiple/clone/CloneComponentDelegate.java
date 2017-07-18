package com.polestar.domultiple.clone;

/**
 * Created by guojia on 2017/7/16.
 */

import android.app.Activity;
import android.content.Intent;

import com.lody.virtual.client.hook.delegate.ComponentDelegate;
import com.polestar.domultiple.utils.MLogs;

import java.util.HashSet;
import java.util.List;

/**
 * Created by guojia on 2016/12/16.
 */

public class CloneComponentDelegate implements ComponentDelegate {

    private HashSet<String> pkgs = new HashSet<>();
    public void init() {
//        List<CloneModel> list = DbManager.queryAppList(MApp.getApp());
//        for(CloneModel app:list) {
//            if (app.isNotificationEnable()) {
//                pkgs.add(app.getPackageName());
//            }
//        }
    }

    @Override
    public void beforeActivityCreate(Activity activity) {

    }

    @Override
    public void beforeActivityResume(String pkg) {
        MLogs.d("beforeActivityResume " + pkg);
        //if (PreferencesUtils.isLockerEnabled(VirtualCore.get().getContext())) {
        //AppLockMonitor.getInstance().onActivityResume(pkg);
        //}
    }

    @Override
    public void beforeActivityPause(String pkg) {
        MLogs.d("beforeActivityPause " + pkg);
        // if (PreferencesUtils.isLockerEnabled(VirtualCore.get().getContext())) {
        //AppLockMonitor.getInstance().onActivityPause(pkg);
        // }
    }

    @Override
    public void beforeActivityDestroy(Activity activity) {

    }

    @Override
    public void onSendBroadcast(Intent intent) {

    }

    @Override
    public boolean isNotificationEnabled(String pkg) {
        MLogs.d("isNotificationEnabled pkg: " + pkg + " " + pkgs.contains(pkg) );
        return pkgs.contains(pkg);
    }

    @Override
    public void reloadLockerSetting(String newKey, boolean adFree, long interval) {
       // AppLockMonitor.getInstance().reloadSetting(newKey, adFree, interval);
    }
}