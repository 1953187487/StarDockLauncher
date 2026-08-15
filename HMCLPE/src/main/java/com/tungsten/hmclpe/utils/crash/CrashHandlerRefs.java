package com.tungsten.hmclpe.utils.crash;

public final class CrashHandlerRefs {
    private CrashHandlerRefs() {}
    public static Class<?> crashLogViewerClass() {
        try {
            return Class.forName("com.tungsten.hmclpe.utils.crash.CrashLogViewerActivity");
        } catch (Throwable t) {
            return null;
        }
    }
}
