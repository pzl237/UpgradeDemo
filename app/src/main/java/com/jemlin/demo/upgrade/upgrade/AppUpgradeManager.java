
package com.jemlin.demo.upgrade.upgrade;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.google.gson.Gson;
import com.jemlin.demo.upgrade.BuildConfig;
import com.jemlin.demo.upgrade.R;
import com.jemlin.demo.upgrade.event.UpgradeActivityFinishEvent;
import com.jemlin.demo.upgrade.helper.CommonHelper;
import com.jemlin.demo.upgrade.helper.NetworkHelper;
import com.jemlin.demo.upgrade.helper.ToastHelper;
import com.jemlin.demo.upgrade.services.ApiCommonRequest;
import com.jemlin.demo.upgrade.services.HttpManager;
import com.jemlin.demo.upgrade.upgrade.internal.AppUpgradePersistentHelper;
import com.jemlin.demo.upgrade.upgrade.internal.DownloadNotificationHelper;
import com.jemlin.demo.upgrade.upgrade.internal.FoundVersionInfoDialog;
import com.jemlin.demo.upgrade.upgrade.internal.PackageReceiver;
import com.jemlin.demo.upgrade.upgrade.internal.UpgradeHelper;
import com.jemlin.demo.upgrade.upgrade.internal.VersionInfo;
import com.jemlin.demo.upgrade.upgrade.internal.VersionInfoDialogListener;
import com.jemlin.demo.upgrade.widget.CustomProgressDialog;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * 升级管理，单例设计
 */
public class AppUpgradeManager implements AppUpgrade, VersionInfoDialogListener {

    private volatile static AppUpgradeManager sAppUpgradeManager;

    private Context appContext;
    private DownloadManager downloader;
    private DownloadReceiver downloaderReceiver;
    private NotificationClickReceiver notificationClickReceiver;
    /**
     * 是否初始化
     */
    private boolean isInit = false;
    private String uriDownload;
    /**
     * 服务器返回的版本信息
     */
    private VersionInfo latestVersion;
    /**
     * true为自动检测升级，false为用户手动点击触发检测升级
     */
    private boolean isCheckLatestVersionBackground = false;
    /**
     * 下载apk文件绝对路径
     */
    private String downloadApkPath;
    private CustomProgressDialog progressDialog;

    private ScheduledExecutorService scheduledExecutorService;
    private DownloadChangeObserver downloadObserver;

    public static AppUpgradeManager getInstance() {
        if (sAppUpgradeManager == null) {
            synchronized (AppUpgradeManager.class) {
                if (sAppUpgradeManager == null) {
                    sAppUpgradeManager = new AppUpgradeManager();
                }
            }
        }
        return sAppUpgradeManager;
    }

    private AppUpgradeManager() {
        downloaderReceiver = new DownloadReceiver();
        notificationClickReceiver = new NotificationClickReceiver();
    }

    @Override
    public void init(Context context) {
        Timber.d("[AppUpgradeManager] init====");
        if (isInit) {
            return;
        }

        appContext = context.getApplicationContext();
        isInit = true;

        appContext.registerReceiver(downloaderReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        appContext.registerReceiver(notificationClickReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
    }

    @Override
    public void unInit() {
        Timber.d("[AppUpgradeManager] unInit====");
        if (!isInit) {
            return;
        }

        if (downloaderReceiver != null) {
            appContext.unregisterReceiver(downloaderReceiver);
        }
        if (notificationClickReceiver != null) {
            appContext.unregisterReceiver(notificationClickReceiver);
        }
        unregisterContentObserver();

        isInit = false;
        appContext = null;
        downloadObserver = null;
    }

    @Override
    public void checkLatestVersion(Activity activity) {
        Timber.d("[AppUpgradeManager] checkLatestVersion====");
        isCheckLatestVersionBackground = false;
        progressDialog = CustomProgressDialog.show(activity, activity.getString(R.string.checkNewVersion));
        startCheckVersion();
    }

    @Override
    public void checkLatestVersionBackground() {
        Timber.d("[AppUpgradeManager] checkLatestVersionBackground====");
        isCheckLatestVersionBackground = true;
        startCheckVersion();
    }

    /**
     * 开始检测版本更新
     */
    private void startCheckVersion() {
        Timber.d("[AppUpgradeManager] startCheckVersion====");
        if (!NetworkHelper.isNetworkConnected(appContext)) {
            if (!isCheckLatestVersionBackground) {
                // 手动更新时，才提示用户网络没有开启
                CustomProgressDialog.dismiss(progressDialog);
                ToastHelper.showToast("无法连接网络，请您检查后重试");
            }
            return;
        }

        //首先确定下载apk文件的绝对路径
        final String apkName = UpgradeHelper.downloadTempName(appContext.getPackageName());
        String dirPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        dirPath = dirPath.endsWith(File.separator) ? dirPath : dirPath + File.separator;
        downloadApkPath = dirPath + apkName;

        //向服务器请求最新的版本信息
        ApiCommonRequest.upgrade(appContext, String.valueOf(CommonHelper.getPackageVersionCode(appContext)), new HttpManager.HttpServiceRequestCallBack() {
            @Override
            public void onSuccess(Object originalResponse, JSONObject response) {
                if (!isCheckLatestVersionBackground) {
                    CustomProgressDialog.dismiss(progressDialog);
                }

                if (response == null) {
                    // TODO: 2016/8/15 返回的数据缺失的提醒，暂时与onFailure处理一致！
                    networkError();
                    return;
                }

                VersionInfo versionInfo = new Gson().fromJson(response.toString(), VersionInfo.class);
                if (versionInfo == null) {
                    // TODO: 2016/8/15 返回的数据缺失的提醒，暂时与onFailure处理一致！
                    networkError();
                    return;
                }

                //与当前安装版本对比版本号，如果服务器上的版本号更大，那就需要提示更新
                if (comparedWithCurrentPackage(versionInfo)) {
                    AppUpgradePersistentHelper.saveVersionInfo(appContext, versionInfo);
                    latestVersion = versionInfo;

                    UpgradeActivity.startInstance(appContext);
                } else {
                    currentIsLatest();
                }
            }

            @Override
            public void onFailure(int errCode, String errMsg, @Nullable JSONObject response) {
                if (!isCheckLatestVersionBackground) {
                    CustomProgressDialog.dismiss(progressDialog);
                }
                networkError();
            }
        });
    }

    /**
     * 版本号比较
     *
     * @return true表示本地版本号比较小需要更新，false表示本地版本号比较大不需要更新
     */
    private boolean comparedWithCurrentPackage(VersionInfo version) {
        if (version == null) {
            return false;
        }

        int currentVersionCode = 0;
        try {
            PackageInfo pkg = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
            currentVersionCode = pkg.versionCode;
        } catch (PackageManager.NameNotFoundException exp) {
            exp.printStackTrace();
        }

        return version.getVersionCode() > currentVersionCode;
    }

    @Override
    public void foundLatestVersion(final Activity activity) {
        if (!isInit) {
            Timber.d("[AppUpgradeManager] you should call init first====");
            return;
        }
        if (latestVersion == null) {
            Timber.d("[AppUpgradeManager] latestVersion is null====");
            return;
        }

        int versionCode = AppUpgradePersistentHelper.getIgnoreUpgradeVersionCode(appContext);
        if (versionCode == latestVersion.getVersionCode()) {
            //用户之前已经选择"忽略该版本"，不更新这个版本。
            Timber.d("[AppUpgradeManager] ignore upgrade version====");
            if (isCheckLatestVersionBackground) {
                EventBus.getDefault().post(new UpgradeActivityFinishEvent());
                return;
            }
        }

        FoundVersionInfoDialog dialog = new FoundVersionInfoDialog(activity, latestVersion, isCheckLatestVersionBackground, this);
        dialog.show();
    }

    /**
     * 已经是最新版本
     */
    private void currentIsLatest() {
        Timber.d("[AppUpgradeManager] currentIsLatest====");
        if (!isCheckLatestVersionBackground) {
            ToastHelper.showToast("您当前使用的是最新版本");
        }
        //要检查本地是否有安装包，有则删除
        File apkFile = new File(downloadApkPath);
        if (apkFile.exists()) {
            boolean isDelSuc = apkFile.delete();
            Timber.d("[currentIsLatest] isDelSuc=%b", isDelSuc);
        }
    }

    private void networkError() {
        Timber.d("[AppUpgradeManager] networkError====");
        if (!isCheckLatestVersionBackground) {
            ToastHelper.showToast("无法连接服务器，请您检查后重试");
        }
    }

    /**
     * 注册ContentObserver
     */
    private void registerContentObserver() {
        if (downloadObserver != null) {
            appContext.getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), false, downloadObserver);
        }
    }

    /**
     * 注销ContentObserver
     */
    private void unregisterContentObserver() {
        if (downloadObserver != null) {
            appContext.getContentResolver().unregisterContentObserver(downloadObserver);
        }
    }

    /**
     * DownloadManager下载apk安装包
     */
    private void downloadApk() {
        Timber.d("[AppUpgradeManager] downloadApk====");
        if (!NetworkHelper.isNetworkConnected(appContext)) {
            if (!isCheckLatestVersionBackground) {
                ToastHelper.showToast("无法连接网络，请您检查后重试");
            }
            return;
        }

        //先检查本地是否已经有需要升级版本的安装包，如有就不需要再下载
        File targetApkFile = new File(downloadApkPath);
        if (targetApkFile.exists()) {
            PackageManager pm = appContext.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(downloadApkPath, PackageManager.GET_ACTIVITIES);
            if (info != null) {
                String versionCode = String.valueOf(info.versionCode);
                //比较已下载到本地的apk安装包，与服务器上apk安装包的版本号是否一致
                if (String.valueOf(latestVersion.getVersionCode()).equals(versionCode)) {
                    //弹出框提示用户安装
                    downLoadHandler.obtainMessage(WHAT_ID_INSTALL_APK, downloadApkPath).sendToTarget();
                    return;
                }
            }
        }

        //要检查本地是否有安装包，有则删除重新下
        File apkFile = new File(downloadApkPath);
        if (apkFile.exists()) {
            boolean isDelSuc = apkFile.delete();
            Timber.d("[currentIsLatest] isDelSuc=%b", isDelSuc);
        }

        if (downloader == null) {
            downloader = (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
        }

        Query query = new Query();
        long downloadTaskId = AppUpgradePersistentHelper.getDownloadTaskId(appContext);
        Timber.d("[downloadApk]setFilterById:%d", downloadTaskId);
        query.setFilterById(downloadTaskId);
        Cursor cur = downloader.query(query);
        // 检查下载任务是否已经存在
        if (cur != null && cur.moveToFirst()) {
            int columnIndex = cur.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cur.getInt(columnIndex);
            if (DownloadManager.STATUS_PENDING == status || DownloadManager.STATUS_RUNNING == status || DownloadManager.STATUS_PAUSED == status) {
                cur.close();
                if (BuildConfig.DEBUG) {
                    // TODO: 2016/8/15 临时提示，后续需要删掉
                    //ToastHelper.showToast("更新任务已在后台进行中，无需重复更新");
                    Timber.d("[downloadApk]更新任务已在后台进行中，无需重复更新");
                }
                return;
            }
        }
        if (cur != null) {
            cur.close();
        }

        if (downloadObserver == null) {
            downloadObserver = new DownloadChangeObserver();
        }
        registerContentObserver();

        Request task = new Request(Uri.parse(latestVersion.getDownloadUrl()));
        //定制Notification的样式
        String title = "最新版本:" + latestVersion.getVersion();
        task.setTitle(title);
        task.setDescription(latestVersion.getVersionDesc());
        //如果我们希望下载的文件可以被系统的Downloads应用扫描到并管理，我们需要调用Request对象的setVisibleInDownloadsUi方法，传递参数true
        task.setVisibleInDownloadsUi(true);
        //设置是否允许手机在漫游状态下下载
        task.setAllowedOverRoaming(false);
        //限定在WiFi、手机流量下进行下载
        task.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        task.setMimeType("application/vnd.android.package-archive");
        //在通知栏通知下载中和下载完成下载完成后该Notification才会被显示3.0(11)以后才有该方法。
        //在下载过程中通知栏会一直显示该下载的Notification，在下载完成后该Notification会继续显示，直到用户点击该Notification或者消除该Notification
        task.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
        task.allowScanningByMediaScanner();

        // 可能无法创建Download文件夹，如无sdcard情况，系统会默认将路径设置为/data/data/com.android.providers.downloads/cache/xxx.apk
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String apkName = UpgradeHelper.downloadTempName(appContext.getPackageName());
            task.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName);
        }
        downloadTaskId = downloader.enqueue(task);
        AppUpgradePersistentHelper.saveDownloadTaskId(appContext, downloadTaskId);
        Timber.d("[downloadApk]saveDownloadTaskId:%d", AppUpgradePersistentHelper.getDownloadTaskId(appContext));

        ToastHelper.showToast("正在后台下载更新");
        Timber.d("[downloadApk]正在后台下载更新");
    }


    @Override
    public void doUpdate(Activity activity) {
        if (NetworkHelper.getNetWorkType(appContext) == NetworkHelper.NETWORK_CLASS_WIFI) {
            // wifi网络下，直接进入下载任务
            EventBus.getDefault().post(new UpgradeActivityFinishEvent());
            downloadApk();
        } else if (NetworkHelper.getNetWorkType(appContext) == NetworkHelper.NETWORK_CLASS_UNKNOWN) {
            EventBus.getDefault().post(new UpgradeActivityFinishEvent());
            ToastHelper.showToast("无法连接网络，请您检查后重试");
        } else {
            // 非wifi网络下，再次提示用户是否继续
            MaterialDialog.Builder builder = new MaterialDialog.Builder(activity);
            final MaterialDialog dialog = builder.title("流量提醒")
                    .theme(Theme.LIGHT)
                    .titleGravity(GravityEnum.CENTER)
                    .content("您当前使用的不是wifi，更新会产生一些网络流量，是否继续下载？")
                    .positiveText("确定")
                    .negativeText("取消")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                            EventBus.getDefault().post(new UpgradeActivityFinishEvent());
                            downloadApk();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                            EventBus.getDefault().post(new UpgradeActivityFinishEvent());
                        }
                    })
                    .build();
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    EventBus.getDefault().post(new UpgradeActivityFinishEvent());
                }
            });
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        EventBus.getDefault().post(new UpgradeActivityFinishEvent());
                    }
                    return false;
                }
            });
            dialog.show();
        }
    }

    @Override
    public void doIgnore() {
        EventBus.getDefault().post(new UpgradeActivityFinishEvent());
        //忽略该版本
        AppUpgradePersistentHelper.saveIgnoreUpgradeVersionCode(appContext, latestVersion.getVersionCode());
    }

    @Override
    public void doRemindMeLater() {
        EventBus.getDefault().post(new UpgradeActivityFinishEvent());
        //什么都不处理
    }

    /**
     * 安装apk
     * <p>
     * 通过广播{@link PackageReceiver}来监听安装完成再删除apk文件
     * </p>
     */
    private void installAPKFile() {
        Timber.d("[AppUpgradeManager] installAPKFile====");
        if (TextUtils.isEmpty(uriDownload)) {
            // FIXME: 2016/8/15 临时提示语
            ToastHelper.showToast("App安装文件不存在!");
            return;
        }

        File apkFile = new File(Uri.parse(uriDownload).getPath());
        if (!apkFile.exists()) {
            // FIXME: 2016/8/15 临时提示语
            ToastHelper.showToast("App安装文件不存在!");
            return;
        }

        Intent installIntent = new Intent();
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.setAction(Intent.ACTION_VIEW);

        Uri apkFileUri;
        // 在24及其以上版本，解决崩溃异常：
        // android.os.FileUriExposedException: file:///storage/emulated/0/xxx exposed beyond app through Intent.getData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkFileUri = FileProvider.getUriForFile(appContext, BuildConfig.APPLICATION_ID + ".provider", apkFile);
        } else {
            apkFileUri = Uri.fromFile(apkFile);
        }
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        installIntent.setDataAndType(apkFileUri, "application/vnd.android.package-archive");
        try {
            appContext.startActivity(installIntent);
        } catch (ActivityNotFoundException e) {
            Timber.d("installAPKFile exception:%s", e.toString());
        }
    }

    /**
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     *
     * @param downloadId 下载任务id
     */
    private int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{
                -1, -1, 0
        };
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = null;
        try {
            cursor = downloader.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                //已经下载文件大小
                bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                //下载文件的总大小
                bytesAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bytesAndStatus;
    }

    /**
     * 发送Handler消息更新进度和状态
     */
    private void updateProgress() {
        long downloadTaskId = AppUpgradePersistentHelper.getDownloadTaskId(appContext);
        int[] bytesAndStatus = getBytesAndStatus(downloadTaskId);
        downLoadHandler.sendMessage(downLoadHandler.obtainMessage(WHAT_ID_DOWNLOAD_INFO, bytesAndStatus[0], bytesAndStatus[1]));
    }

    private final int WHAT_ID_INSTALL_APK = 1;
    private final int WHAT_ID_DOWNLOAD_INFO = 2;
    private Handler downLoadHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == WHAT_ID_INSTALL_APK) {
                uriDownload = (String) msg.obj;
                installAPKFile();
            } else if (msg.what == WHAT_ID_DOWNLOAD_INFO) {
                //被除数可以为0，除数必须大于0
                if (msg.arg1 >= 0 && msg.arg2 > 0) {
                    int progress = (int) (msg.arg1 / (float) msg.arg2 * 100);
                    Timber.e("[AppUpgradeManager] progress=%d%%", progress);
                    String title = "最新版本:" + latestVersion.getVersion();
                    String versionDesc = "";
                    if (!TextUtils.isEmpty(latestVersion.getVersionDesc())) {
                        versionDesc = latestVersion.getVersionDesc().replace("\n", "");
                    }
                    DownloadNotificationHelper.sendDefaultNotice(appContext, title, versionDesc, progress);
                }
            }
        }
    };

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    /**
     * 监听下载进度
     */
    private class DownloadChangeObserver extends ContentObserver {

        DownloadChangeObserver() {
            super(downLoadHandler);
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         *
         * @param selfChange 此值意义不大, 一般情况下该回调值false
         */
        @Override
        public void onChange(boolean selfChange) {
            scheduledExecutorService.scheduleAtFixedRate(progressRunnable, 0, 1, TimeUnit.SECONDS);
        }
    }


    /**
     * 下载完成的广播
     */
    class DownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("[AppUpgradeManager] DownloadReceiver onReceive====");
            if (downloader == null) {
                return;
            }
            long completeId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            long downloadTaskId = AppUpgradePersistentHelper.getDownloadTaskId(context);
            Timber.d("[DownloadReceiver] completeId=%d, downloadTaskId=%d", completeId, downloadTaskId);
            if (completeId != downloadTaskId) {
                return;
            }

            Query query = new Query();
            query.setFilterById(downloadTaskId);
            Cursor cur = downloader.query(query);
            if (cur == null || !cur.moveToFirst()) {
                return;
            }

            int columnIndex = cur.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (DownloadManager.STATUS_SUCCESSFUL == cur.getInt(columnIndex)) {
                //下载完成这里也要通知刷新消息通知栏
                Timber.e("[AppUpgradeManager] download finish!");
                updateProgress();

                String uriString = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                downLoadHandler.obtainMessage(WHAT_ID_INSTALL_APK, uriString).sendToTarget();
            } else {
                // TODO: 2016/8/15 临时提示，后续需要删掉
                ToastHelper.showToast("下载App最新版本失败!");
                Timber.d("[DownloadReceiver]下载最新版本失败!");
            }
            // 下载任务已经完成，清除
            AppUpgradePersistentHelper.removeDownloadTaskId(context);
            cur.close();
        }
    }

    /**
     * 点击通知栏下载项目，下载完成前点击都会进来，下载完成后点击不会进来。
     */
    public class NotificationClickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("[AppUpgradeManager] NotificationClickReceiver onReceive====");
            long[] completeIds = intent.getLongArrayExtra(
                    DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
            //正在下载的任务ID
            long downloadTaskId = AppUpgradePersistentHelper.getDownloadTaskId(context);
            if (completeIds == null || completeIds.length <= 0) {
                openDownloadsPage(appContext);
                return;
            }

            for (long completeId : completeIds) {
                if (completeId == downloadTaskId) {
                    openDownloadsPage(appContext);
                    break;
                }
            }
        }

        /**
         * Open the Activity which shows a list of all downloads.
         *
         * @param context 上下文
         */
        private void openDownloadsPage(Context context) {
            Intent pageView = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            pageView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(pageView);
        }
    }
}
