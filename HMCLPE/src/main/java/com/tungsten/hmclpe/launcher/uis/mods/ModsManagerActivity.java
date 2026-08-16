package com.tungsten.hmclpe.launcher.uis.mods;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.ai.AiModTranslator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModsManagerActivity extends AppCompatActivity {

    private final List<File> mods = new ArrayList<>();
    private ModsAdapter adapter;
    private RecyclerView list;
    private TextView dirText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mods_manager);
        try {
            MaterialToolbar toolbar = findViewById(R.id.mods_toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
            dirText = findViewById(R.id.mods_dir_text);
            list = findViewById(R.id.mods_list);
            list.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ModsAdapter(this, mods);
            list.setAdapter(adapter);
            File modsDir = new File(getExternalFilesDir(null), "mods");
            dirText.setText("Mods 目录：" + modsDir.getAbsolutePath());
            MaterialButton scan = findViewById(R.id.mods_btn_scan);
            scan.setOnClickListener(v -> scanMods());
            MaterialButton ai = findViewById(R.id.mods_btn_ai);
            ai.setOnClickListener(v -> aiTranslateAll());
            scanMods();
        } catch (Throwable t) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.mods_manager_title)
                    .setMessage("初始化失败：" + t.getMessage())
                    .setPositiveButton(R.string.dialog_ok, (d, w) -> finish())
                    .show();
        }
    }

    private File modsDir() {
        return new File(getExternalFilesDir(null), "mods");
    }

    private void scanMods() {
        try {
            mods.clear();
            File dir = modsDir();
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                            mods.add(f);
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged();
        } catch (Throwable t) {
            Toast.makeText(this, "扫描失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void aiTranslateAll() {
        if (mods.isEmpty()) {
            Toast.makeText(this, "没有可汉化的模组", Toast.LENGTH_SHORT).show();
            return;
        }
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("AI 汉化中… (0/" + mods.size() + ")");
        dialog.setCancelable(false);
        dialog.show();
        new Thread(() -> {
            int done = 0;
            for (File f : mods) {
                final int index = done;
                try {
                    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    AiModTranslator.translateMod(this, f, "1.20.1", new AiModTranslator.Callback() {
                        @Override
                        public void onResult(File zhFile, String text) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable t) {
                            latch.countDown();
                        }
                    });
                    latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Throwable ignored) {
                }
                done = index + 1;
                final int d = done;
                runOnUiThread(() -> dialog.setMessage("AI 汉化中… (" + d + "/" + mods.size() + ")"));
            }
            runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(this, "汉化完成", Toast.LENGTH_SHORT).show();
                scanMods();
            });
        }).start();
    }
}
