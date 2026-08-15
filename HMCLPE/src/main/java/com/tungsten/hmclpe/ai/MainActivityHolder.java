package com.tungsten.hmclpe.ai;

import com.tungsten.hmclpe.launcher.MainActivity;

public class MainActivityHolder {

    private static MainActivity instance;

    public static void set(MainActivity activity) {
        instance = activity;
    }

    public static MainActivity get() {
        return instance;
    }

    public static void clear() {
        instance = null;
    }
}
