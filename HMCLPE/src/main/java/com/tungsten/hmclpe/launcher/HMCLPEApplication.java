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
        Log.i(TAG, "应用启动");
        try {
            AppManifest.initializeManifest(this);
            verifyRuntimeAssets();
        } catch (Throwable t) {
            Log.e(TAG, "运行时资产检查失败", t);
        }
    }

    private void verifyRuntimeAssets() {
        try {
            String runtimeDir = AppManifest.DEFAULT_RUNTIME_DIR;
            if (runtimeDir == null || runtimeDir.isEmpty()) {
                Log.w(TAG, "Runtime 目录未初始化，跳过校验");
                return;
            }
            File runtimeVersion = new File(runtimeDir, "version");
            String javaDir = AppManifest.JAVA_DIR;
            File java8 = javaDir != null ? new File(javaDir + "/default/bin/java") : null;
            File java17 = javaDir != null ? new File(javaDir + "/JRE17/bin/java") : null;
            File boatLib = AppManifest.BOAT_LIB_DIR != null ? new File(AppManifest.BOAT_LIB_DIR) : null;

            Log.i(TAG, "Runtime version file: " + (runtimeVersion != null && runtimeVersion.exists()) + " @ " + runtimeVersion);
            Log.i(TAG, "Java 8: " + (java8 != null && java8.exists()) + " @ " + java8);
            Log.i(TAG, "Java 17: " + (java17 != null && java17.exists()) + " @ " + java17);
            Log.i(TAG, "Boat lib: " + (boatLib != null && boatLib.exists()) + " @ " + boatLib);
        } catch (Throwable t) {
            Log.e(TAG, "verifyRuntimeAssets 异常", t);
        }
    }

    public static Context getContext(){
        return context;
    }

    public static void releaseContext(){
        context = null;
    }

}
