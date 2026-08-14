package com.tungsten.hmclpe.launcher.uis.universal.setting.right.help;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.tungsten.hmclpe.BuildConfig;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.uis.tools.BaseUI;
import com.tungsten.hmclpe.update.UpdateChecker;
import com.tungsten.hmclpe.utils.animation.CustomAnimationUtils;

public class AboutUsUI extends BaseUI implements View.OnClickListener {

    public LinearLayout aboutUsUI;

    private Button launcherUpdateCheckBtn;
    private Button aboutLicenseViewBtn;
    private Button aboutLicenseSourceBtn;
    private TextView launcherVersionName;
    private TextView updateStatus;

    public AboutUsUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        aboutUsUI = activity.findViewById(R.id.ui_about);

        launcherUpdateCheckBtn = activity.findViewById(R.id.launcher_update_check_btn);
        aboutLicenseViewBtn = activity.findViewById(R.id.about_license_view_btn);
        aboutLicenseSourceBtn = activity.findViewById(R.id.about_license_source_btn);
        launcherVersionName = activity.findViewById(R.id.launcher_version_name);

        LinearLayout updateRow = (LinearLayout) launcherUpdateCheckBtn.getParent();
        updateStatus = (TextView) updateRow.getChildAt(0);

        launcherVersionName.setText("StarDockLauncher v" + BuildConfig.VERSION_NAME);
        updateStatus.setText(context.getString(R.string.about_update_status_default));

        launcherUpdateCheckBtn.setOnClickListener(this);
        aboutLicenseViewBtn.setOnClickListener(this);
        aboutLicenseSourceBtn.setOnClickListener(this);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(aboutUsUI, activity, context, false);
        if (activity.isLoaded) {
            activity.uiManager.settingUI.startAboutUsUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(aboutUsUI, activity, context, false);
        if (activity.isLoaded) {
            activity.uiManager.settingUI.startAboutUsUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    public void onClick(View v) {
        if (v == launcherUpdateCheckBtn) {
            updateStatus.setText(context.getString(R.string.about_update_status_checking));
            new UpdateChecker(context, activity).check(false, new UpdateChecker.UpdateCallback() {
                @Override
                public void onCheck() {
                }

                @Override
                public void onFinish(boolean latest) {
                    activity.runOnUiThread(() -> updateStatus.setText(
                            latest ? context.getString(R.string.about_update_status_latest)
                                    : context.getString(R.string.about_update_status_available)));
                }
            });
        } else if (v == aboutLicenseViewBtn) {
            showLicenseDialog();
        } else if (v == aboutLicenseSourceBtn) {
            showSourceDialog();
        }
    }

    private void showLicenseDialog() {
        String body = context.getString(R.string.about_license_body)
                + "\n\n"
                + "— GPL-3.0 License (excerpt) —\n"
                + "This program is free software: you can redistribute it and/or modify\n"
                + "it under the terms of the GNU General Public License as published by\n"
                + "the Free Software Foundation, either version 3 of the License, or\n"
                + "(at your option) any later version.\n\n"
                + "This program is distributed in the hope that it will be useful,\n"
                + "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
                + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n\n"
                + "Full text: https://www.gnu.org/licenses/gpl-3.0.txt\n"
                + "Local file (in-app): see LICENSE inside this repository, mirrored\n"
                + "in the GitHub Release asset StarDockLauncher-v"
                + BuildConfig.VERSION_NAME + "-sources.zip.";

        new AlertDialog.Builder(context)
                .setTitle(R.string.about_license_view_btn)
                .setMessage(body)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showSourceDialog() {
        String repoUrl = context.getString(R.string.about_license_source_url);
        String releaseTag = "v" + BuildConfig.VERSION_NAME;
        String sourcesAsset = "StarDockLauncher-" + releaseTag + "-sources.zip";

        new AlertDialog.Builder(context)
                .setTitle(R.string.about_license_source_btn)
                .setMessage("本应用对应源代码获取方式：\n\n"
                        + "1. 仓库（含全部历史与 Issue / Release）：\n" + repoUrl + "\n\n"
                        + "2. 精确对应本版本的 Release 页面：\n" + repoUrl + "/releases/tag/" + releaseTag + "\n\n"
                        + "3. Release 附件（完整对应源代码压缩包）：\n" + repoUrl + "/releases/download/" + releaseTag + "/" + sourcesAsset + "\n\n"
                        + "4. 一键复制仓库地址（点击下方按钮）。")
                .setPositiveButton("复制仓库地址", (dialog, which) -> {
                    ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("StarDockLauncher repo", repoUrl));
                        Toast.makeText(context, "已复制仓库地址", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("打开仓库", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception ignored) {
                    }
                })
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }
}
