package com.tungsten.hmclpe.launcher.launch.pojav;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.tungsten.hmclpe.auth.AccountInfo;
import com.tungsten.hmclpe.auth.AccountManager;
import com.tungsten.hmclpe.launcher.launch.LaunchArgsBuilder;
import com.tungsten.hmclpe.runtime.RuntimeInfo;

import java.io.File;
import java.util.Vector;

import net.kdt.pojavlaunch.BaseMainActivity;
import net.kdt.pojavlaunch.function.PojavCallback;

public class PojavMinecraftActivity extends BaseMainActivity {

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
            setContentView(net.kdt.pojavlaunch.R.layout.activity_pojav);
        } catch (Throwable t) {
            finish();
            return;
        }
        scaleFactor = 1.0F;
        handleCallback();
        try {
            init(gameDirPath, versionId != null && isHighVersion(versionId));
        } catch (Throwable t) {
            finish();
        }
    }

    private boolean isHighVersion(String id) {
        return id != null && (id.startsWith("1.13") || id.startsWith("1.14")
                || id.startsWith("1.15") || id.startsWith("1.16") || id.startsWith("1.17")
                || id.startsWith("1.18") || id.startsWith("1.19") || id.startsWith("1.20")
                || id.startsWith("1.21") || id.startsWith("1.22") || id.startsWith("1.23"));
    }

    private void handleCallback() {
        pojavCallback = new PojavCallback() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                new Thread(() -> {
                    try {
                        File versionDir = new File(gameDirPath + "/versions/" + versionId);
                        File gameDir = new File(gameDirPath);
                        AccountManager mgr = new AccountManager(PojavMinecraftActivity.this);
                        AccountInfo account = mgr.getActive();
                        RuntimeInfo runtime = RuntimeInfo.from(PojavMinecraftActivity.this);
                        LaunchArgsBuilder.Result result = LaunchArgsBuilder.build(
                                PojavMinecraftActivity.this, runtime, RuntimeInfo.ENGINE_POJAV,
                                versionDir, gameDir, account, renderer, width, height, server, maxRam);
                        runOnUiThread(() -> {
                            if (!result.ready) {
                                android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(PojavMinecraftActivity.this);
                                b.setTitle("无法启动游戏");
                                b.setMessage(result.errorMessage == null ? "未知错误" : result.errorMessage);
                                b.setPositiveButton("退出", (d, w) -> finish());
                                b.setCancelable(false);
                                b.show();
                                return;
                            }
                            File javaDir = runtime.java17() != null ? runtime.java17() : runtime.java8();
                            startGame(javaDir.getAbsolutePath(),
                                    javaDir.getAbsolutePath(),
                                    isHighVersion(versionId),
                                    result.args,
                                    renderer,
                                    gameDirPath,
                                    "3.0");
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
                    if (mode == 0) {
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
                        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(PojavMinecraftActivity.this);
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
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static Intent createIntent(android.content.Context ctx, String versionId, String gameDir, String server, String renderer, int ram) {
        Intent i = new Intent(ctx, PojavMinecraftActivity.class);
        i.putExtra(EXTRA_VERSION, versionId);
        i.putExtra(EXTRA_GAME_DIR, gameDir);
        i.putExtra(EXTRA_SERVER, server);
        i.putExtra(EXTRA_RENDERER, renderer);
        i.putExtra(EXTRA_RAM, ram);
        return i;
    }
}
