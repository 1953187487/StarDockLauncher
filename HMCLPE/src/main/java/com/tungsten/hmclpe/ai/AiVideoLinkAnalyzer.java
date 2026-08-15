package com.tungsten.hmclpe.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AiVideoLinkAnalyzer {

    public interface AnalyzeCallback {
        void onResult(String title, String description, String extractedNames);
        void onFailed(String error);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void analyze(final Context context, final String videoUrl, final AnalyzeCallback callback) {
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(videoUrl)
                        .userAgent("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                        .referrer(videoUrl)
                        .timeout(15000)
                        .get();
                String title = "";
                Element ogTitle = doc.selectFirst("meta[property=og:title]");
                if (ogTitle != null) {
                    title = ogTitle.attr("content");
                }
                if (title.isEmpty()) {
                    title = doc.title();
                }
                String description = "";
                Element ogDesc = doc.selectFirst("meta[property=og:description]");
                if (ogDesc != null) {
                    description = ogDesc.attr("content");
                }
                if (description.isEmpty()) {
                    Element desc = doc.selectFirst("meta[name=description]");
                    if (desc != null) {
                        description = desc.attr("content");
                    }
                }
                final String fTitle = title;
                final String fDesc = description;
                extractModNames(context, fTitle, fDesc, new ExtractCallback() {
                    @Override
                    public void onExtracted(String names) {
                        mainHandler.post(() -> callback.onResult(fTitle, fDesc, names));
                    }

                    @Override
                    public void onFailed(String error) {
                        mainHandler.post(() -> callback.onFailed(error));
                    }
                });
            } catch (IOException e) {
                mainHandler.post(() -> callback.onFailed("无法读取视频页面（请确认链接有效）：" + e.getMessage()));
            }
        }, "AiVideoAnalyzer").start();
    }

    private interface ExtractCallback {
        void onExtracted(String names);
        void onFailed(String error);
    }

    private void extractModNames(Context context, String title, String description, ExtractCallback callback) {
        List<AiMessage> messages = new ArrayList<>();
        AiProviderManager manager = AiProviderManager.getInstance(context);
        String system = "你是 Minecraft 模组识别助手。用户会给你一个视频的标题和简介，请从中识别出视频中介绍的 Minecraft 模组/光影/资源包名称。";
        messages.add(new AiMessage(AiMessage.ROLE_SYSTEM, system));
        messages.add(new AiMessage(AiMessage.ROLE_USER,
                "视频标题：" + title + "\n\n视频简介：" + description + "\n\n" +
                        "请只输出识别到的项目名称（模组/光影/资源包），用顿号或逗号分隔；如果识别不出就回复“未识别到项目名称”。"));
        new AiChatClient().send(manager.getActiveProvider(), messages, new AiChatClient.StreamCallback() {
            @Override
            public void onChunk(String chunk, String fullText) {}
            @Override
            public void onComplete(String fullText) {
                callback.onExtracted(fullText == null ? "" : fullText);
            }
            @Override
            public void onError(String error) {
                callback.onFailed(error);
            }
        });
    }
}
