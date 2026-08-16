package com.tungsten.hmclpe.launcher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigationrail.NavigationRailView;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.ai.AiChatActivity;
import com.tungsten.hmclpe.ai.AiProviderManager;
import com.tungsten.hmclpe.ai.MainActivityHolder;
import com.tungsten.hmclpe.launcher.dialogs.VerifyDialog;
import com.tungsten.hmclpe.launcher.fragment.DownloadFragment;
import com.tungsten.hmclpe.launcher.fragment.HomeFragment;
import com.tungsten.hmclpe.launcher.fragment.SettingFragment;
import com.tungsten.hmclpe.launcher.fragment.ToolsFragment;
import com.tungsten.hmclpe.utils.LocaleUtils;
import com.tungsten.hmclpe.utils.crash.CrashHandler;
import com.tungsten.hmclpe.utils.crash.CrashLogViewerActivity;

import java.io.File;

public class HomeActivity extends AppCompatActivity {

    static {
        System.loadLibrary("security");
    }

    public static native boolean isValid(String str);
    public static native void verify();
    public static native void verifyFunc();

    private NavigationRailView navRail;
    private FloatingActionButton fabAi;
    private ImageView avatar;

    private final FragmentManager fm = getSupportFragmentManager();

    private static final int FRAG_HOME = 0;
    private static final int FRAG_DOWNLOAD = 1;
    private static final int FRAG_TOOLS = 2;
    private static final int FRAG_SETTING = 3;
    private int currentFrag = -1;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_home);
        } catch (Throwable t) {
            android.util.Log.e("HomeActivity", "setContentView failed", t);
            finish();
            return;
        }

        navRail = findViewById(R.id.home_nav_rail);
        fabAi = findViewById(R.id.home_fab_ai);
        avatar = findViewById(R.id.home_avatar);

        navRail.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                switchFragment(FRAG_HOME, "主页");
                return true;
            } else if (id == R.id.nav_download) {
                switchFragment(FRAG_DOWNLOAD, "下载");
                return true;
            } else if (id == R.id.nav_tools) {
                switchFragment(FRAG_TOOLS, "工具");
                return true;
            } else if (id == R.id.nav_setting) {
                switchFragment(FRAG_SETTING, "设置");
                return true;
            }
            return false;
        });

        fabAi.setOnClickListener(v -> {
            try {
                Intent i = new Intent(this, AiChatActivity.class);
                i.putExtra("drawer_mode", false);
                startActivity(i);
            } catch (Throwable t) {
                Toast.makeText(this, "启动 AI 失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        avatar.setOnClickListener(v -> {
            switchFragment(FRAG_SETTING, "设置");
            navRail.setSelectedItemId(R.id.nav_setting);
        });

        navRail.setSelectedItemId(R.id.nav_home);
        switchFragment(FRAG_HOME, "主页");

        MainActivityHolder.set(this);
        refreshAvatar();

        if (getIntent().getBooleanExtra("auto_show_crash", false)
                || CrashHandler.hasPendingCrash(this)) {
            showCrashLogIfNeeded();
        }

        startAiOverlayService();

        runOnUiThread(this::ensureAgreementsThenAnnouncement);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAvatar();
    }

    private void refreshAvatar() {
        try {
            int mode = com.tungsten.hmclpe.launcher.setting.AuthManager.currentMode();
            String name = com.tungsten.hmclpe.launcher.setting.AuthManager.currentNickname();
            avatar.setImageResource(R.drawable.ic_account_placeholder);
            if (name != null && !name.isEmpty()) {
                avatar.setContentDescription("已登录：" + name);
            } else {
                avatar.setContentDescription("未登录");
            }
        } catch (Throwable ignored) {
        }
    }

    private void ensureAgreementsThenAnnouncement() {
        try {
            boolean userAgreed = com.tungsten.hmclpe.launcher.setting.AppPrefs.getBool(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_USER_AGREED, false);
            boolean langAgreed = com.tungsten.hmclpe.launcher.setting.AppPrefs.getBool(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_LANG_AGREED, false);
            if (!langAgreed) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("选择语言")
                        .setItems(new String[]{"简体中文", "繁體中文", "English"}, (d, which) -> {
                            String code = which == 1 ? "zh-TW" : which == 2 ? "en-US" : "zh-CN";
                            com.tungsten.hmclpe.launcher.setting.AppPrefs.setString(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_LANGUAGE, code);
                            com.tungsten.hmclpe.launcher.setting.AppPrefs.setBool(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_LANG_AGREED, true);
                            ensureAgreementsThenAnnouncement();
                        })
                        .setCancelable(false)
                        .show();
                return;
            }
            if (!userAgreed) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("用户须知协议")
                        .setMessage("本启动器基于 HMCL-PE / PojavLauncher / Boat 等开源项目二次开发。\n\n" +
                                "使用本启动器下载、安装和启动 Minecraft 时，请遵守 Mojang EULA 和您所在地区法律法规。\n\n" +
                                "本启动器不收集您的个人信息；崩溃日志（仅在崩溃时）保存在本地。\n\n" +
                                "启动器不保证所有版本都可在所有机型上正常运行。")
                        .setPositiveButton("同意", (d, w) -> {
                            com.tungsten.hmclpe.launcher.setting.AppPrefs.setBool(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_USER_AGREED, true);
                            ensureAgreementsThenAnnouncement();
                        })
                        .setNegativeButton("退出", (d, w) -> finishAffinity())
                        .setCancelable(false)
                        .show();
                return;
            }
            boolean aiAgreed = com.tungsten.hmclpe.launcher.setting.AppPrefs.getBool(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_AGREED, false);
            if (!aiAgreed) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("AI 服务协议（可选）")
                        .setMessage("AI 功能由第三方 API 提供。\n\n" +
                                "您发送的关键词、日志、链接会传递给 AI 服务端用于返回结果。\n\n" +
                                "请勿发送个人敏感信息。\n\n" +
                                "可以暂时跳过，之后在 AI 设置页同意即可启用 AI。")
                        .setPositiveButton("同意", (d, w) -> {
                            com.tungsten.hmclpe.launcher.setting.AppPrefs.setBool(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_AGREED, true);
                            ensureAgreementsThenAnnouncement();
                        })
                        .setNegativeButton("暂不同意", (d, w) -> {
                            com.tungsten.hmclpe.launcher.setting.AppPrefs.setBool(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_AGREED, false);
                            ensureAgreementsThenAnnouncement();
                        })
                        .setCancelable(false)
                        .show();
                return;
            }
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("欢迎使用 StarDockLauncher " + com.tungsten.hmclpe.BuildConfig.VERSION_NAME)
                    .setMessage(com.tungsten.hmclpe.launcher.setting.AnnouncementManager.current())
                    .setPositiveButton("知道了", (d, w) -> com.tungsten.hmclpe.launcher.setting.AppPrefs.setBool(this, com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_ANNOUNCEMENT_SEEN, true))
                    .show();
        } catch (Throwable t) {
            android.util.Log.e("HomeActivity", "ensureAgreementsThenAnnouncement failed", t);
        }
    }

    private void switchFragment(int which, String title) {
        try {
            String tag = currentTag(which);
            Fragment existing = fm.findFragmentByTag(tag);
            Fragment frag = existing;
            if (frag == null || frag.isDetached()) {
                switch (which) {
                    case FRAG_HOME: frag = new HomeFragment(); break;
                    case FRAG_DOWNLOAD: frag = new DownloadFragment(); break;
                    case FRAG_TOOLS: frag = new ToolsFragment(); break;
                    case FRAG_SETTING: frag = new SettingFragment(); break;
                    default: return;
                }
            }
            FragmentTransaction tx = fm.beginTransaction();
            tx.setReorderingAllowed(true);
            if (currentFrag != -1) {
                Fragment cur = fm.findFragmentByTag(currentTag(currentFrag));
                if (cur != null) tx.hide(cur);
            }
            if (existing == null || existing.isDetached()) {
                tx.add(R.id.home_content, frag, tag);
            } else {
                tx.show(frag);
            }
            tx.commitNowAllowingStateLoss();
            currentFrag = which;
            android.widget.TextView tv = findViewById(R.id.home_toolbar_subtitle);
            if (tv != null) tv.setText(title);
        } catch (Throwable t) {
            android.util.Log.e("HomeActivity", "switchFragment failed", t);
        }
    }

    private String currentTag(int which) {
        switch (which) {
            case FRAG_HOME: return "home";
            case FRAG_DOWNLOAD: return "download";
            case FRAG_TOOLS: return "tools";
            case FRAG_SETTING: return "setting";
            default: return "home";
        }
    }

    public void switchToFragment(int which, String title) {
        navRail.setSelectedItemId(which == FRAG_HOME ? R.id.nav_home
                : which == FRAG_DOWNLOAD ? R.id.nav_download
                : which == FRAG_TOOLS ? R.id.nav_tools
                : which == FRAG_SETTING ? R.id.nav_setting : R.id.nav_home);
        switchFragment(which, title);
    }

    @Override
    public void onBackPressed() {
        if (currentFrag != FRAG_HOME) {
            navRail.setSelectedItemId(R.id.nav_home);
            return;
        }
        moveTaskToBack(true);
    }

    private void showCrashLogIfNeeded() {
        try {
            File latest = CrashHandler.getLatestCrashLog(this);
            if (latest != null) {
                CrashHandler.clearPendingFlag(this);
                Intent crashIntent = new Intent(this, CrashLogViewerActivity.class);
                crashIntent.putExtra("log_path", latest.getAbsolutePath());
                crashIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(crashIntent);
            }
        } catch (Exception ignored) {
        }
    }

    private void startAiOverlayService() {
        try {
            if (!providerManagerOverlayEnabled()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    return;
                }
            }
            Intent intent = new Intent(this, com.tungsten.hmclpe.ai.AiOverlayService.class);
            intent.setAction(com.tungsten.hmclpe.ai.AiOverlayService.ACTION_START);
            startService(intent);
        } catch (Exception ignored) {
        }
    }

    private boolean providerManagerOverlayEnabled() {
        try {
            return AiProviderManager.getInstance(this).isOverlayEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    public void startVerify(VerifyInterface verifyInterface) {
        try {
            SharedPreferences sp = getSharedPreferences("stardock_native", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            androidx.appcompat.app.AppCompatActivity a = MainActivityHolder.get();
            com.tungsten.hmclpe.launcher.MainActivity legacy = (a instanceof com.tungsten.hmclpe.launcher.MainActivity)
                    ? (com.tungsten.hmclpe.launcher.MainActivity) a : null;
            VerifyDialog dialog = new VerifyDialog(this, legacy, editor, verifyInterface);
            dialog.show();
        } catch (Throwable t) {
            android.util.Log.e("HomeActivity", "startVerify failed", t);
        }
    }
}
