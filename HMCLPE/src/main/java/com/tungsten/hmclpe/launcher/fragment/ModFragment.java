package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tungsten.hmclpe.R;

public class ModFragment extends Fragment {

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
            if (tv != null) tv.setText("模组 / 光影 / 资源包");
            android.widget.TextView body = view.findViewById(R.id.placeholder_body);
            if (body != null) body.setText("Modrinth 全类型搜索：mod / shader / resourcepack / datapack / modpack。\n\n断点续传 + SHA1 校验。\n\n后续版本将把模组 UI 完整迁移到本 Fragment。");
        } catch (Throwable t) {
            android.util.Log.e("ModFragment", "onViewCreated failed", t);
        }
    }
}
