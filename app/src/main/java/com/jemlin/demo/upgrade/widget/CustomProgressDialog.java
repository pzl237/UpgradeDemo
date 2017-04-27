package com.jemlin.demo.upgrade.widget;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.widget.TextView;

import com.jemlin.demo.upgrade.R;


public class CustomProgressDialog extends ProgressDialog {

    private String mTipStr;

    private CustomProgressDialog(Context context) {
        super(context);
    }

    private CustomProgressDialog(Context context, String tipStr) {
        this(context);
        this.mTipStr = tipStr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_progress_dialog);

        //窗口设置透明背景
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView mTipTV = (TextView) findViewById(R.id.tipTV);
        if (!TextUtils.isEmpty(mTipStr)) {
            mTipTV.setText(mTipStr);
        }
    }

    public static CustomProgressDialog show(Context context, String tipStr) {
        return show(context, tipStr, false);
    }

    public static CustomProgressDialog show(Context context, String tipStr, boolean isCanceledOnTouchOutside) {
        CustomProgressDialog dialog = new CustomProgressDialog(context, tipStr);
        dialog.setCanceledOnTouchOutside(isCanceledOnTouchOutside);
        dialog.show();
        return dialog;
    }

    public static void dismiss(CustomProgressDialog dialog) {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }
        dialog.dismiss();
    }
}
