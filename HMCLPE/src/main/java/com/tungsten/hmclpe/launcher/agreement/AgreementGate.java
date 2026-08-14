package com.tungsten.hmclpe.launcher.agreement;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tungsten.hmclpe.manifest.AppManifest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hook point for SplashActivity.
 *
 * Workflow:
 *   1. Decide whether the first-run agreement or an update agreement is needed
 *      based on persisted preferences.
 *   2. Inspect bundled JAVA runtime versions to determine if a JAVA update
 *      notice should be shown.
 *   3. Show the agreement dialog.
 *   4. On accept → run the user-supplied onAccepted callback (which typically
 *      calls enterLauncher()).
 *      On reject → finish the host activity.
 */
public final class AgreementGate {

    public interface OnResolved {
        void onAccepted();

        void onRejected();
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AgreementGate() {
    }

    public static void checkAndShow(@NonNull AppCompatActivity activity,
                                    @NonNull String eulaText,
                                    @NonNull String gplSummary,
                                    @NonNull String changelog,
                                    @NonNull OnResolved callback) {
        AgreementRepository repo = new AgreementRepository(activity);
        if (repo.needsFirstRunAgreement()) {
            showFirstRun(activity, eulaText, gplSummary, repo, callback);
            return;
        }
        if (repo.needsUpdateAgreement(AgreementContent.AGREEMENT_VERSION)) {
            inspectJavaThenShow(activity, repo, /*firstRun=*/ false, changelog, callback);
            return;
        }
        if (repo.needsJavaUpdateAgreement(AgreementContent.JAVA_RUNTIME_VERSION)) {
            inspectJavaThenShow(activity, repo, /*firstRun=*/ false, changelog, callback);
            return;
        }
        callback.onAccepted();
    }

    private static void showFirstRun(@NonNull AppCompatActivity activity,
                                     @NonNull String eulaText,
                                     @NonNull String gplSummary,
                                     @NonNull AgreementRepository repo,
                                     @NonNull OnResolved callback) {
        AgreementDialogFragment.showFirstRun(activity, eulaText, gplSummary,
                new AgreementDialogFragment.AcceptanceCallback() {
                    @Override
                    public void onAccepted(boolean javaUpdatesAvailable) {
                        repo.accept(AgreementContent.AGREEMENT_VERSION,
                                AgreementContent.JAVA_RUNTIME_VERSION, true, true);
                        callback.onAccepted();
                    }

                    @Override
                    public void onRejected() {
                        callback.onRejected();
                    }
                });
    }

    private static void inspectJavaThenShow(@NonNull AppCompatActivity activity,
                                            @NonNull AgreementRepository repo,
                                            boolean firstRun,
                                            @NonNull String changelog,
                                            @NonNull OnResolved callback) {
        IO.execute(() -> {
            JavaUpdateManager mgr = new JavaUpdateManager(activity);
            List<JavaUpdateManager.RuntimeInfo> infos = new ArrayList<>();
            infos.add(mgr.inspect(JavaUpdateManager.JAVA_DEFAULT, AppManifest.JAVA_DIR + "/default"));
            infos.add(mgr.inspect(JavaUpdateManager.JAVA_JRE17, AppManifest.JAVA_DIR + "/JRE17"));
            List<JavaUpdateManager.RuntimeInfo> needsUpdate = new ArrayList<>();
            for (JavaUpdateManager.RuntimeInfo ri : infos) {
                if (ri.needsUpdate) {
                    needsUpdate.add(ri);
                }
            }
            MAIN.post(() -> {
                if (firstRun) {
                    AgreementDialogFragment.showFirstRun(activity, "", "",
                            new AgreementDialogFragment.AcceptanceCallback() {
                                @Override
                                public void onAccepted(boolean javaUpdatesAvailable) {
                                    repo.accept(AgreementContent.AGREEMENT_VERSION,
                                            AgreementContent.JAVA_RUNTIME_VERSION, true, true);
                                    callback.onAccepted();
                                }

                                @Override
                                public void onRejected() {
                                    callback.onRejected();
                                }
                            });
                } else {
                    AgreementDialogFragment.showUpdate(activity, changelog, needsUpdate,
                            new AgreementDialogFragment.AcceptanceCallback() {
                                @Override
                                public void onAccepted(boolean javaUpdatesAvailable) {
                                    repo.accept(AgreementContent.AGREEMENT_VERSION,
                                            javaUpdatesAvailable ? AgreementContent.JAVA_RUNTIME_VERSION : repo.currentJavaVersion(),
                                            true, true);
                                    callback.onAccepted();
                                }

                                @Override
                                public void onRejected() {
                                    callback.onAccepted();
                                }
                            });
                }
            });
        });
    }

    public static void resetForDebug(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        new AgreementRepository(activity).reset();
    }
}
