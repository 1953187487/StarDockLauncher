package net.kdt.pojavlaunch.stardock.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * v0.0.8 重写一键下载 Java Runtime
 * - 直接从 BMCLAPI 下载（国内推荐）
 * - 支持 JRE 8 / 17 / 21
 * - 显示进度
 * - 解压并通过 MultiRTUtils.installRuntimeNamed 安装
 *
 * 不依赖 PojavLauncher 的 NewJREUtil（它依赖 ZXing + GLFWSurface 复杂 UI）
 */
public class OneClickJavaActivity {

    private static final String BMCLAPI_BASE = "https://bmclapi2.bangbang93.com";

    /** 主流 JRE 安装包（BMCLAPI 提供 universal + binpack） */
    private static final String[][] JRE_DOWNLOADS = {
            // {name, major, universalUrl, binpackUrl}
            {"Internal-17", "17",
                    BMCLAPI_BASE + "/java/jre17_linux.tar.xz",
                    BMCLAPI_BASE + "/java/jre17_linux_bin.tar.xz"},
            {"Internal-21", "21",
                    BMCLAPI_BASE + "/java/jre21_linux.tar.xz",
                    BMCLAPI_BASE + "/java/jre21_linux_bin.tar.xz"}
    };

    public static void showDialog(Activity activity) {
        String[] names = new String[JRE_DOWNLOADS.length + 1];
        for (int i = 0; i < JRE_DOWNLOADS.length; i++) names[i] = "JRE " + JRE_DOWNLOADS[i][1] + " (BMCLAPI 镜像)";
        names[JRE_DOWNLOADS.length] = "从本地 .tar.xz 安装";

        new AlertDialog.Builder(activity)
                .setTitle("一键下载 Java 运行时")
                .setItems(names, (d, w) -> {
                    if (w < JRE_DOWNLOADS.length) {
                        startDownload(activity, JRE_DOWNLOADS[w][0], JRE_DOWNLOADS[w][2], JRE_DOWNLOADS[w][3]);
                    } else {
                        Toast.makeText(activity, "请通过「控制布局」旁安装入口选择本地 .tar.xz", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void startDownload(Activity activity, String name, String universalUrl, String binpackUrl) {
        View progressView = activity.getLayoutInflater().inflate(R.layout.dialog_download_progress, null);
        TextView statusText = progressView.findViewById(R.id.dl_status);
        TextView percentText = progressView.findViewById(R.id.dl_percent);
        ProgressBar progressBar = progressView.findViewById(R.id.dl_progress);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("下载 Java 运行时")
                .setView(progressView)
                .setCancelable(false)
                .setNegativeButton("取消", null)
                .create();

        new AsyncTask<Void, Integer, String>() {
            @Override protected void onPreExecute() {
                dialog.show();
                statusText.setText("正在连接 BMCLAPI 镜像…");
                percentText.setText("0%");
                progressBar.setProgress(0);
            }

            @Override protected String doInBackground(Void... voids) {
                try {
                    File cacheDir = new File(activity.getCacheDir(), "jre-download");
                    cacheDir.mkdirs();
                    File universalFile = new File(cacheDir, name + "_universal.tar.xz");
                    File binpackFile = new File(cacheDir, name + "_binpack.tar.xz");

                    // 下载 universal
                    publishProgress(0, "下载通用包…");
                    downloadFile(universalUrl, universalFile, this);

                    // 下载 binpack
                    publishProgress(0, "下载平台包…");
                    downloadFile(binpackUrl, binpackFile, this);

                    // 安装
                    publishProgress(0, "正在解压并安装…");
                    try (InputStream u = new BufferedInputStream(new java.io.FileInputStream(universalFile));
                         InputStream b = new BufferedInputStream(new java.io.FileInputStream(binpackFile))) {
                        MultiRTUtils.installRuntimeNamedBinpack(u, b, name, "1");
                    }

                    // 清理
                    universalFile.delete();
                    binpackFile.delete();
                    return null;
                } catch (Exception e) {
                    return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                }
            }

            private void publishProgress(int p, String status) {
                publishProgress(p, status, "");
            }

            private void publishProgress(int p, String status, String unused) {
                publishProgressOnUi(p, status);
            }

            private void publishProgressOnUi(int p, String status) {
                activity.runOnUiThread(() -> {
                    progressBar.setProgress(p);
                    percentText.setText(p + "%");
                    statusText.setText(status);
                });
            }

            private void downloadFile(String urlStr, File dest, AsyncTask<Void, Integer, String> task) throws IOException {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();
                int code = conn.getResponseCode();
                if (code != 200) throw new IOException("HTTP " + code + " for " + urlStr);
                int total = conn.getContentLength();
                try (InputStream in = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int len;
                    int read = 0;
                    int lastP = -1;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                        read += len;
                        if (total > 0) {
                            int p = (int) (read * 100L / total);
                            if (p != lastP) {
                                lastP = p;
                                publishProgressOnUi(p, "下载中…");
                            }
                        }
                    }
                }
            }

            @Override protected void onProgressUpdate(Integer... values) {
                progressBar.setProgress(values[0]);
                percentText.setText(values[0] + "%");
            }

            @Override protected void onPostExecute(String err) {
                if (dialog.isShowing()) dialog.dismiss();
                if (err == null) {
                    Toast.makeText(activity, "Java 运行时安装完成：" + name, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(activity, "下载失败：" + err, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
}
