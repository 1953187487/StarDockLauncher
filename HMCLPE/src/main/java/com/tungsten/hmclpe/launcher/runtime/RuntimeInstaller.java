package com.tungsten.hmclpe.launcher.runtime;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RuntimeInstaller {

    private static final String TAG = "RuntimeInstaller";
    private static final String ASSET_ROOT = "app_runtime";

    public interface Callback {
        void onProgress(String stage, int percent);

        void onDone(File runtimeDir);

        void onError(Throwable t);
    }

    private static volatile boolean installing;

    public static boolean isInstalling() {
        return installing;
    }

    public static synchronized void ensure(Context ctx, Callback cb) {
        if (installing) {
            if (cb != null) {
                cb.onError(new IllegalStateException("运行时正在安装中"));
            }
            return;
        }
        installing = true;
        new Thread(() -> {
            try {
                File runtimeDir = getRuntimeDir(ctx);
                File versionFile = new File(runtimeDir, "version");
                String assetVersion = readAssetVersion(ctx);
                boolean needInstall = !versionFile.exists()
                        || !versionFile.getParentFile().exists()
                        || assetVersion == null
                        || !assetVersion.trim().equals(readFile(versionFile).trim());
                if (!needInstall && !new File(runtimeDir, "boat").exists()) {
                    needInstall = true;
                }
                if (!needInstall) {
                    if (cb != null) {
                        cb.onDone(runtimeDir);
                    }
                    return;
                }
                copyAssetDir(ctx, ASSET_ROOT, runtimeDir, cb);
                if (cb != null) {
                    cb.onDone(runtimeDir);
                }
            } catch (Throwable t) {
                Log.e(TAG, "install failed", t);
                if (cb != null) {
                    cb.onError(t);
                }
            } finally {
                installing = false;
            }
        }, "runtime-install").start();
    }

    public static File getRuntimeDir(Context ctx) {
        return new File(ctx.getDir("runtime", Context.MODE_PRIVATE), "current");
    }

    public static File getJavaDir(Context ctx) {
        return new File(getRuntimeDir(ctx), "java");
    }

    public static File getJava17(Context ctx) {
        return new File(getJavaDir(ctx), "JRE17");
    }

    public static File getJava8(Context ctx) {
        return new File(getJavaDir(ctx), "default");
    }

    public static File getBoatLib(Context ctx) {
        return new File(getRuntimeDir(ctx), "boat");
    }

    public static File getPojavLib(Context ctx) {
        return new File(getRuntimeDir(ctx), "pojav");
    }

    public static String readFile(File f) {
        if (f == null || !f.exists()) {
            return "";
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[(int) Math.min(4096, f.length())];
            int n = fis.read(buf);
            return new String(buf, 0, Math.max(0, n));
        } catch (Throwable t) {
            return "";
        }
    }

    private static String readAssetVersion(Context ctx) {
        try (InputStream in = ctx.getAssets().open(ASSET_ROOT + "/version")) {
            byte[] buf = new byte[1024];
            int n = in.read(buf);
            return new String(buf, 0, Math.max(0, n));
        } catch (Throwable t) {
            Log.w(TAG, "no asset version", t);
            return null;
        }
    }

    private static void copyAssetDir(Context ctx, String assetPath, File dest, Callback cb) throws IOException {
        String[] children;
        try {
            children = ctx.getAssets().list(assetPath);
        } catch (IOException e) {
            copyFile(ctx, assetPath, dest);
            return;
        }
        if (children == null || children.length == 0) {
            copyFile(ctx, assetPath, dest);
            return;
        }
        if (!dest.exists()) {
            dest.mkdirs();
        }
        int total = countAssets(ctx, assetPath);
        final int[] counter = {0};
        copyAssetsRecursive(ctx, assetPath, dest, total, counter, cb);
    }

    private static int countAssets(Context ctx, String assetPath) {
        try {
            String[] children = ctx.getAssets().list(assetPath);
            if (children == null || children.length == 0) {
                return 1;
            }
            int total = 0;
            for (String c : children) {
                total += countAssets(ctx, assetPath + "/" + c);
            }
            return total;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void copyAssetsRecursive(Context ctx, String assetPath, File dest, int total, int[] counter, Callback cb) throws IOException {
        String[] children;
        try {
            children = ctx.getAssets().list(assetPath);
        } catch (IOException e) {
            copyFile(ctx, assetPath, dest);
            counter[0]++;
            report(cb, dest.getName(), counter[0] * 100 / Math.max(1, total));
            return;
        }
        if (children == null || children.length == 0) {
            copyFile(ctx, assetPath, dest);
            counter[0]++;
            report(cb, dest.getName(), counter[0] * 100 / Math.max(1, total));
            return;
        }
        if (!dest.exists()) {
            dest.mkdirs();
        }
        for (String child : children) {
            copyAssetsRecursive(ctx, assetPath + "/" + child, new File(dest, child), total, counter, cb);
        }
    }

    private static void copyFile(Context ctx, String assetPath, File dest) throws IOException {
        try (InputStream in = ctx.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }

    private static void report(Callback cb, String stage, int percent) {
        if (cb != null) {
            cb.onProgress(stage, percent);
        }
    }
}
