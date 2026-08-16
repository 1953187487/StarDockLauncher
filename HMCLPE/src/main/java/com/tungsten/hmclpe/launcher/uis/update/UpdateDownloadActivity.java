package com.tungsten.hmclpe.launcher.uis.update;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.stardock.launcher.BuildConfig;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.launcher.download.DownloadService;
import com.tungsten.hmclpe.launcher.download.DownloadSource;
import com.tungsten.hmclpe.launcher.download.DownloadTask;
import com.tungsten.hmclpe.launcher.update.LauncherUpdate;
import com.tungsten.hmclpe.launcher.update.UpdateService;

import java.io.File;

public class UpdateDownloadActivity extends AppCompatActivity {

    private static final String TAG = "UpdateDownloadActivity";

    private TextView info;
    private LinearProgressIndicator bar;
    private MaterialButton btn;
    private LauncherUpdate update;
    private File targetApk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_update_download);
        } catch (Throwable t) {
            Log.e(TAG, "setContentView failed", t);
            return;
        }
        try {
            MaterialToolbar tb = findViewById(R.id.update_toolbar);
            if (tb != null) {
                setSupportActionBar(tb);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
                tb.setNavigationOnClickListener(v -> finish());
            }
        } catch (Throwable t) {
            Log.e(TAG, "toolbar failed", t);
        }
        try {
            info = findViewById(R.id.update_info);
            bar = findViewById(R.id.update_progress);
            btn = findViewById(R.id.update_btn_action);
        } catch (Throwable t) {
            Log.e(TAG, "bind failed", t);
        }
        try {
            if (info != null) {
                info.setText("当前版本：v" + BuildConfig.VERSION_NAME + "（versionCode=" + BuildConfig.VERSION_CODE + "）\n点击下方按钮检查最新版本");
            }
            if (bar != null) {
                bar.setMax(100);
            }
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    if (update == null || update.downloadUrl == null) {
                        checkUpdate();
                    } else {
                        startDownload();
                    }
                });
            }
        } catch (Throwable t) {
            Log.e(TAG, "init failed", t);
        }
    }

    private void checkUpdate() {
        try {
            new UpdateService().fetchLatest(new UpdateService.Callback() {
                @Override
                public void onResult(LauncherUpdate u) {
                    runOnUiThread(() -> renderUpdate(u));
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> {
                        if (info != null) {
                            info.setText("检查更新失败：" + t.getMessage());
                        }
                    });
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "checkUpdate failed", t);
        }
    }

    private void renderUpdate(LauncherUpdate u) {
        try {
            this.update = u;
            if (u == null || u.tagName == null) {
                if (info != null) {
                    info.setText("未获取到更新信息");
                }
                if (btn != null) {
                    btn.setEnabled(false);
                }
                return;
            }
            boolean newer = u.isNewer(BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME);
            if (info != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("最新版本：").append(u.tagName).append("\n");
                sb.append("本地版本：v").append(BuildConfig.VERSION_NAME).append("\n");
                sb.append("状态：").append(newer ? "有新版本" : "已是最新").append("\n");
                if (u.changelog != null && !u.changelog.isEmpty()) {
                    sb.append("\n").append(u.changelog);
                }
                info.setText(sb.toString());
            }
            if (btn != null) {
                btn.setEnabled(newer);
                btn.setText(newer ? "下载并安装" : "已是最新");
            }
        } catch (Throwable t) {
            Log.e(TAG, "renderUpdate failed", t);
        }
    }

    private void startDownload() {
        try {
            targetApk = new File(getExternalCacheDir("update"), (update.assetName == null ? "update.apk" : update.assetName));
            DownloadTask task = new DownloadTask(update.downloadUrl, targetApk.getAbsolutePath(), update.assetName);
            task.source = DownloadSource.SOURCE_BMCLAPI;
            if (bar != null) {
                bar.setProgressCompat(0, false);
            }
            if (btn != null) {
                btn.setEnabled(false);
            }
            DownloadService.get().download(this, task, new DownloadService.Callback() {
                @Override
                public void onProgress(long downloaded, long total) {
                    runOnUiThread(() -> {
                        if (bar != null && total > 0) {
                            bar.setProgressCompat((int) Math.min(100, downloaded * 100 / total), true);
                        }
                    });
                }

                @Override
                public void onDone(File file) {
                    runOnUiThread(() -> {
                        if (info != null) {
                            info.setText("下载完成：" + file.getAbsolutePath() + "\n请用系统安装器打开安装");
                        }
                        if (btn != null) {
                            btn.setEnabled(true);
                            btn.setText("完成");
                        }
                    });
                }

                @Override
                public void onError(Throwable t) {
                    runOnUiThread(() -> {
                        if (info != null) {
                            info.setText("下载失败：" + t.getMessage());
                        }
                        if (btn != null) {
                            btn.setEnabled(true);
                            btn.setText("重试");
                        }
                    });
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "startDownload failed", t);
        }
    }

    private File getExternalCacheDir(String sub) {
        File base = getExternalCacheDir();
        File out = new File(base, sub);
        if (!out.exists()) {
            out.mkdirs();
        }
        return out;
    }
}
