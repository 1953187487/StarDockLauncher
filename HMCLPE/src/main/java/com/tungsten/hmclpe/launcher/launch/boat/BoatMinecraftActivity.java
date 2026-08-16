package com.tungsten.hmclpe.launcher.launch.boat;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;

import androidx.annotation.Nullable;

import com.tungsten.hmclpe.auth.AccountInfo;
import com.tungsten.hmclpe.auth.AccountManager;
import com.tungsten.hmclpe.launcher.launch.LaunchArgsBuilder;
import com.tungsten.hmclpe.runtime.RuntimeInfo;
import com.tungsten.hmclpe.launcher.runtime.RuntimeInstaller;

import java.io.File;
import java.util.Vector;

import cosine.boat.BoatActivity;
import cosine.boat.BoatInput;
import cosine.boat.function.BoatCallback;
import cosine.boat.keyboard.BoatKeycodes;

public class BoatMinecraftActivity extends BoatActivity {

    public static final String EXTRA_VERSION = "extra_version";
    public static final String EXTRA_GAME_DIR = "extra_game_dir";
    public static final String EXTRA_SERVER = "extra_server";
    public static final String EXTRA_RENDERER = "extra_renderer";
    public static final String EXTRA_RAM = "extra_ram";

    private String versionId;
    private String gameDirPath;
    private String server;
    private String renderer = "GL4ES";
    private int maxRam = 1024;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            extras = new Bundle();
        }
        versionId = extras.getString(EXTRA_VERSION, "");
        gameDirPath = extras.getString(EXTRA_GAME_DIR, "");
        server = extras.getString(EXTRA_SERVER, "");
        renderer = extras.getString(EXTRA_RENDERER, "GL4ES");
        maxRam = extras.getInt(EXTRA_RAM, 1024);
        try {
            setContentView(cosine.boat.R.layout.activity_boat);
        } catch (Throwable t) {
            finish();
            return;
        }
        scaleFactor = 1.0F;
        handleCallback();
        try {
            init();
        } catch (Throwable t) {
            finish();
        }
    }

    private void handleCallback() {
        setBoatCallback(new BoatCallback() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                surface.setDefaultBufferSize(width, height);
                new Thread(() -> {
                    try {
                        File versionDir = new File(gameDirPath + "/versions/" + versionId);
                        File gameDir = new File(gameDirPath);
                        AccountManager mgr = new AccountManager(BoatMinecraftActivity.this);
                        AccountInfo account = mgr.getActive();
                        RuntimeInfo runtime = RuntimeInfo.from(BoatMinecraftActivity.this);
                        LaunchArgsBuilder.Result result = LaunchArgsBuilder.build(
                                BoatMinecraftActivity.this, runtime, RuntimeInfo.ENGINE_BOAT,
                                versionDir, gameDir, account, renderer, width, height, server, maxRam);
                        runOnUiThread(() -> {
                            if (!result.ready) {
                                android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(BoatMinecraftActivity.this);
                                b.setTitle("无法启动游戏");
                                b.setMessage(result.errorMessage == null ? "未知错误" : result.errorMessage);
                                b.setPositiveButton("退出", (d, w) -> finish());
                                b.setCancelable(false);
                                b.show();
                                return;
                            }
                            BoatActivity.setBoatNativeWindow(new Surface(surface));
                            BoatInput.setEventPipe();
                            File javaDir = runtime.java17() != null ? runtime.java17() : runtime.java8();
                            boolean highVersion = versionId != null && (versionId.startsWith("1.1")
                                    || versionId.startsWith("1.12") || versionId.startsWith("1.13")
                                    || versionId.startsWith("1.14") || versionId.startsWith("1.15")
                                    || versionId.startsWith("1.16") || versionId.startsWith("1.17")
                                    || versionId.startsWith("1.18") || versionId.startsWith("1.19")
                                    || versionId.startsWith("1.20") || versionId.startsWith("1.21")
                                    || versionId.startsWith("1.22") || versionId.startsWith("1.23")
                                    || versionId.startsWith("1.24") || versionId.startsWith("1.25"));
                            startGame(javaDir.getAbsolutePath(),
                                    javaDir.getAbsolutePath(),
                                    highVersion,
                                    result.args,
                                    renderer,
                                    gameDirPath);
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public void onCursorModeChange(int mode) {
                try {
                    if (mode == BoatInput.CursorDisabled) {
                        getWindow().getDecorView().setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    }
                } catch (Throwable ignored) {
                }
            }

            @Override
            public void onStart() {
            }

            @Override
            public void onPicOutput() {
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    try {
                        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(BoatMinecraftActivity.this);
                        b.setTitle("游戏启动失败");
                        b.setMessage(e == null ? "未知错误" : e.getMessage());
                        b.setPositiveButton("退出", (d, w) -> finish());
                        b.show();
                    } catch (Throwable ignored) {
                    }
                });
            }

            @Override
            public void onExit(int code) {
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        try {
            BoatInput.setKey(BoatKeycodes.KEY_ESC, 0, true);
            BoatInput.setKey(BoatKeycodes.KEY_ESC, 0, false);
        } catch (Throwable ignored) {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static Intent createIntent(android.content.Context ctx, String versionId, String gameDir, String server, String renderer, int ram) {
        Intent i = new Intent(ctx, BoatMinecraftActivity.class);
        i.putExtra(EXTRA_VERSION, versionId);
        i.putExtra(EXTRA_GAME_DIR, gameDir);
        i.putExtra(EXTRA_SERVER, server);
        i.putExtra(EXTRA_RENDERER, renderer);
        i.putExtra(EXTRA_RAM, ram);
        return i;
    }
}
