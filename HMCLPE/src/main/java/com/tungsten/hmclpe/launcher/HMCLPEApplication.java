package com.tungsten.hmclpe.launcher;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.github.gzuliyujiang.oaid.DeviceIdentifier;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.utils.crash.CrashHandler;

import java.io.File;

public class HMCLPEApplication extends Application {

    private static final String TAG = "StarDockApp";
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        DeviceIdentifier.register(this);
        context = this.getApplicationContext();
        CrashHandler.init(this);
        Log.i(TAG, "应用启动 - 运行时资产检查");
        verifyRuntimeAssets();
    }

    private void verifyRuntimeAssets() {
        File runtimeVersion = new File(AppManifest.DEFAULT_RUNTIME_DIR + "/version");
        File java8 = new File(AppManifest.JAVA_DIR + "/default/bin/java");
        File java17 = new File(AppManifest.JAVA_DIR + "/JRE17/bin/java");
        File boatLib = new File(AppManifest.BOAT_LIB_DIR);

        Log.i(TAG, "Runtime version file: " + runtimeVersion.exists() + " @ " + runtimeVersion);
        Log.i(TAG, "Java 8: " + java8.exists() + " @ " + java8);
        Log.i(TAG, "Java 17: " + java17.exists() + " @ " + java17);
        Log.i(TAG, "Boat lib: " + boatLib.exists() + " @ " + boatLib);
    }

    public static Context getContext(){
        return context;
    }

    public static void releaseContext(){
        context = null;
    }

}
