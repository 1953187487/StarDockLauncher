package net.kdt.pojavlaunch.stardock.auth;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Yggdrasil / authlib-injector compatible authentication client.
 * Supports two-step login against a third-party Minecraft auth server.
 */
public class ThirdPartyAuthService {

    private static final String TAG = "ThirdPartyAuth";
    private static final int TIMEOUT_MS = 12000;

    /**
     * Resolve an authentication endpoint URL from a user-supplied base URL.
     * The user may enter either the server root (https://example.com) or the
     * full authserver URL (.../authserver/authenticate).
     */
    @NonNull
    public static String resolveAuthEndpoint(@NonNull String input) {
        String url = input.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/authserver/authenticate")) return url;
        if (url.endsWith("/authserver")) return url + "/authenticate";
        // Try the common Yggdrasil layout: <root>/api/yggdrasil/authserver/authenticate
        return url + "/api/yggdrasil/authserver/authenticate";
    }

    /** Validate URL looks roughly like an http(s) URL. */
    public static boolean isValidUrl(@Nullable String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase();
        return u.startsWith("http://") || u.startsWith("https://");
    }

    /**
     * Probe a base URL to make sure the authserver responds.
     */
    public static boolean probe(@NonNull String authEndpoint) throws Exception {
        URL url = new URL(authEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "StarDockLauncher/0.0.3");
        int code;
        try { code = conn.getResponseCode(); } finally { conn.disconnect(); }
        // 405 method not allowed or 200/400 means the endpoint exists.
        return code == 200 || code == 400 || code == 405;
    }

    /**
     * Attempt authentication.
     *
     * @return AccountProfile on success
     * @throws Exception on failure (network or invalid credentials)
     */
    @NonNull
    public static AccountProfile authenticate(@NonNull String authEndpoint,
                                              @NonNull String username,
                                              @NonNull String password,
                                              @NonNull String clientToken) throws Exception {
        URL url = new URL(authEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "StarDockLauncher/0.0.3");
        conn.setDoOutput(true);

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        body.addProperty("clientToken", clientToken);
        body.addProperty("requestUser", true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String responseBody = readAll(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new RuntimeException(parseError(responseBody, code));
        }

        JsonObject root = Tools.GLOBAL_GSON.fromJson(responseBody, JsonObject.class);
        String accessToken = root.get("accessToken").getAsString();
        String returnedClientToken = root.has("clientToken") && !root.get("clientToken").isJsonNull()
                ? root.get("clientToken").getAsString() : clientToken;
        JsonObject selected = root.getAsJsonArray("selectedProfile").get(0).getAsJsonObject();
        String uuid = selected.get("id").getAsString();
        String name = selected.get("name").getAsString();
        Log.i(TAG, "Authenticated third-party account: " + name);
        return new AccountProfile(accessToken, returnedClientToken, uuid, name, username);
    }

    private static String parseError(String body, int code) {
        try {
            JsonObject obj = Tools.GLOBAL_GSON.fromJson(body, JsonObject.class);
            if (obj.has("errorMessage")) return obj.get("errorMessage").getAsString();
            if (obj.has("error")) return obj.get("error").getAsString();
        } catch (Exception ignored) {
        }
        return "HTTP " + code;
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    public static class AccountProfile {
        public final String accessToken;
        public final String clientToken;
        public final String uuid;
        public final String name;
        public final String username;

        AccountProfile(String accessToken, String clientToken, String uuid, String name, String username) {
            this.accessToken = accessToken;
            this.clientToken = clientToken;
            this.uuid = uuid;
            this.name = name;
            this.username = username;
        }
    }
}
