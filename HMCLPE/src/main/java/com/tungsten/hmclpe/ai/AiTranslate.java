package com.tungsten.hmclpe.ai;

import android.content.Context;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiTranslate {

    public interface Callback {
        void onSuccess(String translated);
        void onFailed(String err);
    }

    private static final ExecutorService POOL = Executors.newCachedThreadPool();

    private static Context ctx() {
        try {
            return com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String pref(String key, String def) {
        Context c = ctx();
        if (c == null) return def;
        return com.tungsten.hmclpe.launcher.setting.AppPrefs.getString(c, key, def);
    }

    public static void translateModName(String rawName, Callback cb) {
        translate("请把 Minecraft 模组/资源包/整合包的英文名称翻译为中文，简短一行，不要其他解释：" + rawName, cb);
    }

    public static void translateModDescription(String rawDesc, Callback cb) {
        if (rawDesc == null || rawDesc.trim().isEmpty()) {
            cb.onSuccess("");
            return;
        }
        String trimmed = rawDesc.length() > 1200 ? rawDesc.substring(0, 1200) + "..." : rawDesc;
        translate("请把以下 Minecraft 模组简介翻译为中文（保留 emoji 和换行）：\n" + trimmed, cb);
    }

    public static void translate(String prompt, Callback cb) {
        String baseUrl = pref(com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_BASE_URL, "https://api.hcnsec.cn/v1");
        String apiKey = pref(com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_API_KEY, "sk-rs6nsUU370qLPyuy9PzPAduH5ITwlgEv");
        String model = pref(com.tungsten.hmclpe.launcher.setting.AppPrefs.KEY_AI_MODEL, "auto");

        POOL.execute(() -> {
            try {
                URL u = new URL(baseUrl + "/chat/completions");
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setRequestProperty("Authorization", "Bearer " + apiKey);
                c.setRequestProperty("Content-Type", "application/json");
                c.setDoOutput(true);
                String body = "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"system\",\"content\":\"你是 Minecraft 模组翻译助手。\"},{\"role\":\"user\",\"content\":\"" +
                        jsonEscape(prompt) + "\"}],\"max_tokens\":512}";
                DataOutputStream out = new DataOutputStream(c.getOutputStream());
                out.write(body.getBytes("UTF-8"));
                out.flush();
                out.close();
                int code = c.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                if (code != 200) {
                    cb.onFailed("HTTP " + code + ": " + sb);
                    return;
                }
                org.json.JSONObject obj = new org.json.JSONObject(sb.toString());
                String content = obj.optJSONArray("choices").optJSONObject(0)
                        .optJSONObject("message").optString("content").trim();
                if (content.isEmpty()) cb.onFailed("空响应");
                else cb.onSuccess(content);
            } catch (Throwable t) {
                cb.onFailed(t.getMessage() == null ? t.toString() : t.getMessage());
            }
        });
    }

    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\') sb.append("\\\\");
            else if (ch == '"') sb.append("\\\"");
            else if (ch == '\n') sb.append("\\n");
            else if (ch == '\r') sb.append("\\r");
            else if (ch == '\t') sb.append("\\t");
            else if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
            else sb.append(ch);
        }
        return sb.toString();
    }
}
