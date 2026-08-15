package com.tungsten.hmclpe.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;

import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.uis.game.download.right.DownloadModUI;
import com.tungsten.hmclpe.launcher.uis.tools.BaseUI;
import com.tungsten.hmclpe.launcher.uis.tools.UIManager;

import java.util.Locale;

public class AiActionExecutor {

    public interface ActionResult {
        void onResult(String message);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void execute(MainActivity activity, String action, String arg, ActionResult result) {
        if (activity == null) {
            if (result != null) result.onResult("启动器主界面尚未就绪，请在启动器主页再试。");
            return;
        }
        UIManager ui = activity.uiManager;
        if (ui == null) {
            if (result != null) result.onResult("启动器界面尚未初始化。");
            return;
        }
        switch (action) {
            case "open_main":
                switchTo(activity, ui.mainUI);
                if (result != null) result.onResult("已打开启动器主页。");
                break;
            case "open_versions":
                switchTo(activity, ui.versionListUI);
                if (result != null) result.onResult("已打开版本列表。");
                break;
            case "open_game_manager":
                switchTo(activity, ui.gameManagerUI);
                if (result != null) result.onResult("已打开游戏管理。");
                break;
            case "open_download":
                switchTo(activity, ui.downloadUI);
                if (result != null) result.onResult("已打开下载中心。");
                break;
            case "open_mod_download":
                switchTo(activity, ui.downloadUI);
                if (ui.downloadUI.downloadUIManager != null && ui.downloadUI.downloadUIManager.downloadModUI != null) {
                    ui.downloadUI.downloadUIManager.switchDownloadUI(ui.downloadUI.downloadUIManager.downloadModUI);
                }
                if (arg != null && !arg.isEmpty()) {
                    setModSearchKeyword(activity, arg);
                }
                if (result != null) result.onResult("已打开模组下载" + (arg != null && !arg.isEmpty() ? "，正在搜索：" + arg : "") + "。");
                break;
            case "open_version_download":
                switchTo(activity, ui.downloadUI);
                if (ui.downloadUI.downloadUIManager != null && ui.downloadUI.downloadUIManager.downloadMinecraftUI != null) {
                    ui.downloadUI.downloadUIManager.switchDownloadUI(ui.downloadUI.downloadUIManager.downloadMinecraftUI);
                }
                if (result != null) result.onResult("已打开版本下载。");
                break;
            case "open_setting":
                switchTo(activity, ui.settingUI);
                if (result != null) result.onResult("已打开设置。");
                break;
            case "open_account":
                switchTo(activity, ui.accountUI);
                if (result != null) result.onResult("已打开账户管理。");
                break;
            case "search_video":
                searchVideo(activity, arg);
                if (result != null) result.onResult("已为你搜索相关视频：" + (arg == null ? "" : arg));
                break;
            case "analyze_log":
                if (result != null) result.onResult("开始分析启动器日志，请稍候…");
                AiLogAnalyzer.analyzeAsync(activity, AiLogAnalyzer.findLatestLog(activity), true, new AiLogAnalyzer.AnalyzeCallback() {
                    @Override
                    public void onResult(String summary) {
                        if (result != null) result.onResult("日志分析结果：\n" + summary);
                    }
                });
                break;
            default:
                if (result != null) result.onResult("未知操作：" + action);
                break;
        }
    }

    private void switchTo(MainActivity activity, BaseUI ui) {
        if (ui == null) return;
        if (activity.uiManager.currentUI != ui) {
            activity.uiManager.switchMainUI(ui);
        }
    }

    private void setModSearchKeyword(MainActivity activity, String keyword) {
        mainHandler.postDelayed(() -> {
            if (activity == null) return;
            EditText edit = activity.findViewById(com.tungsten.hmclpe.R.id.download_mod_arg_name);
            if (edit != null) {
                edit.setText(keyword);
            }
        }, 300);
    }

    private void searchVideo(Context context, String query) {
        if (query == null) query = "我的世界 教程";
        String url = String.format(Locale.CHINA, "https://search.bilibili.com/all?keyword=%s", Uri.encode(query));
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }
}
