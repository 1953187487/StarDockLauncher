package com.tungsten.hmclpe.ai;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AiModSearcher {

    public static final String API_BASE = "https://api.modrinth.com/v2/search";

    private static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("mod", "mod");
        TYPE_MAP.put("模组", "mod");
        TYPE_MAP.put("mods", "mod");
        TYPE_MAP.put("shader", "shader");
        TYPE_MAP.put("光影", "shader");
        TYPE_MAP.put("shaders", "shader");
        TYPE_MAP.put("光影包", "shader");
        TYPE_MAP.put("resourcepack", "resourcepack");
        TYPE_MAP.put("资源包", "resourcepack");
        TYPE_MAP.put("材质包", "resourcepack");
        TYPE_MAP.put("datapack", "datapack");
        TYPE_MAP.put("数据包", "datapack");
        TYPE_MAP.put("modpack", "modpack");
        TYPE_MAP.put("整合包", "modpack");
    }

    public static class SearchResult {
        public String title;
        public String slug;
        public String description;
        public String author;
        public String downloads;
        public String version;
        public String iconUrl;
        public String projectType;
        public String projectUrl;
    }

    public interface SearchCallback {
        void onSuccess(List<SearchResult> results);
        void onFailed(String error);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    public void search(String query, String projectType, SearchCallback callback) {
        try {
            String type = null;
            if (projectType != null && !projectType.isEmpty()) {
                type = TYPE_MAP.get(projectType.toLowerCase(Locale.ROOT));
            }
            String url = API_BASE + "?query=" + java.net.URLEncoder.encode(query, "UTF-8") + "&limit=6";
            if (type != null) {
                url += "&facets=" + java.net.URLEncoder.encode("[[\"project_type:" + type + "\"]]", "UTF-8");
            }
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "StarDockLauncher/1.0.2")
                    .get()
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onFailed("搜索失败：" + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        final String msg = "搜索请求失败（HTTP " + response.code() + "）";
                        mainHandler.post(() -> callback.onFailed(msg));
                        return;
                    }
                    final List<SearchResult> results = parse(body);
                    mainHandler.post(() -> callback.onSuccess(results));
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> callback.onFailed("搜索失败：" + e.getMessage()));
        }
    }

    private List<SearchResult> parse(String body) {
        List<SearchResult> results = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(body);
            JSONArray hits = root.optJSONArray("hits");
            if (hits != null) {
                for (int i = 0; i < hits.length(); i++) {
                    JSONObject hit = hits.getJSONObject(i);
                    SearchResult r = new SearchResult();
                    r.title = hit.optString("title", "");
                    r.slug = hit.optString("slug", "");
                    r.description = hit.optString("description", "");
                    r.author = hit.optString("author", "");
                    r.downloads = String.valueOf(hit.optLong("downloads", 0));
                    r.iconUrl = hit.optString("icon_url", "");
                    r.projectType = hit.optString("project_type", "mod");
                    JSONArray versions = hit.optJSONArray("versions");
                    if (versions != null && versions.length() > 0) {
                        r.version = versions.optString(versions.length() - 1);
                    }
                    r.projectUrl = "https://modrinth.com/" + r.projectType + "/" + r.slug;
                    results.add(r);
                }
            }
        } catch (Exception ignored) {
        }
        return results;
    }
}
