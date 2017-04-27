package com.jemlin.demo.upgrade.upgrade.internal;

import android.content.Context;

import com.jemlin.demo.upgrade.helper.SPHelper;

import timber.log.Timber;


/**
 * 该类用于保存版本更新需要持久化的数据，比如服务器返回的最新版本信息、下载任务id、被忽略更新的版本号。
 */
public class AppUpgradePersistent {

    private static final String DOWNLOAD_TASK_ID = "download_task_id";
    private static final String VERSION_CODE = "version_code";
    private static final String VERSION_NAME = "version_name";
    private static final String VERSION_FEATURE = "feature";
    private static final String VERSION_URL = "url";
    private static final String MD5 = "md5";
    private static final String IS_MUST_UPGRADE = "isMustUpgrade";
    private static final String IGNORE_VERSION_CODE = "ignore_version_code";//被忽略更新的版本号

    public AppUpgradePersistent() {
    }

    /**
     * 保存数据
     *
     * @param version 服务器上最新版本的版本信息
     */
    public void saveVersionInfo(Context context, VersionInfo version) {
        if (version == null) {
            return;
        }

        SPHelper.saveInt(context, VERSION_CODE, version.getVersionCode());
        SPHelper.saveString(context, VERSION_NAME, version.getVersion());
        SPHelper.saveString(context, VERSION_FEATURE, version.getVersionDesc());
        SPHelper.saveString(context, VERSION_URL, version.getDownloadUrl());
        SPHelper.saveString(context, MD5, version.getMd5());
        SPHelper.saveBoolean(context, IS_MUST_UPGRADE, version.isMustUpgrade());
    }

    /**
     * 加载数据
     */
    public VersionInfo getVersionInfo(Context context) {
        int code = SPHelper.getInt(context, VERSION_CODE);
        String name = SPHelper.getString(context, VERSION_NAME);
        String feature = SPHelper.getString(context, VERSION_FEATURE);
        String url = SPHelper.getString(context, VERSION_URL);
        String md5 = SPHelper.getString(context, MD5);
        boolean isMustUpgrade = SPHelper.getBoolean(context, IS_MUST_UPGRADE, false);
        return new VersionInfo(code, name, feature, md5, url, isMustUpgrade);
    }

    /**
     * 保存的当前下载任务id
     */
    public void saveDownloadTaskId(Context context, long downloadTaskId) {
        SPHelper.saveLong(context, DOWNLOAD_TASK_ID, downloadTaskId);
    }

    /**
     * 获取保存的当前下载任务id
     */
    public long getDownloadTaskId(Context context) {
        long downloadTaskId = SPHelper.getLong(context, DOWNLOAD_TASK_ID, -12306L);
        Timber.d("[getDownloadTaskId] downloadTaskId=%d", downloadTaskId);
        return downloadTaskId;
    }

    /**
     * 移除保存的下载任务id
     */
    public void removeDownloadTaskId(Context context) {
        SPHelper.remove(context, DOWNLOAD_TASK_ID);
    }

    /**
     * 保存的被忽略更新的版本号
     */
    public void saveIgnoreUpgradeVersionCode(Context context, int versionCode) {
        SPHelper.saveInt(context, IGNORE_VERSION_CODE, versionCode);
    }

    /**
     * 获取被忽略更新的版本号
     */
    public int getIgnoreUpgradeVersionCode(Context context) {
        return SPHelper.getInt(context, IGNORE_VERSION_CODE);
    }

    /**
     * 是否存在被忽略更新的版本号
     */
    public boolean hasIgnoreUpgradeVersionCode() {
        return SPHelper.contains(IGNORE_VERSION_CODE);
    }

    /**
     * 移除保存的被忽略更新的版本号
     */
    public void removeIgnoreUpgradeVersionCode(Context context, int versionCode) {
        SPHelper.remove(context, IGNORE_VERSION_CODE);
    }
}
