package com.tungsten.hmclpe.manifest;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class LauncherDirs {

    private static final String TAG = "LauncherDirs";

    public static void ensure(Context ctx) {
        try {
            File appCache = ctx.getCacheDir();
            File crashDir = new File(appCache, "crash_logs");
            if (!crashDir.exists()) {
                crashDir.mkdirs();
            }
            File downloadDir = new File(appCache, "download");
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            File aiCache = new File(appCache, "ai_cache");
            if (!aiCache.exists()) {
                aiCache.mkdirs();
            }
            File aiConfig = new File(appCache, "ai_config");
            if (!aiConfig.exists()) {
                aiConfig.mkdirs();
            }
        } catch (Throwable t) {
            Log.e(TAG, "ensure failed", t);
        }
    }

    public static File cache(Context ctx) {
        return ctx.getCacheDir();
    }

    public static File crashDir(Context ctx) {
        return new File(ctx.getCacheDir(), "crash_logs");
    }

    public static File aiCache(Context ctx) {
        return new File(ctx.getCacheDir(), "ai_cache");
    }

    public static File aiConfig(Context ctx) {
        return new File(ctx.getCacheDir(), "ai_config");
    }
}
