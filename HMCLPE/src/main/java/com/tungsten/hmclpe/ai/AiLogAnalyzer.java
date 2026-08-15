package com.tungsten.hmclpe.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.tungsten.hmclpe.launcher.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AiLogAnalyzer {

    public static final int MAX_LOG_LINES = 800;

    public interface AnalyzeCallback {
        void onResult(String summary);
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static String findLatestLog(Context context) {
        MainActivity activity = null;
        try {
            androidx.appcompat.app.AppCompatActivity a = MainActivityHolder.get();
            if (a instanceof MainActivity) activity = (MainActivity) a;
        } catch (Throwable ignored) {
        }
        String gameDir = null;
        if (activity != null && activity.launcherSetting != null) {
            gameDir = activity.launcherSetting.gameFileDirectory;
        }
        if (gameDir == null) return null;
        File logsDir = new File(gameDir, "logs");
        File latest = new File(logsDir, "latest.log");
        if (latest.exists()) return latest.getAbsolutePath();
        File crashReport = new File(gameDir, "crash-reports");
        if (crashReport.exists()) {
            File[] files = crashReport.listFiles((dir, name) -> name.endsWith(".txt"));
            if (files != null && files.length > 0) {
                File newest = null;
                for (File f : files) {
                    if (newest == null || f.lastModified() > newest.lastModified()) {
                        newest = f;
                    }
                }
                if (newest != null) return newest.getAbsolutePath();
            }
        }
        return null;
    }

    public static String readLogTail(String path) {
        if (path == null) return "";
        File file = new File(path);
        if (!file.exists()) return "";
        List<String> tail = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                tail.add(line);
                if (tail.size() > MAX_LOG_LINES) {
                    tail.remove(0);
                }
            }
        } catch (IOException e) {
            return "无法读取日志：" + e.getMessage();
        }
        StringBuilder sb = new StringBuilder();
        for (String l : tail) {
            sb.append(l).append('\n');
        }
        return sb.toString();
    }

    public static void analyzeAsync(final Context context, final String logPath, boolean useAi, final AnalyzeCallback callback) {
        final String content = readLogTail(logPath);
        if (content.isEmpty()) {
            mainHandler.post(() -> callback.onResult("未找到游戏日志，无法分析。"));
            return;
        }
        String localSummary = localAnalyze(content);
        if (!useAi) {
            mainHandler.post(() -> callback.onResult(localSummary));
            return;
        }
        AiProviderManager manager = AiProviderManager.getInstance(context);
        AiProvider provider = manager.getActiveProvider();
        List<AiMessage> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(manager, "日志分析");
        messages.add(new AiMessage(AiMessage.ROLE_SYSTEM, systemPrompt));
        messages.add(new AiMessage(AiMessage.ROLE_USER,
                "请分析下面这份 Minecraft 游戏日志，找出错误和闪退原因，并用简体中文给出：\n" +
                        "1. 崩溃/错误原因（简洁）\n2. 建议的解决办法\n3. 一句话总结\n\n日志内容：\n" + content));
        new AiChatClient().send(provider, messages, new AiChatClient.StreamCallback() {
            @Override
            public void onChunk(String chunk, String fullText) {}
            @Override
            public void onComplete(String fullText) {
                final String result = fullText == null || fullText.isEmpty() ? localSummary : fullText;
                mainHandler.post(() -> callback.onResult(result));
            }
            @Override
            public void onError(String error) {
                mainHandler.post(() -> callback.onResult(localSummary + "\n\n（AI 在线分析失败：" + error + "）"));
            }
        });
    }

    public static String localAnalyze(String content) {
        StringBuilder sb = new StringBuilder();
        boolean hasError = false;
        String[] lines = content.split("\n");
        List<String> hints = new ArrayList<>();
        for (String line : lines) {
            String low = line.toLowerCase();
            if (line.contains("NullPointerException")) {
                hints.add("检测到空指针异常（NullPointerException）：通常由下载数据不完整、加载器解析失败或资源缺失导致，建议重新安装对应版本/加载器。");
                hasError = true;
            } else if (line.contains("SocketTimeoutException") || low.contains("timed out")) {
                hints.add("检测到网络超时（SocketTimeoutException）：建议切换下载源或重试。");
                hasError = true;
            } else if (line.contains("IOException")) {
                hints.add("检测到输入输出异常（IOException）：文件可能未下载完整，建议重新下载并校验完整性。");
                hasError = true;
            } else if (line.contains("OutOfMemoryError") || low.contains("out of memory")) {
                hints.add("检测到内存溢出（OutOfMemoryError）：请降低游戏分配内存或关闭其他占用内存的应用。");
                hasError = true;
            } else if (line.contains("UnsupportedClassVersionError")) {
                hints.add("检测到 Java 版本不兼容（UnsupportedClassVersionError）：请使用与版本匹配的 Java 运行。");
                hasError = true;
            } else if (line.contains("NoSuchMethodError") || line.contains("NoClassDefFoundError")) {
                hints.add("检测到类/方法缺失（NoSuchMethodError/NoClassDefFoundError）：依赖库不完整或版本冲突，建议重新安装加载器。");
                hasError = true;
            } else if (line.contains("Failed to load") || line.contains("error")) {
                hasError = true;
            }
        }
        if (!hasError) {
            sb.append("未检测到明显致命错误。日志中可能包含正常警告，可尝试降低画质、安装对应模组兼容版本。");
        } else {
            sb.append("检测到错误，分析如下：\n");
            for (String h : hints) {
                sb.append("• ").append(h).append("\n");
            }
        }
        return sb.toString();
    }

    public static String buildSystemPrompt(AiProviderManager manager, String task) {
        String role = manager.getActiveProvider().systemPrompt;
        StringBuilder sb = new StringBuilder();
        sb.append("你是《StarDock 启动器》内置的 AI 智能助手，名字叫「消息小溪」。");
        if (task != null && !task.isEmpty()) {
            sb.append("当前任务：").append(task).append("。");
        }
        if (role != null && !role.isEmpty()) {
            sb.append("玩家设定给你的角色形象：").append(role).append("。");
        } else {
            sb.append(AiProviderManager.DEFAULT_ROLE);
        }
        sb.append("你可以通过固定的命令格式直接操作启动器，命令单独占一行：\n")
                .append("[ACTION:open_main] 打开主页\n")
                .append("[ACTION:open_versions] 打开版本列表\n")
                .append("[ACTION:open_game_manager] 打开游戏管理\n")
                .append("[ACTION:open_download] 打开下载中心\n")
                .append("[ACTION:open_mod_download 模组名称] 打开模组下载并搜索指定模组\n")
                .append("[ACTION:open_version_download] 打开版本下载\n")
                .append("[ACTION:open_setting] 打开设置\n")
                .append("[ACTION:open_account] 打开账户管理\n")
                .append("[ACTION:search_video 关键词] 打开浏览器搜索教学视频（例如教你制作挖土机的视频）\n")
                .append("[ACTION:analyze_log] 自动分析游戏日志\n")
                .append("当玩家要求你操作启动器、查找设置、搜索模组或版本、搜索教学视频、分析日志时，请在回复末尾输出对应命令。");
        return sb.toString();
    }
}
