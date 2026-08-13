package net.kdt.pojavlaunch.stardock.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.MineButton;
import com.kdt.mcgui.mcAccountSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;

/**
 * v0.0.6 主界面 — 完全仿 ZalithLauncher 双栏布局
 * - 左栏菜单（含账号卡 + 关于 + 分享日志 + 打开目录 + 安装 jar）
 * - 右栏 play_layout：顶部版本 + 管理按钮，底部启动游戏
 * - 账户默认显示 Steve 头像，未登录显示"未登录"
 */
public class MainFragmentV3 extends Fragment {
    public static final String TAG = "MainFragmentV3";

    private MineButton mPlayButton;
    private ImageButton mManageButton;
    private TextView mVersionName;
    private TextView mVersionInfo;
    private ImageView mAccountAvatar;
    private TextView mAccountName;
    private TextView mAccountSub;
    private View mAccountCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_v3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mPlayButton = view.findViewById(R.id.play_button);
        mManageButton = view.findViewById(R.id.manager_profile_button);
        mVersionName = view.findViewById(R.id.version_name);
        mVersionInfo = view.findViewById(R.id.version_info);
        mAccountAvatar = view.findViewById(R.id.menu_account_avatar);
        mAccountName = view.findViewById(R.id.menu_account_name);
        mAccountSub = view.findViewById(R.id.menu_account_sub);
        mAccountCard = view.findViewById(R.id.menu_account);

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

        mAccountCard.setOnClickListener(v -> openAccountPicker());

        View aboutBtn = view.findViewById(R.id.menu_about);
        View shareLogsBtn = view.findViewById(R.id.menu_share_logs);
        View openDirBtn = view.findViewById(R.id.menu_open_dir);
        View installJarBtn = view.findViewById(R.id.menu_install_jar);

        aboutBtn.setOnClickListener(v -> {
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).showAboutDialog();
            }
        });
        shareLogsBtn.setOnClickListener(v -> Tools.shareLog(requireContext()));
        openDirBtn.setOnClickListener(v -> openGameDir());
        installJarBtn.setOnClickListener(v -> {
            if (getActivity() instanceof LauncherActivity) {
                ((LauncherActivity) getActivity()).installJar();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshVersion();
        refreshAccount();
    }

    public void refreshVersion() {
        LauncherProfiles.load();
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
        MinecraftProfile prof = null;
        if (LauncherProfiles.mainProfileJson != null && LauncherProfiles.mainProfileJson.profiles != null) {
            prof = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        }
        if (prof == null || prof.lastVersionId == null || "Unknown".equals(prof.lastVersionId)) {
            mVersionName.setText(R.string.sd_v6_no_version);
            mVersionInfo.setText(R.string.sd_v6_no_version);
        } else {
            mVersionName.setText(prof.lastVersionId);
            mVersionInfo.setText(prof.lastVersionId);
        }
    }

    public void refreshAccount() {
        mcAccountSpinner spinner = null;
        if (getActivity() instanceof LauncherActivity) {
            spinner = ((LauncherActivity) getActivity()).getAccountSpinner();
        }
        if (spinner != null && spinner.getSelectedAccount() != null) {
            String username = spinner.getSelectedAccount().username;
            boolean isDemo = spinner.getSelectedAccount().isDemo();
            mAccountName.setText(username == null ? "?" : username);
            if (isDemo) {
                mAccountSub.setText(R.string.sd_v6_account_local);
                mAccountAvatar.setImageResource(R.drawable.avatar_steve);
            } else {
                mAccountSub.setText(R.string.sd_v6_account_official);
            }
        } else {
            mAccountName.setText(R.string.sd_v6_account_offline);
            mAccountSub.setText(R.string.sd_v6_account_offline);
            mAccountAvatar.setImageResource(R.drawable.avatar_steve);
        }
    }

    private void openAccountPicker() {
        if (getActivity() instanceof LauncherActivity) {
            ((LauncherActivity) getActivity()).openAccountPicker();
        }
    }

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
