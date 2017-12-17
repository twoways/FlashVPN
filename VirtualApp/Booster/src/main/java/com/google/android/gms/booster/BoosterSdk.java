package com.google.android.gms.booster;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.gms.booster.util.AndroidUtil;


public class BoosterSdk {

    public static final boolean DEBUG = true;

    static final String PREF_NAME = "booster_config";
    static final String BOOSTER_NAME = "Do Booster";

    static final String PREF_KEY_BOOST_SHORTCUT_CREATED = "boost_shortcut_created";
    static final String PREF_KEY_BOOST_SHORTCUT_CREATE_COUNT = "boost_shortcut_create_count";
    static final String PREF_KEY_LAST_BOOST_SHORTCUT_REMOVE_TIME = "last_boost_shortcut_remove_time";
    static final String PREF_KEY_LAST_BOOST_SHORTCUT_CREATE_OR_UPDATE_TIME = "last_boost_shortcut_create_or_update_time";

    public static final String EXTRA_SHORTCUT_CLICK_FROM = "FROM";

    public interface IEventReporter {
        void reportEvent(String s, Bundle b);
    }
    public static class BoosterConfig {
        public String boostAdSlot = "slot_boost_ad";
        public String installAdSlot = "slot_install_ad";
        public String unlockAdSlot = "slot_unlock_ad";
        public boolean isAutoClean = false;
        public boolean isAutoCreateShortcut = true;
        public boolean isInstallAd = false;
        public boolean isUnlockAd = false;
        public boolean isPreloadOnUnlock = false;
        public boolean allowPreloadAdTimer = false;
        public long preloadAdTimerInterval = 45*60*1000;
        public int installAdLimit = 1;
        public int autoDismissTime = 30*1000;
        public long autoCreateInterval = 3*24*60*60*1000;
        public long autoCleanInterval = 5*60*60*1000;
        public long unlockAdFirstInterval = 3*24*60*60*1000;
        public long unlockAdInterval = 8*60*60*1000;
        public int memoryThreshold = 70;
    }

    //all res id
    public static class BoosterRes {
        public int titleString;
        public int boosterShorcutIcon;
        public int innerWheelImage;
        public int outterWheelImage;
    }


    static public BoosterConfig boosterConfig;
    static public BoosterRes boosterRes;

    static Context sContext;


    public static void init(Context context, BoosterConfig config, BoosterRes res, IEventReporter reporter) {
        BoosterLog.sReporter = reporter;
        sContext = context.getApplicationContext();
        // start init
        boosterConfig = config;
        boosterRes = res;
        Booster.startInit(sContext);
    }

    public static void useRealUserPresent(boolean useRealUserPresent) {
       // Booster.startUpdateConfigUseRealUserPresent(sContext, config);
    }

    public static boolean isAutoCleanEnabled() {
        return  boosterConfig.isAutoClean;
    }

    public static void setAutoCleanEnabled(boolean autoCleanEnabled) {
        boosterConfig.isAutoClean = true;
    }

    public static int getMemoryThreshold() {
        return boosterConfig.memoryThreshold;
    }

    public static void setMemoryThreshold(int memoryThreshold) {
        boosterConfig.memoryThreshold = memoryThreshold;
    }

    public static void startClean(Context context, String from) {
        Booster.startCleanShortcutClick(context.getApplicationContext(), from);
    }

    public static void showSettings(Context context) {
//        Intent intent = new Intent(context, BoosterSettingActivity.class);
//        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);
    }

    public static void checkCreateCleanShortcut() {
        if (!BoosterSdk.boosterConfig.isAutoCreateShortcut) {
            return;
        }

        long current = System.currentTimeMillis();

        SharedPreferences sp = sContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);;
        boolean cleanShortcutCreated = sp.getBoolean(PREF_KEY_BOOST_SHORTCUT_CREATED, false);
        long cleanShortcutCreateCount = sp.getLong(PREF_KEY_BOOST_SHORTCUT_CREATE_COUNT, 0L);
        long lastCleanShortcutRemoveTime = sp.getLong(PREF_KEY_LAST_BOOST_SHORTCUT_REMOVE_TIME, 0L);
        long lastCleanShortcutCreateOrUpdateTime = sp.getLong(PREF_KEY_LAST_BOOST_SHORTCUT_CREATE_OR_UPDATE_TIME, AndroidUtil.getFirstInstallTime(sContext));

        SharedPreferences.Editor ed = sp.edit();
        boolean dirty = false;

        do {
            boolean maybeRemovedDetected = cleanShortcutCreated && !AndroidUtil.hasShortcut(sContext,
                    sContext.getResources().getString(BoosterSdk.boosterRes.titleString));
            if (maybeRemovedDetected) {
                ed.putBoolean(PREF_KEY_BOOST_SHORTCUT_CREATED, false);
                ed.putLong(PREF_KEY_LAST_BOOST_SHORTCUT_REMOVE_TIME, current);
                dirty = true;
                break;
            }

            long cleanShortcutAutoCreateInterval = BoosterSdk.boosterConfig.autoCreateInterval;
            boolean needCreate = !cleanShortcutCreated && (current - lastCleanShortcutRemoveTime) > cleanShortcutAutoCreateInterval;
            long cleanShortcutUpdateInterval = BoosterSdk.boosterConfig.autoCreateInterval;
            boolean needUpdate = cleanShortcutCreated && (current - lastCleanShortcutCreateOrUpdateTime) > cleanShortcutUpdateInterval;
            if (!needCreate && !needUpdate)
                break;

            if (needUpdate)
                AndroidUtil.delShortcut(sContext, BoosterShortcutActivity.class, sContext.getResources().getString(BoosterSdk.boosterRes.titleString));
            Intent shortcutIntent = new Intent(sContext, BoosterShortcutActivity.class);
            shortcutIntent.setAction(BoosterShortcutActivity.class.getName());
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            AndroidUtil.addShortcut(sContext, shortcutIntent, BoosterSdk.boosterRes.boosterShorcutIcon, sContext.getResources().getString(BoosterSdk.boosterRes.titleString));

            ed.putBoolean(PREF_KEY_BOOST_SHORTCUT_CREATED, true);
            if (needCreate) {
                ed.putLong(PREF_KEY_BOOST_SHORTCUT_CREATE_COUNT, cleanShortcutCreateCount + 1);
                ed.putLong(PREF_KEY_LAST_BOOST_SHORTCUT_CREATE_OR_UPDATE_TIME, current);
            }
            if (needUpdate) {
                ed.putLong(PREF_KEY_LAST_BOOST_SHORTCUT_CREATE_OR_UPDATE_TIME, current);
            }
            dirty = true;

        } while (false);

        if (dirty) {
            ed.apply();
        }
    }
}
