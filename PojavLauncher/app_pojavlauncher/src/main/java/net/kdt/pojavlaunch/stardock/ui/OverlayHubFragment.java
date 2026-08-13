package net.kdt.pojavlaunch.stardock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

/**
 * v0.0.5 游戏内悬浮窗控制面板 — 现代化重写
 * 入口：游戏内点击悬浮窗按钮 → 弹出此面板
 * 包含：联机快捷 / 设置快捷 / 截图 / 退出游戏
 */
public class OverlayHubFragment extends Fragment {
    public static final String TAG = "OverlayHubFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_overlay_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View multiplayerEntry = view.findViewById(R.id.overlay_multiplayer);
        View settingsEntry = view.findViewById(R.id.overlay_settings);
        View screenshotEntry = view.findViewById(R.id.overlay_screenshot);
        View exitEntry = view.findViewById(R.id.overlay_exit);

        multiplayerEntry.setOnClickListener(v -> showMultiplayerOverlay());
        settingsEntry.setOnClickListener(v -> android.widget.Toast.makeText(requireContext(), "设置快捷入口", android.widget.Toast.LENGTH_SHORT).show());
        screenshotEntry.setOnClickListener(v -> android.widget.Toast.makeText(requireContext(), "截图", android.widget.Toast.LENGTH_SHORT).show());
        exitEntry.setOnClickListener(v -> requireActivity().finish());
    }

    private void showMultiplayerOverlay() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_overlay_multiplayer, null);
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("联机")
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
