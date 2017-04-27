package com.jemlin.demo.upgrade.helper;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jemlin.demo.upgrade.R;

import java.lang.ref.SoftReference;

/**
 * 代替原生Toast使用，目的避免多个Toast叠在一起显示。
 */
public class ToastHelper {

    private static ToastImpl toastImpl;

    /**
     * 初始化
     */
    public static void init(Application app) {
        toastImpl = new ToastImpl(app);
    }

    /**
     * 销毁
     */
    public static void cancel() {
        assertInit();
        toastImpl.cancel();
    }

    private static void assertInit() {
        if (toastImpl == null) {
            throw new IllegalStateException("ToastHelper need be init first..");
        }
    }

    public static void showToast(CharSequence message) {
        assertInit();
        toastImpl.showToast(message, Toast.LENGTH_SHORT);
    }

    public static void showToast(CharSequence message, int duration) {
        assertInit();
        toastImpl.showToast(message, duration);
    }

    public static void showToast(CharSequence message, int gravity, int xofsset,
                                 int yoffset) {
        assertInit();
        toastImpl.showToast(message, Toast.LENGTH_SHORT, gravity, xofsset,
                yoffset, 0);
    }

    public static void showLongToast(CharSequence message) {
        assertInit();
        toastImpl.showToast(message, Toast.LENGTH_LONG);
    }

    public static void showLongToast(CharSequence message, int gravity, int xofsset,
                                     int yoffset) {
        assertInit();
        toastImpl.showToast(message, Toast.LENGTH_LONG, gravity, xofsset,
                yoffset, 0);
    }

    public static void showLongToast(CharSequence message, int gravity, int xofsset,
                                     int yoffset, int dpTextSize) {
        assertInit();
        toastImpl.showToast(message, Toast.LENGTH_LONG, gravity, xofsset,
                yoffset, dpTextSize);
    }

    //
    private static class ToastImpl {

        private Application app;
        private LayoutInflater inflater;
        private SoftReference<Toast> softRefToast;
        private static Handler handler = new Handler(Looper.getMainLooper());

        ToastImpl(Application app) {
            this.app = app;
            inflater = LayoutInflater.from(app);
        }

        void cancel() {
            if (softRefToast != null && softRefToast.get() != null) {
                try {
                    softRefToast.get().cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        void showToast(CharSequence message, int duration) {
            showToast(message, duration, null, null, 0);
        }

        void showToast(CharSequence message, int duration, int gravity,
                       int xofsset, int yoffset, int dpTextSize) {
            showToast(message, duration, new GravityBean(gravity, xofsset,
                    yoffset), null, dpTextSize);
        }

        @SuppressWarnings("unused")
        void showToast(CharSequence message, int duration,
                       float horizontalMargin, float verticalMargin) {
            showToast(message, duration, null, new MarginBean(horizontalMargin,
                    verticalMargin), 0);
        }

        private void showToast(final CharSequence message, final int duration,
                               final GravityBean gravity, final MarginBean margin, final int dpTextSize) {
            handler.post(new Runnable() {
                public void run() {
                    Toast toast = new Toast(app);
                    toast.setDuration(duration);
                    if (gravity != null) {
                        toast.setGravity(gravity.gravity, gravity.xoffset,
                                gravity.yoffset);
                    }
                    if (margin != null) {
                        toast.setMargin(margin.horizontalMargin,
                                margin.verticalMargin);
                    }
                    View content = inflater.inflate(R.layout.common_toast, null);
                    TextView textView = (TextView) content.findViewById(R.id.tvToast);
                    toast.setView(content);
                    textView.setText(message);
                    if (dpTextSize > 0) {
                        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, dpTextSize);
                    }
                    synchronized (ToastImpl.this) {
                        cancel();
                        softRefToast = new SoftReference<Toast>(toast);
                        toast.show();
                    }
                }
            });
        }

        private class GravityBean {

            int gravity;
            int xoffset;
            int yoffset;

            public GravityBean(int g, int xo, int yo) {
                gravity = g;
                xoffset = xo;
                yoffset = yo;
            }
        }

        private class MarginBean {

            float horizontalMargin;
            float verticalMargin;

            public MarginBean(float hm, float vm) {
                horizontalMargin = hm;
                verticalMargin = vm;
            }
        }

    }
}
