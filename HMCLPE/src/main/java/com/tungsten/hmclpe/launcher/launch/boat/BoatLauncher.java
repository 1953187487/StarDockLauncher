package com.tungsten.hmclpe.launcher.launch.boat;

import android.content.Context;
import android.util.Log;

import com.tungsten.hmclpe.launcher.launch.AccountPatch;
import com.tungsten.hmclpe.launcher.setting.game.GameLaunchSetting;
import com.tungsten.hmclpe.launcher.launch.LaunchVersion;
import com.tungsten.hmclpe.launcher.launch.TouchInjector;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.utils.string.StringUtils;

import net.kdt.pojavlaunch.utils.Tools;

import java.io.File;
import java.util.Collections;
import java.util.Vector;

public class BoatLauncher {

    private static final String TAG = "BoatLauncher";

    public static class LaunchCheckResult {
        public boolean ready;
        public String errorMessage;
        public Vector<String> args;

        public LaunchCheckResult(boolean ready, String error, Vector<String> args) {
            this.ready = ready;
            this.errorMessage = error;
            this.args = args;
        }
    }

    public static LaunchCheckResult buildLaunchArgs(GameLaunchSetting gameLaunchSetting, Context context, int width, int height, String server) {
        try {
            File versionDir = new File(gameLaunchSetting.currentVersion);
            if (!versionDir.exists()) {
                return new LaunchCheckResult(false, "游戏版本目录不存在：" + gameLaunchSetting.currentVersion + "\n请先在版本管理中创建或下载游戏。", null);
            }
            LaunchVersion version = LaunchVersion.fromDirectory(versionDir);
            String javaPath = gameLaunchSetting.javaPath;
            if (javaPath == null || javaPath.isEmpty()) {
                return new LaunchCheckResult(false, "Java 运行时路径为空。请先在通用设置中选择 Java 运行时。", null);
            }
            File javaBin = new File(javaPath + "/bin/java");
            if (!javaBin.exists()) {
                return new LaunchCheckResult(false, "Java 运行时未就绪：\n" + javaBin.getAbsolutePath() + "\n请重新进入启动器，等待初始化完成后再启动游戏。", null);
            }
            File boatLib = new File(AppManifest.BOAT_LIB_DIR);
            if (!boatLib.exists() || boatLib.listFiles() == null || boatLib.listFiles().length == 0) {
                return new LaunchCheckResult(false, "Boat 库未解压：\n" + boatLib.getAbsolutePath() + "\n请重新进入启动器，等待运行时资产初始化完成。", null);
            }
            if (version.mainClass == null || version.mainClass.isEmpty()) {
                return new LaunchCheckResult(false, "无法识别游戏主类，请检查版本 json 是否正确。", null);
            }

            boolean highVersion = false;
            if (version.minimumLauncherVersion >= 21) {
                highVersion = true;
            }
            String libraryPath;
            String classPath;
            String r = gameLaunchSetting.boatRenderer.equals("VirGL") ? "virgl" : "gl4es";
            boolean isJava17 = javaPath.endsWith("JRE17");
            if (!highVersion) {
                libraryPath = javaPath + "/lib/aarch64/jli:" + javaPath + "/lib/aarch64:" + AppManifest.BOAT_LIB_DIR + ":" + AppManifest.BOAT_LIB_DIR + "/lwjgl-2:" + AppManifest.BOAT_LIB_DIR + "/renderer/" + r;
                classPath = AppManifest.BOAT_LIB_DIR + "/lwjgl-2/lwjgl.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-2/lwjgl_util.jar:" + version.getClassPath(gameLaunchSetting.gameFileDirectory, false, false);
            } else {
                if (isJava17) {
                    libraryPath = javaPath + "/lib:" + AppManifest.BOAT_LIB_DIR + ":" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3:" + AppManifest.BOAT_LIB_DIR + "/renderer/" + r;
                } else {
                    libraryPath = javaPath + "/lib/jli:" + javaPath + "/lib:" + AppManifest.BOAT_LIB_DIR + ":" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3:" + AppManifest.BOAT_LIB_DIR + "/renderer/" + r;
                }
                classPath = AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-jemalloc.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-tinyfd.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-opengl.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-openal.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-glfw.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-stb.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl.jar:" + version.getClassPath(gameLaunchSetting.gameFileDirectory, true, isJava17);
            }
            Vector<String> args = new Vector<String>();
            args.add(javaPath + "/bin/java");
            Tools.getCacioJavaArgs(context, args, !isJava17, width, height);
            args.add("-cp");
            args.add(classPath);
            args.add("-Djava.library.path=" + libraryPath);
            args.add("-Dfml.earlyprogresswindow=false");
            args.add("-Dorg.lwjgl.util.DebugLoader=true");
            args.add("-Dorg.lwjgl.util.Debug=true");
            args.add("-Dos.name=Linux");
            args.add("-Dlwjgl.platform=Boat");
            if (gameLaunchSetting.boatRenderer.equals("VirGL")) {
                args.add("-Dorg.lwjgl.opengl.libname=libGL.so.1");
            } else {
                args.add("-Dorg.lwjgl.opengl.libname=libgl4es_114.so");
            }
            args.add("-Dlwjgl.platform=Boat");
            args.add("-Dos.name=Linux");
            args.add("-Djava.io.tmpdir=" + AppManifest.DEFAULT_CACHE_DIR);
            String[] accountArgs;
            accountArgs = AccountPatch.getAccountArgs(context, gameLaunchSetting.account);
            Collections.addAll(args, accountArgs);
            String[] JVMArgs;
            JVMArgs = version.getJVMArguments(gameLaunchSetting);
            for (int i = 0; i < JVMArgs.length; i++) {
                if (JVMArgs[i].startsWith("-DignoreList") && !JVMArgs[i].endsWith("," + new File(gameLaunchSetting.currentVersion).getName() + ".jar")) {
                    JVMArgs[i] = JVMArgs[i] + "," + new File(gameLaunchSetting.currentVersion).getName() + ".jar";
                }
                if (!JVMArgs[i].startsWith("-DFabricMcEmu") && !JVMArgs[i].startsWith("net.minecraft.client.main.Main")) {
                    args.add(JVMArgs[i]);
                }
            }
            args.add("-Xms" + gameLaunchSetting.minRam + "M");
            args.add("-Xmx" + gameLaunchSetting.maxRam + "M");
            if (!gameLaunchSetting.extraJavaFlags.equals("")) {
                String[] extraJavaFlags = gameLaunchSetting.extraJavaFlags.split(" ");
                Collections.addAll(args, extraJavaFlags);
            }
            args.add(version.mainClass);
            String[] minecraftArgs;
            minecraftArgs = version.getMinecraftArguments(gameLaunchSetting, highVersion);
            Collections.addAll(args, minecraftArgs);
            args.add("--width");
            args.add(Integer.toString(width));
            args.add("--height");
            args.add(Integer.toString(height));
            if (StringUtils.isNotBlank(server)) {
                String[] ser = server.split(":");
                args.add("--server");
                args.add(ser[0]);
                args.add("--port");
                args.add(ser.length > 1 ? ser[1] : "25565");
            }
            String[] extraMinecraftArgs = gameLaunchSetting.extraMinecraftFlags.split(" ");
            Collections.addAll(args, extraMinecraftArgs);
            Vector<String> finalArgs = TouchInjector.rebaseArguments(gameLaunchSetting, args);
            return new LaunchCheckResult(true, null, finalArgs);
        } catch (Exception e) {
            Log.e(TAG, "启动参数构建失败", e);
            return new LaunchCheckResult(false, "启动参数构建失败：" + e.getClass().getSimpleName() + "\n" + e.getMessage() + "\n\n" + Log.getStackTraceString(e), null);
        }
    }

    public static Vector<String> getMcArgs(GameLaunchSetting gameLaunchSetting, Context context, int width, int height, String server) {
        LaunchCheckResult r = buildLaunchArgs(gameLaunchSetting, context, width, height, server);
        return r.args;
    }
}
