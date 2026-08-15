package com.tungsten.hmclpe.launcher.multiplayer;

import android.content.Context;
import android.util.Log;

import java.security.MessageDigest;
import java.util.Random;

public final class InviteCodeUtil {

    private static final String TAG = "InviteCodeUtil";
    private static final char[] BASE32 = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private InviteCodeUtil() {}

    public static String generateCode(Context context, int port) {
        try {
            String seed = context.getPackageName() + ":" + System.currentTimeMillis() + ":" + port + ":" + (new Random().nextInt());
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(seed.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                int idx = (digest[i] & 0xff) % BASE32.length;
                sb.append(BASE32[idx]);
                if (i == 3) sb.append('-');
            }
            return sb.toString();
        } catch (Throwable t) {
            Log.w(TAG, "generateCode fallback", t);
            return "SD-XXXX-XXXX".replace("XXXX", random4());
        }
    }

    private static String random4() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(BASE32[r.nextInt(BASE32.length)]);
        return sb.toString();
    }

    public static String buildInviteLink(String code, int port) {
        return "stardock://join?code=" + code + "&port=" + port;
    }

    public static int parsePort(String url) {
        if (url == null) return 0;
        try {
            int idx = url.indexOf("port=");
            if (idx >= 0) {
                String tail = url.substring(idx + 5);
                StringBuilder n = new StringBuilder();
                for (int i = 0; i < tail.length(); i++) {
                    char c = tail.charAt(i);
                    if (Character.isDigit(c)) n.append(c); else break;
                }
                return Integer.parseInt(n.toString());
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static String parseCode(String url) {
        if (url == null) return "";
        try {
            int idx = url.indexOf("code=");
            if (idx >= 0) {
                String tail = url.substring(idx + 5);
                int amp = tail.indexOf('&');
                return amp > 0 ? tail.substring(0, amp) : tail;
            }
        } catch (Throwable ignored) {}
        return "";
    }
}
