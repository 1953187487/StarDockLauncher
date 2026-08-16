package com.tungsten.hmclpe.launcher.setting;

import com.tungsten.hmclpe.launcher.HMCLPEApplication;

public class ThemePrefs {

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_DARK = 1;
    public static final int MODE_LIGHT = 2;
    public static final int MODE_DYNAMIC = 3;

    public static int getMode() {
        return AppPrefs.getInt(HMCLPEApplication.getContext(), AppPrefs.KEY_THEME_MODE, MODE_DARK);
    }

    public static void setMode(int mode) {
        AppPrefs.setInt(HMCLPEApplication.getContext(), AppPrefs.KEY_THEME_MODE, mode);
    }

    public static String name(int mode) {
        if (mode == MODE_SYSTEM) return "跟随系统";
        if (mode == MODE_LIGHT) return "浅色";
        if (mode == MODE_DYNAMIC) return "动态取色（Android 12+）";
        return "深色（默认）";
    }

    public static String getBackgroundUri() {
        return AppPrefs.getString(HMCLPEApplication.getContext(), AppPrefs.KEY_BACKGROUND_URI, "");
    }

    public static void setBackgroundUri(String uri) {
        AppPrefs.setString(HMCLPEApplication.getContext(), AppPrefs.KEY_BACKGROUND_URI, uri == null ? "" : uri);
    }
}
