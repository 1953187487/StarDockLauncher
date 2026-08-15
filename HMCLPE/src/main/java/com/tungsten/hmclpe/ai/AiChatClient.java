package com.tungsten.hmclpe.ai;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiChatClient {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface ChatCallback {
        void onSuccess(String reply);
        void onFailed(String error);
    }

    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void sendChat(AiProvider provider, List<AiMessage> messages, double temperature, ChatCallback callback) {
        try {
            String url = provider.baseUrl;
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            if (!url.endsWith("/chat/completions")) {
                url = url + "chat/completions";
            }

            JSONObject body = new JSONObject();
            body.put("model", provider.model);
            body.put("temperature", temperature);
            JSONArray msgArray = new JSONArray();
            for (AiMessage msg : messages) {
                msgArray.put(msg.toJson());
            }
            body.put("messages", msgArray);

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + provider.apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onFailed("网络连接失败：" + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String respBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        String detail = "";
                        try {
                            JSONObject json = new JSONObject(respBody);
                            JSONObject error = json.optJSONObject("error");
                            if (error != null) {
                                detail = error.optString("message", respBody);
                            } else {
                                detail = respBody;
                            }
                        } catch (Exception ignored) {
                            detail = respBody;
                        }
                        final String errorMsg = "请求失败（HTTP " + response.code() + "）" + (detail.isEmpty() ? "" : "\n" + detail);
                        mainHandler.post(() -> callback.onFailed(errorMsg));
                        return;
                    }
                    String reply = "";
                    try {
                        JSONObject json = new JSONObject(respBody);
                        JSONArray choices = json.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject message = choice.optJSONObject("message");
                            if (message != null) {
                                reply = message.optString("content", "");
                            } else {
                                reply = choice.optString("text", "");
                            }
                        }
                    } catch (Exception e) {
                        final String errorMsg = "解析响应失败：" + e.getMessage();
                        mainHandler.post(() -> callback.onFailed(errorMsg));
                        return;
                    }
                    final String finalReply = reply;
                    mainHandler.post(() -> callback.onSuccess(finalReply));
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> callback.onFailed("请求构造失败：" + e.getMessage()));
        }
    }
}
