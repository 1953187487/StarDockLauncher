package net.kdt.pojavlaunch.agreement;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import net.kdt.pojavlaunch.R;

/**
 * First-launch user agreement dialog.
 * Shows a EULA-like notice; the app quits unless the user accepts it.
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

        new AlertDialog.Builder(activity)
                .setTitle(R.string.agreement_title)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.agreement_accept, (dialog, which) -> {
                    markAccepted(activity);
                    onAgree.run();
                })
                .setNegativeButton(R.string.agreement_decline, (dialog, which) -> onExit.run())
                .show();
    }
}
