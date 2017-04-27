package com.jemlin.demo.upgrade.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.widget.Button;

import com.jemlin.demo.upgrade.R;
import com.jemlin.demo.upgrade.config.Constants;
import com.jemlin.demo.upgrade.controllers.base.BaseActivity;
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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 检查最新版本，并弹出窗口
                appUpgrade.checkLatestVersionBackground();
            }
        }, Constants.App.UPGRADE_DELAY_TIME);
    }

    @OnClick(R.id.btnCheckUpgrade)
    void onClickBtnCheckUpgrade() {
        appUpgrade.checkLatestVersion(this);
    }
}
