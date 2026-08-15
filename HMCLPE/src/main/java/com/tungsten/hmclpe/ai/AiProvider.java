package com.tungsten.hmclpe.ai;

import org.json.JSONException;
import org.json.JSONObject;

public class AiProvider {

    public String id;
    public String name;
    public String baseUrl;
    public String apiKey;
    public String model;
    public String systemPrompt;
    public boolean isLocked;

    public AiProvider() {
    }

    public AiProvider(String id, String name, String baseUrl, String apiKey, String model, String systemPrompt, boolean isLocked) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.isLocked = isLocked;
    }

    public String getChatCompletionsUrl() {
        if (baseUrl == null) return null;
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (base.endsWith("/chat/completions")) return base;
        if (base.endsWith("/v1")) return base + "/chat/completions";
        return base + "/chat/completions";
    }

    public String getWebSocketUrl() {
        if (baseUrl == null) return null;
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (base.startsWith("https://")) base = "wss://" + base.substring(8);
        else if (base.startsWith("http://")) base = "ws://" + base.substring(7);
        if (!base.endsWith("/v1")) base = base + "/v1";
        return base + "/chat/completions?token=" + apiKey;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            json.put("baseUrl", baseUrl);
            json.put("apiKey", apiKey);
            json.put("model", model);
            json.put("systemPrompt", systemPrompt == null ? "" : systemPrompt);
            json.put("isLocked", isLocked);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static AiProvider fromJson(JSONObject json) {
        return new AiProvider(
                json.optString("id"),
                json.optString("name"),
                json.optString("baseUrl"),
                json.optString("apiKey"),
                json.optString("model", "auto"),
                json.optString("systemPrompt", ""),
                json.optBoolean("isLocked", false)
        );
    }
}
