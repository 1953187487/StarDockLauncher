package com.tungsten.hmclpe.launcher.runtime;

import android.content.Context;

import com.tungsten.hmclpe.utils.Prefs;

public class RuntimePrefs {

    private static final String KEY_ENGINE = "launcher_engine";

    public static final String ENGINE_BOAT = "boat";
    public static final String ENGINE_POJAV = "pojav";

    public static String getEngine(Context ctx) {
        String engine = Prefs.get(ctx).getString(KEY_ENGINE, ENGINE_BOAT);
        if (!ENGINE_BOAT.equals(engine) && !ENGINE_POJAV.equals(engine)) {
            engine = ENGINE_BOAT;
        }
        return engine;
    }

    public static void setEngine(Context ctx, String engine) {
        Prefs.get(ctx).putString(KEY_ENGINE, engine);
    }

    public static boolean isBoat(Context ctx) {
        return ENGINE_BOAT.equals(getEngine(ctx));
    }
}
