package com.polestar.multiaccount;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.delegate.PhoneInfoDelegate;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.helper.utils.VLog;
import com.polestar.ad.AdConstants;
import com.polestar.multiaccount.component.LocalActivityLifecycleCallBacks;
import com.polestar.multiaccount.component.MComponentDelegate;
import com.polestar.multiaccount.constant.AppConstants;
import com.polestar.multiaccount.utils.CommonUtils;
import com.polestar.multiaccount.utils.ImageLoaderUtil;
import com.polestar.multiaccount.utils.LocalExceptionCollectUtils;
import com.polestar.multiaccount.utils.MLogs;
import com.polestar.multiaccount.utils.MTAManager;
import com.polestar.multiaccount.utils.RemoteConfig;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.mobvista.msdk.MobVistaConstans;
import com.mobvista.msdk.MobVistaSDK;
import com.mobvista.msdk.out.MobVistaSDKFactory;

public class MApp extends Application {

    private static MApp gDefault;

    public static MApp getApp() {
        return gDefault;
    }

    public static boolean isOpenLog(){
        File file = new File(Environment.getExternalStorageDirectory() + "/polelog");
        boolean ret =  file.exists();
        if(ret) {
            Log.d(MLogs.DEFAULT_TAG, "log opened by file");
        }
        return  ret;
    }

    @Override
    protected void attachBaseContext(Context base) {
        Log.d(MLogs.DEFAULT_TAG, "APP version: " + BuildConfig.VERSION_NAME + " Type: " + BuildConfig.BUILD_TYPE);
        Log.d(MLogs.DEFAULT_TAG, "LIB version: " + com.lody.virtual.BuildConfig.VERSION_NAME + " Type: " + com.lody.virtual.BuildConfig.BUILD_TYPE );

        super.attachBaseContext(base);
        try {
            StubManifest.ENABLE_IO_REDIRECT = true;
            StubManifest.ENABLE_INNER_SHORTCUT = false;
            VirtualCore.get().startup(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        gDefault = this;
        super.onCreate();
        VirtualCore virtualCore = VirtualCore.get();
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                MobVistaSDK sdk = MobVistaSDKFactory.getMobVistaSDK();
                // test appId and appKey
                String appId = "33047";
                String appKey = "e4a6e0bf98078d3fa81ca6d315c28123";
                Map<String, String> map = sdk.getMVConfigurationMap(appId, appKey);

                // if you modify applicationId, please add the following attributes,
                // otherwise it will crash
                // map.put(MobVistaConstans.PACKAGE_NAME_MANIFEST, "your AndroidManifest
                // package value");
                sdk.init(map, gDefault);

                ImageLoaderUtil.init(gDefault);
                initRawData();
                registerActivityLifecycleCallbacks(new LocalActivityLifecycleCallBacks(MApp.this, true));
            }

            @Override
            public void onVirtualProcess() {
                MComponentDelegate delegate = new MComponentDelegate();
                delegate.init();
                virtualCore.setComponentDelegate(delegate);
                virtualCore.setPhoneInfoDelegate(new MyPhoneInfoDelegate());

                virtualCore.setAppApiDelegate(new AppApiDelegate());
            }

            @Override
            public void onServerProcess() {
                VirtualCore.get().setAppRequestListener(new VirtualCore.AppRequestListener() {
                    @Override
                    public void onRequestInstall(String path) {
                        //We can start AppInstallActivity TODO
                        Toast.makeText(MApp.this, "Installing: " + path, Toast.LENGTH_SHORT).show();
                        InstallResult res = VirtualCore.get().installPackage(path, InstallStrategy.UPDATE_IF_EXIST);
                        if (res.isSuccess) {
                            try {
                                VirtualCore.get().preOpt(res.packageName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (res.isUpdate) {
                                Toast.makeText(MApp.this, "Update: " + res.packageName + " success!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MApp.this, "Install: " + res.packageName + " success!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MApp.this, "Install failed: " + res.error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onRequestUninstall(String pkg) {
                        Toast.makeText(MApp.this, "Uninstall: " + pkg, Toast.LENGTH_SHORT).show();

                    }
                });

                MComponentDelegate delegate = new MComponentDelegate();
                delegate.init();
                VirtualCore.get().setComponentDelegate(delegate);
                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqq");
                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqqi");
                virtualCore.addVisibleOutsidePackage("com.tencent.minihd.qq");
                virtualCore.addVisibleOutsidePackage("com.tencent.qqlite");
                virtualCore.addVisibleOutsidePackage("com.facebook.katana");
                virtualCore.addVisibleOutsidePackage("com.whatsapp");
                virtualCore.addVisibleOutsidePackage("com.tencent.mm");
                virtualCore.addVisibleOutsidePackage("com.immomo.momo");
            }
        });

        try {
            // init exception handler and bugly before attatchBaseContext and appOnCreate
            setDefaultUncaughtExceptionHandler(this);
            initBugly(this);
            MTAManager.init(this);
            FirebaseApp.initializeApp(this);
            RemoteConfig.init();
        }catch (Exception e){
            e.printStackTrace();
        }

        if (isOpenLog() || !AppConstants.IS_RELEASE_VERSION ) {
            VLog.openLog();
            VLog.d(MLogs.DEFAULT_TAG, "VLOG is opened");
            MLogs.DEBUG = true;
            AdConstants.DEBUG = true;
        }
        VLog.setKeyLogger(new VLog.IKeyLogger() {
            @Override
            public void keyLog(Context context, String tag, String log) {
                MLogs.logBug(tag,log);
                MTAManager.keyLog(MApp.gDefault, tag, log);
            }

            @Override
            public void logBug(String tag, String log) {
                MLogs.logBug(tag, log);
            }
        });
    }

    private class MAppCrashHandler implements Thread.UncaughtExceptionHandler {

        private Context context;
        private Thread.UncaughtExceptionHandler orig;
        MAppCrashHandler(Context c, Thread.UncaughtExceptionHandler orig) {
            context = c;
            this.orig = orig;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            MLogs.logBug("uncaughtException");
            MLogs.e(ex);
            CrashReport.startCrashReport();
            Context innerContext = VClientImpl.get().getCurrentApplication();
            //1. innerContext = null, internal error in Pb
            if (innerContext == null) {
                MLogs.logBug("MApp internal exception, exit.");
                CrashReport.setUserSceneTag(context, AppConstants.CrashTag.MAPP_CRASH);
                CrashReport.postCatchedException(ex);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                LocalExceptionCollectUtils.saveExceptionToLocalFile(context, ex);

                //2. innerContext != null, error in third App, bugly and MTA will report.
                MLogs.e("cur process id:" + android.os.Process.myPid());
                MLogs.e("cur thread name:" + Thread.currentThread().getName());
                ActivityManager.RunningAppProcessInfo info = CommonUtils.getForegroundProcess(context);
                if (info != null) {
                    MLogs.e("foreground process: " + info.pid);
                    MLogs.e("foreground process: " + info.processName);
                }

                //2.1 crash and app exit

                if (info != null && android.os.Process.myPid() == info.pid) {
                    // Toast
                    Intent crash = new Intent("appclone.intent.action.SHOW_CRASH_DIALOG");
                    MLogs.logBug("inner packagename: " + innerContext.getPackageName());
                    crash.putExtra("package", innerContext.getPackageName());
                    crash.putExtra("exception", ex);
                    sendBroadcast(crash);
                } else {
                    //2.2 crash but app not exit
                    MLogs.logBug("report crash, but app not exit.");
                    CrashReport.postCatchedException(ex);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
//            if (orig != null) {
//                orig.uncaughtException(thread, ex);
//            } else {
//
//            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }
    private void setDefaultUncaughtExceptionHandler(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new MAppCrashHandler(context, Thread.getDefaultUncaughtExceptionHandler()));
    }

    private void initRawData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String localFilePath = getApplicationContext().getFilesDir().toString();
                String path = localFilePath + "/" + AppConstants.POPULAR_FILE_NAME;
                copyRawDataToLocal(path, R.raw.popular_apps);
            }
        }).start();
    }

    private void copyRawDataToLocal(String filePath, int resourceId) {
        try {
            File file = new File(filePath);
            // already copied
            if (file.exists()) {
                return;
            } else {
                if (file.createNewFile()) {
                    InputStream in = getResources().openRawResource(resourceId);
                    OutputStream out = new FileOutputStream(file);
                    byte[] buff = new byte[4096];
                    int count = 0;
                    while ((count = in.read(buff)) > 0) {
                        out.write(buff, 0, count);
                    }
                    out.close();
                    in.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initBugly(Context context) {
        //Bugly
        String channel = CommonUtils.getMetaDataInApplicationTag(context, "CHANNEL_NAME");
        AppConstants.IS_RELEASE_VERSION = !channel.equals(AppConstants.DEVELOP_CHANNEL);
        MLogs.e("IS_RELEASE_VERSION: " + AppConstants.IS_RELEASE_VERSION);
        MLogs.e("bugly channel: " + channel);
        MLogs.e("versioncode: " + CommonUtils.getCurrentVersionCode(context) + ", versionName:" + CommonUtils.getCurrentVersionName(context));
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        strategy.setAppChannel(channel);
        CrashReport.initCrashReport(context, "900060178", !AppConstants.IS_RELEASE_VERSION, strategy);
        // close auto report, manual control
        CrashReport.closeCrashReport();
    }

    class MyPhoneInfoDelegate implements PhoneInfoDelegate {

        @Override
        public String getDeviceId(String oldDeviceId, int userId) {
            return oldDeviceId;
        }

        @Override
        public String getBluetoothAddress(String oldAddress, int userId) {
            return oldAddress;
        }

        @Override
        public String getMacAddress(String oldMacAddress, int userId) {
            if (oldMacAddress == null || oldMacAddress.startsWith("00-00-00-00-00-00") ){
                    return "00:00:08:76:54:32";
            }
            return oldMacAddress;
        }

    }
}

