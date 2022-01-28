package com.lgcast.sample.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

public class SimpleProgress {
    private final ProgressDialog mDialog;

    public SimpleProgress(Context context) {
        mDialog = new ProgressDialog(context);
        mDialog.setProgressStyle(0);
        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        if (context instanceof Activity) mDialog.setOwnerActivity((Activity) context);
    }

    public SimpleProgress(Context context, String message, boolean cancelable) {
        this(context);
        mDialog.setMessage(message);
        mDialog.setCancelable(cancelable);
    }

    public void show() {
        if (CommUtil.isMainThread()) mDialog.show();
        else CommUtil.runOnMainLooper(mDialog::show);
    }

    public void dismiss() {
        if (CommUtil.isMainThread()) mDialog.dismiss();
        else CommUtil.runOnMainLooper(mDialog::dismiss);
    }
}
