package net.kdt.pojavlaunch.stardock.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * v0.0.8 主界面 — 完全自绘，不依赖 PojavLauncher 的 mcVersionSpinner/mcAccountSpinner/MineButton
 *
 * 左栏：账号卡 + 关于 + 分享日志 + 打开目录 + 安装 jar
 * 右栏：版本卡 + 管理按钮 + 启动游戏大按钮（自绘渐变）
 *
 * 账号 / 版本 / 启动游戏全部直接调用 PojavLauncher 引擎。
 */
public class MainFragmentV4 extends Fragment {
    public static final String TAG = "MainFragmentV4";

    private TextView mAccountName;
    private TextView mAccountSub;
    private ImageView mAccountAvatar;
    private TextView mVersionName;
    private TextView mVersionStatus;
    private Button mPlayBtn;
    private Button mManageBtn;

    /** PojavLauncher 引擎内的账户缓存 */
    private final List<String> mAccounts = new ArrayList<>();

    private final ExtraListener<JMinecraftVersionList> mReleaseListener = (key, value) -> {
        if (value == null || value.versions == null) return false;
        return false;
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_v4, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mAccountName = view.findViewById(R.id.main_account_name);
        mAccountSub = view.findViewById(R.id.main_account_sub);
        mAccountAvatar = view.findViewById(R.id.main_account_avatar);
        mVersionName = view.findViewById(R.id.main_version_name);
        mVersionStatus = view.findViewById(R.id.main_version_status);
        mPlayBtn = view.findViewById(R.id.main_play_btn);
        mManageBtn = view.findViewById(R.id.main_manage_btn);

        View accountCard = view.findViewById(R.id.main_account_card);
        View aboutBtn = view.findViewById(R.id.main_menu_about);
        View shareLogsBtn = view.findViewById(R.id.main_menu_share_logs);
        View openDirBtn = view.findViewById(R.id.main_menu_open_dir);
        View installJarBtn = view.findViewById(R.id.main_menu_install_jar);

        accountCard.setOnClickListener(v -> openAccountPicker());
        aboutBtn.setOnClickListener(v -> openAbout());
        shareLogsBtn.setOnClickListener(v -> Tools.shareLog(requireContext()));
        openDirBtn.setOnClickListener(v -> openGameDir());
        installJarBtn.setOnClickListener(v -> {
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).installJar();
            }
        });

        mManageBtn.setOnClickListener(v -> {
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).openManageVersions();
            }
        });

        mPlayBtn.setOnClickListener(v -> launchGame());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshVersion();
        refreshAccount();
    }

    @Override
    public void onDestroyView() {
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.RELEASE_TABLE, mReleaseListener);
        super.onDestroyView();
    }

    /** 启动游戏（核心引擎调用） */
    private void launchGame() {
        if (getActivity() == null) return;
        LauncherActivity activity = (LauncherActivity) getActivity();
        activity.launchGame();
    }

    /** 刷新当前版本显示 */
    public void refreshVersion() {
        LauncherProfiles.load();
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
        MinecraftProfile prof = null;
        if (LauncherProfiles.mainProfileJson != null && LauncherProfiles.mainProfileJson.profiles != null) {
            prof = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        }
        if (prof == null || prof.lastVersionId == null || "Unknown".equals(prof.lastVersionId)) {
            mVersionName.setText(R.string.sd_v6_no_version);
            mVersionStatus.setText("点击「管理」下载或选择版本");
        } else {
            mVersionName.setText(prof.lastVersionId);
            mVersionStatus.setText("已就绪");
        }
    }

    /** 刷新账户显示 */
    public void refreshAccount() {
        loadAccountsFromStorage();
        if (!mAccounts.isEmpty()) {
            String first = mAccounts.get(0);
            String username = first.split("\\|")[0];
            mAccountName.setText(username);
            mAccountSub.setText("已登录 · " + mAccounts.size() + " 个账户");
            mAccountAvatar.setImageResource(R.drawable.avatar_steve);
        } else {
            mAccountName.setText("未登录");
            mAccountSub.setText("点击登录");
            mAccountAvatar.setImageResource(R.drawable.avatar_steve);
        }
    }

    /** 从 PojavLauncher 存储读取账户列表 */
    private void loadAccountsFromStorage() {
        mAccounts.clear();
        try {
            File accountsDir = new File(Tools.DIR_ACCOUNT_NEW);
            if (accountsDir.isDirectory()) {
                for (File f : accountsDir.listFiles()) {
                    if (f.getName().endsWith(".json")) {
                        String username = f.getName().replace(".json", "");
                        mAccounts.add(username + "|local");
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** 打开账户选择：跳到 SelectAuthFragment（v0.0.8 内置 Microsoft + 离线登录） */
    private void openAccountPicker() {
        if (getActivity() instanceof LauncherActivity) {
            ((LauncherActivity) getActivity()).openAccountPicker();
        }
    }

    /** 关于应用 */
    private void openAbout() {
        if (getActivity() instanceof LauncherActivity) {
            ((LauncherActivity) getActivity()).showAboutDialog();
        }
    }

    /** 打开游戏目录 */
    private void openGameDir() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        File dir;
        if (Tools.isValidString(currentProfile)) {
            LauncherProfiles.load();
            MinecraftProfile prof = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
            dir = prof == null ? new File(Tools.DIR_GAME_NEW) : Tools.getGameDirPath(prof);
        } else {
            dir = new File(Tools.DIR_GAME_NEW);
        }
        Tools.openPath(requireContext(), dir, false);
    }
}
