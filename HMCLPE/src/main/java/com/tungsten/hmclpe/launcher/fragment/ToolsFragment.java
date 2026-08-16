package com.tungsten.hmclpe.launcher.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.stardock.launcher.R;
import com.tungsten.hmclpe.ai.AiProviderManager;

public class ToolsFragment extends Fragment {

    private static final String TAG = "ToolsFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_tools, container, false);
        } catch (Throwable t) {
            Log.e(TAG, "inflate failed", t);
            return null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            TextView status = view.findViewById(R.id.tools_ai_status);
            if (status != null) {
                String name = "未配置";
                try {
                    AiProviderManager mgr = AiProviderManager.get(requireContext());
                    if (mgr.active() != null) {
                        name = mgr.active().displayName();
                    }
                } catch (Throwable ignored) {
                }
                status.setText("当前 AI 提供方：" + name);
            }
        } catch (Throwable t) {
            Log.e(TAG, "bind failed", t);
        }
    }
}
