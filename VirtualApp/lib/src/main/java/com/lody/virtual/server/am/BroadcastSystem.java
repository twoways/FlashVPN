package com.lody.virtual.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.helper.collection.ArrayMap;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.PendingResultData;
import com.lody.virtual.server.pm.PackageSetting;
import com.lody.virtual.server.pm.VAppManagerService;
import com.lody.virtual.server.pm.parser.VPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static android.content.Intent.FLAG_RECEIVER_REGISTERED_ONLY;

/**
 * @author Lody
 */

public class BroadcastSystem {

    private static final String TAG = BroadcastSystem.class.getSimpleName();
    private static final Set<String> SYSTEM_BROADCAST_ACTION = new HashSet<>(7);
    private static final Set<String> SYSTEM_STICKY_BROADCAST_ACTION = new HashSet<>(4);
    /**
     * MUST < 10000.
     */
    private static final int BROADCAST_TIME_OUT = 8500;
    private static BroadcastSystem gDefault;

    static {
        SYSTEM_BROADCAST_ACTION.add("android.net.wifi.STATE_CHANGE");
        SYSTEM_BROADCAST_ACTION.add("android.net.wifi.WIFI_STATE_CHANGED");
        SYSTEM_BROADCAST_ACTION.add("android.net.conn.CONNECTIVITY_CHANGE");
        SYSTEM_BROADCAST_ACTION.add("android.intent.action.BATTERY_CHANGED");
        SYSTEM_BROADCAST_ACTION.add("android.intent.action.BATTERY_LOW");
        SYSTEM_BROADCAST_ACTION.add("android.intent.action.BATTERY_OKAY");
        SYSTEM_BROADCAST_ACTION.add("android.intent.action.ANY_DATA_STATE");

        SYSTEM_STICKY_BROADCAST_ACTION.add("android.net.conn.CONNECTIVITY_CHANGE");
        SYSTEM_STICKY_BROADCAST_ACTION.add("android.net.wifi.WIFI_STATE_CHANGED");
        SYSTEM_STICKY_BROADCAST_ACTION.add("android.intent.action.BATTERY_CHANGED");
        SYSTEM_STICKY_BROADCAST_ACTION.add("android.intent.action.ANY_DATA_STATE");
    }

    private final ArrayMap<String, SystemBroadcastReceiver> mSystemReceivers = new ArrayMap<>();
	private final ArrayMap<String, List<BroadcastReceiver>> mReceivers = new ArrayMap<>();
    private final Map<IBinder, BroadcastRecord> mBroadcastRecords = new HashMap<>();
	private final Context mContext;
	private final StaticScheduler mScheduler;
    private final TimeoutHandler mTimeoutHandler;
	private final VActivityManagerService mAMS;
	private final VAppManagerService mApp;

    private BroadcastSystem(Context context, VActivityManagerService ams, VAppManagerService app) {
		this.mContext = context;
		this.mApp = app;
		this.mAMS = ams;
		mScheduler = new StaticScheduler();
        mTimeoutHandler = new TimeoutHandler();
        //fuckHuaWeiVerifier();
        registerSystemReceiver();
    }

    public static void attach(VActivityManagerService ams, VAppManagerService app) {
        if (gDefault != null) {
            throw new IllegalStateException();
        }
        gDefault = new BroadcastSystem(VirtualCore.get().getContext(), ams, app);
    }

    public static BroadcastSystem get() {
        return gDefault;
    }

    Intent dispatchStickyBroadcast(int vuid, IntentFilter filter) {
        Iterator<String> iterator = filter.actionsIterator();
        while (iterator.hasNext()) {
            String action = iterator.next();
            SystemBroadcastReceiver receiver = mSystemReceivers.get(action);
            if (receiver != null && receiver.sticky && receiver.stickyIntent != null) {
                Intent intent = new Intent(receiver.stickyIntent);
                SpecialComponentList.protectIntent(intent);
                intent.putExtra("_VA_|_uid_", vuid);
                mContext.sendBroadcast(intent);
                if (!iterator.hasNext()) {
                    return receiver.stickyIntent;
                }
            }
        }
        return null;
    }

    private void registerSystemReceiver() {
		for (String action : SYSTEM_BROADCAST_ACTION) {
			SystemBroadcastReceiver receiver = new SystemBroadcastReceiver(false);
			mContext.registerReceiver(receiver, new IntentFilter(action));
			mSystemReceivers.put(action, receiver);
		}
		for (String action : SYSTEM_STICKY_BROADCAST_ACTION) {
			SystemBroadcastReceiver receiver = mSystemReceivers.get(action);
			if (receiver != null) {
				receiver.sticky = true;
			}
		}
	}

	public void startApp(VPackage p) {
        PackageSetting setting = (PackageSetting) p.mExtras;
		VLog.d("BroadcastSystem","startApp " + p.packageName);
		for (VPackage.ActivityComponent receiver : p.receivers) {
			ActivityInfo info = receiver.info;
			List<BroadcastReceiver> receivers = mReceivers.get(p.packageName);
			if (receivers == null) {
				receivers = new ArrayList<>();
				mReceivers.put(p.packageName, receivers);
			}
			String componentAction = String.format("_VA_%s_%s", info.packageName, info.name);
//			IntentFilter componentFilter = new IntentFilter(componentAction);
//			BroadcastReceiver r = new StaticBroadcastReceiver(setting.appId, info, componentFilter);
//			mContext.registerReceiver(r, componentFilter, null, mScheduler);
//			VLog.d("BroadcastSystem", "register " + componentFilter.getAction(0));
//			receivers.add(r);
            for (VPackage.ActivityIntentInfo ci : receiver.intents)  {
				IntentFilter cloneFilter = new IntentFilter(ci.filter);
				redirectFilterActions(cloneFilter);
				cloneFilter.addAction(componentAction);
				BroadcastReceiver r = new StaticBroadcastReceiver(setting.appId, info, cloneFilter);
				mContext.registerReceiver(r, cloneFilter, null, mScheduler);
				receivers.add(r);
			}
		}
	}

	private void redirectFilterActions(IntentFilter filter) {
		List<String> actions = mirror.android.content.IntentFilter.mActions.get(filter);
		ListIterator<String> iterator = actions.listIterator();
		while (iterator.hasNext()) {
			String action = iterator.next();
			if (SpecialComponentList.isActionInBlackList(action)) {
				iterator.remove();
				continue;
			}
			String protectedAction = SpecialComponentList.protectAction(action);
			if (protectedAction != null) {
				iterator.set(protectedAction);
			}
			VLog.d("BroadcastSystem", "register redirected action " + protectedAction);
		}
	}

	public void stopApp(String packageName) {
        synchronized (mBroadcastRecords) {
            Iterator<Map.Entry<IBinder, BroadcastRecord>> iterator = mBroadcastRecords.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<IBinder, BroadcastRecord> entry = iterator.next();
                BroadcastRecord record = entry.getValue();
                if (record.receiverInfo.packageName.equals(packageName)) {
                    record.pendingResult.finish();
                    iterator.remove();
                }
            }
        }
        synchronized (mReceivers) {
            List<BroadcastReceiver> receivers = mReceivers.get(packageName);
            if (receivers != null) {
                for (BroadcastReceiver r : receivers) {
                    mContext.unregisterReceiver(r);
                }
            }
            mReceivers.remove(packageName);
	    }
    }

    void broadcastFinish(PendingResultData res) {
        synchronized (mBroadcastRecords) {
            BroadcastRecord record = mBroadcastRecords.remove(res.mToken);
            if (record == null) {
                VLog.e(TAG, "Unable to find the BroadcastRecord by token: " + res.mToken);
            }
        }
        mTimeoutHandler.removeMessages(0, res.mToken);
        res.finish();
    }

    void broadcastSent(int vuid, ActivityInfo receiverInfo, PendingResultData res) {
        BroadcastRecord record = new BroadcastRecord(vuid, receiverInfo, res);
        synchronized (mBroadcastRecords) {
            mBroadcastRecords.put(res.mToken, record);
        }
        Message msg = new Message();
        msg.obj = res.mToken;
        mTimeoutHandler.sendMessageDelayed(msg, BROADCAST_TIME_OUT);
    }

	private static final class StaticScheduler extends Handler {

	}

    private static final class BroadcastRecord {
        int vuid;
        ActivityInfo receiverInfo;
        PendingResultData pendingResult;

        BroadcastRecord(int vuid, ActivityInfo receiverInfo, PendingResultData pendingResult) {
            this.vuid = vuid;
            this.receiverInfo = receiverInfo;
            this.pendingResult = pendingResult;
        }
    }

    private final class TimeoutHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            IBinder token = (IBinder) msg.obj;
            BroadcastRecord r = mBroadcastRecords.remove(token);
            if (r != null) {
                VLog.w(TAG, "Broadcast timeout, cancel to dispatch it.");
                r.pendingResult.finish();
            }
        }
    }

    private final class SystemBroadcastReceiver extends BroadcastReceiver {

        boolean sticky;
        Intent stickyIntent;

        public SystemBroadcastReceiver(boolean sticky) {
            this.sticky = sticky;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent protectedIntent = new Intent(intent);
            SpecialComponentList.protectIntent(protectedIntent);
            mContext.sendBroadcast(protectedIntent);
            if (sticky) {
                stickyIntent = intent;
            }
        }
    }

	private final class StaticBroadcastReceiver extends BroadcastReceiver {
		private int appId;
		private ActivityInfo info;
		@SuppressWarnings("unused")
		private IntentFilter filter;

		private StaticBroadcastReceiver(int appId, ActivityInfo info, IntentFilter filter) {
			this.appId = appId;
			this.info = info;
			this.filter = filter;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			VLog.logbug("StaticBroadcastReceiver", "E onReceive " + intent.toString());
			if (mApp.isBooting()) {
				return;
			}
            if ((intent.getFlags() & FLAG_RECEIVER_REGISTERED_ONLY) != 0 || isInitialStickyBroadcast()) {
                return;
            }
			PendingResult result = mirror.android.content.BroadcastReceiver.getPendingResult.call(this);
			synchronized (mAMS) {
                if (!mAMS.handleStaticBroadcast(appId, info, intent, new PendingResultData(result))) {
//                    result.finish();
//					if (mOrderedHint) {
//						am.finishReceiver(mToken, mResultCode, mResultData, mResultExtras,
//								mAbortBroadcast, mFlags);
//					} else {
//						// This broadcast was sent to a component; it is not ordered,
//						// but we still need to tell the activity manager we are done.
//						am.finishReceiver(mToken, 0, null, null, false, mFlags);
//					}
                }
            }
			VLog.d("StaticBroadcastReceiver", "X onReceive " + intent.toString());
		}
	}
}
