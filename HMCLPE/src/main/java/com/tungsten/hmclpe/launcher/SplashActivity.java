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
import com.tungsten.hmclpe.launcher.runtime.RuntimeInstaller;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    private LinearProgressIndicator progress;
    private TextView loadingText;
    private final Handler main = new Handler(Looper.getMainLooper());

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
        } catch (Throwable t) {
            Log.e(TAG, "view init failed", t);
        }
        if (RuntimeInstaller.isInstalling()) {
            Log.i(TAG, "runtime already installing, wait");
        }
        startInit();
    }

    private void startInit() {
        try {
            RuntimeInstaller.ensure(this, new RuntimeInstaller.Callback() {
                @Override
                public void onProgress(String stage, int percent) {
                    main.post(() -> setProgress(stage, percent));
                }

                @Override
                public void onDone(java.io.File runtimeDir) {
                    main.post(() -> {
                        setProgress("运行时就绪", 100);
                        enterLauncher();
                    });
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "runtime install error", t);
                    main.post(() -> {
                        setProgress("运行时安装失败：" + t.getMessage(), 0);
                        new Handler(Looper.getMainLooper()).postDelayed(SplashActivity.this::enterLauncher, 1500);
                    });
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "runtime ensure failed", t);
            enterLauncher();
        }
    }

    private void setProgress(String text, int pct) {
        try {
            if (loadingText != null && text != null) {
                loadingText.setText(text);
            }
            if (progress != null) {
                progress.setProgressCompat(Math.max(0, Math.min(100, pct)), true);
            }
        } catch (Throwable ignored) {
        }
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
