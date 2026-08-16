package com.tungsten.hmclpe.launcher.setting;

import android.os.Environment;

import com.tungsten.hmclpe.launcher.HMCLPEApplication;

import java.io.File;

public class VersionManager {

    public static final String ROOT_NAME = "StarDockLauncher";

    public static File root() { return rootDir(); }
    public static File rootDir() {
        File root = new File(Environment.getExternalStorageDirectory(), ROOT_NAME);
        if (!root.exists()) root.mkdirs();
        return root;
    }

    public static File gamesDir() {
        File g = new File(rootDir(), "games");
        if (!g.exists()) g.mkdirs();
        return g;
    }

    public static File versionsDir() {
        File v = new File(gamesDir(), "versions");
        if (!v.exists()) v.mkdirs();
        return v;
    }

    public static File modsDir() {
        File m = new File(gamesDir(), "mods");
        if (!m.exists()) m.mkdirs();
        return m;
    }

    public static File librariesDir() {
        File l = new File(rootDir(), "libraries");
        if (!l.exists()) l.mkdirs();
        return l;
    }

    public static File runtimeDir() {
        File r = new File(rootDir(), "runtime");
        if (!r.exists()) r.mkdirs();
        return r;
    }

    public static String currentGameDir() {
        String custom = AppPrefs.getString(HMCLPEApplication.getContext(), AppPrefs.KEY_GAME_DIR, "");
        if (custom != null && !custom.isEmpty()) return custom;
        return gamesDir().getAbsolutePath();
    }
}
