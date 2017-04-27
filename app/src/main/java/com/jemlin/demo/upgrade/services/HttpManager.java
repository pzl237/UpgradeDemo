package com.jemlin.demo.upgrade.services;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.jemlin.demo.upgrade.BuildConfig;
import com.jemlin.demo.upgrade.config.Constants;
import com.jemlin.demo.upgrade.helper.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class HttpManager {
    private static String TAG = HttpManager.class.getSimpleName();

    private final static int DEFAULT_ERR_CODE = -9999;

    private final static String HEADER_REQUEST_ID = "Upgrade-Request-Id";

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public enum HttpMethod {
        HTTP_METHOD_GET(0, "GET"),
        HTTP_METHOD_POST(1, "POST");

        private int type;
        private String name;

        HttpMethod(int type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    public enum Server {
        DEBUG, RELEASE
    }

    private static HttpManager instance;
    private static OkHttpClient client;
    private Map<String, Object> requestMaps = new HashMap<>();

    public interface HttpServiceRequestCallBack {
        /**
         * 请求成功回调
         *
         * @param originalResponse 服务器返回的原始数据
         * @param response         服务器返回的数据中data字段的数据
         */
        void onSuccess(Object originalResponse, JSONObject response);

        /**
         * 请求失败的回调
         *
         * @param errCode  错误码
         * @param errMsg   错误消息
         * @param response 服务器返回错误数据的封装
         *                 {
         *                 "success": true, //是否成功
         *                 "errCode": 111, //错误码
         *                 "errMsg": "detail message" //错误信息
         *                 }
         */
        void onFailure(int errCode, String errMsg, @Nullable JSONObject response);
    }

    private HttpManager() {
        if (null != client) {
            return;
        }

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext;
        final SSLSocketFactory sslSocketFactory;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            sslSocketFactory = sslContext.getSocketFactory();

            client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .sslSocketFactory(sslSocketFactory)
                    .build();

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public static synchronized HttpManager getInstance() {
        if (null == instance) {
            instance = new HttpManager();
        }
        return instance;
    }

    private String getBaseUrl(Server server) {
        switch (server) {
            case DEBUG:
                return Constants.App.API_DOMAIN_DEBUG;

            case RELEASE:
                return Constants.App.API_DOMAIN_RELEASE;

            default:
                return Constants.App.API_DOMAIN_RELEASE;
        }
    }

    public void request(Context context, String uri, HttpMethod method, JSONObject params, HttpServiceRequestCallBack callback) {
        String requestFullPath;
        if (BuildConfig.DEBUG) {
            requestFullPath = getBaseUrl(Server.DEBUG);
        } else {
            requestFullPath = getBaseUrl(Server.RELEASE);
        }
        requestFullPath = requestFullPath + uri;

        long now = new Date().getTime();
        String requestId = String.format(Locale.getDefault(), "%d%d", now, new Random().nextInt(1000));

        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_REQUEST_ID, requestId);
        //其他需要的header可以在这里继续添加

        // request body
        RequestBody requestBody;
        Request.Builder requestBuilder = new Request.Builder()
                .headers(Headers.of(headers));

        if (method == HttpMethod.HTTP_METHOD_GET) {
            if (params == null) {
                requestBuilder.url(requestFullPath);
            } else {
                Uri.Builder uriBuilder = Uri.parse(requestFullPath).buildUpon();
                Iterator keys = params.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    Object value = params.opt(key);
                    uriBuilder.appendQueryParameter(key, String.valueOf(value));
                }
                requestBuilder.url(uriBuilder.build().toString());
            }
        } else {
            if (params != null) {
                requestBody = RequestBody.create(JSON, params.toString());
            } else {
                requestBody = RequestBody.create(JSON, "");
            }
            requestBuilder.method(method.name, requestBody);
        }

        if (method != HttpMethod.HTTP_METHOD_GET) {
            requestBuilder.url(requestFullPath);
        }

        Call call = client.newCall(requestBuilder.build());
        call.enqueue(new ICJsonHttpResponseHandler());

        requestMaps.put(requestId, new RequestModel(context, call, callback));
    }

    private void handleSuccess(int statusCode, String requestId, JSONObject response, final HttpServiceRequestCallBack callback) {
        if (null != response) {
            Timber.d(TAG, response.toString());
        } else {
            callback.onFailure(DEFAULT_ERR_CODE, "response nothing", null);
            removeRequest(requestId);
            return;
        }

        JSONObject data;
        if (response.optBoolean("success", false)) {
            // 服务器返回的数据
            data = response.optJSONObject("data");
            callback.onSuccess(response, data);
        } else {
            callback.onFailure(response.optInt("errCode", DEFAULT_ERR_CODE),
                    response.optString("errMsg", ""),
                    response);
        }

        removeRequest(requestId);
    }

    private void handleFailure(WeakReference<Context> fromContext, int statusCode, String requestId, String responseString, HttpServiceRequestCallBack callback) {
        try {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("errCode", statusCode);

            Context context = null;
            if (fromContext != null) {
                context = fromContext.get();
            }
            if (context != null && !NetworkHelper.isNetworkConnected(context)) {
                responseString = "网络请求失败，请检查网络！";
            }
            if (null == responseString) {
                // 这里不使用mContext对象来获取字符串，避免内存泄漏
                responseString = "网络请求失败，请检查网络！";
            }
            result.put("errMsg", responseString);
            callback.onFailure(statusCode, responseString, result);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onFailure(DEFAULT_ERR_CODE, e.getMessage(), null);
        }
        removeRequest(requestId);
    }

    private class ICJsonHttpResponseHandler implements Callback {

        @Override
        public void onFailure(Call call, final IOException e) {
            String requestId = null;
            Headers requestHeaders = call.request().headers();
            for (int i = 0; i < requestHeaders.size(); i++) {
                if (requestHeaders.name(i).equals(HEADER_REQUEST_ID)) {
                    requestId = requestHeaders.value(i);
                    break;
                }
            }

            if (requestId == null) {
                return;
            }

            final RequestModel requestModel = (RequestModel) requestMaps.get(requestId);
            if (requestModel == null) {
                return;
            }

            final HttpServiceRequestCallBack callback = requestModel.callback;
            if (callback == null) {
                return;
            }

            final String finalRequestId = requestId;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    instance.handleFailure(requestModel.fromContext, 500, finalRequestId, e.getMessage(), callback);
                }
            });
        }

        @Override
        public void onResponse(Call call, final Response response) throws IOException {
            String rawJsonData = response.body().string();
            JSONObject responseJsonObject = null;
            if ("\n".equals(rawJsonData) || "null".equals(rawJsonData)) {
                responseJsonObject = null;
            } else {
                try {
                    responseJsonObject = new JSONObject(rawJsonData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            String requestId = null;
            Headers requestHeaders = call.request().headers();
            for (int i = 0; i < requestHeaders.size(); i++) {
                if (requestHeaders.name(i).equals(HEADER_REQUEST_ID)) {
                    requestId = requestHeaders.value(i);
                    break;
                }
            }

            if (requestId == null) {
                return;
            }

            RequestModel requestModel = (RequestModel) requestMaps.get(requestId);
            if (requestModel == null) {
                return;
            }

            final HttpServiceRequestCallBack callback = requestModel.callback;
            if (callback == null) {
                return;
            }

            final String finalRequestId = requestId;
            final JSONObject finalResponseJsonObject = responseJsonObject;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    instance.handleSuccess(response.code(), finalRequestId, finalResponseJsonObject, callback);
                }
            });
        }
    }

    /**
     * 用来管理历史请求队列，当请求完成后，回收context，避免内存泄漏
     */
    private class RequestModel {
        Call call;
        HttpServiceRequestCallBack callback;
        WeakReference<Context> fromContext;

        private RequestModel(Context fromContext, Call call, HttpServiceRequestCallBack callback) {
            this.fromContext = new WeakReference<>(fromContext);
            this.call = call;
            this.callback = callback;
        }
    }

    private void removeRequest(String requestId) {
        RequestModel currentRequestModel = (RequestModel) requestMaps.get(requestId);
        if (currentRequestModel == null) {
            return;
        }

        WeakReference<Context> fromContext = currentRequestModel.fromContext;
        if (fromContext == null) {
            requestMaps.remove(requestId);
            return;
        }

        // 查询该requestId所在的context有几个请求
        int contextRequestCount = 0;
        for (String key : requestMaps.keySet()) {
            RequestModel item = (RequestModel) requestMaps.get(key);
            if (item.fromContext.equals(fromContext)) {
                contextRequestCount++;
            }
        }

        if (contextRequestCount == 1) {
            // 只有当前这个请求
            currentRequestModel.fromContext = null;
        }

        requestMaps.remove(requestId);
    }

    // 清除掉一个context下的所有请求
    public void cancelRequest(Context context) {
        List<String> deleting = new ArrayList<>();
        for (String key : requestMaps.keySet()) {
            RequestModel requestModel = (RequestModel) requestMaps.get(key);
            if (requestModel.fromContext != null
                    && requestModel.fromContext.get() != null
                    && requestModel.fromContext.get().equals(context)) {
                deleting.add(key);
            }
        }

        for (String key : deleting) {
            RequestModel requestModel = (RequestModel) requestMaps.get(key);
            requestModel.fromContext = null;
            requestModel.callback = null;
            requestModel.call.cancel();

            requestMaps.remove(key);
        }
    }
}
