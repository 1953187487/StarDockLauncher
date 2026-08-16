package com.tungsten.hmclpe.ai;

import android.content.Context;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiChatService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();
    private static final Gson GSON = new Gson();

    public interface StreamCallback {
        void onDelta(String delta);

        void onDone(String full);

        void onError(Throwable t);
    }

    public void chat(AiProvider provider, List<AiMessage> history, StreamCallback cb) {
        new Thread(() -> {
            if (provider == null || provider.baseUrl == null) {
                if (cb != null) {
                    cb.onError(new IllegalStateException("未配置 AI 提供方"));
                }
                return;
            }
            if (provider.baseUrl.startsWith("local://")) {
                mock(provider, history, cb);
                return;
            }
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("model", provider.model == null ? "deepseek-chat" : provider.model);
                List<Map<String, String>> msgs = new ArrayList<>();
                if (history != null) {
                    for (AiMessage m : history) {
                        Map<String, String> mm = new HashMap<>();
                        mm.put("role", m.role);
                        mm.put("content", m.content);
                        msgs.add(mm);
                    }
                }
                body.put("messages", msgs);
                body.put("stream", false);
                Request.Builder rb = new Request.Builder().url(provider.baseUrl);
                if (provider.apiKey != null && !provider.apiKey.isEmpty()) {
                    rb.header("Authorization", "Bearer " + provider.apiKey);
                }
                rb.post(RequestBody.create(GSON.toJson(body), JSON));
                try (Response resp = CLIENT.newCall(rb.build()).execute()) {
                    if (!resp.isSuccessful()) {
                        if (cb != null) {
                            cb.onError(new RuntimeException("HTTP " + resp.code()));
                        }
                        return;
                    }
                    String text = resp.body() == null ? "" : resp.body().string();
                    Map<?, ?> map = GSON.fromJson(text, Map.class);
                    String content = "";
                    if (map != null && map.get("choices") instanceof List) {
                        List<?> list = (List<?>) map.get("choices");
                        if (!list.isEmpty() && list.get(0) instanceof Map) {
                            Map<?, ?> first = (Map<?, ?>) list.get(0);
                            if (first.get("message") instanceof Map) {
                                Object c = ((Map<?, ?>) first.get("message")).get("content");
                                if (c != null) {
                                    content = c.toString();
                                }
                            }
                        }
                    }
                    if (cb != null) {
                        cb.onDelta(content);
                        cb.onDone(content);
                    }
                }
            } catch (Throwable t) {
                if (cb != null) {
                    cb.onError(t);
                }
            }
        }).start();
    }

    private void mock(AiProvider provider, List<AiMessage> history, StreamCallback cb) {
        try {
            Thread.sleep(300);
        } catch (Throwable ignored) {
        }
        String last = "";
        if (history != null && !history.isEmpty()) {
            last = history.get(history.size() - 1).content;
        }
        String reply = "[离线演示模式]\n这是基于本地规则的回复。你说：" + (last == null ? "" : last);
        if (cb != null) {
            cb.onDelta(reply);
            cb.onDone(reply);
        }
    }
}
