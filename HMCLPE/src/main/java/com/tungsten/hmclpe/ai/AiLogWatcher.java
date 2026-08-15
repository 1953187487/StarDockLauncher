package com.tungsten.hmclpe.ai;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AiLogWatcher {

    private static AiLogWatcher instance;

    public interface ErrorListener {
        void onError(String line);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ErrorListener> listeners = new ArrayList<>();
    private Thread worker;
    private volatile boolean running = false;
    private long lastSize = 0;
    private long lastModified = 0;
    private String logPath;
    private final Set<String> recentErrors = new HashSet<>();

    private AiLogWatcher() {
    }

    public static synchronized AiLogWatcher getInstance() {
        if (instance == null) {
            instance = new AiLogWatcher();
        }
        return instance;
    }

    public void start(String logPath) {
        this.logPath = logPath;
        File file = logPath == null ? null : new File(logPath);
        if (file == null || !file.exists()) {
            return;
        }
        lastSize = file.length();
        lastModified = file.lastModified();
        if (running) {
            return;
        }
        running = true;
        worker = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(2000);
                    File f = new File(logPath);
                    if (f.exists() && f.lastModified() > lastModified) {
                        long newSize = f.length();
                        if (newSize >= lastSize) {
                            readNew(f, lastSize, newSize);
                            lastSize = newSize;
                            lastModified = f.lastModified();
                        } else {
                            lastSize = newSize;
                            lastModified = f.lastModified();
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {
                }
            }
        }, "AiLogWatcher");
        worker.setDaemon(true);
        worker.start();
    }

    public void addListener(ErrorListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ErrorListener listener) {
        listeners.remove(listener);
    }

    public boolean hasListeners() {
        return !listeners.isEmpty();
    }

    private void readNew(File file, long from, long to) {
        if (to - from > 10 * 1024 * 1024) {
            from = to - 10 * 1024 * 1024;
        }
        byte[] buffer = new byte[(int) (to - from)];
        try (InputStream in = new FileInputStream(file)) {
            in.skip(from);
            int read = in.read(buffer);
            if (read > 0) {
                String content = new String(buffer, 0, read, "UTF-8");
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (isErrorLine(line)) {
                        notifyError(line);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private boolean isErrorLine(String line) {
        if (line == null || line.isEmpty()) return false;
        String low = line.toLowerCase();
        if (line.contains("Exception") || line.contains("Error")
                || low.contains("fatal") || low.contains("crash")
                || (low.contains("error") && (low.contains("failed") || low.contains("cannot")))) {
            return true;
        }
        return false;
    }

    private void notifyError(String line) {
        String key = line.length() > 200 ? line.substring(0, 200) : line;
        if (recentErrors.contains(key)) {
            return;
        }
        if (recentErrors.size() > 300) {
            recentErrors.clear();
        }
        recentErrors.add(key);
        mainHandler.post(() -> {
            for (ErrorListener listener : listeners) {
                listener.onError(line);
            }
        });
    }

    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        listeners.clear();
    }

    public void stopWatcher() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    public boolean isRunning() {
        return running;
    }
}
