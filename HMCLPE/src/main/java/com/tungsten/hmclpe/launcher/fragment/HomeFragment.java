package com.tungsten.hmclpe.launcher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.launcher.HomeActivity;
import com.tungsten.hmclpe.launcher.uis.login.AccountActivity;
import com.tungsten.hmclpe.launcher.uis.versions.VersionsActivity;
import com.tungsten.hmclpe.launcher.version.VersionInfo;
import com.tungsten.hmclpe.launcher.version.VersionManager;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private VersionManager versionManager;
    private RecyclerView versionList;
    private VersionAdapter adapter;
    private TextView versionLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_home, container, false);
        } catch (Throwable t) {
            Log.e(TAG, "inflate failed", t);
            return null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            versionManager = new VersionManager(requireContext());
        } catch (Throwable t) {
            Log.e(TAG, "VersionManager init failed", t);
        }
        try {
            versionList = view.findViewById(R.id.home_version_list);
            if (versionList != null) {
                versionList.setLayoutManager(new LinearLayoutManager(requireContext()));
                adapter = new VersionAdapter();
                versionList.setAdapter(adapter);
            }
        } catch (Throwable t) {
            Log.e(TAG, "bind list failed", t);
        }
        try {
            versionLabel = view.findViewById(R.id.home_account_name);
        } catch (Throwable t) {
            Log.e(TAG, "bind label failed", t);
        }
        try {
            View card = view.findViewById(R.id.home_account_card);
            if (card != null) {
                card.setOnClickListener(v -> openAccount());
            }
            MaterialButton btnAccount = view.findViewById(R.id.home_btn_account);
            if (btnAccount != null) {
                btnAccount.setOnClickListener(v -> openAccount());
            }
            MaterialButton btnVersions = view.findViewById(R.id.home_btn_versions);
            if (btnVersions != null) {
                btnVersions.setOnClickListener(v -> openVersions());
            }
            MaterialButton btnDownload = view.findViewById(R.id.home_btn_download);
            if (btnDownload != null) {
                btnDownload.setOnClickListener(v -> {
                    if (getActivity() instanceof HomeActivity) {
                        ((HomeActivity) getActivity()).switchFragment("download");
                    }
                });
            }
        } catch (Throwable t) {
            Log.e(TAG, "bind buttons failed", t);
        }
        try {
            refresh();
        } catch (Throwable t) {
            Log.e(TAG, "refresh failed", t);
        }
    }

    public void refresh() {
        if (versionManager != null) {
            versionManager.scanInstalled();
        }
        List<VersionInfo> data = versionManager == null ? new ArrayList<>() : versionManager.installed();
        if (adapter != null) {
            adapter.submit(data);
        }
    }

    private void openAccount() {
        try {
            startActivity(new Intent(requireContext(), AccountActivity.class));
        } catch (Throwable t) {
            Log.e(TAG, "openAccount failed", t);
        }
    }

    private void openVersions() {
        try {
            startActivity(new Intent(requireContext(), VersionsActivity.class));
        } catch (Throwable t) {
            Log.e(TAG, "openVersions failed", t);
        }
    }

    static class VersionAdapter extends RecyclerView.Adapter<VersionAdapter.VH> {

        private final List<VersionInfo> data = new ArrayList<>();

        void submit(List<VersionInfo> in) {
            data.clear();
            if (in != null) {
                data.addAll(in);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_version, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            VersionInfo v = data.get(position);
            h.title.setText(v.id == null ? "(未命名)" : v.id);
            h.subtitle.setText(v.installed ? "已安装" : "未安装");
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {

            final TextView title;
            final TextView subtitle;

            VH(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.item_home_version_title);
                subtitle = itemView.findViewById(R.id.item_home_version_subtitle);
            }
        }
    }
}
