package com.tungsten.hmclpe.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AiProviderManager {

    public static final String PREFS_NAME = "ai_provider_config";
    public static final String KEY_PROVIDERS = "providers";
    public static final String KEY_ACTIVE_ID = "active_provider_id";
    public static final String KEY_ROLE = "ai_role";
    public static final String KEY_REALTIME_SCAN = "realtime_scan";

    public static final String DEFAULT_ROLE = "你是一只温柔可爱的猫娘，名字叫「消息小溪」，非常关心玩家。你熟悉 Minecraft 与 HMCL 系启动器，说话语气亲切俏皮，使用简体中文，结尾偶尔带上「喵~」。";

    private static AiProviderManager instance;

    private final Context context;
    private final List<AiProvider> providers;
    private String activeProviderId;
    private String role;
    private boolean realtimeScan;

    private AiProviderManager(Context context) {
        this.context = context.getApplicationContext();
        providers = new ArrayList<>();
        load();
    }

    public static synchronized AiProviderManager getInstance(Context context) {
        if (instance == null) {
            instance = new AiProviderManager(context);
        }
        return instance;
    }

    private void load() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        providers.clear();
        AiProvider def = AiProvider.createDefault();
        providers.add(def);
        try {
            String json = sp.getString(KEY_PROVIDERS, "");
            if (json != null && !json.isEmpty()) {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    AiProvider provider = AiProvider.fromJson(obj);
                    if (provider.id == null || provider.id.isEmpty() || provider.id.equals(AiProvider.DEFAULT_PROVIDER_ID)) {
                        continue;
                    }
                    providers.add(provider);
                }
            }
        } catch (Exception ignored) {
        }
        activeProviderId = sp.getString(KEY_ACTIVE_ID, AiProvider.DEFAULT_PROVIDER_ID);
        boolean activeExists = false;
        for (AiProvider provider : providers) {
            if (provider.id.equals(activeProviderId)) {
                activeExists = true;
                break;
            }
        }
        if (!activeExists) {
            activeProviderId = AiProvider.DEFAULT_PROVIDER_ID;
        }
        role = sp.getString(KEY_ROLE, "");
        if (role == null || role.isEmpty()) {
            role = DEFAULT_ROLE;
        }
        realtimeScan = sp.getBoolean(KEY_REALTIME_SCAN, true);
    }

    private void save() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        JSONArray array = new JSONArray();
        for (int i = 0; i < providers.size(); i++) {
            AiProvider provider = providers.get(i);
            if (provider.id.equals(AiProvider.DEFAULT_PROVIDER_ID)) {
                continue;
            }
            array.put(provider.toJson());
        }
        editor.putString(KEY_PROVIDERS, array.toString());
        editor.putString(KEY_ACTIVE_ID, activeProviderId);
        editor.putString(KEY_ROLE, role);
        editor.putBoolean(KEY_REALTIME_SCAN, realtimeScan);
        editor.apply();
    }

    public List<AiProvider> getProviders() {
        return providers;
    }

    public AiProvider getDefaultProvider() {
        for (AiProvider provider : providers) {
            if (provider.id.equals(AiProvider.DEFAULT_PROVIDER_ID)) {
                return provider;
            }
        }
        return AiProvider.createDefault();
    }

    public AiProvider getActiveProvider() {
        for (AiProvider provider : providers) {
            if (provider.id.equals(activeProviderId)) {
                return provider;
            }
        }
        return getDefaultProvider();
    }

    public void setActiveProvider(String id) {
        activeProviderId = id;
        save();
    }

    public void addProvider(AiProvider provider) {
        providers.add(provider);
        save();
    }

    public boolean removeProvider(String id) {
        for (int i = 0; i < providers.size(); i++) {
            AiProvider provider = providers.get(i);
            if (provider.id.equals(id)) {
                if (provider.locked) {
                    return false;
                }
                providers.remove(i);
                if (activeProviderId.equals(id)) {
                    activeProviderId = AiProvider.DEFAULT_PROVIDER_ID;
                }
                save();
                return true;
            }
        }
        return false;
    }

    public String getRole() {
        if (role == null || role.isEmpty()) {
            return DEFAULT_ROLE;
        }
        return role;
    }

    public void setRole(String role) {
        this.role = role;
        save();
    }

    public boolean isRealtimeScan() {
        return realtimeScan;
    }

    public void setRealtimeScan(boolean realtimeScan) {
        this.realtimeScan = realtimeScan;
        save();
    }
}
