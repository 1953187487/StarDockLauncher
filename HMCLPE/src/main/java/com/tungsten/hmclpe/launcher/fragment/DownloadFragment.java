package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stardock.launcher.R;

import java.util.ArrayList;
import java.util.List;

public class DownloadFragment extends Fragment {

    private static final String TAG = "DownloadFragment";

    private RecyclerView recycler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_download, container, false);
        } catch (Throwable t) {
            Log.e(TAG, "inflate failed", t);
            return null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            recycler = view.findViewById(R.id.download_list);
            if (recycler != null) {
                recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
                CardAdapter adapter = new CardAdapter();
                adapter.submit(sample());
                recycler.setAdapter(adapter);
            }
        } catch (Throwable t) {
            Log.e(TAG, "bind failed", t);
        }
    }

    private List<String[]> sample() {
        List<String[]> out = new ArrayList<>();
        out.add(new String[]{"Minecraft 正式版", "BMCLAPI · 官方源镜像"});
        out.add(new String[]{"Modrinth 整合包", "Modrinth 官方源"});
        out.add(new String[]{"资源中心", "Mods / 资源包 / 光影"});
        return out;
    }

    static class CardAdapter extends RecyclerView.Adapter<CardAdapter.VH> {

        private final List<String[]> data = new ArrayList<>();

        void submit(List<String[]> in) {
            data.clear();
            data.addAll(in);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            String[] row = data.get(position);
            h.title.setText(row[0]);
            h.subtitle.setText(row[1]);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {

            final android.widget.TextView title;
            final android.widget.TextView subtitle;

            VH(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.item_download_title);
                subtitle = itemView.findViewById(R.id.item_download_subtitle);
            }
        }
    }
}
