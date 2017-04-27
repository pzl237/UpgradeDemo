package com.jemlin.demo.upgrade.controllers.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.jemlin.demo.upgrade.R;
import com.jemlin.demo.upgrade.helper.ToastHelper;
import com.jude.swipbackhelper.SwipeBackHelper;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.ButterKnife;

public abstract class BaseActivity extends AppCompatActivity {

    protected Context context;

    private AtomicBoolean isBack;//判断用户是否按了返回键
    private boolean pressBackToExit;// 按返回键是否要退出程序

    /**
     * 处理intent参数
     */
    abstract protected void handleIntent(@Nullable Intent intent);

    abstract protected void initUI(Bundle savedInstanceState);

    abstract protected void initData();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SwipeBackHelper.onCreate(this);
        //设置SwipeBackHelper选项
        SwipeBackHelper.getCurrentPage(this)//获取当前页面
                .setSwipeEdgePercent(0.2f)//可滑动的范围。百分比。0.2表示为左边20%的屏幕
                .setSwipeRelateEnable(false);//是否与下一级activity联动(微信效果)。默认关
        context = this;
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        // 子类不需要再bind一次
        ButterKnife.bind(this);

        handleIntent(getIntent());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        SwipeBackHelper.onPostCreate(this);

        initUI(savedInstanceState);
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SwipeBackHelper.onDestroy(this);
    }

    /**
     * 设置该界面为按两次界面键就退出程序
     *
     * @param flag 如果当前界面需要点击两次退出程序的话，将这个值设置为true
     */
    protected void setPressBackToExit(boolean flag) {
        pressBackToExit = flag;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && pressBackToExit) {
            // 在欢迎界面挂起，标识程序已经退出，不做后续处理
            if (isBack == null) {
                isBack = new AtomicBoolean(true);
                //final Toast pressBackExitToast = Toast.makeText(context, R.string.pressAgainToExit, Toast.LENGTH_SHORT);
                ToastHelper.showToast(getResources().getString(R.string.pressAgainToExit));
                //pressBackExitToast.show();

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //pressBackExitToast.cancel();
                        isBack = null;
                    }
                }, 1200);
                return true;
            } else if (isBack.get()) {
                moveTaskToBack(true);
                finish();

                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
