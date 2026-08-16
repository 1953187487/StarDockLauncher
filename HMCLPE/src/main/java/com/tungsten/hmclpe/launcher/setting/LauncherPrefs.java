package com.tungsten.hmclpe.launcher.setting;

public class LauncherPrefs {

    public static final String[] RUNTIMES = {
            "默认（自动）", "Java 8", "Java 11", "Java 17", "Java 21", "Java 25"
    };

    public static final String[] RENDERERS = {
            "默认（自动）", "GL4ES", "Zink (Mesa)", "LTW (TinyWINE)", "ANGLE"
    };

    public static final String[] DRIVERS = {
            "默认（自动）", "OpenGL ES", "Vulkan"
    };

    public static String currentRuntime() {
        return AppPrefs.getString(com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext(),
                AppPrefs.KEY_LAUNCH_RUNTIME, "默认（自动）");
    }

    public static void setRuntime(String v) {
        AppPrefs.setString(com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext(),
                AppPrefs.KEY_LAUNCH_RUNTIME, v);
    }

    public static String currentRenderer() {
        return AppPrefs.getString(com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext(),
                AppPrefs.KEY_LAUNCH_RENDERER, "默认（自动）");
    }

    public static void setRenderer(String v) {
        AppPrefs.setString(com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext(),
                AppPrefs.KEY_LAUNCH_RENDERER, v);
    }

    public static String currentDriver() {
        return AppPrefs.getString(com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext(),
                AppPrefs.KEY_LAUNCH_DRIVER, "默认（自动）");
    }

    public static void setDriver(String v) {
        AppPrefs.setString(com.tungsten.hmclpe.launcher.HMCLPEApplication.getContext(),
                AppPrefs.KEY_LAUNCH_DRIVER, v);
    }
}
