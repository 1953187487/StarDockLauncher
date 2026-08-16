package com.tungsten.hmclpe.launcher.uis.versions;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.launcher.version.VersionInfo;
import com.tungsten.hmclpe.launcher.version.VersionManager;

import java.util.ArrayList;
import java.util.List;

public class VersionsActivity extends AppCompatActivity {

    private static final String TAG = "VersionsActivity";

    private VersionManager versionManager;
    private RecyclerView recycler;
    private VerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_versions);
        } catch (Throwable t) {
            Log.e(TAG, "setContentView failed", t);
            return;
        }
        try {
            MaterialToolbar tb = findViewById(R.id.versions_toolbar);
            if (tb != null) {
                setSupportActionBar(tb);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
                tb.setNavigationOnClickListener(v -> finish());
            }
        } catch (Throwable t) {
            Log.e(TAG, "toolbar failed", t);
        }
        try {
            versionManager = new VersionManager(getApplicationContext());
        } catch (Throwable t) {
            Log.e(TAG, "version manager failed", t);
        }
        try {
            recycler = findViewById(R.id.versions_list);
            if (recycler != null) {
                recycler.setLayoutManager(new LinearLayoutManager(this));
                adapter = new VerAdapter();
                recycler.setAdapter(adapter);
            }
            MaterialButton btnRefresh = findViewById(R.id.versions_btn_refresh);
            if (btnRefresh != null) {
                btnRefresh.setOnClickListener(v -> refresh());
            }
            MaterialButton btnAdd = findViewById(R.id.versions_btn_add);
            if (btnAdd != null) {
                btnAdd.setOnClickListener(v -> addSample());
            }
        } catch (Throwable t) {
            Log.e(TAG, "bind failed", t);
        }
        refresh();
    }

    private void refresh() {
        try {
            if (versionManager != null) {
                versionManager.scanInstalled();
            }
            List<VersionInfo> data = versionManager == null ? new ArrayList<>() : versionManager.all();
            if (adapter != null) {
                adapter.submit(data);
            }
        } catch (Throwable t) {
            Log.e(TAG, "refresh failed", t);
        }
    }

    private void addSample() {
        try {
            if (versionManager == null) {
                return;
            }
            VersionInfo info = new VersionInfo("1.20.4", "release", false);
            info.url = "https://bmclapi2.bangbang93.com/version/1.20.4.json";
            versionManager.add(info);
            refresh();
        } catch (Throwable t) {
            Log.e(TAG, "addSample failed", t);
        }
    }

    static class VerAdapter extends RecyclerView.Adapter<VerAdapter.VH> {

        private final List<VersionInfo> data = new ArrayList<>();

        void submit(List<VersionInfo> in) {
            data.clear();
            data.addAll(in);
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_version, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            VersionInfo v = data.get(position);
            h.title.setText(v.id == null ? "(未命名)" : v.id);
            h.subtitle.setText((v.installed ? "已安装" : "未安装") + " · " + (v.type == null ? "release" : v.type));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {

            final TextView title;
            final TextView subtitle;

            VH(View v) {
                super(v);
                title = v.findViewById(R.id.item_home_version_title);
                subtitle = v.findViewById(R.id.item_home_version_subtitle);
            }
        }
    }
}
