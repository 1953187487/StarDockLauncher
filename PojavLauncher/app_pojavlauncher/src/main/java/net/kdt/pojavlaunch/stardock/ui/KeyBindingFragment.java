package net.kdt.pojavlaunch.stardock.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * v0.0.5 键位系统
 * - 5 个键位预设（战斗 / 建筑 / 极简 / PVP / 红石）
 * - 自定义键位编辑器入口（复用 PojavLauncher 内置 CustomControlsActivity）
 * - 导入 / 分享 / 重置
 */
public class KeyBindingFragment extends Fragment {
    public static final String TAG = "KeyBindingFragment";
    public static final String PREFS_NAME = "stardock_keybinding";
    public static final String KEY_PRESET = "active_preset";

    private final List<PresetItem> mPresets = new ArrayList<>();
    private PresetAdapter mAdapter;

    private final ActivityResultLauncher<String[]> mKeyFilePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) importKeyBinding(uri); }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_keybinding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ListView list = view.findViewById(R.id.keybinding_preset_list);
        Button editorBtn = view.findViewById(R.id.keybinding_open_editor);
        Button importBtn = view.findViewById(R.id.keybinding_import);
        Button shareBtn = view.findViewById(R.id.keybinding_share);
        Button resetBtn = view.findViewById(R.id.keybinding_reset);

        mPresets.clear();
        mPresets.add(new PresetItem(R.drawable.ic_menu_custom_controls, getString(R.string.sd_keybinding_preset_combat), "适合生存、PVE、副本", "4x4"));
        mPresets.add(new PresetItem(R.drawable.ic_setting_misc, getString(R.string.sd_keybinding_preset_build), "适合创造、建筑", "5x5"));
        mPresets.add(new PresetItem(R.drawable.ic_setting_video, getString(R.string.sd_keybinding_preset_minimal), "适合老旧设备、视野开阔", "3x3"));
        mPresets.add(new PresetItem(R.drawable.ic_setting_gesture_time, getString(R.string.sd_keybinding_preset_pvp), "竞技优化按键排布", "4x4-fast"));
        mPresets.add(new PresetItem(R.drawable.ic_setting_hash_verification, getString(R.string.sd_keybinding_preset_redstone), "红石快速调试布局", "5x5-tech"));

        mAdapter = new PresetAdapter();
        list.setAdapter(mAdapter);

        int active = getActivePreset();
        mAdapter.setActiveIndex(active);
        list.setOnItemClickListener((parent, v, position, id) -> showPresetDetail(position));

        editorBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(requireContext(), CustomControlsActivity.class));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "打开键位编辑器失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        importBtn.setOnClickListener(v -> mKeyFilePicker.launch(new String[]{"*/*"}));
        shareBtn.setOnClickListener(v -> exportKeyBinding());
        resetBtn.setOnClickListener(v -> confirmReset());
    }

    private void showPresetDetail(int position) {
        PresetItem item = mPresets.get(position);
        new AlertDialog.Builder(requireContext())
                .setTitle(item.title)
                .setMessage(item.summary + "\n\n布局：" + item.layout + "\n\n应用后重启游戏生效。")
                .setPositiveButton(R.string.sd_keybinding_apply_preset, (d, w) -> {
                    setActivePreset(position);
                    mAdapter.setActiveIndex(position);
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "已应用：" + item.title, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sd_keybinding_reset)
                .setMessage("恢复默认键位将清除所有自定义布局，确定继续？")
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    File ctrlDir = new File(Tools.CTRLDEF_FILE).getParentFile();
                    if (ctrlDir != null && ctrlDir.isDirectory()) {
                        for (File f : ctrlDir.listFiles()) {
                            if (f.getName().endsWith(".json")) f.delete();
                        }
                    }
                    SharedPreferences sp = requireContext().getSharedPreferences(PREFS_NAME, 0);
                    sp.edit().clear().apply();
                    Toast.makeText(requireContext(), "已恢复默认", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void importKeyBinding(android.net.Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
                count++;
            }
            Tools.dialog(requireContext(), "导入成功",
                    "已读取 " + count + " 行键位配置。\n\n重启游戏后生效。");
        } catch (Exception e) {
            Toast.makeText(requireContext(), "导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportKeyBinding() {
        File ctrl = new File(Tools.CTRLDEF_FILE);
        if (!ctrl.exists()) {
            Toast.makeText(requireContext(), "当前键位文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
        share.setType("application/json");
        share.putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(ctrl));
        try {
            startActivity(android.content.Intent.createChooser(share, "分享键位"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int getActivePreset() {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS_NAME, 0);
        return sp.getInt(KEY_PRESET, 0);
    }

    private void setActivePreset(int idx) {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS_NAME, 0);
        sp.edit().putInt(KEY_PRESET, idx).apply();
    }

    private static class PresetItem {
        final int iconRes;
        final String title;
        final String summary;
        final String layout;
        PresetItem(int iconRes, String title, String summary, String layout) {
            this.iconRes = iconRes;
            this.title = title;
            this.summary = summary;
            this.layout = layout;
        }
    }

    private class PresetAdapter extends BaseAdapter {
        private int activeIndex = 0;
        void setActiveIndex(int idx) { activeIndex = idx; }

        @Override public int getCount() { return mPresets.size(); }
        @Override public PresetItem getItem(int position) { return mPresets.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_settings_stardock, parent, false);
            }
            PresetItem item = getItem(position);
            ImageView icon = convertView.findViewById(R.id.setting_item_icon);
            TextView title = convertView.findViewById(R.id.setting_item_title);
            TextView summary = convertView.findViewById(R.id.setting_item_summary);
            icon.setImageResource(item.iconRes);
            title.setText(item.title + (position == activeIndex ? "  ✓" : ""));
            summary.setText(item.summary + " · " + item.layout);
            return convertView;
        }
    }
}
