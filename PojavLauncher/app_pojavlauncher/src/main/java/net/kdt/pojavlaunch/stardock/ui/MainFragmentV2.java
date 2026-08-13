package net.kdt.pojavlaunch.stardock.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.MineButton;
import com.kdt.mcgui.mcAccountSpinner;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * v0.0.5 重写主界面：现代化单页布局
 * - 顶部：启动游戏 + 管理版本
 * - 下方：已下载版本列表（卡片）
 * - 右下：账号头像/登录面板
 */
public class MainFragmentV2 extends Fragment {
    public static final String TAG = "MainFragmentV2";

    private MineButton mPlayButton;
    private Button mManageButton;
    private ListView mVersionsList;
    private TextView mVersionsEmpty;
    private TextView mAccountName;
    private TextView mAccountSubtitle;
    private ImageView mAccountAvatar;
    private mcAccountSpinner mAccountSpinner;
    private VersionAdapter mAdapter;
    private final List<String> mVersions = new ArrayList<>();

    public MainFragmentV2() {
        super(R.layout.fragment_main_v2);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mPlayButton = view.findViewById(R.id.main_play_button);
        mManageButton = view.findViewById(R.id.main_manage_versions_button);
        mVersionsList = view.findViewById(R.id.versions_list);
        mVersionsEmpty = view.findViewById(R.id.versions_empty);
        mAccountName = view.findViewById(R.id.account_name);
        mAccountSubtitle = view.findViewById(R.id.account_subtitle);
        mAccountAvatar = view.findViewById(R.id.account_avatar);
        mAccountSpinner = view.findViewById(R.id.account_spinner);

        mAdapter = new VersionAdapter();
        mVersionsList.setAdapter(mAdapter);

        mPlayButton.setOnClickListener(v -> {
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).launchGame();
            }
        });

        mManageButton.setOnClickListener(v -> {
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).openManageVersions();
            }
        });

        View accountPanel = view.findViewById(R.id.account_panel);
        accountPanel.setOnClickListener(v -> {
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).openAccountPicker();
            }
        });

        mVersionsList.setOnItemClickListener((parent, v, position, id) -> {
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).openVersionEditor(position);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshVersions();
        refreshAccount();
    }

    public void refreshVersions() {
        mVersions.clear();
        LauncherProfiles.load();
        if (LauncherProfiles.mainProfileJson != null && LauncherProfiles.mainProfileJson.profiles != null) {
            for (Map.Entry<String, MinecraftProfile> e : LauncherProfiles.mainProfileJson.profiles.entrySet()) {
                MinecraftProfile prof = e.getValue();
                if (prof != null && prof.lastVersionId != null && !"Unknown".equals(prof.lastVersionId)) {
                    mVersions.add(prof.lastVersionId);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
        mVersionsEmpty.setVisibility(mVersions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void refreshAccount() {
        if (mAccountSpinner == null) return;
        if (mAccountSpinner.getSelectedAccount() != null) {
            String username = mAccountSpinner.getSelectedAccount().username;
            mAccountName.setText(username == null ? "?" : username);
            mAccountSubtitle.setText(getString(R.string.sd_account_logged_in));
        } else {
            mAccountName.setText(R.string.sd_account_login);
            mAccountSubtitle.setText(R.string.sd_offline);
        }
    }

    private class VersionAdapter extends BaseAdapter {
        @Override public int getCount() { return mVersions.size(); }
        @Override public String getItem(int position) { return mVersions.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_centered_textview_large, parent, false);
            }
            TextView tv = (TextView) convertView;
            tv.setText(getItem(position));
            return convertView;
        }
    }
}
