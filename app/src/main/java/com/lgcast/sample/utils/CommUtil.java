package com.lgcast.sample.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class CommUtil {
    public interface DialogClickListener {
        void onClick(int index);
    }

    public static void showDialog(Context context, boolean cancelable, String title, String message, DialogClickListener onClickOk, DialogClickListener onClickCancel) {
        runOnMainLooper(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(cancelable);
            if (title != null) builder.setTitle(title);
            if (message != null) builder.setMessage(message);
            if (onClickOk != null) builder.setPositiveButton(android.R.string.ok, (dialog, index) -> onClickOk.onClick(index));
            if (onClickCancel != null) builder.setNegativeButton(android.R.string.cancel, (dialog, index) -> onClickCancel.onClick(index));
            builder.show();
        });
    }

    public static boolean isMainThread() {
        String thread = Thread.currentThread().getName();
        return "main".equals(thread);
    }

    public static void runInBackground(Runnable r) {
        if (r != null) {
            Thread thread = new Thread(r);
            thread.start();
        }
    }

    public static void runOnMainLooper(Runnable r) {
        if (r != null) {
            (new Handler(Looper.getMainLooper())).post(r);
        }
    }
}
