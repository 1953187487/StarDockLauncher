package com.tungsten.hmclpe.multiplayer;

import android.content.Context;
import android.content.SharedPreferences;

public final class Hin2nConfig {

    private static final String PREFS = "stardock_multiplayer";
    private static final String KEY_RELAY_HOST = "relay_host";
    private static final String KEY_RELAY_PORT = "relay_port";
    private static final String KEY_VIRTUAL_NET = "virtual_net";
    private static final String KEY_GAME_PORT = "game_port";

    public static final String DEFAULT_RELAY_HOST = "relay.stardock.example.com";
    public static final int DEFAULT_RELAY_PORT = 35001;
    public static final String DEFAULT_VIRTUAL_NET = "10.10.0";
    public static final int DEFAULT_GAME_PORT = 25565;

    public static final int PUNCH_PORT = 35000;
    public static final int HEARTBEAT_INTERVAL_MS = 15000;
    public static final int HEARTBEAT_TIMEOUT_MS = 45000;

    private Hin2nConfig() {
    }

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getRelayHost(Context ctx) {
        return prefs(ctx).getString(KEY_RELAY_HOST, DEFAULT_RELAY_HOST);
    }

    public static void setRelayHost(Context ctx, String host) {
        prefs(ctx).edit().putString(KEY_RELAY_HOST, host).apply();
    }

    public static int getRelayPort(Context ctx) {
        return prefs(ctx).getInt(KEY_RELAY_PORT, DEFAULT_RELAY_PORT);
    }

    public static void setRelayPort(Context ctx, int port) {
        prefs(ctx).edit().putInt(KEY_RELAY_PORT, port).apply();
    }

    public static String getVirtualNet(Context ctx) {
        return prefs(ctx).getString(KEY_VIRTUAL_NET, DEFAULT_VIRTUAL_NET);
    }

    public static int getGamePort(Context ctx) {
        return prefs(ctx).getInt(KEY_GAME_PORT, DEFAULT_GAME_PORT);
    }

    public static void setGamePort(Context ctx, int port) {
        prefs(ctx).edit().putInt(KEY_GAME_PORT, port).apply();
    }
}
