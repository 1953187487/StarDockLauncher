package com.tungsten.hmclpe.launcher.uis.mods;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stardock.launcher.R;
import com.tungsten.hmclpe.launcher.mod.ModrinthProject;

import java.util.List;

public class ModrinthAdapter extends RecyclerView.Adapter<ModrinthAdapter.Holder> {

    public interface OnItemClick {
        void onDownload(ModrinthProject project);
    }

    private final Context context;
    private final List<ModrinthProject> items;
    private final OnItemClick click;

    public ModrinthAdapter(Context context, List<ModrinthProject> items, OnItemClick click) {
        this.context = context;
        this.items = items;
        this.click = click;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_modrinth, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        try {
            ModrinthProject p = items.get(position);
            h.title.setText(p.title == null ? "" : p.title);
            h.desc.setText(p.description == null ? "" : p.description);
            String downloads = p.downloads > 10000
                    ? String.format("%.1f万", p.downloads / 10000f)
                    : String.valueOf(p.downloads);
            h.downloads.setText(context.getString(R.string.modrinth_downloads, downloads));
            h.meta.setText(p.getType() + " · " + (p.categories == null ? "" : String.join(" / ", p.categories)));
            h.btn.setOnClickListener(v -> {
                if (click != null) {
                    click.onDownload(p);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView title, desc, downloads, meta;
        View btn;

        Holder(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.item_modrinth_title);
            desc = v.findViewById(R.id.item_modrinth_desc);
            downloads = v.findViewById(R.id.item_modrinth_downloads);
            meta = v.findViewById(R.id.item_modrinth_meta);
            btn = v.findViewById(R.id.item_modrinth_btn_download);
        }
    }
}
