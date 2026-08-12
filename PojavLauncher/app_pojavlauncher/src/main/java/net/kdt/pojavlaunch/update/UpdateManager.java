package net.kdt.pojavlaunch.update;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonObject;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Application-internal update manager.
 * Checks GitHub releases and offers in-app APK download via DownloadManager.
 */
public class UpdateManager {

    public static final String RELEASES_URL =
            "https://api.github.com/repos/1953187487/StarDockLauncher/releases/latest";
    public static final String RELEASE_PAGE =
            "https://github.com/1953187487/StarDockLauncher/releases/latest";

    private final Activity mActivity;
    private long mDownloadId = -1;

    public UpdateManager(Activity activity) {
        this.mActivity = activity;
    }

    /** Kick off a background check; show a dialog if a newer release is available. */
    public void checkForUpdates() {
        new Thread(() -> {
            try {
                String json = httpGet(RELEASES_URL);
                JsonObject root = Tools.GLOBAL_GSON.fromJson(json, JsonObject.class);
                String tagName = root.has("tag_name") ? root.get("tag_name").getAsString() : "";
                String version = stripTagPrefix(tagName);
                String body = root.has("body") ? root.get("body").getAsString() : "";
                String apkUrl = pickApkUrl(root);
                if (version.isEmpty() || apkUrl.isEmpty()) return;

                String current = currentVersionName();
                if (current.isEmpty()) return;

                if (isNewer(version, current)) {
                    mActivity.runOnUiThread(() -> showUpdateDialog(version, body, apkUrl));
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    /** Triggered when DownloadManager completes the APK download. */
    public void onDownloadComplete(long id) {
        if (id != mDownloadId) return;
        try {
            DownloadManager dm = (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = dm.getUriForDownloadedFile(id);
            if (uri == null) {
                Toast.makeText(mActivity, "下载完成但无法定位文件", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mActivity.startActivity(install);
            } catch (Exception e) {
                Toast.makeText(mActivity, "无法启动安装器：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }

    private void showUpdateDialog(String newVersion, String changelog, String apkUrl) {
        new AlertDialog.Builder(mActivity)
                .setTitle("发现新版本 v" + newVersion)
                .setMessage("当前版本：v" + currentVersionName()
                        + "\n\n更新日志：\n" + changelog)
                .setPositiveButton("立即下载", (DialogInterface d, int w) -> startDownload(apkUrl))
                .setNegativeButton(R.string.preference_check_update_summary_open, (d, w) ->
                        Tools.openURL(mActivity, RELEASE_PAGE))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void startDownload(String apkUrl) {
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(apkUrl));
            req.setTitle("StarDockLauncher v" + currentVersionName());
            req.setDescription("正在下载新版本");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setMimeType("application/vnd.android.package-archive");
            String filename = "StarDockLauncher-" + System.currentTimeMillis() + ".apk";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            } else {
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            }
            DownloadManager dm = (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
            mDownloadId = dm.enqueue(req);
            Toast.makeText(mActivity, "已开始下载", Toast.LENGTH_SHORT).show();

            mActivity.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    onDownloadComplete(completedId);
                    try { mActivity.unregisterReceiver(this); } catch (Exception ignored) {}
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
        } catch (Exception e) {
            Toast.makeText(mActivity, "下载启动失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static String stripTagPrefix(String tag) {
        if (tag == null) return "";
        String t = tag.trim();
        if (t.startsWith("v") || t.startsWith("V")) t = t.substring(1);
        return t;
    }

    private static String pickApkUrl(JsonObject release) {
        if (release.has("assets") && release.get("assets").isJsonArray()) {
            for (int i = 0; i < release.get("assets").getAsJsonArray().size(); i++) {
                JsonObject asset = release.get("assets").getAsJsonArray().get(i).getAsJsonObject();
                String name = asset.has("name") ? asset.get("name").getAsString() : "";
                String url = asset.has("browser_download_url") ? asset.get("browser_download_url").getAsString() : "";
                if (url.endsWith(".apk") && (name.toLowerCase().contains("release") || name.toLowerCase().contains("stardock"))) {
                    return url;
                }
            }
            for (int i = 0; i < release.get("assets").getAsJsonArray().size(); i++) {
                JsonObject asset = release.get("assets").getAsJsonArray().get(i).getAsJsonObject();
                String url = asset.has("browser_download_url") ? asset.get("browser_download_url").getAsString() : "";
                if (url.endsWith(".apk")) return url;
            }
        }
        return "";
    }

    /** Compare two dotted version strings; returns true if {@code remote} is newer. */
    private static boolean isNewer(String remote, String current) {
        String[] r = remote.split("\\.");
        String[] c = current.split("\\.");
        int len = Math.max(r.length, c.length);
        for (int i = 0; i < len; i++) {
            int ri = i < r.length ? parseIntSafe(r[i]) : 0;
            int ci = i < c.length ? parseIntSafe(c[i]) : 0;
            if (ri > ci) return true;
            if (ri < ci) return false;
        }
        return false;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private String currentVersionName() {
        try {
            PackageInfo info = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "StarDockLauncher/0.0.2");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
