package com.tungsten.hmclpe.launcher.multiplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class TaowaPrefs {

    private static final String NAME = "stardock_taowa";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LAST_ID = "last_id";

    public static boolean isEnabled(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
            return sp.getBoolean(KEY_ENABLED, false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
            sp.edit().putBoolean(KEY_ENABLED, enabled).apply();
        } catch (Throwable ignored) {}
    }

    public static String getLastId(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
            return sp.getString(KEY_LAST_ID, "");
        } catch (Throwable t) {
            return "";
        }
    }

    public static void setLastId(Context ctx, String id) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_LAST_ID, id == null ? "" : id).apply();
        } catch (Throwable ignored) {}
    }
}
