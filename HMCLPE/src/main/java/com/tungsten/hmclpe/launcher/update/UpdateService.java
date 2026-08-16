package com.tungsten.hmclpe.launcher.update;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateService {

    private static final String TAG = "UpdateService";
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();
    private static final Gson GSON = new Gson();

    public interface Callback {
        void onResult(LauncherUpdate update);

        void onError(Throwable t);
    }

    public void fetchLatest(Callback cb) {
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(LauncherUpdate.RELEASES_API)
                        .header("Accept", "application/vnd.github+json")
                        .build();
                try (Response resp = CLIENT.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        if (cb != null) {
                            cb.onError(new IOException("HTTP " + resp.code()));
                        }
                        return;
                    }
                    String body = resp.body().string();
                    LauncherUpdate update = parse(body);
                    if (cb != null) {
                        cb.onResult(update);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "fetchLatest failed", t);
                if (cb != null) {
                    cb.onError(t);
                }
            }
        }).start();
    }

    private LauncherUpdate parse(String body) {
        try {
            java.util.Map<String, Object> map = GSON.fromJson(body, java.util.Map.class);
            LauncherUpdate u = new LauncherUpdate();
            u.tagName = asString(map.get("tag_name"));
            u.releaseId = String.valueOf(map.get("id"));
            Object assetsObj = map.get("assets");
            if (assetsObj instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) assetsObj;
                for (Object o : list) {
                    if (o instanceof java.util.Map) {
                        java.util.Map<?, ?> a = (java.util.Map<?, ?>) o;
                        String name = asString(a.get("name"));
                        if (name != null && name.endsWith(".apk")) {
                            u.assetName = name;
                            u.downloadUrl = asString(a.get("browser_download_url"));
                            Object sizeObj = a.get("size");
                            if (sizeObj instanceof Number) {
                                u.size = ((Number) sizeObj).longValue();
                            }
                            break;
                        }
                    }
                }
            }
            Object body2 = map.get("body");
            u.changelog = body2 == null ? "" : body2.toString();
            if (u.tagName != null && u.tagName.startsWith("v")) {
                u.versionCode = u.tagName.substring(1);
            }
            return u;
        } catch (Throwable t) {
            Log.e(TAG, "parse failed", t);
            return null;
        }
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
