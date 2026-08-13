package com.tungsten.hmclpe.launcher.uis.universal.setting.right.help;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.uis.tools.BaseUI;
import com.tungsten.hmclpe.utils.animation.CustomAnimationUtils;

public class FeedbackUI extends BaseUI implements View.OnClickListener {

    public static final String AUTHOR_EMAIL = "1953187487@qq.com";

    public LinearLayout feedbackUI;

    private ImageButton copyEmail;

    public FeedbackUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        feedbackUI = activity.findViewById(R.id.ui_feedback);

        copyEmail = activity.findViewById(R.id.copy_email);

        copyEmail.setOnClickListener(this);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(feedbackUI,activity,context,false);
        if (activity.isLoaded){
            activity.uiManager.settingUI.startFeedbackUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(feedbackUI,activity,context,false);
        if (activity.isLoaded){
            activity.uiManager.settingUI.startFeedbackUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    public void onClick(View v) {
        if (v == copyEmail) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("StarDockLauncher 反馈邮箱", AUTHOR_EMAIL);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "已复制邮箱：" + AUTHOR_EMAIL + "\n请粘贴到邮箱客户端发给作者", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "复制失败，请手动记录邮箱：" + AUTHOR_EMAIL, Toast.LENGTH_LONG).show();
            }
            Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
            mailIntent.setData(Uri.parse("mailto:" + AUTHOR_EMAIL));
            try {
                context.startActivity(mailIntent);
            } catch (Exception ignored) {
            }
        }
    }
}
