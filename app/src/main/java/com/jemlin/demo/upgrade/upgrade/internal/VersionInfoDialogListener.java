package com.jemlin.demo.upgrade.upgrade.internal;

import android.app.Activity;

/**
 * 版本信息弹出框监听器
 */
public interface VersionInfoDialogListener {
    /**
     * 立即更新
     */
    void doUpdate(Activity activity);

    /**
     * 忽略该版本
     */
    void doIgnore();

    /**
     * 稍后提醒
     */
    void doRemindMeLater();
}
