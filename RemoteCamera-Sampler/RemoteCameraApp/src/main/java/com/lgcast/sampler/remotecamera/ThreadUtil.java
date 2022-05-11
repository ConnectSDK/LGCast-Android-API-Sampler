package com.lgcast.sampler.remotecamera;

import android.os.Handler;
import android.os.Looper;

public class ThreadUtil {
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
