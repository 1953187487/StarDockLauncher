package com.tungsten.hmclpe.launcher.download;

public class DownloadTask {

    public String url;
    public String sha1;
    public long size;
    public String target;
    public String title;
    public int source = DownloadSource.SOURCE_BMCLAPI;
    public long downloaded;
    public int state = STATE_PENDING;
    public Throwable error;

    public static final int STATE_PENDING = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_DONE = 2;
    public static final int STATE_FAILED = 3;
    public static final int STATE_CANCELLED = 4;

    public DownloadTask(String url, String target, String title) {
        this.url = url;
        this.target = target;
        this.title = title;
    }

    public int progressPercent() {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.min(100, downloaded * 100 / size);
    }
}
