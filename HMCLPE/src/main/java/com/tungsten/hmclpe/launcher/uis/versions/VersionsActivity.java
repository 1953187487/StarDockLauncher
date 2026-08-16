package com.tungsten.hmclpe.launcher.uis.versions;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.setting.AppPrefs;
import com.tungsten.hmclpe.launcher.setting.VersionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class VersionsActivity extends AppCompatActivity {

    private RecyclerView list;
    private LinearLayout emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_versions);
        } catch (Throwable t) {
            Toast.makeText(this, "加载失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        MaterialToolbar toolbar = findViewById(R.id.versions_toolbar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> finish());

        list = findViewById(R.id.versions_list);
        emptyView = findViewById(R.id.versions_empty);

        if (list != null) list.setLayoutManager(new LinearLayoutManager(this));

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        try {
            File gamesDir = VersionManager.gamesDir();
            List<File> data = new ArrayList<>();
            if (gamesDir != null && gamesDir.exists()) {
                File[] arr = gamesDir.listFiles();
                if (arr != null) {
                    Arrays.sort(arr, new Comparator<File>() {
                        @Override public int compare(File a, File b) {
                            return a.getName().compareToIgnoreCase(b.getName());
                        }
                    });
                    for (File f : arr) {
                        if (f.isDirectory()) data.add(f);
                    }
                }
            }
            if (data.isEmpty()) {
                if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                if (list != null) list.setVisibility(View.GONE);
            } else {
                if (emptyView != null) emptyView.setVisibility(View.GONE);
                if (list != null) list.setVisibility(View.VISIBLE);
                if (list != null) list.setAdapter(new VersionAdapter(data));
            }
        } catch (Throwable t) {
            Toast.makeText(this, "读取失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.VH> {

        private final List<File> data;

        VersionAdapter(List<File> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_versions_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            try {
                File f = data.get(pos);
                String name = f.getName();
                h.title.setText(name);
                File jar = new File(f, "client.jar");
                File json = new File(f, name + ".json");
                File libs = new File(f, "libraries");
                StringBuilder meta = new StringBuilder();
                meta.append("目录：").append(f.getAbsolutePath()).append("\n");
                meta.append(jar.exists() ? "客户端 ✓" : "客户端 ✗").append("  ");
                meta.append(json.exists() ? "配置 ✓" : "配置 ✗").append("  ");
                meta.append(libs.exists() ? "依赖 ✓" : "依赖 ✗");
                h.meta.setText(meta);
                h.itemView.setOnClickListener(v -> showActions(f));
            } catch (Throwable ignored) {}
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title;
            TextView meta;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.item_version_title);
                meta = v.findViewById(R.id.item_version_meta);
            }
        }
    }

    private void showActions(File f) {
        String name = f.getName();
        String[] items = new String[]{"设为当前版本", "启动游戏", "打开目录", "删除版本"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(name)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            AppPrefs.setString(this, AppPrefs.KEY_LAST_GAME_VERSION, name);
                            AppPrefs.setString(this, AppPrefs.KEY_LAST_PROFILE, name);
                            Toast.makeText(this, "已选择：" + name, Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            launch(name);
                            break;
                        case 2:
                            try {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setDataAndType(android.net.Uri.fromFile(f), "resource/folder");
                                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                if (i.resolveActivity(getPackageManager()) == null) {
                                    Toast.makeText(this, "目录：" + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
                                } else {
                                    startActivity(i);
                                }
                            } catch (Throwable t) {
                                Toast.makeText(this, "目录：" + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
                            }
                            break;
                        case 3:
                            confirmDelete(f);
                            break;
                    }
                })
                .show();
    }

    private void confirmDelete(File f) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除版本 " + f.getName() + "？")
                .setMessage("将永久删除 " + f.getAbsolutePath() + "，操作不可恢复。")
                .setPositiveButton("删除", (d, w) -> {
                    try {
                        deleteRecursive(f);
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        refresh();
                    } catch (Throwable t) {
                        Toast.makeText(this, "删除失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        if (!f.delete()) {
            throw new RuntimeException("无法删除：" + f.getAbsolutePath());
        }
    }

    private void launch(String name) {
        try {
            AppPrefs.setString(this, AppPrefs.KEY_LAST_GAME_VERSION, name);
            AppPrefs.setString(this, AppPrefs.KEY_LAST_PROFILE, name);
            Intent intent = new Intent();
            intent.setClassName("com.tungsten.hmclpe", "com.tungsten.hmclpe.launcher.launch.boat.BoatMinecraftActivity");
            intent.putExtra("version_name", name);
            intent.putExtra("version_id", name);
            startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.tungsten.hmclpe", "com.tungsten.hmclpe.launcher.launch.pojav.PojavMinecraftActivity");
                intent.putExtra("version_name", name);
                startActivity(intent);
            } catch (Throwable tt) {
                Toast.makeText(this, "启动游戏失败：" + tt.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
