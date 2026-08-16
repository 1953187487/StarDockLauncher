package com.tungsten.hmclpe.launcher.setting;

import com.tungsten.hmclpe.launcher.HMCLPEApplication;

public class AuthManager {

    public static final int OFFLINE = 0;
    public static final int MICROSOFT = 1;
    public static final int THIRD_PARTY = 2;

    public static int currentMode() {
        return AppPrefs.getInt(HMCLPEApplication.getContext(), AppPrefs.KEY_LOGIN_MODE, OFFLINE);
    }

    public static String currentNickname() {
        String n = AppPrefs.getString(HMCLPEApplication.getContext(), AppPrefs.KEY_LOGIN_NICKNAME, "StarDockPlayer");
        return n == null || n.isEmpty() ? "StarDockPlayer" : n;
    }

    public static String currentServer() {
        return AppPrefs.getString(HMCLPEApplication.getContext(), AppPrefs.KEY_LOGIN_SERVER, "");
    }

    public static void setMode(int mode) {
        AppPrefs.setInt(HMCLPEApplication.getContext(), AppPrefs.KEY_LOGIN_MODE, mode);
    }

    public static void setNickname(String nickname) {
        AppPrefs.setString(HMCLPEApplication.getContext(), AppPrefs.KEY_LOGIN_NICKNAME, nickname == null ? "" : nickname);
    }

    public static void setServer(String server) {
        AppPrefs.setString(HMCLPEApplication.getContext(), AppPrefs.KEY_LOGIN_SERVER, server == null ? "" : server);
    }

    public static String name(int mode) {
        if (mode == MICROSOFT) return "正版微软";
        if (mode == THIRD_PARTY) return "第三方服务器";
        return "离线模式";
    }
}
