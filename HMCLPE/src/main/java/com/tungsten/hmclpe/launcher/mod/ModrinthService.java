package com.tungsten.hmclpe.launcher.mod;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ModrinthService {

    private static final String TAG = "ModrinthService";
    private static final String API = "https://api.modrinth.com/v2";
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();
    private static final Gson GSON = new Gson();

    public static class SearchResult {
        public List<ModrinthProject> hits = new ArrayList<>();
        public int total_hits;
    }

    public interface SearchCallback {
        void onResult(List<ModrinthProject> projects, int total);

        void onError(Throwable t);
    }

    public interface VersionCallback {
        void onResult(ModrinthVersion version);

        void onError(Throwable t);
    }

    public void search(String query, String type, int limit, int offset, SearchCallback cb) {
        new Thread(() -> {
            try {
                StringBuilder url = new StringBuilder(API).append("/search?limit=").append(limit).append("&offset=").append(offset);
                if (query != null && !query.isEmpty()) {
                    url.append("&query=").append(query.replace(" ", "%20"));
                }
                if (type != null && !type.isEmpty()) {
                    url.append("&facets=[[\"project_type:").append(type).append("\"]]");
                }
                Request req = new Request.Builder().url(url.toString()).build();
                try (Response resp = CLIENT.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        if (cb != null) {
                            cb.onError(new IOException("HTTP " + resp.code()));
                        }
                        return;
                    }
                    String body = resp.body().string();
                    SearchResult result = GSON.fromJson(body, SearchResult.class);
                    if (cb != null) {
                        cb.onResult(result == null ? new ArrayList<>() : result.hits,
                                result == null ? 0 : result.total_hits);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "search failed", t);
                if (cb != null) {
                    cb.onError(t);
                }
            }
        }).start();
    }

    public void getLatestVersion(String projectId, VersionCallback cb) {
        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(API + "/project/" + projectId + "/version?featured=true")
                        .build();
                try (Response resp = CLIENT.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        if (cb != null) {
                            cb.onError(new IOException("HTTP " + resp.code()));
                        }
                        return;
                    }
                    String body = resp.body().string();
                    ModrinthVersion[] versions = GSON.fromJson(body, ModrinthVersion[].class);
                    ModrinthVersion pick = null;
                    if (versions != null && versions.length > 0) {
                        for (ModrinthVersion v : versions) {
                            if (v.primaryUrl() != null) {
                                pick = v;
                                break;
                            }
                        }
                        if (pick == null) {
                            pick = versions[0];
                        }
                    }
                    if (cb != null) {
                        cb.onResult(pick);
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "getLatestVersion failed", t);
                if (cb != null) {
                    cb.onError(t);
                }
            }
        }).start();
    }
}
