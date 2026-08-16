package com.tungsten.hmclpe.launcher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.ai.AiChatActivity;
import com.tungsten.hmclpe.launcher.setting.SettingNavigation;

public class HomeFragment extends Fragment {

    private MaterialSwitch taowaSwitch;
    private TextView taowaHint;

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
            if (b != null) b.setOnClickListener(v -> startGame());

            View aiCard = view.findViewById(R.id.home_btn_ai_card);
            if (aiCard != null) aiCard.setOnClickListener(v -> openAi());

            View dlCard = view.findViewById(R.id.home_btn_download_card);
            if (dlCard != null) dlCard.setOnClickListener(v -> openDownload());

            taowaSwitch = view.findViewById(R.id.home_switch_multiplayer);
            taowaHint = view.findViewById(R.id.home_multiplayer_hint);

            if (taowaSwitch != null) {
                taowaSwitch.setChecked(com.tungsten.hmclpe.launcher.multiplayer.TaowaPrefs.isEnabled(requireContext()));
                if (taowaHint != null) {
                    taowaHint.setText(com.tungsten.hmclpe.launcher.multiplayer.TaowaPrefs.isEnabled(requireContext())
                            ? "已启用：游戏中悬浮窗会显示淘瓦联机按钮"
                            : "开启后在游戏内悬浮窗显示淘瓦联机菜单");
                }
                taowaSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
                    com.tungsten.hmclpe.launcher.multiplayer.TaowaPrefs.setEnabled(requireContext(), checked);
                    if (taowaHint != null) {
                        taowaHint.setText(checked
                                ? "已启用：游戏中悬浮窗会显示淘瓦联机按钮"
                                : "已关闭：仅在启动器主界面控制联机");
                    }
                    if (checked) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("淘瓦联机已启用")
                                .setMessage("1. 进入 Java 版我的世界，按 ESC → 对局域网开放\n" +
                                        "2. 点击屏幕上的悬浮球 → 菜单 → 联机 → 当房主\n" +
                                        "3. 应用会生成淘瓦连接 ID，发给对方即可加入\n\n" +
                                        "在另一台设备：悬浮球 → 联机 → 当玩家，输入淘瓦 ID 即可加入。")
                                .setPositiveButton("好的", null)
                                .show();
                    }
                });
            }
        } catch (Throwable t) {
            android.util.Log.e("HomeFragment", "onViewCreated failed", t);
        }
    }

    private void startGame() {
        try {
            android.widget.Toast.makeText(requireContext(), "启动功能已迁移到版本页面，请先在「下载」安装版本", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "启动失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAi() {
        try {
            Intent i = new Intent(requireContext(), AiChatActivity.class);
            i.putExtra("drawer_mode", false);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "AI 启动失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openDownload() {
        try {
            SettingNavigation.openDownloadTab(requireActivity());
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "打开失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
