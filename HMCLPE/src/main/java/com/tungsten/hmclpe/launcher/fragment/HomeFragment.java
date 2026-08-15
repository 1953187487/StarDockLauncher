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

public class HomeFragment extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            MaterialButton b = view.findViewById(R.id.home_btn_play);
            if (b != null) b.setOnClickListener(this);
            MaterialButton ai = view.findViewById(R.id.home_btn_ai_card);
            if (ai != null) ai.setOnClickListener(this);
        } catch (Throwable t) {
            android.util.Log.e("HomeFragment", "onViewCreated failed", t);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.home_btn_ai_card) {
            try {
                android.content.Intent i = new android.content.Intent(requireContext(), com.tungsten.hmclpe.ai.AiChatActivity.class);
                i.putExtra("drawer_mode", false);
                startActivity(i);
            } catch (Throwable t) {
                Toast.makeText(requireContext(), "AI 启动失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
