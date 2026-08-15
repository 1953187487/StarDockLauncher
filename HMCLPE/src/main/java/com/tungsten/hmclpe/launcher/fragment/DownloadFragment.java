package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tungsten.hmclpe.R;

public class DownloadFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_placeholder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            android.widget.TextView tv = view.findViewById(R.id.placeholder_title);
            if (tv != null) tv.setText("下载中心");
            android.widget.TextView body = view.findViewById(R.id.placeholder_body);
            if (body != null) body.setText("MC 版本、Forge、Fabric、Quilt、OptiFine、LiteLoader 镜像全部走 BMCLAPI。\n\n失败时会自动重试 + 镜像降级。\n\n后续版本将把下载 UI 完整迁移到本 Fragment。");
        } catch (Throwable t) {
            android.util.Log.e("DownloadFragment", "onViewCreated failed", t);
        }
    }
}
