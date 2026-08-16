package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.setting.SettingNavigation;
import com.tungsten.hmclpe.launcher.setting.VersionManager;

import java.io.File;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            MaterialButton play = view.findViewById(R.id.home_btn_play);
            MaterialButton download = view.findViewById(R.id.home_btn_download_card);
            MaterialButton tools = view.findViewById(R.id.home_btn_tools_card);

            if (play != null) play.setOnClickListener(v -> startGame());
            if (download != null) download.setOnClickListener(v -> SettingNavigation.openDownloadTab(requireActivity()));
            if (tools != null) tools.setOnClickListener(v -> SettingNavigation.openToolsTab(requireActivity()));
        } catch (Throwable t) {
            android.util.Log.e("HomeFragment", "onViewCreated failed", t);
        }
    }

    private void startGame() {
        try {
            File root = VersionManager.root();
            if (!root.exists() || root.listFiles() == null || root.listFiles().length == 0) {
                Toast.makeText(requireContext(), "未安装游戏版本，请先到「下载」安装", Toast.LENGTH_SHORT).show();
                SettingNavigation.openDownloadTab(requireActivity());
                return;
            }
            SettingNavigation.openToolsTab(requireActivity());
            Toast.makeText(requireContext(), "请到「工具」页启动游戏", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "启动失败", Toast.LENGTH_SHORT).show();
        }
    }
}
