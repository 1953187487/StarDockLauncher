package com.tungsten.hmclpe.launcher.multiplayer;

import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalPortScanner {

    private static final String TAG = "LocalPortScanner";
    private static final Pattern PORT_PATTERN = Pattern.compile(
            "(?:hosted|started|opened)\\s+(?:on\\s+)?(?:port\\s+)?(\\d{2,5})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CHINESE_PORT = Pattern.compile("对局域网开放.*?端口[：:]?\\s*(\\d{2,5})");

    private LocalPortScanner() {}

    public static int scanLatestLog(String logPath) {
        if (logPath == null) return -1;
        File f = new File(logPath);
        if (!f.exists()) return -1;
        long size = f.length();
        long start = Math.max(0, size - 64 * 1024);
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            raf.seek(start);
            byte[] buf = new byte[(int) Math.min(size - start, 64 * 1024)];
            raf.readFully(buf);
            String content = new String(buf, "UTF-8");
            Matcher m = PORT_PATTERN.matcher(content);
            int found = -1;
            while (m.find()) {
                try {
                    int p = Integer.parseInt(m.group(1));
                    if (p >= 1024 && p <= 65535) {
                        found = p;
                    }
                } catch (Throwable ignored) {}
            }
            Matcher cm = CHINESE_PORT.matcher(content);
            if (cm.find()) {
                try {
                    int p = Integer.parseInt(cm.group(1));
                    if (p >= 1024 && p <= 65535) {
                        found = p;
                    }
                } catch (Throwable ignored) {}
            }
            return found;
        } catch (Throwable t) {
            Log.w(TAG, "scanLatestLog failed: " + t.getMessage());
            return -1;
        }
    }
}
