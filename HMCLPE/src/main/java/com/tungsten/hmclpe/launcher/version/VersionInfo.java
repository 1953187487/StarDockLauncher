package com.tungsten.hmclpe.launcher.version;

import java.io.Serializable;

public class VersionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public String type;
    public String url;
    public String time;
    public String releaseTime;
    public String sha1;
    public long size;
    public long installedSize;
    public boolean installed;
    public String javaVersion;
    public String loader;
    public boolean isVanilla;

    public VersionInfo() {
    }

    public VersionInfo(String id, String type, boolean installed) {
        this.id = id;
        this.type = type;
        this.installed = installed;
    }

    @Override
    public String toString() {
        return id == null ? "(null)" : id;
    }
}
