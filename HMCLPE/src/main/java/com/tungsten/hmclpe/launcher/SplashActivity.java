package com.tungsten.hmclpe.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.manifest.AppManifest;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final String[] STAGES = new String[]{
            "正在初始化运行时...",
            "正在加载资源...",
            "正在准备启动框架...",
            "正在检查更新...",
            "正在进入启动器..."
    };

    private LinearProgressIndicator progress;
    private TextView loadingText;
    private final Handler main = new Handler(Looper.getMainLooper());
    private int stage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        try {
            progress = findViewById(R.id.loading_progress_bar);
            loadingText = findViewById(R.id.loading_text);
            if (progress != null) {
                progress.setMax(100);
                progress.setProgressCompat(0, false);
            }
            if (loadingText != null && STAGES.length > 0) {
                loadingText.setText(STAGES[0]);
            }
        } catch (Throwable t) {
            Log.e(TAG, "view init failed", t);
        }
        try {
            new Thread(this::doInit, "sd-init").start();
        } catch (Throwable t) {
            Log.e(TAG, "init thread failed", t);
            enterLauncher();
        }
    }

    private void doInit() {
        try {
            advance("运行时目录：" + AppManifest.RUNTIME_DIR);
            Thread.sleep(300);
            advance("渲染器与控件目录已就绪");
            Thread.sleep(300);
            advance("启动框架：Boat + Pojav");
            Thread.sleep(300);
            advance("检查更新中...");
            Thread.sleep(300);
            advance("进入启动器...");
            Thread.sleep(200);
        } catch (Throwable t) {
            Log.e(TAG, "init failed", t);
        }
        main.post(this::enterLauncher);
    }

    private void advance(String text) {
        stage++;
        int pct = Math.min(100, stage * 100 / STAGES.length);
        main.post(() -> {
            try {
                if (progress != null) {
                    progress.setProgressCompat(pct, true);
                }
                if (loadingText != null && text != null) {
                    loadingText.setText(text);
                }
            } catch (Throwable t) {
                Log.e(TAG, "advance ui failed", t);
            }
        });
    }

    private void enterLauncher() {
        try {
            Intent i = new Intent(this, HomeActivity.class);
            startActivity(i);
        } catch (Throwable t) {
            Log.e(TAG, "start HomeActivity failed", t);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            main.removeCallbacksAndMessages(null);
        } catch (Throwable ignored) {
        }
    }
}
