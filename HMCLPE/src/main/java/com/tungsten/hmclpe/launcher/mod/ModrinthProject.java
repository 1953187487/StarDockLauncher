package com.tungsten.hmclpe.launcher.mod;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ModrinthProject {

    public String slug;
    public String title;
    public String description;
    public List<String> categories;
    public String project_type;
    public int downloads;
    public String icon_url;
    @SerializedName("project_id")
    public String projectId;

    public String getType() {
        return project_type == null ? "mod" : project_type;
    }
}
