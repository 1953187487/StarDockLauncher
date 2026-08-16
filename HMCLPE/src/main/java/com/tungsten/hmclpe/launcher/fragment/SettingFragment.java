package com.tungsten.hmclpe.launcher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.stardock.launcher.R;
import com.tungsten.hmclpe.ai.AiChatActivity;
import com.tungsten.hmclpe.launcher.uis.about.AboutActivity;
import com.tungsten.hmclpe.launcher.uis.crash.CrashLogViewerActivity;
import com.tungsten.hmclpe.launcher.uis.update.UpdateDownloadActivity;
import com.tungsten.hmclpe.utils.Prefs;

public class SettingFragment extends Fragment {

    private static final String TAG = "SettingFragment";

    private static final String KEY_DARK = "ui_dark_mode";
    private static final String KEY_RUNTIME = "launcher_runtime";
    private static final String KEY_AUTO_UPDATE = "auto_check_update";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_setting, container, false);
        } catch (Throwable t) {
            Log.e(TAG, "inflate failed", t);
            return null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Prefs prefs;
        try {
            prefs = Prefs.get(requireContext());
        } catch (Throwable t) {
            Log.e(TAG, "prefs failed", t);
            return;
        }
        try {
            SwitchMaterial swDark = view.findViewById(R.id.setting_switch_dark);
            if (swDark != null) {
                swDark.setChecked(prefs.getBool(KEY_DARK, false));
                swDark.setOnCheckedChangeListener((b, c) -> {
                    prefs.putBool(KEY_DARK, c);
                    toast(c ? "已启用深色主题（重启后生效）" : "已切换浅色主题（重启后生效）");
                });
            }
        } catch (Throwable t) {
            Log.e(TAG, "dark switch failed", t);
        }
        try {
            SwitchMaterial swAuto = view.findViewById(R.id.setting_switch_auto_update);
            if (swAuto != null) {
                swAuto.setChecked(prefs.getBool(KEY_AUTO_UPDATE, true));
                swAuto.setOnCheckedChangeListener((b, c) -> prefs.putBool(KEY_AUTO_UPDATE, c));
            }
        } catch (Throwable t) {
            Log.e(TAG, "auto switch failed", t);
        }
        try {
            MaterialButton btnRuntime = view.findViewById(R.id.setting_btn_runtime);
            if (btnRuntime != null) {
                btnRuntime.setOnClickListener(v -> toast("运行时：" + (prefs.getString(KEY_RUNTIME, "Boat"))));
            }
        } catch (Throwable t) {
            Log.e(TAG, "runtime btn failed", t);
        }
        try {
            MaterialButton btnAi = view.findViewById(R.id.setting_btn_ai);
            if (btnAi != null) {
                btnAi.setOnClickListener(v -> openActivity(AiChatActivity.class));
            }
        } catch (Throwable t) {
            Log.e(TAG, "ai btn failed", t);
        }
        try {
            MaterialButton btnUpdate = view.findViewById(R.id.setting_btn_update);
            if (btnUpdate != null) {
                btnUpdate.setOnClickListener(v -> openActivity(UpdateDownloadActivity.class));
            }
        } catch (Throwable t) {
            Log.e(TAG, "update btn failed", t);
        }
        try {
            MaterialButton btnCrash = view.findViewById(R.id.setting_btn_crash);
            if (btnCrash != null) {
                btnCrash.setOnClickListener(v -> openActivity(CrashLogViewerActivity.class));
            }
        } catch (Throwable t) {
            Log.e(TAG, "crash btn failed", t);
        }
        try {
            MaterialButton btnAbout = view.findViewById(R.id.setting_btn_about);
            if (btnAbout != null) {
                btnAbout.setOnClickListener(v -> openActivity(AboutActivity.class));
            }
        } catch (Throwable t) {
            Log.e(TAG, "about btn failed", t);
        }
    }

    private void openActivity(Class<?> cls) {
        try {
            startActivity(new Intent(requireContext(), cls));
        } catch (Throwable t) {
            Log.e(TAG, "open " + cls.getSimpleName() + " failed", t);
            toast("打开失败：" + cls.getSimpleName());
        }
    }

    private void toast(String s) {
        try {
            Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }
}
