package net.kdt.pojavlaunch.stardock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import net.kdt.pojavlaunch.R;

/**
 * v0.0.5 下载中心：5 tab 横向切换
 * - 版本 / 模组 / 光影 / 存档 / 资源包
 */
public class DownloadHubFragment extends Fragment {
    public static final String TAG = "DownloadHubFragment";

    private LinearLayout mTabBar;
    private FragmentContainerView mContainer;
    private final TextView[] mTabViews = new TextView[5];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_download_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mTabBar = view.findViewById(R.id.download_tab_bar);
        mContainer = view.findViewById(R.id.download_tab_container);

        String[] labels = {
                getString(R.string.sd_tab_versions),
                getString(R.string.sd_tab_mods),
                getString(R.string.sd_tab_shaders),
                getString(R.string.sd_tab_worlds),
                getString(R.string.sd_tab_resourcepacks)
        };
        mTabBar.removeAllViews();
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            TextView tv = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            tv.setLayoutParams(lp);
            tv.setPadding(dp(18), dp(10), dp(18), dp(10));
            tv.setText(labels[i]);
            tv.setTextSize(13);
            tv.setTextColor(getResources().getColor(R.color.text_primary, requireContext().getTheme()));
            tv.setBackgroundResource(R.drawable.background_item);
            tv.setOnClickListener(v -> showTab(idx));
            mTabBar.addView(tv);
            mTabViews[i] = tv;
        }

        if (getChildFragmentManager().findFragmentById(R.id.download_tab_container) == null) {
            showTab(0);
        }
        updateTabStyle(0);
    }

    private void showTab(int idx) {
        getChildFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.download_tab_container, DownloadCenterV2Fragment.newInstance(idx), "tab_" + idx)
                .commit();
        updateTabStyle(idx);
    }

    private void updateTabStyle(int active) {
        for (int i = 0; i < 5; i++) {
            TextView tv = mTabViews[i];
            if (tv == null) continue;
            if (i == active) {
                tv.setSelected(true);
            } else {
                tv.setSelected(false);
            }
        }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d);
    }
}
