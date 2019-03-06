package winterfell.flash.vpn;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


import winterfell.flash.vpn.utils.MLogs;
import winterfell.flash.vpn.utils.PreferenceUtils;
import winterfell.flash.vpn.utils.RemoteConfig;

public class FlashUser {

    private Context mContext;
    private static FlashUser sInstance;
    private Handler mHandler;
    private long freePremiumTime;

    private final static int MSG_SAVE = 0;

    private final static String RC_USE_PREMIUM_SECONDS = "use_premium_seconds";
    private final static String RC_INIT_PREMIUM_SECONDS = "init_premium_seconds";

    private FlashUser(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_SAVE:
                        PreferenceUtils.putLong(mContext, "premium_seconds", freePremiumTime);
                        break;
                }
            }
        };
        freePremiumTime = PreferenceUtils.getLong(mContext, "premium_seconds", RemoteConfig.getLong(RC_INIT_PREMIUM_SECONDS));
    }

    synchronized public static FlashUser getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FlashUser(context);
        }
        return sInstance;
    }

    public long getFreePremiumSeconds() {
        MLogs.d("free premium: " + freePremiumTime);
        return freePremiumTime;
    }

    public void doRewardFreePremium() {
        if (usePremiumSeconds()) {
            long add = RemoteConfig.getLong("gift_reward_sec");
            freePremiumTime += add;
            MLogs.d("add reward: " + add);
            scheduleSave();
        }
    }

    public void costFreePremiumSec(long sec) {
        if (usePremiumSeconds()) {
            freePremiumTime -= sec;
            if (freePremiumTime < 0) freePremiumTime = 0;
            scheduleSave();
        }
    }

    private void scheduleSave() {
        if (! mHandler.hasMessages(MSG_SAVE)) {
            mHandler.sendEmptyMessage(MSG_SAVE);
        }
    }

    public boolean isVIP() {
        return PreferenceUtils.getBoolean(FlashApp.getApp(), "is_vip", false);
    }

    public boolean usePremiumSeconds() {
        return RemoteConfig.getBoolean(RC_USE_PREMIUM_SECONDS);
    }

    public void setVIP(boolean enable) {
        PreferenceUtils.putBoolean(FlashApp.getApp(), "is_vip", enable);
    }
}