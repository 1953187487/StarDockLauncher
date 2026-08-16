package com.tungsten.hmclpe.launcher.uis.versions;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.ai.AiTranslate;
import com.tungsten.hmclpe.launcher.setting.AppPrefs;
import com.tungsten.hmclpe.launcher.setting.VersionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ModsManagerActivity extends AppCompatActivity {

    private static final int REQ_IMPORT = 8800;

    private String version;
    private File modsDir;
    private RecyclerView list;
    private ModAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_mods_manager);
        } catch (Throwable t) {
            Toast.makeText(this, "加载失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        version = getIntent() == null ? "" : getIntent().getStringExtra("version");
        if (version == null || version.isEmpty()) version = AppPrefs.getString(this, AppPrefs.KEY_LAST_GAME_VERSION, "");

        MaterialToolbar toolbar = findViewById(R.id.mods_toolbar);
        if (toolbar != null) {
            toolbar.setTitle("模组管理 - " + version);
            toolbar.setSubtitle("导入模组自动经过 AI 汉化");
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        modsDir = new File(VersionManager.gamesDir(), version + "/mods");
        if (!modsDir.exists()) modsDir.mkdirs();

        list = findViewById(R.id.mods_list);
        if (list != null) list.setLayoutManager(new LinearLayoutManager(this));

        MaterialButton btnImport = findViewById(R.id.mods_btn_import);
        MaterialButton btnTranslateAll = findViewById(R.id.mods_btn_translate_all);
        MaterialButton btnOpenDir = findViewById(R.id.mods_btn_open_dir);

        if (btnImport != null) btnImport.setOnClickListener(v -> importMod());
        if (btnTranslateAll != null) btnTranslateAll.setOnClickListener(v -> translateAllMods());
        if (btnOpenDir != null) btnOpenDir.setOnClickListener(v -> openDir());

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        try {
            File[] arr = modsDir.listFiles();
            List<File> mods = new ArrayList<>();
            if (arr != null) {
                Arrays.sort(arr, new Comparator<File>() {
                    @Override public int compare(File a, File b) {
                        return a.getName().compareToIgnoreCase(b.getName());
                    }
                });
                for (File f : arr) if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) mods.add(f);
            }
            if (adapter == null) {
                adapter = new ModAdapter(mods);
                if (list != null) list.setAdapter(adapter);
            } else {
                adapter.setData(mods);
                adapter.notifyDataSetChanged();
            }
        } catch (Throwable t) {
            Toast.makeText(this, "读取失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importMod() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            String[] types = {"application/java-archive", "application/zip", "application/octet-stream"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, types);
            startActivityForResult(i, REQ_IMPORT);
        } catch (Throwable t) {
            Toast.makeText(this, "无法打开文件选择器：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Uri uri = data.getData();
            String displayName = queryDisplayName(uri);
            if (displayName == null) displayName = "mod_" + System.currentTimeMillis() + ".jar";
            if (!displayName.toLowerCase().endsWith(".jar")) displayName = displayName + ".jar";
            File target = new File(modsDir, displayName);
            InputStream in = getContentResolver().openInputStream(uri);
            FileOutputStream out = new FileOutputStream(target);
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            out.flush();
            out.close();
            Toast.makeText(this, "已导入：" + displayName + "，正在 AI 汉化…", Toast.LENGTH_SHORT).show();
            translateSingleMod(target);
            refresh();
        } catch (Throwable t) {
            Toast.makeText(this, "导入失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void translateSingleMod(File mod) {
        String raw = stripExt(mod.getName());
        AiTranslate.translateModName(raw, new AiTranslate.Callback() {
            @Override public void onSuccess(String translated) {
                try {
                    File note = new File(modsDir, stripExt(mod.getName()) + ".zh.txt");
                    try (FileOutputStream fos = new FileOutputStream(note)) {
                        String content = "原名：" + mod.getName() + "\n中文：" + translated + "\n导入时间：" + System.currentTimeMillis() + "\n";
                        fos.write(content.getBytes("UTF-8"));
                    }
                    Toast.makeText(ModsManagerActivity.this, "AI 汉化：" + translated, Toast.LENGTH_SHORT).show();
                    refresh();
                } catch (Throwable ignored) {}
            }
            @Override public void onFailed(String err) {
                Toast.makeText(ModsManagerActivity.this, "AI 汉化失败：" + err, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void translateAllMods() {
        try {
            File[] arr = modsDir.listFiles();
            if (arr == null || arr.length == 0) {
                Toast.makeText(this, "当前版本没有模组", Toast.LENGTH_SHORT).show();
                return;
            }
            int count = 0;
            for (File f : arr) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                    File note = new File(modsDir, stripExt(f.getName()) + ".zh.txt");
                    if (note.exists()) continue;
                    translateSingleMod(f);
                    count++;
                }
            }
            Toast.makeText(this, "已对 " + count + " 个模组发起 AI 汉化", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "批量汉化失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openDir() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(modsDir), "resource/folder");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (i.resolveActivity(getPackageManager()) == null) {
                Toast.makeText(this, "目录：" + modsDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } else startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "目录：" + modsDir.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(0, i);
    }

    private class ModAdapter extends RecyclerView.Adapter<ModAdapter.VH> {

        private List<File> data;

        ModAdapter(List<File> data) {
            this.data = data;
        }

        void setData(List<File> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mod_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            try {
                File f = data.get(pos);
                String name = f.getName();
                File note = new File(modsDir, stripExt(name) + ".zh.txt");
                if (note.exists()) {
                    String translated = readFirstLine(note);
                    if (!translated.isEmpty()) {
                        h.title.setText(translated);
                        h.sub.setText(name);
                    } else {
                        h.title.setText(name);
                        h.sub.setText(readAll(note));
                    }
                } else {
                    h.title.setText(name);
                    h.sub.setText(formatSize(f.length()) + " · 等待 AI 汉化");
                }
                h.itemView.setOnClickListener(v -> showModActions(f, note));
            } catch (Throwable ignored) {}
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title;
            TextView sub;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.item_mod_title);
                sub = v.findViewById(R.id.item_mod_sub);
            }
        }
    }

    private void showModActions(File f, File note) {
        String[] items;
        if (note.exists()) items = new String[]{"查看 AI 翻译", "重新 AI 汉化", "删除模组"};
        else items = new String[]{"AI 汉化", "删除模组"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(f.getName())
                .setItems(items, (d, w) -> {
                    switch (w) {
                        case 0:
                            if (note.exists()) showTranslation(note);
                            else translateSingleMod(f);
                            break;
                        case 1:
                            if (note.exists()) translateSingleMod(f);
                            else confirmDelete(f);
                            break;
                        case 2:
                            confirmDelete(f);
                            break;
                    }
                })
                .show();
    }

    private void showTranslation(File note) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("AI 翻译备注")
                .setMessage(readAll(note))
                .setPositiveButton("重新汉化", (d, w) -> {
                    try { note.delete(); } catch (Throwable ignored) {}
                    translateSingleMod(new File(modsDir, stripExt(note.getName().replace(".zh.txt","")) + ".jar"));
                    refresh();
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void confirmDelete(File f) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除模组 " + f.getName() + "？")
                .setPositiveButton("删除", (d, w) -> {
                    try {
                        if (!f.delete()) throw new RuntimeException("无法删除");
                        File note = new File(modsDir, stripExt(f.getName()) + ".zh.txt");
                        try { if (note.exists()) note.delete(); } catch (Throwable ignored) {}
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        refresh();
                    } catch (Throwable t) {
                        Toast.makeText(this, "删除失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static String readAll(File f) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int total = 0;
            while (total < buf.length) {
                int n = fis.read(buf, total, buf.length - total);
                if (n <= 0) break;
                total += n;
            }
            fis.close();
            return new String(buf, 0, total, "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }

    private static String readFirstLine(File f) {
        String s = readAll(f);
        if (s.isEmpty()) return "";
        for (String line : s.split("\n")) {
            if (line.startsWith("中文：")) return line.substring(3).trim();
        }
        return "";
    }

    private static String formatSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / 1024.0 / 1024.0);
    }
}
