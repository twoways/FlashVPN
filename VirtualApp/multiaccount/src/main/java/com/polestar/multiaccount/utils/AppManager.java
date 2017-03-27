package com.polestar.multiaccount.utils;

import android.content.Context;
import android.content.Intent;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.os.VUserHandle;
import com.polestar.multiaccount.constant.AppConstants;
import com.polestar.multiaccount.db.DbManager;
import com.polestar.multiaccount.model.AppModel;

import java.util.ArrayList;
import java.util.List;

public class AppManager {
    private final static String TAG = "AppManager";
    private static AppManager instance;

    public static List<AppModel> getClonedApp(Context context) {
        List<AppModel> appList = DbManager.queryAppList(context);
        List<AppModel> uninstalledApp = new ArrayList<>();
        for (AppModel model : appList) {
            if (!CommonUtils.isAppInstalled(context, model.getPackageName())) {
                uninstalledApp.add(model);
                continue;
            }
            if (! VirtualCore.get().isAppInstalled(model.getPackageName())){
                uninstalledApp.add(model);
                continue;
            }
            if (model.getCustomIcon() == null) {
                model.setCustomIcon(BitmapUtils.createCustomIcon(context, model.initDrawable(context)));
            }
            setAppNotificationFlag(model.getPackageName(), model.isNotificationEnable());
        }
        deleteApp(context, uninstalledApp);
        appList.removeAll(uninstalledApp);
        return appList;
    }

    public synchronized static void deleteApp(Context context, List<AppModel> uninstalledApp) {
        if (uninstalledApp != null && uninstalledApp.size() > 0) {
            for (AppModel model : uninstalledApp) {
                CommonUtils.removeShortCut(context, model);
                uninstallApp(model.getPackageName());
            }
            DbManager.deleteAppModeList(context, uninstalledApp);
            DbManager.notifyChanged();
        }
    }

    public static void launchApp(String packageName) {
        try {
            MLogs.d(TAG, "packageName = " + packageName);
            Intent intent = VirtualCore.get().getLaunchIntent(packageName, VUserHandle.myUserId());
            VActivityManager.get().startActivity(intent, VUserHandle.myUserId());
        } catch (Exception e) {
            MLogs.e(e);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static final String[] GMS_PKG = {
            "com.android.vending",

            "com.google.android.gsf",
            "com.google.android.gsf.login",
            "com.google.android.gms",

            "com.google.android.backuptransport",
            "com.google.android.backup",
            "com.google.android.configupdater",
            "com.google.android.syncadapters.contacts",
            "com.google.android.feedback",
            "com.google.android.onetimeinitializer",
            "com.google.android.partnersetup",
            "com.google.android.setupwizard",
            "com.google.android.syncadapters.calendar",};

    public static String[] getPreInstalledPkgs() {
        return GMS_PKG;
    }

    public static boolean isAppInstalled(String packageName) {
        return VirtualCore.get().isAppInstalled(packageName);
    }

    public static boolean installApp(Context context, AppModel appModel) {
        MLogs.d(TAG, "apkPath = " + appModel.getApkPath());
        InstallResult result = VirtualCore.get().installPackage(appModel.getApkPath(), InstallStrategy.COMPARE_VERSION | InstallStrategy.DEPEND_SYSTEM_IF_EXIST);
        if (result.isSuccess && PreferencesUtils.getBoolean(context, AppConstants.KEY_AUTO_CREATE_SHORTCUT, false)) {
            CommonUtils.createShortCut(context, appModel);
        }
        return result.isSuccess;
    }

    public static boolean uninstallApp(String packageName) {
        MLogs.d(TAG, "packageName = " + packageName);
        return VirtualCore.get().uninstallPackage(packageName, 0);
    }

//    public static Collection<ProcessRecord> getProcessList(){
//        return VirtualCore.get().getProcessList();
//    }

    public static void killApp(String packageName) {
        MLogs.d(TAG, "packageName = " + packageName);
        VirtualCore.get().killApp(packageName, VUserHandle.USER_ALL);
    }

    public static void setAppNotificationFlag(String packageName, boolean enable) {
        MLogs.d(TAG, "packageName = " + packageName + ", enable = " + enable);
        //VirtualCore.get().getService().setAppNotificationFlag(packageName, enable);
    }

//    public static void appOnCreate(Application application) {
//        VirtualCore.get().applicationOnCreate(application);
//    }

//    public static Context getOuterContext() {
//        return VirtualCore.get().getOutterContext();
//    }

    public static Context getInnerContext() {
        return VirtualCore.get().getContext();
    }

    public static boolean isMainProcess() {
        return VirtualCore.get().isMainProcess();
    }

//    public static void setInitCallback(IinitCallback callback) {
//        VirtualCore.get().setInitCallback(callback);
//    }

}
