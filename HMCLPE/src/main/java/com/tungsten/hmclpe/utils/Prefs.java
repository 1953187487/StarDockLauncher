package com.tungsten.hmclpe.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Prefs {

    private static Prefs instance;
    private SharedPreferences sp;

    private Prefs(Context ctx) {
        sp = ctx.getSharedPreferences("stardock_prefs", Context.MODE_PRIVATE);
    }

    public static synchronized Prefs get(Context ctx) {
        if (instance == null) {
            instance = new Prefs(ctx.getApplicationContext());
        }
        return instance;
    }

    public String getString(String key, String def) {
        try {
            return sp.getString(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public void putString(String key, String value) {
        try {
            sp.edit().putString(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    public int getInt(String key, int def) {
        try {
            return sp.getInt(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public void putInt(String key, int value) {
        try {
            sp.edit().putInt(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    public boolean getBool(String key, boolean def) {
        try {
            return sp.getBoolean(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public void putBool(String key, boolean value) {
        try {
            sp.edit().putBoolean(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    public long getLong(String key, long def) {
        try {
            return sp.getLong(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    public void putLong(String key, long value) {
        try {
            sp.edit().putLong(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    public Set<String> getStringSet(String key, Set<String> def) {
        try {
            return sp.getStringSet(key, def);
        } catch (Throwable t) {
            return def == null ? new HashSet<>() : def;
        }
    }

    public void putStringSet(String key, Set<String> value) {
        try {
            sp.edit().putStringSet(key, value).apply();
        } catch (Throwable ignored) {
        }
    }
}
