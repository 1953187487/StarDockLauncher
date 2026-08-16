package com.tungsten.hmclpe.launcher.launch;

import android.content.Context;
import android.util.Log;

import com.tungsten.hmclpe.auth.AccountInfo;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.runtime.RuntimeInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import net.kdt.pojavlaunch.utils.Tools;

public class LaunchArgsBuilder {

    private static final String TAG = "LaunchArgsBuilder";

    public static class Result {
        public boolean ready;
        public String errorMessage;
        public Vector<String> args;

        public Result(boolean ready, String error, Vector<String> args) {
            this.ready = ready;
            this.errorMessage = error;
            this.args = args;
        }
    }

    public static Result build(Context ctx, RuntimeInfo runtime, String engine,
                               File versionDir, File gameDir, AccountInfo account,
                               String renderer, int width, int height, String server, int maxRam) {
        try {
            if (versionDir == null || !versionDir.exists()) {
                return new Result(false, "游戏版本目录不存在：\n" + (versionDir == null ? "null" : versionDir.getAbsolutePath()), null);
            }
            MinecraftVersion version = MinecraftVersion.fromDirectory(versionDir);
            if (version.mainClass == null || version.mainClass.isEmpty()) {
                return new Result(false, "无法识别游戏主类，请检查版本 json 是否正确。", null);
            }
            File javaDir;
            if (runtime == null) {
                return new Result(false, "Java 运行时未就绪，请稍后重试。", null);
            }
            javaDir = version.isHighVersion() ? runtime.java17() : runtime.java8();
            if (javaDir == null || !javaDir.exists() || !new File(javaDir, "bin/java").exists()) {
                return new Result(false, "Java 运行时未就绪：\n" + (javaDir == null ? "null" : javaDir.getAbsolutePath()) + "/bin/java", null);
            }
            File engineLib;
            String libSub;
            if (RuntimeInfo.ENGINE_BOAT.equals(engine)) {
                engineLib = runtime.boatLib();
                libSub = "lwjgl-2";
            } else {
                engineLib = runtime.pojavLib();
                libSub = "lwjgl-3";
            }
            if (engineLib == null || !engineLib.exists()) {
                return new Result(false, "引擎库未解压：" + (engineLib == null ? "null" : engineLib.getAbsolutePath()), null);
            }
            boolean high = version.isHighVersion();
            boolean isJava17 = javaDir.getName().contains("17");
            String r = "gl4es";
            String libraryPath;
            String classPath;
            StringBuilder cp = new StringBuilder();
            if (high) {
                if (engineLib != null) {
                    File lwjgl3 = new File(engineLib, "lwjgl-3");
                    if (lwjgl3.exists()) {
                        cp.append(lwjgl3).append("/lwjgl-jemalloc.jar:")
                                .append(lwjgl3).append("/lwjgl-tinyfd.jar:")
                                .append(lwjgl3).append("/lwjgl-opengl.jar:")
                                .append(lwjgl3).append("/lwjgl-openal.jar:")
                                .append(lwjgl3).append("/lwjgl-glfw.jar:")
                                .append(lwjgl3).append("/lwjgl-stb.jar:")
                                .append(lwjgl3).append("/lwjgl.jar:");
                    }
                }
                libraryPath = javaDir + "/lib:" + engineLib + ":" + engineLib + "/lwjgl-3:" + engineLib + "/renderer/" + r;
            } else {
                if (engineLib != null) {
                    File lwjgl2 = new File(engineLib, "lwjgl-2");
                    if (lwjgl2.exists()) {
                        cp.append(lwjgl2).append("/lwjgl.jar:")
                                .append(lwjgl2).append("/lwjgl_util.jar:");
                    }
                }
                libraryPath = javaDir + "/lib/aarch64/jli:" + javaDir + "/lib/aarch64:" + engineLib + ":" + engineLib + "/lwjgl-2:" + engineLib + "/renderer/" + r;
            }
            for (String lib : version.getLibraryPaths(gameDir, high)) {
                cp.append(lib).append(":");
            }
            cp.append(gameDir).append("/versions/").append(versionDir.getName()).append("/").append(versionDir.getName()).append(".jar");
            classPath = cp.toString();

            Vector<String> args = new Vector<>();
            args.add(javaDir + "/bin/java");
            args.add("-Xms" + maxRam + "M");
            args.add("-Xmx" + maxRam + "M");
            args.add("-cp");
            args.add(classPath);
            args.add("-Djava.library.path=" + libraryPath);
            args.add("-Dfml.earlyprogresswindow=false");
            args.add("-Dorg.lwjgl.util.DebugLoader=true");
            args.add("-Dorg.lwjgl.util.Debug=true");
            args.add("-Dos.name=Linux");
            args.add("-Dlwjgl.platform=Boat");
            if ("VirGL".equalsIgnoreCase(renderer)) {
                args.add("-Dorg.lwjgl.opengl.libname=libGL.so.1");
            } else {
                args.add("-Dorg.lwjgl.opengl.libname=libgl4es_114.so");
            }
            args.add("-Djava.io.tmpdir=" + ctx.getCacheDir().getAbsolutePath());
            if (account != null && !account.isOffline()) {
                args.add("-Dauthlibinjector.enabled=false");
            }
            List<String> mcArgs = buildMinecraftArgs(version, gameDir, account, server, width, height);
            args.add(version.mainClass);
            args.addAll(mcArgs);
            return new Result(true, null, args);
        } catch (Throwable t) {
            Log.e(TAG, "build failed", t);
            return new Result(false, "启动参数构建失败：" + t.getMessage(), null);
        }
    }

    private static List<String> buildMinecraftArgs(MinecraftVersion version, File gameDir,
                                                   AccountInfo account, String server, int width, int height) {
        List<String> out = new ArrayList<>();
        String player = account == null ? "Steve" : (account.username == null ? "Steve" : account.username);
        String uuid = account == null ? "00000000-0000-0000-0000-000000000000" : account.uuid;
        String token = account == null ? "0" : (account.accessToken == null ? "0" : account.accessToken);
        String userType = account == null ? "legacy" : account.userType;

        StringBuilder tpl = new StringBuilder();
        boolean high = version.isHighVersion();
        if (high && version.arguments != null && version.arguments.game != null) {
            for (Object obj : version.arguments.game) {
                if (obj instanceof String) {
                    tpl.append(obj.toString()).append(" ");
                }
            }
        } else if (version.minecraftArguments != null) {
            tpl.append(version.minecraftArguments);
        }
        String raw = tpl.toString();
        int state = 0;
        int start = 0;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (state == 0) {
                if (c != '$') {
                    result.append(c);
                } else if (i + 1 < raw.length() && raw.charAt(i + 1) == '{') {
                    state = 1;
                    start = i;
                } else {
                    result.append(c);
                }
            } else {
                if (c == '}') {
                    String key = raw.substring(start + 2, i);
                    String value = resolve(key, version, gameDir, player, uuid, token, userType);
                    result.append(value);
                    state = 0;
                }
            }
        }
        String joined = result.toString();
        Collections.addAll(out, joined.trim().split("\\s+"));
        out.add("--width");
        out.add(String.valueOf(width));
        out.add("--height");
        out.add(String.valueOf(height));
        if (server != null && !server.isEmpty()) {
            String[] ser = server.split(":");
            out.add("--server");
            out.add(ser[0]);
            out.add("--port");
            out.add(ser.length > 1 ? ser[1] : "25565");
        }
        return out;
    }

    private static String resolve(String key, MinecraftVersion version, File gameDir,
                                  String player, String uuid, String token, String userType) {
        switch (key) {
            case "version_name":
                return version.id == null ? "" : version.id;
            case "launcher_name":
                return "StarDock";
            case "launcher_version":
                return "1.1.1";
            case "version_type":
                return version.type == null ? "release" : version.type;
            case "assets_index_name":
                String idx = version.assetsIndexName();
                return idx == null ? "" : idx;
            case "game_directory":
                return gameDir.getAbsolutePath();
            case "assets_root":
                return gameDir + "/assets";
            case "user_properties":
                return "{}";
            case "auth_player_name":
                return player;
            case "auth_session":
                return "token:" + token + ":" + uuid;
            case "auth_uuid":
                return uuid == null ? "" : uuid;
            case "auth_access_token":
                return token;
            case "user_type":
                return userType;
            case "primary_jar_name":
                return version.id + ".jar";
            case "library_directory":
                return gameDir + "/libraries";
            case "classpath_separator":
                return ":";
            default:
                return "";
        }
    }
}
