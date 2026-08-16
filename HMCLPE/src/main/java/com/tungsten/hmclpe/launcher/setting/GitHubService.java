package com.tungsten.hmclpe.launcher.setting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GitHubService {

    public static final String REPO = "1953187487/StarDockLauncher";
    public static final String VERSION_URL = "https://raw.githubusercontent.com/" + REPO + "/main/launcher_version.json";

    public static class Release {
        public String tagName;
        public String name;
        public String body;
        public String publishedAt;
        public String apkUrl;
    }

    public static Release fetchLatest() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(VERSION_URL).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "StarDockLauncher-Update");
            int code = conn.getResponseCode();
            if (code != 200) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONObject rel = root.optJSONObject("latestRelease");
            if (rel == null) return null;
            Release r = new Release();
            r.tagName = "v" + rel.optString("versionName");
            r.name = "StarDock Launcher " + r.tagName;
            r.body = rel.optString("updateLog");
            JSONArray urls = rel.optJSONArray("url");
            if (urls != null && urls.length() > 0) r.apkUrl = urls.optString(0);
            return r;
        } catch (Throwable t) {
            android.util.Log.e("GitHubService", "fetchLatest failed", t);
            return null;
        }
    }

    public static List<Release> fetchAllReleases() {
        List<Release> list = new ArrayList<>();
        try {
            String api = "https://api.github.com/repos/" + REPO + "/releases?per_page=30";
            HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "StarDockLauncher-Updater");
            int code = conn.getResponseCode();
            if (code != 200) return list;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj == null) continue;
                Release r = new Release();
                r.tagName = obj.optString("tag_name");
                r.name = obj.optString("name");
                r.body = obj.optString("body");
                r.publishedAt = obj.optString("published_at");
                JSONArray assets = obj.optJSONArray("assets");
                if (assets != null) {
                    for (int j = 0; j < assets.length(); j++) {
                        JSONObject a = assets.optJSONObject(j);
                        if (a != null && a.optString("name", "").endsWith(".apk")) {
                            r.apkUrl = a.optString("browser_download_url");
                            break;
                        }
                    }
                }
                list.add(r);
            }
        } catch (Throwable t) {
            android.util.Log.e("GitHubService", "fetchAllReleases failed", t);
        }
        return list;
    }
}
