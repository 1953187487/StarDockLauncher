package com.tungsten.hmclpe.launcher.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tungsten.hmclpe.BuildConfig;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.ai.AiProvider;
import com.tungsten.hmclpe.ai.AiProviderManager;
import com.tungsten.hmclpe.ai.AiTester;
import com.tungsten.hmclpe.auth.microsoft.MicrosoftLoginActivity;
import com.tungsten.hmclpe.launcher.setting.AnnouncementManager;
import com.tungsten.hmclpe.launcher.setting.AppPrefs;
import com.tungsten.hmclpe.launcher.setting.AuthManager;
import com.tungsten.hmclpe.launcher.setting.DownloadSource;
import com.tungsten.hmclpe.launcher.setting.GitHubService;
import com.tungsten.hmclpe.launcher.setting.ThemePrefs;
import com.tungsten.hmclpe.launcher.setting.VersionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingFragment extends Fragment {

    private TextView themeSubtitle;
    private TextView backgroundSubtitle;
    private TextView downloadSourceSubtitle;
    private TextView gameDirSubtitle;
    private TextView loginSubtitle;
    private TextView versionText;
    private TextView aiApiSubtitle;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            themeSubtitle = view.findViewById(R.id.setting_theme_subtitle);
            backgroundSubtitle = view.findViewById(R.id.setting_background_subtitle);
            downloadSourceSubtitle = view.findViewById(R.id.setting_download_source_subtitle);
            gameDirSubtitle = view.findViewById(R.id.setting_game_dir_subtitle);
            loginSubtitle = view.findViewById(R.id.setting_login_subtitle);
            versionText = view.findViewById(R.id.setting_version_text);

            view.findViewById(R.id.setting_row_theme).setOnClickListener(v -> pickTheme());
            view.findViewById(R.id.setting_row_background).setOnClickListener(v -> pickBackground());
            view.findViewById(R.id.setting_row_download_source).setOnClickListener(v -> pickDownloadSource());
            view.findViewById(R.id.setting_row_game_dir).setOnClickListener(v -> pickGameDir());
            view.findViewById(R.id.setting_row_login).setOnClickListener(v -> pickLogin());
            view.findViewById(R.id.setting_row_announcement).setOnClickListener(v -> showAnnouncement());
            view.findViewById(R.id.setting_row_user_agreement).setOnClickListener(v -> showUserAgreement());
            view.findViewById(R.id.setting_row_ai_agreement).setOnClickListener(v -> showAiAgreement());
            view.findViewById(R.id.setting_row_language_agreement).setOnClickListener(v -> pickLanguage());
            view.findViewById(R.id.setting_row_check_update).setOnClickListener(v -> checkUpdate());
            view.findViewById(R.id.setting_row_history).setOnClickListener(v -> openHistory());
            view.findViewById(R.id.setting_row_repo).setOnClickListener(v -> openRepo());

            aiApiSubtitle = view.findViewById(R.id.setting_ai_api_subtitle);
            view.findViewById(R.id.setting_row_ai_api).setOnClickListener(v -> pickAiApi());
            view.findViewById(R.id.setting_row_ai_models).setOnClickListener(v -> fetchModels());
            view.findViewById(R.id.setting_row_ai_test).setOnClickListener(v -> testModel());

            refreshAll();
        } catch (Throwable t) {
            android.util.Log.e("SettingFragment", "init failed", t);
        }
    }

    private void refreshAll() {
        if (themeSubtitle != null) themeSubtitle.setText(ThemePrefs.name(ThemePrefs.getMode()));
        if (backgroundSubtitle != null) {
            String uri = ThemePrefs.getBackgroundUri();
            backgroundSubtitle.setText(uri == null || uri.isEmpty() ? "使用默认深色" : "已设置自定义背景");
        }
        if (downloadSourceSubtitle != null) downloadSourceSubtitle.setText(DownloadSource.name(DownloadSource.current()));
        if (gameDirSubtitle != null) gameDirSubtitle.setText(VersionManager.currentGameDir());
        if (loginSubtitle != null) loginSubtitle.setText(AuthManager.name(AuthManager.currentMode()));
        if (versionText != null) versionText.setText("StarDockLauncher v" + BuildConfig.VERSION_NAME + " (build " + BuildConfig.VERSION_CODE + ")");
        try {
            if (aiApiSubtitle != null) {
                AiProvider p = AiProviderManager.getInstance(requireContext()).getActiveProvider();
                aiApiSubtitle.setText(p.name + " · " + (p.model == null ? "auto" : p.model));
            }
        } catch (Throwable t) {
            if (aiApiSubtitle != null) aiApiSubtitle.setText("默认服务商");
        }
    }

    private void pickAiApi() {
        try {
            List<AiProvider> list = AiProviderManager.getInstance(requireContext()).listProviders();
            String[] names = new String[list.size()];
            for (int i = 0; i < list.size(); i++) names[i] = list.get(i).name;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("选择 AI 服务商")
                    .setItems(names, (d, which) -> {
                        AiProvider p = list.get(which);
                        AiProviderManager.getInstance(requireContext()).setActiveProvider(p.id);
                        refreshAll();
                    })
                    .setNegativeButton("新增自定义", (d, w) -> showAddProvider())
                    .show();
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "AI 配置失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddProvider() {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(48, 24, 48, 0);
        EditText name = new EditText(requireContext()); name.setHint("名称");
        EditText url = new EditText(requireContext()); url.setHint("Base URL（含 /v1）");
        EditText key = new EditText(requireContext()); key.setHint("API Key");
        EditText model = new EditText(requireContext()); model.setHint("模型（可空）");
        wrap.addView(name); wrap.addView(url); wrap.addView(key); wrap.addView(model);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("新增 AI 服务商")
                .setView(wrap)
                .setPositiveButton("保存", (d, w) -> {
                    try {
                        AiProvider p = new AiProvider();
                        p.id = "custom_" + System.currentTimeMillis();
                        p.name = name.getText().toString().trim();
                        p.baseUrl = url.getText().toString().trim();
                        p.apiKey = key.getText().toString().trim();
                        p.model = model.getText().toString().trim();
                        p.systemPrompt = "你是消息小溪，友好的游戏助手。";
                        p.isLocked = false;
                        AiProviderManager.getInstance(requireContext()).addProvider(p);
                        AiProviderManager.getInstance(requireContext()).setActiveProvider(p.id);
                        refreshAll();
                    } catch (Throwable t) {
                        Toast.makeText(requireContext(), "保存失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void fetchModels() {
        Toast.makeText(requireContext(), "正在拉取模型列表…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            try {
                AiProvider p = AiProviderManager.getInstance(requireContext()).getActiveProvider();
                final List<String> models = AiTester.fetchModels(p.baseUrl, p.apiKey);
                requireActivity().runOnUiThread(() -> {
                    if (models == null || models.isEmpty()) {
                        Toast.makeText(requireContext(), "未取到模型。请检查 API 地址或 Key。", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("可用模型（点击使用）")
                            .setItems(models.toArray(new String[0]), (d, which) -> {
                                p.model = models.get(which);
                                AppPrefs.setString(requireContext(), AppPrefs.KEY_AI_MODEL, p.model);
                                refreshAll();
                            })
                            .show();
                });
            } catch (Throwable t) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "拉取失败：" + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void testModel() {
        final EditText input = new EditText(requireContext());
        input.setHint("要发送的测试消息");
        input.setText("你好，请用一句话介绍你自己。");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("测试模型")
                .setView(input)
                .setPositiveButton("发送", (d, w) -> {
                    final String msg = input.getText().toString().trim();
                    Toast.makeText(requireContext(), "正在测试…", Toast.LENGTH_SHORT).show();
                    io.execute(() -> {
                        try {
                            AiProvider p = AiProviderManager.getInstance(requireContext()).getActiveProvider();
                            AiTester.test(p.baseUrl, p.apiKey, p.model, msg, new AiTester.Callback() {
                                @Override public void onSuccess(String message) {
                                    requireActivity().runOnUiThread(() -> new MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("模型回复")
                                            .setMessage(message)
                                            .setPositiveButton("好的", null)
                                            .show());
                                }
                                @Override public void onFailed(String error) {
                                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "测试失败：" + error, Toast.LENGTH_SHORT).show());
                                }
                            });
                        } catch (Throwable t) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "测试失败：" + t.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAll();
    }

    private void pickTheme() {
        final String[] items = new String[]{"深色（默认）", "浅色", "跟随系统", "动态取色（Android 12+）"};
        final int[] modes = new int[]{ThemePrefs.MODE_DARK, ThemePrefs.MODE_LIGHT, ThemePrefs.MODE_SYSTEM, ThemePrefs.MODE_DYNAMIC};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择主题")
                .setItems(items, (d, which) -> {
                    ThemePrefs.setMode(modes[which]);
                    refreshAll();
                    Toast.makeText(requireContext(), "主题已切换为：" + ThemePrefs.name(modes[which]), Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void pickBackground() {
        final String[] items = new String[]{"使用默认深色", "使用默认渐变", "清除自定义"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("背景")
                .setItems(items, (d, which) -> {
                    if (which == 0) ThemePrefs.setBackgroundUri("");
                    else if (which == 1) ThemePrefs.setBackgroundUri("default_gradient");
                    else ThemePrefs.setBackgroundUri("");
                    refreshAll();
                })
                .show();
    }

    private void pickDownloadSource() {
        final String[] items = new String[]{DownloadSource.name(DownloadSource.BMCLAPI), DownloadSource.name(DownloadSource.MOJANG)};
        final int[] srcs = new int[]{DownloadSource.BMCLAPI, DownloadSource.MOJANG};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("下载源")
                .setItems(items, (d, which) -> {
                    DownloadSource.setCurrent(srcs[which]);
                    refreshAll();
                })
                .show();
    }

    private void pickGameDir() {
        final EditText input = new EditText(requireContext());
        input.setText(VersionManager.currentGameDir());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("游戏目录")
                .setView(input)
                .setPositiveButton("保存", (d, w) -> {
                    String s = input.getText().toString().trim();
                    if (s.isEmpty()) s = VersionManager.gamesDir().getAbsolutePath();
                    AppPrefs.setString(requireContext(), AppPrefs.KEY_GAME_DIR, s);
                    refreshAll();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void pickLogin() {
        final String[] items = new String[]{AuthManager.name(AuthManager.OFFLINE), AuthManager.name(AuthManager.MICROSOFT), AuthManager.name(AuthManager.THIRD_PARTY)};
        final int[] modes = new int[]{AuthManager.OFFLINE, AuthManager.MICROSOFT, AuthManager.THIRD_PARTY};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("登录方式")
                .setItems(items, (d, which) -> {
                    AuthManager.setMode(modes[which]);
                    if (modes[which] == AuthManager.OFFLINE) {
                        promptNickname();
                    } else if (modes[which] == AuthManager.MICROSOFT) {
                        try {
                            Intent i = new Intent(requireContext(), MicrosoftLoginActivity.class);
                            startActivity(i);
                        } catch (Throwable t) {
                            Toast.makeText(requireContext(), "无法启动微软登录：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else if (modes[which] == AuthManager.THIRD_PARTY) {
                        promptThirdPartyServer();
                    }
                    refreshAll();
                })
                .show();
    }

    private void promptNickname() {
        final EditText input = new EditText(requireContext());
        input.setHint("玩家名");
        input.setText(AuthManager.currentNickname());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("离线玩家名")
                .setView(input)
                .setPositiveButton("保存", (d, w) -> AuthManager.setNickname(input.getText().toString().trim()))
                .setNegativeButton("取消", null)
                .show();
    }

    private void promptThirdPartyServer() {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(48, 24, 48, 0);
        final EditText api = new EditText(requireContext());
        api.setHint("API 服务器地址，如 https://example.yggdrasil/");
        api.setText(AuthManager.currentServer());
        final EditText email = new EditText(requireContext());
        email.setHint("邮箱（可选）");
        final EditText pwd = new EditText(requireContext());
        pwd.setHint("密码（可选）");
        wrap.addView(api);
        wrap.addView(email);
        wrap.addView(pwd);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("第三方服务器（authlib-injector）")
                .setView(wrap)
                .setPositiveButton("保存", (d, w) -> {
                    AuthManager.setServer(api.getText().toString().trim());
                    Toast.makeText(requireContext(), "已保存第三方服务器配置。启动游戏时将通过 authlib-injector 注入。", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAnnouncement() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("公告")
                .setMessage(AnnouncementManager.current())
                .setPositiveButton("知道了", null)
                .show();
    }

    private void showUserAgreement() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("用户须知协议")
                .setMessage("本启动器基于 HMCL-PE / PojavLauncher / Boat 等开源项目二次开发。\n\n" +
                        "使用本启动器下载、安装和启动 Minecraft 时，请遵守 Mojang EULA 和您所在地区法律法规。\n\n" +
                        "本启动器不收集您的个人信息；崩溃日志（仅在崩溃时）保存在本地，您可以在「下载与游戏」中查看。\n\n" +
                        "启动器不保证所有版本都可在所有机型上正常运行。")
                .setPositiveButton("同意", (d, w) -> AppPrefs.setBool(requireContext(), AppPrefs.KEY_USER_AGREED, true))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAiAgreement() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("AI 服务协议")
                .setMessage("AI 功能由第三方 API（默认 api.hcnsec.cn）提供。\n\n" +
                        "您发送的消息、日志内容、游戏版本信息会传递给 AI 服务端，用于生成回复。\n\n" +
                        "请勿发送个人敏感信息。")
                .setPositiveButton("同意", (d, w) -> AppPrefs.setBool(requireContext(), AppPrefs.KEY_AI_AGREED, true))
                .setNegativeButton("取消", null)
                .show();
    }

    private void pickLanguage() {
        final String[] items = new String[]{"简体中文", "繁體中文", "English"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("语言")
                .setItems(items, (d, which) -> {
                    String code = which == 1 ? "zh-TW" : which == 2 ? "en-US" : "zh-CN";
                    AppPrefs.setString(requireContext(), AppPrefs.KEY_LANGUAGE, code);
                    Toast.makeText(requireContext(), "重启启动器后生效", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void checkUpdate() {
        Toast.makeText(requireContext(), "正在检查更新…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            final GitHubService.Release r = GitHubService.fetchLatest();
            requireActivity().runOnUiThread(() -> {
                if (r == null) {
                    Toast.makeText(requireContext(), "检查失败，请稍后重试", Toast.LENGTH_SHORT).show();
                    return;
                }
                String cur = "v" + BuildConfig.VERSION_NAME;
                if (r.tagName != null && r.tagName.equals(cur)) {
                    Toast.makeText(requireContext(), "已是最新版本 " + cur, Toast.LENGTH_SHORT).show();
                } else {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("发现新版本 " + r.tagName)
                            .setMessage((r.body == null ? "" : r.body) + "\n\n下载地址：" + r.apkUrl)
                            .setPositiveButton("前往下载", (d, w) -> {
                                try {
                                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(r.apkUrl));
                                    startActivity(i);
                                } catch (Throwable t) {
                                    Toast.makeText(requireContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            });
        });
    }

    private void openHistory() {
        Toast.makeText(requireContext(), "正在拉取历史版本…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            final List<GitHubService.Release> list = GitHubService.fetchAllReleases();
            requireActivity().runOnUiThread(() -> {
                if (list == null || list.isEmpty()) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("历史版本")
                            .setMessage("无法连接 GitHub。\n\n请直接打开：\nhttps://github.com/" + GitHubService.REPO + "/releases")
                            .setPositiveButton("打开网页", (d, w) -> {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/" + GitHubService.REPO + "/releases")));
                                } catch (Throwable ignored) {}
                            })
                            .setNegativeButton("关闭", null)
                            .show();
                    return;
                }
                String[] names = new String[list.size()];
                for (int i = 0; i < list.size(); i++) names[i] = list.get(i).tagName + " · " + (list.get(i).publishedAt == null ? "" : list.get(i).publishedAt.substring(0, Math.min(10, list.get(i).publishedAt.length())));
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("历史版本（点击打开）")
                        .setItems(names, (d, which) -> {
                            try {
                                String url = list.get(which).apkUrl != null ? list.get(which).apkUrl : ("https://github.com/" + GitHubService.REPO + "/releases/tag/" + list.get(which).tagName);
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                            } catch (Throwable ignored) {}
                        })
                        .show();
            });
        });
    }

    private void openRepo() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/" + GitHubService.REPO)));
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }
}
