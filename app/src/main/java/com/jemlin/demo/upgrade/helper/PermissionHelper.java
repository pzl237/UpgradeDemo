package com.jemlin.demo.upgrade.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jemlin.demo.upgrade.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限工具类
 * Created by panzhilong on 2017/11/30.
 */

public final class PermissionHelper {
    public static final int REQUEST_SETTING_CODE = 999; //调到设置界面的请求码
    public static final int REQUEST_PERMISSION_CODE = 120; // 权限请求码

    /**
     * 在Activity中验证、申请权限
     *
     * @param context     Activity
     * @param requestCode 请求码
     * @param permissions 需要申请的权限（可多个）
     * @return 是否有权限
     */
    public static boolean checkAndRequestPermissionOnActivity(Activity context, int requestCode, String... permissions) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        List<String> requestPermissions = getNoGrantedPermission(context, permissions);

        if (requestPermissions.isEmpty()) {
            return true;
        }

        ActivityCompat.requestPermissions(context, requestPermissions.toArray(new String[requestPermissions.size()]), requestCode);
        return false;
    }

    /**
     * 在Fragment中验证、申请权限
     *
     * @param context     Fragment
     * @param requestCode 请求码
     * @param permissions 需要申请的权限（可多个）
     * @return 是否有权限
     */
    public static boolean checkAndRequestPermissionOnFragment(Fragment context, int requestCode, String... permissions) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        List<String> requestPermissions = getNoGrantedPermission(context.getContext(), permissions);
        if (requestPermissions.isEmpty()) {
            return true;
        }

        context.requestPermissions(requestPermissions.toArray(new String[requestPermissions.size()]), requestCode);
        return false;
    }

    /**
     * @param context     context
     * @param permissions 权限数组
     * @return 未授权的权限
     */
    private static List<String> getNoGrantedPermission(Context context, String[] permissions) {
        List<String> requestPermissions = new ArrayList<>();

        for (String permission : permissions) {

            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(permission);
            }
        }
        return requestPermissions;
    }

    /**
     * 打开设置界面
     *
     * @param context          Activity
     * @param isFinishActivity 不授权时是否关闭Activity
     */
    public static void openSettingActivity(final Activity context, final boolean isFinishActivity) {
        new MaterialDialog.Builder(context)
                .cancelable(!isFinishActivity)
                .title(context.getString(R.string.no_permissions))
                .content(context.getString(R.string.string_help_text))
                .positiveText(context.getString(R.string.setting))
                .negativeText(context.getString(R.string.cancel))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                        intent.setData(uri);
                        context.startActivityForResult(intent, REQUEST_SETTING_CODE);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (isFinishActivity) {
                            context.finish();
                        }
                    }
                })
                .show();
    }
}
