package com.tungsten.hmclpe.launcher.download;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ModrinthService {

    public static class VersionInfo {
        public String id;
        public String name;
        public String versionNumber;
        public List<String> gameVersions = new ArrayList<>();
        public List<String> loaders = new ArrayList<>();
        public String primaryFileUrl;
        public String primaryFileName;
        public List<String> dependencies = new ArrayList<>();
    }

    public static VersionInfo fetchVersion(String versionId) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.modrinth.com/v2/version/" + versionId).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "StarDockLauncher/1.0.6");
            int code = conn.getResponseCode();
            if (code != 200) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject v = new JSONObject(sb.toString());
            VersionInfo info = new VersionInfo();
            info.id = v.optString("id");
            info.name = v.optString("name");
            info.versionNumber = v.optString("version_number");
            JSONArray gvs = v.optJSONArray("game_versions");
            if (gvs != null) for (int i = 0; i < gvs.length(); i++) info.gameVersions.add(gvs.optString(i));
            JSONArray lds = v.optJSONArray("loaders");
            if (lds != null) for (int i = 0; i < lds.length(); i++) info.loaders.add(lds.optString(i));
            JSONArray files = v.optJSONArray("files");
            if (files != null && files.length() > 0) {
                JSONObject f = files.optJSONObject(0);
                if (f != null) {
                    info.primaryFileUrl = f.optString("url");
                    info.primaryFileName = f.optString("filename");
                }
            }
            JSONArray deps = v.optJSONArray("dependencies");
            if (deps != null) {
                for (int i = 0; i < deps.length(); i++) {
                    JSONObject d = deps.optJSONObject(i);
                    if (d != null && "required".equals(d.optString("dependency_type"))) {
                        info.dependencies.add(d.optString("version_id"));
                    }
                }
            }
            return info;
        } catch (Throwable t) {
            android.util.Log.e("ModrinthService", "fetchVersion failed", t);
            return null;
        }
    }

    public static List<VersionInfo> searchVersions(String projectSlug, String mcVersion) {
        List<VersionInfo> list = new ArrayList<>();
        try {
            String url = "https://api.modrinth.com/v2/project/" + projectSlug + "/version?game_versions=" + URLEncoder.encode("[\"" + mcVersion + "\"]", "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "StarDockLauncher/1.0.6");
            int code = conn.getResponseCode();
            if (code != 200) return list;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject v = arr.optJSONObject(i);
                if (v == null) continue;
                VersionInfo info = new VersionInfo();
                info.id = v.optString("id");
                info.name = v.optString("name");
                info.versionNumber = v.optString("version_number");
                JSONArray gvs = v.optJSONArray("game_versions");
                if (gvs != null) for (int j = 0; j < gvs.length(); j++) info.gameVersions.add(gvs.optString(j));
                JSONArray lds = v.optJSONArray("loaders");
                if (lds != null) for (int j = 0; j < lds.length(); j++) info.loaders.add(lds.optString(j));
                JSONArray files = v.optJSONArray("files");
                if (files != null && files.length() > 0) {
                    JSONObject f = files.optJSONObject(0);
                    if (f != null) {
                        info.primaryFileUrl = f.optString("url");
                        info.primaryFileName = f.optString("filename");
                    }
                }
                list.add(info);
            }
        } catch (Throwable t) {
            android.util.Log.e("ModrinthService", "searchVersions failed", t);
        }
        return list;
    }
}
