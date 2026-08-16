package com.tungsten.hmclpe.ai;

import android.content.Context;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class AiProviderManager {

    private static volatile AiProviderManager instance;
    private final List<AiProvider> providers = new ArrayList<>();
    private final Gson gson = new Gson();
    private final File cfgFile;

    private AiProviderManager(Context ctx) {
        File dir = new File(ctx.getCacheDir(), "ai_config");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        cfgFile = new File(dir, "providers.json");
        providers.add(new AiProvider("deepseek", "DeepSeek", "https://api.deepseek.com/v1/chat/completions"));
        providers.add(new AiProvider("openai", "OpenAI 兼容", "https://api.openai.com/v1/chat/completions"));
        providers.add(new AiProvider("mock", "内置离线演示", "local://mock"));
        load();
    }

    public static AiProviderManager get(Context ctx) {
        if (instance == null) {
            synchronized (AiProviderManager.class) {
                if (instance == null) {
                    instance = new AiProviderManager(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public List<AiProvider> all() {
        return providers;
    }

    public AiProvider active() {
        for (AiProvider p : providers) {
            if (p.active) {
                return p;
            }
        }
        if (!providers.isEmpty()) {
            providers.get(0).active = true;
        }
        return providers.isEmpty() ? null : providers.get(0);
    }

    public void setActive(String id) {
        for (AiProvider p : providers) {
            p.active = id != null && id.equals(p.id);
        }
        save();
    }

    public AiProvider find(String id) {
        for (AiProvider p : providers) {
            if (id != null && id.equals(p.id)) {
                return p;
            }
        }
        return null;
    }

    public void saveAll() {
        save();
    }

    private void load() {
        if (!cfgFile.exists()) {
            return;
        }
        try (FileReader fr = new FileReader(cfgFile)) {
            AiProvider[] arr = gson.fromJson(fr, AiProvider[].class);
            if (arr != null) {
                for (AiProvider p : arr) {
                    AiProvider exist = find(p.id);
                    if (exist != null) {
                        exist.apiKey = p.apiKey;
                        exist.baseUrl = p.baseUrl;
                        exist.model = p.model;
                        exist.active = p.active;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void save() {
        try (FileWriter fw = new FileWriter(cfgFile)) {
            gson.toJson(providers.toArray(new AiProvider[0]), fw);
        } catch (Throwable ignored) {
        }
    }
}
