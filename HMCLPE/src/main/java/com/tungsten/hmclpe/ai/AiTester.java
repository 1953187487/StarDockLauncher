package com.tungsten.hmclpe.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AiTester {

    public interface Callback {
        void onSuccess(String message);
        void onFailed(String error);
    }

    public static List<String> fetchModels(String baseUrl, String apiKey) {
        List<String> list = new ArrayList<>();
        try {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String url = base;
            if (url.endsWith("/chat/completions")) url = url.substring(0, url.length() - "/chat/completions".length());
            if (!url.endsWith("/v1")) url = url + "/v1";
            url = url + "/models";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Authorization", "Bearer " + (apiKey == null ? "" : apiKey));
            conn.setRequestProperty("User-Agent", "StarDockLauncher-Tester");
            int code = conn.getResponseCode();
            if (code != 200) return list;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONArray data = root.optJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject m = data.optJSONObject(i);
                    if (m != null) list.add(m.optString("id"));
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("AiTester", "fetchModels failed", t);
        }
        return list;
    }

    public static void test(String baseUrl, String apiKey, String model, String message, Callback cb) {
        try {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String url = base;
            if (url.endsWith("/chat/completions")) url = url.substring(0, url.length() - "/chat/completions".length());
            if (!url.endsWith("/v1")) url = url + "/v1";
            url = url + "/chat/completions";
            JSONObject body = new JSONObject();
            body.put("model", model == null ? "auto" : model);
            body.put("stream", false);
            JSONArray msgs = new JSONArray();
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", message == null ? "hi" : message);
            msgs.put(user);
            body.put("messages", msgs);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + (apiKey == null ? "" : apiKey));
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "StarDockLauncher-Tester");
            conn.setDoOutput(true);
            conn.getOutputStream().write(body.toString().getBytes());
            int code = conn.getResponseCode();
            if (code != 200) {
                cb.onFailed("HTTP " + code);
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject resp = new JSONObject(sb.toString());
            JSONArray choices = resp.optJSONArray("choices");
            String content = "(空响应)";
            if (choices != null && choices.length() > 0) {
                JSONObject first = choices.optJSONObject(0);
                if (first != null) {
                    JSONObject msg = first.optJSONObject("message");
                    if (msg != null) content = msg.optString("content", content);
                }
            }
            cb.onSuccess(content);
        } catch (Throwable t) {
            cb.onFailed(t.getMessage() == null ? t.toString() : t.getMessage());
        }
    }
}
