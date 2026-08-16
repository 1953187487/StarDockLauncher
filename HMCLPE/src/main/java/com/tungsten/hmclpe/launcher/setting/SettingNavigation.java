package com.tungsten.hmclpe.launcher.setting;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.navigationrail.NavigationRailView;
import com.tungsten.hmclpe.R;

public class SettingNavigation {

    public static void openDownloadTab(FragmentActivity activity) {
        try {
            NavigationRailView rail = activity.findViewById(R.id.home_nav_rail);
            if (rail != null) rail.setSelectedItemId(R.id.nav_download);
        } catch (Throwable t) {
            android.util.Log.e("SettingNavigation", "openDownloadTab failed", t);
        }
    }

    public static void openSettingTab(FragmentActivity activity) {
        try {
            NavigationRailView rail = activity.findViewById(R.id.home_nav_rail);
            if (rail != null) rail.setSelectedItemId(R.id.nav_setting);
        } catch (Throwable t) {
            android.util.Log.e("SettingNavigation", "openSettingTab failed", t);
        }
    }

    public static void openToolsTab(FragmentActivity activity) {
        try {
            NavigationRailView rail = activity.findViewById(R.id.home_nav_rail);
            if (rail != null) rail.setSelectedItemId(R.id.nav_tools);
        } catch (Throwable t) {
            android.util.Log.e("SettingNavigation", "openToolsTab failed", t);
        }
    }

    public static void openVersionTab(FragmentActivity activity) {
        // legacy Version tab removed in v1.0.7
        if (activity == null) return;
        try {
            NavigationRailView rail = activity.findViewById(R.id.home_nav_rail);
            if (rail != null) rail.setSelectedItemId(R.id.nav_download);
        } catch (Throwable t) {
            android.util.Log.e("SettingNavigation", "openVersionTab failed", t);
        }
    }
}
