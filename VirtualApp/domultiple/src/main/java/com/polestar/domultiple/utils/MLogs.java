package com.polestar.domultiple.utils;

import android.util.Log;

import com.polestar.domultiple.BuildConfig;

/**
 * Created by PolestarApp on 2017/7/16.
 */

public class MLogs {

    public static boolean DEBUG = BuildConfig.DEBUG ;
    public final static String DEFAULT_TAG = "DoMultiple";

    public static void e(String msg) {
        e(DEFAULT_TAG, msg);
    }

    public static void i(String msg) {
        i(DEFAULT_TAG, msg);
    }

    public static void d(String msg) {
        d(DEFAULT_TAG, msg);
    }

    public static void v(String msg) {
        v(DEFAULT_TAG, msg);
    }

    public static void e(String tag, String msg) {
        if (DEBUG)
            Log.e(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (DEBUG)
            Log.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (DEBUG)
            Log.d(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (DEBUG)
            Log.v(tag, msg);
    }

    public static String getStackTraceString(Throwable tr) {
        return Log.getStackTraceString(tr);
    }
    public static void e(Throwable e) {
        Log.e(DEFAULT_TAG, getStackTraceString(e));
    }

    public static void logBug(String tag, String msg) {
        Log.e(tag, msg);
        //BuglyLog.e(tag+"_BUGLY", msg);
    }

    public static void logBug(String msg) {
        Log.e(DEFAULT_TAG, msg);
        //BuglyLog.e(DEFAULT_TAG+"_BUGLY", msg);
    }
    public static void logBug(Exception ex) {
        MLogs.logBug(MLogs.getStackTraceString(ex));
    }
}
