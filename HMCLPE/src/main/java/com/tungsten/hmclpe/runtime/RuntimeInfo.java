package com.tungsten.hmclpe.runtime;

import android.content.Context;

import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.launcher.runtime.RuntimeInstaller;

import java.io.File;

public class RuntimeInfo {

    public static final String ENGINE_BOAT = "boat";
    public static final String ENGINE_POJAV = "pojav";

    private final File runtimeDir;
    private final File javaDir;

    public RuntimeInfo(File runtimeDir) {
        this.runtimeDir = runtimeDir;
        this.javaDir = new File(runtimeDir, "java");
    }

    public static RuntimeInfo from(Context ctx) {
        return new RuntimeInfo(RuntimeInstaller.getRuntimeDir(ctx));
    }

    public boolean ready() {
        return runtimeDir != null && runtimeDir.exists() && javaDir != null;
    }

    public File java8() {
        File f = new File(javaDir, "default");
        return f.exists() ? f : null;
    }

    public File java17() {
        File f = new File(javaDir, "JRE17");
        return f.exists() ? f : null;
    }

    public File boatLib() {
        File f = new File(runtimeDir, "boat");
        return f.exists() ? f : null;
    }

    public File pojavLib() {
        File f = new File(runtimeDir, "pojav");
        return f.exists() ? f : null;
    }

    public File rendererDir() {
        File f = new File(runtimeDir, "boat/renderer");
        return f.exists() ? f : null;
    }

    public static File gameDir(Context ctx) {
        return new File(AppManifest.GAME_DIR);
    }

    public static File versionDir(Context ctx) {
        return new File(AppManifest.VERSION_DIR);
    }
}
