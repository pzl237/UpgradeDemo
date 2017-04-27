package com.jemlin.demo.upgrade.helper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.ColorRes;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.Window;
import android.widget.CheckBox;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * 常用工具类
 */
public class CommonHelper {

    public static File getSdcardFile() {
        return Environment.getExternalStorageDirectory();
    }

    /**
     * 获取sdcard下以包名命名的文件夹
     */
    public static String getPackageDir(Context context) {
        String simplePackageName = context.getPackageName().substring(
                context.getPackageName().lastIndexOf(".") + 1);
        return new StringBuilder(getSdcardFile().getAbsolutePath())
                .append(File.separator).append(simplePackageName)
                .append(File.separator).toString();
    }

    /**
     * 根绝给定的路径逐级创建目录
     *
     * @param filePath 文件路径
     */
    public static void createFileDirs(String filePath) {
        StringTokenizer st = new StringTokenizer(filePath, File.separator);
        String path1 = st.nextToken() + File.separator;
        String path2 = path1;
        while (st.hasMoreTokens()) {
            path1 = st.nextToken() + File.separator;
            path2 += path1;
            File inbox = new File(path2);
            if (!inbox.exists()) {
                inbox.mkdir();
            }
        }
    }

    /**
     * 唯一设备ID：GSM手机的 IMEI 和 CDMA手机的 MEID
     */
    public static String getImei(Context mContext) {
        TelephonyManager mTelephonyMgr = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getDeviceId();
    }

    /**
     * 唯一的用户ID：IMSI(国际移动用户识别码) for a GSM phone
     */
    public static String getImsi(Context mContext) {
        TelephonyManager mTelephonyMgr = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        return mTelephonyMgr.getSubscriberId();
    }

    /**
     * 获取长宽都不超过160dip的图片，基本思想是设置Options.inSampleSize按比例取得缩略图
     */
    public static Options getOptionsWithInSampleSize(String filePath,
                                                     int maxWidth) {
        Options bitmapOptions = new Options();
        bitmapOptions.inJustDecodeBounds = true;// 只取得outHeight(图片原始高度)和
        // outWidth(图片的原始宽度)而不加载图片
        BitmapFactory.decodeFile(filePath, bitmapOptions);
        bitmapOptions.inJustDecodeBounds = false;
        int inSampleSize = bitmapOptions.outWidth / (maxWidth / 10);// 应该直接除160的，但这里出16是为了增加一位数的精度
        if ((inSampleSize % 10) != 0) {
            inSampleSize += 10;// 尽量取大点图片，否则会模糊
        }
        inSampleSize = inSampleSize / 10;
        if (inSampleSize <= 0) {// 判断200是否超过原始图片高度
            inSampleSize = 1;// 如果超过，则不进行缩放
        }
        bitmapOptions.inSampleSize = inSampleSize;
        return bitmapOptions;
    }

    /**
     * 获取屏幕最小边
     */
    public static int getScreenMin(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = context.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        return (screenWidth > screenHeight) ? screenHeight : screenWidth;
    }

    /**
     * 获取屏幕宽度
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 获取屏幕信息
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        return context.getResources().getDisplayMetrics();
    }

    /**
     * 获取当前应用的名称
     */
    public static String getSimplePackageName(Context context) {
        return context.getPackageName().substring(
                context.getPackageName().lastIndexOf(".") + 1);
    }

    /**
     * 获取包版本号
     */
    public static int getPackageVersionCode(Context context) {
        PackageInfo info;
        try {
            info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return info.versionCode;
        } catch (NameNotFoundException e) {
        }
        return 0;
    }

    /**
     * 获取包版本名
     */
    public static String getPackageVersionName(Context context) {
        PackageInfo info = null;
        try {
            info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return info.versionName;
        } catch (NameNotFoundException e) {
        }
        return "";
    }

    public static String getTimeString() {
        return getTimeString("yyyyMMddHHmmss");
    }

    public static String getTimeString(long time) {
        return getTimeString("yyyyMMddHHmmss");
    }

    public static String getTimeString(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    public static String getTimeString(String pattern, long time) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(time));
    }

    public static long getTimeLong(String pattern, String dateString) {
        try {
            Date date = new SimpleDateFormat(pattern, Locale.getDefault()).parse(dateString);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 返回当前应用是否横屏
     */
    public static boolean isLandscape(Context context) {
        Configuration cf = context.getResources().getConfiguration();
        return cf.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * 是否需要缩放图片 1.图片尺寸大于200k 2.图片大小大于屏幕最宽
     */
    public static Boolean isNeedScaleImage(String filename) {
        long MAX_SIZE_IMAGE = 20 * 1024;
        // 判断文件大小
        File imageFile = new File(filename);
        if (!imageFile.exists()) {
            return false;
        }
        if (imageFile.length() > MAX_SIZE_IMAGE) {
            return true;
        }
        // 判断文件尺寸
        Options bitmapOptions = new Options();
        bitmapOptions.inJustDecodeBounds = true;// 只取得outHeight(图片原始高度)和
        bitmapOptions.inSampleSize = 1;
        // outWidth(图片的原始宽度)而不加载图片
        Bitmap bitmap = BitmapFactory.decodeFile(filename, bitmapOptions);
        if (bitmap == null) {
            return false;
        }
        return false;
    }

    /**
     * 是否有可用网络
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null) {
            return false;
        }
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            return false;
        }
        if (((tm.getDataState() == TelephonyManager.DATA_CONNECTED) || (tm
                .getDataState() == TelephonyManager.DATA_ACTIVITY_NONE))
                && info.isAvailable()) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * 判断网络是否可用
     */
    public static boolean isNetworkEnabled(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifiEnabled = false;
        // wifi
        NetworkInfo wifiNetworkInfo = conMan
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo != null) {
            State wifi = State.DISCONNECTED;
            wifi = wifiNetworkInfo.getState();
            if (wifi == State.CONNECTED || wifi == State.CONNECTING) {
                isWifiEnabled = true;
                return true;
            }
        }

        // mobile 3G Data Network
        NetworkInfo mobileNetworkInfo = conMan
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetworkInfo != null) {
            State mobile = State.DISCONNECTED;
            mobile = mobileNetworkInfo.getState();
            // 如果3G网络和wifi网络都未连接，且不是处于正在连接状态 则进入Network Setting界面 由用户配置网络连接
            if (mobile == State.CONNECTED || mobile == State.CONNECTING) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为Wifi网络
     */
    public static boolean isWifiNetwrok(Context mContext) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否有sdcard
     */
    public static boolean isSDcardExist() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    public static void killMyProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 等比例缩放图片（带滤波器）
     *
     * @param srcFile   来源文件
     * @param dstFile   目标文件
     * @param dstMaxWH  目标文件宽高最大值
     * @param bContrast 提高对比度滤波器，可使图片变亮丽
     */
    public static boolean scaleImageWithFilter(File srcFile, File dstFile,
                                               int dstMaxWH, Boolean bContrast) {
        boolean bRet = false;
        // 路径文件不存在
        if (!srcFile.exists()) {
            return bRet;
        }
        try {
            // 打开源文件
            Bitmap srcBitmap;
            {
                java.io.InputStream is;
                is = new FileInputStream(srcFile);
                Options opts = getOptionsWithInSampleSize(
                        srcFile.getPath(), dstMaxWH);
                srcBitmap = BitmapFactory.decodeStream(is, null, opts);
                if (srcBitmap == null) {
                    return bRet;
                }
            }
            // 原图片宽高
            int width = srcBitmap.getWidth();
            int height = srcBitmap.getHeight();
            // 获得缩放因子
            float scale = 1.f;
            {
                if ((width > dstMaxWH) || (height > dstMaxWH)) {
                    float scaleTemp = (float) dstMaxWH / (float) width;
                    float scaleTemp2 = (float) dstMaxWH / (float) height;
                    if (scaleTemp > scaleTemp2) {
                        scale = scaleTemp2;
                    } else {
                        scale = scaleTemp;
                    }
                }
            }
            // 图片缩放
            Bitmap dstBitmap;
            if (scale == 1.f) {
                dstBitmap = srcBitmap;
            } else {
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                dstBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, width, height,
                        matrix, true);
                if (!srcBitmap.isRecycled()) {
                    srcBitmap.recycle();
                }
                srcBitmap = null;
            }
            // 提高对比度
            if (bContrast) {
                Bitmap tempBitmap = Bitmap.createBitmap(dstBitmap.getWidth(),
                        dstBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(tempBitmap);
                ColorMatrix cm = new ColorMatrix();
                float contrast = 30.f / 180.f; // 提高30对比度
                setContrast(cm, contrast);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColorFilter(new ColorMatrixColorFilter(cm));
                canvas.drawBitmap(dstBitmap, 0, 0, paint);
                if (!dstBitmap.isRecycled()) {
                    dstBitmap.recycle();
                }
                dstBitmap = null;
                dstBitmap = tempBitmap;
            }
            // 保存文件
            if (dstFile.exists()) {
                dstFile.delete();
            }
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(dstFile));
            dstBitmap.compress(CompressFormat.JPEG, 90, bos);
            if (!dstBitmap.isRecycled()) {
                dstBitmap.recycle();
            }
            dstBitmap = null;
            bos.flush();
            bos.close();
            bRet = true;
        } catch (Exception e) {
            return bRet;
        }
        return bRet;
    }

    /**
     * 设置对比度矩阵
     */
    private static void setContrast(ColorMatrix cm, float contrast) {
        float scale = contrast + 1.f;
        float translate = ((-.5f * scale) + .5f) * 255.f;
        cm.set(new float[]{scale, 0, 0, 0, translate, 0, scale, 0, 0,
                translate, 0, 0, scale, 0, translate, 0, 0, 0, 1, 0});
    }

    /**
     * GPS是否可用
     */
    public static boolean isGpsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false;
        }
        return true;
    }

    /**
     * 把一个view转化成bitmap对象
     */
    public static Bitmap getBitmapByView(View view) {
        view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.buildDrawingCache();
        return view.getDrawingCache();
    }

    /**
     * 返回状态栏+标题栏高度
     */
    public static int getStatusTitleBarHeight(Activity activity) {
        Rect outRect = new Rect();
        activity.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getDrawingRect(outRect);
        return outRect.top;
    }

    // 获取手机状态栏高度
    public static int getStatusBarHeight(Context context) {
        Class<?> c;
        Object obj;
        Field field;
        int x;
        int statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return statusBarHeight;
    }

    // 获取ActionBar的高度
    public static int getActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        int actionBarHeight = 0;
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        }
        return actionBarHeight;
    }

    /**
     * 判断应用是在前台运行还是在后台运行
     */
    public static boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance != RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 判断应用是否是最新的task
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean isLatestTask(Context mContext) {
        ActivityManager mActivityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> appTasks = mActivityManager
                .getAppTasks();
        if (appTasks != null && !appTasks.isEmpty()) {
            if ((appTasks.get(0).getTaskInfo().baseIntent.getComponent()
                    .getPackageName()).equals(mContext.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public static PackageInfo getPackageInfo(Context context) {
        PackageInfo pkg = null;
        try {
            pkg = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
        } catch (NameNotFoundException exp) {
            exp.printStackTrace();
        }
        return pkg;
    }

    /**
     * 返回屏幕dpi
     */
    public static int getDisplayDensity(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        dm = context.getResources().getDisplayMetrics();
        return dm.densityDpi;
    }

    public static String getDisplayDensityName(Context context) {
        int density = getDisplayDensity(context);
        // 只取xhdpi&&xxhdpi两种分辨率,低于xh的提示手机分辨率不支持
        String drawableDir = "xhdpi";
        switch (density) {
            case DisplayMetrics.DENSITY_HIGH:
                drawableDir = "";
                break;
            case DisplayMetrics.DENSITY_LOW:
                drawableDir = "";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                drawableDir = "";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                drawableDir = "xhdpi";
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                drawableDir = "xxhdpi";
                break;
            case DisplayMetrics.DENSITY_TV:
                drawableDir = "xxhdpi";
                break;
            default:
                drawableDir = "xxhdpi";
                break;
        }
        return drawableDir;
    }

    /**
     * 按名称获取图片id
     *
     * @param context 上下文
     * @param name    图片资源名称
     * @return 图片资源id
     */
    public static int getDrawableId(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable",
                context.getPackageName());
    }

    /**
     * 从string.xml资源文件中获取字符串
     */
    public static String getString(Context context, int resId) {
        return context.getResources().getString(resId);
    }

    /**
     * 根据颜色资源ID获取ARGB颜色值
     *
     * @see {@link Resources#getColor(int, Resources.Theme)}
     */
    @SuppressWarnings("deprecation")
    public static int getColor(Context context, @ColorRes int colorId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getResources().getColor(colorId, null);
        } else {
            return context.getResources().getColor(colorId);
        }
    }

    /**
     * 计算两个经纬度间的距离
     *
     * @param lat_a a点纬度
     * @param lng_a a点经度
     * @param lat_b b点纬度
     * @param lng_b b点经度
     * @return 米
     */
    public static double getDistance(double lat_a, double lng_a, double lat_b, double lng_b) {
        final double EARTH_RADIUS = 6378137.0;

        // 角度转换为弧度
        double radLat1 = (lat_a * Math.PI / 180.0);
        double radLat2 = (lat_b * Math.PI / 180.0);
        // 弧度差
        double dRadLat = radLat1 - radLat2;
        double dRadLng = (lng_a - lng_b) * Math.PI / 180.0;

        double distance = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(dRadLat / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(dRadLng / 2), 2)));
        distance = distance * EARTH_RADIUS;
        distance = Math.round(distance * 10000) / 10000;
        return distance;
    }

    /**
     * Set the enabled state of this view.
     *
     * @param visibility One of {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     */
    public static void setViewVisibility(View view, int visibility) {
        if (view == null) {
            return;
        }
        if (view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }

    /**
     * Set the enabled state of this view. The interpretation of the enabled
     * state varies by subclass.
     *
     * @param enabled True if this view is enabled, false otherwise.
     */
    public static void setViewEnabled(View view, boolean enabled) {
        if (view == null) {
            return;
        }
        if (view.isEnabled() != enabled) {
            view.setEnabled(enabled);
        }
    }

    /**
     * <p>Changes the checked state of this button.</p>
     *
     * @param checked true to check the button, false to uncheck it
     */
    public static void setCheckBoxChecked(CheckBox view, boolean checked) {
        if (view == null) {
            return;
        }

        if (view.isChecked() != checked) {
            view.setChecked(checked);
        }
    }

    /**
     * 获取缓存地址
     *
     * @param context 上下文
     * @return 如果有外置SD卡就返回SD上的缓存地址/sdcard/Android/data/<application package>/cache，
     * 否则返回/data/data/<application package>/cache目录地址
     */
    public static String getDiskCacheDir(Context context) {
        String cachePath = "";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            File cacheFile = context.getExternalCacheDir();
            if (cacheFile != null && cacheFile.exists()) {
                cachePath = cacheFile.getPath();
            }
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath;
    }

    /**
     * 把float变成两位小数
     */
    public static float changToTwoDecimal(float in) {
        DecimalFormat df = new DecimalFormat("0.00");
        String out = df.format(in);
        return Float.parseFloat(out);
    }

    /**
     * 获取进程号对应的进程名
     *
     * @param pid 进程号
     * @return 进程名
     */
    private static String getProcessName(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }
}
