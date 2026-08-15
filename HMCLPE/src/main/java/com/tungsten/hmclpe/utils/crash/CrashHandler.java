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
        File flag = new File(AppManifest.DEFAULT_CACHE_DIR, PENDING_FLAG);
        boolean exists = flag.exists();
        return exists;
    }

    public static void clearPendingFlag(Context context) {
        File flag = new File(AppManifest.DEFAULT_CACHE_DIR, PENDING_FLAG);
        if (flag.exists()) flag.delete();
    }

    public static File getLatestCrashLog(Context context) {
        File dir = new File(AppManifest.DEFAULT_CACHE_DIR, CRASH_DIR);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
        if (files == null || files.length == 0) return null;
        File latest = files[0];
        for (File f : files) {
            if (f.lastModified() > latest.lastModified()) latest = f;
        }
        return latest;
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

            File crashDir = new File(AppManifest.DEFAULT_CACHE_DIR, CRASH_DIR);
            if (!crashDir.exists()) crashDir.mkdirs();

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File crashFile = new File(crashDir, "crash_" + ts + ".log");
            FileOutputStream fos = new FileOutputStream(crashFile);
            fos.write(("Time: " + new Date().toString() + "\n").getBytes());
            fos.write(("Thread: " + t.getName() + "\n").getBytes());
            fos.write(("AppContext: " + appContext.getPackageName() + "\n").getBytes());
            fos.write(("AppRuntimeExists: " + new File(AppManifest.DEFAULT_RUNTIME_DIR + "/version").exists() + "\n").getBytes());
            fos.write(("Java8Exists: " + new File(AppManifest.JAVA_DIR + "/default/bin/java").exists() + "\n").getBytes());
            fos.write(("Java17Exists: " + new File(AppManifest.JAVA_DIR + "/JRE17/bin/java").exists() + "\n").getBytes());
            fos.write(("BoatLibExists: " + new File(AppManifest.BOAT_LIB_DIR).exists() + "\n").getBytes());
            fos.write("\n".getBytes());
            fos.write(stack.getBytes());
            fos.flush();
            fos.close();

            File flag = new File(AppManifest.DEFAULT_CACHE_DIR, PENDING_FLAG);
            FileOutputStream flagFos = new FileOutputStream(flag);
            flagFos.write(crashFile.getAbsolutePath().getBytes());
            flagFos.close();
        } catch (Exception ex) {
            Log.e(TAG, "写入崩溃日志失败", ex);
        }

        try {
            Intent intent = new Intent(appContext, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("auto_show_crash", true);
            appContext.startActivity(intent);
        } catch (Exception ex) {
            Log.e(TAG, "重启主界面失败", ex);
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
