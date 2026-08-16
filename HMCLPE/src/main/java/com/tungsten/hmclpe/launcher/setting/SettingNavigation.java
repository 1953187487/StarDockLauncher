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

    public static void openVersionTab(FragmentActivity activity) {
        try {
            NavigationRailView rail = activity.findViewById(R.id.home_nav_rail);
            if (rail != null) rail.setSelectedItemId(R.id.nav_version);
        } catch (Throwable t) {
            android.util.Log.e("SettingNavigation", "openVersionTab failed", t);
        }
    }
}
