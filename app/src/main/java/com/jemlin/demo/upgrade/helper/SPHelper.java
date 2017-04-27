package com.jemlin.demo.upgrade.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import timber.log.Timber;

/**
 * 本地数据存储类
 */
public class SPHelper {

    final static String SUFFIX = "myPref";
    final static String PREF_PASSWORD = "P@ssw0rd!@##@!";

    /**
     * Preferences的存储文件名称
     */
    static String sPrefFileName = null;

    static SharedPreferences mPref = null;

    private static SharedPreferences getPref(Context context) {
        if (mPref == null) {
            mPref = context.getSharedPreferences("upgrade_pref", Context.MODE_PRIVATE);
        }
        return mPref;
    }

    private static String getFileName(Context context) {
        if (sPrefFileName != null) {
            return sPrefFileName;
        } else {
            sPrefFileName = context.getPackageName();
            sPrefFileName = String.format("%s.%s", sPrefFileName, SUFFIX);
            return sPrefFileName;
        }
    }

    public static boolean contains(String key) {
        return mPref.contains(key);
    }

    public static void saveString(Context context, String key, String content) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putString(key, content);
        editor.apply();
    }

    public static String getString(Context context, int resId) {
        return getPref(context).getString(context.getResources().getString(resId, context), null);
    }

    @Nullable
    public static String getString(Context context, String key) {
        return getPref(context).getString(key, "");
    }

    public static void saveInt(Context context, String key, int content) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putInt(key, content);
        editor.apply();
    }

    public static int getInt(Context context, String key) {
        int value = -1;
        try {
            boolean contains = getPref(context).contains(key);
            if (contains) {
                value = getPref(context).getInt(key, -1);
            }
        } catch (NumberFormatException e) {
            Timber.d("[SecurityPref]getInt throw exception:%s", e.toString());
        }
        return value;
    }

    public static long getLong(Context context, String key, long defaultValue) {
        return getPref(context).getLong(key, defaultValue);
    }

    public static void saveLong(Context context, String key, long defaultValue) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putLong(key, defaultValue);
        editor.apply();
    }

    public static float getFloat(Context context, String key, float defaultValue) {
        return getPref(context).getFloat(key, defaultValue);
    }

    public static void saveFloat(Context context, String key, float defaultValue) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putFloat(key, defaultValue);
        editor.apply();
    }

    public static void saveBoolean(Context context, String key, boolean defaultValue) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.putBoolean(key, defaultValue);
        editor.apply();
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getPref(context).getBoolean(key, defaultValue);
    }

    public static void remove(Context context, String key) {
        SharedPreferences.Editor editor = getPref(context).edit();
        editor.remove(key);
        editor.apply();
    }
}
