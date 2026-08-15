package com.tungsten.hmclpe.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import okhttp3.RequestBody;
import okhttp3.MediaType;

public class AiChatClient {

    public static final String TAG = "AiChatClient";

    public enum Mode {
        WEBSOCKET,
        SSE,
        HTTP
    }

    public interface StreamCallback {
        void onChunk(String chunk, String fullText);
        void onComplete(String fullText);
        void onError(String error);
    }

    private static final int MAX_CHARS_PER_SEGMENT = 1800;

    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WebSocket currentWebSocket;
    private EventSource currentEventSource;
    private volatile boolean cancelled = false;

    public AiChatClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
    }

    public void cancel() {
        cancelled = true;
        if (currentWebSocket != null) {
            try { currentWebSocket.cancel(); } catch (Exception ignored) {}
            currentWebSocket = null;
        }
        if (currentEventSource != null) {
            try { currentEventSource.cancel(); } catch (Exception ignored) {}
            currentEventSource = null;
        }
    }

    public void send(AiProvider provider, List<AiMessage> messages, StreamCallback cb) {
        cancel();
        cancelled = false;
        if (provider == null || provider.baseUrl == null || provider.apiKey == null) {
            mainHandler.post(() -> cb.onError("服务商配置不完整"));
            return;
        }
        List<String> segments = splitMessages(messages);
        if (segments.size() > 1) {
            sendChunked(provider, messages, segments, 0, cb);
        } else {
            sendOnce(provider, messages, cb);
        }
    }

    private void sendChunked(AiProvider provider, List<AiMessage> messages, List<String> segments, int idx, StreamCallback outerCb) {
        if (cancelled || idx >= segments.size()) {
            outerCb.onComplete("[分段发送完成，共 " + segments.size() + " 段]");
            return;
        }
        List<AiMessage> slice = new ArrayList<>();
        if (idx == 0) slice.addAll(messages);
        else {
            AiMessage head = new AiMessage("assistant", "已接收上文。");
            slice.add(head);
            slice.add(new AiMessage("user", segments.get(idx)));
        }
        StreamCallback inner = new StreamCallback() {
            @Override
            public void onChunk(String chunk, String fullText) {
                outerCb.onChunk(chunk, fullText);
            }
            @Override
            public void onComplete(String fullText) {
                sendChunked(provider, messages, segments, idx + 1, outerCb);
            }
            @Override
            public void onError(String error) {
                outerCb.onError("第 " + (idx + 1) + "/" + segments.size() + " 段失败：" + error);
            }
        };
        sendOnce(provider, slice, inner);
    }

    private void sendOnce(AiProvider provider, List<AiMessage> messages, StreamCallback cb) {
        Mode mode = detectMode(provider.baseUrl);
        if (mode == Mode.WEBSOCKET) {
            trySendWebSocket(provider, messages, cb);
        } else if (mode == Mode.SSE) {
            trySendSse(provider, messages, cb);
        } else {
            trySendHttp(provider, messages, cb);
        }
    }

    private Mode detectMode(String baseUrl) {
        if (baseUrl == null) return Mode.HTTP;
        String l = baseUrl.toLowerCase();
        if (l.contains("anthropic") || l.contains("claude")) return Mode.HTTP;
        return Mode.WEBSOCKET;
    }

    private List<String> splitMessages(List<AiMessage> messages) {
        if (messages.isEmpty()) return new ArrayList<>();
        AiMessage last = messages.get(messages.size() - 1);
        if (!"user".equals(last.role)) return new ArrayList<>();
        String content = last.content == null ? "" : last.content;
        if (content.length() <= MAX_CHARS_PER_SEGMENT) {
            List<String> one = new ArrayList<>();
            one.add(content);
            return one;
        }
        List<String> result = new ArrayList<>();
        result.add(content.substring(0, MAX_CHARS_PER_SEGMENT));
        for (int i = MAX_CHARS_PER_SEGMENT; i < content.length(); i += MAX_CHARS_PER_SEGMENT) {
            int end = Math.min(i + MAX_CHARS_PER_SEGMENT, content.length());
            result.add(content.substring(i, end));
        }
        return result;
    }

    private void trySendWebSocket(AiProvider provider, List<AiMessage> messages, StreamCallback cb) {
        try {
            JSONObject body = buildRequestBody(provider, messages, true);
            String url = provider.getWebSocketUrl();
            Request req = new Request.Builder().url(url).build();
            StringBuilder fullText = new StringBuilder();
            currentWebSocket = client.newWebSocket(req, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    try {
                        webSocket.send(body.toString());
                    } catch (Exception e) {
                        cb.onError("WebSocket 发送失败：" + e.getMessage());
                    }
                }
                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    if (cancelled) return;
                    try {
                        JSONObject obj = new JSONObject(text);
                        String chunk = extractDelta(obj);
                        if (chunk != null && !chunk.isEmpty()) {
                            fullText.append(chunk);
                            final String snapshot = fullText.toString();
                            mainHandler.post(() -> cb.onChunk(chunk, snapshot));
                        }
                        if (isFinished(obj)) {
                            final String snapshot = fullText.toString();
                            mainHandler.post(() -> cb.onComplete(snapshot));
                            webSocket.close(1000, "done");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "WebSocket 解析失败，降级 SSE", e);
                        webSocket.close(1000, "fallback");
                        mainHandler.post(() -> trySendSse(provider, messages, cb));
                    }
                }
                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    if (!cancelled) {
                        Log.w(TAG, "WebSocket 失败，降级 SSE", t);
                        mainHandler.post(() -> trySendSse(provider, messages, cb));
                    }
                }
                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    if (!cancelled && fullText.length() == 0) {
                        mainHandler.post(() -> trySendSse(provider, messages, cb));
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "WebSocket 构造失败，降级 SSE", e);
            trySendSse(provider, messages, cb);
        }
    }

    private void trySendSse(AiProvider provider, List<AiMessage> messages, StreamCallback cb) {
        try {
            String url = provider.getChatCompletionsUrl();
            JSONObject body = buildRequestBody(provider, messages, true);
            RequestBody reqBody = RequestBody.create(body.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + provider.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .post(reqBody)
                    .build();
            StringBuilder fullText = new StringBuilder();
            currentEventSource = EventSources.createFactory(client).newEventSource(req, new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if (cancelled) return;
                    if (data == null || data.isEmpty() || "[DONE]".equals(data)) return;
                    try {
                        JSONObject obj = new JSONObject(data);
                        String chunk = extractDelta(obj);
                        if (chunk != null && !chunk.isEmpty()) {
                            fullText.append(chunk);
                            final String snapshot = fullText.toString();
                            mainHandler.post(() -> cb.onChunk(chunk, snapshot));
                        }
                        if (isFinished(obj)) {
                            final String snapshot = fullText.toString();
                            mainHandler.post(() -> cb.onComplete(snapshot));
                            eventSource.cancel();
                        }
                    } catch (Exception ignored) {
                    }
                }
                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    if (!cancelled) {
                        Log.w(TAG, "SSE 失败，降级 HTTP", t);
                        mainHandler.post(() -> trySendHttp(provider, messages, cb));
                    }
                }
                @Override
                public void onClosed(EventSource eventSource) {
                    if (!cancelled && fullText.length() > 0) {
                        final String snapshot = fullText.toString();
                        mainHandler.post(() -> cb.onComplete(snapshot));
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "SSE 构造失败，降级 HTTP", e);
            trySendHttp(provider, messages, cb);
        }
    }

    private void trySendHttp(AiProvider provider, List<AiMessage> messages, StreamCallback cb) {
        new Thread(() -> {
            try {
                String url = provider.getChatCompletionsUrl();
                JSONObject body = buildRequestBody(provider, messages, false);
                RequestBody reqBody = RequestBody.create(body.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request req = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + provider.apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(reqBody)
                        .build();
                Response resp = client.newCall(req).execute();
                if (!resp.isSuccessful()) {
                    String err = resp.body() == null ? "" : resp.body().string();
                    mainHandler.post(() -> cb.onError("HTTP " + resp.code() + "：" + err));
                    return;
                }
                String body1 = resp.body() == null ? "" : resp.body().string();
                try {
                    JSONObject obj = new JSONObject(body1);
                    JSONArray choices = obj.optJSONArray("choices");
                    String content = "";
                    if (choices != null && choices.length() > 0) {
                        JSONObject first = choices.getJSONObject(0);
                        JSONObject message = first.optJSONObject("message");
                        if (message != null) content = message.optString("content", "");
                        else content = first.optString("text", "");
                    }
                    final String text = content;
                    mainHandler.post(() -> {
                        cb.onChunk(text, text);
                        cb.onComplete(text);
                    });
                } catch (JSONException e) {
                    final String text = body1;
                    mainHandler.post(() -> {
                        cb.onChunk(text, text);
                        cb.onComplete(text);
                    });
                }
            } catch (Exception e) {
                final String err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                mainHandler.post(() -> cb.onError(err));
            }
        }).start();
    }

    private JSONObject buildRequestBody(AiProvider provider, List<AiMessage> messages, boolean stream) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("model", provider.model == null || provider.model.isEmpty() ? "auto" : provider.model);
        body.put("stream", stream);
        body.put("temperature", AiProviderManager.get().getTemperature());
        body.put("max_tokens", AiProviderManager.get().getMaxTokens());
        JSONArray msgArr = new JSONArray();
        String sys = provider.systemPrompt;
        if (sys == null || sys.isEmpty()) sys = AiProviderManager.DEFAULT_ROLE;
        msgArr.put(buildMessage("system", sys));
        for (AiMessage m : messages) {
            if (m.content == null || m.content.isEmpty()) continue;
            msgArr.put(buildMessage(m.role, m.content));
        }
        body.put("messages", msgArr);
        return body;
    }

    private JSONObject buildMessage(String role, String content) throws JSONException {
        JSONObject m = new JSONObject();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private String extractDelta(JSONObject obj) {
        try {
            JSONArray choices = obj.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return null;
            JSONObject choice = choices.getJSONObject(0);
            JSONObject delta = choice.optJSONObject("delta");
            if (delta != null) {
                String c = delta.optString("content", null);
                if (c != null && !c.isEmpty()) return c;
            }
            JSONObject message = choice.optJSONObject("message");
            if (message != null) {
                String c = message.optString("content", null);
                if (c != null && !c.isEmpty()) return c;
            }
            String text = choice.optString("text", null);
            if (text != null && !text.isEmpty()) return text;
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isFinished(JSONObject obj) {
        JSONArray choices = obj.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject choice = choices.optJSONObject(0);
            if (choice != null) {
                String reason = choice.optString("finish_reason", "");
                if (!reason.isEmpty() && !"null".equals(reason)) return true;
            }
        }
        return false;
    }
}
