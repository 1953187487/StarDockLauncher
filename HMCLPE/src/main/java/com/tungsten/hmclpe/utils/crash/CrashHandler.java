package com.tungsten.hmclpe.utils.crash;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.manifest.AppManifest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "StarDockCrash";
    private static final String CRASH_DIR = "crash_logs";
    private static final String PENDING_FLAG = "crash_pending.flag";
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Context appContext;

    public CrashHandler(Context context) {
        this.appContext = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void init(Context context) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
    }

    public static boolean hasPendingCrash(Context context) {
        try {
            File flag = new File(AppManifest.DEFAULT_CACHE_DIR, PENDING_FLAG);
            return flag.exists();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void clearPendingFlag(Context context) {
        try {
            File flag = new File(AppManifest.DEFAULT_CACHE_DIR, PENDING_FLAG);
            if (flag.exists()) flag.delete();
        } catch (Throwable t) {
        }
    }

    public static File getLatestCrashLog(Context context) {
        try {
            File dir = new File(AppManifest.DEFAULT_CACHE_DIR, CRASH_DIR);
            if (!dir.exists()) return null;
            File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
            if (files == null || files.length == 0) return null;
            File latest = files[0];
            for (File f : files) {
                if (f.lastModified() > latest.lastModified()) latest = f;
            }
            return latest;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            String stack = sw.toString();

            Log.e(TAG, "捕获到未捕获异常：" + e.getMessage(), e);

            if (AppManifest.DEFAULT_CACHE_DIR != null) {
                File crashDir = new File(AppManifest.DEFAULT_CACHE_DIR, CRASH_DIR);
                if (!crashDir.exists()) crashDir.mkdirs();

                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File crashFile = new File(crashDir, "crash_" + ts + ".log");
                try (FileOutputStream fos = new FileOutputStream(crashFile)) {
                    fos.write(("Time: " + new Date().toString() + "\n").getBytes());
                    fos.write(("Thread: " + t.getName() + "\n").getBytes());
                    fos.write(("AppContext: " + appContext.getPackageName() + "\n").getBytes());
                    try {
                        String rd = AppManifest.DEFAULT_RUNTIME_DIR;
                        fos.write(("AppRuntimeExists: " + (rd != null && new File(rd + "/version").exists()) + "\n").getBytes());
                        String jd = AppManifest.JAVA_DIR;
                        fos.write(("Java8Exists: " + (jd != null && new File(jd, "default/bin/java").exists()) + "\n").getBytes());
                        fos.write(("Java17Exists: " + (jd != null && new File(jd, "JRE17/bin/java").exists()) + "\n").getBytes());
                        String bd = AppManifest.BOAT_LIB_DIR;
                        fos.write(("BoatLibExists: " + (bd != null && new File(bd).exists()) + "\n").getBytes());
                    } catch (Throwable ignored) {}
                    fos.write("\n".getBytes());
                    fos.write(stack.getBytes());
                    fos.flush();
                }

                File flag = new File(AppManifest.DEFAULT_CACHE_DIR, PENDING_FLAG);
                try (FileOutputStream flagFos = new FileOutputStream(flag)) {
                    flagFos.write(crashFile.getAbsolutePath().getBytes());
                    flagFos.close();
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "写入崩溃日志失败", ex);
        }

        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(t, e);
            } else {
                System.exit(1);
            }
        }, 500);
    }
}
