package com.jemlin.demo.upgrade.services;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public final class ApiCommonRequest {

    /**
     * 检查版本更新
     */
    public static void upgrade(Context context, String versionCode, HttpManager.HttpServiceRequestCallBack callBack) {
        JSONObject params = new JSONObject();
        try {
            params.put("version", versionCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        HttpManager.getInstance().request(
                context,
                "/common/upgrade",
                HttpManager.HttpMethod.HTTP_METHOD_GET,
                params,
                callBack);
    }
}
