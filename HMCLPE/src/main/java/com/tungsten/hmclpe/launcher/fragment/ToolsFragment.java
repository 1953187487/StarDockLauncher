package com.tungsten.hmclpe.launcher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.control.ControlPatternActivity;
import com.tungsten.hmclpe.launcher.multiplayer.TaowaPrefs;
import com.tungsten.hmclpe.launcher.uis.multiplayer.MultiplayerActivity;

public class ToolsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tools, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialSwitch taowaSwitch = view.findViewById(R.id.tools_taowa_switch);
        MaterialButton taowaEnter = view.findViewById(R.id.tools_taowa_enter);
        MaterialButton keymapEdit = view.findViewById(R.id.tools_keymap_edit);
        MaterialButton keymapReset = view.findViewById(R.id.tools_keymap_reset);
        MaterialButton keymapAi = view.findViewById(R.id.tools_keymap_ai);

        taowaSwitch.setChecked(TaowaPrefs.isEnabled(requireContext()));
        taowaSwitch.setOnCheckedChangeListener((b, on) -> {
            TaowaPrefs.setEnabled(requireContext(), on);
            Toast.makeText(requireContext(), on ? "淘瓦联机已开启" : "淘瓦联机已关闭", Toast.LENGTH_SHORT).show();
        });

        taowaEnter.setOnClickListener(v -> {
            if (!TaowaPrefs.isEnabled(requireContext())) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("开启淘瓦联机")
                        .setMessage("淘瓦联机需先开启，开启后才能进入联机页面。")
                        .setPositiveButton("开启", (d, w) -> {
                            TaowaPrefs.setEnabled(requireContext(), true);
                            taowaSwitch.setChecked(true);
                            openMultiplayer();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return;
            }
            openMultiplayer();
        });

        keymapEdit.setOnClickListener(v -> {
            try {
                Intent i = new Intent(requireContext(), ControlPatternActivity.class);
                startActivity(i);
            } catch (Throwable t) {
                Toast.makeText(requireContext(), "打开键位编辑器失败", Toast.LENGTH_SHORT).show();
            }
        });

        keymapReset.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("还原为 MC 移动版默认键位")
                    .setMessage("将加载《我的世界》移动版（网易版）默认键位布局。\n\n确定要应用吗？")
                    .setPositiveButton("应用", (d, w) -> {
                        try {
                            String json = com.tungsten.hmclpe.utils.file.FileStringUtils.getStringFromFile(
                                    com.tungsten.hmclpe.manifest.AppManifest.SETTING_DIR + "/control_pattern/mc_mobile.json");
                            if (json == null) {
                                com.tungsten.hmclpe.utils.file.FileStringUtils.writeFile(
                                        com.tungsten.hmclpe.manifest.AppManifest.SETTING_DIR + "/control_pattern/mc_mobile.json",
                                        McMobileKeymap.JSON);
                            }
                            Toast.makeText(requireContext(), "已应用 MC 移动版默认键位", Toast.LENGTH_SHORT).show();
                        } catch (Throwable t) {
                            Toast.makeText(requireContext(), "应用失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        keymapAi.setOnClickListener(v -> {
            final View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ai_keymap_input, null);
            final TextInputEditText input = dv.findViewById(R.id.ai_keymap_input);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("AI 生成键位描述")
                    .setView(dv)
                    .setPositiveButton("生成", (d, w) -> {
                        String desc = input.getText() == null ? "" : input.getText().toString().trim();
                        if (desc.isEmpty()) {
                            Toast.makeText(requireContext(), "请输入键位描述（如：FPS 模式 8 个按钮 + 摇杆）", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        com.tungsten.hmclpe.ai.AiTester.test(
                                com.tungsten.hmclpe.launcher.setting.AppPrefs.getString(requireContext(), com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_BASE_URL, "https://api.hcnsec.cn/v1"),
                                com.tungsten.hmclpe.launcher.setting.AppPrefs.getString(requireContext(), com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_API_KEY, "sk-rs6nsUU370qLPyuy9PzPAduH5ITwlgEv"),
                                com.tungsten.hmclpe.launcher.setting.AppPrefs.getString(requireContext(), com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_MODEL, "auto"),
                                "你是 Minecraft 移动版键位设计师。请基于如下描述生成一份键位 JSON 描述：" + desc,
                                new com.tungsten.hmclpe.ai.AiTester.Callback() {
                                    @Override public void onSuccess(String response) {
                                        requireActivity().runOnUiThread(() -> new MaterialAlertDialogBuilder(requireContext())
                                                .setTitle("AI 键位建议")
                                                .setMessage(response)
                                                .setPositiveButton("知道了", null)
                                                .show());
                                    }
                                    @Override public void onFailed(String error) {
                                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "生成失败：" + error, Toast.LENGTH_SHORT).show());
                                    }
                                });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void openMultiplayer() {
        try {
            Intent i = new Intent(requireContext(), MultiplayerActivity.class);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "启动淘瓦联机失败", Toast.LENGTH_SHORT).show();
        }
    }
}
