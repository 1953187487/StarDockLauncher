package com.tungsten.hmclpe.launcher.fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MinecraftVersionService {

    public static class MinecraftVersion {
        public String id;
        public String type;
        public String releaseTime;
        public MinecraftVersion(String id, String type, String releaseTime) {
            this.id = id; this.type = type; this.releaseTime = releaseTime;
        }
    }

    public static class MinecraftVersionFile {
        public String id;
        public String name;
        public String path;
        public MinecraftVersionFile(String id, String name, String path) {
            this.id = id; this.name = name; this.path = path;
        }
    }

    public static List<MinecraftVersion> list() {
        List<MinecraftVersion> versions = new ArrayList<>();
        try {
            String url = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "StarDockLauncher/1.0.6");
            int code = conn.getResponseCode();
            if (code != 200) return versions;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.optJSONArray("versions");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject v = arr.optJSONObject(i);
                    if (v != null) versions.add(new MinecraftVersion(v.optString("id"), v.optString("type"), v.optString("releaseTime")));
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("MinecraftVersionService", "list failed", t);
        }
        return versions;
    }

    public static List<MinecraftVersionFile> assetsFor(String mcVersion) {
        List<MinecraftVersionFile> files = new ArrayList<>();
        try {
            String listUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
            HttpURLConnection conn = (HttpURLConnection) new URL(listUrl).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "StarDockLauncher/1.0.6");
            int code = conn.getResponseCode();
            if (code != 200) return files;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.optJSONArray("versions");
            String versionUrl = null;
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject v = arr.optJSONObject(i);
                    if (v != null && mcVersion.equals(v.optString("id"))) {
                        versionUrl = v.optString("url");
                        break;
                    }
                }
            }
            if (versionUrl == null) return files;
            String verDir = "mc/game/" + mcVersion;
            files.add(new MinecraftVersionFile("client_" + mcVersion, mcVersion + "/client.jar", verDir + "/client.jar"));
            files.add(new MinecraftVersionFile("client_index_" + mcVersion, mcVersion + "/version.json", verDir + "/" + mcVersion + ".json"));
        } catch (Throwable t) {
            android.util.Log.e("MinecraftVersionService", "assetsFor failed", t);
        }
        return files;
    }
}
