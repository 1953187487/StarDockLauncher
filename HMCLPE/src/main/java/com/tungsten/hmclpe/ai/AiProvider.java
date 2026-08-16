package com.tungsten.hmclpe.ai;

public class AiProvider {

    public String id;
    public String displayName;
    public String baseUrl;
    public String apiKey;
    public String model = "deepseek-chat";
    public boolean active;

    public AiProvider() {
    }

    public AiProvider(String id, String displayName, String baseUrl) {
        this.id = id;
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }

    public String displayName() {
        return displayName == null ? (id == null ? "未命名" : id) : displayName;
    }
}
