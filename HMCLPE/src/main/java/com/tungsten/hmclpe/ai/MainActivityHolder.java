package com.tungsten.hmclpe.ai;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivityHolder {

    private static AppCompatActivity instance;

    public static void set(AppCompatActivity activity) {
        instance = activity;
    }

    public static AppCompatActivity get() {
        return instance;
    }

    public static void clear() {
        instance = null;
    }
}
