package com.tungsten.hmclpe.launcher.update;

public class LauncherUpdate {

    public static final String RELEASES_API = "https://api.github.com/repos/1953187487/StarDockLauncher/releases/latest";

    public String tagName;
    public String versionCode;
    public String downloadUrl;
    public String changelog;
    public String releaseId;
    public long size;
    public String assetName;

    public boolean isNewer(int currentCode, String currentName) {
        try {
            int remote = Integer.parseInt(versionCode == null ? "0" : versionCode);
            return remote > currentCode;
        } catch (Throwable t) {
            return tagName != null && currentName != null && !tagName.equals(currentName);
        }
    }
}
