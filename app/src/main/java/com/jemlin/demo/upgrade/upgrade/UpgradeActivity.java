package com.jemlin.demo.upgrade.upgrade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.jemlin.demo.upgrade.R;
import com.jemlin.demo.upgrade.controllers.base.BaseActivity;
import com.jemlin.demo.upgrade.event.UpgradeActivityFinishEvent;
import com.jude.swipbackhelper.SwipeBackHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * 透明Activity，用于检查版本更新
 * <p>
 * 采用EventBus实现订阅方法：
 * 1、{@link #handleUpgradeEvent} 订阅从{@link AppUpgradeManager}post的{@link UpgradeActivityFinishEvent}类型数据，用于通知关闭透明Activity。
 * </p>
 */
public class UpgradeActivity extends BaseActivity {

    boolean isShowDialog = false;

    public static void startInstance(Context context) {
        Intent intent = new Intent(context, UpgradeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);
        SwipeBackHelper.getCurrentPage(this).setSwipeBackEnable(false);
        EventBus.getDefault().register(this);

        overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void handleIntent(@Nullable Intent intent) {

    }

    @Override
    protected void initUI(Bundle savedInstanceState) {

    }

    @Override
    protected void initData() {

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !isShowDialog) {
            isShowDialog = true;
            AppUpgradeManager.getInstance().foundLatestVersion(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void handleUpgradeEvent(UpgradeActivityFinishEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
                overridePendingTransition(R.anim.alpha_out, R.anim.alpha_in);
            }
        });
    }
}
