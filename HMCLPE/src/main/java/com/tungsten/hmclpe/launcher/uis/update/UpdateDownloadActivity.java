package com.tungsten.hmclpe.launcher.uis.update;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;
import com.tungsten.hmclpe.BuildConfig;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.setting.VersionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateDownloadActivity extends AppCompatActivity {

    private String tag;
    private String apkUrl;
    private String changelog;
    private File target;

    private MaterialTextView versionView;
    private MaterialTextView percentView;
    private MaterialTextView speedView;
    private MaterialTextView sizeView;
    private MaterialTextView changelogView;
    private LinearProgressIndicator progress;
    private MaterialButton btnCancel;
    private MaterialButton btnInstall;

    private volatile boolean cancelled = false;
    private volatile boolean finished = false;
    private long downloaded = 0;
    private long total = 0;
    private long lastTick = 0;
    private long lastBytes = 0;

    private Thread workThread;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_update_download);
        } catch (Throwable t) {
            Toast.makeText(this, "加载失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        MaterialToolbar toolbar = findViewById(R.id.update_toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        tag = getIntent() == null ? "v" + BuildConfig.VERSION_NAME : getIntent().getStringExtra("tag");
        apkUrl = getIntent() == null ? null : getIntent().getStringExtra("apkUrl");
        changelog = getIntent() == null ? "" : getIntent().getStringExtra("changelog");
        if (tag == null) tag = "latest";
        if (changelog == null) changelog = "";

        versionView = findViewById(R.id.update_version);
        percentView = findViewById(R.id.update_percent);
        speedView = findViewById(R.id.update_speed);
        sizeView = findViewById(R.id.update_size);
        changelogView = findViewById(R.id.update_changelog);
        progress = findViewById(R.id.update_progress);
        btnCancel = findViewById(R.id.update_btn_cancel);
        btnInstall = findViewById(R.id.update_btn_install);

        versionView.setText("当前 v" + BuildConfig.VERSION_NAME + "  →  " + tag);
        changelogView.setText(changelog);
        progress.setProgress(0, true);

        File root = VersionManager.root();
        if (!root.exists()) root.mkdirs();
        target = new File(root, "StarDockLauncher-" + tag + ".apk");

        btnCancel.setOnClickListener(v -> {
            if (finished) {
                finish();
                return;
            }
            cancelled = true;
            new MaterialAlertDialogBuilder(this)
                    .setTitle("取消下载？")
                    .setMessage("下载进度将丢失。")
                    .setPositiveButton("确定", (d, w) -> {
                        cancelWork();
                        finish();
                    })
                    .setNegativeButton("继续下载", null)
                    .show();
        });

        btnInstall.setOnClickListener(v -> installApk());

        if (apkUrl == null || apkUrl.isEmpty()) {
            Toast.makeText(this, "下载链接为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        startDownload();
    }

    private void startDownload() {
        btnInstall.setEnabled(false);
        progress.setIndeterminate(false);
        progress.setProgress(0, true);
        downloaded = 0;
        total = 0;
        lastTick = System.currentTimeMillis();
        lastBytes = 0;
        cancelled = false;
        finished = false;

        workThread = new Thread(() -> {
            HttpURLConnection con = null;
            InputStream in = null;
            FileOutputStream out = null;
            try {
                URL u = new URL(apkUrl);
                con = (HttpURLConnection) u.openConnection();
                con.setConnectTimeout(15000);
                con.setReadTimeout(30000);
                con.setRequestProperty("User-Agent", "StarDockLauncher");
                con.connect();
                int code = con.getResponseCode();
                if (code >= 300 && code < 400) {
                    String loc = con.getHeaderField("Location");
                    if (loc != null) {
                        con = (HttpURLConnection) new URL(loc).openConnection();
                        con.setConnectTimeout(15000);
                        con.setReadTimeout(30000);
                        con.connect();
                        code = con.getResponseCode();
                    }
                }
                if (code != 200) {
                    final int statusCode = code;
                ui.post(() -> Toast.makeText(this, "下载失败 HTTP " + statusCode, Toast.LENGTH_LONG).show());
                    return;
                }
                int len = con.getContentLength();
                if (len > 0) total = len;
                in = con.getInputStream();
                out = new FileOutputStream(target);
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    if (cancelled) {
                        try { target.delete(); } catch (Throwable ignored) {}
                        return;
                    }
                    out.write(buf, 0, n);
                    downloaded += n;
                    tick();
                }
                out.flush();
                finished = true;
                ui.post(this::onDone);
            } catch (Throwable t) {
                try { if (target != null) target.delete(); } catch (Throwable ignored) {}
                ui.post(() -> Toast.makeText(this, "下载异常：" + t.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                try { if (in != null) in.close(); } catch (Throwable ignored) {}
                try { if (out != null) out.close(); } catch (Throwable ignored) {}
                try { if (con != null) con.disconnect(); } catch (Throwable ignored) {}
            }
        }, "UpdateDownloader");
        workThread.start();
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long dt = now - lastTick;
        if (dt < 250) return;
        long dbytes = downloaded - lastBytes;
        long speedBps = (dbytes * 1000) / Math.max(dt, 1);
        lastTick = now;
        lastBytes = downloaded;

        final int percent = (total > 0) ? (int) (downloaded * 100 / total) : 0;
        final String speed = formatSize(speedBps) + "/s";
        final String size = formatSize(downloaded) + " / " + (total > 0 ? formatSize(total) : "未知");

        ui.post(() -> {
            try {
                progress.setProgressCompat(percent, true);
                percentView.setText(percent + "%");
                speedView.setText(speed);
                sizeView.setText(size);
            } catch (Throwable ignored) {}
        });
    }

    private void onDone() {
        progress.setProgressCompat(100, true);
        percentView.setText("100%");
        speedView.setText("已完成");
        sizeView.setText(formatSize(downloaded) + " / " + formatSize(downloaded));
        btnInstall.setEnabled(true);
        btnCancel.setText("关闭");
        Toast.makeText(this, "下载完成：" + target.getAbsolutePath(), Toast.LENGTH_LONG).show();
    }

    private void cancelWork() {
        cancelled = true;
        try {
            if (workThread != null) workThread.interrupt();
        } catch (Throwable ignored) {}
        try {
            if (target != null && target.exists()) target.delete();
        } catch (Throwable ignored) {}
    }

    private void installApk() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(target), "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "无法安装：" + t.getMessage() + "\n文件位置：" + target.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        cancelWork();
        super.onDestroy();
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        double v = bytes;
        int i = 0;
        while (v >= 1024 && i < units.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format("%.1f %s", v, units[i]);
    }
}
