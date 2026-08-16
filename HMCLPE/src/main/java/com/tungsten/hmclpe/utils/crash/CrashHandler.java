package com.tungsten.hmclpe.utils.crash;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "SDCrash";
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat FILE_FMT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    private static volatile boolean installed;
    private static Context appCtx;

    public static synchronized void install(Context ctx) {
        if (installed) {
            return;
        }
        appCtx = ctx.getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
        installed = true;
        Log.i(TAG, "CrashHandler installed");
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            writeLog(t, e);
        } catch (Throwable inner) {
            Log.e(TAG, "writeLog failed", inner);
        }
        try {
            Thread.UncaughtExceptionHandler def = Thread.getDefaultUncaughtExceptionHandler();
            if (def != null && def != this) {
                def.uncaughtException(t, e);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void writeLog(Thread t, Throwable e) throws java.io.IOException {
        if (appCtx == null) {
            return;
        }
        File dir = new File(appCtx.getCacheDir(), "crash_logs");
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        String name = "crash_" + FILE_FMT.format(new Date()) + ".log";
        File f = new File(dir, name);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        try (FileWriter fw = new FileWriter(f)) {
            fw.write("Time: " + FMT.format(new Date()) + "\n");
            fw.write("Thread: " + t.getName() + "\n");
            fw.write("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n");
            fw.write("SDK: " + Build.VERSION.SDK_INT + "\n");
            fw.write("\n");
            fw.write(sw.toString());
            fw.write("\n");
        }
        Log.e(TAG, "crash saved: " + f.getAbsolutePath());
    }
}
