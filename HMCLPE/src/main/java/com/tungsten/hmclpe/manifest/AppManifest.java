package com.tungsten.hmclpe.manifest;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class AppManifest {

    public static String PUBLIC_DIR;
    public static String RUNTIME_DIR;
    public static String CACHE_DIR;
    public static String PLUGIN_DIR;
    public static String IMG_DIR;
    public static String CONTROL_DIR;
    public static String GAME_DIR;
    public static String CRASH_DIR;
    public static String DOWNLOAD_DIR;
    public static String VERSION_DIR;
    public static String ACCOUNT_DIR;
    public static String CONFIG_DIR;

    public static void init(Context ctx) {
        try {
            File pub = Environment.getExternalStorageDirectory();
            PUBLIC_DIR = pub == null ? "/sdcard" : pub.getAbsolutePath();
        } catch (Throwable t) {
            PUBLIC_DIR = "/sdcard";
        }
        RUNTIME_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/runtime";
        CACHE_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/cache";
        PLUGIN_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/runtime/plugin";
        IMG_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/runtime/img";
        CONTROL_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/runtime/control";
        GAME_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/game";
        DOWNLOAD_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/download";
        VERSION_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/version";
        ACCOUNT_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/account";
        CONFIG_DIR = PUBLIC_DIR + "/Android/data/com.stardock.launcher/files/config";
        CRASH_DIR = ctx == null ? CACHE_DIR : ctx.getCacheDir().getAbsolutePath() + "/crash_logs";
        new java.io.File(RUNTIME_DIR).mkdirs();
        new java.io.File(CACHE_DIR).mkdirs();
        new java.io.File(GAME_DIR).mkdirs();
        new java.io.File(DOWNLOAD_DIR).mkdirs();
        new java.io.File(VERSION_DIR).mkdirs();
        new java.io.File(ACCOUNT_DIR).mkdirs();
        new java.io.File(CONFIG_DIR).mkdirs();
    }
}
