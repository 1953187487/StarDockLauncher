package net.kdt.pojavlaunch.stardock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

/**
 * 下载中心 5 tab 容器：版本 / 模组 / 光影 / 存档 / 资源包
 */
public class DownloadCenterV2Fragment extends Fragment {
    public static final String TAG = "DownloadCenterV2Fragment";
    public static final int TAB_VERSIONS = 0;
    public static final int TAB_MODS = 1;
    public static final int TAB_SHADERS = 2;
    public static final int TAB_WORLDS = 3;
    public static final int TAB_RESOURCEPACKS = 4;

    private static final String ARG_TAB = "tab";

    public static DownloadCenterV2Fragment newInstance(int tab) {
        DownloadCenterV2Fragment f = new DownloadCenterV2Fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB, tab);
        f.setArguments(args);
        return f;
    }

    private int mTab = TAB_VERSIONS;

    public DownloadCenterV2Fragment() {
        super(R.layout.fragment_download_tab_placeholder);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTab = getArguments().getInt(ARG_TAB, TAB_VERSIONS);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView title = view.findViewById(R.id.tab_title);
        TextView desc = view.findViewById(R.id.tab_desc);
        int[] titles = {R.string.sd_tab_versions, R.string.sd_tab_mods, R.string.sd_tab_shaders, R.string.sd_tab_worlds, R.string.sd_tab_resourcepacks};
        String[] descs = {
                "从官方镜像下载 Minecraft Java 版版本，支持任意版本选择。",
                "从 Modrinth 浏览与下载社区模组。",
                "下载与启用光影包 (Iris / OptiFine / BSL 等)。",
                "下载与导入玩家分享的存档世界。",
                "下载高分辨率资源包与材质包。"
        };
        title.setText(titles[mTab]);
        desc.setText(descs[mTab]);
    }
}
