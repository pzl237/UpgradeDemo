package com.jemlin.demo.upgrade.controllers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Button;

import com.jemlin.demo.upgrade.R;
import com.jemlin.demo.upgrade.config.Constants;
import com.jemlin.demo.upgrade.controllers.base.BaseActivity;
import com.jemlin.demo.upgrade.helper.PermissionHelper;
import com.jemlin.demo.upgrade.upgrade.AppUpgrade;
import com.jemlin.demo.upgrade.upgrade.AppUpgradeManager;
import com.jude.swipbackhelper.SwipeBackHelper;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends BaseActivity {
    @BindView(R.id.btnCheckUpgrade)
    Button btnCheckUpgrade;

    AppUpgrade appUpgrade;

    // 所需的必要权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appUpgrade = AppUpgradeManager.getInstance();
        appUpgrade.init(getApplicationContext());
    }

    @Override
    public void finish() {
        Timber.d("[MainTabActivity] finish=====");
        if (appUpgrade != null) {
            appUpgrade.unInit();
        }
        super.finish();
    }

    @Override
    protected void handleIntent(@Nullable Intent intent) {

    }

    @Override
    protected void initUI(Bundle savedInstanceState) {
        // 最开始的activity要开启点击两次返回键关闭程序的功能
        setPressBackToExit(true);
        // 最开始的activity要关闭右滑返回的功能
        SwipeBackHelper.getCurrentPage(this).setSwipeBackEnable(false);
    }

    @Override
    protected void initData() {
        //检测权限
        if (PermissionHelper.checkAndRequestPermissionOnActivity(this, PermissionHelper.REQUEST_PERMISSION_CODE, PERMISSIONS)) {
            checkUpgrade();
        }
    }

    @OnClick(R.id.btnCheckUpgrade)
    void onClickBtnCheckUpgrade() {
        appUpgrade.checkLatestVersion(this);
    }

    private void checkUpgrade(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 检查最新版本，并弹出窗口
                appUpgrade.checkLatestVersionBackground();
            }
        }, Constants.App.UPGRADE_DELAY_TIME);
    }

    /**
     * 权限请求回调
     *
     * @param requestCode  请求码
     * @param permissions  权限
     * @param grantResults 请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_PERMISSION_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    PermissionHelper.openSettingActivity(this, true);
                    return;
                }
            }

            checkUpgrade();
        }
    }
}
