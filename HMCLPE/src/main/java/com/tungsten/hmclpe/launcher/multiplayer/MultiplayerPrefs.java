package com.tungsten.hmclpe.launcher.multiplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class MultiplayerPrefs {

    private static final String PREFS = "stardock_multiplayer";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LAST_INVITE = "last_invite";
    private static final String KEY_LAST_PORT = "last_port";
    private static final String KEY_NICKNAME = "nickname";

    private final SharedPreferences prefs;

    public MultiplayerPrefs(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public String getNickname() {
        return prefs.getString(KEY_NICKNAME, "StarDock玩家");
    }

    public void setNickname(String nick) {
        prefs.edit().putString(KEY_NICKNAME, nick).apply();
    }

    public String getLastInviteCode() {
        return prefs.getString(KEY_LAST_INVITE, "");
    }

    public void setLastInviteCode(String code) {
        prefs.edit().putString(KEY_LAST_INVITE, code).apply();
    }

    public int getLastPort() {
        return prefs.getInt(KEY_LAST_PORT, 0);
    }

    public void setLastPort(int port) {
        prefs.edit().putInt(KEY_LAST_PORT, port).apply();
    }
}
