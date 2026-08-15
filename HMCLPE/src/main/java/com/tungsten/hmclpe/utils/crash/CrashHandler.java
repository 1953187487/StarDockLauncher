package com.tungsten.hmclpe.utils.crash;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Context context;

    public CrashHandler(Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void init(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context.getApplicationContext()));
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e("CrashHandler", "启动崩溃捕获：" + e.getMessage(), e);
        // 尝试重启主界面
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch (Exception ex) {
            Log.e("CrashHandler", "重启失败", ex);
        }
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        }
    }
}
