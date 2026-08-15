package com.tungsten.hmclpe.launcher.launch.pojav;

import static com.tungsten.hmclpe.launcher.setting.game.GameLaunchSetting.isHighVersion;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.tungsten.hmclpe.launcher.launch.AccountPatch;
import com.tungsten.hmclpe.launcher.setting.game.GameLaunchSetting;
import com.tungsten.hmclpe.launcher.launch.LaunchVersion;
import com.tungsten.hmclpe.launcher.launch.TouchInjector;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.manifest.info.AppInfo;
import com.tungsten.hmclpe.utils.string.StringUtils;

import net.kdt.pojavlaunch.utils.Tools;
import net.kdt.pojavlaunch.utils.JREUtils;

import java.io.File;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

public class PojavLauncher {

    private static final String TAG = "PojavLauncher";

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
            String javaPath = gameLaunchSetting.javaPath;
            if (javaPath == null || javaPath.isEmpty()) {
                return new LaunchCheckResult(false, "Java 运行时路径为空。请先在通用设置中选择 Java 运行时。", null);
            }
            File javaBin = new File(javaPath + "/bin/java");
            if (!javaBin.exists()) {
                return new LaunchCheckResult(false, "Java 运行时未就绪：\n" + javaBin.getAbsolutePath() + "\n请重新进入启动器，等待初始化完成后再启动游戏。", null);
            }
            File pojavLib = new File(AppManifest.POJAV_LIB_DIR);
            if (!pojavLib.exists() || pojavLib.listFiles() == null || pojavLib.listFiles().length == 0) {
                return new LaunchCheckResult(false, "PojavLauncher 库未解压：\n" + pojavLib.getAbsolutePath() + "\n请重新进入启动器，等待运行时资产初始化完成。", null);
            }

            JREUtils.jreReleaseList = JREUtils.readJREReleaseProperties(javaPath);
            LaunchVersion version = LaunchVersion.fromDirectory(versionDir);
            if (version.mainClass == null || version.mainClass.isEmpty()) {
                return new LaunchCheckResult(false, "无法识别游戏主类，请检查版本 json 是否正确。", null);
            }

            JREUtils.relocateLibPath(context, javaPath);
            String libraryPath = javaPath + "/lib/aarch64/jli:" + javaPath + "/lib/aarch64:" + AppManifest.POJAV_LIB_DIR + "/lwjgl3:" + JREUtils.LD_LIBRARY_PATH + ":" + AppManifest.POJAV_LIB_DIR + "/lwjgl3";
            boolean isJava17 = javaPath.endsWith("JRE17");
            String classPath = getLWJGL3ClassPath() + ":" + version.getClassPath(gameLaunchSetting.gameFileDirectory, isHighVersion(gameLaunchSetting), isJava17);
            Vector<String> args = new Vector<String>();
            Tools.getCacioJavaArgs(context, args, !isJava17, width, height);
            args.add("-Djava.home=" + javaPath);
            args.add("-Djava.io.tmpdir=" + AppManifest.DEFAULT_CACHE_DIR);
            args.add("-Duser.home=" + new File(gameLaunchSetting.gameFileDirectory).getParent());
            args.add("-Duser.language=" + System.getProperty("user.language"));
            args.add("-Dos.name=Linux");
            args.add("-Dos.version=Android-" + Build.VERSION.RELEASE);
            args.add("-Dpojav.path.minecraft=" + gameLaunchSetting.gameFileDirectory);
            args.addAll(JREUtils.getJavaArgs(context));
            args.add("-Dnet.minecraft.clientmodname=" + AppInfo.APP_NAME);
            args.add("-Dfml.earlyprogresswindow=false");
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
            args.add("-Dorg.lwjgl.opengl.libname=" + JREUtils.getGraphicsLibrary(gameLaunchSetting.pojavRenderer));
            args.add("-cp");
            args.add(classPath);
            args.add(version.mainClass);
            String[] minecraftArgs;
            minecraftArgs = version.getMinecraftArguments(gameLaunchSetting, isHighVersion(gameLaunchSetting));
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

    private static String getLWJGL3ClassPath() {
        StringBuilder libStr = new StringBuilder();
        File lwjgl3Folder = new File(AppManifest.POJAV_LIB_DIR, "lwjgl3");
        if (lwjgl3Folder.exists()) {
            File[] files = lwjgl3Folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".jar")) {
                        libStr.append(file.getAbsolutePath()).append(":");
                    }
                }
            }
        }
        if (libStr.length() > 0) {
            libStr.setLength(libStr.length() - 1);
        }
        return libStr.toString();
    }

    public static String getGlVersion(String currentVersion){
        LaunchVersion version = LaunchVersion.fromDirectory(new File(currentVersion));
        if (version == null) {
            return "2";
        }
        String creationDate = version.time;
        if(creationDate == null || creationDate.isEmpty()){
            return "2";
        }
        try {
            return Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(creationDate.substring(0, creationDate.indexOf("T")))).before(new Date(2011-1900, 6, 7)) ? "1" : "2";
        } catch (ParseException exception) {
            Log.e("OPENGL SELECTION", exception.toString());
            return "2";
        }
    }

}
