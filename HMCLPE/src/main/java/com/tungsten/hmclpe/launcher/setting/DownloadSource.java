package com.tungsten.hmclpe.launcher.setting;

public class DownloadSource {

    public static final int BMCLAPI = 0;
    public static final int MOJANG = 1;

    public static String url(int source, String path) {
        if (source == MOJANG) {
            return "https://launcher.mojang.com/" + path;
        }
        return "https://bmclapi2.bangbang93.com/" + path;
    }

    public static String name(int source) {
        if (source == MOJANG) return "Mojang 官方";
        return "BMCLAPI（默认，国内快）";
    }

    public static int current() {
        return AppPrefs.getInt(com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext(), AppPrefs.KEY_DOWNLOAD_SOURCE, BMCLAPI);
    }

    public static void setCurrent(int source) {
        AppPrefs.setInt(com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext(), AppPrefs.KEY_DOWNLOAD_SOURCE, source);
    }
}
