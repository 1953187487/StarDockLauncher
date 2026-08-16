package com.tungsten.hmclpe.launcher.launch;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinecraftVersion {

    public String id;
    public String type;
    public String mainClass;
    public int minimumLauncherVersion;
    public String assets;
    public String minecraftArguments;
    public AssetIndex assetIndex;
    public Arguments arguments;
    public List<Library> libraries = new ArrayList<>();

    public static class AssetIndex {
        public String id;
        public String sha1;
        public long size;
        public String url;
    }

    public static class Arguments {
        public Object[] game;
    }

    public static class Library {
        public String name;
        public String url;
        public Download downloads;
        public Map<String, Object> natives;
        public Map<String, List<String>> rules;

        public String path() {
            if (downloads != null && downloads.artifact != null && downloads.artifact.path != null) {
                return downloads.artifact.path;
            }
            return name;
        }
    }

    public static class Download {
        public Artifact artifact;
    }

    public static class Artifact {
        public String path;
        public String sha1;
        public long size;
        public String url;
    }

    public boolean isHighVersion() {
        return minimumLauncherVersion >= 21;
    }

    public String assetsIndexName() {
        if (assetIndex != null && assetIndex.id != null) {
            return assetIndex.id;
        }
        return assets;
    }

    public List<String> getLibraryPaths(File gameDir, boolean high) {
        List<String> out = new ArrayList<>();
        for (Library lib : libraries) {
            String p = lib.path();
            if (p == null) {
                continue;
            }
            out.add(gameDir + "/libraries/" + p);
        }
        return out;
    }

    public static MinecraftVersion fromDirectory(File versionDir) {
        MinecraftVersion result = new MinecraftVersion();
        if (versionDir == null || !versionDir.exists() || !versionDir.isDirectory()) {
            return result;
        }
        File json = new File(versionDir, versionDir.getName() + ".json");
        if (!json.exists()) {
            for (File f : versionDir.listFiles()) {
                if (f != null && f.isFile() && f.getName().endsWith(".json")) {
                    json = f;
                    break;
                }
            }
        }
        if (!json.exists()) {
            return result;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(json), StandardCharsets.UTF_8)) {
            MinecraftVersion v = new Gson().fromJson(reader, MinecraftVersion.class);
            if (v != null) {
                if (v.id != null) result.id = v.id;
                if (v.type != null) result.type = v.type;
                if (v.mainClass != null) result.mainClass = v.mainClass;
                result.minimumLauncherVersion = v.minimumLauncherVersion;
                if (v.assets != null) result.assets = v.assets;
                if (v.minecraftArguments != null) result.minecraftArguments = v.minecraftArguments;
                result.assetIndex = v.assetIndex;
                result.arguments = v.arguments;
                if (v.libraries != null) result.libraries = v.libraries;
            }
        } catch (Throwable t) {
        }
        return result;
    }
}
