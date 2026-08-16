package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.setting.SettingNavigation;

public class VersionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_placeholder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            TextView tv = view.findViewById(R.id.placeholder_title);
            if (tv != null) tv.setText("版本");
            TextView body = view.findViewById(R.id.placeholder_body);
            if (body != null) body.setText("游戏版本下载入口已迁移到「下载中心 → 游戏版本」。\n\n点此跳转 →");
            body.setOnClickListener(v -> SettingNavigation.openDownloadTab(requireActivity()));
            view.setOnClickListener(v -> SettingNavigation.openDownloadTab(requireActivity()));
        } catch (Throwable t) {
            android.util.Log.e("VersionFragment", "init failed", t);
        }
    }
}
