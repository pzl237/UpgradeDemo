package com.jemlin.demo.upgrade.helper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 获取手机网络相关的工具类
 */
public class NetworkHelper {
    private static final String NETWORK_WIFI = "wifi";
    private static final String NETWORK_MOBILE = "mobile";

    /**
     * Unknown network class.
     */
    public static final int NETWORK_CLASS_UNKNOWN = 0;
    /**
     * Class of broadly defined "2G" networks.
     */
    public static final int NETWORK_CLASS_2_G = 1;
    /**
     * Class of broadly defined "3G" networks.
     */
    public static final int NETWORK_CLASS_3_G = 2;
    /**
     * Class of broadly defined "4G" networks.
     */
    public static final int NETWORK_CLASS_4_G = 3;

    /**
     * Class of broadly defined "WIFI" networks.
     */
    public static final int NETWORK_CLASS_WIFI = 4;


    /**
     * 获取运营商名称（有sim卡）
     *
     * @param context
     * @return
     */
    public String getOperatorName(Context context) {
        if (null == context) {
            return "";
        }
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyManager && telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
            return telephonyManager.getNetworkOperator();// String
        } else {
            return "";
        }
    }

    /**
     * 获取网络连接类型
     */
    public static int getNetWorkType(Context context) {
        if (null == context) {
            return NETWORK_CLASS_UNKNOWN;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null != connectivityManager) {
            NetworkInfo activeNetInfo;
            try {
                activeNetInfo = connectivityManager.getActiveNetworkInfo();
            } catch (Exception e) {
                return NETWORK_CLASS_UNKNOWN;
            }
            if (activeNetInfo == null)
                return NETWORK_CLASS_UNKNOWN;
            if (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return NETWORK_CLASS_WIFI;
            } else if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return getMobileNetType(context, activeNetInfo);
            }
        }
        return NETWORK_CLASS_UNKNOWN;
    }

    /**
     * 获取网络连接状态
     *
     * @param context 上下文
     * @return true已连接，false未连接
     */
    @SuppressWarnings("deprecation")
    public static boolean isNetworkConnected(Context context) {
        boolean isConnected = false;
        if (null == context) {
            return false;
        }
        try {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (null != connMgr) {
                if (Build.VERSION.SDK_INT >= 21) {
                    NetworkInfo info;
                    Network[] networks = connMgr.getAllNetworks();
                    if (networks != null && networks.length > 0) {
                        for (int i = 0; i < networks.length; i++) {
                            info = connMgr.getNetworkInfo(networks[i]);
                            if (info != null && info.isConnected()) {
                                isConnected = true;
                                break;
                            }
                        }
                    }
                } else {
                    NetworkInfo info = connMgr
                            .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    NetworkInfo infoM = connMgr
                            .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                    if (info != null && info.isConnected()) {
                        isConnected = true;
                    }
                    if (infoM != null && infoM.isConnected()) {
                        isConnected = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isConnected;
    }

    /**
     * Return general class of network type, such as "3G" or "4G". In cases
     * where classification is contentious, this method is conservative.
     *
     * @param context
     */
    public static int getMobileNetType(Context context, NetworkInfo networkInfo) {
        if (null == context) {
            return NETWORK_CLASS_UNKNOWN;
        }
        TelephonyManager telephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (null != telephonyMgr) {
            switch (telephonyMgr.getNetworkType()) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return NETWORK_CLASS_2_G;
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return NETWORK_CLASS_3_G;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return NETWORK_CLASS_4_G;
                default:
                    if (networkInfo != null && networkInfo.isConnected()) {
                        String _strSubTypeName = networkInfo.getSubtypeName();
                        if (_strSubTypeName.equalsIgnoreCase("TD-SCDMA") || _strSubTypeName.equalsIgnoreCase("WCDMA") || _strSubTypeName.equalsIgnoreCase("CDMA2000")) {
                            return NETWORK_CLASS_3_G;
                        }
                    }
                    return NETWORK_CLASS_UNKNOWN;
            }
        }
        return NETWORK_CLASS_UNKNOWN;
    }

    /**
     * 0:无连接,1:wifi,2:2G,3:3G,4:其他
     */
    public static int getCheckNetWork(Context context) {
        if (null == context) {
            return 0;
        }

        ConnectivityManager mConnectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager mTelephony = (TelephonyManager) context
                .getSystemService(context.TELEPHONY_SERVICE); // 检查网络连接，如果无网络可用，就不需要进行连网操作等
        if (null == mConnectivity || null == mTelephony) {
            return 0;
        }

        NetworkInfo info = null;
        try {
            info = mConnectivity.getActiveNetworkInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (info == null) {
            return 0;
        }

        try {
            // 判断网络连接类型，只有在2G/3G/wifi里进行一些数据更新。
            int netType = info.getType();
            int netSubtype = info.getSubtype();
            if (netType == ConnectivityManager.TYPE_WIFI) {
                return 1;
            } else if (netType == ConnectivityManager.TYPE_MOBILE
                    && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
                    && !mTelephony.isNetworkRoaming()) {
                return 3;
            } else if (netSubtype == TelephonyManager.NETWORK_TYPE_GPRS
                    || netSubtype == TelephonyManager.NETWORK_TYPE_CDMA
                    || netSubtype == TelephonyManager.NETWORK_TYPE_EDGE) {
                return 2;
            } else {
                return 4;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return 4;
    }

    private static Pattern httpPattern = Pattern.compile("(http|https):\\/\\/([\\w.]+\\/?)\\S*");

    public static boolean isHttpProtocol(String url) {
        Matcher matcher = httpPattern.matcher(url);
        return matcher.matches();
    }


    private static SSLSocketFactory sslSocketFactory;

    public static void trustAllHttpsURLConnection() {
        // Create a trust manager that does not validate certificate chains
        if (sslSocketFactory == null) {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs,
                                               String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs,
                                               String authType) {
                }
            }};
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, null);
                sslSocketFactory = sslContext.getSocketFactory();
            } catch (Throwable e) {

            }
        }

        if (sslSocketFactory != null) {
            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
            HttpsURLConnection
                    .setDefaultHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        }
    }
}
