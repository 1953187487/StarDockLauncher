package net.kdt.pojavlaunch.agreement;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;

/**
 * First-launch user agreement dialog.
 * Shows EULA-like notice; the app quits unless the user accepts it.
 * Also offers an in-app link to open the open-source license and to download
 * a Java runtime through Pojav's multi-runtime installer.
 */
public class AgreementDialog {

    public static final String PREF_NAME = "user_agreement";
    public static final String KEY_ACCEPTED = "agreement_accepted_v1";

    public static boolean isAccepted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ACCEPTED, false);
    }

    public static void markAccepted(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ACCEPTED, true).apply();
    }

    /** Show the dialog; call {@code onAgree} when accepted, {@code onExit} when declined. */
    public static void show(Activity activity, Runnable onAgree, Runnable onExit) {
        if (isAccepted(activity)) {
            onAgree.run();
            return;
        }

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_agreement, null);
        TextView contentView = dialogView.findViewById(R.id.agreement_content);
        contentView.setMovementMethod(new ScrollingMovementMethod());

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.agreement_title)
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

    /** Show the open-source license dialog at any time (linked from the about screen). */
    public static void showOpenSourceLicense(Activity activity) {
        String content =
                "本应用基于 PojavLauncher 进行二次开发。\n\n" +
                "上游内核协议：\n" +
                "  PojavLauncher — MIT License\n" +
                "  https://github.com/PojavLauncherTeam/PojavLauncher\n\n" +
                "本项目协议：\n" +
                "  StarDockLauncher — MIT License\n" +
                "  https://github.com/1953187487/StarDockLauncher\n\n" +
                "感谢 PojavLauncher 团队与社区贡献者。";
        new AlertDialog.Builder(activity)
                .setTitle(R.string.agreement_open_source_view)
                .setMessage(content)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.agreement_open_source, (d, w) ->
                        Tools.openURL(activity, "https://github.com/1953187487/StarDockLauncher/blob/main/LICENSE"))
                .show();
    }
}
