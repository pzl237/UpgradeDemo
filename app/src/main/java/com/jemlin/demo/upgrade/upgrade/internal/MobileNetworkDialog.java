package com.jemlin.demo.upgrade.upgrade.internal;

import android.content.Context;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

/**
 * 流量提醒对话框
 */
public class MobileNetworkDialog {

    private final Context context;
    private final MobileNetworkDialogListener listener;

    public MobileNetworkDialog(Context context,
                               MobileNetworkDialogListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show(String content) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        final MaterialDialog dialog = builder.title("流量提醒")
                .theme(Theme.LIGHT)
                .titleGravity(GravityEnum.CENTER)
                .content(content)
                .positiveText("确定")
                .negativeText("取消")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                        listener.sure();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .build();

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
