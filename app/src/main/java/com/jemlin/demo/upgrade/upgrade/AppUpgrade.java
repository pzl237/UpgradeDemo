
package com.jemlin.demo.upgrade.upgrade;

import android.app.Activity;
import android.content.Context;

/**
 * 升级服务的操作接口
 * <p>需要的权限如下：
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 * <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * <uses-permission
 * android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
 * <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
 * <p>
 */
public interface AppUpgrade {

    /**
     * 初始化，在主Activity的onCreate调用，注意只需调用一次
     *
     * @param context 必须是ApplicationContext
     */
    void init(Context context);

    /**
     * 反初始化，在程序退出调用一次
     */
    void unInit();

    /**
     * 检测升级，一般是用户手动点击触发检测升级逻辑。
     */
    void checkLatestVersion(Activity activity);

    /**
     * 与checkLatestVersion相似，区别在于该请求用于后台，不会出现相关toast提示
     */
    void checkLatestVersionBackground();

    void foundLatestVersion(Activity activity);

}
