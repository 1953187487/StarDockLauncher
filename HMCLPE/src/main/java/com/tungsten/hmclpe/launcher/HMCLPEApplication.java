package com.tungsten.hmclpe.launcher;

import android.app.Application;
import android.content.Context;

import com.github.gzuliyujiang.oaid.DeviceIdentifier;

public class HMCLPEApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        DeviceIdentifier.register(this);
        context = this.getApplicationContext();
        com.tungsten.hmclpe.utils.crash.CrashHandler.init(this);
    }

    public static Context getContext(){
        return context;
    }

    public static void releaseContext(){
        context = null;
    }

}