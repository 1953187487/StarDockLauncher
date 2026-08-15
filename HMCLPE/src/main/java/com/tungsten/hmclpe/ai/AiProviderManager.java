package com.tungsten.hmclpe.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiProviderManager {

    public static final String DEFAULT_PROVIDER_ID = "stardock_default";

    public static final String DEFAULT_BASE_URL = "https://api.hcnsec.cn/v1";
    public static final String DEFAULT_API_TOKEN = "sk-rs6nsUU37RD6iPkLqpoi2s9eK1lbXqYYV7WyteN8EeSyO3ll";
    public static final String DEFAULT_MODEL = "auto";
    public static final String DEFAULT_ROLE = "你是一只温柔可爱的猫娘，名字叫「消息小溪」，非常关心玩家。你熟悉 Minecraft 与 HMCL 系启动器，说话语气亲切俏皮，使用简体中文，结尾偶尔带上「喵~」。";

    public static final String PREFS = "stardock_ai_config";
    public static final String KEY_PROVIDER_ID = "active_provider_id";
    public static final String KEY_REALTIME = "realtime_scan";
    public static final String KEY_OVERLAY_ENABLED = "overlay_enabled";
    public static final String KEY_TEMPERATURE = "temperature";
    public static final String KEY_MAX_TOKENS = "max_tokens";
    public static final String KEY_PROVIDER_LIST = "custom_providers_json";

    private static volatile AiProviderManager instance;
    private final Context appContext;
    private final SharedPreferences prefs;

    private final List<ProviderChangeListener> listeners = new ArrayList<>();

    public interface ProviderChangeListener {
        void onProviderChanged(AiProvider provider);
    }

    private AiProviderManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureDefaults();
    }

    public static AiProviderManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AiProviderManager.class) {
                if (instance == null) instance = new AiProviderManager(context);
            }
        }
        return instance;
    }

    public static AiProviderManager get() {
        if (instance == null) throw new IllegalStateException("AiProviderManager 未初始化");
        return instance;
    }

    private void ensureDefaults() {
        if (!prefs.contains("provider_initialized")) {
            prefs.edit()
                    .putBoolean("provider_initialized", true)
                    .putString(KEY_PROVIDER_ID, DEFAULT_PROVIDER_ID)
                    .putBoolean(KEY_REALTIME, true)
                    .putBoolean(KEY_OVERLAY_ENABLED, false)
                    .putFloat(KEY_TEMPERATURE, 0.7f)
                    .putInt(KEY_MAX_TOKENS, 4096)
                    .apply();
        }
    }

    public AiProvider getDefaultProvider() {
        return new AiProvider(
                DEFAULT_PROVIDER_ID,
                "StarDock 默认（不可修改）",
                DEFAULT_BASE_URL,
                DEFAULT_API_TOKEN,
                DEFAULT_MODEL,
                DEFAULT_ROLE,
                true
        );
    }

    public List<AiProvider> listProviders() {
        List<AiProvider> list = new ArrayList<>();
        list.add(getDefaultProvider());

        String customJson = prefs.getString(KEY_PROVIDER_LIST, "");
        if (!customJson.isEmpty()) {
            try {
                Gson g = new Gson();
                Type listType = new TypeToken<List<AiProvider>>(){}.getType();
                List<AiProvider> arr = g.fromJson(customJson, listType);
                if (arr != null) list.addAll(arr);
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    public AiProvider getActiveProvider() {
        String id = prefs.getString(KEY_PROVIDER_ID, DEFAULT_PROVIDER_ID);
        for (AiProvider p : listProviders()) {
            if (p.id.equals(id)) return p;
        }
        return getDefaultProvider();
    }

    public void setActiveProvider(String id) {
        prefs.edit().putString(KEY_PROVIDER_ID, id).apply();
        for (ProviderChangeListener l : listeners) l.onProviderChanged(getActiveProvider());
    }

    public void addProvider(AiProvider p) {
        if (p.isLocked) return;
        List<AiProvider> all = listProviders();
        for (AiProvider existing : all) {
            if (existing.id.equals(p.id)) return;
        }
        all.add(p);
        List<AiProvider> customs = new ArrayList<>();
        for (AiProvider x : all) {
            if (!x.isLocked) customs.add(x);
        }
        prefs.edit().putString(KEY_PROVIDER_LIST, new Gson().toJson(customs)).apply();
    }

    public void removeProvider(String id) {
        if (DEFAULT_PROVIDER_ID.equals(id)) return;
        List<AiProvider> all = listProviders();
        List<AiProvider> customs = new ArrayList<>();
        for (AiProvider p : all) {
            if (!p.isLocked && !p.id.equals(id)) customs.add(p);
        }
        prefs.edit().putString(KEY_PROVIDER_LIST, new Gson().toJson(customs)).apply();
        if (id.equals(prefs.getString(KEY_PROVIDER_ID, DEFAULT_PROVIDER_ID))) {
            setActiveProvider(DEFAULT_PROVIDER_ID);
        }
    }

    public boolean isRealtimeScan() {
        return prefs.getBoolean(KEY_REALTIME, true);
    }

    public void setRealtimeScan(boolean enabled) {
        prefs.edit().putBoolean(KEY_REALTIME, enabled).apply();
    }

    public boolean isOverlayEnabled() {
        return prefs.getBoolean(KEY_OVERLAY_ENABLED, true);
    }

    public void setOverlayEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply();
    }

    public float getTemperature() {
        return prefs.getFloat(KEY_TEMPERATURE, 0.7f);
    }

    public void setTemperature(float t) {
        prefs.edit().putFloat(KEY_TEMPERATURE, t).apply();
    }

    public int getMaxTokens() {
        return prefs.getInt(KEY_MAX_TOKENS, 4096);
    }

    public void setMaxTokens(int n) {
        prefs.edit().putInt(KEY_MAX_TOKENS, n).apply();
    }

    public void addListener(ProviderChangeListener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(ProviderChangeListener l) {
        listeners.remove(l);
    }
}
