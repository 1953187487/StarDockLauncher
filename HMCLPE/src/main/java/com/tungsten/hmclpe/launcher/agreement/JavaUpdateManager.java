package com.tungsten.hmclpe.launcher.agreement;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.tungsten.hmclpe.utils.file.FileStringUtils;

import java.io.File;

/**
 * Reads the bundled JAVA runtime version file shipped under
 * assets/app_runtime/java/{default,JRE17}/version and compares it against
 * the version recorded by the user. Returns 0 if the file is missing
 * (treated as "no JAVA bundled").
 */
public final class JavaUpdateManager {

    private static final String TAG = "JavaUpdate";

    public static final String JAVA_DEFAULT = "default";
    public static final String JAVA_JRE17 = "JRE17";

    private final Context ctx;

    public JavaUpdateManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public static final class RuntimeInfo {
        public final String name;
        public final int installedVersion;
        public final int bundledVersion;
        public final boolean needsUpdate;

        RuntimeInfo(String name, int installed, int bundled) {
            this.name = name;
            this.installedVersion = installed;
            this.bundledVersion = bundled;
            this.needsUpdate = installed < bundled;
        }

        public String displayLabel() {
            switch (name) {
                case JAVA_DEFAULT:
                    return "Java 8";
                case JAVA_JRE17:
                    return "Java 17";
                default:
                    return name;
            }
        }
    }

    @WorkerThread
    @NonNull
    public RuntimeInfo inspect(String name, String externalDir) {
        int installed = readVersionFromFile(new File(externalDir, "version"));
        int bundled = readVersionFromAssets("app_runtime/java/" + name + "/version");
        if (bundled == Integer.MIN_VALUE) {
            bundled = installed;
        }
        return new RuntimeInfo(name, installed, bundled);
    }

    private int readVersionFromFile(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        try {
            String s = FileStringUtils.getStringFromFile(file.getAbsolutePath());
            return s == null ? 0 : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "bad version file: " + file, e);
            return 0;
        }
    }

    private int readVersionFromAssets(String relativePath) {
        try (java.io.InputStream is = ctx.getAssets().open(relativePath);
             java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
            String s = br.readLine();
            return s == null ? Integer.MIN_VALUE : Integer.parseInt(s.trim());
        } catch (Exception e) {
            Log.w(TAG, "cannot read asset version: " + relativePath, e);
            return Integer.MIN_VALUE;
        }
    }
}
