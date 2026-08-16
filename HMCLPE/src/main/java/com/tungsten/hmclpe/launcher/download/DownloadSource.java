package com.tungsten.hmclpe.launcher.download;

public class DownloadSource {

    public static final int SOURCE_BMCLAPI = 0;
    public static final int SOURCE_MOJANG = 1;
    public static final int SOURCE_MCBBS = 2;

    private static final String[] HOSTS = new String[]{
            "https://bmclapi2.bangbang93.com",
            "https://launcher.mojang.com",
            "https://download.mcbbs.net"
    };

    public static String host(int source) {
        int idx = source;
        if (idx < 0 || idx >= HOSTS.length) {
            idx = 0;
        }
        return HOSTS[idx];
    }

    public static String url(int source, String path) {
        return host(source) + (path.startsWith("/") ? path : "/" + path);
    }

    public static String[] labels() {
        return new String[]{"BMCLAPI", "Mojang 官方", "MCBBS 镜像"};
    }
}
