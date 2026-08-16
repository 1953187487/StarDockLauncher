package com.tungsten.hmclpe.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.manifest.LauncherDirs;
import com.tungsten.hmclpe.utils.crash.CrashHandler;

public class SDApplication extends Application {

    private static SDApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        try {
            LauncherDirs.ensure(getApplicationContext());
        } catch (Throwable t) {
            android.util.Log.e("SDApplication", "ensure dirs failed", t);
        }
        try {
            CrashHandler.install(getApplicationContext());
        } catch (Throwable t) {
            android.util.Log.e("SDApplication", "crash install failed", t);
        }
        AppManifest.init(getApplicationContext());
        android.util.Log.i("SDApplication", "StarDock v1.1.0 boot done sdk=" + Build.VERSION.SDK_INT);
    }

    public static Context getContext() {
        return instance == null ? null : instance.getApplicationContext();
    }

    public static SDApplication get() {
        return instance;
    }
}
