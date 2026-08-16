package com.tungsten.hmclpe.launcher.uis.mods;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stardock.launcher.R;

import java.io.File;
import java.util.List;

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.Holder> {

    private final Context context;
    private final List<File> items;

    public ModsAdapter(Context context, List<File> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_mod, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        try {
            File f = items.get(position);
            h.name.setText(f.getName());
            h.size.setText(String.format("%.1f KB", f.length() / 1024f));
            String zh = readZh(f);
            h.zh.setText(zh == null ? "未汉化（点击下方 AI 汉化）" : "汉化名：" + zh);
        } catch (Throwable ignored) {
        }
    }

    private String readZh(File mod) {
        try {
            String base = mod.getName();
            int dot = base.lastIndexOf('.');
            String stem = dot > 0 ? base.substring(0, dot) : base;
            File zh = new File(mod.getParentFile(), stem + ".zh.txt");
            if (zh.exists()) {
                String[] lines = new String(java.nio.file.Files.readAllBytes(zh.toPath()), "UTF-8").split("\n");
                for (String line : lines) {
                    if (line.startsWith("mod=")) {
                        return line.substring(4).trim();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView name, size, zh;

        Holder(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.item_mod_name);
            size = v.findViewById(R.id.item_mod_size);
            zh = v.findViewById(R.id.item_mod_zh);
        }
    }
}
