package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tungsten.hmclpe.R;

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
            android.widget.TextView tv = view.findViewById(R.id.placeholder_title);
            if (tv != null) tv.setText("版本管理");
            android.widget.TextView body = view.findViewById(R.id.placeholder_body);
            if (body != null) body.setText("在此查看已安装的游戏版本。\n\nv1.0.5 已重写启动参数构建逻辑，启动游戏失败会给出具体错误。\n\n后续版本将把版本管理 UI 完整迁移到本 Fragment。");
        } catch (Throwable t) {
            android.util.Log.e("VersionFragment", "onViewCreated failed", t);
        }
    }
}
