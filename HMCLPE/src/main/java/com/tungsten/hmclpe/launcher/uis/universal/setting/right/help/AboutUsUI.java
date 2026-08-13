package com.tungsten.hmclpe.launcher.uis.universal.setting.right.help;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tungsten.hmclpe.BuildConfig;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.uis.tools.BaseUI;
import com.tungsten.hmclpe.update.UpdateChecker;
import com.tungsten.hmclpe.utils.animation.CustomAnimationUtils;

public class AboutUsUI extends BaseUI implements View.OnClickListener {

    public LinearLayout aboutUsUI;

    private Button launcherUpdateCheckBtn;
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
        launcherVersionName = activity.findViewById(R.id.launcher_version_name);

        LinearLayout updateRow = (LinearLayout) launcherUpdateCheckBtn.getParent();
        updateStatus = (TextView) updateRow.getChildAt(0);

        launcherVersionName.setText("StarDockLauncher v" + BuildConfig.VERSION_NAME);
        updateStatus.setText(context.getString(R.string.about_update_status_default));

        launcherUpdateCheckBtn.setOnClickListener(this);
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
        }
    }
}
