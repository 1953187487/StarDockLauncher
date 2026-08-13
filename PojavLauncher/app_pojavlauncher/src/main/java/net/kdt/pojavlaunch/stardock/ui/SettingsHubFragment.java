package net.kdt.pojavlaunch.stardock.ui;

import android.content.Intent;
import android.net.Uri;
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
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.agreement.AgreementDialog;
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.0.5 设置中心：主题颜色 / 字体导入 / 全局布局 / 关于 / 检查更新
 */
public class SettingsHubFragment extends Fragment {
    public static final String TAG = "SettingsHubFragment";

    private final ActivityResultLauncher<String[]> mFontPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) handleFontPick(uri); }
    );

    private final List<SettingItem> mItems = new ArrayList<>();
    private SettingAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ListView list = view.findViewById(R.id.settings_list);

        mItems.clear();
        mItems.add(new SettingItem(R.drawable.ic_setting_engine, "主题颜色", "调节启动器全局主题色", SettingItem.ACTION_THEME));
        mItems.add(new SettingItem(R.drawable.ic_setting_misc, "字体导入", "导入 .ttf / .otf 自定义字体", SettingItem.ACTION_FONT));
        mItems.add(new SettingItem(R.drawable.ic_setting_video, "全局布局", "调节启动器全局布局参数", SettingItem.ACTION_LAYOUT));
        mItems.add(new SettingItem(R.drawable.ic_download, "检查更新", "检测并下载最新版本", SettingItem.ACTION_UPDATE));
        mItems.add(new SettingItem(R.drawable.ic_setting_java_runtime, "Java 运行时", "管理 Java 多版本运行时", SettingItem.ACTION_MULTIRT));
        mItems.add(new SettingItem(R.drawable.ic_menu_custom_controls, "控制布局", "自定义虚拟按键", SettingItem.ACTION_CONTROLS));
        mItems.add(new SettingItem(R.drawable.ic_agreement, "用户协议", "查看 v0.0.5 协议与开源信息", SettingItem.ACTION_AGREEMENT));
        mItems.add(new SettingItem(R.drawable.ic_menu_custom_controls, "游戏内悬浮窗", "查看现代化悬浮窗面板预览", SettingItem.ACTION_OVERLAY));
        mItems.add(new SettingItem(R.drawable.ic_setting_misc, "开源与项目源码", "查看本项目与依赖项目链接", SettingItem.ACTION_OPEN_SOURCE));
        mItems.add(new SettingItem(R.drawable.ic_sharp_settings_24, "关于此项目", "StarDockLauncher v0.0.5", SettingItem.ACTION_ABOUT));

        mAdapter = new SettingAdapter();
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, v, position, id) -> handleAction(mItems.get(position).action));
    }

    private void handleAction(int action) {
        if (getActivity() == null) return;
        switch (action) {
            case SettingItem.ACTION_THEME:
                showThemeDialog();
                break;
            case SettingItem.ACTION_FONT:
                mFontPicker.launch(new String[]{"*/*"});
                break;
            case SettingItem.ACTION_LAYOUT:
                showLayoutDialog();
                break;
            case SettingItem.ACTION_UPDATE:
                if (getActivity() instanceof LauncherActivity) {
                    ((LauncherActivity) getActivity()).triggerUpdateCheck();
                }
                break;
            case SettingItem.ACTION_MULTIRT: {
                MultiRTConfigDialog dialog = new MultiRTConfigDialog();
                dialog.prepare(requireContext(), null);
                dialog.show();
                break;
            }
            case SettingItem.ACTION_CONTROLS:
                startActivity(new Intent(requireContext(), CustomControlsActivity.class));
                break;
            case SettingItem.ACTION_AGREEMENT:
                AgreementDialog.showOpenSourceLicense(requireActivity());
                break;
            case SettingItem.ACTION_OVERLAY:
                Tools.swapFragment(requireActivity(), OverlayHubFragment.class, OverlayHubFragment.TAG, null);
                break;
            case SettingItem.ACTION_OPEN_SOURCE:
                showOpenSourceDialog();
                break;
            case SettingItem.ACTION_ABOUT:
                showAboutDialog();
                break;
        }
    }

    private void showThemeDialog() {
        final String[] names = {"默认绿", "星空蓝", "暗夜紫", "琥珀橙", "玫瑰红"};
        final int[] colors = {
                R.color.brand_primary,
                R.color.brand_primary_blue,
                R.color.brand_primary_purple,
                R.color.brand_primary_amber,
                R.color.brand_primary_rose
        };
        new AlertDialog.Builder(requireContext())
                .setTitle("主题颜色")
                .setItems(names, (d, w) -> {
                    Toast.makeText(requireContext(), "已选择：" + names[w], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showLayoutDialog() {
        final String[] opts = {"紧凑", "标准", "宽松"};
        new AlertDialog.Builder(requireContext())
                .setTitle("全局布局")
                .setItems(opts, (d, w) -> {
                    Toast.makeText(requireContext(), "已选择：" + opts[w], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAboutDialog() {
        String version;
        try {
            version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "0.0.5";
        }
        String content =
                "StarDockLauncher v" + version + "\n\n" +
                "现代化 Minecraft Java 版 Android 启动器\n\n" +
                "本项目以 PojavLauncher (MIT License) 为后端启动内核，\n" +
                "前端 UI 与交互由本项目独立设计与实现。\n\n" +
                "项目仓库：\n  https://github.com/1953187487/StarDockLauncher\n\n" +
                "与 Mojang / Microsoft 无任何官方关联。";
        new AlertDialog.Builder(requireContext())
                .setTitle("关于此项目")
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton("查看 GitHub", (d, w) -> Tools.openURL(requireActivity(),
                        "https://github.com/1953187487/StarDockLauncher"))
                .show();
    }

    private void showOpenSourceDialog() {
        String content =
                "本项目源码：\n" +
                "  https://github.com/1953187487/StarDockLauncher\n\n" +
                "──────────────────\n" +
                "依赖 / 内核项目：\n" +
                "  • PojavLauncher (MIT)\n" +
                "    https://github.com/PojavLauncherTeam/PojavLauncher\n" +
                "  • LWJGL3 (BSD-3)\n" +
                "    https://github.com/LWJGL/lwjgl3\n\n" +
                "前端 UI、交互、悬浮窗、协议全部由本项目原创实现。";
        new AlertDialog.Builder(requireContext())
                .setTitle("开源与项目源码")
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton("打开 GitHub", (d, w) -> Tools.openURL(requireActivity(),
                        "https://github.com/1953187487/StarDockLauncher"))
                .show();
    }

    private void handleFontPick(Uri uri) {
        try {
            String name = uri.getLastPathSegment();
            Toast.makeText(requireContext(), "已选择字体：" + name, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "字体导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static class SettingItem {
        static final int ACTION_THEME = 1;
        static final int ACTION_FONT = 2;
        static final int ACTION_LAYOUT = 3;
        static final int ACTION_UPDATE = 4;
        static final int ACTION_MULTIRT = 5;
        static final int ACTION_CONTROLS = 6;
        static final int ACTION_AGREEMENT = 7;
        static final int ACTION_OPEN_SOURCE = 8;
        static final int ACTION_ABOUT = 9;
        static final int ACTION_OVERLAY = 10;

        final int iconRes;
        final String title;
        final String summary;
        final int action;

        SettingItem(int iconRes, String title, String summary, int action) {
            this.iconRes = iconRes;
            this.title = title;
            this.summary = summary;
            this.action = action;
        }
    }

    private class SettingAdapter extends BaseAdapter {
        @Override public int getCount() { return mItems.size(); }
        @Override public SettingItem getItem(int position) { return mItems.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_settings_stardock, parent, false);
            }
            SettingItem item = getItem(position);
            ImageView icon = convertView.findViewById(R.id.setting_item_icon);
            TextView title = convertView.findViewById(R.id.setting_item_title);
            TextView summary = convertView.findViewById(R.id.setting_item_summary);
            icon.setImageResource(item.iconRes);
            title.setText(item.title);
            summary.setText(item.summary);
            return convertView;
        }
    }
}
