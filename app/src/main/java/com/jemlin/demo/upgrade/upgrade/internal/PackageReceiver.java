package com.jemlin.demo.upgrade.upgrade.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;

import timber.log.Timber;

/**
 * 安装包（安装、更新等事件）监听器，主要用于删除已安装的apk 用法如下：
 * <p>
 * <pre>
 *         <receiver android:name="com.base.upgrade.PackageReceiver" >
 *             <intent-filter>
 *                 <action android:name="android.intent.action.PACKAGE_ADDED" >
 *                 </action>
 *                 <action android:name="android.intent.action.PACKAGE_REPLACED" >
 *                 </action>
 *                 <action android:name="android.intent.action.PACKAGE_INSTALL" />
 *             </intent-filter>
 *         </receiver>
 * </pre>
 */
public class PackageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String packageName = intent.getDataString();
        Timber.d("[PackageReceiver] packageName:" + packageName);
        String apkName = UpgradeHelper.downloadTempName(context.getPackageName());
        if (!TextUtils.isEmpty(packageName) && (apkName.equals(packageName) || packageName.equals("com.jemlin.demo.upgrade"))) {
            String dirPath = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            dirPath = dirPath.endsWith(File.separator) ? dirPath : dirPath
                    + File.separator;
            String targetApkPath = dirPath + apkName;
            Timber.d("[PackageReceiver] PACKAGE_ADDED:"
                    + targetApkPath);
            File targetApkFile = new File(targetApkPath);
            if (targetApkFile.exists()) {
                targetApkFile.delete();
            }
        }
    }
}