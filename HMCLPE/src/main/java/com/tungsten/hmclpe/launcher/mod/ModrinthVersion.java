package com.tungsten.hmclpe.launcher.mod;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ModrinthVersion {

    public String id;
    public String name;
    @SerializedName("version_number")
    public String versionNumber;
    public List<String> game_versions;
    public List<String> loaders;
    public List<File> files;
    @SerializedName("project_id")
    public String projectId;

    public static class File {
        public String url;
        public String filename;
        public int size;
        public String primary;
    }

    public String primaryUrl() {
        if (files == null || files.isEmpty()) {
            return null;
        }
        for (File f : files) {
            if (Boolean.parseBoolean(f.primary)) {
                return f.url;
            }
        }
        return files.get(0).url;
    }

    public String primaryName() {
        if (files == null || files.isEmpty()) {
            return versionNumber + ".jar";
        }
        for (File f : files) {
            if (Boolean.parseBoolean(f.primary)) {
                return f.filename;
            }
        }
        return files.get(0).filename;
    }
}
