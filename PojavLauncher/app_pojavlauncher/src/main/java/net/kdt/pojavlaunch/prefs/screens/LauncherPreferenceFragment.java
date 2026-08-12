package net.kdt.pojavlaunch.prefs.screens;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * Preference for the main screen, any sub-screen should inherit this class for consistent behavior,
 * overriding only onCreatePreferences
 */
public class LauncherPreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(getResources().getColor(R.color.background_app));
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_main);
        setupNotificationRequestPreference();
        setupUpdateAndAboutPreferences();
    }

    private void setupUpdateAndAboutPreferences() {
        Activity activity = getActivity();
        if (activity == null) return;
        Preference checkUpdatePref = findPreference("check_update_setting");
        if (checkUpdatePref != null) {
            checkUpdatePref.setOnPreferenceClickListener(p -> {
                if (activity instanceof LauncherActivity) {
                    ((LauncherActivity) activity).triggerUpdateCheck();
                } else {
                    Tools.openURL(activity, Tools.URL_UPDATE);
                }
                return true;
            });
        }
        Preference aboutPref = findPreference("about_setting");
        if (aboutPref != null) {
            aboutPref.setOnPreferenceClickListener(p -> {
                showAboutDialog(activity);
                return true;
            });
        }
    }

    private void showAboutDialog(Activity activity) {
        String version;
        try {
            version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "0.0.2";
        }
        String content =
                "StarDockLauncher\n" +
                "应用版本：v" + version + "\n\n" +
                "──────────────────\n" +
                "【用户使用协议】\n" +
                "首次启动已完整展示用户须知与使用协议。\n" +
                "您可以随时在协议弹窗中查看完整内容。\n\n" +
                "【开源协议 / 内核协议】\n" +
                "本应用基于 PojavLauncher（MIT License）\n" +
                "进行二次开发，遵循 MIT 协议开源。\n" +
                "PojavLauncher 内核版权与协议信息：\n" +
                "  https://github.com/PojavLauncherTeam/PojavLauncher\n" +
                "本项目协议：\n" +
                "  https://github.com/1953187487/StarDockLauncher\n\n" +
                "【免责声明】\n" +
                "本项目与 Mojang / Microsoft 无任何关联。\n" +
                "《Minecraft》相关商标归其各自权利人所有。";
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(R.string.preference_about_title)
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.agreement_open_source, (d, w) ->
                        Tools.openURL(activity, "https://github.com/1953187487/StarDockLauncher/blob/main/LICENSE"))
                .show();
    }

    private void setupNotificationRequestPreference() {
        Preference mRequestNotificationPermissionPreference = requirePreference("notification_permission_request");
        Activity activity = getActivity();
        if(activity instanceof LauncherActivity) {
            LauncherActivity launcherActivity = (LauncherActivity)activity;
            mRequestNotificationPermissionPreference.setVisible(!launcherActivity.checkForNotificationPermission());
            mRequestNotificationPermissionPreference.setOnPreferenceClickListener(preference -> {
                launcherActivity.askForNotificationPermission(()->mRequestNotificationPermissionPreference.setVisible(false));
                return true;
            });
        }else{
            mRequestNotificationPermissionPreference.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if(sharedPreferences != null) sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        LauncherPreferences.loadPreferences(getContext());
    }

    protected Preference requirePreference(CharSequence key) {
        Preference preference = findPreference(key);
        if(preference != null) return preference;
        throw new IllegalStateException("Preference "+key+" is null");
    }
    @SuppressWarnings("unchecked")
    protected <T extends Preference> T requirePreference(CharSequence key, Class<T> preferenceClass) {
        Preference preference = requirePreference(key);
        if(preferenceClass.isInstance(preference)) return (T)preference;
        throw new IllegalStateException("Preference "+key+" is not an instance of "+preferenceClass.getSimpleName());
    }
}
