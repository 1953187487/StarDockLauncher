package com.tungsten.hmclpe.launcher.setting;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {

    private static final String NAME = "stardock_prefs";

    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_BACKGROUND_URI = "background_uri";
    public static final String KEY_DOWNLOAD_SOURCE = "download_source";
    public static final String KEY_GAME_DIR = "game_dir";
    public static final String KEY_LOGIN_MODE = "login_mode";
    public static final String KEY_LOGIN_NICKNAME = "login_nickname";
    public static final String KEY_LOGIN_SERVER = "login_server";
    public static final String KEY_AI_AGREED = "ai_agreed";
    public static final String KEY_USER_AGREED = "user_agreed";
    public static final String KEY_LANG_AGREED = "lang_agreed";
    public static final String KEY_LAST_GAME_VERSION = "last_game_version";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_ANNOUNCEMENT_SEEN = "announcement_seen";
    public static final String KEY_ANNOUNCEMENT_ENABLED = "announcement_enabled";
    public static final String KEY_AI_ENABLED = "ai_enabled";
    public static final String KEY_AI_BASE_URL = "ai_base_url";
    public static final String KEY_AI_API_KEY = "ai_api_key";
    public static final String KEY_AI_MODEL = "ai_model";
    public static final String KEY_AI_CHARACTER = "ai_character";
    public static final String KEY_LAUNCH_RUNTIME = "launch_runtime";
    public static final String KEY_LAUNCH_RENDERER = "launch_renderer";
    public static final String KEY_LAUNCH_DRIVER = "launch_driver";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String getString(Context c, String key, String def) {
        try { return sp(c).getString(key, def); } catch (Throwable t) { return def; }
    }
    public static void setString(Context c, String key, String val) {
        try { sp(c).edit().putString(key, val).apply(); } catch (Throwable ignored) {}
    }
    public static int getInt(Context c, String key, int def) {
        try { return sp(c).getInt(key, def); } catch (Throwable t) { return def; }
    }
    public static void setInt(Context c, String key, int val) {
        try { sp(c).edit().putInt(key, val).apply(); } catch (Throwable ignored) {}
    }
    public static boolean getBool(Context c, String key, boolean def) {
        try { return sp(c).getBoolean(key, def); } catch (Throwable t) { return def; }
    }
    public static void setBool(Context c, String key, boolean val) {
        try { sp(c).edit().putBoolean(key, val).apply(); } catch (Throwable ignored) {}
    }
}
