package net.kdt.pojavlaunch.agreement;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * First-launch user agreement dialog.
 * Shows EULA-like notice; the app quits unless the user accepts it.
 * On acceptance, the dialog also requests the system permissions the
 * launcher needs (audio, storage, notifications).
 */
public class AgreementDialog {

    public static final String PREF_NAME = "user_agreement";
    public static final String KEY_ACCEPTED = "agreement_accepted_v1";

    private static final int REQ_PERMISSIONS = 1001;

    public static boolean isAccepted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ACCEPTED, false);
    }

    public static void markAccepted(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ACCEPTED, true).apply();
    }

    /** Show the dialog; call {@code onAgree} when accepted, {@code onExit} when declined.
     *  v0.0.6 起每次启动都强制弹出协议（除非用户在同一会话内已同意）。 */
    public static void show(Activity activity, Runnable onAgree, Runnable onExit) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_agreement, null);
        TextView contentView = dialogView.findViewById(R.id.agreement_content);
        contentView.setMovementMethod(new ScrollingMovementMethod());
        contentView.setText(R.string.agreement_content_v2);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.agreement_title_v2)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.agreement_accept, null)
                .setNegativeButton(R.string.agreement_decline, null)
                .setNeutralButton(R.string.agreement_download_java, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                markAccepted(activity);
                dialog.dismiss();
                requestRuntimePermissions(activity);
                onAgree.run();
            });
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negative.setOnClickListener(v -> {
                dialog.dismiss();
                onExit.run();
            });
            Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutral.setOnClickListener(v -> {
                MultiRTConfigDialog jvmDialog = new MultiRTConfigDialog();
                jvmDialog.prepare(activity, null);
                jvmDialog.show();
            });
        });

        dialog.show();
    }

    /** Show the v0.0.5 open-source license dialog (linked from the about screen). */
    public static void showOpenSourceLicense(Activity activity) {
        String content =
                "本项目源码与协议：\n" +
                "  StarDockLauncher — MIT License\n" +
                "  https://github.com/1953187487/StarDockLauncher\n\n" +
                "──────────────────\n" +
                "依赖 / 内核项目：\n" +
                "  • PojavLauncher — MIT License\n" +
                "    https://github.com/PojavLauncherTeam/PojavLauncher\n" +
                "  • LWJGL3 — BSD-3-Clause\n" +
                "    https://github.com/LWJGL/lwjgl3\n\n" +
                "──────────────────\n" +
                "前端 UI、悬浮窗、协议内容、交互逻辑全部由本项目独立设计。\n\n" +
                "如对协议有疑问，可在「设置 → 开源与项目源码」查看详细列表。";
        new AlertDialog.Builder(activity)
                .setTitle(R.string.sd_setting_open_source)
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.agreement_open_source, (d, w) ->
                        Tools.openURL(activity, "https://github.com/1953187487/StarDockLauncher/blob/main/LICENSE"))
                .show();
    }

    private static void requestRuntimePermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!permissions.isEmpty()) {
            try {
                ActivityCompat.requestPermissions(activity,
                        permissions.toArray(new String[0]), REQ_PERMISSIONS);
            } catch (Exception e) {
                Toast.makeText(activity, "权限请求失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
