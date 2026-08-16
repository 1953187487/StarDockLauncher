package com.tungsten.hmclpe.ai;

public class AiMessage {

    public String role;
    public String content;

    public AiMessage() {
    }

    public AiMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static AiMessage user(String content) {
        return new AiMessage("user", content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage("assistant", content);
    }

    public static AiMessage system(String content) {
        return new AiMessage("system", content);
    }
}
