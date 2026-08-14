package com.tungsten.hmclpe.launcher.agreement;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.tungsten.hmclpe.R;

/**
 * The first-run / per-update agreement dialog.
 *
 * Behaviour:
 *   - Cancel/Back/Disagree → finish() the host activity (the launcher refuses
 *     to continue until the user accepts).
 *   - Agree is only enabled when the user ticks both the EULA and GPL
 *     acknowledgement checkboxes on first-run.
 */
public final class AgreementDialogFragment extends DialogFragment {

    private static final String TAG = "AgreementDialog";

    public interface AcceptanceCallback {
        void onAccepted(boolean javaUpdatesAvailable);

        void onRejected();
    }

    public static void showFirstRun(@NonNull FragmentActivity activity,
                                    @NonNull String eulaText,
                                    @NonNull String gplSummary,
                                    @NonNull AcceptanceCallback cb) {
        show(activity, AgreementContent.forFirstRun(eulaText, gplSummary), cb, true);
    }

    public static void showUpdate(@NonNull FragmentActivity activity,
                                  @NonNull String changelog,
                                  @NonNull java.util.List<JavaUpdateManager.RuntimeInfo> javaUpdates,
                                  @NonNull AcceptanceCallback cb) {
        show(activity, AgreementContent.forUpdate(changelog, javaUpdates), cb, false);
    }

    private static void show(@NonNull FragmentActivity activity,
                             @NonNull AgreementContent.Snapshot snapshot,
                             @NonNull AcceptanceCallback cb,
                             boolean requireCheckboxes) {
        AgreementDialogFragment frag = new AgreementDialogFragment();
        frag.snapshot = snapshot;
        frag.callback = cb;
        frag.requireCheckboxes = requireCheckboxes;
        frag.setCancelable(false);
        frag.show(activity.getSupportFragmentManager(), TAG);
    }

    private AgreementContent.Snapshot snapshot;
    private AcceptanceCallback callback;
    private boolean requireCheckboxes;
    private CheckBox cbEula;
    private CheckBox cbGpl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agreement, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(d.getWindow().getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85f);
            d.getWindow().setLayout(lp.width, lp.height);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView title = view.findViewById(R.id.agreement_title);
        LinearLayout sectionsContainer = view.findViewById(R.id.agreement_sections);
        cbEula = view.findViewById(R.id.agreement_cb_eula);
        cbGpl = view.findViewById(R.id.agreement_cb_gpl);
        CheckBox cbData = view.findViewById(R.id.agreement_cb_data);
        Button btnAccept = view.findViewById(R.id.agreement_btn_accept);
        Button btnReject = view.findViewById(R.id.agreement_btn_reject);

        title.setText(snapshot.isFirstRun
                ? R.string.agreement_title_first_run
                : R.string.agreement_title_update);

        sectionsContainer.removeAllViews();
        for (AgreementContent.Section s : snapshot.sections) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_agreement_section, sectionsContainer, false);
            ((TextView) row.findViewById(R.id.section_title)).setText(s.titleResId);
            TextView body = row.findViewById(R.id.section_body);
            body.setText(s.body);
            body.setMovementMethod(ScrollingMovementMethod.getInstance());
            sectionsContainer.addView(row);
        }

        cbEula.setVisibility(requireCheckboxes ? View.VISIBLE : View.GONE);
        cbGpl.setVisibility(requireCheckboxes ? View.VISIBLE : View.GONE);
        cbData.setVisibility(requireCheckboxes ? View.VISIBLE : View.GONE);

        if (requireCheckboxes) {
            btnAccept.setEnabled(false);
            View.OnClickListener toggle = v -> btnAccept.setEnabled(
                    cbEula.isChecked() && cbGpl.isChecked() && cbData.isChecked());
            cbEula.setOnClickListener(toggle);
            cbGpl.setOnClickListener(toggle);
            cbData.setOnClickListener(toggle);
        } else {
            btnAccept.setEnabled(true);
        }

        btnAccept.setOnClickListener(v -> {
            boolean javaUpdatesAvailable = !snapshot.javaUpdates.isEmpty();
            if (callback != null) {
                callback.onAccepted(javaUpdatesAvailable);
            }
            dismissAllowingStateLoss();
        });
        btnReject.setOnClickListener(v -> {
            if (callback != null) {
                callback.onRejected();
            }
            dismissAllowingStateLoss();
        });

        if (!requireCheckboxes) {
            btnReject.setText(R.string.agreement_action_later);
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (callback != null) {
            callback.onRejected();
        }
    }
}
