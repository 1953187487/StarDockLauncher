package com.tungsten.hmclpe.launcher.download;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadService {

    private static final String TAG = "DownloadService";

    private static volatile DownloadService instance;
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private final ExecutorService exec = Executors.newFixedThreadPool(2);

    public static DownloadService get() {
        if (instance == null) {
            instance = new DownloadService();
        }
        return instance;
    }

    public interface Callback {
        void onProgress(long downloaded, long total);

        void onDone(File file);

        void onError(Throwable t);
    }

    public void download(Context ctx, DownloadTask task, Callback cb) {
        exec.submit(() -> doDownload(task, cb));
    }

    private void doDownload(DownloadTask task, Callback cb) {
        try {
            File out = new File(task.target);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Request req = new Request.Builder().url(task.url).build();
            try (Response resp = client.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    task.state = DownloadTask.STATE_FAILED;
                    if (cb != null) {
                        cb.onError(new IOException("HTTP " + resp.code()));
                    }
                    return;
                }
                long total = resp.body().contentLength();
                task.size = total;
                task.state = DownloadTask.STATE_RUNNING;
                try (InputStream in = resp.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192];
                    long read = 0;
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                        read += n;
                        task.downloaded = read;
                        if (cb != null) {
                            cb.onProgress(read, total);
                        }
                    }
                    fos.flush();
                }
            }
            task.state = DownloadTask.STATE_DONE;
            if (cb != null) {
                cb.onDone(out);
            }
        } catch (Throwable t) {
            Log.e(TAG, "download failed: " + task.url, t);
            task.state = DownloadTask.STATE_FAILED;
            task.error = t;
            if (cb != null) {
                cb.onError(t);
            }
        }
    }
}
