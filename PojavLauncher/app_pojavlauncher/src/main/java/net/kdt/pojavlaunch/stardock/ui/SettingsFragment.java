package net.kdt.pojavlaunch.stardock.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;
import net.kdt.pojavlaunch.agreement.AgreementDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen — fully redesigned for StarDockLauncher.
 * Lists core settings and forwards to Pojav's internal configuration screens
 * (multirt, controls) where unavoidable.
 */
public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        super(R.layout.fragment_settings_stardock);
    }

    private final List<SettingItem> mItems = new ArrayList<>();
    private SettingAdapter mAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ListView list = view.findViewById(R.id.settings_list);
        Button aboutButton = view.findViewById(R.id.settings_about_button);

        mItems.clear();
        mItems.add(new SettingItem(R.drawable.ic_setting_java_runtime, "Java 运行时",
                "管理多版本 Java 运行时", SettingItem.ACTION_MULTIRT));
        mItems.add(new SettingItem(R.drawable.ic_menu_custom_controls, "控制布局",
                "自定义虚拟按键与键位", SettingItem.ACTION_CONTROLS));
        mItems.add(new SettingItem(R.drawable.ic_download, "检查更新",
                "在应用内检查并下载新版本", SettingItem.ACTION_UPDATE));
        mItems.add(new SettingItem(R.drawable.ic_agreement, "用户协议 / 开源协议",
                "查看使用须知与开源许可证", SettingItem.ACTION_AGREEMENT));

        mAdapter = new SettingAdapter(requireContext(), mItems);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, v, position, id) -> handleAction(mItems.get(position).action));

        aboutButton.setOnClickListener(v -> showAboutDialog());
    }

    private void handleAction(int action) {
        if (getActivity() == null) return;
        switch (action) {
            case SettingItem.ACTION_MULTIRT: {
                MultiRTConfigDialog dialog = new MultiRTConfigDialog();
                dialog.prepare(requireContext(), null);
                dialog.show();
                break;
            }
            case SettingItem.ACTION_CONTROLS: {
                startActivity(new Intent(requireContext(), CustomControlsActivity.class));
                break;
            }
            case SettingItem.ACTION_UPDATE: {
                if (getActivity() instanceof LauncherActivity) {
                    ((LauncherActivity) getActivity()).triggerUpdateCheck();
                }
                break;
            }
            case SettingItem.ACTION_AGREEMENT: {
                AgreementDialog.showOpenSourceLicense(requireActivity());
                break;
            }
        }
    }

    private void showAboutDialog() {
        if (getActivity() == null) return;
        String version;
        try {
            version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "0.0.4";
        }
        String content =
                "StarDockLauncher\n" +
                "应用版本：v" + version + "\n\n" +
                "──────────────────\n" +
                "【用户使用协议】\n" +
                "首次启动已完整展示用户须知与使用协议。\n" +
                "您可以随时在「用户协议 / 开源协议」入口查看完整内容。\n\n" +
                "【开源协议 / 内核协议】\n" +
                "本应用基于 PojavLauncher（MIT License）二次开发，\n" +
                "并借鉴 ZalithLauncher 1（GPL v3）的 UI 模式与双栏布局。\n" +
                "前端 UI 原创；后端启动内核为 PojavLauncher。\n" +
                "  PojavLauncher: https://github.com/PojavLauncherTeam/PojavLauncher\n" +
                "  ZalithLauncher: https://github.com/ZalithLauncher/ZalithLauncher\n" +
                "本项目协议：\n" +
                "  https://github.com/1953187487/StarDockLauncher\n\n" +
                "【免责声明】\n" +
                "本项目与 Mojang / Microsoft 无任何关联。\n" +
                "《Minecraft》相关商标归其各自权利人所有。";
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.preference_about_title)
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton("查看 LICENSE", (d, w) ->
                        Tools.openURL(requireActivity(),
                                "https://github.com/1953187487/StarDockLauncher/blob/main/LICENSE"))
                .show();
    }

    private static class SettingItem {
        static final int ACTION_MULTIRT = 1;
        static final int ACTION_CONTROLS = 2;
        static final int ACTION_UPDATE = 3;
        static final int ACTION_AGREEMENT = 4;

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

    private static class SettingAdapter extends BaseAdapter {
        private final android.content.Context mContext;
        private final List<SettingItem> mItems;

        SettingAdapter(android.content.Context context, List<SettingItem> items) {
            mContext = context;
            mItems = items;
        }

        @Override public int getCount() { return mItems.size(); }
        @Override public SettingItem getItem(int position) { return mItems.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_settings_stardock, parent, false);
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
