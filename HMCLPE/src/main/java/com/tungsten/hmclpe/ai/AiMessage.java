package com.tungsten.hmclpe.ai;

import org.json.JSONException;
import org.json.JSONObject;

public class AiMessage {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    public String role;
    public String content;

    public AiMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("role", role);
            json.put("content", content);
        } catch (JSONException ignored) {
        }
        return json;
    }
}
