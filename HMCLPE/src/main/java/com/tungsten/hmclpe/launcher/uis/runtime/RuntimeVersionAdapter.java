package com.tungsten.hmclpe.launcher.uis.runtime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stardock.launcher.R;
import com.tungsten.hmclpe.launcher.version.VersionInfo;

import java.util.List;

public class RuntimeVersionAdapter extends RecyclerView.Adapter<RuntimeVersionAdapter.Holder> {

    public interface OnPick {
        void onPick(VersionInfo info);
    }

    private final Context context;
    private final List<VersionInfo> items;
    private final OnPick pick;
    private String selectedId;

    public RuntimeVersionAdapter(Context context, List<VersionInfo> items, OnPick pick) {
        this.context = context;
        this.items = items;
        this.pick = pick;
    }

    public void setSelected(String id) {
        selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_runtime_version, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        try {
            VersionInfo v = items.get(position);
            h.name.setText(v.id);
            String extra = v.javaVersion != null ? " · " + v.javaVersion : "";
            h.meta.setText((v.installed ? "已安装" : "未安装") + (v.type != null ? " · " + v.type : "") + extra);
            boolean sel = v.id != null && v.id.equals(selectedId);
            h.root.setSelected(sel);
            h.name.setTextColor(sel ? 0xFF7C4DFF : 0xFFECEFF1);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        View root;
        TextView name, meta;

        Holder(@NonNull View v) {
            super(v);
            root = v;
            name = v.findViewById(R.id.item_runtime_version_name);
            meta = v.findViewById(R.id.item_runtime_version_meta);
            v.setOnClickListener(view -> {
                int pos = getBindingAdapterPosition();
                if (pos >= 0 && pos < items.size() && pick != null) {
                    pick.onPick(items.get(pos));
                }
            });
        }
    }
}
