package com.tungsten.hmclpe.launcher.download;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class DownloadService {

    private static final OkHttpClient http = new OkHttpClient.Builder().build();
    private static final List<Task> tasks = new ArrayList<>();
    private static final List<Listener> listeners = new ArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());

    public static class Task {
        public String id;
        public String name;
        public String url;
        public String targetVersion;
        public long totalBytes;
        public long downloadedBytes;
        public int progress;
        public State state = State.QUEUED;
        public File destination;

        public enum State { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELED }
    }

    public interface Listener {
        void onUpdate(List<Task> snapshot);
        void onCompleted(Task task);
        void onFailed(Task task, String error);
    }

    public synchronized static void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public synchronized static void removeListener(Listener l) {
        listeners.remove(l);
    }

    public static List<Task> snapshot() {
        synchronized (tasks) {
            return new ArrayList<>(tasks);
        }
    }

    public static void enqueue(Task t) {
        synchronized (tasks) {
            tasks.add(t);
        }
        notifyUpdate();
        new Thread(() -> runTask(t)).start();
    }

    public static void cancel(Task t) {
        t.state = Task.State.CANCELED;
        notifyUpdate();
    }

    private static void runTask(Task t) {
        try {
            t.state = Task.State.RUNNING;
            notifyUpdate();
            HttpURLConnection conn = (HttpURLConnection) new URL(t.url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "StarDockLauncher/1.0.6");
            conn.connect();
            int code = conn.getResponseCode();
            if (code / 100 != 2) {
                fail(t, "HTTP " + code);
                return;
            }
            t.totalBytes = conn.getContentLengthLong();
            if (t.destination.getParentFile() != null) t.destination.getParentFile().mkdirs();
            try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(t.destination)) {
                byte[] buf = new byte[8192];
                int n;
                long lastUpdate = 0;
                while ((n = in.read(buf)) != -1) {
                    if (t.state == Task.State.CANCELED || t.state == Task.State.PAUSED) {
                        try { out.close(); } catch (Throwable ignored) {}
                        if (t.destination.exists()) t.destination.delete();
                        return;
                    }
                    out.write(buf, 0, n);
                    t.downloadedBytes += n;
                    if (t.totalBytes > 0) t.progress = (int) (t.downloadedBytes * 100 / t.totalBytes);
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 200) {
                        lastUpdate = now;
                        notifyUpdate();
                    }
                }
            }
            t.state = Task.State.COMPLETED;
            t.progress = 100;
            notifyUpdate();
            for (Listener l : listeners) {
                try { l.onCompleted(t); } catch (Throwable ignored) {}
            }
        } catch (IOException e) {
            fail(t, e.getMessage() == null ? "IO 错误" : e.getMessage());
        } catch (Throwable t2) {
            fail(t, t2.getMessage() == null ? "未知错误" : t2.getMessage());
        }
    }

    private static void fail(Task t, String error) {
        t.state = Task.State.FAILED;
        synchronized (tasks) {
            for (Listener l : listeners) {
                try { l.onFailed(t, error); } catch (Throwable ignored) {}
            }
        }
        notifyUpdate();
    }

    private static void notifyUpdate() {
        final List<Task> snap = snapshot();
        main.post(() -> {
            for (Listener l : listeners) {
                try { l.onUpdate(snap); } catch (Throwable ignored) {}
            }
        });
    }
}
