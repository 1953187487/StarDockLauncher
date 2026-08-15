package com.tungsten.hmclpe.ai;

import org.json.JSONException;
import org.json.JSONObject;

public class AiProvider {

    public static final String DEFAULT_PROVIDER_ID = "builtin_default";

    public String id;
    public String name;
    public String baseUrl;
    public String apiKey;
    public String model;
    public boolean locked;

    public AiProvider(String id, String name, String baseUrl, String apiKey, String model, boolean locked) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.locked = locked;
    }

    public static AiProvider createDefault() {
        return new AiProvider(
                DEFAULT_PROVIDER_ID,
                "AI 智能助手（默认）",
                "https://api.hcnsec.cn/v1",
                "sk-rs6nsUU37RD6iPkLqpoi2s9eK1lbXqYYV7WyteN8EeSyO3ll",
                "auto",
                true
        );
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            json.put("baseUrl", baseUrl);
            json.put("apiKey", apiKey);
            json.put("model", model);
            json.put("locked", locked);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static AiProvider fromJson(JSONObject json) {
        AiProvider provider = new AiProvider(
                json.optString("id"),
                json.optString("name"),
                json.optString("baseUrl"),
                json.optString("apiKey"),
                json.optString("model", "auto"),
                json.optBoolean("locked", false)
        );
        return provider;
    }
}
