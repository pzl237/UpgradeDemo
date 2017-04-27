package com.jemlin.demo.upgrade.controllers.base;

import android.app.Application;
import android.util.Log;

import com.jemlin.demo.upgrade.BuildConfig;
import com.jemlin.demo.upgrade.helper.ToastHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executors;

import timber.log.Timber;


/**
 * 适配6.0+权限问题大家自己解决啊。
 */
public class UpgradeApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 定义日志记录的级别
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
        ToastHelper.init(this);

        //配置EventBus
        final int NUM_THREADS = 4;
        EventBus.builder().executorService(Executors.newFixedThreadPool(NUM_THREADS))
                .eventInheritance(false) //subscribers to super classes will not be notified
                .installDefaultEventBus();
    }

    /**
     * A tree which logs important information for crash reporting.
     */
    private static class CrashReportingTree extends Timber.Tree {
        private static final int MAX_LOG_LENGTH = 4000;

        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
                return;
            }

            if (message.length() < MAX_LOG_LENGTH) {
                if (priority == Log.ASSERT) {
                    Log.wtf(tag, message);
                } else {
                    Log.println(priority, tag, message);
                }
                return;
            }

            // Split by line, then ensure each line can fit into Log's maximum length.
            for (int i = 0, length = message.length(); i < length; i++) {
                int newline = message.indexOf('\n', i);
                newline = newline != -1 ? newline : length;
                do {
                    int end = Math.min(newline, i + MAX_LOG_LENGTH);
                    String part = message.substring(i, end);
                    if (priority == Log.ASSERT) {
                        Log.wtf(tag, part);
                    } else {
                        Log.println(priority, tag, part);
                    }
                    i = end;
                } while (i < newline);
            }
        }
    }
}
