package com.tungsten.hmclpe.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stardock.launcher.BuildConfig;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.auth.AccountInfo;
import com.tungsten.hmclpe.auth.AccountManager;
import com.tungsten.hmclpe.launcher.fragment.DownloadFragment;
import com.tungsten.hmclpe.launcher.fragment.HomeFragment;
import com.tungsten.hmclpe.launcher.fragment.SettingFragment;
import com.tungsten.hmclpe.launcher.fragment.ToolsFragment;
import com.tungsten.hmclpe.runtime.RuntimeInfo;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private HomeFragment homeFragment;
    private DownloadFragment downloadFragment;
    private ToolsFragment toolsFragment;
    private SettingFragment settingFragment;
    private Fragment currentFragment;
    private String currentTag;

    private AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_home);
        } catch (Throwable t) {
            Log.e(TAG, "setContentView failed", t);
            return;
        }
        try {
            accountManager = new AccountManager(getApplicationContext());
        } catch (Throwable t) {
            Log.e(TAG, "accountManager init failed", t);
        }
        try {
            bindViews();
        } catch (Throwable t) {
            Log.e(TAG, "bindViews failed", t);
        }
        try {
            initFragments();
        } catch (Throwable t) {
            Log.e(TAG, "initFragments failed", t);
        }
        try {
            switchFragment("home");
        } catch (Throwable t) {
            Log.e(TAG, "switchFragment failed", t);
        }
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.home_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            try {
                toolbar.setTitle(getString(R.string.app_name));
            } catch (Throwable ignored) {
            }
        }
        BottomNavigationView nav = findViewById(R.id.home_bottom_nav);
        if (nav != null) {
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    switchFragment("home");
                } else if (id == R.id.nav_download) {
                    switchFragment("download");
                } else if (id == R.id.nav_tools) {
                    switchFragment("tools");
                } else if (id == R.id.nav_setting) {
                    switchFragment("setting");
                }
                return true;
            });
        }
        FloatingActionButton fab = findViewById(R.id.home_fab_start);
        if (fab != null) {
            fab.setOnClickListener(v -> startLastGame());
        }
    }

    private void initFragments() {
        try {
            homeFragment = new HomeFragment();
            downloadFragment = new DownloadFragment();
            toolsFragment = new ToolsFragment();
            settingFragment = new SettingFragment();
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction tx = fm.beginTransaction();
            tx.add(R.id.home_content, homeFragment, "home");
            tx.add(R.id.home_content, downloadFragment, "download").hide(downloadFragment);
            tx.add(R.id.home_content, toolsFragment, "tools").hide(toolsFragment);
            tx.add(R.id.home_content, settingFragment, "setting").hide(settingFragment);
            tx.commitNowAllowingStateLoss();
            currentFragment = homeFragment;
            currentTag = "home";
        } catch (Throwable t) {
            Log.e(TAG, "initFragments failed", t);
        }
    }

    public void switchFragment(String tag) {
        try {
            FragmentManager fm = getSupportFragmentManager();
            Fragment target;
            switch (tag) {
                case "download":
                    target = downloadFragment;
                    break;
                case "tools":
                    target = toolsFragment;
                    break;
                case "setting":
                    target = settingFragment;
                    break;
                default:
                    target = homeFragment;
                    tag = "home";
            }
            if (target == null) {
                return;
            }
            FragmentTransaction tx = fm.beginTransaction();
            if (currentFragment != null) {
                tx.hide(currentFragment);
            }
            tx.show(target);
            tx.commitNowAllowingStateLoss();
            currentFragment = target;
            currentTag = tag;
        } catch (Throwable t) {
            Log.e(TAG, "switchFragment failed", t);
        }
    }

    public void startLastGame() {
        try {
            com.tungsten.hmclpe.launcher.version.VersionManager vm = new com.tungsten.hmclpe.launcher.version.VersionManager(getApplicationContext());
            java.util.List<com.tungsten.hmclpe.launcher.version.VersionInfo> installed = vm.installed();
            if (installed.isEmpty()) {
                showToast("尚未安装游戏版本，请先在「管理版本」中添加");
                return;
            }
            com.tungsten.hmclpe.launcher.version.VersionInfo first = installed.get(0);
            String versionId = first.id;
            String gameDir = com.tungsten.hmclpe.manifest.AppManifest.GAME_DIR;
            String engine = com.tungsten.hmclpe.launcher.runtime.RuntimePrefs.getEngine(this);
            int ram = com.tungsten.hmclpe.utils.Prefs.get(this).getInt("launcher_max_ram", 1024);
            if (RuntimeInfo.ENGINE_BOAT.equals(engine)) {
                Intent i = com.tungsten.hmclpe.launcher.launch.boat.BoatMinecraftActivity.createIntent(this, versionId, gameDir, "", "GL4ES", ram);
                startActivity(i);
            } else {
                Intent i = com.tungsten.hmclpe.launcher.launch.pojav.PojavMinecraftActivity.createIntent(this, versionId, gameDir, "", "GL4ES", ram);
                startActivity(i);
            }
        } catch (Throwable t) {
            Log.e(TAG, "startLastGame failed", t);
            showToast("启动失败：" + t.getMessage());
        }
    }

    public void refreshAvatar() {
        try {
            AccountInfo info = accountManager == null ? null : accountManager.getActive();
            View avatar = findViewById(R.id.home_account_avatar);
            TextView name = findViewById(R.id.home_account_name);
            if (info != null) {
                if (name != null) {
                    name.setText(info.username == null ? "未登录" : info.username);
                }
            } else {
                if (name != null) {
                    name.setText(getString(R.string.account_not_logged));
                }
            }
            if (avatar != null) {
                avatar.setBackgroundResource(R.drawable.ic_account_placeholder);
            }
        } catch (Throwable t) {
            Log.e(TAG, "refreshAvatar failed", t);
        }
    }

    private void showToast(String text) {
        try {
            android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Log.e(TAG, "toast failed", t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            refreshAvatar();
        } catch (Throwable t) {
            Log.e(TAG, "onResume failed", t);
        }
    }

    static {
        Log.i("HomeActivity", "StarDock v1.1.0 home loaded");
    }
}
